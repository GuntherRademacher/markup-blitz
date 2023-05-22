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
          + "               -s, -_Zs_9_a_d_comment_choice.\n"
          + "_Zs_9_a_d_comment_choice:\n"
          + "               -[Zs; #9; #a; #d];\n"
          + "               comment.\n"
          + "       prolog: version, s.\n"
          + "         rule: -_mark_s_option, name, s, -_choice, s, -alts, -'.'.\n"
          + "_mark_s_option:\n"
          + "               ;\n"
          + "               mark, s.\n"
          + "      _choice: -'=';\n"
          + "               -':'.\n"
          + "          -RS: -_Zs_9_a_d_comment_choice;\n"
          + "               -RS, -_Zs_9_a_d_comment_choice.\n"
          + "      comment: -'{', -_9_a_d_z_d7ff_e000_fffd_10000_10fffd_comment_list_option, -'}'.\n"
          + "_9_a_d_z_d7ff_e000_fffd_10000_10fffd_comment_choice:\n"
          + "               [#9-#a; #d; ' '-'z'; '|'; '~'-#d7ff; #e000-#fffd; #10000-#10fffd];\n"
          + "               comment.\n"
          + "_9_a_d_z_d7ff_e000_fffd_10000_10fffd_comment_list_option:\n"
          + "               ;\n"
          + "               -_9_a_d_z_d7ff_e000_fffd_10000_10fffd_comment_list_option, -_9_a_d_z_d7ff_e000_fffd_10000_10fffd_comment_choice.\n"
          + "      version: -'i', -'x', -'m', -'l', RS, -'v', -'e', -'r', -'s', -'i', -'o', -'n', RS, string, s, -'.'.\n"
          + "        @mark: ['@^-'].\n"
          + "        @name: -_L_choice, -_L_.·‿⁀_Nd_Mn_list_option.\n"
          + "    _L_choice: '_';\n"
          + "               [L].\n"
          + "_L_.·‿⁀_Nd_Mn_choice:\n"
          + "               '_';\n"
          + "               [L];\n"
          + "               '-';\n"
          + "               '.';\n"
          + "               '·';\n"
          + "               '‿';\n"
          + "               '⁀';\n"
          + "               [Nd];\n"
          + "               [Mn].\n"
          + "_L_.·‿⁀_Nd_Mn_list_option:\n"
          + "               ;\n"
          + "               -_L_.·‿⁀_Nd_Mn_list_option, -_L_.·‿⁀_Nd_Mn_choice.\n"
          + "         alts: alt;\n"
          + "               -alts, -_choice_1, s, alt.\n"
          + "    _choice_1: -';';\n"
          + "               -'|'.\n"
          + "      @string: -'\"', -_dchar_list, -'\"';\n"
          + "               -'''', -_schar_list, -''''.\n"
          + "  _dchar_list: dchar;\n"
          + "               -_dchar_list, dchar.\n"
          + "  _schar_list: schar;\n"
          + "               -_schar_list, schar.\n"
          + "          alt: ;\n"
          + "               -_term_s_list.\n"
          + " _term_s_list: term;\n"
          + "               -_term_s_list, -',', s, term.\n"
          + "        dchar: [#9; ' '-'!'; '#'-#d7ff; #e000-#fffd; #10000-#10fffd];\n"
          + "               '\"', -'\"'.\n"
          + "        schar: [#9; ' '-'&'; '('-#d7ff; #e000-#fffd; #10000-#10fffd];\n"
          + "               '''', -''''.\n"
          + "        -term: factor;\n"
          + "               option;\n"
          + "               repeat0;\n"
          + "               repeat1.\n"
          + "      -factor: terminal;\n"
          + "               nonterminal;\n"
          + "               insertion;\n"
          + "               -'(', s, alts, -')', s.\n"
          + "       option: factor, -'?', s.\n"
          + "      repeat0: factor, -_s_s_sep_choice.\n"
          + "_s_s_sep_choice:\n"
          + "               -'*', s;\n"
          + "               -'*', -'*', s, sep.\n"
          + "      repeat1: factor, -_s_s_sep_choice_1.\n"
          + "_s_s_sep_choice_1:\n"
          + "               -'+', s;\n"
          + "               -'+', -'+', s, sep.\n"
          + "    -terminal: literal;\n"
          + "               charset.\n"
          + "  nonterminal: -_mark_s_option, name, s.\n"
          + "    insertion: -'+', s, -_string_hex_choice, s.\n"
          + "_string_hex_choice:\n"
          + "               string;\n"
          + "               -'#', hex.\n"
          + "          sep: factor.\n"
          + "      literal: quoted;\n"
          + "               encoded.\n"
          + "     -charset: inclusion;\n"
          + "               exclusion.\n"
          + "         @hex: -_0_9_a_f_A_F_choice;\n"
          + "               -hex, -_0_9_a_f_A_F_choice.\n"
          + "_0_9_a_f_A_F_choice:\n"
          + "               ['0'-'9'];\n"
          + "               ['a'-'f'];\n"
          + "               ['A'-'F'].\n"
          + "      -quoted: -_tmark_s_option, string, s.\n"
          + "_tmark_s_option:\n"
          + "               ;\n"
          + "               tmark, s.\n"
          + "     -encoded: -_tmark_s_option, -'#', hex, s.\n"
          + "    inclusion: -_tmark_s_option, set.\n"
          + "    exclusion: -_tmark_s_option, -'~', s, set.\n"
          + "       @tmark: ['^-'].\n"
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
          + "       -class: code.\n"
          + "        @from: character.\n"
          + "          @to: character.\n"
          + "        @code: ['A'-'Z'], -_a_z_option.\n"
          + "  _a_z_option: ;\n"
          + "               ['a'-'z'].\n"
          + "   -character: -'\"', dchar, -'\"';\n"
          + "               -'''', schar, -'''';\n"
          + "               '#', hex.";
      Grammar grammar = parse(ixmlIxmlResourceContent, ixmlResource);
      Grammar bnf = BNF.process(grammar);
      assertEquals(expectedResult, bnf.toString());
    }

    @Test
    public void testBnfOfJsonResource() {
      String expectedResult =
            "        -json: ws, -value, ws.\n"
            + "          -ws: ;\n"
            + "               -ws, -_9_a_d_choice.\n"
            + "_9_a_d_choice: -[#9-#a; #d];\n"
            + "               -' '.\n"
            + "        value: map;\n"
            + "               array;\n"
            + "               number;\n"
            + "               string;\n"
            + "               boolean;\n"
            + "               null.\n"
            + "          map: -'{', -_ws_member_ws_list_option, ws, -'}'.\n"
            + "_ws_member_ws_list:\n"
            + "               ws, member;\n"
            + "               -_ws_member_ws_list, ws, -',', ws, member.\n"
            + "_ws_member_ws_list_option:\n"
            + "               ;\n"
            + "               -_ws_member_ws_list.\n"
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
            + "       string: -'\"', -_char_list_option, -'\"'.\n"
            + "_char_list_option:\n"
            + "               ;\n"
            + "               -_char_list_option, char.\n"
            + "      boolean: 'f', 'a', 'l', 's', 'e';\n"
            + "               't', 'r', 'u', 'e'.\n"
            + "         null: -'n', -'u', -'l', -'l'.\n"
            + "       member: key, ws, -':', ws, value.\n"
            + "         -int: '0';\n"
            + "               ['1'-'9'], -_0_1_9_list_option.\n"
            + "_0_1_9_choice: '0';\n"
            + "               ['1'-'9'].\n"
            + "_0_1_9_list_option:\n"
            + "               ;\n"
            + "               -_0_1_9_list_option, -_0_1_9_choice.\n"
            + "        -frac: '.', -_0_1_9_list.\n"
            + "  _0_1_9_list: -_0_1_9_choice;\n"
            + "               -_0_1_9_list, -_0_1_9_choice.\n"
            + "         -exp: -_E_e_choice, -_option_1, -_0_1_9_list.\n"
            + "  _E_e_choice: 'E';\n"
            + "               'e'.\n"
            + "      _choice: '+';\n"
            + "               '-'.\n"
            + "    _option_1: ;\n"
            + "               -_choice.\n"
            + "        -char: -_G_Z_g_k_m_o_q_v_z_d7ff_e000_fffd_10000_10fffd_._choice;\n"
            + "               -'\\', escaped.\n"
            + "_G_Z_g_k_m_o_q_v_z_d7ff_e000_fffd_10000_10fffd_._choice:\n"
            + "               ' ';\n"
            + "               ['!'; '#'-'*'; ';'-'@'; 'G'-'Z'; '^'-'`'; 'g'-'k'; 'm'; 'o'-'q'; 'v'-'z'; '|'; '~'-#d7ff; #e000-#fffd; #10000-#10fffd];\n"
            + "               '+';\n"
            + "               ',';\n"
            + "               '-';\n"
            + "               '.';\n"
            + "               '/';\n"
            + "               '0';\n"
            + "               ['1'-'9'];\n"
            + "               ':';\n"
            + "               ['A'-'D'; 'F'; 'c'-'d'];\n"
            + "               'E';\n"
            + "               '[';\n"
            + "               ']';\n"
            + "               'a';\n"
            + "               'b';\n"
            + "               'e';\n"
            + "               'f';\n"
            + "               'l';\n"
            + "               'n';\n"
            + "               'r';\n"
            + "               's';\n"
            + "               't';\n"
            + "               'u';\n"
            + "               '{';\n"
            + "               '}'.\n"
            + "          key: -string.\n"
            + "     -escaped: -_choice_1;\n"
            + "               -'b', +#8;\n"
            + "               -'f', +#c;\n"
            + "               -'n', +#a;\n"
            + "               -'r', +#d;\n"
            + "               -'t', +#9;\n"
            + "               unicode.\n"
            + "    _choice_1: '\"';\n"
            + "               '/';\n"
            + "               '\\'.\n"
            + "      unicode: -'u', code.\n"
            + "        @code: -_0_1_9_A_D_F_c_d_E_a_b_e_f_choice, -_0_1_9_A_D_F_c_d_E_a_b_e_f_choice, -_0_1_9_A_D_F_c_d_E_a_b_e_f_choice, -_0_1_9_A_D_F_c_d_E_a_b_e_f_choice.\n"
            + "_0_1_9_A_D_F_c_d_E_a_b_e_f_choice:\n"
            + "               '0';\n"
            + "               ['1'-'9'];\n"
            + "               ['A'-'D'; 'F'; 'c'-'d'];\n"
            + "               'E';\n"
            + "               'a';\n"
            + "               'b';\n"
            + "               'e';\n"
            + "               'f'.";
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
