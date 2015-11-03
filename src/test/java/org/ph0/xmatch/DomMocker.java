package org.ph0.xmatch;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DomMocker {
  /**
   * Construct a mock DOM {@link Document} containing one element, with the supplied attributes in
   * alternating name/value pairs. Methods that retrieve these attributes by index (e.g. calling
   * {@link NamedNodeMap#item(int)} on the result of {@link Element#getAttributes()}) will respect
   * the order of the original pairs.
   * 
   * This is necessary to test matcher behaviors that depend on attribute ordering.
   * 
   * @param namesAndValues sequence of attributes, in the order [name1, value1, name2, value2]
   * @return
   */
  public static Document mockDocumentElementWithAttrs(String... namesAndValues) {
    if (namesAndValues.length % 2 != 0)
      throw new IllegalArgumentException();
    int attrCount = namesAndValues.length / 2;

    /*
     * Let this be a lesson in interface design: the org.w3c.dom APIs are really complex and have
     * a lot of overlapping functionality, so you have to mock a ton of stuff in order to get them
     * to behave as expected.
     */
    Document d = mock(Document.class, throwingAnswer());
    doReturn("[mock Document]").when(d).toString();
    doReturn(Node.DOCUMENT_NODE).when(d).getNodeType();
    doReturn(null).when(d).getNamespaceURI();
    doReturn(d).when(d).getOwnerDocument();
    doReturn(true).when(d).hasChildNodes();
    doReturn(null).when(d).getDoctype();
    doReturn(null).when(d).getPrefix();

    Element e = mock(Element.class, throwingAnswer());
    doReturn("[mock Element with attrs: " + join(", ", namesAndValues) + "]").when(e).toString();
    doReturn(Node.ELEMENT_NODE).when(e).getNodeType();
    doReturn("test").when(e).getNodeName();
    doReturn("test").when(e).getLocalName();
    doReturn(e).when(e).cloneNode(anyBoolean());
    doReturn(true).when(e).hasAttributes();
    doReturn(null).when(e).getPrefix();
    doReturn(null).when(e).getNamespaceURI();
    doReturn(null).when(e).getFirstChild();
    doReturn(false).when(e).hasChildNodes();
    doReturn(d).when(e).getOwnerDocument();
    NodeList emptyNodeList = mock(NodeList.class, throwingAnswer());
    doReturn("[mock NodeList]").when(emptyNodeList).toString();
    doReturn(0).when(emptyNodeList).getLength();
    doReturn(null).when(emptyNodeList).item(anyInt());
    doReturn(emptyNodeList).when(e).getChildNodes();

    NamedNodeMap nm = Mockito.mock(NamedNodeMap.class, throwingAnswer());
    doReturn("[mock NamedNodeMap]").when(nm).toString();
    doReturn(attrCount).when(nm).getLength();
    for (int i = 0; i < attrCount; i++) {
      String name = namesAndValues[2 * i];
      String value = namesAndValues[2 * i + 1];
      Attr a = mock(Attr.class, throwingAnswer());
      doReturn(Node.ATTRIBUTE_NODE).when(a).getNodeType();
      doReturn("[mock Attr #" + i + ": " + name + "=\"" + value + "\"]").when(a).toString();
      doReturn(null).when(a).getNamespaceURI();
      doReturn(name).when(a).getNodeName();
      doReturn(name).when(a).getLocalName();
      doReturn(name).when(a).getName();
      doReturn(value).when(a).getValue();
      doReturn(value).when(a).getNodeValue();
      doReturn(null).when(a).getPrefix();
      doReturn(a).when(a).cloneNode(anyBoolean());
      doReturn(true).when(a).getSpecified();
      doReturn(e).when(a).getOwnerElement();
      doReturn(d).when(a).getOwnerDocument();

      doReturn(true).when(e).hasAttribute(name);
      doReturn(a).when(e).getAttributeNode(name);
      doReturn(value).when(e).getAttribute(name);
      doReturn(value).when(e).getAttributeNS(anyString(), eq(name));
      doReturn(a).when(e).getAttributeNodeNS(anyString(), eq(name));

      doReturn(a).when(nm).getNamedItem(name);
      doReturn(a).when(nm).getNamedItemNS(anyString(), eq(name));
      doReturn(a).when(nm).item(i);
    }

    doReturn(nm).when(e).getAttributes();

    doReturn(e).when(d).getFirstChild();
    doReturn(e).when(d).getDocumentElement();

    return d;
  }

  /**
   * Returns an {@link Answer} that throws an exception, for getting stacktraces when unmocked
   * methods are invoked.
   * 
   * @return
   */
  private static final <T> Answer<T> throwingAnswer() {
    return new Answer<T>() {
      @Override
      public T answer(InvocationOnMock invocation) {
        throw new UnsupportedOperationException("Unmocked method: " + invocation.getMethod()
            + " on mock of class: " + invocation.getMock().getClass());
      };
    };
  }

  private static String join(String delim, String... vals) {
    if (vals == null) {
      return null;
    }
    else if (vals.length == 0) {
      return "";
    }
    else {
      if (delim == null) {
        delim = "";
      }
      StringBuilder ret = new StringBuilder(vals[0]);
      for (int i = 1; i < vals.length; i++) {
        ret.append(delim).append(vals[i]);
      }
      return ret.toString();
    }
  }
}