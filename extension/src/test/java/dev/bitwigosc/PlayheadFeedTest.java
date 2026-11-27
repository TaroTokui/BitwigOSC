package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlayheadFeedTest
{
   private record Sent(String address, List<Object> args) {}

   private final List<Sent> sent = new ArrayList<>();
   private final PlayheadFeed feed = new PlayheadFeed(
      new Feedback((address, args) -> sent.add(new Sent(address, List.of(args)))));

   @Test
   void sendsTheBarFirst()
   {
      feed.update(BarPosition.of(5.5, 4, 4));

      assertEquals(List.of(new Sent("/0/position", List.of("2.2"))), sent);
   }

   @Test
   void sendsNothingWhileTheBeatStaysTheSame()
   {
      // 再生位置は細かく何度も届く。表示の文字が変わらないなら送らない。
      feed.update(BarPosition.of(4.0, 4, 4));
      sent.clear();

      feed.update(BarPosition.of(4.5, 4, 4));
      feed.update(BarPosition.of(4.99, 4, 4));

      assertEquals(List.of(), sent);
   }

   @Test
   void sendsTheBarWhenTheBeatChanges()
   {
      feed.update(BarPosition.of(4.5, 4, 4));
      sent.clear();

      feed.update(BarPosition.of(5.0, 4, 4));

      assertEquals(List.of(new Sent("/0/position", List.of("2.2"))), sent);
   }

   @Test
   void sendsTheBarAgainAfterClear()
   {
      // /sync のとき。TouchOSC があとから開いたなら、前に送った位置は届いていない。
      feed.update(BarPosition.of(0, 4, 4));
      sent.clear();
      feed.clear();

      feed.update(BarPosition.of(0, 4, 4));

      assertEquals(List.of(new Sent("/0/position", List.of("1.1"))), sent);
   }
}
