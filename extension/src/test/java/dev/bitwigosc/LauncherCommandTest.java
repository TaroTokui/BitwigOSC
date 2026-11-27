package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.bitwigosc.LauncherCommand.Direction;
import dev.bitwigosc.LauncherCommand.LaunchScene;
import dev.bitwigosc.LauncherCommand.LaunchSlot;
import dev.bitwigosc.LauncherCommand.Scroll;
import dev.bitwigosc.LauncherCommand.StopTrack;

class LauncherCommandTest
{
   private static final int TRACKS = 8;
   private static final int SCENES = 8;

   @ParameterizedTest
   @CsvSource({
      "/clip/1/1/launch, 0, 0",
      "/clip/3/5/launch, 2, 4",
      "/clip/8/8/launch, 7, 7",
   })
   void parsesSlotLaunchAsOneBasedTrackAndScene(final String address, final int track, final int scene)
   {
      assertEquals(Optional.of(new LaunchSlot(track, scene)), OscCommand.parse(address, TRACKS, SCENES));
   }

   @Test
   void parsesTrackStop()
   {
      assertEquals(Optional.of(new StopTrack(3)), OscCommand.parse("/clip/4/stop", TRACKS, SCENES));
   }

   @Test
   void parsesSceneLaunch()
   {
      assertEquals(Optional.of(new LaunchScene(7)), OscCommand.parse("/scene/8/launch", TRACKS, SCENES));
   }

   @ParameterizedTest
   @CsvSource({
      "/clip/scrollUp, UP",
      "/clip/scrollDown, DOWN",
      "/clip/scrollLeft, LEFT",
      "/clip/scrollRight, RIGHT",
   })
   void parsesScroll(final String address, final Direction direction)
   {
      assertEquals(Optional.of(new Scroll(direction)), OscCommand.parse(address, TRACKS, SCENES));
   }

   @ParameterizedTest
   @ValueSource(strings = {
      "/clip/0/1/launch",      // 1 から数える
      "/clip/9/1/launch",      // トラックがバンクの外
      "/clip/1/9/launch",      // シーンがバンクの外
      "/clip/1/0/launch",
      "/clip/1/1/colorR",      // 送り返す専用
      "/clip/1/colorR",
      "/clip/9/stop",
      "/clip/0/stop",
      "/scene/0/launch",
      "/scene/9/launch",
      "/scene/1/stop",
      "/clip/scrollSideways",
      "/clip/1/1/launch/extra",
      "/clip/99999999999/1/launch",
      "/clip",
   })
   void ignoresAddressesOutsideTheProtocol(final String address)
   {
      assertTrue(OscCommand.parse(address, TRACKS, SCENES).isEmpty(), address);
   }

   @Test
   void everyCommandParsesBackFromItsOwnAddress()
   {
      for (int track = 0; track < TRACKS; track++)
      {
         assertRoundTrip(new StopTrack(track));
         for (int scene = 0; scene < SCENES; scene++)
            assertRoundTrip(new LaunchSlot(track, scene));
      }
      for (int scene = 0; scene < SCENES; scene++)
         assertRoundTrip(new LaunchScene(scene));
      for (final Direction direction : Direction.values())
         assertRoundTrip(new Scroll(direction));
   }

   @Test
   void buildsSendOnlyAddresses()
   {
      assertEquals("/clip/2/3/colorG", LauncherCommand.slotAddress(1, 2, "colorG"));
      assertEquals("/clip/8/colorB", LauncherCommand.trackAddress(7, "colorB"));
   }

   private static void assertRoundTrip(final OscCommand command)
   {
      assertEquals(Optional.of(command), OscCommand.parse(command.address(), TRACKS, SCENES), command.address());
   }
}
