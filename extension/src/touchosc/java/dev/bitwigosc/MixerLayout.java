package dev.bitwigosc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import dev.bitwigosc.LauncherCommand.Direction;
import dev.bitwigosc.OscCommand.TrackParam;
import dev.bitwigosc.OscCommand.TransportParam;

/**
 * TouchOSC のレイアウトに置く部品の並び。位置と色はここで決める。
 * XML にするのは {@link ToscWriter}。
 */
final class MixerLayout
{
   static final int TRACK_COUNT = BitwigControls.TRACK_COUNT;

   enum Kind
   {
      FADER, BUTTON,
      /** 再生ボタン。押し方はボタンと同じで、形が右向きの三角(以前のレイアウトと同じ)。 */
      PLAY_BUTTON,
      /** ページ 2 のボタン。押している間だけオンで、押すたびに送る。Bitwig からの値は受けない(状態は下の色の箱で見せる)。 */
      MOMENTARY_BUTTON,
      COLOR_BOX, BOX, LABEL,
      /** 文字を Bitwig から受けて出すラベル(ヘッダーの小節の表示)。触れない。 */
      TEXT_LABEL
   }

   record Frame(int x, int y, int w, int h)
   {
   }

   record Color(double r, double g, double b, double a)
   {
   }

   /**
    * 部品 1 つ。
    *
    * @param tag     TouchOSC の tag(アドレスの番号)。空なら tag を持たない部品
    * @param oscName フェーダーとボタンが送るアドレスの名前。ほかの部品は空
    * @param text    ラベルの文字。ほかの部品は空
    */
   record Control(Kind kind, String name, String tag, Frame frame, Color color, String oscName, String text)
   {
      Control
      {
         Objects.requireNonNull(kind);
         Objects.requireNonNull(name);
         Objects.requireNonNull(tag);
         Objects.requireNonNull(frame);
         Objects.requireNonNull(color);
         Objects.requireNonNull(oscName);
         Objects.requireNonNull(text);
      }

      /** TouchOSC が送るアドレス。フェーダーとボタンだけが持つ。 */
      Optional<String> address()
      {
         final boolean sends = kind == Kind.FADER || kind == Kind.BUTTON || kind == Kind.PLAY_BUTTON
            || kind == Kind.MOMENTARY_BUTTON;
         return sends ? Optional.of("/" + tag + "/" + oscName) : Optional.empty();
      }
   }

   /** ページ 1 枚。{@code name} は ID の元、{@code tabLabel} はタブに出る文字。 */
   record Page(String name, String tabLabel, List<Control> controls)
   {
      Page
      {
         controls = List.copyOf(controls);
      }
   }

   /**
    * レイアウト全体。{@code rootScript} はいちばん外の入れ物に置く Lua のスクリプト。
    * TouchOSC はレイアウトを読み込んだときに、その {@code init()} を呼ぶ。
    *
    * @param header いちばん外の入れ物に直に置く部品。ページを切り替えても出たまま({@link MixerLayout#PAGER_FRAME} の上)
    */
   record Layout(String rootScript, List<Control> header, List<Page> pages)
   {
      Layout
      {
         Objects.requireNonNull(rootScript);
         header = List.copyOf(header);
         pages = List.copyOf(pages);
      }
   }

   /**
    * 開いたときに {@link OscCommand#SYNC_ADDRESS} を送り、拡張に今の状態を全部送り返してもらう。
    * Bitwig を先に起動していても、トラックの色や音量が開いた時点のものに揃う。
    */
   static final String ROOT_SCRIPT = """
      function init()
        sendOSC('%s')
      end
      """.formatted(OscCommand.SYNC_ADDRESS);

   /** 画面(いちばん外の入れ物)は 640 × 480。上の 40 をヘッダーにする。 */
   private static final int SCREEN_WIDTH = 640;
   private static final int SCREEN_HEIGHT = 480;
   private static final int HEADER_HEIGHT = 40;
   /** タブの帯の高さ。pager.xml の tabbarSize と同じにする。 */
   private static final int TAB_BAR_HEIGHT = 40;
   /** ページを切り替える入れ物。ヘッダーの下。 */
   static final Frame PAGER_FRAME = new Frame(0, HEADER_HEIGHT, SCREEN_WIDTH, SCREEN_HEIGHT - HEADER_HEIGHT);
   /** ページ 1 枚(pager の中の位置)。タブの帯の下で、部品の座標はこの中の位置。 */
   static final Frame PAGE_FRAME = new Frame(0, TAB_BAR_HEIGHT, SCREEN_WIDTH, PAGER_FRAME.h() - TAB_BAR_HEIGHT);

