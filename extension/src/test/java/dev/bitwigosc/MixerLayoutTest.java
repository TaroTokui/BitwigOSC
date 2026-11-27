package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import dev.bitwigosc.MixerLayout.Control;
import dev.bitwigosc.MixerLayout.Frame;
import dev.bitwigosc.MixerLayout.Kind;
import dev.bitwigosc.MixerLayout.Page;

class MixerLayoutTest
{
   private final MixerLayout.Layout layout = MixerLayout.layout();
   private final List<Page> pages = layout.pages();
   /** 画面の上の、どのページにも出る部品(再生・ループ・小節の表示)。 */
   private final List<Control> header = layout.header();
   private final List<Control> controls = Stream.concat(header.stream(),
      pages.stream().flatMap(p -> p.controls().stream())).toList();
   /** ページ 1(ミキサー)。ページ 2 は LauncherPageTest で見る。 */
   private final List<Control> mixer = pages.get(0).controls();

   @Test
   void keepsThePagesOfThePreviousLayoutWithClipsOnTheSecond()
   {
      // ページの名前は ID の元なので変えない。タブの文字だけ「Clips」にする。
      assertEquals(List.of("1", "2", "3"), pages.stream().map(Page::name).toList());
      assertEquals(List.of("Controller", "Clips", "3"), pages.stream().map(Page::tabLabel).toList());
      assertTrue(pages.get(2).controls().isEmpty());
   }

   @Test
   void putsThePagesBelowTheHeader()
   {
      // 画面は 640 × 480。上の 40 がヘッダー、その下をタブ(40)とページで分ける。
      assertEquals(new Frame(0, 40, 640, 440), MixerLayout.PAGER_FRAME);
      assertEquals(new Frame(0, 40, 640, 400), MixerLayout.PAGE_FRAME);
   }

   @Test
   void hasTheTrackControlsAndTheTrackScrollButtonsButNoTransport()
   {
      final Map<Kind, Long> counts = mixer.stream()
         .collect(Collectors.groupingBy(Control::kind, Collectors.counting()));

      assertEquals(Map.of(Kind.FADER, 8L, Kind.BUTTON, 24L, Kind.COLOR_BOX, 8L, Kind.MOMENTARY_BUTTON, 2L,
         Kind.LABEL, 2L), counts);
   }

   @Test
   void sendsTheTrackAddressesAndTrackScrolling()
   {
      // ping ボタンは置かない(表示は Bitwig 側の変化か iPad の操作で揃う)。再生とループはヘッダーにある。
      final Set<String> expected = new HashSet<>(Set.of("/mixer/scrollUp", "/mixer/scrollDown"));
      for (int track = 1; track <= 8; track++)
         for (final String name : List.of("volume", "arm", "solo", "mute"))
            expected.add("/" + track + "/" + name);

      final List<String> sent = mixer.stream().map(Control::address).flatMap(Optional::stream).toList();

      assertEquals(expected, Set.copyOf(sent));
      assertEquals(expected.size(), sent.size());
   }

   @Test
   void everySentAddressMeansTheSameToTheExtension()
   {
      for (final Control control : controls)
      {
         control.address().ifPresent(address -> {
            final Optional<OscCommand> command = OscCommand.parse(address, MixerLayout.TRACK_COUNT,
               LauncherPage.SCENE_COUNT);
            assertTrue(command.isPresent(), address);
            assertEquals(address, command.get().address());
         });
      }
   }

   @Test
   void placesOneRowPerTrackFortyApartLikeTheClipGrid()
   {
      // Bitwig のアレンジ画面と同じく、トラックは上から下へ。1 行は 色・arm・solo・mute・横向きの音量。
      final Map<String, Control> byName = byName(mixer);
      for (int i = 0; i < 8; i++)
      {
         final int y = 40 + 40 * i;
         final String n = String.valueOf(i + 1);
         assertEquals(new Frame(60, y, 10, 36), byName.get("color" + n).frame());
         assertEquals(new Frame(80, y, 50, 36), byName.get("arm" + n).frame());
         assertEquals(new Frame(140, y, 50, 36), byName.get("solo" + n).frame());
         assertEquals(new Frame(200, y, 50, 36), byName.get("mute" + n).frame());
         assertEquals(new Frame(260, y, 360, 36), byName.get("volume" + n).frame());
         assertEquals(n, byName.get("volume" + n).tag());
      }
   }

   @Test
   void keepsEveryControlOnThePage()
   {
      for (final Control c : mixer)
         assertTrue(fits(c.frame(), MixerLayout.PAGE_FRAME), c.name());
   }

   @Test
   void putsPlayStopLoopAndTheBarInTheHeader()
   {
      final Map<String, Control> byName = byName(header);

      final Frame play = new Frame(20, 3, 50, 34);
      assertEquals(play, byName.get("playBackground").frame());
      assertEquals(play, byName.get("playState").frame());
      assertEquals(play, byName.get("playStateLabel").frame());
      assertEquals("▶", byName.get("playStateLabel").text());
      final Frame stop = new Frame(80, 3, 50, 34);
      assertEquals(stop, byName.get("stopBackground").frame());
      assertEquals(stop, byName.get("stop").frame());
      assertEquals(Kind.MOMENTARY_BUTTON, byName.get("stop").kind());
      assertEquals("■", byName.get("stopLabel").text());
      assertEquals(new Frame(140, 3, 110, 34), byName.get("loopEnabled").frame());
      assertEquals(new Frame(140, 3, 110, 34), byName.get("loopLabel").frame());
      assertEquals("Loop", byName.get("loopLabel").text());

      final Control position = byName.get("position");
      assertEquals(Kind.TEXT_LABEL, position.kind());
      assertEquals(new Frame(260, 3, 110, 34), position.frame());
      assertEquals(OscCommand.POSITION_ADDRESS, "/" + position.tag() + "/" + position.oscName());
      assertEquals("1.1", position.text());
   }

