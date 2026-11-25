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

import org.junit.jupiter.api.Test;

import dev.bitwigosc.MixerLayout.Control;
import dev.bitwigosc.MixerLayout.Frame;
import dev.bitwigosc.MixerLayout.Kind;
import dev.bitwigosc.MixerLayout.Page;

class MixerLayoutTest
{
   private final List<Page> pages = MixerLayout.layout().pages();
   private final List<Control> controls = pages.stream().flatMap(p -> p.controls().stream()).toList();

   @Test
   void keepsThePagesOfThePreviousLayout()
   {
      assertEquals(List.of("1", "2", "3"), pages.stream().map(Page::name).toList());
      assertEquals(List.of("Controller", "2", "3"), pages.stream().map(Page::tabLabel).toList());
      assertTrue(pages.get(1).controls().isEmpty());
      assertTrue(pages.get(2).controls().isEmpty());
   }

   @Test
   void hasAsManyControlsOfEachKindAsThePreviousLayout()
   {
      final Map<Kind, Long> counts = controls.stream()
         .collect(Collectors.groupingBy(Control::kind, Collectors.counting()));

      assertEquals(Map.of(Kind.FADER, 8L, Kind.BUTTON, 25L, Kind.PLAY_BUTTON, 1L, Kind.COLOR_BOX, 8L,
         Kind.BOX, 1L, Kind.LABEL, 1L), counts);
   }

   @Test
   void sendsExactlyTheAddressesOfThePreviousLayout()
   {
      // ping ボタンは置かない(表示は Bitwig 側の変化か iPad の操作で揃う)。
      final Set<String> expected = new HashSet<>(Set.of("/0/playState", "/0/loopEnabled"));
      for (int track = 1; track <= 8; track++)
         for (final String name : List.of("volume", "arm", "solo", "mute"))
            expected.add("/" + track + "/" + name);

      final List<String> sent = controls.stream().map(Control::address).flatMap(Optional::stream).toList();

      assertEquals(expected, Set.copyOf(sent));
      assertEquals(expected.size(), sent.size());
   }

   @Test
   void everySentAddressMeansTheSameToTheExtension()
   {
      for (final Control control : controls)
      {
         control.address().ifPresent(address -> {
            final Optional<OscCommand> command = OscCommand.parse(address, MixerLayout.TRACK_COUNT);
            assertTrue(command.isPresent(), address);
            assertEquals(address, command.get().address());
         });
      }
   }

   @Test
   void placesOneColumnPerTrackSixtyApart()
   {
      final Map<String, Control> byName = byName();
      for (int i = 0; i < 8; i++)
      {
         final int x = 20 + 60 * i;
         final String n = String.valueOf(i + 1);
         assertEquals(new Frame(x, 20, 50, 10), byName.get("color" + n).frame());
         assertEquals(new Frame(x, 40, 50, 200), byName.get("volume" + n).frame());
         assertEquals(new Frame(x, 260, 50, 40), byName.get("arm" + n).frame());
         assertEquals(new Frame(x, 320, 50, 40), byName.get("solo" + n).frame());
         assertEquals(new Frame(x, 380, 50, 40), byName.get("mute" + n).frame());
         assertEquals(n, byName.get("volume" + n).tag());
      }
   }

   @Test
   void placesTheTransportOnTheRightAsBefore()
   {
      final Map<String, Control> byName = byName();

      assertEquals(new Frame(530, 40, 70, 70), byName.get("playBackground").frame());
      assertEquals(new Frame(537, 50, 50, 50), byName.get("playState").frame());
      assertEquals(new Frame(530, 130, 70, 70), byName.get("loopEnabled").frame());
      assertEquals(new Frame(525, 153, 80, 25), byName.get("loopLabel").frame());
      assertEquals("Loop", byName.get("loopLabel").text());
   }

   @Test
   void drawsThePlayButtonWithThePlayShape()
   {
      assertEquals(Kind.PLAY_BUTTON, byName().get("playState").kind());
   }

   @Test
   void asksTheExtensionForTheStateWhenTheLayoutOpens()
   {
      // Bitwig を先に起動していても、開いたときに今の色や音量が揃うように。
      final String script = MixerLayout.layout().rootScript();

      assertTrue(script.contains("function init()"), script);
      assertTrue(script.contains("sendOSC('" + OscCommand.SYNC_ADDRESS + "')"), script);
   }

   @Test
   void drawsTheBackgroundBelowThePlayButtonAndTheLabelAboveTheLoopButton()
   {
      // TouchOSC は後ろの部品ほど上に描く。
      final List<String> names = controls.stream().map(Control::name).toList();

      assertTrue(names.indexOf("playBackground") < names.indexOf("playState"));
      assertTrue(names.indexOf("loopEnabled") < names.indexOf("loopLabel"));
   }

   @Test
   void namesAreUniqueBecauseIdsAreMadeFromThem()
   {
      final List<String> names = new ArrayList<>(controls.stream().map(Control::name).toList());
      pages.forEach(p -> names.add(p.name()));

      assertEquals(names.size(), Set.copyOf(names).size());
   }

   private Map<String, Control> byName()
   {
      return controls.stream().collect(Collectors.toMap(Control::name, Function.identity()));
   }
}
