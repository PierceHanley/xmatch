package org.ph0.xmatch;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.DifferenceListener;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Hamcrest {@link Matcher} allowing fluent comparisons of different XML documents.
 * 
 * TODO: right now this is coupled to {@link XmlMatcherValue}'s use of SAX {@link InputSource}s. It
 * should be extended to support other common XML types, such as DOM {@link Document}s and JAXP
 * {@link Source}s.
 * 
 * @author phanley
 */
public class XmlEquivalenceMatcher extends TypeSafeDiagnosingMatcher<XmlMatcherValue> {
  /**
   * Settings controlling the behavior/strictness of the XML matcher.
   * 
   * TODO: clean up nomenclature
   * 
   * @author phanley
   */
  public static enum Setting {
    ONLY_COMPARE_SIMILARITY, IGNORE_ATTRIBUTE_ORDER, IGNORE_COMMENTS,
    IGNORE_CDATA_TEXT_DISTINCTION, IGNORE_LEADING_TRAILING_WHITESPACE,
    TOLERATE_DIFFERENT_NAMESPACE_PREFIXES, NORMALIZE_DOCUMENT, NORMALIZE_WHITESPACE
  }


  /**
   * Reasonable default settings for XML comparisons. Relatively lax, only the "content" parts of
   * the different documents are compared. Whitespace, attribute order, or differences between
   * namespace prefixes are all ignored.
   */
  private static final Set<Setting> DEFAULT_SETTINGS =
      Collections.unmodifiableSet(EnumSet.of(Setting.IGNORE_ATTRIBUTE_ORDER,
          Setting.IGNORE_COMMENTS, Setting.IGNORE_CDATA_TEXT_DISTINCTION,
          Setting.IGNORE_LEADING_TRAILING_WHITESPACE, Setting.TOLERATE_DIFFERENT_NAMESPACE_PREFIXES,
          Setting.NORMALIZE_DOCUMENT, Setting.NORMALIZE_WHITESPACE));

  /**
   * @return the default settings for XML equivalence
   */
  public static final Set<Setting> defaultSettings() {
    return DEFAULT_SETTINGS;
  }

  private final XmlMatcherValue expectedValue;
  private final Set<Setting> settings;

  /**
   * Create a matcher for the specified XML value using the default settings.
   * 
   * @param expectedValue
   * @return
   */
  public static final XmlEquivalenceMatcher defaultMatcherFor(XmlMatcherValue expectedValue) {
    return new XmlEquivalenceMatcher(expectedValue, defaultSettings());
  }

  protected XmlEquivalenceMatcher(XmlMatcherValue expectedValue, Set<Setting> settings) {
    this.expectedValue = expectedValue;
    
    this.settings = Collections.unmodifiableSet(
        settings.isEmpty() ? EnumSet.noneOf(Setting.class) : EnumSet.copyOf(settings));
  }
  
  /**
   * Create a copy of this matcher, but with the specified additional setting(s) enabled.
   * 
   * @param settings
   * @return
   */
  public XmlEquivalenceMatcher enabling(Setting... settings) {
    if (settings == null || settings.length == 0) {
      return this;
    }

    EnumSet<Setting> newSettings = EnumSet.copyOf(this.settings);
    newSettings.addAll(Arrays.asList(settings));
    return new XmlEquivalenceMatcher(this.expectedValue, Collections.unmodifiableSet(newSettings));
  }

  /**
   * Create a copy of this matcher, but with the specified additional setting(s) disabled.
   * 
   * @param settings
   * @return
   */
  public XmlEquivalenceMatcher disabling(Setting... settings) {
    if (settings == null || settings.length == 0) {
      return this;
    }
    EnumSet<Setting> newSettings = EnumSet.copyOf(this.settings);
    newSettings.removeAll(Arrays.asList(settings));
    return new XmlEquivalenceMatcher(this.expectedValue, Collections.unmodifiableSet(newSettings));
  }

  @Override
  public void describeTo(Description description) {
    String comparisonType =
        settings.contains(Setting.ONLY_COMPARE_SIMILARITY) ? "similar" : "identical";
    description.appendText("XML content " + comparisonType + " to ")
        .appendDescriptionOf(this.expectedValue);
  }

  /**
   * Run the matching operation. Uses {@link #initializeDiff(XmlMatcherValue, XmlMatcherValue)} and
   * {@link #configureDiff(Diff)} to create and extend the {@link Diff} used for matching.
   */
  @Override
  protected final boolean matchesSafely(XmlMatcherValue testValue,
      Description mismatchDescription) {

    try (XMLUnitContext context = new XMLUnitContext(this.settings)) {
      Diff diff;
      diff = initializeDiff(expectedValue, testValue);
      diff = configureDiff(diff);

      boolean success = false;
      if (settings.contains(Setting.ONLY_COMPARE_SIMILARITY)) {
        success = diff.similar();
      }
      else {
        success = diff.identical();
      }

      if (!success) {
        StringBuffer diffMessage = new StringBuffer();
        diff.appendMessage(diffMessage);
        mismatchDescription.appendText(diffMessage.toString());
        mismatchDescription.appendDescriptionOf(testValue);
      }
      return success;
    }
  }

  /**
   * Create an initialized {@link Diff} based on the specified {@link XmlMatcherValue}s. By default,
   * simply calls {@link Diff#Diff(org.xml.sax.InputSource, org.xml.sax.InputSource)} using the
   * results of {@link XmlMatcherValue#get()}. Subclasses may override this method to control the
   * {@code Diff} construction.
   * 
   * @param controlValue the XML value with which this matcher was originally created (in other
   *        words, the "expected" value).
   * @param testValue the value being tested (this would be the first parameter to
   *        {@link MatcherAssert#assertThat(Object, Matcher)}).
   * @return
   */
  protected Diff initializeDiff(XmlMatcherValue controlValue, XmlMatcherValue testValue) {
    try {
      return new Diff(controlValue.get(), testValue.get());
    }
    catch (IOException | SAXException e) {
      throw new RuntimeException("Exception occurred while initializing XML matcher values.", e);
    }
  }

  /**
   * Create a {@link Diff} object for comparing XML values. It requires a "base" {@code Diff} that
   * has already been populated with the values, and may return that same object with appropriate
   * alterations, or a new {@code Diff} object entirely.
   * 
   * By default, it wraps the {@code Diff} object in a {@link DetailedDiff} for better comparison
   * output, then uses {@link DetailedDiff#overrideDifferenceListener(DifferenceListener)} to
   * replacing the default behavior with a {@link ConfigurableDifferenceListener} for the settings
   * of this matcher.
   * 
   * @param control
   * @param test
   * @return
   */
  protected Diff configureDiff(Diff baseDiff) {
    ConfigurableDifferenceListener diffListener = new ConfigurableDifferenceListener(settings);
    baseDiff.overrideDifferenceListener(diffListener);
    Diff ret = new DetailedDiff(baseDiff);
    ret.overrideDifferenceListener(diffListener);
    return ret;
  }
}
