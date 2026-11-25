package dev.bitwigosc;

/**
 * Bitwig 側の変化を、TouchOSC に送る OSC のメッセージにする。
 * アドレスは受け取るときと同じ {@link OscCommand} から作るので、行きと帰りでずれない。
 * 値はすべて float で送る(TouchOSC のフェーダーとボタンは float を受ける)。
 */
final class Feedback
{
   /** 送る処理。拡張では TouchOSC への接続、テストでは記録するだけの偽物を渡す。 */
   @FunctionalInterface
   interface OscSender
   {
      void send(String address, Object... args);
   }

   private static final float ON = 1.0f;
   private static final float OFF = 0.0f;
   /** トラックがない位置の色(R・G・B とも 0 = 黒)。 */
   private static final float NO_TRACK = 0.0f;

   private final OscSender sender;

   Feedback(final OscSender sender)
   {
      this.sender = sender;
   }

   /** 音量やパンなど、0〜1 の値。 */
   void value(final OscCommand command, final double value)
   {
      sender.send(command.address(), (float) value);
   }

   /** mute や再生など、オン・オフ。 */
   void flag(final OscCommand command, final boolean on)
   {
      sender.send(command.address(), on ? ON : OFF);
   }

   /**
    * トラックの色。レイアウトの箱が R・G・B を別々のアドレスで受けるので、3 つに分けて送る。
    * トラックがない位置には黒を送る。黒はレイアウトの背景と同じなので、箱が見えなくなる。
    */
   void trackColor(final int index, final boolean exists, final float red, final float green, final float blue)
   {
      sender.send(OscCommand.trackAddress(index, "colorR"), exists ? red : NO_TRACK);
      sender.send(OscCommand.trackAddress(index, "colorG"), exists ? green : NO_TRACK);
      sender.send(OscCommand.trackAddress(index, "colorB"), exists ? blue : NO_TRACK);
   }
}
