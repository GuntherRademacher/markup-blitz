package de.bottlecaps.markup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.parser.Parser;

public class BlitzTest extends TestBase {

  @Test
  public void testEmpty() {
    Parser parser = Blitz.generate("S: .");
    String result = parser.parse("");
    assertEquals("<S/>", result);
  }

  @Test
  public void testEmptyCharset() {
    Blitz.generate("S: [], 'a'.");
  }

  @Test
  public void testInfinite() {
    Blitz.generate("s: -s.");
  }

  @Test
  public void testInsertion() {
    Parser parser = Blitz.generate(
        "S: +'a', +'b', 'c', +'d', +'e', -'f', +'g', +'h', 'i', +'j', +'k', + 'l', 'm'.");
    String result = parser.parse(
        "cfim",
        BlitzOption.INDENT);
    assertEquals(
          "<S>abcdeghijklm</S>",
        result);
  }

  @Test
  @Disabled // runs out of memory
  public void testAmbiguousInsertion() {
    Parser parser = Blitz.generate(
        "S:'a', +'a'+.");
    String result = parser.parse(
        "a",
        BlitzOption.TRACE);
    assertEquals(
        "",
        result);
  }

  @Test
  public void testAmbiguity1() {
    Parser parser = Blitz.generate("S: 'a', 'b'+; 'a'+, 'b'.");
    String result = parser.parse("ab");
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">ab</S>", result);
  }

  @Test
  public void testAmbiguity2() {
    Parser parser = Blitz.generate("S: 'a', 'b'+, 'c'; 'a'+, 'b', 'c'.");
    String result = parser.parse("abc");
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">abc</S>", result);
  }

  @Test
  public void testCss() {
    Parser parser = Blitz.generate(
          "     css = S, rule+.\n"
        + "    rule = selector, block.\n"
        + "   block = -\"{\", S, property**(-\";\", S), -\"}\", S.\n"
        + "property =  @name, S, -\":\", S, value | empty.\n"
        + "selector = name, S.\n"
        + "    name = letter+.\n"
        + " -letter = [\"a\"-\"z\" | \"-\"].\n"
        + "   digit = [\"0\"-\"9\"].\n"
        + "   value = (@name | @number), S.\n"
        + "  number = digit+.\n"
        + "  -empty = .\n"
        + "      -S = -[\" \" | #a]*.");
    String result = parser.parse(
        "p { }",
        BlitzOption.INDENT);
    Set<String> expectedResults = Set.of(
        "<css xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">\n"
        + "   <rule>\n"
        + "      <selector>\n"
        + "         <name>p</name>\n"
        + "      </selector>\n"
        + "      <block>\n"
        + "         <property/>\n"
        + "      </block>\n"
        + "   </rule>\n"
        + "</css>",
          "<css xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">\n"
        + "   <rule>\n"
        + "      <selector>\n"
        + "         <name>p</name>\n"
        + "      </selector>\n"
        + "      <block/>\n"
        + "   </rule>\n"
        + "</css>");
    assertTrue(expectedResults.contains(result),
        "unexpected result: " + result);
  }

  @Test
  public void testIxml() {
    Parser parser = Blitz.generate(resourceContent("ixml.ixml"), BlitzOption.INDENT); // , BlitzOption.TIMING);
    String xml = parser.parse(resourceContent("ixml.ixml"));
    assertEquals(resourceContent("ixml.xml"), xml);
  }

  @Test
  public void testJson() {
    Parser parser = Blitz.generate(resourceContent("json.ixml")); // , BlitzOption.TIMING, BlitzOption.TRACE, BlitzOption.VERBOSE);
    String result = parser.parse(resourceContent("sample.json"));
    String expectedResult = resourceContent("sample.json.xml");
    assertEquals(expectedResult, result);
  }

  @Test
  public void testAddress() {
    Parser parser = Blitz.generate(resourceContent("address.ixml"));
    String xml = parser.parse(resourceContent("address.input"), BlitzOption.INDENT);
    assertEquals(resourceContent("address.xml"), xml);
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
        + "-digit: [\"0\"-\"9\"].");
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

  @Test
  public void testFrege() {
    Parser parser = Blitz.generate(
        resourceContent("frege.ixml"),
        BlitzOption.INDENT);
    assertEquals(
          "<formula>\n"
        + "   <maybe>\n"
        + "      <conditional>\n"
        + "         <consequent>\n"
        + "            <leaf>\n"
        + "               <var>Α</var>\n"
        + "            </leaf>\n"
        + "         </consequent>\n"
        + "         <antecedent>\n"
        + "            <leaf>\n"
        + "               <var>Β</var>\n"
        + "            </leaf>\n"
        + "         </antecedent>\n"
        + "      </conditional>\n"
        + "   </maybe>\n"
        + "</formula>",
        parser.parse("Alpha if Beta"));
    assertEquals(
          "<formula>\n"
          + "   <maybe>\n"
          + "      <leaf>\n"
          + "         <fa>\n"
          + "            <functor>\n"
          + "               <var>Ψ</var>\n"
          + "            </functor>\n"
          + "            <arg>\n"
          + "               <var>Α</var>\n"
          + "            </arg>\n"
          + "            <arg>\n"
          + "               <var>Β</var>\n"
          + "            </arg>\n"
          + "         </fa>\n"
          + "      </leaf>\n"
          + "   </maybe>\n"
          + "</formula>",
        parser.parse("Psi(Alpha, Beta)"));
    assertEquals(
            "<inference>\n"
          + "   <premise>\n"
          + "      <formula>\n"
          + "         <yes>\n"
          + "            <conditional>\n"
          + "               <consequent>\n"
          + "                  <leaf>\n"
          + "                     <var>Α</var>\n"
          + "                  </leaf>\n"
          + "               </consequent>\n"
          + "               <antecedent>\n"
          + "                  <leaf>\n"
          + "                     <var>Β</var>\n"
          + "                  </leaf>\n"
          + "               </antecedent>\n"
          + "            </conditional>\n"
          + "         </yes>\n"
          + "      </formula>\n"
          + "   </premise>\n"
          + "   <premise>\n"
          + "      <formula>\n"
          + "         <yes>\n"
          + "            <leaf>\n"
          + "               <var>Β</var>\n"
          + "            </leaf>\n"
          + "         </yes>\n"
          + "      </formula>\n"
          + "   </premise>\n"
          + "   <infstep>\n"
          + "      <conclusion>\n"
          + "         <formula>\n"
          + "            <yes>\n"
          + "               <leaf>\n"
          + "                  <var>Α</var>\n"
          + "               </leaf>\n"
          + "            </yes>\n"
          + "         </formula>\n"
          + "      </conclusion>\n"
          + "   </infstep>\n"
          + "</inference>",
        parser.parse(
            "we have: yes Alpha if Beta\n"
          + "    and: yes Beta\n"
          + "from which we infer: yes Alpha."));
    assertEquals(
          "<inference>\n"
        + "   <premise>\n"
        + "      <formula>\n"
        + "         <yes>\n"
        + "            <conditional>\n"
        + "               <consequent>\n"
        + "                  <leaf>\n"
        + "                     <var>Α</var>\n"
        + "                  </leaf>\n"
        + "               </consequent>\n"
        + "               <antecedent>\n"
        + "                  <leaf>\n"
        + "                     <var>Β</var>\n"
        + "                  </leaf>\n"
        + "               </antecedent>\n"
        + "            </conditional>\n"
        + "         </yes>\n"
        + "      </formula>\n"
        + "   </premise>\n"
        + "   <infstep>\n"
        + "      <premise-ref-ant>\n"
        + "         <ref>XX</ref>\n"
        + "      </premise-ref-ant>\n"
        + "      <conclusion>\n"
        + "         <formula>\n"
        + "            <yes>\n"
        + "               <leaf>\n"
        + "                  <var>Α</var>\n"
        + "               </leaf>\n"
        + "            </yes>\n"
        + "         </formula>\n"
        + "      </conclusion>\n"
        + "   </infstep>\n"
        + "</inference>",
      parser.parse(
            "we have: yes Alpha if Beta,\n"
          + "from which via (XX)::\n"
          + "we infer: yes Alpha."));
    assertEquals(
          "<formula>\n"
        + "   <maybe>\n"
        + "      <not>\n"
        + "         <univ bound-var=\"𝔡\">\n"
        + "            <not>\n"
        + "               <univ bound-var=\"𝔞\">\n"
        + "                  <leaf>\n"
        + "                     <fa>\n"
        + "                        <functor>\n"
        + "                           <var>Φ</var>\n"
        + "                        </functor>\n"
        + "                        <arg>\n"
        + "                           <bound-var>𝔞</bound-var>\n"
        + "                        </arg>\n"
        + "                        <arg>\n"
        + "                           <bound-var>𝔡</bound-var>\n"
        + "                        </arg>\n"
        + "                     </fa>\n"
        + "                  </leaf>\n"
        + "               </univ>\n"
        + "            </not>\n"
        + "         </univ>\n"
        + "      </not>\n"
        + "   </maybe>\n"
        + "</formula>",
      parser.parse(
          "not all fd satisfy not all fa satisfy Phi(fa, fd)"));
//    assertEquals(
//        "",
//      parser.parse(
//          ""));
    assertEquals(
          "<formula>\n"
        + "   <maybe>\n"
        + "      <conditional>\n"
        + "         <consequent>\n"
        + "            <univ bound-var=\"𝔞\">\n"
        + "               <leaf>\n"
        + "                  <var>Α</var>\n"
        + "               </leaf>\n"
        + "            </univ>\n"
        + "         </consequent>\n"
        + "         <antecedent>\n"
        + "            <leaf>\n"
        + "               <var>Β</var>\n"
        + "            </leaf>\n"
        + "         </antecedent>\n"
        + "      </conditional>\n"
        + "   </maybe>\n"
        + "</formula>",
        parser.parse("all fa satisfy Alpha if Beta"));
  }

  @Test
  public void testInsertSeparatorAlternate() {
    Parser parser = Blitz.generate(
        "S: [L]++(+':';+'=').");
    String result = parser.parse(
        "abc",
        BlitzOption.INDENT);
    Set<String> expectedResults = Set.of(
        "<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">a=b=c</S>",
        "<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">a:b:c</S>"
        );
    assertTrue(expectedResults.contains(result),
        "unexpected result: " + result);
  }

  @Test
  public void testCombinedLexicographicalAndNumericSortingCriteria() {
    Parser parser = Blitz.generate(
        "word : a,(space,b)?,(space, c)?;\n"
      + "             b.\n"
      + "a : [L]+.\n"
      + "c : [L]+.\n"
      + "b : -numeric.\n"
      + "numeric : [Nd]+.\n"
      + "-space : -[\" \"]*.",
      BlitzOption.VERBOSE);
    assertEquals(
        "<word><a>Chapter</a><b>10</b><c>a</c></word>",
        parser.parse("Chapter 10 a"));
    assertEquals(
        "<word><a>Chapter</a><b>1</b><c>B</c></word>",
        parser.parse("Chapter 1B"));
    assertEquals(
        "<word><a>Chapter</a><b>1</b><c>B</c></word>",
        parser.parse("Chapter 1 B"));
    assertEquals(
        "<word><a>Chapter</a><b>2</b><c>A</c></word>",
        parser.parse("Chapter 2A"));
    assertEquals(
        "<word><a>Chapter</a><b>1</b><c>A</c></word>",
        parser.parse("Chapter 1A"));
    assertEquals(
        "<word><a>Chapter</a><b>0</b></word>",
        parser.parse("Chapter0"));
    assertEquals(
        "<word><b>1</b></word>",
        parser.parse("1"));
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
