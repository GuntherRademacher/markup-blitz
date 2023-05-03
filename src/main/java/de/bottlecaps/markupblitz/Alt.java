package de.bottlecaps.markupblitz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Alt extends Node {
  private static int n = 0;
  private List<Term> terms;

  public Alt() {
    terms = new ArrayList<>();
  }

  public Term removeLast() {
    return terms.remove(terms.size() - 1);
  }

//  public Alt addTerm(Term t) {
//    terms.add(t);
//    return this;
//  }

  public Alt addNonterminal(Mark mark, String name) {
    terms.add(new Nonterminal(mark, name));
    return this;
  }

  public void addString(boolean deleted, String value) {
    terms.add(new Literal(deleted, value, false));
  }

  public void addCodePoint(boolean deleted, String value) {
    terms.add(new Literal(deleted, value, true));
  }

  public void addCharSet(CharSet charSet) {
    terms.add(charSet);
  }

  public void addAlts(Alts alts) {
    terms.add(alts);
  }

  public void addControl(Occurrence occurrence, Term term, Term separator) {
    terms.add(new Control(occurrence, term, separator));
  }

  public void addStringInsertion(String string) {
    terms.add(new Insertion(string, false));
  }

  public void addHexInsertion(String hex) {
    terms.add(new Insertion(hex, true));
  }

  @Override
  public Node[] toBnf() {
    Alt alt = new Alt();
    List<Node> rules = new ArrayList<>();
    terms.forEach(a -> {
      Node[] bnf = a.toBnf();
      alt.mergeTerm((Term) bnf[0], rules);
      Arrays.stream(bnf)
        .skip(1)
        .forEach(r -> rules.add(r));
    });
    return Stream.concat(Stream.of(alt), rules.stream()).toArray(Node[]::new);
  }

  private Alt mergeTerm(Term term, List<Node> rules) {
    if (! (term instanceof Alts)) {
      terms.add(term);
    }
    else if (((Alts) term).alts.size() == 1) {
      for (Term t : ((Alts) term).alts.get(0).terms)
        terms.add(t);
    }
    else {
      // (e1; e2; ...; en) ==> x where -x: e1; e2; ...; en.
      String name = "x" + ++n;
      terms.add(new Nonterminal(Mark.NONE, name));
      Rule rule = new Rule(Mark.DELETED, name);
      for (Alt a : ((Alts) term).alts)
        rule.addAlt(a);
      rules.add(rule);
    }
    return this;
  }

  @Override
  public String toString() {
    return terms.stream().map(Term::toString).collect(Collectors.joining(", "));
  }

  public static abstract class Term extends Node {
  }

  public static class Nonterminal extends Term {
    private Mark mark;
    private String name;

    public Nonterminal(Mark mark, String name) {
      this.mark = mark;
      this.name = name;
    }

    @Override
    public String toString() {
      return mark + name;
    }
  }

  public static class Literal extends Term {
    protected boolean deleted;
    protected String value;
    protected Boolean isHex;

    public Literal(boolean deleted, String value, boolean isHex) {
      this.deleted = deleted;
      this.value = value;
      this.isHex = isHex;
    }

    @Override
    public String toString() {
      return
          (deleted ? "-" : "") +
          (isHex ? value : "'" + value.replace("'", "''") + "'");
    }
  }

  public static class Insertion extends Literal {

    public Insertion(String value, boolean isHex) {
      super(false, value, isHex);
    }

    @Override
    public String toString() {
      return "+" + super.toString();
    }
  }

  public static class Control extends Term {
    private Occurrence occurrence;
    private Term term;
    private Term separator;

    public Control(Occurrence occurrence, Term term, Term separator) {
      this.occurrence = occurrence;
      this.term = term;
      this.separator = separator;
    }

    @Override
    public Node[] toBnf() {
      Node[] termBnf = term.toBnf();
      List<Node> rules = new ArrayList<>();
      switch (occurrence) {
      case ONE_OR_MORE:
        if (separator == null) {
          // e* ==> x where -x: e; x, e.
          String name = "x" + ++n;
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
          String name = "x" + ++n;
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
          String name = "x" + ++n;
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
          String name1 = "x" + ++n;
          String name2 = "x" + ++n;
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
        String name = "x" + ++n;
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
  }
}
