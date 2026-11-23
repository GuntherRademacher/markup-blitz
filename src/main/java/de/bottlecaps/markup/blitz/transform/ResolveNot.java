// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Not;
import de.bottlecaps.markup.blitz.grammar.Term;

/**
 * Resolve the operand of each negative lookahead {@code !operand} to the finite set
 * of character sequences that it matches. The operand may be a string, an encoded
 * character, a character set, or a nonterminal whose rule only consists of strings,
 * encoded characters, character sets, groups and nonterminals of the same kind, i.e.
 * without repetitions, options, empty alternatives or recursion.
 */
public class ResolveNot extends Visitor {
  /** Upper limit for the number of sequences that an operand may expand to. */
  private static final int MAX_SEQUENCES = 4096;

  private final Grammar grammar;
  private final Set<String> active = new HashSet<>();
  private Not not;

  private ResolveNot(Grammar grammar) {
    this.grammar = grammar;
  }

  /**
   * Resolve all negative lookaheads of a grammar.
   *
   * @param grammar the grammar
   */
  public static void process(Grammar grammar) {
    new ResolveNot(grammar).visit(grammar);
  }

  @Override
  public void visit(Not n) {
    if (n.getSequences() != null)
      return;
    not = n;
    active.clear();
    List<RangeSet[]> sequences = expand(n.getOperand());
    n.setSequences(sequences.toArray(RangeSet[][]::new));
  }

  private List<RangeSet[]> expand(Term term) {
    List<RangeSet[]> result = new ArrayList<>();
    if (term instanceof Literal) {
      result.add(Arrays.stream(((Literal) term).getCodepoints())
          .mapToObj(c -> RangeSet.builder().add(c).build())
          .toArray(RangeSet[]::new));
    }
    else if (term instanceof Charset) {
      result.add(new RangeSet[] {((Charset) term).getRangeSet()});
    }
    else if (term instanceof Nonterminal) {
      String name = ((Nonterminal) term).getName();
      if (! active.add(name))
        fail("the rule " + name + " is recursive");
      result = expand(grammar.getRule(name).getAlts());
      active.remove(name);
    }
    else if (term instanceof Alts) {
      for (Alt alt : ((Alts) term).getAlts()) {
        List<RangeSet[]> sequences = new ArrayList<>();
        sequences.add(new RangeSet[0]);
        for (Term t : alt.getTerms()) {
          if (t instanceof Insertion)
            continue;
          List<RangeSet[]> product = new ArrayList<>();
          List<RangeSet[]> suffixes = expand(t);
          for (RangeSet[] prefix : sequences) {
            for (RangeSet[] suffix : suffixes) {
              RangeSet[] s = Arrays.copyOf(prefix, prefix.length + suffix.length);
              System.arraycopy(suffix, 0, s, prefix.length, suffix.length);
              product.add(s);
              if (product.size() > MAX_SEQUENCES)
                fail("it matches more than " + MAX_SEQUENCES + " different sequences");
            }
          }
          sequences = product;
        }
        for (RangeSet[] s : sequences)
          if (s.length == 0)
            fail("it can match the empty string, so the lookahead would never succeed");
        result.addAll(sequences);
        if (result.size() > MAX_SEQUENCES)
          fail("it matches more than " + MAX_SEQUENCES + " different sequences");
      }
    }
    else {
      fail("it contains " + term + ", but only strings, encoded characters, character sets,"
         + " groups and nonterminals matching fixed sequences of characters are supported"
         + " (no repetitions, options or nested !)");
    }
    return result;
  }

  private void fail(String reason) {
    throw new BlitzException("Unsupported operand of negative lookahead " + not
        + (not.getRule() == null ? "" : " in rule " + not.getRule().getName())
        + ": " + reason);
  }
}