   @Test
   void headerSendsPlayStopAndLoopOnly()
   {
      // 小節の表示は受けるだけ。
      final List<String> sent = header.stream().map(Control::address).flatMap(Optional::stream).toList();

      assertEquals(List.of("/0/playState", "/0/stop", "/0/loopEnabled"), sent);
   }

   @Test
   void lightsThePlayButtonInBitwigOrange()
   {
      // 再生中はボタンが塗られるので、この色で光る(利用者の指定で Bitwig のテーマの橙)。
      // mute と同じ橙にそろえる。
      assertEquals(MixerLayout.ORANGE, byName(header).get("playState").color());
      assertEquals(new MixerLayout.Color(1, 0.45063, 0.0222223, 1), MixerLayout.ORANGE);
   }

   @Test
   void putsPlayAndStopOnTheSameGreyLikeBitwig()
   {
      // 止まっているときは下の灰色が見え、記号(▶ ■)はラベルの白い文字で出す。
      final Map<String, Control> byName = byName(header);
      final MixerLayout.Color grey = new MixerLayout.Color(0.35, 0.35, 0.35, 1);

      assertEquals(grey, byName.get("playBackground").color());
      assertEquals(grey, byName.get("stopBackground").color());
      assertEquals(MixerLayout.CLEAR, byName.get("playStateLabel").color());
      assertEquals(MixerLayout.CLEAR, byName.get("stopLabel").color());
   }

   @Test
   void keepsTheHeaderAboveThePages()
   {
      final Frame strip = new Frame(0, 0, 640, MixerLayout.PAGER_FRAME.y());
      for (final Control c : header)
         assertTrue(fits(c.frame(), strip), c.name());
   }

   @Test
   void drawsThePlayButtonWithThePlayShape()
   {
      assertEquals(Kind.PLAY_BUTTON, byName(header).get("playState").kind());
   }

   @Test
   void asksTheExtensionForTheStateWhenTheLayoutOpens()
   {
      // Bitwig を先に起動していても、開いたときに今の色や音量が揃うように。
      final String script = layout.rootScript();

      assertTrue(script.contains("function init()"), script);
      assertTrue(script.contains("sendOSC('" + OscCommand.SYNC_ADDRESS + "')"), script);
   }

   @Test
   void drawsTheBackgroundBelowThePlayButtonAndTheLabelAboveTheLoopButton()
   {
      // TouchOSC は後ろの部品ほど上に描く。
      final List<String> names = header.stream().map(Control::name).toList();

      assertTrue(names.indexOf("playBackground") < names.indexOf("playState"));
      assertTrue(names.indexOf("playState") < names.indexOf("playStateLabel"));
      assertTrue(names.indexOf("stopBackground") < names.indexOf("stop"));
      assertTrue(names.indexOf("stop") < names.indexOf("stopLabel"));
      assertTrue(names.indexOf("loopEnabled") < names.indexOf("loopLabel"));
   }

   @Test
   void namesAreUniqueBecauseIdsAreMadeFromThem()
   {
      final List<String> names = new ArrayList<>(controls.stream().map(Control::name).toList());
      pages.forEach(p -> names.add(p.name()));

      assertEquals(names.size(), Set.copyOf(names).size());
   }

   @Test
   void placesTrackScrollingWhereTheClipPageHasIt()
   {
      // ページ 1 と 2 で、トラックを動かすボタンが同じ所にあるように。
      final Map<String, Control> byName = byName(mixer);
      final Map<String, Control> clips = byName(pages.get(1).controls());

      // トラックは上から下に並ぶので、▲ は色の列の上、▼ は下。
      assertEquals(new Frame(20, 0, 50, 36), byName.get("mixerScrollUp").frame());
      assertEquals(new Frame(20, 360, 50, 36), byName.get("mixerScrollDown").frame());
      assertEquals(clips.get("scrollUp").frame(), byName.get("mixerScrollUp").frame());
      assertEquals(clips.get("scrollDown").frame(), byName.get("mixerScrollDown").frame());
      assertEquals("▲", byName.get("mixerScrollUpLabel").text());
      assertEquals("▼", byName.get("mixerScrollDownLabel").text());
   }

   /** {@code inner} が {@code outer} と同じ大きさの入れ物に収まるか(座標は入れ物の中の位置)。 */
   static boolean fits(final Frame inner, final Frame outer)
   {
      return inner.x() >= 0 && inner.y() >= 0 && inner.x() + inner.w() <= outer.w()
         && inner.y() + inner.h() <= outer.h();
   }

   private static Map<String, Control> byName(final List<Control> list)
   {
      return list.stream().collect(Collectors.toMap(Control::name, Function.identity()));
   }
}
