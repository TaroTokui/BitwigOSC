package dev.bitwigosc;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ページ 2(クリップ)の操作。表示する範囲を動かせる。トラックの範囲はミキサーと同じバンクなので、上下に動かすとページ 1 も一緒に動く。
 * 番号はミキサーと同じく 1 から数える。番号は「範囲の中の位置」で、曲の何番目のトラックかではない。
 * どれも押したとき(値がオン)だけ動く。ボタンはモーメンタリで、離したときにも 0 を送ってくる。
 *
 * <pre>
 * /clip/{トラック}/{シーン}/launch   マスのクリップを起動する
 * /clip/{トラック}/stop              トラックのクリップを止める
 * /scene/{シーン}/launch             シーンを丸ごと起動する
 * /clip/scrollUp など                表示する範囲を 1 つずらす
 * </pre>
 */
sealed interface LauncherCommand extends OscCommand
   permits LauncherCommand.LaunchSlot, LauncherCommand.StopTrack, LauncherCommand.LaunchScene, LauncherCommand.Scroll
{
   /** {@code track}・{@code scene} は範囲の中の位置(0 始まり)。 */
   record LaunchSlot(int track, int scene) implements LauncherCommand
   {
      @Override
      public String address()
      {
         return slotAddress(track, scene, "launch");
      }
   }

   record StopTrack(int track) implements LauncherCommand
   {
      @Override
      public String address()
      {
         return trackAddress(track, "stop");
      }
   }

   record LaunchScene(int scene) implements LauncherCommand
   {
      @Override
      public String address()
      {
         return "/" + SCENE_PREFIX + "/" + (scene + 1) + "/launch";
      }
   }

   record Scroll(Direction direction) implements LauncherCommand
   {
      @Override
      public String address()
      {
         return "/" + CLIP_PREFIX + "/" + direction.oscName();
      }
   }

   /** 上下はトラック、左右はシーンの範囲を動かす(トラックは上から下、シーンは左から右に並ぶ)。 */
   enum Direction
   {
      UP("scrollUp"), DOWN("scrollDown"), LEFT("scrollLeft"), RIGHT("scrollRight");

      private final String oscName;

      Direction(final String oscName)
      {
         this.oscName = oscName;
      }

      String oscName()
      {
         return oscName;
      }

      static Optional<Direction> fromOscName(final String name)
      {
         return Arrays.stream(values()).filter(d -> d.oscName.equals(name)).findFirst();
      }
   }

   /** レイアウトの tag の頭。tag を {@code clip/1/3} にすると、TouchOSC は {@code /clip/1/3/launch} を送る。 */
   String CLIP_PREFIX = "clip";
   String SCENE_PREFIX = "scene";

   // 番号は OscCommand と同じく 4 桁まで(int に直す前に、桁あふれするものを弾く)。
   Pattern SLOT = Pattern.compile("^/clip/(\\d{1,4})/(\\d{1,4})/launch$");
   Pattern STOP = Pattern.compile("^/clip/(\\d{1,4})/stop$");
   Pattern SCENE = Pattern.compile("^/scene/(\\d{1,4})/launch$");
   Pattern SCROLL = Pattern.compile("^/clip/([A-Za-z]+)$");

   /** マスに送り返すアドレス({@code colorR} など)。0 始まりの位置から、1 始まりのアドレスを作る。 */
   static String slotAddress(final int track, final int scene, final String name)
   {
      return "/" + CLIP_PREFIX + "/" + (track + 1) + "/" + (scene + 1) + "/" + name;
   }

   /** ページ 2 のトラック(行)のアドレス。 */
   static String trackAddress(final int track, final String name)
   {
      return "/" + CLIP_PREFIX + "/" + (track + 1) + "/" + name;
   }

   /** 決まりの外は空を返す。{@link OscCommand#parse} から呼ばれる(null はそちらで弾く)。 */
   static Optional<LauncherCommand> parse(final String address, final int trackCount, final int sceneCount)
   {
      Matcher m = SLOT.matcher(address);
      if (m.matches())
      {
         final OptionalInt track = position(m.group(1), trackCount);
         final OptionalInt scene = position(m.group(2), sceneCount);
         return track.isPresent() && scene.isPresent()
            ? Optional.of(new LaunchSlot(track.getAsInt(), scene.getAsInt()))
            : Optional.empty();
      }
      m = STOP.matcher(address);
      if (m.matches())
         return box(position(m.group(1), trackCount)).map(StopTrack::new);
      m = SCENE.matcher(address);
      if (m.matches())
         return box(position(m.group(1), sceneCount)).map(LaunchScene::new);
      m = SCROLL.matcher(address);
      if (m.matches())
         return Direction.fromOscName(m.group(1)).map(Scroll::new);
      return Optional.empty();
   }

   /** 1 始まりの番号を、範囲の中の位置(0 始まり)にする。範囲の外は空。 */
   private static OptionalInt position(final String digits, final int count)
   {
      final int number = Integer.parseInt(digits);
      return number >= 1 && number <= count ? OptionalInt.of(number - 1) : OptionalInt.empty();
   }

   private static Optional<Integer> box(final OptionalInt value)
   {
      return value.isPresent() ? Optional.of(value.getAsInt()) : Optional.empty();
   }
}
