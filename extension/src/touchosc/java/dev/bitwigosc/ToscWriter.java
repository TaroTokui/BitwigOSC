package dev.bitwigosc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DeflaterOutputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import dev.bitwigosc.MixerLayout.Color;
import dev.bitwigosc.MixerLayout.Control;
import dev.bitwigosc.MixerLayout.Frame;
import dev.bitwigosc.MixerLayout.Kind;
import dev.bitwigosc.MixerLayout.Layout;
import dev.bitwigosc.MixerLayout.Page;

/**
 * {@link MixerLayout} の並びを TouchOSC のレイアウト(.tosc = zlib で圧縮した XML)にする。
 * 部品の種類ごとのひな形を読み、部品ごとに違うところ(ID・名前・tag・位置・色・アドレス・文字)だけを書き換える。
 * いちばん外の入れ物には、開いたときに動くスクリプトも書く。
 */
final class ToscWriter
{
   private static final String TEMPLATES = "/dev/bitwigosc/templates/";
   private static final String ID_PREFIX = "BitwigOSC/";
   private static final String HEADER = "<?xml version='1.0' encoding='UTF-8'?>";

   private static final Map<Kind, String> TEMPLATE_OF = Map.of(
      Kind.FADER, "fader.xml",
      Kind.BUTTON, "button.xml",
      Kind.PLAY_BUTTON, "play-button.xml",
      Kind.COLOR_BOX, "color-box.xml",
      Kind.BOX, "box.xml",
      Kind.LABEL, "label.xml");

   private ToscWriter()
   {
   }

   /**
    * TouchOSC に読み込ませるファイルの中身。同じ並びなら、展開した XML は毎回同じになる。
    * 圧縮したバイト列は、同じ JDK なら同じ(JDK の zlib が変わると変わりうるので、テストは展開して比べる)。
    */
   static byte[] toTosc(final Layout layout)
   {
      final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (DeflaterOutputStream zlib = new DeflaterOutputStream(bytes))
      {
         zlib.write(toXml(layout).getBytes(StandardCharsets.UTF_8));
      }
      catch (final IOException e)
      {
         throw new UncheckedIOException(e);
      }
      return bytes.toByteArray();
   }

   /** 圧縮する前の XML。TouchOSC が書くのと同じく、字下げなし・属性は一重引用符。 */
   static String toXml(final Layout layout)
   {
      final Document document = newBuilder().newDocument();
      final Element lexml = document.createElement("lexml");
      lexml.setAttribute("version", "5");
      document.appendChild(lexml);

      final List<Element> pageNodes = new ArrayList<>();
      for (final Page page : layout.pages())
         pageNodes.add(pageNode(document, page));

      final Element pager = container(document, "pager.xml", "pager", pageNodes);
      final Element root = container(document, "root-group.xml", "group", List.of(pager));
      // TouchOSC はレイアウトを読み込んだときに、このスクリプトの init() を呼ぶ。
      setString(root, "script", layout.rootScript());
      lexml.appendChild(root);

      final StringBuilder out = new StringBuilder(HEADER);
      XmlText.append(out, lexml);
      return out.toString();
   }

   private static Element pageNode(final Document document, final Page page)
   {
      final List<Element> controls = new ArrayList<>();
      for (final Control control : page.controls())
         controls.add(controlNode(document, control));

      final Element node = container(document, "page.xml", page.name(), controls);
      setString(node, "tabLabel", page.tabLabel());
      return node;
   }

   private static Element container(final Document document, final String template, final String name,
      final List<Element> children)
   {
      final Element node = load(document, template, name);
      final Element holder = Dom.child(node, "children")
         .orElseThrow(() -> new IllegalStateException(template + " has no <children>"));
      if (children.isEmpty())
         node.removeChild(holder);
      else
         children.forEach(holder::appendChild);
      return node;
   }

   private static Element controlNode(final Document document, final Control control)
   {
      final Element node = load(document, TEMPLATE_OF.get(control.kind()), control.name());
      if (!control.tag().isEmpty())
         setString(node, "tag", control.tag());
      setFrame(node, control.frame());
      setColor(node, control.color());
      if (!control.oscName().isEmpty())
         setPathName(node, control.oscName());
      if (!control.text().isEmpty())
         setDefaultValue(node, "text", control.text());
      return node;
   }

   /** ひな形を読み、ID と名前を付ける。ID は名前から作るので、作るたびに変わらない。 */
   private static Element load(final Document document, final String template, final String name)
   {
      try (InputStream in = ToscWriter.class.getResourceAsStream(TEMPLATES + template))
      {
         if (in == null)
            throw new IllegalStateException("template not found: " + template);
         final Element parsed = newBuilder().parse(in).getDocumentElement();
         Dom.removeWhitespace(parsed);
         final Element node = (Element) document.importNode(parsed, true);
         node.setAttribute("ID", UUID.nameUUIDFromBytes((ID_PREFIX + name).getBytes(StandardCharsets.UTF_8)).toString());
         setString(node, "name", name);
         return node;
      }
      catch (final IOException e)
      {
         throw new UncheckedIOException(e);
      }
      catch (final SAXException e)
      {
         throw new IllegalStateException("template is not valid XML: " + template, e);
      }
   }

