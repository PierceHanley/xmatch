package org.ph0.xmatch;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.SelfDescribing;
import org.xml.sax.InputSource;

/**
 * Holder for an XML value that can be matched by an {@link XmlEquivalenceMatcher}. Can represent
 * that value as a new SAX {@link InputSource} on demand. A brief description of the origin of the
 * XML document, and a friendly textual representation of its contents, may also be supplied...
 * these can be translated into a full textual description of the matched value using
 * {@link #toString()} or appended to a {@link Description} using {@link #describeTo(Description)}.
 * 
 * @author phanley
 */
public class XmlMatcherValue implements SelfDescribing, InputSourceSupplier {
  private final InputSourceSupplier inputSourceSupplier;
  private final String sourceDescription;
  private final String valueText;

  protected XmlMatcherValue(final CharSequence xmlText, String sourceDescription,
      String valueText) {

    this(sourceDescription, valueText, new InputSourceSupplier() {
      final String xmlTextStr = xmlText.toString();

      @Override
      public InputSource get() {
        return new InputSource(new StringReader(xmlTextStr));
      }
    });
  }

  protected XmlMatcherValue(final byte[] xmlBytes, String sourceDescription, String valueText) {

    this(sourceDescription, valueText, new InputSourceSupplier() {
      final byte[] xmlByteArray = Arrays.copyOf(xmlBytes, xmlBytes.length);

      @Override
      public InputSource get() {
        return new InputSource(new ByteArrayInputStream(xmlByteArray));
      }
    });
  }

  protected XmlMatcherValue(String sourceDescription, String valueText,
      InputSourceSupplier inputSourceSupplier) {

    this.sourceDescription = sourceDescription;
    this.valueText = valueText;
    this.inputSourceSupplier = inputSourceSupplier;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(this.toString());
  }

  @Override
  public InputSource get() {
    return this.inputSourceSupplier.get();
  }

  @Override
  public String toString() {
    String ret = valueText;
    if (sourceDescription != null) {
      ret = sourceDescription + ":\n" + ret;
    }
    return ret;
  }
}
