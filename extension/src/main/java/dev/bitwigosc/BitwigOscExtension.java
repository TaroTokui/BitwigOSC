package dev.bitwigosc;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import com.bitwig.extension.api.opensoundcontrol.OscAddressSpace;
import com.bitwig.extension.api.opensoundcontrol.OscConnection;
import com.bitwig.extension.api.opensoundcontrol.OscMessage;
import com.bitwig.extension.api.opensoundcontrol.OscModule;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SettableStringValue;

/**
 * TouchOSC と OSC で直接やりとりする Bitwig の拡張。
 * 届いたメッセージをコンソールに出し、/ping に /pong を返し、/sync に今の状態を全部返し、トラックとトランスポートを操作し、Bitwig 側の変化を送り返す。
 */
public class BitwigOscExtension extends ControllerExtension
{
   private static final String CATEGORY = "TouchOSC";
   private static final int DEFAULT_RECEIVE_PORT = 10001;
   private static final int DEFAULT_SEND_PORT = 10002;
   private static final String DEFAULT_SEND_HOST = "127.0.0.1";
   private static final int MIN_PORT = 1024;
   private static final int MAX_PORT = 65535;

   // 書くのは init() だけだが、読むのは OSC の受信と Observer なので、ほかのスレッドからも見えるようにする。
   private volatile OscConnection touchOsc;
   private BitwigControls controls;
   // 送り先と Observer ができてから入れる。それまでは null で、/sync が来ても送り返さない。
   private volatile Feedback feedback;
   private final SendFailureLog sendFailures = new SendFailureLog();
   // 設定の Observer と OSC の受信が別のスレッドでも、新しい値が見えるようにする。
   private volatile boolean logIncoming;

   protected BitwigOscExtension(final BitwigOscDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      final ControllerHost host = getHost();
      final Preferences prefs = host.getPreferences();
      final SettableRangedValue receivePortSetting = prefs.getNumberSetting("Receive port", CATEGORY,
         MIN_PORT, MAX_PORT, 1, "", DEFAULT_RECEIVE_PORT);
      final SettableStringValue sendHostSetting = prefs.getStringSetting("TouchOSC host", CATEGORY, 64,
         DEFAULT_SEND_HOST);
      final SettableRangedValue sendPortSetting = prefs.getNumberSetting("Send port", CATEGORY,
         MIN_PORT, MAX_PORT, 1, "", DEFAULT_SEND_PORT);
      // フェーダーは大量に送ってくるので、ログはオフにもできるようにする。再起動なしで切り替わる。
      final SettableBooleanValue logSetting = prefs.getBooleanSetting("Log incoming messages", CATEGORY, true);

      final int receivePort = (int) receivePortSetting.getRaw();
      final String sendHost = sendHostSetting.get().trim();
      final int sendPort = (int) sendPortSetting.getRaw();
      logIncoming = logSetting.get();
      logSetting.addValueObserver(enabled -> logIncoming = enabled);
      // 起動前に登録する。ホストが空で止まっても、直せば自動で起動し直す。
      restartOnChange(receivePortSetting, sendHostSetting, sendPortSetting);
      // OSC の受信を始める前に作る(受信の処理から使うため)。
      controls = new BitwigControls(host);

      if (sendHost.isEmpty())
      {
         fail("TouchOSC host is empty. Set it in the controller settings and reload.");
         return;
      }

      final OscModule osc = host.getOscModule();
      final OscAddressSpace space = osc.createAddressSpace();
      space.setName("BitwigOSC");
      space.registerDefaultMethod(this::onMessage);

      try
      {
         osc.createUdpServer(space).start(receivePort);
      }
      catch (final IOException e)
      {
         fail("Cannot listen on UDP port " + receivePort + ": " + e.getMessage());
         return;
      }
      try
      {
         touchOsc = osc.connectToUdpServer(sendHost, sendPort, space);
      }
      catch (final RuntimeException e)
      {
         fail("Cannot send to " + sendHost + ":" + sendPort + ": " + e.getMessage());
         return;
      }
      // 送り先ができてから登録する。起動直後の今の値を取りこぼさないため(送り先がなければ送らずに捨てる)。
      final Feedback created = new Feedback(this::send);
      controls.observe(created);
      // Observer を付けてから見せる。付ける前の /sync で、Bitwig がまだ見ていない値を読まないように。
      // 起動の直後は Bitwig から値が届く前なので、/sync が既定の値(0 や黒)を送ることがある。そのあと Observer が今の値で送り直すので、最後は揃う。
      feedback = created;

      host.println("BitwigOSC " + BitwigOscDefinition.VERSION + ": listening on " + receivePort
         + ", sending to " + sendHost + ":" + sendPort);
   }

