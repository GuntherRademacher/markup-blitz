// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup;

import static de.bottlecaps.markup.Blitz.normalizeEol;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.Option;
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
        "S: +'a', +'b', 'c', +'d', +'e', -'f', +'g', +'h', 'i', +'j', +'k', + 'l', 'm'.",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
        "cfim");
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
        + "      -S = -[\" \" | #a]*.",
        Map.of(Option.INDENT, true));
    String result = parser.parse("p { }");
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
    Parser parser = generate(Blitz.ixmlGrammar(), Map.of(Option.INDENT, true));
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
    Parser parser = generate(resourceContent("address.ixml"), Map.of(Option.INDENT, true));
    String xml = parser.parse(resourceContent("address.input"));
    assertEquals(normalizeEol(resourceContent("address.xml")), xml);
  }

  @Test
  public void testMultiThreadParsing() throws Throwable {
    Parser parser = generate(Blitz.ixmlGrammar(), Map.of(Option.INDENT, true));
    AtomicReference<Throwable> throwable = new AtomicReference<>();
    long timeLimit = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3);
    List<Thread> threads = Arrays.asList("ixml", "json", "frege").stream()
      .map(grammar ->
        new Thread(() -> {
          while (System.currentTimeMillis() < timeLimit && throwable.get() == null) {
            try {
              String resource = grammar == "ixml" ? Blitz.IXML_GRAMMAR_RESOURCE : grammar + ".ixml";
              String xml = parser.parse(normalizeEol(resourceContent(resource)));
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
    Parser parser = generate(resourceContent("arith.ixml"), Map.of(Option.INDENT, true));
    String result = parser.parse(resourceContent("arith.input"));
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
        + "year: ['0'-'9']+ .",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
        "February, 2022");
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
        + "-nl: -#a | -#d, -#a .",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
          "24 December 2021\n"
        + "Panic shopping! Panic packing! Will we make it before midnight?\n"
        + "\n"
        + "25 December 2021\n"
        + "Food! Presents!\n"
        + "\n"
        + "26 December 2021\n"
        + "Groan.\n"
        + "\n");
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
        + "-nl: -#a | -#d, -#a .",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
          "24 December 2021\n"
        + "Panic shopping! Panic packing! Will we make it before midnight?\n"
        + "\n"
        + "25 December 2021\n"
        + "Food! Presents!\n"
        + "\n"
        + "26 December 2021\n"
        + "Groan.\n"
        + "\n");
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
        + "-nl: -#a | -#d, -#a .",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
          "24 December 2021\n"
        + "Panic shopping! Panic packing! Will we make it before midnight?\n"
        + "\n"
        + "25 December 2021\n"
        + "Food! Presents!\n"
        + "\n"
        + "26 December 2021\n"
        + "Groan.\n"
        + "\n");
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
        + "-digit: [\"0\"-\"9\"].",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
        "pi+(10\u00d7b)");
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
        Map.of(Option.INDENT, true));
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
        "S: [L]++(+':';+'=').",
        Map.of(Option.INDENT, true));
    String result = parser.parse(
        "abc");
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
    Parser parser = generate(Blitz.ixmlGrammar(), Map.of(Option.INDENT, true));
    String result = parser.parse(
        "S: 'a'.");
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
    Parser parser = generate("S: 'a'.", Map.of(Option.FAIL_ON_ERROR, true));
    BlitzParseException exception = assertThrows(BlitzParseException.class, () -> {
      parser.parse("b");
    });
    assertEquals(
            "Failed to parse input:\n"
          + "lexical analysis failed\n"
          + "while expecting 'a'\n"
          + "at line 1, column 1:\n"
          + "...b...",
        exception.getMessage());
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
        Map.of(Option.INDENT, true)); // , Option.VERBOSE);
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
  public void testIssue9() {
    Parser parser = generate(
          "input = (A; B; C)*.\n"
        + "A = -'A', rs, a-list, stop.\n"
        + "B = -'B', rs, b-list, stop.\n"
        + "C = -'C', rs, name, stop.\n"
        + "@a-list = name ++ comma-space.\n"
        + "@b-list = id ++ comma-space.\n"
        + "id = name.\n"
        + "@name = [L]+.\n"
        + "\n"
        + "-comma-space = (s?, -\",\", s?, +\" \").\n"
        + "-stop = os, -\".\", os.\n"
        + "-s = -[Zs; #09; #0A].\n"
        + "-rs = s+.\n"
        + "-os = s*.",
        Map.of(Option.INDENT, true));
    String result = parser.parse("A x. A x, y, z. B z. B a, b, c. C q.");
    assertEquals(
          "<input>\n"
        + "   <A a-list=\"x\"/>\n"
        + "   <A a-list=\"x y z\"/>\n"
        + "   <B b-list=\"z\"/>\n"
        + "   <B b-list=\"a b c\"/>\n"
        + "   <C name=\"q\"/>\n"
        + "</input>",
        result);
  }

  @Test
  public void testIssue10() {
    Parser parser = generate(
          "input = (A|B)*.\n"
        + "A = -'A', rs, a-list, stop.\n"
        + "B = -'B', rs, b-list, stop.\n"
        + "@a-list = name ++ rs.\n"
        + "@b-list = name ++ (rs, +#20).\n"
        + "name = [L]+.\n"
        + "\n"
        + "-stop = os, -\".\", os.\n"
        + "-s = -[Zs; #09; #0A].\n"
        + "-rs = s+.\n"
        + "-os = s*.",
        Map.of(Option.INDENT, true));
    String result = parser.parse("A  x. A x  y  z. B  x. B x  y  z.");
    assertEquals(
          "<input>\n"
        + "   <A a-list=\"x\"/>\n"
        + "   <A a-list=\"xyz\"/>\n"
        + "   <B b-list=\"x\"/>\n"
        + "   <B b-list=\"x y z\"/>\n"
        + "</input>", result);
    parser = generate(
          "input = (A|B)*.\n"
        + "A = -'A', rs, a-list, stop.\n"
        + "B = -'B', rs, b-list, stop.\n"
        + "@a-list = name ++ rs.\n"
        + "@b-list = name ++ (\" \", +#20).\n"
        + "name = [L]+.\n"
        + "\n"
        + "-stop = os, -\".\", os.\n"
        + "-s = -[Zs; #09; #0A].\n"
        + "-rs = s+.\n"
        + "-os = s*.",
        Map.of(Option.INDENT, true));
    result = parser.parse("A  x. A x y z. B  x. B x y z.");
    assertEquals(
          "<input>\n"
        + "   <A a-list=\"x\"/>\n"
        + "   <A a-list=\"xyz\"/>\n"
        + "   <B b-list=\"x\"/>\n"
        + "   <B b-list=\"x  y  z\"/>\n"
        + "</input>", result);
    parser = generate(
          "input = (A|B)*.\n"
        + "A = -'A', rs, a-list, stop.\n"
        + "B = -'B', rs, b-list, stop.\n"
        + "@a-list = name ++ rs.\n"
        + "@b-list = name ++ (s, +#20).\n"
        + "name = [L]+.\n"
        + "\n"
        + "-stop = os, -\".\", os.\n"
        + "-s = -[Zs; #09; #0A].\n"
        + "-rs = s+.\n"
        + "-os = s*.",
        Map.of(Option.INDENT, true));
    result = parser.parse("A  x. A x y z. B  x. B x y z.");
    assertEquals(
          "<input>\n"
        + "   <A a-list=\"x\"/>\n"
        + "   <A a-list=\"xyz\"/>\n"
        + "   <B b-list=\"x\"/>\n"
        + "   <B b-list=\"x y z\"/>\n"
        + "</input>", result);
  }

  @Test
  public void testLongestMatch() {
    Parser parser = generate("S: 'a'.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("ab"));

    parser = generate("S: 'a'?.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse(""));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("ab"));

    parser = generate("S: 'a'+.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("a"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"2\">aa</S>", parser.parse("aa"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"3\">aaa</S>", parser.parse("aaa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("ab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"2\">aa</S>", parser.parse("aab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"3\">aaa</S>", parser.parse("aaab"));

    parser = generate("S: 'a'*.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse(""));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("a"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"2\">aa</S>", parser.parse("aa"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"3\">aaa</S>", parser.parse("aaa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("b"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("ab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"2\">aa</S>", parser.parse("aab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"3\">aaa</S>", parser.parse("aaab"));

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
        Map.of(Option.INDENT, true, Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH));
    assertEquals(
          "<expression xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"5\">\n"
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
          "<expression xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"11\">\n"
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
    Parser parser = generate("S: 'a'.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("ab"));

    parser = generate("S: 'a'?.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse(""));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("aa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("ab"));

    parser = generate("S: 'a'+.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aaa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("ab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"1\">a</S>", parser.parse("aaab"));

    parser = generate("S: 'a'*.", Map.of(Option.TRAILING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse(""));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("aa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("aaa"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("b"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("ab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("aab"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"0\"/>", parser.parse("aaab"));

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
        Map.of(Option.INDENT, true, Option.TRAILING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH));
    assertEquals(
          "<expression xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"5\">\n"
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
          "<expression xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:length=\"5\">\n"
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

  @Test
  public void testFirstMatch() {
    Parser parser = generate("S: 'abc'.", Map.of(Option.LEADING_CONTENT_POLICY, Option.Value.FIRST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"0\">abc</S>", parser.parse("abc"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\">abc</S>", parser.parse("dabc"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\">abc</S>", parser.parse("aabc"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\">abc</S>", parser.parse("aaabc"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\">abc</S>", parser.parse("ababc"));

    parser = generate("S: #1f642, #1f609, #1f600.", Map.of(Option.LEADING_CONTENT_POLICY, Option.Value.FIRST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"0\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("a\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("ab\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE1F\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE1F\uD83E\uDD15\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));

    parser = generate("S: #1f642, #1f609, #1f600.", Map.of(Option.LEADING_CONTENT_POLICY, Option.Value.FIRST_MATCH, Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"0\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("a\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("ab\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE1F\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE1F\uD83E\uDD15\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"0\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("a\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("ab\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00a"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"1\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE1F\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00\uD83D\uDE1F"));
    assertEquals("<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"2\" blitz:length=\"3\">\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00</S>", parser.parse("\uD83D\uDE1F\uD83E\uDD15\uD83D\uDE42\uD83D\uDE09\uD83D\uDE00\uD83D\uDE1F"));
  }

  @Test
  public void testMain() throws IOException {
    PrintStream sysOut = System.out;
    PrintStream sysErr = System.err;
    ByteArrayOutputStream outData = new ByteArrayOutputStream();
    ByteArrayOutputStream errData = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(outData, true, StandardCharsets.UTF_8);
         PrintStream err = new PrintStream(errData, true, StandardCharsets.UTF_8)) {
      System.setOut(out);
      System.setErr(err);
      try {
        Blitz.main(new String[] {
            "--" + Option.INDENT.externalName(),
            "--" + Option.TRACE.externalName() + ":" + true,
            "--" + Option.TIMING.externalName() + ":" + false,
            "--" + Option.VERBOSE.externalName() + ":" + false,
            "--" + Option.FAIL_ON_ERROR.externalName(),
            "--" + Option.LEADING_CONTENT_POLICY.externalName() + ":" + Option.Value.FIRST_MATCH.externalName(),
            "--" + Option.TRAILING_CONTENT_POLICY.externalName() + ":" + Option.Value.LONGEST_MATCH.externalName(),
            "!S:A+. A:[Ll].",
            "!...abc..."
        });
      }
      finally {
        System.setOut(sysOut);
        System.setErr(sysErr);
      }
    }
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
      + "<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"3\" blitz:length=\"3\">\n"
      + "   <A>a</A>\n"
      + "   <A>b</A>\n"
      + "   <A>c</A>\n"
      + "</S>",
      outData.toString(StandardCharsets.UTF_8));
    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<trace>\n"
        + "  <tokenize thread=\"0\" offset=\"0\" char=\".\" codepoint=\"46\" class=\"2\" status=\"success\" result=\"#0...\" end=\"1\"/>\n"
        + "  <tokenize thread=\"0\" offset=\"1\" char=\".\" codepoint=\"46\" class=\"2\" status=\"success\" result=\"#0...\" end=\"2\"/>\n"
        + "  <tokenize thread=\"0\" offset=\"2\" char=\".\" codepoint=\"46\" class=\"2\" status=\"success\" result=\"#0...\" end=\"3\"/>\n"
        + "  <tokenize thread=\"0\" offset=\"3\" char=\"a\" codepoint=\"97\" class=\"1\" status=\"success\" result=\"'a'...\" end=\"4\"/>\n"
        + "  <parse thread=\"0\" offset=\"3\" state=\"0\" input=\"'a'...\" action=\"shift reduce\" nonterminal=\"A\" count=\"1\"/>\n"
        + "  <parse thread=\"0\" offset=\"4\" state=\"0\" input=\"A\" action=\"shift reduce\" nonterminal=\"S\" count=\"1\"/>\n"
        + "  <parse thread=\"0\" offset=\"4\" state=\"0\" input=\"S\" action=\"shift\"/>\n"
        + "  <tokenize thread=\"0\" offset=\"4\" char=\"b\" codepoint=\"98\" class=\"1\" status=\"success\" result=\"'a'...\" end=\"5\"/>\n"
        + "  <parse thread=\"0\" offset=\"4\" state=\"1\" input=\"'a'...\" action=\"fork\"/>\n"
        + "  <parse thread=\"1\" offset=\"4\" state=\"1\" input=\"'a'...\" action=\"reduce\" nonterminal=\"_start\" count=\"1\"/>\n"
        + "  <parse thread=\"1\" offset=\"4\" state=\"0\" input=\"_start 'a'...\" action=\"accept\"/>\n"
        + "  <parse thread=\"0\" offset=\"4\" state=\"1\" input=\"'a'...\" action=\"shift reduce\" nonterminal=\"A\" count=\"1\"/>\n"
        + "  <parse thread=\"0\" offset=\"5\" state=\"1\" input=\"A\" action=\"shift reduce\" nonterminal=\"S\" count=\"2\"/>\n"
        + "  <parse thread=\"0\" offset=\"5\" state=\"0\" input=\"S\" action=\"shift\"/>\n"
        + "  <tokenize thread=\"0\" offset=\"5\" char=\"c\" codepoint=\"99\" class=\"1\" status=\"success\" result=\"'a'...\" end=\"6\"/>\n"
        + "  <parse thread=\"0\" offset=\"5\" state=\"1\" input=\"'a'...\" action=\"fork\"/>\n"
        + "  <parse thread=\"0\" offset=\"5\" state=\"1\" input=\"'a'...\" action=\"shift reduce\" nonterminal=\"A\" count=\"1\"/>\n"
        + "  <parse thread=\"0\" offset=\"6\" state=\"1\" input=\"A\" action=\"shift reduce\" nonterminal=\"S\" count=\"2\"/>\n"
        + "  <parse thread=\"0\" offset=\"6\" state=\"0\" input=\"S\" action=\"shift\"/>\n"
        + "  <tokenize thread=\"0\" offset=\"6\" char=\".\" codepoint=\"46\" class=\"2\" status=\"success\" result=\"#0...\" end=\"7\"/>\n"
        + "  <parse thread=\"2\" offset=\"5\" state=\"1\" input=\"'a'...\" action=\"reduce\" nonterminal=\"_start\" count=\"1\"/>\n"
        + "  <parse thread=\"2\" offset=\"5\" state=\"0\" input=\"_start 'a'...\" action=\"accept\"/>\n"
        + "  <parse thread=\"0\" offset=\"6\" state=\"1\" input=\"#0...\" action=\"reduce\" nonterminal=\"_start\" count=\"1\"/>\n"
        + "  <parse thread=\"0\" offset=\"6\" state=\"0\" input=\"_start #0...\" action=\"accept\"/>\n"
        + "</trace>\n",
      errData.toString(StandardCharsets.UTF_8));
  }

  public void testIssue18() {
    Parser parser = generate(
        "S: -E1; -E2. E1: 'x'++'y'. E2: 'x'**'y'.");
    String result = parser.parse(
        "xyx");
    assertEquals(
        "<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"ambiguous\">xyx</S>",
        result);
  }

  @Test
  public void testIssue20() {
    Parser parser = generate(
        "s: 'a'**'bc'.");
    String result = parser.parse(
        "abca");
    assertEquals(
        "<s>abca</s>",
        result);
    parser = generate(
        "s: 'bc'**'a'.");
    result = parser.parse(
        "bcabc");
    assertEquals(
        "<s>bcabc</s>",
        result);
  }

  @Test
  public void testIssue24() {
    Parser parser = generate(
        "S:'a'.");
    String result = parser.parse(
        "");
    assertEquals(
          "<ixml xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"failed\">Failed to parse input:\n"
        + "syntax error, found end of input\n"
        + "while expecting 'a'\n"
        + "at line 1, column 1:\n"
        + "......</ixml>",
        result);

    Parser failingParser = generate(
        "S:'a'.", Map.of(Option.FAIL_ON_ERROR, true));
    BlitzParseException exception = assertThrows(BlitzParseException.class, () -> {
      failingParser.parse("");
    });
    assertEquals(
          "Failed to parse input:\n"
        + "syntax error, found end of input\n"
        + "while expecting 'a'\n"
        + "at line 1, column 1:\n"
        + "......",
        exception.getMessage());
    assertEquals(1, exception.getLine());
    assertEquals(1, exception.getColumn());
    assertEquals("end of input", exception.getOffendingToken());

    PrintStream originalErr = System.err;
    ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    System.setErr(new PrintStream(errContent));
    try {
      generate(
          "S:'a'.",
          Map.of(Option.VERBOSE, true));
      String expected =
            "\n"
          + "BNF grammar:\n"
          + "------------\n"
          + "      -_start: ^S.\n"
          + "            S: ['a'].\n"
          + "\n"
          + "Number of charClasses: 2\n"
          + "----------------------\n"
          + "0: end of input\n"
          + "1: ['a']\n"
          + "\n"
          + "2 states (not counting LR(0) reduce states)\n"
          + "2 reduce arguments\n"
          + "0 forks\n"
          + "\n"
          + "state 0:\n"
          + "[-_start: . ^S | {end of input}] shift 1\n"
          + "[S: . ['a'] | {end of input}] shift-reduce 1 (pop 1, id 1, nonterminal S, marks ^)\n"
          + "\n"
          + "state 1:\n"
          + "[-_start: ^S . | {end of input}] reduce 0 (pop 1, id 0, nonterminal _start, marks ^)\n"
          + "\n"
          + "size of token code map: 107, shift: [3, 4, 4]\n"
          + "size of terminal transition map: 5, shift: [2]\n"
          + "size of nonterminal transition map: 5, shift: [2]\n"
          + "\n";
      assertEquals(expected, errContent.toString().replace("\r\n", "\n"));
    } finally {
      System.setErr(originalErr);
    }
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
