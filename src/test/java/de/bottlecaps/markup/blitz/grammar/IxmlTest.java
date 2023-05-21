package de.bottlecaps.markup.blitz.grammar;

import static de.bottlecaps.markup.blitz.Blitz.resourceContent;
import static de.bottlecaps.markup.blitz.Blitz.urlContent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.grammar.Ixml.ParseException;
import de.bottlecaps.markup.blitz.transform.BNF;
import de.bottlecaps.markup.blitz.transform.PostProcess;

public class IxmlTest {
    private static final String invisiblexmlOrgUrl = "https://invisiblexml.org/1.0/ixml.ixml";
    private static final String ixmlResource = "ixml.ixml";

    private static final String githubJsonIxmlUrl = "https://raw.githubusercontent.com/GuntherRademacher/rex-parser-benchmark/main/src/main/resources/de/bottlecaps/rex/benchmark/json/parsers/xquery/json.ixml";
    private static final String jsonIxmlResource = "json.ixml";

    private static String ixmlIxmlResourceContent;
    private static String jsonIxmlResourceContent;

    @BeforeAll
    public static void beforeAll() throws URISyntaxException, IOException {
      ixmlIxmlResourceContent = resourceContent(ixmlResource);
      jsonIxmlResourceContent = resourceContent(jsonIxmlResource);
    }

    @Test
    public void testIxmlResource() throws Exception {
      Grammar grammar = parse(ixmlIxmlResourceContent, ixmlResource);
      assertEquals(ixmlIxmlResourceContent, grammar.toString(), "roundtrip failed for " + ixmlResource);
    }

    @Test
    public void testJsonIxmlResource() throws Exception {
      Grammar grammar = parse(jsonIxmlResourceContent, jsonIxmlResource);
      assertEquals(jsonIxmlResourceContent, grammar.toString(), "roundtrip failed for " + jsonIxmlResource);
    }

    @Test
    @Disabled
    public void testGithubJsonIxmlUrl() throws Exception {
      testUrlContent(githubJsonIxmlUrl, jsonIxmlResource, jsonIxmlResourceContent);
    }

    @Test
    @Disabled
    public void testInvisiblexmlOrgUrlContent() throws Exception {
      testUrlContent(invisiblexmlOrgUrl, ixmlResource, ixmlIxmlResourceContent);
    }

    @Test
    public void testCopyIxmlResource() {
      Grammar grammar = parse(ixmlIxmlResourceContent, ixmlResource);
//      Grammar copy = new Copy(grammar).get();
      Grammar copy = grammar.copy();
      assertEquals(grammar.toString(), copy.toString());
      assertEquals(grammar, copy);
    }

    @Test
    public void testCopyJsonResource() {
      Grammar grammar = parse(jsonIxmlResourceContent, jsonIxmlResource);
//      Grammar copy = new Copy(grammar).get();
      Grammar copy = grammar.copy();
      assertEquals(grammar.toString(), copy.toString());
      assertEquals(grammar, copy);
    }

