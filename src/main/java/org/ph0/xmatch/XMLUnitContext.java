package org.ph0.xmatch;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import org.custommonkey.xmlunit.XMLUnit;

import org.ph0.xmatch.XmlEquivalenceMatcher.Setting;

/**
 * Locking context, for preventing multiple changes to XMLUnit's static configuration settings
 * across multiple executing instances of this class.
 * 
 * It seems too greedy/risky to lock on the XMLUnit class as a whole, but at least within this class
 * we want to be able to prevent conflicts and restore the previous settings after the tests
 * complete, so we don't corrupt other equivalence tests.
 */
/* package-private */ final class XMLUnitContext implements AutoCloseable {
  private static final Logger log = Logger.getLogger(XMLUnitContext.class.getName());
  
  /**
   * Lock allowing only one thread to have access to the XMLUnit static context.
   */
  private static final Lock XMLUNIT_STATIC_LOCK = new ReentrantLock();
  
  /**
   * Locking object to protect a single XMLUnitContext instance from concurrent {@link #close()}
   * invocations.
   */
  private final Object closeLock = new Object();

  // TODO: a less repetitive way to implement saving/changing/restoring the settings
  private final boolean prevIgnoringAttributeOrder;
  private final boolean prevIgnoringComments;
  private final boolean prevIgnoringTextCdataDiff;
  private final boolean prevIgnoringWhitespace;
  private final boolean prevNormalizing;
  private final boolean prevNormalizingWhitespace;
  private boolean isOpen = false;

  public XMLUnitContext(Set<Setting> settings) {
    if (settings == null) {
      throw new NullPointerException();
    }
    boolean locked = XMLUNIT_STATIC_LOCK.tryLock();
    if (!locked) {
      log.warning("Unable to immediately obtain lock on XMLUnit configuration state, "
          + "beware of thread contention between tests.  Trying again...");
      XMLUNIT_STATIC_LOCK.lock();
    }

    isOpen = true;

    // save the existing settings so they can be restored later
    prevIgnoringAttributeOrder = XMLUnit.getIgnoreAttributeOrder();
    prevIgnoringComments = XMLUnit.getIgnoreComments();
    prevIgnoringTextCdataDiff = XMLUnit.getIgnoreDiffBetweenTextAndCDATA();
    prevIgnoringWhitespace = XMLUnit.getIgnoreWhitespace();
    prevNormalizing = XMLUnit.getNormalize();
    prevNormalizingWhitespace = XMLUnit.getNormalizeWhitespace();

    // set the settings with the desired values
    XMLUnit.setIgnoreAttributeOrder(settings.contains(Setting.IGNORE_ATTRIBUTE_ORDER));
    XMLUnit.setIgnoreComments(settings.contains(Setting.IGNORE_COMMENTS));
    XMLUnit.setIgnoreDiffBetweenTextAndCDATA(settings.contains(Setting.IGNORE_CDATA_TEXT_DISTINCTION));
    XMLUnit.setIgnoreWhitespace(settings.contains(Setting.IGNORE_LEADING_TRAILING_WHITESPACE));
    XMLUnit.setNormalize(settings.contains(Setting.NORMALIZE_DOCUMENT));
    XMLUnit.setNormalizeWhitespace(settings.contains(Setting.NORMALIZE_WHITESPACE));
  }
  
  /*
   * Logic to close the XMLUnit context under various circumstances.  In a perfect world, we'd
   * refactor XMLUnit itself to not use a static state like this.  In this world, we're only likely
   * to be used in test code so it's more important to not cause major deadlocks and crashes rather
   * than preserving state integrity.
   */

  @Override
  public void close() {
    synchronized(closeLock) {
      if (isOpen) {
        RuntimeException runtimeException = null;
        try {
          // restore the original settings
          XMLUnit.setIgnoreAttributeOrder(prevIgnoringAttributeOrder);
          XMLUnit.setIgnoreComments(prevIgnoringComments);
          XMLUnit.setIgnoreDiffBetweenTextAndCDATA(prevIgnoringTextCdataDiff);
          XMLUnit.setIgnoreWhitespace(prevIgnoringWhitespace);
          XMLUnit.setNormalize(prevNormalizing);
          XMLUnit.setNormalizeWhitespace(prevNormalizingWhitespace);
        }
        catch (RuntimeException re) {
          runtimeException = re;
        }
        finally {
          isOpen = false;
          try {
            if (runtimeException != null) {
              Logger log = Logger.getLogger(XMLUnitContext.class.getName());
              log.severe("Exception occurred while closing XMLUnit context.  "
                  + "Releasing lock and rethrowing, but further XMLUnit behavior is undefined.");
              throw runtimeException;
            }
          }
          finally {
            XMLUNIT_STATIC_LOCK.unlock();
          }
        }
      }
      else {
        log.severe("Detected repeated attempt to close a single XMLUnitContext.");
      }
    }
  }

  /**
   * Would normally avoid overriding {@code finalize()}, but it's important to release the
   * {@link Lock} on the XMLUnit context in the event that this object isn't explicitly
   * {@link #close() closed}, so that it can at least try to avoid messing up other tests.
   * 
   * @see XmlEquivalenceMatcher#xmlUnitLock
   */
  @Override
  protected void finalize() throws Throwable {
    if (isOpen) {
      try {
        close();
      }
      finally {
        /* ugh with the nested try/catches, but it's really important not to do *anything* to
         * suppress finalization in the event that outputting the error throws an exception. */
        try {
          log.severe("XMLUnit context was not closed at time of finalization.");
        }
        catch (Exception e) {
          try {
            System.err.println("Exception while logging failure to close XMLUnit context:");
            e.printStackTrace();
          }
          catch (Exception e2) {
            // not much else we can do :(
          }
        }
      }
    }
    
    // only perform superclass finalization if we're actually going to be finalized ourselves
    super.finalize();
  }
}
