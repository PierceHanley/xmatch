package org.ph0.xmatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.ph0.xmatch.XmlMatchers.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.ph0.xmatch.XmlEquivalenceMatcher.Setting;

@RunWith(Parameterized.class)
public class XmlEquivalenceSettingSimpleTests {

  @Parameters(
      name = "{3} causes equivalence to be {4} when enabled and {5} when disabled. [comparing {0}]")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
      {
        "simple elements with different comments",
        xmlText("<test><!-- first comment -->xyz</test>"),
        xmlText("<test><!-- second comment -->xyz</test>"),
        Setting.IGNORE_COMMENTS, true, false
      },
      {
        "elements with different leading and trailing whitespace",
        xmlText("<test>abc def</test>"),
        xmlText("<test>    abc def    </test>"),
        Setting.IGNORE_LEADING_TRAILING_WHITESPACE, true, false
      },
      {
        "semantically equivalent elements, one with a CDATA section",
        xmlText("<test>abc def</test>"),
        xmlText("<test><![CDATA[abc]]> def</test>"),
        Setting.IGNORE_CDATA_TEXT_DISTINCTION, true, false
      },
      {
        "elements with different leading, trailing, and interior whitespace",
        xmlText("<test>abc def</test>"),
        xmlText("<test>   abc    def   </test>"),
        Setting.NORMALIZE_WHITESPACE, true, false
      },
      {
        "attributes having different prefixes, but the same namespace URI",
        xmlText("<test xmlns:abc=\"http://example.com/ns\"><abc:elem>123</abc:elem></test>"),
        xmlText("<test xmlns:def=\"http://example.com/ns\"><def:elem>123</def:elem></test>"),
        Setting.TOLERATE_DIFFERENT_NAMESPACE_PREFIXES, true, false
      },
      {
        "attributes with different prefixes, and different namespace URIs",
        xmlText("<test xmlns:abc=\"http://example.com/ns/abc\"><abc:elem>123</abc:elem></test>"),
        xmlText("<test xmlns:def=\"http://example.com/ns/def\"><def:elem>123</def:elem></test>"),
        Setting.TOLERATE_DIFFERENT_NAMESPACE_PREFIXES, false, false
      },
      {
        "attributes with the same prefix, but different namespace URIs",
        xmlText("<test xmlns:abc=\"http://example.com/ns/abc1\"><abc:elem>123</abc:elem></test>"),
        xmlText("<test xmlns:abc=\"http://example.com/ns/abc2\"><abc:elem>123</abc:elem></test>"),
        Setting.TOLERATE_DIFFERENT_NAMESPACE_PREFIXES, false, false
      }
    });
  }

  private final XmlMatcherValue testValue;
  private final XmlMatcherValue expectedValue;
  private final Setting setting;
  private final boolean enablingShouldMatch;
  private final boolean disablingShouldMatch;

  public XmlEquivalenceSettingSimpleTests(
      String description,
      XmlMatcherValue testValue,
      XmlMatcherValue expectedValue,
      Setting setting,
      boolean enablingShouldMatch,
      boolean disablingShouldMatch) {
    // ignore the description, it's there for JUnit's purposes
    this.testValue = testValue;
    this.expectedValue = expectedValue;
    this.setting = setting;
    this.enablingShouldMatch = enablingShouldMatch;
    this.disablingShouldMatch = disablingShouldMatch;
  }

  private void assertMatchBehavior(boolean enableSetting) {
    Set<Setting> settings = enableSetting ? EnumSet.of(setting) : EnumSet.noneOf(Setting.class);
    Matcher<XmlMatcherValue> xmlMatcher = new XmlEquivalenceMatcher(expectedValue, settings);

    // if [enabled should match] and [disabled for this test]
    // or [enabled should not match] and [enabled for this test]
    // negate the matcher
    boolean shouldBeEquivalent =
        (enableSetting && enablingShouldMatch) || (!enableSetting && disablingShouldMatch);

    if (!shouldBeEquivalent) {
      xmlMatcher = not(xmlMatcher);
    }

    assertThat(
        "when " + setting + " is " + (enableSetting ? "enabled" : "disabled") + " then XML content "
            + (shouldBeEquivalent ? "should" : "should not") + " be equivalent:",
        testValue, xmlMatcher);
  }

  @Test
  public void testSettingBehavior_enabled() {
    assertMatchBehavior(true);
  }

  @Test
  public void testSettingBehavior_disabled() {
    assertMatchBehavior(false);
  }
}