    @Test
    public void testBnfOfIxmlResource() {
      String expectedResult =
          "         ixml: s, -_prolog_option, -_rule_RS_list, s.\n"
        + "_prolog_option:\n"
        + "               ;\n"
        + "               prolog.\n"
        + "_rule_RS_list: rule;\n"
        + "               -_rule_RS_list, RS, rule.\n"
        + "           -s: ;\n"
        + "               -s, -_whitespace_comment_choice.\n"
        + "_whitespace_comment_choice:\n"
        + "               whitespace;\n"
        + "               comment.\n"
        + "          -RS: -_whitespace_comment_choice;\n"
        + "               -RS, -_whitespace_comment_choice.\n"
        + "  -whitespace: -[Zs];\n"
        + "               tab;\n"
        + "               lf;\n"
        + "               cr.\n"
        + "         -tab: -#9.\n"
        + "          -lf: -#a.\n"
        + "          -cr: -#d.\n"
        + "      comment: -'{', -_cchar_comment_list_option, -'}'.\n"
        + "_cchar_comment_choice:\n"
        + "               cchar;\n"
        + "               comment.\n"
        + "_cchar_comment_list_option:\n"
        + "               ;\n"
        + "               -_cchar_comment_list_option, -_cchar_comment_choice.\n"
        + "       -cchar: ~['{}'].\n"
        + "       prolog: version, s.\n"
        + "      version: -'i', -'x', -'m', -'l', RS, -'v', -'e', -'r', -'s', -'i', -'o', -'n', RS, string, s, -'.'.\n"
        + "         rule: -_mark_s_option, name, s, -_choice, s, -alts, -'.'.\n"
        + "_mark_s_option:\n"
        + "               ;\n"
        + "               mark, s.\n"
        + "      _choice: -'=';\n"
        + "               -':'.\n"
        + "        @mark: ['@^-'].\n"
        + "         alts: alt;\n"
        + "               -alts, -_choice_1, s, alt.\n"
        + "    _choice_1: -';';\n"
        + "               -'|'.\n"
        + "          alt: ;\n"
        + "               -_term_s_list.\n"
        + " _term_s_list: term;\n"
        + "               -_term_s_list, -',', s, term.\n"
        + "        -term: factor;\n"
        + "               option;\n"
        + "               repeat0;\n"
        + "               repeat1.\n"
        + "      -factor: terminal;\n"
        + "               nonterminal;\n"
        + "               insertion;\n"
        + "               -'(', s, alts, -')', s.\n"
        + "      repeat0: factor, -_s_s_sep_choice.\n"
        + "_s_s_sep_choice:\n"
        + "               -'*', s;\n"
        + "               -'*', -'*', s, sep.\n"
        + "      repeat1: factor, -_s_s_sep_choice_1.\n"
        + "_s_s_sep_choice_1:\n"
        + "               -'+', s;\n"
        + "               -'+', -'+', s, sep.\n"
        + "       option: factor, -'?', s.\n"
        + "          sep: factor.\n"
        + "  nonterminal: -_mark_s_option, name, s.\n"
        + "        @name: namestart, -_namefollower_list_option.\n"
        + "_namefollower_list_option:\n"
        + "               ;\n"
        + "               -_namefollower_list_option, namefollower.\n"
        + "   -namestart: ['_'; L].\n"
        + "-namefollower: namestart;\n"
        + "               ['-.·‿⁀'; Nd; Mn].\n"
        + "    -terminal: literal;\n"
        + "               charset.\n"
        + "      literal: quoted;\n"
        + "               encoded.\n"
        + "      -quoted: -_tmark_s_option, string, s.\n"
        + "_tmark_s_option:\n"
        + "               ;\n"
        + "               tmark, s.\n"
        + "       @tmark: ['^-'].\n"
        + "      @string: -'\"', -_dchar_list, -'\"';\n"
        + "               -'''', -_schar_list, -''''.\n"
        + "  _dchar_list: dchar;\n"
        + "               -_dchar_list, dchar.\n"
        + "  _schar_list: schar;\n"
        + "               -_schar_list, schar.\n"
        + "        dchar: ~['\"'; #a; #d];\n"
        + "               '\"', -'\"'.\n"
        + "        schar: ~[''''; #a; #d];\n"
        + "               '''', -''''.\n"
        + "     -encoded: -_tmark_s_option, -'#', hex, s.\n"
        + "         @hex: -_0_9_a_f_A_F_choice;\n"
        + "               -hex, -_0_9_a_f_A_F_choice.\n"
        + "_0_9_a_f_A_F_choice:\n"
        + "               ['0'-'9'];\n"
        + "               ['a'-'f'];\n"
        + "               ['A'-'F'].\n"
        + "     -charset: inclusion;\n"
        + "               exclusion.\n"
        + "    inclusion: -_tmark_s_option, set.\n"
        + "    exclusion: -_tmark_s_option, -'~', s, set.\n"
        + "         -set: -'[', s, -_member_s_s_list_option, -']', s.\n"
        + "_member_s_s_list:\n"
        + "               member, s;\n"
        + "               -_member_s_s_list, -_choice_1, s, member, s.\n"
        + "_member_s_s_list_option:\n"
        + "               ;\n"
        + "               -_member_s_s_list.\n"
        + "       member: string;\n"
        + "               -'#', hex;\n"
        + "               range;\n"
        + "               class.\n"
        + "       -range: from, s, -'-', s, to.\n"
        + "        @from: character.\n"
        + "          @to: character.\n"
        + "   -character: -'\"', dchar, -'\"';\n"
        + "               -'''', schar, -'''';\n"
        + "               '#', hex.\n"
        + "       -class: code.\n"
        + "        @code: capital, -_letter_option.\n"
        + "_letter_option:\n"
        + "               ;\n"
        + "               letter.\n"
        + "     -capital: ['A'-'Z'].\n"
        + "      -letter: ['a'-'z'].\n"
        + "    insertion: -'+', s, -_string_hex_choice, s.\n"
        + "_string_hex_choice:\n"
        + "               string;\n"
        + "               -'#', hex.";
      Grammar grammar = parse(ixmlIxmlResourceContent, ixmlResource);
      Grammar bnf = BNF.process(grammar);
      assertEquals(expectedResult, bnf.toString());
    }

