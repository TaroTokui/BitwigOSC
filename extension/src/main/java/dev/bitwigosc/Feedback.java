package dev.bitwigosc;

import java.util.function.UnaryOperator;

/**
 * Bitwig 側の変化を、TouchOSC に送る OSC のメッセージにする。
 * アドレスは受け取るときと同じ {@link OscCommand} から作るので、行きと帰りでずれない。
 * 値は float で送る(TouchOSC のフェーダーとボタンは float を受ける)。小節の表示だけは文字で送る。
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
      color(name -> OscCommand.trackAddress(index, name), exists, red, green, blue);
   }

   /** ページ 2 の行の頭の箱。ミキサーと同じく、トラックがない位置には黒を送る。 */
   void launcherTrackColor(final int track, final boolean exists, final float red, final float green,
      final float blue)
   {
      color(name -> LauncherCommand.trackAddress(track, name), exists, red, green, blue);
   }

   /** ページ 2 のマス。色は {@link SlotColor} で状態から決めたもの。 */
   void slotColor(final int track, final int scene, final SlotColor.Rgb rgb)
   {
      color(name -> LauncherCommand.slotAddress(track, scene, name), true, rgb.red(), rgb.green(), rgb.blue());
   }

   /** ヘッダーの小節の表示({@code 12.3} など)。TouchOSC のラベルは文字で受ける。 */
   void position(final String label)
   {
      sender.send(OscCommand.POSITION_ADDRESS, label);
   }

   private void color(final UnaryOperator<String> address, final boolean exists, final float red,
      final float green, final float blue)
   {
      sender.send(address.apply("colorR"), exists ? red : NO_TRACK);
      sender.send(address.apply("colorG"), exists ? green : NO_TRACK);
      sender.send(address.apply("colorB"), exists ? blue : NO_TRACK);
   }
}
