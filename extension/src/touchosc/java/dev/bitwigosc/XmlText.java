package dev.bitwigosc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * DOM を、TouchOSC が書くのと同じ形の文字にする(字下げなし、属性は一重引用符、文字は CDATA のまま)。
 * JDK の Transformer を使わないのは、属性の引用符や宣言の書き方を選べず、TouchOSC の書くものと形が変わるため。
 */
final class XmlText
{
   private static final List<String> ATTRIBUTE_ORDER = List.of("ID", "type");

   private XmlText()
   {
   }

   static void append(final StringBuilder out, final Element element)
   {
      out.append('<').append(element.getTagName());
      appendAttributes(out, element.getAttributes());
      if (element.getFirstChild() == null)
      {
         out.append("/>");
         return;
      }
      out.append('>');
      for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling())
         appendNode(out, n);
      out.append("</").append(element.getTagName()).append('>');
   }

   private static void appendNode(final StringBuilder out, final Node node)
   {
      switch (node.getNodeType())
      {
         case Node.ELEMENT_NODE -> append(out, (Element) node);
         case Node.TEXT_NODE -> out.append(escape(node.getNodeValue(), false));
         case Node.CDATA_SECTION_NODE -> out.append("<![CDATA[").append(cdata(node.getNodeValue())).append("]]>");
         default -> throw new IllegalStateException("unexpected XML node type " + node.getNodeType());
      }
   }

   /** TouchOSC の書く順(ID, type)を先に、ほかは名前順に書く。DOM の持つ順には頼らない(決まりがないため)。 */
   private static void appendAttributes(final StringBuilder out, final NamedNodeMap attributes)
   {
      final List<Attr> sorted = new ArrayList<>();
      for (int i = 0; i < attributes.getLength(); i++)
         sorted.add((Attr) attributes.item(i));
      sorted.sort(Comparator.comparingInt((Attr a) -> rank(a.getName())).thenComparing(Attr::getName));

      for (final Attr attr : sorted)
         out.append(' ').append(attr.getName()).append("='").append(escape(attr.getValue(), true)).append('\'');
   }

   private static int rank(final String name)
   {
      final int index = ATTRIBUTE_ORDER.indexOf(name);
      return index < 0 ? ATTRIBUTE_ORDER.size() : index;
   }

   private static String cdata(final String text)
   {
      if (text.contains("]]>"))
         throw new IllegalArgumentException("text cannot be written as CDATA: " + text);
      return text;
   }

   private static String escape(final String text, final boolean attribute)
   {
      final StringBuilder out = new StringBuilder(text.length());
      for (final char c : text.toCharArray())
      {
         switch (c)
         {
            case '&' -> out.append("&amp;");
            case '<' -> out.append("&lt;");
            case '>' -> out.append("&gt;");
            case '\'' -> out.append(attribute ? "&apos;" : "'");
            case '"' -> out.append(attribute ? "&quot;" : "\"");
            default -> out.append(c);
         }
      }
      return out.toString();
   }
}
