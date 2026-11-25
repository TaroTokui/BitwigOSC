package dev.bitwigosc;

import java.util.Optional;

/**
 * 送れなかったことを、コンソールにあふれさせずに知らせる。
 * 送り返しはフェーダー 1 回の操作で何十回も起きるので、続けて失敗したら最初の 1 回だけ出し、
 * 送れるように戻ったときに、隠した回数を出す。
 * 状態を持つが、呼ぶのは Bitwig の拡張のスレッドだけなので、同期はしない。
 */
final class SendFailureLog
{
   private boolean failing;
   private int hidden;

   /** 出すべき行があれば返す。続けての失敗なら空。 */
   Optional<String> failed(final String address, final Exception error)
   {
      if (failing)
      {
         hidden++;
         return Optional.empty();
      }
      failing = true;
      return Optional.of("BitwigOSC: failed to send " + address + ": " + describe(error)
         + " (further failures are hidden until a send succeeds)");
   }

   /** 失敗が続いていたなら、戻ったことを知らせる行を返す。 */
   Optional<String> succeeded()
   {
      if (!failing)
         return Optional.empty();
      final int count = hidden;
      failing = false;
      hidden = 0;
      return Optional.of("BitwigOSC: sending works again (" + count + " failures were hidden)");
   }

   // メッセージのない例外もあるので、そのときは種類の名前を出す。
   private static String describe(final Exception error)
   {
      final String message = error.getMessage();
      return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
   }
}
