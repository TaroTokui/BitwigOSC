package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BarPositionTest
{
   /** 再生位置は 4 分音符を 1 拍とした数(Bitwig の playPosition と同じ)。 */
   @ParameterizedTest
   @CsvSource({
      // 拍, 分子, 分母, 小節, 拍
      "0,     4, 4,  1, 1",
      "5.5,   4, 4,  2, 2",
      "15.99, 4, 4,  4, 4",
      "16,    4, 4,  5, 1",
      "17,    4, 4,  5, 2",
      // 6/8 は 1 小節が 4 分音符 3 つ、拍は 8 分音符
      "3.5,   6, 8,  2, 2",
      // 3/4 は 1 小節が 4 分音符 3 つ
      "12,    3, 4,  5, 1",
      // 再生を始める前(プリロール)は 0 小節目
      "-1,    4, 4,  0, 4",
   })
   void countsBarsAndBeatsFromThePlayPosition(final double beats, final int numerator, final int denominator,
      final int bar, final int beat)
   {
      final BarPosition position = BarPosition.of(beats, numerator, denominator);

      assertEquals(bar, position.bar());
      assertEquals(beat, position.beat());
   }

   @ParameterizedTest
   @CsvSource({ "0, 1.1", "5.5, 2.2", "39, 10.4" })
   void showsBarDotBeat(final double beats, final String label)
   {
      assertEquals(label, BarPosition.of(beats, 4, 4).label());
   }

   @ParameterizedTest
   @CsvSource({ "0, 4, 0", "8, 0, 4", "8, 4, 0" })
   void treatsABrokenTimeSignatureAsFourFour(final double beats, final int numerator, final int denominator)
   {
      // 拍子を読む前の 0 が来ても、0 で割らない。
      assertEquals(BarPosition.of(beats, 4, 4), BarPosition.of(beats, numerator, denominator));
   }
}
