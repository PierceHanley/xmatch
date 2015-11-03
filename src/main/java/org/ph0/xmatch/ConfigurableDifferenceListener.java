package org.ph0.xmatch;

import java.util.Set;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.ph0.xmatch.XmlEquivalenceMatcher.Setting;
import org.w3c.dom.Node;

/**
 * {@link DifferenceListener} implementation that reflects the provided {@link Setting} values.
 * 
 * @author phanley
 */
class ConfigurableDifferenceListener implements DifferenceListener {
  private final Set<Setting> settings;

  ConfigurableDifferenceListener(Set<Setting> settings) {
    this.settings = settings;
  }

  @Override
  public int differenceFound(Difference difference) {
    if (difference.equals(DifferenceConstants.ATTR_SEQUENCE)) {
      if (settings.contains(Setting.IGNORE_ATTRIBUTE_ORDER)) {
        return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
      }
      else {
        return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
      }
    }
    else if (difference.equals(DifferenceConstants.NAMESPACE_PREFIX)) {
      if (settings.contains(Setting.TOLERATE_DIFFERENT_NAMESPACE_PREFIXES)) {
        return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
      }
      else {
        return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
      }
    }
    else if (difference.equals(DifferenceConstants.COMMENT_VALUE)) {
      if (settings.contains(Setting.IGNORE_COMMENTS)) {
        return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
      }
      else {
        return RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
      }
    }
    else {
      return RETURN_ACCEPT_DIFFERENCE;
    }
  }

  @Override
  public void skippedComparison(Node control, Node test) {}
}