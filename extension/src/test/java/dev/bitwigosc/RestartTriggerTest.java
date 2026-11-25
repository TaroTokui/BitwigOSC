package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class RestartTriggerTest
{
   private int restarts;
   private final RestartTrigger trigger = new RestartTrigger(() -> restarts++);

   @Test
   void doesNotRestartWhenObserverReportsTheInitialValue()
   {
      final Consumer<String> host = trigger.watch("192.0.2.10");

      host.accept("192.0.2.10");

      assertEquals(0, restarts);
   }

   @Test
   void restartsWhenValueDiffersFromInitial()
   {
      final Consumer<String> host = trigger.watch("127.0.0.1");

      host.accept("192.0.2.10");

      assertEquals(1, restarts);
   }

   @Test
   void restartsOnlyOnceEvenIfSeveralSettingsChange()
   {
      final Consumer<String> host = trigger.watch("127.0.0.1");
      final Consumer<Double> port = trigger.watch(10002.0);

      host.accept("192.0.2.10");
      port.accept(10003.0);
      host.accept("192.0.2.11");

      assertEquals(1, restarts);
   }

   @Test
   void comparesBoxedNumbersAndBooleansByValue()
   {
      final Consumer<Double> port = trigger.watch(10001.0);
      final Consumer<Boolean> log = trigger.watch(true);

      port.accept(10001.0);
      log.accept(true);

      assertEquals(0, restarts);
   }

   @Test
   void handlesNullValues()
   {
      final Consumer<String> host = trigger.watch(null);

      host.accept(null);
      assertEquals(0, restarts);

      host.accept("192.0.2.10");
      assertEquals(1, restarts);
   }
}
