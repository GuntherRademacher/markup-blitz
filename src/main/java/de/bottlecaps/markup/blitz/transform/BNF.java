package de.bottlecaps.markup.blitz.transform;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class BNF extends Visitor {
  private Stack<Alts> alts = new Stack<>();
  private Grammar copy;
  private Queue<Rule> justAdded = new LinkedList<>();
  private Queue<Rule> charsets = new LinkedList<>();
  private Set<String> additionalRules = new HashSet<>();
  private Grammar grammar;
  private boolean isolateCharsets;

  private BNF(Grammar grammar, boolean isolateCharsets) {
    this.grammar = grammar;
    this.isolateCharsets = isolateCharsets;
  }

  public static Grammar process(Grammar g) {
    return process(g, false);
  }

  public static Grammar process(Grammar g, boolean isolateCharsets) {
    CombineCharsets cc = new CombineCharsets();
    Grammar grammar = cc.combine(g);

    GenerateAdditionalNames generateNames = new GenerateAdditionalNames(grammar, r -> cc.smallestUsingNonterminal(r.iterator().next()));
    generateNames.visit(grammar);

    BNF bnf = new BNF(grammar, isolateCharsets);
    bnf.visit(grammar);

    bnf.copy.setAdditionalNames(grammar.getAdditionalNames());
    PostProcess.process(bnf.copy);

//    System.out.println("-------REx:\n" + ToREx.process(bnf.copy, generateNames.getAdditionalNames()));

    return bnf.copy;
  }

  @Override
  public void visit(Grammar g) {
    copy = new Grammar();
    super.visit(g);
    if (isolateCharsets)
      for (Rule rule; (rule = charsets.poll()) != null; )
        copy.getRules().put(rule.getName(), rule);
  }

  @Override
  public void visit(Rule r) {
    if (copy.getRules().isEmpty()) {
      // augment grammar with rule: _start: someNonterminal.
      Alt alt = new Alt();
      final var mark = r.getMark() == Mark.NONE
          ? Mark.ELEMENT
          : r.getMark();
      alt.addNonterminal(mark, r.getName());
      Alts alts = new Alts();
      alts.addAlt(alt);
      Rule rule = new Rule(Mark.DELETED, grammar.getAdditionalNames().get(Term.START)[0], alts);
      copy.addRule(rule);
    }
    super.visit(r);
    copy.addRule(new Rule(Mark.NONE, r.getName(), alts.pop()));
    for (Rule rule; (rule = justAdded.poll()) != null; )
      copy.getRules().put(rule.getName(), rule);
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
    else {
      String name = names[0];
      Alts pop = alts.pop();
      Nonterminal nonterminal = new Nonterminal(Mark.DELETED, name);
      alts.peek().last().getTerms().add(nonterminal);
      if (! additionalRules.contains(name)) {
        Rule additionalRule = new Rule(Mark.NONE, name, pop);
        additionalRules.add(additionalRule.getName());
        justAdded.offer(additionalRule);
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
    alts.peek().last().getTerms().add(new Nonterminal(n.getEffectiveMark(), n.getName()));
  }

  @Override
  public void visit(Literal l) {
    throw new IllegalStateException();
  }

  @Override
  public void visit(Insertion i) {
    alts.peek().last().getTerms().add(i.copy());
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
    alts.peek().last().getTerms().add(new Nonterminal(Mark.DELETED, name));
    if (! additionalRules.contains(name)) {
      Rule additionalRule;
      switch (c.getOccurrence()) {
      case ONE_OR_MORE: {
          Alts alts = new Alts();
          Alt alt1 = new Alt();
          alt1.getTerms().add(term.copy());
          Alt alt2 = new Alt();
          alt2.addNonterminal(Mark.DELETED, name);
          if (separator != null)
            alt2.getTerms().add(separator.copy());
          alt2.getTerms().add(term.copy());
          alts.addAlt(alt1);
          alts.addAlt(alt2);
          additionalRule = new Rule(Mark.NONE, name, alts);
          additionalRules.add(additionalRule.getName());
          justAdded.offer(additionalRule);
        }
        break;
      case ZERO_OR_MORE: {
          if (separator == null) {
            Alts alts = new Alts();
            Alt alt1 = new Alt();
            Alt alt2 = new Alt();
            alt2.addNonterminal(Mark.DELETED, name);
            alt2.getTerms().add(term.copy());
            alts.addAlt(alt1);
            alts.addAlt(alt2);
            additionalRule = new Rule(Mark.NONE, name, alts);
            additionalRules.add(additionalRule.getName());
            justAdded.offer(additionalRule);
          }
          else {
            String listName = names[1]; {
              Alts alts = new Alts();
              Alt alt1 = new Alt();
              alt1.getTerms().add(term.copy());
              Alt alt2 = new Alt();
              alt2.addNonterminal(Mark.DELETED, listName);
              alt2.getTerms().add(separator.copy());
              alt2.getTerms().add(term.copy());
              alts.addAlt(alt1);
              alts.addAlt(alt2);
              additionalRule = new Rule(Mark.NONE, listName, alts);
              additionalRules.add(additionalRule.getName());
              justAdded.offer(additionalRule);
            } {
              Alts alts = new Alts();
              Alt alt2 = new Alt();
              alt2.addNonterminal(Mark.DELETED, listName);
              alts.addAlt(new Alt());
              alts.addAlt(alt2);
              additionalRule = new Rule(Mark.NONE, name, alts);
              additionalRules.add(additionalRule.getName());
              justAdded.offer(additionalRule);
            }
          }
        }
        break;
      case ZERO_OR_ONE: {
          Alts alts = new Alts();
          alts.addAlt(new Alt());
          alts.addAlt(new Alt());
          alts.last().getTerms().add(term);
          additionalRule = new Rule(Mark.NONE, name, alts);
          additionalRules.add(additionalRule.getName());
          justAdded.offer(additionalRule);
        }
        break;
      default:
        throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public void visit(Charset c) {
    if (! isolateCharsets || grammar.getAdditionalNames().get(c) == null) {
      alts.peek().last().getTerms().add(c.copy());
    }
    else {
      String name = grammar.getAdditionalNames().get(c)[0];
      Nonterminal nonterminal = new Nonterminal(Mark.DELETED, name);
      alts.peek().last().getTerms().add(nonterminal);
      if (! additionalRules.contains(name)) {
        Alts alts = new Alts();
        Alt alt = new Alt();
        alt.addCharset(c);
        alts.addAlt(alt);
        Rule additionalRule = new Rule(Mark.NONE, name, alts);
        additionalRules.add(additionalRule.getName());
        charsets.offer(additionalRule);
      }
    }
  }
}
