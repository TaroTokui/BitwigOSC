package dev.bitwigosc;

import java.util.List;
import java.util.OptionalDouble;

/**
 * OSC の引数を、Bitwig に渡せる値に直す。
 * TouchOSC はフェーダーもボタンも float で送ってくるが、ほかの送信元も考えて int と bool も受ける。
 */
final class OscArgs
{
   private static final double ON_THRESHOLD = 0.5;

   private OscArgs()
   {
   }

   static OptionalDouble firstNumber(final List<Object> args)
   {
      if (args == null || args.isEmpty())
         return OptionalDouble.empty();
      final Object first = args.get(0);
      if (first instanceof Boolean b)
         return OptionalDouble.of(b ? 1.0 : 0.0);
      if (first instanceof Number n && Double.isFinite(n.doubleValue()))
         return OptionalDouble.of(n.doubleValue());
      return OptionalDouble.empty();
   }

   /** ボタンは押すと 1.0、離すと 0.0。中間の値も半分を境に分ける。 */
   static boolean isOn(final double value)
   {
      return value >= ON_THRESHOLD;
   }

   /** Bitwig の値は 0〜1 に正規化されている。範囲の外は端に寄せる。 */
   static double clampUnit(final double value)
   {
      return Math.max(0.0, Math.min(1.0, value));
   }
}
