package org.ph0.xmatch;

import static org.ph0.xmatch.XmlEquivalenceMatcher.Setting.*;
import static org.ph0.xmatch.XmlMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import org.ph0.xmatch.XmlEquivalenceMatcher.Setting;

public class XmlEquivalenceMatcherTest {
  @Test
  public void testXmlEquivalence_ignoringAttributeOrder() {
    Set<Setting> settings = EnumSet.of(IGNORE_ATTRIBUTE_ORDER);
    XmlMatcherValue placeholderValue = new XmlMatcherValue(
        "<test attrA='value A' attrB='value B'/>", "placeholder", "placeholder");
    XmlEquivalenceMatcher matcher = new XmlEquivalenceMatcher(placeholderValue, settings);

    Diff baseDiff =
        new Diff(DomMocker.mockDocumentElementWithAttrs("attrA", "value A", "attrB", "value B"),
            DomMocker.mockDocumentElementWithAttrs("attrB", "value B", "attrA", "value A"));
    Diff matcherDiff = matcher.configureDiff(baseDiff);

    boolean identical = matcherDiff.identical();
    String errMsg = null;
    if (!identical) {
      StringBuffer sb = new StringBuffer();
      matcherDiff.appendMessage(sb);
      errMsg = sb.toString();
    }
    assertThat("documents are identical if attribute order isn't enforced", errMsg,
        isEmptyOrNullString());
  }

  @Test
  public void testXmlEquivalence_enforcingAttributeOrder() {
    Set<Setting> settings = EnumSet.noneOf(Setting.class);
    XmlMatcherValue placeholderValue = new XmlMatcherValue(
        "<test attrA='value A' attrB='value B'/>", "placeholder", "placeholder");

    XmlEquivalenceMatcher matcher = new XmlEquivalenceMatcher(placeholderValue, settings);
    Diff baseDiff =
        new Diff(DomMocker.mockDocumentElementWithAttrs("attrA", "value A", "attrB", "value B"),
            DomMocker.mockDocumentElementWithAttrs("attrB", "value B", "attrA", "value A"));
    Diff matcherDiff = matcher.configureDiff(baseDiff);

    boolean identical = matcherDiff.identical();
    String errMsg = null;
    if (!identical) {
      StringBuffer sb = new StringBuffer();
      matcherDiff.appendMessage(sb);
      errMsg = sb.toString();
    }
    assertThat("documents aren't identical if attribute order is enforced", errMsg, notNullValue());
  }

  private Document newDomDocument() {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }
    catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private Document createUnnormalizedDomDocument(String... textNodes) {
    final Document testDoc = newDomDocument();
    Element testElem = testDoc.createElement("test");
    testDoc.appendChild(testElem);
    for (String text : textNodes) {
      Text node = testDoc.createTextNode(text);
      testElem.appendChild(node);
    }
    return testDoc;
  }

  /**
   * Create an {@link XmlEquivalenceMatcher} for semantically identical XML documents, distinguished
   * only by the use of a {@code <!CDATA![...]]>} block for the contents.
   * 
   * It's necessary to make this a more complex method using fake DOM {@link Document}s, because it
   * seems like most SAX {@link InputSource}s will normalize the difference away.
   * 
   * @param ignoreDifference whether to enable the {@link Setting#IGNORE_CDATA_TEXT_DISTINCTION}
   *        setting or not during this execution.
   */
  @Test
  public void testXmlEquivalence_normalizedDocument() {
    verifyNormalizedDocumentEquivalence(true, true);
    verifyNormalizedDocumentEquivalence(false, false);
  }


  /**
   * Create an {@link XmlEquivalenceMatcher} for semantically identical XML documents, distinguished
   * only by having multiple consecutive text nodes which have the same concatenated value.
   * 
   * It's necessary to make this a more complex method using fake DOM {@link Document}s, because it
   * seems like most SAX {@link InputSource}s will normalize the difference away anyway.
   * 
   * @param shouldNormalize whether to enable the {@link Setting#NORMALIZE_DOCUMENT} setting or not
   *        during this execution.
   * @param expectedResult whether the comparison corresponding to {@code shouldNormalize} is
   *        expected to be equivalent.
   */
  private void verifyNormalizedDocumentEquivalence(boolean shouldNormalize,
      boolean expectedResult) {
    final Document testDoc1 = createUnnormalizedDomDocument("a", "bcd", "ef");
    final Document testDoc2 = createUnnormalizedDomDocument("abc", "de", "f");

    // dummy matcher values
    final XmlMatcherValue value1 = new XmlMatcherValue("DOM document",
        "[manually constructed value 1]", new InputSourceSupplier() {
          @Override
          public InputSource get() {
            throw new UnsupportedOperationException();
          }
        });

    // dummy matcher values
    final XmlMatcherValue value2 = new XmlMatcherValue("DOM document",
        "[manually constructed value 2]", new InputSourceSupplier() {
          @Override
          public InputSource get() {
            throw new UnsupportedOperationException();
          }
        });

    Set<Setting> settings =
        shouldNormalize ? EnumSet.of(NORMALIZE_DOCUMENT) : EnumSet.noneOf(Setting.class);
    Matcher<XmlMatcherValue> matcher;
    matcher = new XmlEquivalenceMatcher(value1, settings) {
      @Override
      protected Diff initializeDiff(XmlMatcherValue controlValue, XmlMatcherValue testValue) {

        assertThat(controlValue, sameInstance(value1));
        assertThat(testValue, sameInstance(value2));
        return new Diff(testDoc1, testDoc2);
      }
    };

    if (!expectedResult) {
      matcher = not(matcher);
    }

    assertThat(value2, matcher);
  }

  @Test
  public void testXmlEquivalence_detectsMultipleDifferences() {
    XmlMatcherValue firstXml = xmlText("<test type='first'>" + "<a testNumber='1'>first</a>"
        + "<b>value:</b>" + "<c>ONE</c>" + "</test>");
    XmlMatcherValue secondXml = xmlText("<test>" // first difference: missing attribute
        + "<a testNumber='2'>second</a>" // second difference: different attribute value
        + "<b>value:</b>" + "<c>TWO</c>" // third difference: element content
        + "</test>");

    // need a holder so that we can retrieve the spied-on diff afterwards
    class DiffSpy {
      Diff spiedDiff = null;
    }
    final DiffSpy spy = new DiffSpy();

    XmlEquivalenceMatcher matcher =
        new XmlEquivalenceMatcher(firstXml, EnumSet.noneOf(Setting.class)) {
          @Override
          protected Diff configureDiff(Diff baseDiff) {
            spy.spiedDiff = spy(super.configureDiff(baseDiff));
            return spy.spiedDiff;
          }
        };

    assertThat(matcher.matches(secondXml), equalTo(false));

    /*
     * can't just "verify", because the matcher checks the difference twice (once to see if it
     * matches, and once to report the mismatch description
     */
    verify(spy.spiedDiff, atLeastOnce())
        .differenceFound(eq(DifferenceConstants.ATTR_NAME_NOT_FOUND));
    verify(spy.spiedDiff, atLeastOnce()).differenceFound(eq(DifferenceConstants.ATTR_VALUE));
    verify(spy.spiedDiff, atLeastOnce()).differenceFound(eq(DifferenceConstants.TEXT_VALUE));
  }

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
