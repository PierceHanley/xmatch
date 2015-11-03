## Synopsis

This library offers fluent JUnit/Hamcrest matchers for XML comparison, based on the functionality provided by the [XMLUnit](http://www.xmlunit.org/) project.

## Code Example

```java
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
```

## TODO

Error messages are currently a little messed up, especially for the JAXB calls.