package dev.bitwigosc;

import java.util.List;

/**
 * 受け取った OSC メッセージを、Bitwig のコンソールに出す1行の文字列にする。
 * Bitwig の API に依存しないので、単体テストで確かめられる。
 */
final class OscLog
{
   private OscLog()
   {
   }

   static String format(final String address, final List<Object> args)
   {
      final StringBuilder line = new StringBuilder(address);
      if (args == null)
         return line.toString();

      for (final Object arg : args)
         line.append(' ').append(describe(arg));
      return line.toString();
   }

   private static String describe(final Object arg)
   {
      if (arg == null)
         return "nil";
      final String type = arg.getClass().getSimpleName();
      if (arg instanceof String s)
         return "\"" + s + "\":" + type;
      return arg + ":" + type;
   }
}