   /** 列は x = 80 から 60 ずつ(ページ 1 は arm・solo・mute、ページ 2 はシーン、ヘッダーは停止など)。 */
   static final int COLUMN_X = 80;
   static final int COLUMN_STEP = 60;
   static final int COLUMN_WIDTH = 50;
   /**
    * トラックは Bitwig のアレンジ画面と同じく上から下へ、1 本 1 行。行は y = 40 から 40 ずつ(8 本で 360 まで)。
    * 上の 0〜36 と下の 360〜396 は ▲▼ などのボタンの段。ページ 1 と 2 で同じ行にする。
    */
   static final int ROW_Y = 40;
   static final int ROW_STEP = 40;
   static final int ROW_HEIGHT = 36;
   /** 行の頭のトラックの色の帯(縦長)。 */
   static final int TRACK_COLOR_X = 60;
   static final int TRACK_COLOR_WIDTH = 10;
   /** トラックを 1 本ずつずらすボタン。色の帯の列の上と下に置き、ページ 1 と 2 で同じ所にする。 */
   static final Frame SCROLL_UP_FRAME = new Frame(20, 0, COLUMN_WIDTH, ROW_HEIGHT);
   static final Frame SCROLL_DOWN_FRAME = new Frame(20, ROW_Y + ROW_STEP * TRACK_COUNT, COLUMN_WIDTH, ROW_HEIGHT);
   static final Color SCROLL = new Color(0.155555, 0.801111, 1, 1);
   /** ラベルの下地(透明)。文字だけが見える。 */
   static final Color CLEAR = new Color(1, 0, 0, 0);

   private static final Color VOLUME = new Color(0.784314, 0.313726, 0, 1);
   private static final Color ARM = new Color(1, 0, 0, 1);
   private static final Color SOLO = new Color(1, 0.977398, 0, 1);
   /** Bitwig のテーマの橙(mute と、再生中の再生ボタン)。 */
   static final Color ORANGE = new Color(1, 0.45063, 0.0222223, 1);
   private static final Color MUTE = ORANGE;
   /** 起動すると Bitwig のトラックの色で上書きされる。 */
   private static final Color TRACK_COLOR = new Color(0.627451, 0.752941, 0.298039, 1);
   /** 再生ボタンは Bitwig の再生の状態を受け、再生中だけこの色で塗られる(止まっていると下の灰色が見える)。 */
   private static final Color PLAY = ORANGE;
   /** 停止ボタンを押している間の塗りと枠。下の灰色より明るくして、押したのが分かるようにする。 */
   private static final Color STOP = new Color(0.784314, 0.784314, 0.784314, 1);
   /** 再生と停止の下地。Bitwig のトランスポートのボタンに近い灰色で、白い ▶ ■ が見える。 */
   private static final Color TRANSPORT_GREY = new Color(0.35, 0.35, 0.35, 1);
   private static final Color LOOP = new Color(0.155555, 0.801111, 1, 1);
   /** 音量のフェーダーの幅。mute の右からページの右端の 20 手前まで。 */
   private static final int VOLUME_WIDTH = 360;
   /** ミキサーのスクロールのボタンの tag。{@code /mixer/scrollUp} などを送る。 */
   private static final String MIXER_TAG = OscCommand.MIXER_PREFIX;

   private MixerLayout()
   {
   }

   static Layout layout()
   {
      return new Layout(ROOT_SCRIPT, header(), List.of(
         new Page("1", "Controller", controllerPage()),
         new Page("2", "Clips", LauncherPage.controls()),
         new Page("3", "3", List.of())));
   }

   private static List<Control> controllerPage()
   {
      final List<Control> controls = new ArrayList<>();
      for (int i = 0; i < TRACK_COUNT; i++)
         controls.addAll(trackRow(i));
      // 名前はページ 2 の scrollUp と重ならないようにする(ID は名前から作るため)。
      controls.addAll(labeled(Kind.MOMENTARY_BUTTON, "mixerScrollUp", MIXER_TAG, Direction.UP.oscName(),
         SCROLL_UP_FRAME, SCROLL, "▲"));
      controls.addAll(labeled(Kind.MOMENTARY_BUTTON, "mixerScrollDown", MIXER_TAG, Direction.DOWN.oscName(),
         SCROLL_DOWN_FRAME, SCROLL, "▼"));
      return controls;
   }

   /** トラック {@code track}(0 始まり)の行の y。 */
   static int rowY(final int track)
   {
      return ROW_Y + ROW_STEP * track;
   }

   /** 行の頭の、トラックの色の帯。 */
   static Frame trackColorFrame(final int track)
   {
      return new Frame(TRACK_COLOR_X, rowY(track), TRACK_COLOR_WIDTH, ROW_HEIGHT);
   }

