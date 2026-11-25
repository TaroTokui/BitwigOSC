package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.bitwigosc.OscCommand.TrackCommand;
import dev.bitwigosc.OscCommand.TrackParam;
import dev.bitwigosc.OscCommand.TransportCommand;
import dev.bitwigosc.OscCommand.TransportParam;

class OscCommandTest
{
   private static final int TRACKS = 8;

   @ParameterizedTest
   @CsvSource({
      "/1/volume, 0, VOLUME",
      "/1/pan, 0, PAN",
      "/2/mute, 1, MUTE",
      "/3/solo, 2, SOLO",
      "/4/arm, 3, ARM",
      "/8/active, 7, ACTIVE",
   })
   void parsesTrackAddressesAsOneBasedTrackNumbers(final String address, final int index, final TrackParam param)
   {
      assertEquals(Optional.of(new TrackCommand(index, param)), OscCommand.parse(address, TRACKS));
   }

   @ParameterizedTest
   @CsvSource({
      "/0/playState, PLAY",
      "/0/loopEnabled, LOOP",
   })
   void parsesTrackZeroAsTransport(final String address, final TransportParam param)
   {
      assertEquals(Optional.of(new TransportCommand(param)), OscCommand.parse(address, TRACKS));
   }

   @ParameterizedTest
   @ValueSource(strings = {
      "/9/volume",       // バンクの外
      "/0/volume",       // 0 はトランスポート用
      "/1/playState",    // トランスポートの操作はトラックにはない
      "/1/colorR",       // 送り返す専用で、受け取らない
      "/1/unknown",
      "/x/volume",
      "/-1/volume",
      "/1/volume/extra",
      "/1",
      "/ping",
      "",
   })
   void ignoresAddressesOutsideTheProtocol(final String address)
   {
      assertTrue(OscCommand.parse(address, TRACKS).isEmpty(), address);
   }

   @Test
   void syncIsHandledByTheExtensionNotAsACommand()
   {
      // /sync は TouchOSC がレイアウトを開いたときに送る。拡張が今の状態を全部送り返す。
      assertTrue(OscCommand.parse(OscCommand.SYNC_ADDRESS, TRACKS).isEmpty());
      assertEquals("/sync", OscCommand.SYNC_ADDRESS);
   }

   @Test
   void ignoresNullAddress()
   {
      assertTrue(OscCommand.parse(null, TRACKS).isEmpty());
   }

   @Test
   void ignoresHugeTrackNumbersWithoutOverflow()
   {
      assertTrue(OscCommand.parse("/99999999999/volume", TRACKS).isEmpty());
   }

   @ParameterizedTest
   @CsvSource({
      "0, VOLUME, /1/volume",
      "1, MUTE, /2/mute",
      "7, ACTIVE, /8/active",
   })
   void buildsOneBasedTrackAddresses(final int index, final TrackParam param, final String address)
   {
      assertEquals(address, new TrackCommand(index, param).address());
   }

   @ParameterizedTest
   @CsvSource({
      "PLAY, /0/playState",
      "LOOP, /0/loopEnabled",
   })
   void buildsTransportAddressesOnNumberZero(final TransportParam param, final String address)
   {
      assertEquals(address, new TransportCommand(param).address());
   }

   @Test
   void everyCommandParsesBackFromItsOwnAddress()
   {
      // 受け取りと送り返しでアドレスの決まりがずれないことを、全部の組み合わせで確かめる。
      for (int index = 0; index < TRACKS; index++)
         for (final TrackParam param : TrackParam.values())
         {
            final OscCommand command = new TrackCommand(index, param);
            assertEquals(Optional.of(command), OscCommand.parse(command.address(), TRACKS));
         }
      for (final TransportParam param : TransportParam.values())
      {
         final OscCommand command = new TransportCommand(param);
         assertEquals(Optional.of(command), OscCommand.parse(command.address(), TRACKS));
      }
   }

   @Test
   void buildsTrackAddressForSendOnlyNames()
   {
      assertEquals("/3/colorR", OscCommand.trackAddress(2, "colorR"));
   }
}
