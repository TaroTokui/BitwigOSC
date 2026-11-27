package dev.bitwigosc;

/**
 * 再生位置を「何小節目の何拍目か」にする。ヘッダーの小節の表示に使う。
 * Bitwig の再生位置は 4 分音符を 1 拍とした数で届くので、拍子(分子 / 分母)で小節に分ける。
 * 曲の頭から今の拍子が続いているとみなす。曲の途中で拍子が変わると、変わった後の小節の番号は Bitwig の表示とずれる。
 *
 * @param bar  小節。1 から数える(再生前のプリロールは 0 以下)
 * @param beat 小節の中の拍。1 から数え、拍の長さは分母の音符(6/8 なら 8 分音符)
 */
record BarPosition(int bar, int beat)
{
   private static final int QUARTERS_PER_WHOLE = 4;
   private static final int DEFAULT_NUMERATOR = 4;
   private static final int DEFAULT_DENOMINATOR = 4;

   static BarPosition of(final double beats, final int numerator, final int denominator)
   {
      // 拍子を読む前は 0 が来ることがある。0 で割らないよう、そのときは 4/4 とみなす。
      final boolean valid = numerator > 0 && denominator > 0;
      final int num = valid ? numerator : DEFAULT_NUMERATOR;
      final int den = valid ? denominator : DEFAULT_DENOMINATOR;

      final double beatLength = (double) QUARTERS_PER_WHOLE / den;
      final double barLength = num * beatLength;
      final int bar = (int) Math.floor(beats / barLength) + 1;
      final int beat = (int) Math.floor(floorMod(beats, barLength) / beatLength) + 1;
      return new BarPosition(bar, beat);
   }

   /** TouchOSC に出す文字。{@code 12.3} は 12 小節目の 3 拍目。 */
   String label()
   {
      return bar + "." + beat;
   }

   /** 負の数でも 0 以上 m 未満を返す余り(Math.floorMod の double 版)。 */
   private static double floorMod(final double x, final double m)
   {
      return x - Math.floor(x / m) * m;
   }
}
