package dev.bitwigosc;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TouchOSC から届いたアドレスが、Bitwig の何の操作にあたるか。
 * 形は {@code /{番号}/{操作}}。
 * 番号は TouchOSC のレイアウトの tag で、トラックは 1 から数え、0 はトランスポートに使う。
 */
sealed interface OscCommand permits OscCommand.TrackCommand, OscCommand.TransportCommand
{
   /** {@code index} はバンクの中の位置(0 始まり)。アドレスの番号から 1 を引いたもの。 */
   record TrackCommand(int index, TrackParam param) implements OscCommand
   {
      @Override
      public String address()
      {
         return trackAddress(index, param.oscName());
      }
   }

   record TransportCommand(TransportParam param) implements OscCommand
   {
      @Override
      public String address()
      {
         return "/" + TRANSPORT_NUMBER + "/" + param.oscName();
      }
   }

   /** 送り返すときのアドレス。{@link #parse} の逆で、{@code parse(c.address())} は {@code c} に戻る。 */
   String address();

   /** トラックの {@code index}(0 始まり)と操作の名前から、1 始まりのアドレスを作る。色など送り返す専用の名前にも使う。 */
   static String trackAddress(final int index, final String name)
   {
      return "/" + (index + 1) + "/" + name;
   }

   enum TrackParam
   {
      VOLUME("volume"), PAN("pan"), MUTE("mute"), SOLO("solo"), ARM("arm"), ACTIVE("active");

      private final String oscName;

      TrackParam(final String oscName)
      {
         this.oscName = oscName;
      }

      String oscName()
      {
         return oscName;
      }

      static Optional<TrackParam> fromOscName(final String name)
      {
         return Arrays.stream(values()).filter(p -> p.oscName.equals(name)).findFirst();
      }
   }

   enum TransportParam
   {
      PLAY("playState"), LOOP("loopEnabled");

      private final String oscName;

      TransportParam(final String oscName)
      {
         this.oscName = oscName;
      }

      String oscName()
      {
         return oscName;
      }

      static Optional<TransportParam> fromOscName(final String name)
      {
         return Arrays.stream(values()).filter(p -> p.oscName.equals(name)).findFirst();
      }
   }

   int TRANSPORT_NUMBER = 0;

   /**
    * TouchOSC がレイアウトを開いたときに送るアドレス(レイアウトのスクリプトが送る)。操作ではないので {@link #parse} は受けない。
    * 拡張はこれを受けたら、今の状態を全部送り返す。Bitwig を先に起動していても、開いた時点の色や音量が揃う。
    */
   String SYNC_ADDRESS = "/sync";

   // 番号は 4 桁まで。それより長いものはバンクに入らないので、int に直す前に弾く。
   Pattern ADDRESS = Pattern.compile("^/(\\d{1,4})/([A-Za-z]+)$");

   /** 決まりの外のアドレスは空を返す(届いたものをすべて受けるので、例外にはしない)。 */
   static Optional<OscCommand> parse(final String address, final int trackCount)
   {
      if (address == null)
         return Optional.empty();
      final Matcher m = ADDRESS.matcher(address);
      if (!m.matches())
         return Optional.empty();

      final int number = Integer.parseInt(m.group(1));
      final String name = m.group(2);
      if (number == TRANSPORT_NUMBER)
         return TransportParam.fromOscName(name).map(TransportCommand::new);
      if (number > trackCount)
         return Optional.empty();
      return TrackParam.fromOscName(name).map(p -> new TrackCommand(number - 1, p));
   }
}
