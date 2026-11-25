package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

class OscArgsTest
{
   @Test
   void readsFloatAsTouchOscSendsIt()
   {
      assertEquals(OptionalDouble.of(0.5), OscArgs.firstNumber(List.of(0.5f)));
   }

   @Test
   void readsIntegerAndDouble()
   {
      assertEquals(OptionalDouble.of(1.0), OscArgs.firstNumber(List.of(1)));
      assertEquals(OptionalDouble.of(0.25), OscArgs.firstNumber(List.of(0.25d)));
   }

   @Test
   void readsBooleanAsOneOrZero()
   {
      assertEquals(OptionalDouble.of(1.0), OscArgs.firstNumber(List.of(true)));
      assertEquals(OptionalDouble.of(0.0), OscArgs.firstNumber(List.of(false)));
   }

   @Test
   void returnsEmptyWhenThereIsNoUsableNumber()
   {
      assertTrue(OscArgs.firstNumber(List.of()).isEmpty());
      assertTrue(OscArgs.firstNumber(null).isEmpty());
      assertTrue(OscArgs.firstNumber(List.of("1")).isEmpty());
      assertTrue(OscArgs.firstNumber(Arrays.asList((Object) null)).isEmpty());
      assertTrue(OscArgs.firstNumber(List.of(Float.NaN)).isEmpty());
   }

   @Test
   void treatsHalfAndAboveAsOn()
   {
      assertTrue(OscArgs.isOn(1.0));
      assertTrue(OscArgs.isOn(0.5));
      assertFalse(OscArgs.isOn(0.49));
      assertFalse(OscArgs.isOn(0.0));
   }

   @Test
   void clampsToNormalizedRange()
   {
      assertEquals(0.0, OscArgs.clampUnit(-0.2));
      assertEquals(0.7, OscArgs.clampUnit(0.7));
      assertEquals(1.0, OscArgs.clampUnit(1.3));
   }
}
