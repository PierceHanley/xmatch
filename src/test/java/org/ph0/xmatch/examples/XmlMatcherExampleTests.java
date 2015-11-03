package org.ph0.xmatch.examples;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.ph0.xmatch.XmlMatchers.*;

import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;
import org.ph0.xmatch.XmlEquivalenceMatcher.Setting;

public class XmlMatcherExampleTests {
  @XmlRootElement(name="foo")
  public static class MyJaxbThing {
    public String bar = "hello world";
  }
  
  @Test
  public void testJaxbElement() {
    MyJaxbThing thing = new MyJaxbThing();
    assertThat(thing, isJaxbObject(equivalentTo("<foo> <bar> hello    world </bar> </foo>")));
    
    assertThat(thing, not(isJaxbObject(equivalentTo("<foo> <bar> hello    world </bar> </foo>")
        .disabling(Setting.NORMALIZE_WHITESPACE))));
  }
  
  @Test
  public void testJaxbToClasspathResource() {
    assertThat(new MyJaxbThing(),
        isJaxbObject(equivalentTo(xmlResource(XmlMatcherExampleTests.class, "foo.xml"))));
  }
  
  @Test
  public void testStringComparison() {
    assertThat("<foo>bar</foo>", isXmlText(equivalentTo("<foo>\nbar\n</foo>")));
  }
}
