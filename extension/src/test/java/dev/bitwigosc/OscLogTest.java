package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OscLogTest
{
   @Test
   void formatsAddressWithoutArguments()
   {
      assertEquals("/ping", OscLog.format("/ping", List.of()));
   }

   @Test
   void formatsEachArgumentWithItsType()
   {
      assertEquals("/1/volume 0.5:Float", OscLog.format("/1/volume", List.of(0.5f)));
      assertEquals("/1/mute 1:Integer true:Boolean",
         OscLog.format("/1/mute", List.of(1, true)));
   }

   @Test
   void quotesStringArguments()
   {
      assertEquals("/1/name \"Bass\":String", OscLog.format("/1/name", List.of("Bass")));
   }

   @Test
   void showsNullArgumentAsNil()
   {
      assertEquals("/x nil", OscLog.format("/x", Arrays.asList((Object) null)));
   }

   @Test
   void treatsNullArgumentListAsEmpty()
   {
      assertEquals("/ping", OscLog.format("/ping", null));
   }
}