   // ---- 性質の書き換え(ひな形にないものを書こうとしたら止める) ----

   private static void setString(final Element node, final String key, final String value)
   {
      Dom.setCdata(propertyValue(node, key), value);
   }

   private static void setFrame(final Element node, final Frame frame)
   {
      final Element value = propertyValue(node, "frame");
      Dom.setText(Dom.required(value, "x"), String.valueOf(frame.x()));
      Dom.setText(Dom.required(value, "y"), String.valueOf(frame.y()));
      Dom.setText(Dom.required(value, "w"), String.valueOf(frame.w()));
      Dom.setText(Dom.required(value, "h"), String.valueOf(frame.h()));
   }

   private static void setColor(final Element node, final Color color)
   {
      final Element value = propertyValue(node, "color");
      Dom.setText(Dom.required(value, "r"), number(color.r()));
      Dom.setText(Dom.required(value, "g"), number(color.g()));
      Dom.setText(Dom.required(value, "b"), number(color.b()));
      Dom.setText(Dom.required(value, "a"), number(color.a()));
   }

   /** アドレス {@code /}{tag}{@code /name} の最後の部分を書き換える。送りが 1 つだけの部品に使う。 */
   private static void setPathName(final Element node, final String oscName)
   {
      final List<Element> oscs = Dom.children(Dom.required(node, "messages"), "osc");
      if (oscs.size() != 1)
         throw new IllegalStateException(name(node) + " should send exactly one OSC message, has " + oscs.size());
      final List<Element> partials = Dom.children(Dom.required(oscs.get(0), "path"), "partial");
      final Element last = partials.get(partials.size() - 1);
      if (!"CONSTANT".equals(Dom.required(last, "type").getTextContent()))
         throw new IllegalStateException(name(node) + ": the last part of the OSC path is not a constant");
      Dom.setCdata(Dom.required(last, "value"), "/" + oscName);
   }

   private static void setDefaultValue(final Element node, final String key, final String value)
   {
      for (final Element v : Dom.children(Dom.required(node, "values"), "value"))
         if (key.equals(Dom.required(v, "key").getTextContent()))
         {
            Dom.setCdata(Dom.required(v, "default"), value);
            return;
         }
      throw new IllegalStateException(name(node) + " has no value '" + key + "'");
   }

   private static Element propertyValue(final Element node, final String key)
   {
      for (final Element p : Dom.children(Dom.required(node, "properties"), "property"))
         if (key.equals(Dom.required(p, "key").getTextContent()))
            return Dom.required(p, "value");
      throw new IllegalStateException(node.getAttribute("type") + " template has no property '" + key + "'");
   }

   private static String name(final Element node)
   {
      return node.getAttribute("type") + " " + propertyValue(node, "name").getTextContent();
   }

   /** 0.784314 や 1 のように、TouchOSC と同じく余分な 0 を付けずに書く。 */
   private static String number(final double value)
   {
      return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
   }

   private static DocumentBuilder newBuilder()
   {
      try
      {
         final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         // ひな形は自前のファイルだが、外の DTD や実体は読まない。
         factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
         factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         factory.setXIncludeAware(false);
         // CDATA を文字とまとめない(TouchOSC と同じく CDATA のまま書き戻すため)。
         factory.setCoalescing(false);
         return factory.newDocumentBuilder();
      }
      catch (final ParserConfigurationException e)
      {
         throw new IllegalStateException(e);
      }
   }

   /** DOM を辿る小さな道具。直下の子だけを見る(子の部品の中まで入らないように)。 */
   private static final class Dom
   {
      private Dom()
      {
      }

      static List<Element> children(final Element parent, final String name)
      {
         final List<Element> found = new ArrayList<>();
         for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof final Element e && e.getTagName().equals(name))
               found.add(e);
         return found;
      }

      static java.util.Optional<Element> child(final Element parent, final String name)
      {
         return children(parent, name).stream().findFirst();
      }

      static Element required(final Element parent, final String name)
      {
         return child(parent, name)
            .orElseThrow(() -> new IllegalStateException("<" + parent.getTagName() + "> has no <" + name + ">"));
      }

      static void setText(final Element element, final String text)
      {
         element.setTextContent(text);
      }

      static void setCdata(final Element element, final String text)
      {
         while (element.getFirstChild() != null)
            element.removeChild(element.getFirstChild());
         element.appendChild(element.getOwnerDocument().createCDATASection(text));
      }

      /** 字下げのための空白だけの文字を消す(CDATA は残す)。 */
      static void removeWhitespace(final Node node)
      {
         Node n = node.getFirstChild();
         while (n != null)
         {
            final Node next = n.getNextSibling();
            if (n.getNodeType() == Node.TEXT_NODE && n.getNodeValue().isBlank())
               node.removeChild(n);
            else if (n.getNodeType() == Node.ELEMENT_NODE)
               removeWhitespace(n);
            n = next;
         }
      }
   }
}