   /**
    * 接続の設定は起動時にしか使えないので、変わったら拡張ごと起動し直す。
    * 以前は読み込み直しを忘れると古い送り先に送り続け、UDP なので何も起きないように見えた。
    */
   private void restartOnChange(final SettableRangedValue receivePort, final SettableStringValue sendHost,
      final SettableRangedValue sendPort)
   {
      final ControllerHost host = getHost();
      final RestartTrigger trigger = new RestartTrigger(() -> {
         host.println("BitwigOSC: connection settings changed, restarting");
         // Observer の呼び出しの中で自分を壊さないよう、次の処理の番に回す。
         host.scheduleTask(host::restart, 0);
      });

      final Consumer<Double> onReceivePort = trigger.watch(receivePort.getRaw());
      receivePort.addRawValueObserver(value -> onReceivePort.accept(value));
      // 接続には前後の空白を除いた値を使うので、比べるのも同じ形にする(空白だけの変更で起動し直さない)。
      final Consumer<String> onSendHost = trigger.watch(sendHost.get().trim());
      sendHost.addValueObserver(value -> onSendHost.accept(value == null ? null : value.trim()));
      final Consumer<Double> onSendPort = trigger.watch(sendPort.getRaw());
      sendPort.addRawValueObserver(value -> onSendPort.accept(value));
   }

   private void onMessage(final OscConnection source, final OscMessage message)
   {
      final String address = message.getAddressPattern();
      if (logIncoming)
         getHost().println("OSC in: " + OscLog.format(address, message.getArguments()));
      if ("/ping".equals(address))
      {
         send("/pong");
         // UDP は届かなくてもエラーにならないので、送ったことだけは残す。
         getHost().println("OSC out: /pong");
         return;
      }
      if (OscCommand.SYNC_ADDRESS.equals(address))
      {
         // 300 通近く(ミキサー 74 + クリップ 216)をまとめて送り、Bitwig の値も読むので、Observer と同じ拡張のスレッドで動かす(起動し直しと同じやり方)。
         getHost().scheduleTask(this::sendAll, 0);
         return;
      }
      final Optional<OscCommand> command = OscCommand.parse(address, BitwigControls.TRACK_COUNT,
         BitwigControls.SCENE_COUNT);
      final OptionalDouble value = OscArgs.firstNumber(message.getArguments());
      if (command.isPresent() && value.isPresent())
      {
         controls.apply(command.get(), value.getAsDouble());
         return;
      }
      // レイアウトのアドレスがずれていても気づけるよう、何もしなかったことをログに残す。
      if (logIncoming)
         getHost().println("OSC ignored: " + address
            + (command.isPresent() ? " (no numeric argument)" : " (unknown address)"));
   }

   /** TouchOSC がレイアウトを開いたときに呼ばれる。Bitwig を先に起動していても、今の状態に揃える。 */
   private void sendAll()
   {
      final Feedback current = feedback;
      if (current == null)
         return;
      controls.sendAll(current);
      getHost().println("BitwigOSC: sent the current state to TouchOSC");
   }

   private void send(final String address, final Object... args)
   {
      if (touchOsc == null)
         return;
      try
      {
         touchOsc.sendMessage(address, args);
         sendFailures.succeeded().ifPresent(getHost()::println);
      }
      // 引数の型が OSC にない場合は RuntimeException。Observer の中から呼ばれるので、Bitwig に投げ返さない。
      catch (final IOException | RuntimeException e)
      {
         sendFailures.failed(address, e).ifPresent(getHost()::errorln);
      }
   }

   private void fail(final String reason)
   {
      getHost().errorln("BitwigOSC: " + reason);
      getHost().showPopupNotification("BitwigOSC: " + reason);
   }

   @Override
   public void exit()
   {
      getHost().println("BitwigOSC exited");
   }

   @Override
   public void flush()
   {
   }
}