   /** 行 {@code track} の、列 {@code column}(0 始まり)のマス。 */
   static Frame cell(final int column, final int track)
   {
      return new Frame(COLUMN_X + COLUMN_STEP * column, rowY(track), COLUMN_WIDTH, ROW_HEIGHT);
   }

   /**
    * ボタンと、その上に重ねる記号のラベル。ラベルの名前はボタンの名前 + "Label"。
    * ラベルは触れない部品なので、上に重ねてもボタンは押せる(Loop と同じ)。
    */
   static List<Control> labeled(final Kind kind, final String name, final String tag, final String oscName,
      final Frame frame, final Color color, final String glyph)
   {
      return List.of(new Control(kind, name, tag, frame, color, oscName, ""),
         new Control(Kind.LABEL, name + "Label", "", frame, CLEAR, "", glyph));
   }

   /** トラック 1 本ぶんの行。左から 色・arm・solo・mute・横向きの音量。 */
   private static List<Control> trackRow(final int index)
   {
      final String tag = String.valueOf(index + 1);
      final Frame volume = new Frame(COLUMN_X + 3 * COLUMN_STEP, rowY(index), VOLUME_WIDTH, ROW_HEIGHT);
      return List.of(
         new Control(Kind.COLOR_BOX, "color" + tag, tag, trackColorFrame(index), TRACK_COLOR, "", ""),
         trackControl(Kind.BUTTON, TrackParam.ARM, tag, cell(0, index), ARM),
         trackControl(Kind.BUTTON, TrackParam.SOLO, tag, cell(1, index), SOLO),
         trackControl(Kind.BUTTON, TrackParam.MUTE, tag, cell(2, index), MUTE),
         trackControl(Kind.FADER, TrackParam.VOLUME, tag, volume, VOLUME));
   }

   private static Control trackControl(final Kind kind, final TrackParam param, final String tag,
      final Frame frame, final Color color)
   {
      return new Control(kind, param.oscName() + tag, tag, frame, color, param.oscName(), "");
   }

   /**
    * 画面の上の、どのページにも出る部品。再生・停止・ループ・小節.拍の表示を左から並べる
    * (再生は ▲ の列、停止は 1 列ぶん、ループと小節の表示は 2 列ぶんの幅)。
    * 後ろの部品ほど上に描かれるので、下地はボタンの前、文字はボタンの後に置く。
    */
   static List<Control> header()
   {
      final String tag = String.valueOf(OscCommand.TRANSPORT_NUMBER);
      final int y = 3;
      final int h = HEADER_HEIGHT - 2 * y;
      final int wide = COLUMN_STEP + COLUMN_WIDTH;
      final Frame play = new Frame(20, y, COLUMN_WIDTH, h);
      final Frame stop = new Frame(COLUMN_X, y, COLUMN_WIDTH, h);
      final Frame loop = new Frame(COLUMN_X + COLUMN_STEP, y, wide, h);
      // 再生: 灰色の箱の上に、オンのときだけ橙に塗られるボタンと、白い ▶ を重ねる。
      final List<Control> controls = new ArrayList<>(List.of(greyBox("playBackground", play)));
      controls.addAll(labeled(Kind.PLAY_BUTTON, TransportParam.PLAY.oscName(), tag, TransportParam.PLAY.oscName(),
         play, PLAY, "▶"));
      // 停止は押したときだけ止める操作で、状態がないのでモーメンタリ(受信しない)。下地は再生と同じ灰色。
      controls.add(greyBox("stopBackground", stop));
      controls.addAll(labeled(Kind.MOMENTARY_BUTTON, OscCommand.STOP_NAME, tag, OscCommand.STOP_NAME,
         stop, STOP, "■"));
      controls.add(transportButton(Kind.BUTTON, TransportParam.LOOP, tag, loop, LOOP));
      controls.add(new Control(Kind.LABEL, "loopLabel", tag, loop, CLEAR, "", "Loop"));
      controls.add(new Control(Kind.TEXT_LABEL, OscCommand.POSITION_NAME, tag,
         new Frame(COLUMN_X + 3 * COLUMN_STEP, y, wide, h), CLEAR, OscCommand.POSITION_NAME, "1.1"));
      return List.copyOf(controls);
   }

   /** 再生と停止の下の、灰色の箱(受信しない)。 */
   private static Control greyBox(final String name, final Frame frame)
   {
      return new Control(Kind.BOX, name, "", frame, TRANSPORT_GREY, "", "");
   }

   private static Control transportButton(final Kind kind, final TransportParam param, final String tag, final Frame frame,
      final Color color)
   {
      return new Control(kind, param.oscName(), tag, frame, color, param.oscName(), "");
   }
}
