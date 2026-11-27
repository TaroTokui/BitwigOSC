package dev.bitwigosc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import dev.bitwigosc.MixerLayout.Color;
import dev.bitwigosc.MixerLayout.Control;
import dev.bitwigosc.MixerLayout.Frame;
import dev.bitwigosc.MixerLayout.Kind;
import dev.bitwigosc.MixerLayout.Layout;
import dev.bitwigosc.MixerLayout.Page;

class ToscWriterTest
{
   /** テストは extension/ で動くので、リポジトリの touchosc/ は 1 つ上にある。 */
   private static final Path COMMITTED = Path.of("..", "touchosc", "BitwigOSC.tosc");

   private final String xml = ToscWriter.toXml(MixerLayout.layout());
   private final List<Element> nodes = nodes(parse(xml));

   @Test
   void writesTheSameHeaderAsTouchOsc()
   {
      assertTrue(xml.startsWith("<?xml version='1.0' encoding='UTF-8'?><lexml version='5'><node "), xml.substring(0, 80));
   }

   @Test
   void hasTheNodesOfThePreviousLayoutPlusTheClipPage()
   {
      final Map<String, Integer> counts = new TreeMap<>();
      nodes.forEach(n -> counts.merge(n.getAttribute("type"), 1, Integer::sum));

      // ヘッダー: 再生の下地 1、再生とループ 2、Loop と小節の表示 2(以前はページ 1 の右にあった)
      // ページ 1: ミキサーのスクロールのボタン 2 とラベル 2
      // ページ 2: 色の箱 64 + 列の色 8、ボタン 64 + 停止 8 + シーン 8 + スクロール 4、ラベル 8 + 8 + 4
      assertEquals(Map.of("GROUP", 4, "PAGER", 1, "BOX", 10 + 72, "FADER", 8, "BUTTON", 29 + 84, "LABEL", 6 + 20),
         counts);
   }

   @Test
   void launchButtonsAreMomentaryAndIgnoreWhatBitwigSends()
   {
      final Element launch = nodes.stream().filter(n -> "launch1_1".equals(property(n, "name"))).findFirst()
         .orElseThrow();

      assertEquals("BUTTON", launch.getAttribute("type"));
      assertEquals("0", property(launch, "buttonType"));
      assertEquals(List.of("/clip/1/1/launch"), oscPaths(launch));
      final Element osc = descendants(launch, "osc").get(0);
      assertEquals("0", text(child(osc, "receive").orElseThrow()));
   }

   @Test
   void givesEveryNodeItsOwnId()
   {
      final List<String> ids = nodes.stream().map(n -> n.getAttribute("ID")).toList();

      assertEquals(ids.size(), Set.copyOf(ids).size());
      assertFalse(ids.contains("00000000-0000-0000-0000-000000000000"));
   }

   @Test
   void writesIdBeforeTypeLikeTouchOsc()
   {
      final long nodeCount = Pattern.compile("<node ").matcher(xml).results().count();
      final long ordered = Pattern.compile("<node ID='[0-9a-f-]{36}' type='[A-Z]+'>").matcher(xml).results().count();

      assertEquals(nodeCount, ordered);
   }

   @Test
   void sendsNoMidiAndHasNoPingButton()
   {
      assertFalse(xml.contains("<midi>"));

      final List<String> allPaths = new ArrayList<>();
      nodes.forEach(n -> allPaths.addAll(oscPaths(n)));
      assertFalse(allPaths.contains("/ping"), allPaths.toString());
      assertFalse(nodes.stream().anyMatch(n -> property(n, "name").toLowerCase(Locale.ROOT).contains("ping")));
   }

