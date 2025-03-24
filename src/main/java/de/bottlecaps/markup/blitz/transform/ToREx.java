// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import static de.bottlecaps.markup.Blitz.url;
import static de.bottlecaps.markup.Blitz.urlContent;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Ixml;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class ToREx extends Visitor {
  private final String padding = "                ";

  private StringBuilder sb = new StringBuilder();
  private Map<String, String> charsets = new LinkedHashMap<>();
  private Grammar grammar;

  private ToREx() {
  }

  public static String process(Grammar g) {
    ToREx toREx = new ToREx();
    toREx.grammar = g;
    toREx.visit(Charset.END);
    toREx.sb.setLength(0);
    toREx.visit(g);

    if (! toREx.charsets.isEmpty()) {
      toREx.sb.append("\n\n<?TOKENS?>\n");
      toREx.charsets.values().forEach(toREx.sb::append);
    }
    return toREx.sb.toString();
  }

  @Override
  public void visit(Rule r) {
    Rule firstRule = r.getGrammar().getRules().values().iterator().next();
    Charset singleCharset = singleCharset(r);
	if (singleCharset != null && ! isSingleAscii(singleCharset)) {
      super.visit(r);
    }
	else {
      if (r != firstRule)
        sb.append("\n");
      String name = r.getName();
      sb.append(name);
      int paddingLength = padding.length() - name.length() - 2;
      if (paddingLength < 1) {
        sb.append("\n");
        paddingLength = padding.length() - 2;
      }
      sb.append(padding.substring(0, paddingLength));
      sb.append("::=");
      super.visit(r);
      if (r == firstRule)
        sb.append(" ").append(grammar.getAdditionalNames().get(Charset.END)[0]);
	}
  }

  @Override
  public void visit(Alt a) {
    if (a != a.getRule().getAlts().getAlts().iterator().next())
      sb.append("\n").append(padding).append("|");
    super.visit(a);
  }

  @Override
  public void visit(Charset c) {
    if (isSingleAscii(c)) {
      sb.append(" ");
      sb.append(c.getRangeSet().iterator().next().toREx());
    }
    else {
      String[] name = grammar.getAdditionalNames().get(c);
      if (c.getRule() == null || ! name[0].equals(c.getRule().getName())) {
        sb.append(" ");
        sb.append(name[0]);
      }
      if (! charsets.containsKey(name[0])) {
        StringBuilder tb = new StringBuilder("\n").append(name[0]);
        int paddingLength = padding.length() - name[0].length() - 2;
        if (paddingLength < 1) {
          tb.append("\n");
          paddingLength = padding.length() - 2;
        }
        tb.append(padding.substring(0, paddingLength));
        tb.append("::= ");
        if (c == Charset.END)
          tb.append("$");
        else
          tb.append(c.getRangeSet().stream().map(Range::toREx).collect(Collectors.joining("\n" + padding + "| ")));
        charsets.put(name[0], tb.toString());
      }
    }
  }

  private static Charset singleCharset(Rule r) {
    List<Alt> alts = r.getAlts().getAlts();
    if (alts.size() == 1) {
      List<Term> terms = alts.get(0).getTerms();
      if (terms.size() == 1) {
        Term term = terms.get(0);
        if (term instanceof Charset)
          return (Charset) term;
      }
    }
    return null;
  }

  private static boolean isSingleAscii(Charset c) {
    RangeSet rangeSet = c.getRangeSet();
    return rangeSet.isSingleton() && Codepoint.isAscii(rangeSet.iterator().next().getFirstCodepoint());
  }

  @Override
  public void visit(Nonterminal n) {
    sb.append(" ").append(n.getName());
  }

  @Override
  public void visit(Literal l) {
    throw new IllegalStateException();
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Usage: java " + ToREx.class.getName() + " <ixml-grammar>");
      System.exit(1);
    }
    String grammar = args[0];
    String grammarString = grammar.startsWith("!")
            ? grammar.substring(1)
            : urlContent(url(grammar));
    System.out.println(process(BNF.process(Ixml.parse(grammarString))));
  }
}
