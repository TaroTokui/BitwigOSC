package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.bitwigosc.SlotColor.Rgb;
import dev.bitwigosc.SlotColor.State;

class SlotColorTest
{
   private static final float R = 0.8f;
   private static final float G = 0.4f;
   private static final float B = 0.2f;

   @Test
   void emptySlotIsBlack()
   {
      assertEquals(new Rgb(0, 0, 0), SlotColor.of(state(false, false, false, false, false)));
   }

   @Test
   void stoppedClipIsADimClipColor()
   {
      assertEquals(scaled(SlotColor.STOPPED_BRIGHTNESS), SlotColor.of(state(true, false, false, false, false)));
   }

   @Test
   void queuedClipIsBrighterThanStopped()
   {
      assertEquals(scaled(SlotColor.QUEUED_BRIGHTNESS), SlotColor.of(state(true, false, true, false, false)));
   }

   @Test
   void playingClipIsTheFullClipColor()
   {
      assertEquals(new Rgb(R, G, B), SlotColor.of(state(true, true, false, false, false)));
   }

   @Test
   void playingWinsOverQueued()
   {
      // 再生中のクリップを押し直すと、再生中のまま待機にもなる。
      assertEquals(new Rgb(R, G, B), SlotColor.of(state(true, true, true, false, false)));
   }

   @Test
   void recordingIsRedEvenOnAnEmptySlot()
   {
      assertEquals(SlotColor.RECORDING, SlotColor.of(state(false, false, false, true, false)));
      assertEquals(SlotColor.RECORDING, SlotColor.of(state(true, true, false, true, false)));
   }

   @Test
   void recordingQueuedIsDarkRed()
   {
      assertEquals(SlotColor.RECORDING_QUEUED, SlotColor.of(state(false, false, false, false, true)));
   }

   private static State state(final boolean hasContent, final boolean playing, final boolean playbackQueued,
      final boolean recording, final boolean recordingQueued)
   {
      return new State(hasContent, playing, playbackQueued, recording, recordingQueued, R, G, B);
   }

   private static Rgb scaled(final float brightness)
   {
      return new Rgb(R * brightness, G * brightness, B * brightness);
   }
}