    @Test
    public void testBnfOfJsonResource() {
      String expectedResult =
            "        -json: ws, -value, ws.\n"
          + "        value: map;\n"
          + "               array;\n"
          + "               number;\n"
          + "               string;\n"
          + "               boolean;\n"
          + "               null.\n"
          + "      boolean: 'f', 'a', 'l', 's', 'e';\n"
          + "               't', 'r', 'u', 'e'.\n"
          + "         null: -'n', -'u', -'l', -'l'.\n"
          + "          map: -'{', -_ws_member_ws_list_option, ws, -'}'.\n"
          + "_ws_member_ws_list:\n"
          + "               ws, member;\n"
          + "               -_ws_member_ws_list, ws, -',', ws, member.\n"
          + "_ws_member_ws_list_option:\n"
          + "               ;\n"
          + "               -_ws_member_ws_list.\n"
          + "       member: key, ws, -':', ws, value.\n"
          + "          key: -string.\n"
          + "        array: -'[', -_ws_value_ws_list_option, ws, -']'.\n"
          + "_ws_value_ws_list:\n"
          + "               ws, -value;\n"
          + "               -_ws_value_ws_list, ws, -',', ws, -value.\n"
          + "_ws_value_ws_list_option:\n"
          + "               ;\n"
          + "               -_ws_value_ws_list.\n"
          + "       number: -_option, int, -_frac_option, -_exp_option.\n"
          + "      _option: ;\n"
          + "               '-'.\n"
          + " _frac_option: ;\n"
          + "               frac.\n"
          + "  _exp_option: ;\n"
          + "               exp.\n"
          + "    -digit1-9: ['1'-'9'].\n"
          + "           -e: ['eE'].\n"
          + "         -exp: e, -_option_1, -_DIGIT_list.\n"
          + "      _choice: '-';\n"
          + "               '+'.\n"
          + "    _option_1: ;\n"
          + "               -_choice.\n"
          + "  _DIGIT_list: DIGIT;\n"
          + "               -_DIGIT_list, DIGIT.\n"
          + "        -frac: '.', -_DIGIT_list.\n"
          + "         -int: '0';\n"
          + "               digit1-9, -_DIGIT_list_option.\n"
          + "_DIGIT_list_option:\n"
          + "               ;\n"
          + "               -_DIGIT_list_option, DIGIT.\n"
          + "       string: -'\"', -_char_list_option, -'\"'.\n"
          + "_char_list_option:\n"
          + "               ;\n"
          + "               -_char_list_option, char.\n"
          + "        -char: ~['\"\\'; #9; #a; #d];\n"
          + "               -'\\', escaped.\n"
          + "     -escaped: '\"';\n"
          + "               '\\';\n"
          + "               '/';\n"
          + "               -'b', +#8;\n"
          + "               -'f', +#c;\n"
          + "               -'n', +#a;\n"
          + "               -'r', +#d;\n"
          + "               -'t', +#9;\n"
          + "               unicode.\n"
          + "      unicode: -'u', code.\n"
          + "        @code: HEXDIG, HEXDIG, HEXDIG, HEXDIG.\n"
          + "       -DIGIT: ['0'-'9'].\n"
          + "      -HEXDIG: DIGIT;\n"
          + "               ['a'-'f'; 'A'-'F'].\n"
          + "          -ws: ;\n"
          + "               -ws, -_9_a_d_choice.\n"
          + "_9_a_d_choice: -' ';\n"
          + "               -#9;\n"
          + "               -#a;\n"
          + "               -#d.";
      Grammar grammar = parse(jsonIxmlResourceContent, jsonIxmlResource);
      Grammar bnf = BNF.process(grammar);
      assertEquals(expectedResult, bnf.toString());
    }

    private void testUrlContent(String url, String resource, String resourceContent) throws IOException, MalformedURLException {
      Grammar grammar = parse(urlContent(new URL(url)), url);
      assertEquals(resourceContent, grammar.toString(), "parsing content of " + url + " did not yield " + resource);
    }

    private Grammar parse(String content, String source) {
      Ixml parser = new Ixml(content);
      try
      {
        parser.parse_ixml();
      }
      catch (ParseException pe)
      {
        throw new RuntimeException("ParseException while processing " + source + ":\n" + parser.getErrorMessage(pe), pe);
      }
      Grammar grammar = parser.grammar();
      PostProcess.process(grammar);
      return  grammar;
    }
}
