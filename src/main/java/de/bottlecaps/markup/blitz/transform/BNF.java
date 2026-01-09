// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Occurrence;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class BNF extends Visitor {
  private Stack<Alts> alts = new Stack<>();
  private Grammar copy;
  private Map<String, Rule> justAdded = new LinkedHashMap<>();
  private Queue<Rule> charsets = new LinkedList<>();
  private Set<String> coveredRules = new HashSet<>();
  private Grammar grammar;
  private boolean isolateCharsets;

  private BNF(Grammar grammar, boolean isolateCharsets) {
    this.grammar = grammar;
    this.isolateCharsets = isolateCharsets;
  }

  public static Grammar process(Grammar g) {
    return process(g, Collections.emptySet());
  }

  public static Grammar process(Grammar g, Set<Blitz.Option> options) {
    return process(g, false, options);
  }

  public static Grammar process(Grammar g, boolean isolateCharsets, Set<Blitz.Option> options) {
    long t0 = 0, t1 = 0, t2 = 0, t3 = 0;
    boolean timing = options.contains(Blitz.Option.TIMING);

    if (timing)
      t0 = System.currentTimeMillis();
    ClassifyCharacters cc = new ClassifyCharacters(new Grammar(g));
    Grammar grammar = cc.combine(g, options);

    if (timing)
      t1 = System.currentTimeMillis();

    new GenerateAdditionalNames(grammar).visit(grammar);

    if (timing)
      t2 = System.currentTimeMillis();
    BNF bnf = new BNF(grammar, isolateCharsets);
    bnf.visit(grammar);
    bnf.copy.setAdditionalNames(grammar.getAdditionalNames());
    PostProcess.process(bnf.copy);

    if (timing) {
      t3 = System.currentTimeMillis();
      System.err.println("                                charset combination time: " + (t1 - t0) + " msec");
      System.err.println("                                    name generation time: " + (t2 - t1) + " msec");
      System.err.println("                                                BNF time: " + (t3 - t2) + " msec");
    }

    return bnf.copy;
  }

  @Override
  public void visit(Grammar g) {
    copy = new Grammar(g);
    super.visit(g);
    if (isolateCharsets)
      for (Rule rule; (rule = charsets.poll()) != null; )
        copy.addRule(rule);
  }

  @Override
  public void visit(Rule r) {
    if (copy.getRules().isEmpty()) {
      // augment grammar with rule: _start: someNonterminal.
      Alt alt = new Alt();
      final var mark = r.getMark() == Mark.NONE
          ? Mark.NODE
          : r.getMark();
      alt.addNonterminal(mark, r.getAlias(), r.getName());
      Alts alts = new Alts();
      alts.addAlt(alt);
      Rule rule = new Rule(Mark.DELETE, null, grammar.getAdditionalNames().get(Term.START)[0], alts);
      copy.addRule(rule);
    }
    if (! copy.getRules().containsKey(r.getName())) {
      super.visit(r);
      Alts a = alts.pop();
      if (justAdded.containsKey(r.getName())) {
        copy.addRule(justAdded.remove(r.getName()));
      }
      else {
        copy.addRule(new Rule(Mark.NONE, null, r.getName(), a));
        coveredRules.add(r.getName());
      }
      for (Rule rule : justAdded.values())
        copy.addRule(rule);
      justAdded.clear();
    }
  }

  @Override
  public void visit(Alts a) {
    alts.push(new Alts());
    super.visit(a);
    String[] names = grammar.getAdditionalNames().get(a);
    if (names == null) {
      // add to enclosing term, unless rule level Alts
      if (alts.size() != 1) {
        Alts nested = alts.pop();
        alts.peek().last().addAlts(nested);
      }
    }
    else if (alts.size() != 1) {
      String name = names[0];
      Alts pop = alts.pop();
      Nonterminal nonterminal = new Nonterminal(Mark.DELETE, null, name);
      alts.peek().last().getTerms().add(nonterminal);
      if (! coveredRules.contains(name)) {
        Rule additionalRule = new Rule(Mark.NONE, null, name, pop);
        coveredRules.add(additionalRule.getName());
        justAdded.put(additionalRule.getName(), additionalRule);
      }
    }
  }

  @Override
  public void visit(Alt a) {
    alts.peek().addAlt(new Alt());
    super.visit(a);
  }

  @Override
  public void visit(Nonterminal n) {
    alts.peek().last().getTerms().add(new Nonterminal(n.getEffectiveMark(), n.getEffectiveAlias(), n.getName()));
  }

  @Override
  public void visit(Literal l) {
    throw new IllegalStateException();
  }

  @Override
  public void visit(Control c) {
    super.visit(c);
    Term separator = c.getSeparator() == null
        ? null
        : alts.peek().last().removeLast();
    Term term = alts.peek().last().removeLast();
    String[] names = grammar.getAdditionalNames().get(c);
    String name = names[0];
    alts.peek().last().getTerms().add(new Nonterminal(Mark.DELETE, null, name));
    Rule additionalRule;
    switch (c.getOccurrence()) {
    case ONE_OR_MORE:
      if (! coveredRules.contains(name)) {
        Alts alts = new Alts();
        Alt alt1 = new Alt();
        alt1.getTerms().add(term.copy());
        Alt alt2 = new Alt();
        alt2.addNonterminal(Mark.DELETE, null, name);
        if (separator != null)
          alt2.getTerms().add(separator.copy());
        alt2.getTerms().add(term.copy());
        alts.addAlt(alt1);
        alts.addAlt(alt2);
        additionalRule = new Rule(Mark.NONE, null, name, alts);
        coveredRules.add(name);
        justAdded.put(name, additionalRule);
      }
      break;
    case ZERO_OR_MORE:
      if (separator == null) {
        if (! coveredRules.contains(name)) {
          Alts alts = new Alts();
          Alt alt1 = new Alt();
          Alt alt2 = new Alt();
          alt2.addNonterminal(Mark.DELETE, null, name);
          alt2.getTerms().add(term.copy());
          alts.addAlt(alt1);
          alts.addAlt(alt2);
          additionalRule = new Rule(Mark.NONE, null, name, alts);
          coveredRules.add(name);
          justAdded.put(name, additionalRule);
        }
      }
      else {
        String listName = names[1];
        if (! coveredRules.contains(listName)) {
          Alts alts = new Alts();
          Alt alt1 = new Alt();
          alt1.getTerms().add(term.copy());
          Alt alt2 = new Alt();
          alt2.addNonterminal(Mark.DELETE, null, listName);
          alt2.getTerms().add(separator.copy());
          alt2.getTerms().add(term.copy());
          alts.addAlt(alt1);
          alts.addAlt(alt2);
          additionalRule = new Rule(Mark.NONE, null, listName, alts);
          coveredRules.add(listName);
          justAdded.put(listName, additionalRule);
        } 
        if (! coveredRules.contains(name)) {
          Alts alts = new Alts();
          Alt alt2 = new Alt();
          alt2.addNonterminal(Mark.DELETE, null, listName);
          alts.addAlt(new Alt());
          alts.addAlt(alt2);
          additionalRule = new Rule(Mark.NONE, null, name, alts);
          coveredRules.add(name);
          justAdded.put(name, additionalRule);
        }
      }
      break;
    case ZERO_OR_ONE:
      if (! coveredRules.contains(name)) {
        Alts alts = new Alts();
        alts.addAlt(new Alt());
        alts.addAlt(new Alt());
        alts.last().getTerms().add(term);
        additionalRule = new Rule(Mark.NONE, null, name, alts);
        coveredRules.add(name);
        justAdded.put(name, additionalRule);
      }
      break;
    default:
      throw new IllegalArgumentException();
    }
  }

  @Override
  public void visit(Charset c) {
    if (! isolateCharsets || grammar.getAdditionalNames().get(c) == null) {
      alts.peek().last().getTerms().add(c.copy());
    }
    else {
      String name = grammar.getAdditionalNames().get(c)[0];
      Nonterminal nonterminal = new Nonterminal(Mark.DELETE, null, name);
      alts.peek().last().getTerms().add(nonterminal);
      if (! coveredRules.contains(name)) {
        Alts alts = new Alts();
        Alt alt = new Alt();
        alt.addCharset(c);
        alts.addAlt(alt);
        Rule additionalRule = new Rule(Mark.NONE, null, name, alts);
        coveredRules.add(additionalRule.getName());
        charsets.offer(additionalRule);
      }
    }
  }

  @Override
  public void visit(Insertion i) {
    if (i.getNext() == null && ! isRepeated(i)) {
      alts.peek().last().getTerms().add(i.copy());
    }
    else {
      String name = grammar.getAdditionalNames().get(i)[0];
      Nonterminal nonterminal = new Nonterminal(Mark.DELETE, null, name);
      alts.peek().last().getTerms().add(nonterminal);
      if (! coveredRules.contains(name)) {
        Alts alts = new Alts();
        Alt alt = new Alt();
        alt.getTerms().add(i);
        alts.addAlt(alt);
        Rule additionalRule = new Rule(Mark.NONE, null, name, alts);
        coveredRules.add(additionalRule.getName());
        justAdded.put(additionalRule.getName(), additionalRule);
      }
    }
  }

  private boolean isRepeated(Node node) {
    while (! (node.getParent() instanceof Rule)) {
      node = node.getParent();
      if (node instanceof Control && ((Control) node).getOccurrence() != Occurrence.ZERO_OR_ONE)
        return true;
    }
    return false;
  }

}
