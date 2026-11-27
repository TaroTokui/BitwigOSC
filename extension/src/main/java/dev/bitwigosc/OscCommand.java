package dev.bitwigosc;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TouchOSC から届いたアドレスが、Bitwig の何の操作にあたるか。
 * 形は {@code /{番号}/{操作}}。
 * 番号は TouchOSC のレイアウトの tag で、トラックは 1 から数え、0 はトランスポートに使う。
 * ページ 2(クリップ)の {@code /clip/...}・{@code /scene/...} は {@link LauncherCommand}。
 * ページ 1 のトラックを 1 本ずつずらす {@code /mixer/scrollUp}・{@code /mixer/scrollDown} は {@link MixerScroll}。
 * 再生を止める {@code /0/stop} は {@link TransportStop}。
 */
sealed interface OscCommand
   permits OscCommand.TrackCommand, OscCommand.TransportCommand, OscCommand.TransportStop, OscCommand.MixerScroll,
   LauncherCommand
{
   /** {@code index} はバンクの中の位置(0 始まり)。アドレスの番号から 1 を引いたもの。 */
   record TrackCommand(int index, TrackParam param) implements OscCommand
   {
      @Override
      public String address()
      {
         return trackAddress(index, param.oscName());
      }
   }

   record TransportCommand(TransportParam param) implements OscCommand
   {
      @Override
      public String address()
      {
         return "/" + TRANSPORT_NUMBER + "/" + param.oscName();
      }
   }

   /**
    * {@code /0/stop}。再生を止める。状態ではなく操作なので送り返さず、{@link TransportParam} に入れない
    * (TransportParam は全部を {@code sendAll} で送り返す)。
    */
   record TransportStop() implements OscCommand
   {
      @Override
      public String address()
      {
         return "/" + TRANSPORT_NUMBER + "/" + STOP_NAME;
      }
   }

   /**
    * ミキサー(ページ 1)のトラックを 1 本ずらす。トラックは上から下に並ぶので上下だけ(ミキサーにはシーンがない)。
    * ページ 2 と同じバンクなので、ページ 2 の範囲と Bitwig の枠も一緒に動く。
    */
   record MixerScroll(LauncherCommand.Direction direction) implements OscCommand
   {
      public MixerScroll
      {
         if (direction != LauncherCommand.Direction.UP && direction != LauncherCommand.Direction.DOWN)
            throw new IllegalArgumentException("mixer scrolls only up or down: " + direction);
      }

      @Override
      public String address()
      {
         return "/" + MIXER_PREFIX + "/" + direction.oscName();
      }
   }

   /** 送り返すときのアドレス。{@link #parse} の逆で、{@code parse(c.address())} は {@code c} に戻る。 */
   String address();

   /** トラックの {@code index}(0 始まり)と操作の名前から、1 始まりのアドレスを作る。色など送り返す専用の名前にも使う。 */
   static String trackAddress(final int index, final String name)
   {
      return "/" + (index + 1) + "/" + name;
   }

   enum TrackParam
   {
      VOLUME("volume"), PAN("pan"), MUTE("mute"), SOLO("solo"), ARM("arm"), ACTIVE("active");

      private final String oscName;

      TrackParam(final String oscName)
      {
         this.oscName = oscName;
      }

      String oscName()
      {
         return oscName;
      }

      static Optional<TrackParam> fromOscName(final String name)
      {
         return Arrays.stream(values()).filter(p -> p.oscName.equals(name)).findFirst();
      }
   }

   enum TransportParam
   {
      PLAY("playState"), LOOP("loopEnabled");

      private final String oscName;

      TransportParam(final String oscName)
      {
         this.oscName = oscName;
      }

      String oscName()
      {
         return oscName;
      }

      static Optional<TransportParam> fromOscName(final String name)
      {
         return Arrays.stream(values()).filter(p -> p.oscName.equals(name)).findFirst();
      }
   }

   int TRANSPORT_NUMBER = 0;

   /**
    * 小節と拍の表示({@code 12.3} など)。送り返すだけの名前で、TouchOSC からは届かない({@link #parse} は空を返す)。
    * 表示はどのページにも出るヘッダーにあるので、トランスポートの番号で送る。
    */
   String POSITION_NAME = "position";
   String POSITION_ADDRESS = "/" + TRANSPORT_NUMBER + "/" + POSITION_NAME;

   /** 停止のボタンの名前。{@code /0/stop}。 */
   String STOP_NAME = "stop";

   /** ページ 1 のスクロールのボタンの tag。 */
   String MIXER_PREFIX = "mixer";

   /**
    * TouchOSC がレイアウトを開いたときに送るアドレス(レイアウトのスクリプトが送る)。操作ではないので {@link #parse} は受けない。
    * 拡張はこれを受けたら、今の状態を全部送り返す。Bitwig を先に起動していても、開いた時点の色や音量が揃う。
    */
   String SYNC_ADDRESS = "/sync";

   // 番号は 4 桁まで。それより長いものはバンクに入らないので、int に直す前に弾く。
   Pattern ADDRESS = Pattern.compile("^/(\\d{1,4})/([A-Za-z]+)$");
   Pattern MIXER_SCROLL = Pattern.compile("^/mixer/(scrollUp|scrollDown)$");

   /**
    * 決まりの外のアドレスは空を返す(届いたものをすべて受けるので、例外にはしない)。
    * {@code trackCount} はミキサーとページ 2 のトラックの数、{@code sceneCount} はページ 2 のシーンの数。
    */
   static Optional<OscCommand> parse(final String address, final int trackCount, final int sceneCount)
   {
      if (address == null)
         return Optional.empty();
      final Optional<LauncherCommand> launcher = LauncherCommand.parse(address, trackCount, sceneCount);
      if (launcher.isPresent())
         return Optional.of(launcher.get());
      final Matcher scroll = MIXER_SCROLL.matcher(address);
      if (scroll.matches())
         return LauncherCommand.Direction.fromOscName(scroll.group(1)).map(MixerScroll::new);
      final Matcher m = ADDRESS.matcher(address);
      if (!m.matches())
         return Optional.empty();

      final int number = Integer.parseInt(m.group(1));
      final String name = m.group(2);
      if (number == TRANSPORT_NUMBER && STOP_NAME.equals(name))
         return Optional.of(new TransportStop());
      if (number == TRANSPORT_NUMBER)
         return TransportParam.fromOscName(name).map(TransportCommand::new);
      if (number > trackCount)
         return Optional.empty();
      return TrackParam.fromOscName(name).map(p -> new TrackCommand(number - 1, p));
   }
}
