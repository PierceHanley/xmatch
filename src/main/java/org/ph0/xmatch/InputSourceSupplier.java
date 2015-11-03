package org.ph0.xmatch;

import org.xml.sax.InputSource;

/**
 * Would prefer to use Guava's {@link com.google.common.base.Supplier} or Java 8's
 * {@link java.util.function.Supplier Supplier}, but in the interest of being a general-purpose
 * test library with few dependencies we'll just roll our own here.
 * @author phanley
 */
interface InputSourceSupplier {
  public InputSource get();
}