   @Test
   void writesEachControlsValuesIntoItsNode()
   {
      final Map<String, Element> byName = new TreeMap<>();
      nodes.forEach(n -> byName.put(property(n, "name"), n));

      for (final Control control : allControls())
         {
            final Element node = byName.get(control.name());
            assertTrue(node != null, control.name());
            assertEquals(control.frame(), frame(node), control.name());
            assertEquals(control.color(), color(node), control.name());
            if (!control.tag().isEmpty())
               assertEquals(control.tag(), property(node, "tag"), control.name());
            if (!control.text().isEmpty())
               assertEquals(control.text(), defaultValue(node, "text"), control.name());
         }
   }

   @Test
   void putsTheScriptOnTheOutermostGroup()
   {
      final Element root = nodes.get(0);

      assertEquals("GROUP", root.getAttribute("type"));
      assertEquals(MixerLayout.layout().rootScript(), property(root, "script"));
   }

   @Test
   void drawsThePlayButtonAsAFillOverTheGreyBoxThatShowsOnlyWhilePlaying()
   {
      // 四角で、オフのときの薄い塗り(background)も枠もない。オンのときだけ橙に塗られ、下の灰色の箱を隠す。
      final Element play = nodes.stream().filter(n -> "playState".equals(property(n, "name"))).findFirst().orElseThrow();

      assertEquals("BUTTON", play.getAttribute("type"));
      assertEquals("1", property(play, "shape"));
      assertEquals("0", property(play, "background"));
      assertEquals("0", property(play, "outline"));
      assertEquals("1", property(play, "buttonType"));
      assertEquals(List.of("/0/playState"), oscPaths(play));
   }

   @Test
   void drawsTheVolumeFadersSideways()
   {
      // トラックが行に並ぶので、音量は横向き(右へ大きくなる)。
      nodes.stream().filter(n -> "FADER".equals(n.getAttribute("type")))
         .forEach(n -> assertEquals("1", property(n, "orientation"), property(n, "name")));
   }

   @Test
   void fadersAndButtonsSendTheAddressesOfTheLayout()
   {
      final Set<String> expected = new HashSet<>();
      allControls().forEach(c -> c.address().ifPresent(expected::add));

      final List<String> paths = new ArrayList<>();
      nodes.stream()
         .filter(n -> Set.of("FADER", "BUTTON").contains(n.getAttribute("type")))
         .forEach(n -> paths.addAll(sentPaths(n)));

      assertEquals(expected, Set.copyOf(paths));
      assertEquals(expected.size(), paths.size());
   }

   @Test
   void colorBoxesListenToTheAddressesTheExtensionSendsColorsTo()
   {
      final Set<String> expected = new HashSet<>();
      for (int i = 0; i < MixerLayout.TRACK_COUNT; i++)
         for (final String name : List.of("colorR", "colorG", "colorB"))
         {
            expected.add(OscCommand.trackAddress(i, name));
            expected.add(LauncherCommand.trackAddress(i, name));
            for (int s = 0; s < LauncherPage.SCENE_COUNT; s++)
               expected.add(LauncherCommand.slotAddress(i, s, name));
         }

      final List<String> paths = new ArrayList<>();
      nodes.stream().filter(n -> "BOX".equals(n.getAttribute("type"))).forEach(n -> paths.addAll(oscPaths(n)));

      assertEquals(expected, Set.copyOf(paths));
   }

   @Test
   void leavesEmptyPagesWithoutChildren()
   {
      final List<Element> pages = nodes.stream()
         .filter(n -> "GROUP".equals(n.getAttribute("type")) && "PAGER".equals(parentType(n)))
         .toList();

      assertEquals(3, pages.size());
      assertTrue(child(pages.get(0), "children").isPresent());
      assertTrue(child(pages.get(1), "children").isPresent());
      assertFalse(child(pages.get(2), "children").isPresent());
   }

   @Test
   void writesTheSameBytesEveryTime()
   {
      assertArrayEquals(ToscWriter.toTosc(MixerLayout.layout()), ToscWriter.toTosc(MixerLayout.layout()));
   }

