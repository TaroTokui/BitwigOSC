package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class SendFailureLogTest
{
   private final SendFailureLog log = new SendFailureLog();

   @Test
   void reportsTheFirstFailure()
   {
      final Optional<String> line = log.failed("/1/volume", new IOException("unreachable"));

      assertEquals(Optional.of("BitwigOSC: failed to send /1/volume: unreachable"
         + " (further failures are hidden until a send succeeds)"), line);
   }

   @Test
   void hidesRepeatedFailures()
   {
      log.failed("/1/volume", new IOException("unreachable"));

      assertTrue(log.failed("/1/volume", new IOException("unreachable")).isEmpty());
      assertTrue(log.failed("/2/mute", new IOException("unreachable")).isEmpty());
   }

   @Test
   void reportsRecoveryWithTheNumberOfHiddenFailures()
   {
      log.failed("/1/volume", new IOException("x"));
      log.failed("/1/volume", new IOException("x"));
      log.failed("/1/volume", new IOException("x"));

      assertEquals(Optional.of("BitwigOSC: sending works again (2 failures were hidden)"), log.succeeded());
   }

   @Test
   void saysNothingOnSuccessWhenNothingFailed()
   {
      assertTrue(log.succeeded().isEmpty());
   }

   @Test
   void reportsAgainAfterRecovering()
   {
      log.failed("/1/volume", new IOException("x"));
      log.succeeded();

      assertTrue(log.failed("/1/volume", new IOException("x")).isPresent());
      assertEquals(Optional.of("BitwigOSC: sending works again (0 failures were hidden)"), log.succeeded());
   }

   @Test
   void usesTheExceptionTypeWhenThereIsNoMessage()
   {
      assertEquals(Optional.of("BitwigOSC: failed to send /0/playState: IllegalArgumentException"
         + " (further failures are hidden until a send succeeds)"),
         log.failed("/0/playState", new IllegalArgumentException()));
   }
}
