// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

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
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class ToREx extends Visitor {
  /** The name of the REx annotation that holds an alternative in ixml notation. */
  public static final String ANNOTATION = "ixml";

  /** The column where annotations start. */
  private static final int ANNOTATION_COLUMN = 51;

  /** The maximum length of lines with abbreviated annotations. */
  private static final int MAX_LINE_LENGTH = 100;

  private static final String HINT =
      "// Alternatives are followed by an <?" + ANNOTATION + " ...?> annotation that holds them in ixml notation, so that\n"
    + "// they are not folded when they differ in what they yield. For alternatives that are just a\n"
    + "// character or token, without deletion, the annotation is for information only, and is abbreviated\n"
    + "// by ... when it is long.\n"
    + "// In order to have REx take the annotations into account, use REx option: -a " + ANNOTATION + "\n"
    + "\n";

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
    return HINT + toREx.sb;
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
    if (hasAnnotation(a))
      appendAnnotation(a);
  }

  /**
   * Append an alternative in ixml notation as an annotation. Annotations keep REx from
   * folding alternatives that look the same, but differ in what they yield, e.g. with
   * respect to insertions, deletions, marks or aliases. The annotation starts in a column
   * of its own, in order to stand out. If there is no space left for it in the current line,
   * it goes to the next line.
   *
   * The annotation of an alternative that is just a single, preserved charset is for
   * information only: It is a single character or token in REx, and the token determines the
   * charset, so there is nothing that could be folded with an alternative that differs in what
   * it yields. Such an alternative would at least need to differ by deletion, which is not
   * abbreviated. So the annotation is abbreviated by ... if it would make the line too long.
   * Any other annotation must be complete, as the omitted part could make a difference.
   */
  private void appendAnnotation(Alt a) {
    int lineLength = sb.length() - (sb.lastIndexOf("\n") + 1);
    if (lineLength >= ANNOTATION_COLUMN - 1) {
      sb.append("\n");
      lineLength = 0;
    }
    sb.append(" ".repeat(ANNOTATION_COLUMN - 1 - lineLength));

    String prefix = "<?" + ANNOTATION + " ";
    String suffix = "?>";
    String text = a.toString();
    if (isPlainCharset(a)) {
      int maxLength = MAX_LINE_LENGTH - (ANNOTATION_COLUMN - 1) - prefix.length() - suffix.length();
      if (text.codePointCount(0, text.length()) > maxLength)
        text = text.substring(0, text.offsetByCodePoints(0, maxLength - 3)) + "...";
      // an annotation must not contain "?>"
      int end = text.indexOf(suffix);
      if (end >= 0)
        text = text.substring(0, end + 1) + "...";
      sb.append(prefix).append(text).append(suffix);
    }
    else {
      // an annotation must not contain "?>", so split it, if present
      String separator = "";
      for (String part : text.split("(?<=\\?)(?=>)", -1)) {
        sb.append(separator).append(prefix).append(part).append(suffix);
        separator = " ";
      }
    }
  }

  private static boolean isPlainCharset(Alt a) {
    List<Term> terms = a.getTerms();
    return terms.size() == 1
        && terms.get(0) instanceof Charset
        && ! ((Charset) terms.get(0)).isDeleted();
  }

  /**
   * Whether an alternative is annotated. This is not the case for the start rule, which has just
   * one alternative, and for rules that are represented as tokens.
   */
  private static boolean hasAnnotation(Alt a) {
    Rule r = a.getRule();
    Rule firstRule = r.getGrammar().getRules().values().iterator().next();
    if (r == firstRule)
      return false;
    Charset singleCharset = singleCharset(r);
    return singleCharset == null || isSingleAscii(singleCharset);
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
}
