package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.bitwigosc.OscCommand.TrackCommand;
import dev.bitwigosc.OscCommand.TrackParam;
import dev.bitwigosc.OscCommand.TransportCommand;
import dev.bitwigosc.OscCommand.TransportParam;

class FeedbackTest
{
   private record Sent(String address, List<Object> args) {}

   private final List<Sent> sent = new ArrayList<>();
   private final Feedback feedback = new Feedback((address, args) -> sent.add(new Sent(address, List.of(args))));

   @Test
   void sendsTrackValuesAsFloat()
   {
      feedback.value(new TrackCommand(0, TrackParam.VOLUME), 0.75);

      assertEquals(List.of(new Sent("/1/volume", List.of(0.75f))), sent);
   }

   @Test
   void sendsPanOfALaterTrackAsIs()
   {
      // 送るときは寄せない。Bitwig の値はもともと 0〜1 に収まっているため。
      feedback.value(new TrackCommand(4, TrackParam.PAN), 0.5);

      assertEquals(List.of(new Sent("/5/pan", List.of(0.5f))), sent);
   }

   @Test
   void sendsFlagsAsOneOrZero()
   {
      feedback.flag(new TrackCommand(2, TrackParam.MUTE), true);
      feedback.flag(new TrackCommand(2, TrackParam.MUTE), false);

      assertEquals(List.of(
         new Sent("/3/mute", List.of(1.0f)),
         new Sent("/3/mute", List.of(0.0f))), sent);
   }

   @Test
   void sendsTransportFlagsOnNumberZero()
   {
      feedback.flag(new TransportCommand(TransportParam.PLAY), true);

      assertEquals(List.of(new Sent("/0/playState", List.of(1.0f))), sent);
   }

   @Test
   void sendsTrackColorAsThreeMessages()
   {
      feedback.trackColor(7, true, 1.0f, 0.5f, 0.0f);

      assertEquals(List.of(
         new Sent("/8/colorR", List.of(1.0f)),
         new Sent("/8/colorG", List.of(0.5f)),
         new Sent("/8/colorB", List.of(0.0f))), sent);
   }

   @Test
   void sendsBlackWhereThereIsNoTrack()
   {
      // 曲のトラックが 8 本より少ないとき。黒は背景と同じなので、箱が見えなくなる。
      feedback.trackColor(3, false, 1.0f, 0.5f, 0.25f);

      assertEquals(List.of(
         new Sent("/4/colorR", List.of(0.0f)),
         new Sent("/4/colorG", List.of(0.0f)),
         new Sent("/4/colorB", List.of(0.0f))), sent);
   }
}
