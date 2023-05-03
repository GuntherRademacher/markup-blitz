package de.bottlecaps.markupblitz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Control extends Term {
  private final Occurrence occurrence;
  private final Term term;
  private final Term separator;

  public Control(Occurrence occurrence, Term term, Term separator) {
    this.occurrence = occurrence;
    this.term = term;
    this.separator = separator;
  }

  public Occurrence getOccurrence() {
    return occurrence;
  }

  public Term getTerm() {
    return term;
  }

  public Term getSeparator() {
    return separator;
  }

  @Override
  public Node[] toBnf() {
    Node[] termBnf = term.toBnf();
    List<Node> rules = new ArrayList<>();
    switch (occurrence) {
    case ONE_OR_MORE:
      if (separator == null) {
        // e* ==> x where -x: e; x, e.
        String name = "x" + ++Alt.n;
        Rule rule = new Rule(Mark.DELETED, name);
        rule.addAlt(new Alt()
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        rule.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        return Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name)),
              Arrays.stream(termBnf).skip(1)),
              Stream.of(rule)),
              rules.stream())
            .toArray(Node[]::new);
      }
      else {
        // e* ==> x where -x: e; x, s, e.
        String name = "x" + ++Alt.n;
        Node[] separatorBnf = separator == null ? new Node[] {} : separator.toBnf();
        Rule rule = new Rule(Mark.DELETED, name);
        rule.addAlt(new Alt()
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        rule.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name)
            .mergeTerm((Term) separatorBnf[0].toBnf()[0], rules)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        return Stream.concat(Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name)),
              Arrays.stream(termBnf).skip(1)),
              Arrays.stream(separatorBnf).skip(1)),
              Stream.of(rule)),
              rules.stream())
            .toArray(Node[]::new);
      }
    case ZERO_OR_MORE:
      if (separator == null) {
        // e* ==> x where -x: ; x, e.
        String name = "x" + ++Alt.n;
        Rule rule = new Rule(Mark.DELETED, name);
        rule.addAlt(new Alt());
        rule.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        return Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name)),
              Arrays.stream(termBnf).skip(1)),
              Stream.of(rule)),
              rules.stream())
            .toArray(Node[]::new);
      }
      else {
        // e* ==> x where -x: ; x, y. -y: e; y, s, e.
        String name1 = "x" + ++Alt.n;
        String name2 = "x" + ++Alt.n;
        Node[] separatorBnf = separator == null ? new Node[] {} : separator.toBnf();
        Rule rule1 = new Rule(Mark.DELETED, name1);
        rule1.addAlt(new Alt());
        rule1.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name2));
        Rule rule2 = new Rule(Mark.DELETED, name2);
        rule2.addAlt(new Alt()
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        rule2.addAlt(new Alt()
            .addNonterminal(Mark.NONE, name2)
            .mergeTerm((Term) separatorBnf[0].toBnf()[0], rules)
            .mergeTerm((Term) termBnf[0].toBnf()[0], rules));
        return Stream.concat(Stream.concat(Stream.concat(Stream.concat(Stream.concat(
              Stream.of(new Nonterminal(Mark.NONE, name1)),
              Arrays.stream(termBnf).skip(1)),
              Arrays.stream(separatorBnf).skip(1)),
              Stream.of(rule1)),
              Stream.of(rule2)),
              rules.stream())
            .toArray(Node[]::new);
      }
    case ZERO_OR_ONE:
      // e? ==> x where -x: ; e.
      String name = "x" + ++Alt.n;
      Rule rule = new Rule(Mark.DELETED, name);
      rule.addAlt(new Alt());
      rule.addAlt(new Alt().mergeTerm(term, rules));
      return Stream.concat(Stream.concat(
            Stream.of(new Nonterminal(Mark.NONE, name)),
            Stream.of(rule)),
            rules.stream())
          .toArray(Node[]::new);
    default:
      throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    return term.toString() + occurrence.toString()
         + (separator == null
           ? ""
           : occurrence.toString() + separator.toString());
  }

  @Override
  public void accept(Visitor v) {
    term.accept(v);
    if (separator != null)
      separator.accept(v);
  }
}