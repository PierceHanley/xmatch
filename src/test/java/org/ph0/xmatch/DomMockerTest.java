package org.ph0.xmatch;

import java.util.UUID;

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DomMockerTest {
  @Test
  public void testMockDocument_attributeOrderBehavior() {
    final int numAttrs = 15;
    String[] attrs = new String[numAttrs * 2];
    for (int i = 0; i < numAttrs; i++) {
      String id = UUID.randomUUID().toString();
      attrs[2 * i] = "attr" + id;
      attrs[2 * i + 1] = "value " + id;
    }
    Document mockDoc = DomMocker.mockDocumentElementWithAttrs(attrs);
    NamedNodeMap attrNodes = mockDoc.getDocumentElement().getAttributes();
    assertThat(attrNodes.getLength(), equalTo(numAttrs));
    for (int i = 0; i < numAttrs; i++) {
      Node curAttrNode = attrNodes.item(i);
      assertThat(curAttrNode, instanceOf(Attr.class));
      Attr curAttr = (Attr) attrNodes.item(i);
      assertThat(curAttr.getName(), equalTo(attrs[2 * i]));
      assertThat(curAttr.getValue(), equalTo(attrs[2 * i + 1]));
    }
  }
}
