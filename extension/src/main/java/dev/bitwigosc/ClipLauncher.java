package dev.bitwigosc;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.SceneBank;
import com.bitwig.extension.controller.api.SettableColorValue;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

import dev.bitwigosc.LauncherCommand.Direction;
import dev.bitwigosc.LauncherCommand.LaunchScene;
import dev.bitwigosc.LauncherCommand.LaunchSlot;
import dev.bitwigosc.LauncherCommand.Scroll;
import dev.bitwigosc.LauncherCommand.StopTrack;

/**
 * ページ 2(クリップ)の操作を Bitwig のクリップランチャーに書き込み、マスの状態を送り返す。
 * バンクはミキサー({@link BitwigControls})と同じものを受け取るので、トラックの範囲を上下に動かすとページ 1 のトラックも一緒に変わる。
 */
final class ClipLauncher
{
   private final int trackCount;
   private final int sceneCount;
   private final TrackBank tracks;
   private final SceneBank scenes;
   private final SlotColorCache sentColors;

   /**
    * {@code tracks} は {@code sceneCount} 段のシーンを持たせて作ったバンク({@link BitwigControls} の構築子で作る)。
    * シーンの数を渡して作ると、各トラックのクリップのバンクもその大きさになり、シーンのバンクと一緒に動く。
    */
   ClipLauncher(final TrackBank tracks, final int trackCount, final int sceneCount)
   {
      this.trackCount = trackCount;
      this.sceneCount = sceneCount;
      sentColors = new SlotColorCache(trackCount, sceneCount);
      this.tracks = tracks;
      scenes = tracks.sceneBank();
      // 今 TouchOSC で触れる範囲を、Bitwig のクリップランチャーに枠で出す。
      tracks.setShouldShowClipLauncherFeedback(true);
   }

   void apply(final LauncherCommand command, final double value)
   {
      // ボタンはモーメンタリで、離したときにも 0 を送ってくる。押したときだけ動かす。
      if (!OscArgs.isOn(value))
         return;
      // 範囲の外(曲のトラックやシーンが足りない位置)への操作は、Bitwig が無視する。
      switch (command)
      {
         case LaunchSlot c -> slot(c.track(), c.scene()).launch();
         case StopTrack c -> tracks.getItemAt(c.track()).stop();
         case LaunchScene c -> scenes.getItemAt(c.scene()).launch();
         case Scroll c -> scroll(c.direction());
      }
   }

   private void scroll(final Direction direction)
   {
      switch (direction)
      {
         // 1 つずつ動かす(ページ単位だと 8 つ先へ飛び、途中のトラックやシーンを見られない)。
         // トラックは上から下、シーンは左から右に並ぶ(Bitwig のアレンジ画面と同じ)。
         case UP -> tracks.scrollBackwards();
         case DOWN -> tracks.scrollForwards();
         case LEFT -> scenes.scrollBackwards();
         case RIGHT -> scenes.scrollForwards();
      }
   }

   /**
    * マスの状態が変わったら送る。範囲を動かしたときも、バンクの中身が入れ替わって Observer が呼ばれる。
    * 状態はいくつかの値から決まるので、どれが変わっても全部を読み直す。色が前と同じなら送らない({@link SlotColorCache})。
    */
   void observe(final Feedback feedback)
   {
      for (int t = 0; t < trackCount; t++)
      {
         final int track = t;
         final Track item = tracks.getItemAt(track);
         item.color().addValueObserver((red, green, blue) -> sendTrackColor(track, feedback));
         item.exists().addValueObserver(exists -> sendTrackColor(track, feedback));
         for (int s = 0; s < sceneCount; s++)
         {
            final int scene = s;
            final ClipLauncherSlot slot = slot(track, scene);
            final Runnable send = () -> sendSlot(track, scene, feedback);
            slot.hasContent().addValueObserver(v -> send.run());
            slot.isPlaying().addValueObserver(v -> send.run());
            slot.isPlaybackQueued().addValueObserver(v -> send.run());
            slot.isRecording().addValueObserver(v -> send.run());
            slot.isRecordingQueued().addValueObserver(v -> send.run());
            slot.color().addValueObserver((red, green, blue) -> send.run());
         }
      }
   }

   /** 今の状態を全部送る。{@link #observe} を先に呼んでおくこと。 */
   void sendAll(final Feedback feedback)
   {
      // TouchOSC があとから開いたなら、前に送った色は届いていない。覚えた色を忘れて、全部送る。
      sentColors.clear();
      for (int t = 0; t < trackCount; t++)
      {
         sendTrackColor(t, feedback);
         for (int s = 0; s < sceneCount; s++)
            sendSlot(t, s, feedback);
      }
   }

   private ClipLauncherSlot slot(final int track, final int scene)
   {
      return tracks.getItemAt(track).clipLauncherSlotBank().getItemAt(scene);
   }

   private void sendTrackColor(final int track, final Feedback feedback)
   {
      final Track item = tracks.getItemAt(track);
      final SettableColorValue color = item.color();
      feedback.launcherTrackColor(track, item.exists().get(), color.red(), color.green(), color.blue());
   }

   private void sendSlot(final int track, final int scene, final Feedback feedback)
   {
      final ClipLauncherSlot slot = slot(track, scene);
      final SettableColorValue color = slot.color();
      final SlotColor.State state = new SlotColor.State(slot.hasContent().get(), slot.isPlaying().get(),
         slot.isPlaybackQueued().get(), slot.isRecording().get(), slot.isRecordingQueued().get(),
         color.red(), color.green(), color.blue());
      final SlotColor.Rgb rgb = SlotColor.of(state);
      if (sentColors.update(track, scene, rgb))
         feedback.slotColor(track, scene, rgb);
   }
}
