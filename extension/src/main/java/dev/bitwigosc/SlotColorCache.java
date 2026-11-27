package dev.bitwigosc;

import java.util.Arrays;

import dev.bitwigosc.SlotColor.Rgb;

/**
 * マスごとに、最後に送った色を覚える。同じ色なら送らない。
 * マスには Observer が 6 つあり、クリップの状態が 1 回変わるだけでいくつも呼ばれる。そのたびに 3 通送ると、
 * 起動したときやスクロールしたときに 1000 通を超え、Wi-Fi の UDP で取りこぼしやすくなる。
 * 拡張のスレッドだけから使う(Observer と scheduleTask は同じスレッド)。
 */
final class SlotColorCache
{
   private final Rgb[][] sent;

   SlotColorCache(final int trackCount, final int sceneCount)
   {
      sent = new Rgb[trackCount][sceneCount];
   }

   /** 前に送った色と違えば覚え直して true(送ってよい)。同じなら false。 */
   boolean update(final int track, final int scene, final Rgb rgb)
   {
      if (rgb.equals(sent[track][scene]))
         return false;
      sent[track][scene] = rgb;
      return true;
   }

   /** 全部忘れる。次はどのマスも送る(TouchOSC があとから開いた /sync のとき)。 */
   void clear()
   {
      for (final Rgb[] column : sent)
         Arrays.fill(column, null);
   }
}
