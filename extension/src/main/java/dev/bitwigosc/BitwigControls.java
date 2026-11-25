package dev.bitwigosc;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.Transport;

import dev.bitwigosc.OscCommand.TrackCommand;
import dev.bitwigosc.OscCommand.TrackParam;
import dev.bitwigosc.OscCommand.TransportCommand;
import dev.bitwigosc.OscCommand.TransportParam;

/**
 * 解釈した OSC の操作を、Bitwig のトラックとトランスポートに書き込む。
 * Bitwig の API に触るのはここだけにし、アドレスの解釈(OscCommand)と値の変換(OscArgs)は単体テストする。
 */
final class BitwigControls
{
   /** TouchOSC のレイアウト(tag 1〜8)に合わせる。 */
   static final int TRACK_COUNT = 8;

   private final TrackBank tracks;
   private final Transport transport;

   /** Bitwig の決まりで、バンクやトランスポートは init() の中でしか作れない。 */
   BitwigControls(final ControllerHost host)
   {
      // FX トラックを除いた、ミキサーの左から順のトラック。バンクは動かさず、先頭の 8 本に固定する。
      tracks = host.createMainTrackBank(TRACK_COUNT, 0, 0);
      transport = host.createTransport();
   }

   void apply(final OscCommand command, final double value)
   {
      switch (command)
      {
         case TrackCommand t -> applyToTrack(tracks.getItemAt(t.index()), t.param(), value);
         case TransportCommand t -> applyToTransport(t.param(), value);
      }
   }

   // トラックがない位置(曲のトラックが 8 本より少ない)への書き込みは、Bitwig が無視する。
   private static void applyToTrack(final Track track, final TrackParam param, final double value)
   {
      switch (param)
      {
         case VOLUME -> track.volume().set(OscArgs.clampUnit(value));
         case PAN -> track.pan().set(OscArgs.clampUnit(value));
         // TouchOSC のボタンはトグルで、押すたびに新しい状態を送ってくる。切り替えではなく、その状態にする。
         case MUTE -> track.mute().set(OscArgs.isOn(value));
         case SOLO -> track.solo().set(OscArgs.isOn(value));
         case ARM -> track.arm().set(OscArgs.isOn(value));
         case ACTIVE -> track.isActivated().set(OscArgs.isOn(value));
      }
   }

   /**
    * Bitwig 側で値が変わったら TouchOSC に送る。Observer も init() の中でしか登録できない。
    * 登録すると Bitwig はその値を見るようになり(markInterested と同じ)、起動直後に今の値で一度呼ばれる。
    * 値を見るようになるので、{@link #sendAll} が今の値を読める。
    */
   void observe(final Feedback feedback)
   {
      for (int i = 0; i < TRACK_COUNT; i++)
      {
         final int index = i;
         final Track track = tracks.getItemAt(index);
         for (final TrackParam param : TrackParam.values())
            observeTrack(track, new TrackCommand(index, param), feedback);
         // 色はトラックがあるかどうかでも変わる(ない位置は黒)。どちらが変わっても、両方を見て送る。
         track.color().addValueObserver((red, green, blue) -> sendColor(index, feedback));
         track.exists().addValueObserver(exists -> sendColor(index, feedback));
      }
      for (final TransportParam param : TransportParam.values())
         observeTransport(new TransportCommand(param), feedback);
   }

   /**
    * 今の状態を全部送る。TouchOSC があとから開いたとき({@link OscCommand#SYNC_ADDRESS})に使う。
    * {@link #observe} を先に呼んでおくこと(Bitwig が値を見ていないと、今の値を読めない)。
    */
   void sendAll(final Feedback feedback)
   {
      for (int i = 0; i < TRACK_COUNT; i++)
      {
         final Track track = tracks.getItemAt(i);
         for (final TrackParam param : TrackParam.values())
            sendTrack(track, new TrackCommand(i, param), feedback);
         sendColor(i, feedback);
      }
      for (final TransportParam param : TransportParam.values())
         sendTransport(new TransportCommand(param), feedback);
   }

   private void sendColor(final int index, final Feedback feedback)
   {
      final Track track = tracks.getItemAt(index);
      final SettableColorValue color = track.color();
      feedback.trackColor(index, track.exists().get(), color.red(), color.green(), color.blue());
   }

   private static void observeTrack(final Track track, final TrackCommand command, final Feedback feedback)
   {
      switch (command.param())
      {
         case VOLUME -> track.volume().value().addValueObserver(value -> feedback.value(command, value));
         case PAN -> track.pan().value().addValueObserver(value -> feedback.value(command, value));
         case MUTE -> track.mute().addValueObserver(on -> feedback.flag(command, on));
         case SOLO -> track.solo().addValueObserver(on -> feedback.flag(command, on));
         case ARM -> track.arm().addValueObserver(on -> feedback.flag(command, on));
         case ACTIVE -> track.isActivated().addValueObserver(on -> feedback.flag(command, on));
      }
   }

   private static void sendTrack(final Track track, final TrackCommand command, final Feedback feedback)
   {
      switch (command.param())
      {
         case VOLUME -> feedback.value(command, track.volume().value().get());
         case PAN -> feedback.value(command, track.pan().value().get());
         case MUTE -> feedback.flag(command, track.mute().get());
         case SOLO -> feedback.flag(command, track.solo().get());
         case ARM -> feedback.flag(command, track.arm().get());
         case ACTIVE -> feedback.flag(command, track.isActivated().get());
      }
   }

   private void observeTransport(final TransportCommand command, final Feedback feedback)
   {
      switch (command.param())
      {
         case PLAY -> transport.isPlaying().addValueObserver(on -> feedback.flag(command, on));
         case LOOP -> transport.isArrangerLoopEnabled().addValueObserver(on -> feedback.flag(command, on));
      }
   }

   private void sendTransport(final TransportCommand command, final Feedback feedback)
   {
      switch (command.param())
      {
         case PLAY -> feedback.flag(command, transport.isPlaying().get());
         case LOOP -> feedback.flag(command, transport.isArrangerLoopEnabled().get());
      }
   }

   private void applyToTransport(final TransportParam param, final double value)
   {
      switch (param)
      {
         case PLAY -> transport.isPlaying().set(OscArgs.isOn(value));
         case LOOP -> transport.isArrangerLoopEnabled().set(OscArgs.isOn(value));
      }
   }
}