   @Test
   void compressesTheXmlWithZlibLikeTouchOsc() throws IOException
   {
      final byte[] tosc = ToscWriter.toTosc(MixerLayout.layout());

      assertEquals((byte) 0x78, tosc[0]);
      assertEquals((byte) 0x9C, tosc[1]);
      assertEquals(xml, new String(inflate(tosc), StandardCharsets.UTF_8));
   }

   @Test
   void refusesToSetAPropertyTheTemplateDoesNotHave()
   {
      // 下地の箱のひな形には tag がない。黙って捨てずに止まる。
      final Control boxWithTag = new Control(Kind.BOX, "box", "1", new Frame(0, 0, 10, 10),
         new Color(0, 0, 0, 1), "", "");
      final List<Page> pages = List.of(new Page("1", "1", List.of(boxWithTag)));

      assertThrows(IllegalStateException.class, () -> ToscWriter.toXml(new Layout("", List.of(), pages)));
   }

   @Test
   void committedLayoutIsUpToDate() throws IOException
   {
      assertTrue(Files.exists(COMMITTED), COMMITTED.toAbsolutePath() + " がない。./gradlew generateLayout で作る");
      assertEquals(xml, new String(inflate(Files.readAllBytes(COMMITTED)), StandardCharsets.UTF_8),
         "touchosc/BitwigOSC.tosc が古い。./gradlew generateLayout で作り直す");
   }

   @Test
   void putsTheHeaderOnTheOutermostGroupSoEveryPageShowsIt()
   {
      // ページの中に置くと、そのページを開いている間しか見えない。いちばん外の入れ物に直に置く。
      final Element root = nodes.get(0);
      for (final Control control : MixerLayout.layout().header())
         assertEquals(root, owner(node(control.name())), control.name());
   }

   @Test
   void movesThePagerBelowTheHeader()
   {
      final Element pager = nodes.stream().filter(n -> "PAGER".equals(n.getAttribute("type"))).findFirst()
         .orElseThrow();

      assertEquals(MixerLayout.PAGER_FRAME, frame(pager));
      nodes.stream().filter(n -> "PAGER".equals(parentType(n)))
         .forEach(page -> assertEquals(MixerLayout.PAGE_FRAME, frame(page), property(page, "name")));
   }

   @Test
   void showsTheBarInALabelThatListensForText()
   {
      final Element position = node("position");

      assertEquals("LABEL", position.getAttribute("type"));
      assertEquals(List.of(OscCommand.POSITION_ADDRESS), oscPaths(position));
      assertEquals(List.of(), sentPaths(position));
      final Element osc = descendants(position, "osc").get(0);
      assertEquals("1", text(child(osc, "receive").orElseThrow()));
      assertEquals("text", text(child(child(osc, "triggers").orElseThrow(), "trigger").flatMap(t -> child(t, "var")).orElseThrow()));
   }

   // ---- 読み返すための道具 ----

   /** ヘッダーとすべてのページの部品。 */
   private static List<Control> allControls()
   {
      final Layout layout = MixerLayout.layout();
      final List<Control> all = new ArrayList<>(layout.header());
      layout.pages().forEach(p -> all.addAll(p.controls()));
      return all;
   }

   /** 部品を子に持つ部品。部品は親の {@code <children>} の中にある。 */
   private static Element owner(final Element node)
   {
      return (Element) node.getParentNode().getParentNode();
   }

   private Element node(final String name)
   {
      return nodes.stream().filter(n -> name.equals(property(n, "name"))).findFirst().orElseThrow();
   }

   /** 送るメッセージ(send が 1)のアドレスだけ。受けるだけのものは入れない。 */
   private static List<String> sentPaths(final Element node)
   {
      final List<String> paths = new ArrayList<>();
      final List<String> all = oscPaths(node);
      final List<Element> messages = descendants(node, "osc");
      for (int i = 0; i < messages.size(); i++)
         if ("1".equals(text(child(messages.get(i), "send").orElseThrow())))
            paths.add(all.get(i));
      return paths;
   }

