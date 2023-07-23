package de.bottlecaps.markup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.parser.Parser;

public class BlitzTest {
  public static String resourceContent(String resource) {
    URL url = BlitzTest.class.getClassLoader().getResource(resource);
    try {
      return Blitz.urlContent(url);
    }
    catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Test
  public void testIxml() {
    Parser parser = Blitz.generate(resourceContent("ixml.ixml"));
    String xml = parser.parse(resourceContent("ixml.ixml"), BlitzOption.INDENT);
    assertEquals(resourceContent("ixml.xml"), xml);
  }

  @Test
  public void testAddress() {
    Parser parser = Blitz.generate(resourceContent("address.ixml"));
    String xml = parser.parse(resourceContent("address.input"), BlitzOption.INDENT);
    assertEquals(resourceContent("address.xml"), xml);
  }

  @Test
  public void testJson() {
    Parser parser = Blitz.generate(resourceContent("json.ixml"));
    String result = parser.parse(resourceContent("sample.json"));
    String expectedResult = "<json><map><member><key>string</key><value><string>Hello, World!</string></value></member><member><key>number</key><value><number>42</number></value></member><member><key>boolean</key><value><boolean>true</boolean></value></member><member><key>nullValue</key><value><null/></value></member><member><key>arrayEmpty</key><value><array/></value></member><member><key>arraySingle</key><value><array><number>1</number></array></value></member><member><key>arrayMultiple</key><value><array><number>1</number><number>2</number></array></value></member><member><key>object</key><value><map><member><key>property</key><value><string>value</string></value></member></map></value></member><member><key>escapedString</key><value><string>This string contains escape sequences: \\\" \\\\ \\/ \\b \\f \\n \\r \\t</string></value></member><member><key>unicodeString</key><value><string><unicode code=\"20AC\"/></string></value></member></map></json>";
    assertEquals(expectedResult, result);
  }

  @Test
  public void testArith() {
    Parser parser = Blitz.generate(resourceContent("arith.ixml"));
    String result = parser.parse(resourceContent("arith.input"), BlitzOption.INDENT);
    assertEquals(resourceContent("arith.xml"), result);
  }

  @Test
  public void testAttributeValue() {
    Parser parser = Blitz.generate(
          "test: a, \".\".\n"
        + "@a: ~[\".\"]*.");
    String result = parser.parse(
        "\"'<>/&.");
    assertEquals("<test a=\"&quot;'&lt;&gt;/&amp;\">.</test>", result);
  }

  @Test
  public void testAttributeMultipart() {
    Parser parser = Blitz.generate(
          "date: month, -',', -' '*, year . \n"
        + "@month: 'Feb', 'ruary' .\n"
        + "year: ['0'-'9']+ .");
    String result = parser.parse(
        "February, 2022",
        BlitzOption.INDENT);
    assertEquals(
          "<date month=\"February\">\n"
        + "   <year>2022</year>\n"
        + "</date>",
        result);
  }

  @Test
  public void testDiary() {
    Parser parser = Blitz.generate(
          "diary: entry+.\n"
        + "entry: date, para.\n"
        + "date: day, s, month, s,  year, nl.\n"
        + "day: digit, digit?.\n"
        + "-digit:[\"0\"-\"9\"].\n"
        + "month: \"January\"; \"February\"; \"March\"; \"April\"; \"May\"; \"June\";\n"
        + "       \"July\"; \"August\"; \"September\"; \"October\"; \"November\"; \"December\".\n"
        + "year: digit, digit, digit, digit.\n"
        + "\n"
        + "para: word++s, s?, blank.\n"
        + "-blank: nl, nl.\n"
        + "-word: (letter; punctuation)+.\n"
        + "-letter: [L].\n"
        + "-punctuation: [\".;:,'?!\"].\n"
        + "-s: \" \"+.\n"
        + "-nl: -#a | -#d, -#a .");
    String result = parser.parse(
          "24 December 2021\n"
        + "Panic shopping! Panic packing! Will we make it before midnight?\n"
        + "\n"
        + "25 December 2021\n"
        + "Food! Presents!\n"
        + "\n"
        + "26 December 2021\n"
        + "Groan.\n"
        + "\n",
        BlitzOption.INDENT);
    assertEquals(
          "<diary>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>24</day> \n"
        + "         <month>December</month> \n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Panic shopping! Panic packing! Will we make it before midnight?</para>\n"
        + "   </entry>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>25</day> \n"
        + "         <month>December</month> \n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Food! Presents!</para>\n"
        + "   </entry>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>26</day> \n"
        + "         <month>December</month> \n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Groan.</para>\n"
        + "   </entry>\n"
        + "</diary>",
        result);
  }

  @Test
  public void testDiary2() {
    Parser parser = Blitz.generate(
          "diary: entry+.\n"
        + "entry: date, para.\n"
        + "date: day, s, month, s,  year, nl.\n"
        + "day: digit, digit?.\n"
        + "-digit:[\"0\"-\"9\"].\n"
        + "month: \"January\"; \"February\"; \"March\"; \"April\"; \"May\"; \"June\";\n"
        + "       \"July\"; \"August\"; \"September\"; \"October\"; \"November\"; \"December\".\n"
        + "year: digit, digit, digit, digit.\n"
        + "\n"
        + "para: char*, blank.\n"
        + "-blank: nl, nl.\n"
        + "-char: letter; punctuation; s.\n"
        + "-letter: [L].\n"
        + "-punctuation: [\".;:,'?!\"].\n"
        + "-s: \" \".\n"
        + "-nl: -#a | -#d, -#a .");
    String result = parser.parse(
          "24 December 2021\n"
        + "Panic shopping! Panic packing! Will we make it before midnight?\n"
        + "\n"
        + "25 December 2021\n"
        + "Food! Presents!\n"
        + "\n"
        + "26 December 2021\n"
        + "Groan.\n"
        + "\n",
        BlitzOption.INDENT);
    assertEquals(
          "<diary>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>24</day> \n"
        + "         <month>December</month> \n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Panic shopping! Panic packing! Will we make it before midnight?</para>\n"
        + "   </entry>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>25</day> \n"
        + "         <month>December</month> \n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Food! Presents!</para>\n"
        + "   </entry>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>26</day> \n"
        + "         <month>December</month> \n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Groan.</para>\n"
        + "   </entry>\n"
        + "</diary>",
        result);
  }

  @Test
  public void testDiary3() {
    Parser parser = Blitz.generate(
          "diary: entry+.\n"
        + "entry: date, para.\n"
        + "date: day, s, month, s,  year, nl.\n"
        + "-s: -\" \"+.\n"
        + "day: digit, digit?.\n"
        + "-digit:[\"0\"-\"9\"].\n"
        + "month: \"January\"; \"February\"; \"March\"; \"April\"; \"May\"; \"June\";\n"
        + "       \"July\"; \"August\"; \"September\"; \"October\"; \"November\"; \"December\".\n"
        + "year: digit, digit, digit, digit.\n"
        + "\n"
        + "para: char*, blank.\n"
        + "-blank: nl, nl.\n"
        + "-char: ~[#a].\n"
        + "-nl: -#a | -#d, -#a .");
    String result = parser.parse(
          "24 December 2021\n"
        + "Panic shopping! Panic packing! Will we make it before midnight?\n"
        + "\n"
        + "25 December 2021\n"
        + "Food! Presents!\n"
        + "\n"
        + "26 December 2021\n"
        + "Groan.\n"
        + "\n",
        BlitzOption.INDENT);
    assertEquals(
          "<diary>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>24</day>\n"
        + "         <month>December</month>\n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Panic shopping! Panic packing! Will we make it before midnight?</para>\n"
        + "   </entry>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>25</day>\n"
        + "         <month>December</month>\n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Food! Presents!</para>\n"
        + "   </entry>\n"
        + "   <entry>\n"
        + "      <date>\n"
        + "         <day>26</day>\n"
        + "         <month>December</month>\n"
        + "         <year>2021</year>\n"
        + "      </date>\n"
        + "      <para>Groan.</para>\n"
        + "   </entry>\n"
        + "</diary>",
        result);
  }

  @Test
  public void testEmail() {
    Parser parser = Blitz.generate(
          "email: user, -\"@\", host.\n"
        + "@user: atom++\".\".\n"
        + "-atom: char+.\n"
        + "@host: domain++\".\".\n"
        + "-domain: word++\"-\".\n"
        + "-word: letgit+.\n"
        + "-letgit: [\"A\"-\"Z\"; \"a\"-\"z\"; \"0\"-\"9\"].\n"
        + "-char:   letgit; [\"!#$%&'*+-/=?^_`{|}~\"].");
    String result = parser.parse(
        "~my_mail+{nospam}$?@sub-domain.example.info");
    assertEquals(
        "<email user=\"~my_mail+{nospam}$?\" host=\"sub-domain.example.info\"/>",
        result);
  }

  @Test
  public void testExpr() {
    Parser parser = Blitz.generate(
          "expression: expr.\n"
        + "-expr: term; sum; diff.\n"
        + "sum: expr, -\"+\", term.\n"
        + "diff: expr, \"-\", term.\n"
        + "-term: factor; prod; div.\n"
        + "prod: term, -\"\u00d7\", factor.\n"
        + "div: term, \"\u00f7\", factor.\n"
        + "-factor: id; number; bracketed.\n"
        + "bracketed: -\"(\", expr, -\")\".\n"
        + "id: @name.\n"
        + "name: letter+.\n"
        + "number: @value.\n"
        + "value: digit+.\n"
        + "-letter: [\"a\"-\"z\"].\n"
        + "-digit: [\"0\"-\"9\"].",
        BlitzOption.VERBOSE);
    String result = parser.parse(
        "pi+(10\u00d7b)",
        BlitzOption.INDENT);
    assertEquals(
          "<expression>\n"
        + "   <sum>\n"
        + "      <id name=\"pi\"/>\n"
        + "      <bracketed>\n"
        + "         <prod>\n"
        + "            <number value=\"10\"/>\n"
        + "            <id name=\"b\"/>\n"
        + "         </prod>\n"
        + "      </bracketed>\n"
        + "   </sum>\n"
        + "</expression>",
        result);
  }

//  @Test
//  public void test() {
//    Parser parser = Blitz.generate(
//        "");
//    String result = parser.parse(
//        "",
//        BlitzOption.INDENT);
//    assertEquals(
//        "",
//        result);
//  }
}
