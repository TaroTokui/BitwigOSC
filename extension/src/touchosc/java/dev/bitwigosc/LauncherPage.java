package dev.bitwigosc;

import java.util.ArrayList;
import java.util.List;

import dev.bitwigosc.LauncherCommand.Direction;
import dev.bitwigosc.MixerLayout.Color;
import dev.bitwigosc.MixerLayout.Control;
import dev.bitwigosc.MixerLayout.Frame;
import dev.bitwigosc.MixerLayout.Kind;

/**
 * ページ 2(クリップ)の部品。8 トラック × 8 シーンのマスと、シーンの起動・トラックの停止・範囲のスクロール。
 * Bitwig のアレンジ画面のクリップランチャーと同じく、トラックが行(上から下)、シーンが列(左から右)。
 * 再生・ループ・小節の表示は、どのページにも出るヘッダー({@link MixerLayout#header()})にある。
 *
 * <pre>
 *  x:    20  60  80 ... 500    560
 *  y=0  [▲]     [▶ シーン] ...          シーンの起動(▲ はトラックの範囲を上へ。ページ 1 と同じ所)
 *  y=40     [色][マ] ... [マ]   [■]   トラック 1 の行(■ はトラックの停止)
 *   ...                                 (40 ずつ下へ、8 行)
 * y=320     [色][マ] ... [マ]   [■]
 * y=360 [▼]    [◀]  ...  [▶]          ▼ はトラック、◀ ▶ はシーンの範囲(ページは高さ 400)
 * </pre>
 *
 * マスは「色の箱の上に、枠だけのボタン」を重ねる。色はクリップの状態(Bitwig から)、ボタンは押すだけ。
 * ラベルは触れない部品なので、上に重ねてもボタンは押せる(ヘッダーの Loop と同じ)。
 */
final class LauncherPage
{
   static final int SCENE_COUNT = BitwigControls.SCENE_COUNT;

   /** シーンの起動のボタンは、マスの上の段。 */
   private static final int SCENE_Y = 0;
   /** マスの下の段(▼ ◀ ▶)。 */
   private static final int BOTTOM_Y = MixerLayout.rowY(MixerLayout.TRACK_COUNT);
   /** 停止は行の最後のマスの次の列(マスどうしと同じ 10 の隙間)。 */
   private static final int STOP_COLUMN = SCENE_COUNT;

   private static final Color SLOT = new Color(0.6, 0.6, 0.6, 1);
   private static final Color STOP = new Color(0.6, 0.6, 0.6, 1);
   private static final Color SCENE = new Color(0.35, 0.8, 0.35, 1);
   /** 箱の最初の色。Bitwig から色が届くまでは黒(背景と同じ)。 */
   private static final Color UNSET = new Color(0, 0, 0, 1);

   private LauncherPage()
   {
   }

   static List<Control> controls()
   {
      final List<Control> controls = new ArrayList<>();
      for (int t = 0; t < MixerLayout.TRACK_COUNT; t++)
         controls.addAll(row(t));
      for (int s = 0; s < SCENE_COUNT; s++)
         controls.addAll(MixerLayout.labeled(Kind.MOMENTARY_BUTTON, "scene" + (s + 1),
            LauncherCommand.SCENE_PREFIX + "/" + (s + 1), "launch", edge(s, SCENE_Y), SCENE, "▶"));
      controls.addAll(scroll(Direction.UP, MixerLayout.SCROLL_UP_FRAME, "▲"));
      controls.addAll(scroll(Direction.DOWN, MixerLayout.SCROLL_DOWN_FRAME, "▼"));
      controls.addAll(scroll(Direction.LEFT, edge(0, BOTTOM_Y), "◀"));
      controls.addAll(scroll(Direction.RIGHT, edge(SCENE_COUNT - 1, BOTTOM_Y), "▶"));
      return List.copyOf(controls);
   }

   /** トラック 1 本ぶんの行(色・マス 8 つ・停止)。{@code t} は 0 始まり。 */
   private static List<Control> row(final int t)
   {
      final String n = String.valueOf(t + 1);
      final String trackTag = LauncherCommand.CLIP_PREFIX + "/" + n;
      final List<Control> controls = new ArrayList<>();
      controls.add(new Control(Kind.COLOR_BOX, "clipTrackColor" + n, trackTag, MixerLayout.trackColorFrame(t),
         UNSET, "", ""));
      for (int s = 0; s < SCENE_COUNT; s++)
      {
         final String cell = n + "_" + (s + 1);
         final String slotTag = trackTag + "/" + (s + 1);
         final Frame frame = MixerLayout.cell(s, t);
         // 後ろの部品ほど上に描かれる。色の箱を先に置き、枠だけのボタンを重ねる。
         controls.add(new Control(Kind.COLOR_BOX, "slotColor" + cell, slotTag, frame, UNSET, "", ""));
         controls.add(new Control(Kind.MOMENTARY_BUTTON, "launch" + cell, slotTag, frame, SLOT, "launch", ""));
      }
      controls.addAll(MixerLayout.labeled(Kind.MOMENTARY_BUTTON, "stop" + n, trackTag, "stop",
         MixerLayout.cell(STOP_COLUMN, t), STOP, "■"));
      return controls;
   }

   private static List<Control> scroll(final Direction direction, final Frame frame, final String glyph)
   {
      return MixerLayout.labeled(Kind.MOMENTARY_BUTTON, direction.oscName(), LauncherCommand.CLIP_PREFIX,
         direction.oscName(), frame, MixerLayout.SCROLL, glyph);
   }

   /** マスの上か下の段の、シーンの列 {@code scene}(0 始まり)の所。 */
   private static Frame edge(final int scene, final int y)
   {
      final Frame column = MixerLayout.cell(scene, 0);
      return new Frame(column.x(), y, column.w(), column.h());
   }
}
