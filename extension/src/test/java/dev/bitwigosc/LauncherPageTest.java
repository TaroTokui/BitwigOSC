package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import dev.bitwigosc.MixerLayout.Control;
import dev.bitwigosc.MixerLayout.Frame;
import dev.bitwigosc.MixerLayout.Kind;

class LauncherPageTest
{
   private static final int TRACKS = MixerLayout.TRACK_COUNT;
   private static final int SCENES = LauncherPage.SCENE_COUNT;

   private final List<Control> controls = LauncherPage.controls();
   private final Map<String, Control> byName = controls.stream()
      .collect(Collectors.toMap(Control::name, Function.identity()));

   @Test
   void isTheSecondPageOfTheLayout()
   {
      final MixerLayout.Page page = MixerLayout.layout().pages().get(1);

      assertEquals("2", page.name());
      assertEquals("Clips", page.tabLabel());
      assertEquals(controls, page.controls());
   }

   @Test
   void sendsALaunchForEverySlotSceneStopAndScroll()
   {
      final Set<String> expected = new HashSet<>();
      for (int t = 1; t <= TRACKS; t++)
      {
         expected.add("/clip/" + t + "/stop");
         for (int s = 1; s <= SCENES; s++)
            expected.add("/clip/" + t + "/" + s + "/launch");
      }
      for (int s = 1; s <= SCENES; s++)
         expected.add("/scene/" + s + "/launch");
      for (final String d : List.of("Up", "Down", "Left", "Right"))
         expected.add("/clip/scroll" + d);

      final List<String> sent = controls.stream().map(Control::address).flatMap(Optional::stream).toList();

      assertEquals(expected, Set.copyOf(sent));
      assertEquals(expected.size(), sent.size());
   }

   @Test
   void everySentAddressMeansTheSameToTheExtension()
   {
      for (final Control control : controls)
         control.address().ifPresent(address -> {
            final Optional<OscCommand> command = OscCommand.parse(address, TRACKS, SCENES);
            assertTrue(command.isPresent(), address);
            assertEquals(address, command.get().address());
         });
   }

   @Test
   void usesMomentaryButtonsSoEveryPressSends()
   {
      // トグルだと、押すたびに 1 と 0 が交互に届き、2 回に 1 回しか起動しない。
      controls.stream().filter(c -> c.address().isPresent())
         .forEach(c -> assertEquals(Kind.MOMENTARY_BUTTON, c.kind(), c.name()));
   }

   @Test
   void placesOneTrackPerRowInTheSameRowsAsTheMixer()
   {
      // Bitwig のアレンジ画面のクリップランチャーと同じく、トラックが行、シーンが列。
      for (int t = 0; t < TRACKS; t++)
      {
         final int y = 40 + 40 * t;
         final String n = String.valueOf(t + 1);
         assertEquals(new Frame(60, y, 10, 36), byName.get("clipTrackColor" + n).frame());
         assertEquals("clip/" + n, byName.get("clipTrackColor" + n).tag());
         assertEquals(new Frame(560, y, 50, 36), byName.get("stop" + n).frame());
         for (int s = 0; s < SCENES; s++)
         {
            final String cell = n + "_" + (s + 1);
            final Frame frame = new Frame(80 + 60 * s, y, 50, 36);
            assertEquals(frame, byName.get("slotColor" + cell).frame());
            assertEquals(frame, byName.get("launch" + cell).frame());
            assertEquals("clip/" + n + "/" + (s + 1), byName.get("slotColor" + cell).tag());
         }
      }
   }

   @Test
   void placesSceneButtonsAtTheTopOfEachColumn()
   {
      for (int s = 0; s < SCENES; s++)
         assertEquals(new Frame(80 + 60 * s, 0, 50, 36), byName.get("scene" + (s + 1)).frame());
   }

   @Test
   void drawsTheClipColorBelowTheButtonThatLaunchesIt()
   {
      // TouchOSC は後ろの部品ほど上に描く。ボタンは枠だけなので、下の色が見える。
      final List<String> names = controls.stream().map(Control::name).toList();
      for (int t = 1; t <= TRACKS; t++)
         for (int s = 1; s <= SCENES; s++)
            assertTrue(names.indexOf("slotColor" + t + "_" + s) < names.indexOf("launch" + t + "_" + s));
   }

   @Test
   void labelsAreDrawnAboveTheirButtons()
   {
      final List<String> names = controls.stream().map(Control::name).toList();
      for (final String button : List.of("scene1", "stop1", "scrollUp", "scrollRight"))
         assertTrue(names.indexOf(button) < names.indexOf(button + "Label"), button);
   }

   @Test
   void placesTrackScrollingAboveAndBelowTheTrackColorsAndSceneScrollingBelowTheGrid()
   {
      // ▲▼ はトラック(行)、◀▶ はシーン(列)を動かす。
      assertEquals(new Frame(20, 0, 50, 36), byName.get("scrollUp").frame());
      assertEquals(new Frame(20, 360, 50, 36), byName.get("scrollDown").frame());
      assertEquals(new Frame(80, 360, 50, 36), byName.get("scrollLeft").frame());
      assertEquals(new Frame(80 + 60 * 7, 360, 50, 36), byName.get("scrollRight").frame());
      assertEquals("▲", byName.get("scrollUpLabel").text());
      assertEquals("◀", byName.get("scrollLeftLabel").text());
   }

   @Test
   void leavesTheBarToTheHeaderAndHasNoPlayheadLine()
   {
      // 小節の表示はどのページにも出るヘッダーにある(MixerLayoutTest)。縦線は置かない。
      assertFalse(byName.containsKey("position"));
      assertFalse(byName.containsKey("playhead"));
   }

   @Test
   void keepsEveryControlOnThePage()
   {
      for (final Control c : controls)
         assertTrue(MixerLayoutTest.fits(c.frame(), MixerLayout.PAGE_FRAME), c.name());
   }
}
