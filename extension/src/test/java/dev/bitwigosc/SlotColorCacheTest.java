package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.bitwigosc.SlotColor.Rgb;

class SlotColorCacheTest
{
   private static final Rgb RED = new Rgb(1, 0, 0);
   private static final Rgb BLUE = new Rgb(0, 0, 1);

   private final SlotColorCache cache = new SlotColorCache(8, 8);

   @Test
   void sendsTheFirstColorOfEverySlot()
   {
      assertTrue(cache.update(0, 0, RED));
      assertTrue(cache.update(7, 7, RED));
   }

   @Test
   void skipsAColorThatWasAlreadySent()
   {
      cache.update(2, 3, RED);

      assertFalse(cache.update(2, 3, new Rgb(1, 0, 0)));
   }

   @Test
   void sendsWhenTheColorChanges()
   {
      cache.update(2, 3, RED);

      assertTrue(cache.update(2, 3, BLUE));
      assertFalse(cache.update(2, 3, BLUE));
   }

   @Test
   void keepsEachSlotApart()
   {
      cache.update(0, 0, RED);

      assertTrue(cache.update(0, 1, RED));
      assertTrue(cache.update(1, 0, RED));
   }

   @Test
   void sendsEverythingAgainAfterClear()
   {
      // /sync のとき。TouchOSC があとから開いたなら、前に送った色は届いていない。
      cache.update(0, 0, RED);
      cache.clear();

      assertTrue(cache.update(0, 0, RED));
   }
}