   private static Document parse(final String text)
   {
      try
      {
         final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         return factory.newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
      }
      catch (final Exception e)
      {
         throw new AssertionError("XML として読めない", e);
      }
   }

   private static List<Element> nodes(final Document document)
   {
      final List<Element> found = new ArrayList<>();
      final var list = document.getElementsByTagName("node");
      for (int i = 0; i < list.getLength(); i++)
         found.add((Element) list.item(i));
      return found;
   }

   /** OSC のアドレスを、TouchOSC と同じように組み立てる(PROPERTY は部品の性質の値に置き換える)。 */
   private static List<String> oscPaths(final Element node)
   {
      final List<String> paths = new ArrayList<>();
      for (final Element osc : descendants(node, "osc"))
      {
         final StringBuilder path = new StringBuilder();
         for (final Element partial : children(child(osc, "path").orElseThrow(), "partial"))
         {
            final String value = text(child(partial, "value").orElseThrow());
            final String type = text(child(partial, "type").orElseThrow());
            path.append("PROPERTY".equals(type) ? property(node, value) : value);
         }
         paths.add(path.toString());
      }
      return paths;
   }

   private static String property(final Element node, final String key)
   {
      return text(propertyValue(node, key));
   }

   private static Element propertyValue(final Element node, final String key)
   {
      for (final Element p : children(child(node, "properties").orElseThrow(), "property"))
         if (key.equals(text(child(p, "key").orElseThrow())))
            return child(p, "value").orElseThrow();
      throw new AssertionError("no property " + key);
   }

   private static Frame frame(final Element node)
   {
      final Element v = propertyValue(node, "frame");
      return new Frame(integer(v, "x"), integer(v, "y"), integer(v, "w"), integer(v, "h"));
   }

   private static Color color(final Element node)
   {
      final Element v = propertyValue(node, "color");
      return new Color(decimal(v, "r"), decimal(v, "g"), decimal(v, "b"), decimal(v, "a"));
   }

   private static int integer(final Element parent, final String name)
   {
      return Integer.parseInt(text(child(parent, name).orElseThrow()));
   }

   private static double decimal(final Element parent, final String name)
   {
      return Double.parseDouble(text(child(parent, name).orElseThrow()));
   }

   private static String defaultValue(final Element node, final String key)
   {
      for (final Element v : children(child(node, "values").orElseThrow(), "value"))
         if (key.equals(text(child(v, "key").orElseThrow())))
            return text(child(v, "default").orElseThrow());
      throw new AssertionError("no value " + key);
   }

   /** 部品の中のものだけ(子の部品の中は見ない)。 */
   private static List<Element> descendants(final Element node, final String name)
   {
      return child(node, "messages").map(m -> children(m, name)).orElse(List.of());
   }

   private static List<Element> children(final Element parent, final String name)
   {
      final List<Element> found = new ArrayList<>();
      for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
         if (n instanceof final Element e && e.getTagName().equals(name))
            found.add(e);
      return found;
   }

   private static Optional<Element> child(final Element parent, final String name)
   {
      return children(parent, name).stream().findFirst();
   }

   /** 部品は親の {@code <children>} の中にある。いちばん外の部品の親は {@code <lexml>} なので空を返す。 */
   private static String parentType(final Element node)
   {
      final Node holder = node.getParentNode();
      return holder.getParentNode() instanceof final Element parent ? parent.getAttribute("type") : "";
   }

   private static String text(final Element e)
   {
      return e.getTextContent();
   }

   private static byte[] inflate(final byte[] data) throws IOException
   {
      try (var in = new InflaterInputStream(new ByteArrayInputStream(data)); var out = new ByteArrayOutputStream())
      {
         in.transferTo(out);
         return out.toByteArray();
      }
   }
}
