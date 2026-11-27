package dev.bitwigosc;

/**
 * ページ 2 のマスの色を、クリップの状態から決める。TouchOSC の箱は色しか受けないので、状態は明るさで見せる。
 *
 * <ul>
 * <li>録音中: 赤。録音の待機中: 暗い赤</li>
 * <li>クリップなし: 黒(背景と同じで見えない。マスの枠はボタンが描く)</li>
 * <li>再生中: クリップの色そのまま。再生の待機中: やや暗く。止まっている: 暗く</li>
 * </ul>
 *
 * Bitwig の API に触らないので、{@code SlotColorTest} で確かめる。
 */
final class SlotColor
{
   /** Bitwig から読んだマスの状態。色はクリップの色(0〜1)。 */
   record State(boolean hasContent, boolean playing, boolean playbackQueued, boolean recording,
      boolean recordingQueued, float red, float green, float blue)
   {
   }

   record Rgb(float red, float green, float blue)
   {
   }

   static final Rgb EMPTY = new Rgb(0, 0, 0);
   static final Rgb RECORDING = new Rgb(1, 0, 0);
   static final Rgb RECORDING_QUEUED = new Rgb(0.5f, 0, 0);
   static final float STOPPED_BRIGHTNESS = 0.35f;
   static final float QUEUED_BRIGHTNESS = 0.7f;

   private SlotColor()
   {
   }

   static Rgb of(final State state)
   {
      // 録音は空のマスにも起きるので、クリップがあるかどうかより先に見る。
      if (state.recording())
         return RECORDING;
      if (state.recordingQueued())
         return RECORDING_QUEUED;
      if (!state.hasContent())
         return EMPTY;
      // 再生中のクリップを押し直すと、再生中のまま待機にもなる。そのときは再生中として見せる。
      if (state.playing())
         return scaled(state, 1);
      if (state.playbackQueued())
         return scaled(state, QUEUED_BRIGHTNESS);
      return scaled(state, STOPPED_BRIGHTNESS);
   }

   private static Rgb scaled(final State state, final float brightness)
   {
      return new Rgb(state.red() * brightness, state.green() * brightness, state.blue() * brightness);
   }
}
