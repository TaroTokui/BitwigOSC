package dev.bitwigosc;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 設定が起動時の値から変わったら、拡張の再起動を1回だけ合図する。
 * Bitwig の Observer は登録直後に今の値で呼ばれることがあるので、起動時の値と同じなら何もしない。
 */
final class RestartTrigger
{
   private final Runnable restart;
   private boolean fired;

   RestartTrigger(final Runnable restart)
   {
      this.restart = restart;
   }

   <T> Consumer<T> watch(final T initial)
   {
      return value -> {
         if (fired || Objects.equals(initial, value))
            return;
         fired = true;
         restart.run();
      };
   }
}
