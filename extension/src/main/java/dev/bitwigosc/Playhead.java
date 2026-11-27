package dev.bitwigosc;

import com.bitwig.extension.controller.api.SettableIntegerValue;
import com.bitwig.extension.controller.api.Transport;

/**
 * Bitwig の再生位置と拍子を見て、ヘッダーの小節の表示に送る。
 * 計算は {@link BarPosition}、送るかどうかは {@link PlayheadFeed} が決める。ここは Bitwig の値をつなぐだけ。
 */
final class Playhead
{
   private final Transport transport;
   private PlayheadFeed feed;

   Playhead(final Transport transport)
   {
      this.transport = transport;
   }

   /** Observer は init() の中でしか登録できない。拍子が変わったときも、今の位置で計算し直す。 */
   void observe(final Feedback feedback)
   {
      feed = new PlayheadFeed(feedback);
      transport.playPosition().addValueObserver(beats -> send());
      numerator().addValueObserver(value -> send());
      denominator().addValueObserver(value -> send());
   }

   /** 今の位置を送り直す。{@link #observe} を先に呼んでおくこと。 */
   void sendAll()
   {
      feed.clear();
      send();
   }

   private void send()
   {
      feed.update(BarPosition.of(transport.playPosition().get(), numerator().get(), denominator().get()));
   }

   private SettableIntegerValue numerator()
   {
      return transport.timeSignature().numerator();
   }

   private SettableIntegerValue denominator()
   {
      return transport.timeSignature().denominator();
   }
}
