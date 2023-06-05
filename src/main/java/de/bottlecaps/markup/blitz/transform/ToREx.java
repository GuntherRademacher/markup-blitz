package de.bottlecaps.markup.blitz.transform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;

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
    toREx.visit(g);
    if (! toREx.charsets.isEmpty()) {
      toREx.sb.append("\n\n<?TOKENS?>\n");
      toREx.charsets.values().forEach(toREx.sb::append);
    }
    return toREx.sb.toString();
  }

  @Override
  public void visit(Rule r) {
    if (r != r.getGrammar().getRules().values().iterator().next())
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
  }

  @Override
  public void visit(Alt a) {
    if (a != a.getRule().getAlts().getAlts().iterator().next())
      sb.append("\n").append(padding).append("|");
    super.visit(a);
  }

  @Override
  public void visit(Charset c) {
    sb.append(" ");
    RangeSet rangeSet = RangeSet.of(c);
    if (rangeSet.charCount() == 1 && Range.isAscii(rangeSet.iterator().next().getFirstCodePoint())) {
      sb.append(rangeSet.iterator().next().toREx());
    }
    else {
      String[] name = grammar.getAdditionalNames().get(c);
      sb.append(name[0]);
      if (! charsets.containsKey(name[0])) {
        StringBuilder tb = new StringBuilder("\n").append(name[0]);
        int paddingLength = padding.length() - name[0].length() - 2;
        if (paddingLength < 1) {
          tb.append("\n");
          paddingLength = padding.length() - 2;
        }
        tb.append(padding.substring(0, paddingLength));
        tb.append(rangeSet.stream().map(Range::toREx).collect(Collectors.joining("\n" + padding + "| ", "::= ", "")));
        charsets.put(name[0], tb.toString());
      }
    }
  }

  @Override
  public void visit(Nonterminal n) {
    sb.append(" ").append(n.getName());
  }

  @Override
  public void visit(Literal l) {
    throw new IllegalStateException();
  }
}
