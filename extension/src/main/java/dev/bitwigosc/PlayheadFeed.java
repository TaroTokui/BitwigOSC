package dev.bitwigosc;

/**
 * 再生位置を小節の表示として TouchOSC に送る。前と同じ文字なら送らない。
 * Bitwig は再生中、位置を細かく何度も知らせてくる。全部送ると Wi-Fi の UDP があふれるので、
 * 拍が変わって文字が変わったときだけ送る(4/4・120 BPM で 1 秒に 2 通)。
 * 拡張のスレッドだけから使う(Observer と scheduleTask は同じスレッド)。
 */
final class PlayheadFeed
{
   private final Feedback feedback;
   private String sentLabel = "";

   PlayheadFeed(final Feedback feedback)
   {
      this.feedback = feedback;
   }

   void update(final BarPosition position)
   {
      final String label = position.label();
      if (!label.equals(sentLabel))
      {
         sentLabel = label;
         feedback.position(label);
      }
   }

   /** 送ったものを忘れる。次の {@link #update} は必ず送る(TouchOSC があとから開いた /sync のとき)。 */
   void clear()
   {
      sentLabel = "";
   }
}
