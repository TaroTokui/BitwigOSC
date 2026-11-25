package dev.bitwigosc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
      COLOR_BOX, BOX, LABEL
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
         final boolean sends = kind == Kind.FADER || kind == Kind.BUTTON || kind == Kind.PLAY_BUTTON;
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
    */
   record Layout(String rootScript, List<Page> pages)
   {
      Layout
      {
         Objects.requireNonNull(rootScript);
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

   private static final int COLUMN_X = 20;
   private static final int COLUMN_STEP = 60;
   private static final int COLUMN_WIDTH = 50;

   private static final Color VOLUME = new Color(0.784314, 0.313726, 0, 1);
   private static final Color ARM = new Color(1, 0, 0, 1);
   private static final Color SOLO = new Color(1, 0.977398, 0, 1);
   private static final Color MUTE = new Color(1, 0.45063, 0.0222223, 1);
   /** 起動すると Bitwig のトラックの色で上書きされる。 */
   private static final Color TRACK_COLOR = new Color(0.627451, 0.752941, 0.298039, 1);
   private static final Color PLAY = new Color(0.784314, 0.784314, 0.784314, 1);
   private static final Color PLAY_BACKGROUND = new Color(0.131579, 0.131579, 0.131579, 1);
   private static final Color LOOP = new Color(0.155555, 0.801111, 1, 1);
   private static final Color CLEAR = new Color(1, 0, 0, 0);

   private MixerLayout()
   {
   }

   static Layout layout()
   {
      return new Layout(ROOT_SCRIPT, List.of(
         new Page("1", "Controller", controllerPage()),
         new Page("2", "2", List.of()),
         new Page("3", "3", List.of())));
   }

   private static List<Control> controllerPage()
   {
      final List<Control> controls = new ArrayList<>();
      for (int i = 0; i < TRACK_COUNT; i++)
         controls.addAll(trackColumn(i));
      controls.addAll(transport());
      return controls;
   }

   private static List<Control> trackColumn(final int index)
   {
      final int x = COLUMN_X + COLUMN_STEP * index;
      final String tag = String.valueOf(index + 1);
      return List.of(
         new Control(Kind.COLOR_BOX, "color" + tag, tag, new Frame(x, 20, COLUMN_WIDTH, 10), TRACK_COLOR, "", ""),
         trackControl(Kind.FADER, TrackParam.VOLUME, tag, new Frame(x, 40, COLUMN_WIDTH, 200), VOLUME),
         trackControl(Kind.BUTTON, TrackParam.ARM, tag, new Frame(x, 260, COLUMN_WIDTH, 40), ARM),
         trackControl(Kind.BUTTON, TrackParam.SOLO, tag, new Frame(x, 320, COLUMN_WIDTH, 40), SOLO),
         trackControl(Kind.BUTTON, TrackParam.MUTE, tag, new Frame(x, 380, COLUMN_WIDTH, 40), MUTE));
   }

   private static Control trackControl(final Kind kind, final TrackParam param, final String tag,
      final Frame frame, final Color color)
   {
      return new Control(kind, param.oscName() + tag, tag, frame, color, param.oscName(), "");
   }

   /** 後ろの部品ほど上に描かれるので、下地は再生ボタンの前、文字はループボタンの後に置く。 */
   private static List<Control> transport()
   {
      final String tag = String.valueOf(OscCommand.TRANSPORT_NUMBER);
      return List.of(
         new Control(Kind.BOX, "playBackground", "", new Frame(530, 40, 70, 70), PLAY_BACKGROUND, "", ""),
         transportButton(Kind.PLAY_BUTTON, TransportParam.PLAY, tag, new Frame(537, 50, 50, 50), PLAY),
         transportButton(Kind.BUTTON, TransportParam.LOOP, tag, new Frame(530, 130, 70, 70), LOOP),
         new Control(Kind.LABEL, "loopLabel", tag, new Frame(525, 153, 80, 25), CLEAR, "", "Loop"));
   }

   private static Control transportButton(final Kind kind, final TransportParam param, final String tag, final Frame frame,
      final Color color)
   {
      return new Control(kind, param.oscName(), tag, frame, color, param.oscName(), "");
   }
}
