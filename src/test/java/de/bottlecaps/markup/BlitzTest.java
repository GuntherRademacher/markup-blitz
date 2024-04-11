// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

import static de.bottlecaps.markup.Blitz.normalizeEol;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.Blitz.Option;
import de.bottlecaps.markup.blitz.Parser;

public class BlitzTest extends TestBase {

  @Test
  public void testEmpty() {
    Parser parser = generate("S: .");
    String result = parser.parse("");
    assertEquals("<S/>", result);
  }

  @Test
  public void testEmptyCharset() {
    generate("S: [], 'a'.");
  }

  @Test
  public void testInfinite() {
    generate("s: -s.");
  }

  @Test
  public void testInsertion() {
    Parser parser = generate(
        "S: +'a', +'b', 'c', +'d', +'e', -'f', +'g', +'h', 'i', +'j', +'k', + 'l', 'm'.");
    String result = parser.parse(
        "cfim",
        Option.INDENT);
    assertEquals(
          "<S>abcdeghijklm</S>",
        result);
  }

  @Test
  public void testAmbiguousInsertion() {
    Parser parser = generate(
        "S:'a', +'a'+.");
    String result = parser.parse(
        "a");
    assertEquals(
        "<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">aa</S>",
        result);
  }

  @Test
  public void testAmbiguity1() {
    Parser parser = generate("S: 'a', 'b'+; 'a'+, 'b'.");
    String result = parser.parse("ab");
    assertEquals("<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">ab</S>", result);
  }

  @Test
  public void testAmbiguity2() {
    Parser parser = generate("S: 'a', 'b'+, 'c'; 'a'+, 'b', 'c'.");
    String result = parser.parse("abc");
    assertEquals("<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">abc</S>", result);
  }

  @Test
  public void testCss() {
    Parser parser = generate(
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
        Option.INDENT);
    Set<String> expectedResults = Set.of(
        "<css xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">\n"
        + "   <rule>\n"
        + "      <selector>\n"
        + "         <name>p</name>\n"
        + "      </selector>\n"
        + "      <block>\n"
        + "         <property/>\n"
        + "      </block>\n"
        + "   </rule>\n"
        + "</css>",
          "<css xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">\n"
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
    Parser parser = generate(Blitz.ixmlGrammar(), Option.INDENT); // , Option.TIMING);
    String xml = parser.parse(Blitz.ixmlGrammar());
    assertEquals(normalizeEol(resourceContent("ixml.xml")), xml);
  }

  @Test
  public void testJson() {
    Parser parser = generate(resourceContent("json.ixml")); // , Option.TIMING, Option.TRACE, Option.VERBOSE);
    String result = parser.parse(resourceContent("sample.json"));
    String expectedResult = normalizeEol(resourceContent("sample.json.xml"));
    assertEquals(expectedResult, result);
  }

  @Test
  public void testAddress() {
    Parser parser = generate(resourceContent("address.ixml"));
    String xml = parser.parse(resourceContent("address.input"), Option.INDENT);
    assertEquals(normalizeEol(resourceContent("address.xml")), xml);
  }

  @Test
  public void testMultiThreadParsing() throws Throwable {
    Parser parser = generate(Blitz.ixmlGrammar(), Option.INDENT);
    AtomicReference<Throwable> throwable = new AtomicReference<>();
    long timeLimit = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3);
    List<Thread> threads = Arrays.asList("ixml", "json", "frege").stream()
      .map(grammar ->
        new Thread(() -> {
          while (System.currentTimeMillis() < timeLimit && throwable.get() == null) {
            try {
              String resource = grammar == "ixml" ? Blitz.IXML_GRAMMAR_RESOURCE : grammar + ".ixml";
              String xml = parser.parse(normalizeEol(resourceContent(resource)), Option.INDENT);
              assertEquals(normalizeEol(resourceContent(grammar + ".xml")), xml);
            }
            catch (Throwable t) {
              throwable.compareAndSet(null, t);
            }
          }
        }))
      .collect(Collectors.toList());
    for (Thread thread : threads)
      thread.start();
    for (Thread thread : threads)
      thread.join();
    if (throwable.get() != null)
      throw throwable.get();
  }

  @Test
  public void testCrLf() {
    final String grammar = "S:  ~[]*.";
    final String crLf = "\r\n";
    Parser parser = generate(grammar);
    assertEquals("<S>\n</S>", parser.parse(crLf));
    parser = generate("ixml version '1.0'. " + grammar);
    assertEquals("<S>&#xD;\n</S>", parser.parse(crLf));
    parser = generate("ixml version '1.1'. " + grammar);
    assertEquals("<S>\n</S>", parser.parse(crLf));
  }

  @Test
  public void testArith() {
    Parser parser = generate(resourceContent("arith.ixml"));
    String result = parser.parse(resourceContent("arith.input"), Option.INDENT);
    assertEquals(normalizeEol(resourceContent("arith.xml")), result);
  }

  @Test
  public void testAttributeValue() {
    Parser parser = generate(
          "test: a, \".\".\n"
        + "@a: ~[\".\"]*.");
    String result = parser.parse(
        "\"'<>/&.");
    assertEquals("<test a=\"&quot;'&lt;&gt;/&amp;\">.</test>", result);
  }

  @Test
  public void testAttributeMultipart() {
    Parser parser = generate(
          "date: month, -',', -' '*, year . \n"
        + "@month: 'Feb', 'ruary' .\n"
        + "year: ['0'-'9']+ .");
    String result = parser.parse(
        "February, 2022",
        Option.INDENT);
    assertEquals(
          "<date month=\"February\">\n"
        + "   <year>2022</year>\n"
        + "</date>",
        result);
  }

  @Test
  public void testDiary() {
    Parser parser = generate(
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
        Option.INDENT);
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
    Parser parser = generate(
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
        Option.INDENT);
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
    Parser parser = generate(
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
        Option.INDENT);
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
    Parser parser = generate(
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
    Parser parser = generate(
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
        Option.INDENT);
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
    Parser parser = generate(
        resourceContent("frege.ixml"),
        Option.INDENT);
    assertEquals(
          "<formula>\n"
        + "   <maybe>\n"
        + "      <conditional>\n"
        + "         <consequent>\n"
        + "            <leaf>\n"
        + "               <var>\u0391</var>\n"
        + "            </leaf>\n"
        + "         </consequent>\n"
        + "         <antecedent>\n"
        + "            <leaf>\n"
        + "               <var>\u0392</var>\n"
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
          + "               <var>\u03a8</var>\n"
          + "            </functor>\n"
          + "            <arg>\n"
          + "               <var>\u0391</var>\n"
          + "            </arg>\n"
          + "            <arg>\n"
          + "               <var>\u0392</var>\n"
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
          + "                     <var>\u0391</var>\n"
          + "                  </leaf>\n"
          + "               </consequent>\n"
          + "               <antecedent>\n"
          + "                  <leaf>\n"
          + "                     <var>\u0392</var>\n"
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
          + "               <var>\u0392</var>\n"
          + "            </leaf>\n"
          + "         </yes>\n"
          + "      </formula>\n"
          + "   </premise>\n"
          + "   <infstep>\n"
          + "      <conclusion>\n"
          + "         <formula>\n"
          + "            <yes>\n"
          + "               <leaf>\n"
          + "                  <var>\u0391</var>\n"
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
        + "                     <var>\u0391</var>\n"
        + "                  </leaf>\n"
        + "               </consequent>\n"
        + "               <antecedent>\n"
        + "                  <leaf>\n"
        + "                     <var>\u0392</var>\n"
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
        + "                  <var>\u0391</var>\n"
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
        + "         <univ bound-var=\"\ud835\udd21\">\n"
        + "            <not>\n"
        + "               <univ bound-var=\"\ud835\udd1e\">\n"
        + "                  <leaf>\n"
        + "                     <fa>\n"
        + "                        <functor>\n"
        + "                           <var>\u03a6</var>\n"
        + "                        </functor>\n"
        + "                        <arg>\n"
        + "                           <bound-var>\ud835\udd1e</bound-var>\n"
        + "                        </arg>\n"
        + "                        <arg>\n"
        + "                           <bound-var>\ud835\udd21</bound-var>\n"
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
        + "            <univ bound-var=\"\ud835\udd1e\">\n"
        + "               <leaf>\n"
        + "                  <var>\u0391</var>\n"
        + "               </leaf>\n"
        + "            </univ>\n"
        + "         </consequent>\n"
        + "         <antecedent>\n"
        + "            <leaf>\n"
        + "               <var>\u0392</var>\n"
        + "            </leaf>\n"
        + "         </antecedent>\n"
        + "      </conditional>\n"
        + "   </maybe>\n"
        + "</formula>",
        parser.parse("all fa satisfy Alpha if Beta"));
  }

  @Test
  public void testInsertSeparatorAlternate() {
    Parser parser = generate(
        "S: [L]++(+':';+'=').");
    String result = parser.parse(
        "abc",
        Option.INDENT);
    Set<String> expectedResults = Set.of(
        "<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">a=b=c</S>",
        "<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">a:b:c</S>",
        "<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE +  "\" ixml:state=\"ambiguous\">a=b:c</S>",
        "<S xmlns:ixml=\"" + Parser.IXML_NAMESPACE + "\" ixml:state=\"ambiguous\">a:b=c</S>"
        );
    assertTrue(expectedResults.contains(result),
        "unexpected result: " + result);
  }

  @Test
  public void testCombinedLexicographicalAndNumericSortingCriteria() {
    Parser parser = generate(
        "word : a,(space,b)?,(space, c)?;\n"
      + "             b.\n"
      + "a : [L]+.\n"
      + "c : [L]+.\n"
      + "b : -numeric.\n"
      + "numeric : [Nd]+.\n"
      + "-space : -[\" \"]*.");
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

  @Test
  public void testUnicodeVersion() {
    // source: https://lists.w3.org/Archives/Public/public-ixml/2023Oct/0014.html
    Parser parser = generate(
          "{ Input must be #11F04 #10F70 #18B00 #10FE0 (with newlines since the characters are rtl)\n"
        + "\ud807\udf04\n"
        + "\ud803\udf70\n"
        + "\ud822\udf00\n"
        + "\ud803\udfe0\n"
        + "}\n"
        + "\n"
        + "Unicode: version.\n"
        + "\n"
        + "@version: v15; v14; v13; v12; pre-v12.\n"
        + "\n"
        + "-v15: -[Lo], -#a, -[Lo], -#a, -[Lo], -#a, -[Lo], -#a, +\"15\".\n"
        + "-v14: -[Cn], -#a, -[Lo], -#a, -[Lo], -#a, -[Lo], -#a, +\"14\".\n"
        + "-v13: -[Cn], -#a, -[Cn], -#a, -[Lo], -#a, -[Lo], -#a, +\"13\".\n"
        + "-v12: -[Cn], -#a, -[Cn], -#a, -[Cn], -#a, -[Lo], -#a, +\"12\".\n"
        + "-pre-v12: -[Cn], -#a, -[Cn], -#a, -[Cn], -#a, -[Cn], -#a, +\"pre 12\".");
    String result = parser.parse(
          "\ud807\udf04\n"
        + "\ud803\udf70\n"
        + "\ud822\udf00\n"
        + "\ud803\udfe0\n");
    assertEquals(
        "<Unicode version=\"15\"/>",
        result);
  }

  @Test
  public void testIxmlGrammar() {
    Parser parser = generate(Blitz.ixmlGrammar());
    String result = parser.parse(
        "S: 'a'.",
        Option.INDENT);
    assertEquals(
          "<ixml>\n"
        + "   <rule name=\"S\">\n"
        + "      <alt>\n"
        + "         <literal string=\"a\"/>\n"
        + "      </alt>\n"
        + "   </rule>\n"
        + "</ixml>",
        result);
  }

  @Test
  public void testErrorDocument() {
    Parser parser = generate("S: 'a'.");
    String result = parser.parse("b");
    assertEquals(
          "<ixml xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"failed\">Failed to parse input:\n"
          + "lexical analysis failed\n"
          + "while expecting 'a'\n"
          + "at line 1, column 1:\n"
          + "...b...</ixml>",
        result);
  }

  @Test
  public void testFailOnError() {
    Parser parser = generate("S: 'a'.", Option.FAIL_ON_ERROR);
    try {
      String result = parser.parse("b");
      Assertions.fail("Parse did not fail, returned: \n" + result);
    }
    catch (BlitzParseException e) {
      assertEquals(
            "Failed to parse input:\n"
            + "lexical analysis failed\n"
            + "while expecting 'a'\n"
            + "at line 1, column 1:\n"
            + "...b...",
          e.getMessage());
    }
  }

  @Test
  public void testRename() {
    Parser parser = generate(
          "          expr: open, -arith, @close, -\";\".\n"
        + "         @open: \"(\".\n"
        + "         close: \")\".\n"
        + "         arith: left, op, ^right>second.\n"
        + "    left>first: operand.\n"
        + "        -right: operand.\n"
        + "      -operand: name; -number.\n"
        + "         @name: [\"a\"-\"z\"].\n"
        + "       @number: [\"0\"-\"9\"].\n"
        + "           -op: sign.\n"
        + "@sign>operator: \"+\"; \"-\".",
        Option.INDENT); // , Option.VERBOSE);
    String result = parser.parse(
        "(a+1);");
    assertEquals(
          "<expr open=\"(\" operator=\"+\" close=\")\">\n"
        + "   <first name=\"a\"/>\n"
        + "   <second>1</second>\n"
        + "</expr>",
        result);
  }

  @Test
  public void testLongestMatch() {
    Parser parser = generate("S: 'a'.", Option.LONGEST_MATCH);
    assertEquals("<S>a</S>", parser.parse("a"));
    assertEquals("<S>a</S>", parser.parse("aa"));
    assertEquals("<S>a</S>", parser.parse("ab"));

    parser = generate("S: 'a'?.", Option.LONGEST_MATCH);
    assertEquals("<S/>", parser.parse(""));
    assertEquals("<S>a</S>", parser.parse("a"));
    assertEquals("<S>a</S>", parser.parse("aa"));
    assertEquals("<S>a</S>", parser.parse("ab"));

    parser = generate("S: 'a'+.", Option.LONGEST_MATCH);
    assertEquals("<S>a</S>", parser.parse("a"));
    assertEquals("<S>aa</S>", parser.parse("aa"));
    assertEquals("<S>aaa</S>", parser.parse("aaa"));
    assertEquals("<S>a</S>", parser.parse("ab"));
    assertEquals("<S>aa</S>", parser.parse("aab"));
    assertEquals("<S>aaa</S>", parser.parse("aaab"));

    parser = generate("S: 'a'*.", Option.LONGEST_MATCH);
    assertEquals("<S/>", parser.parse(""));
    assertEquals("<S>a</S>", parser.parse("a"));
    assertEquals("<S>aa</S>", parser.parse("aa"));
    assertEquals("<S>aaa</S>", parser.parse("aaa"));
    assertEquals("<S/>", parser.parse("b"));
    assertEquals("<S>a</S>", parser.parse("ab"));
    assertEquals("<S>aa</S>", parser.parse("aab"));
    assertEquals("<S>aaa</S>", parser.parse("aaab"));

    parser = generate(
          "expression: expr.\n"
        + "-expr: term; sum; diff.\n"
        + "sum: expr, -\"+\", term.\n"
        + "diff: expr, \"-\", term.\n"
        + "-term: factor; prod; div.\n"
        + "prod: term, -\"*\", factor.\n"
        + "div: term, \"/\", factor.\n"
        + "-factor: id; number; bracketed.\n"
        + "bracketed: -\"(\", expr, -\")\".\n"
        + "id: @name.\n"
        + "name: letter+.\n"
        + "number: @value.\n"
        + "value: digit+.\n"
        + "-letter: [\"a\"-\"z\"].\n"
        + "-digit: [\"0\"-\"9\"].",
        Option.INDENT, Option.LONGEST_MATCH);
    assertEquals(
          "<expression>\n"
        + "   <bracketed>\n"
        + "      <sum>\n"
        + "         <id name=\"a\"/>\n"
        + "         <number value=\"1\"/>\n"
        + "      </sum>\n"
        + "   </bracketed>\n"
        + "</expression>",
      parser.parse(
          "(a+1)*$%&$&"));
    assertEquals(
          "<expression>\n"
        + "   <prod>\n"
        + "      <bracketed>\n"
        + "         <sum>\n"
        + "            <id name=\"a\"/>\n"
        + "            <number value=\"1\"/>\n"
        + "         </sum>\n"
        + "      </bracketed>\n"
        + "      <bracketed>\n"
        + "         <sum>\n"
        + "            <id name=\"a\"/>\n"
        + "            <number value=\"2\"/>\n"
        + "         </sum>\n"
        + "      </bracketed>\n"
        + "   </prod>\n"
        + "</expression>",
        parser.parse(
            "(a+1)*(a+2)*(a+3"));
  }

  @Test
  public void testShortestMatch() {
    Parser parser = generate("S: 'a'.", Option.SHORTEST_MATCH);
    assertEquals("<S>a</S>", parser.parse("a"));
    assertEquals("<S>a</S>", parser.parse("aa"));
    assertEquals("<S>a</S>", parser.parse("ab"));

    parser = generate("S: 'a'?.", Option.SHORTEST_MATCH);
    assertEquals("<S/>", parser.parse(""));
    assertEquals("<S/>", parser.parse("a"));
    assertEquals("<S/>", parser.parse("aa"));
    assertEquals("<S/>", parser.parse("ab"));

    parser = generate("S: 'a'+.", Option.SHORTEST_MATCH);
    assertEquals("<S>a</S>", parser.parse("a"));
    assertEquals("<S>a</S>", parser.parse("aa"));
    assertEquals("<S>a</S>", parser.parse("aaa"));
    assertEquals("<S>a</S>", parser.parse("ab"));
    assertEquals("<S>a</S>", parser.parse("aab"));
    assertEquals("<S>a</S>", parser.parse("aaab"));

    parser = generate("S: 'a'*.", Option.SHORTEST_MATCH);
    assertEquals("<S/>", parser.parse(""));
    assertEquals("<S/>", parser.parse("a"));
    assertEquals("<S/>", parser.parse("aa"));
    assertEquals("<S/>", parser.parse("aaa"));
    assertEquals("<S/>", parser.parse("b"));
    assertEquals("<S/>", parser.parse("ab"));
    assertEquals("<S/>", parser.parse("aab"));
    assertEquals("<S/>", parser.parse("aaab"));

    parser = generate(
          "expression: expr.\n"
        + "-expr: term; sum; diff.\n"
        + "sum: expr, -\"+\", term.\n"
        + "diff: expr, \"-\", term.\n"
        + "-term: factor; prod; div.\n"
        + "prod: term, -\"*\", factor.\n"
        + "div: term, \"/\", factor.\n"
        + "-factor: id; number; bracketed.\n"
        + "bracketed: -\"(\", expr, -\")\".\n"
        + "id: @name.\n"
        + "name: letter+.\n"
        + "number: @value.\n"
        + "value: digit+.\n"
        + "-letter: [\"a\"-\"z\"].\n"
        + "-digit: [\"0\"-\"9\"].",
        Option.INDENT, Option.SHORTEST_MATCH);
    assertEquals(
          "<expression>\n"
        + "   <bracketed>\n"
        + "      <sum>\n"
        + "         <id name=\"a\"/>\n"
        + "         <number value=\"1\"/>\n"
        + "      </sum>\n"
        + "   </bracketed>\n"
        + "</expression>",
      parser.parse(
          "(a+1)*$%&$&"));
    assertEquals(
          "<expression>\n"
        + "   <bracketed>\n"
        + "      <sum>\n"
        + "         <id name=\"a\"/>\n"
        + "         <number value=\"1\"/>\n"
        + "      </sum>\n"
        + "   </bracketed>\n"
        + "</expression>",
        parser.parse(
            "(a+1)*(a+2)*(a+3"));
  }

//  @Test
//  public void test() {
//    Parser parser = generate(
//        "");
//    String result = parser.parse(
//        "",
//        Option.INDENT);
//    assertEquals(
//        "",
//        result);
//  }
}
