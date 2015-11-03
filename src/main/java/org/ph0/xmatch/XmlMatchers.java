package org.ph0.xmatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.ph0.xmatch.XmlEquivalenceMatcher.Setting;
import org.xml.sax.InputSource;

/**
 * Utility class with static imports for XML-based unit test matchers.
 * 
 * @see org.ph0.xmatch.examples.XmlMatcherExampleTests
 * 
 * @author phanley
 *
 */
public class XmlMatchers {
  
  public static <T> Matcher<? super T> isXml(final XmlEquivalenceMatcher matcher) {
    return (Matcher<? super T>) new CustomMatcher<Object>("") {
      @Override
      public boolean matches(Object item) {
        if (item instanceof CharSequence) {
          return matcher.matches(xmlText((CharSequence) item));
        }
        else if (item instanceof URL) {
          return matcher.matches(xmlAt((URL) item));
        }
        else if (item instanceof URI) {
          return matcher.matches(xmlAt((URI) item));
        }
        else if (item instanceof byte[]) {
          return matcher.matches(xmlAt((URL) item));
        }
        return false;
      }
    };
  }
  
  public static <T> Matcher<? super T> isJaxbObject(final XmlEquivalenceMatcher matcher) {
    return new TypeSafeMatcher<Object>() {
      @Override
      public boolean matchesSafely(Object jaxbObject) {
        return matcher.matches(jaxbXmlFor(jaxbObject));
      }

      @Override
      public void describeTo(Description description) {
        matcher.describeTo(description);
      }
    };
  }
  
  public static Matcher<CharSequence> isXmlText(final XmlEquivalenceMatcher matcher) {
    return new TypeSafeMatcher<CharSequence>() {
      @Override
      public boolean matchesSafely(CharSequence xml) {
        return matcher.matches(xmlText(xml));
      }

      @Override
      public void describeTo(Description description) {
        matcher.describeTo(description);
      }
    };
  }
  
  public static final XmlEquivalenceMatcher equivalentTo(XmlMatcherValue value) {
    return XmlEquivalenceMatcher.defaultMatcherFor(value);
  }

  public static final XmlEquivalenceMatcher similarTo(XmlMatcherValue value) {
    return XmlEquivalenceMatcher.defaultMatcherFor(value).enabling(Setting.ONLY_COMPARE_SIMILARITY);
  }

  public static final XmlEquivalenceMatcher equivalentTo(CharSequence value) {
    return XmlEquivalenceMatcher.defaultMatcherFor(xmlText(value));
  }

  public static final XmlEquivalenceMatcher similarTo(CharSequence value) {
    return XmlEquivalenceMatcher.defaultMatcherFor(xmlText(value))
        .enabling(Setting.ONLY_COMPARE_SIMILARITY);
  }

  public static final XmlMatcherValue xmlText(CharSequence xmlString) {
    String str = xmlString.toString();
    return new XmlMatcherValue(str, "XML text", str);
  }

  public static final XmlMatcherValue jaxbXml(JAXBElement<?> jaxbObj) {
    return jaxbXmlFor(jaxbObj, jaxbObj.getDeclaredType());
  }

  public static final XmlMatcherValue jaxbXmlFor(Object obj) {
    return jaxbXmlFor(obj, obj.getClass());
  }

  public static final XmlMatcherValue jaxbXmlFor(Object obj, Class<?> jaxbType) {
    StringWriter xmlSink = new StringWriter();
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(jaxbType);
      final Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(obj, xmlSink);
    }
    catch (JAXBException je) {
      throw new IllegalStateException(
          "Error occured during marshalling of " + jaxbType + " object for matching.", je);
    }
    String xmlStr = xmlSink.toString();
    return new XmlMatcherValue(xmlStr, "JAXB object of type " + jaxbType, xmlStr);
  }

  public static final XmlMatcherValue xmlAt(URL url) {
    try (InputStream urlStream = url.openStream()) {
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      byte[] data = new byte[1024];
      int bytesRead = 0;
      while ((bytesRead = urlStream.read(data)) != -1) {
        bytesOut.write(data, 0, bytesRead);
      }
      byte[] urlBytes = bytesOut.toByteArray();
      
      return new XmlMatcherValue(urlBytes, "XML document at URL \"" + url + "\"",
          prettyXml(new InputSource(new ByteArrayInputStream(urlBytes))));
    }
    catch (IOException ioe) {
      throw new RuntimeException("I/O exception occurred while reading from URL: " + url, ioe);
    }
  }

  public static final XmlMatcherValue xmlAt(URI uri) {
    try {
      return xmlAt(uri.toURL());
    }
    catch (MalformedURLException mue) {
      throw new RuntimeException(
          "Unable to match XML at URI \"" + uri + "\" because it represents a malformed URL.", mue);
    }
  }

  public static final XmlMatcherValue xmlResource(Class<?> loadingClass, String path) {
    return xmlAt(loadingClass.getResource(path));
  }

  private static final String prettyXml(InputSource source) {
    StringWriter sw = new StringWriter();
    try {
      TransformerFactory.newInstance().newTransformer().transform(new SAXSource(source),
          new StreamResult(sw));
      return sw.toString();
    }
    catch (TransformerException te) {
      throw new RuntimeException("Unable to render input source as pretty XML due to an exception.",
          te);
    }
  }

}
