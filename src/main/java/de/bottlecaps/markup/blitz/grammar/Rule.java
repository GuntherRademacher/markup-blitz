package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Rule extends Node {
  private final Mark mark;
  private final String name;
  private final Alts alts;
  private Grammar grammar;

  public Rule(Mark mark, String name, Alts alts) {
    this.mark = mark;
    this.name = name;
    this.alts = alts;
  }

  public Mark getMark() {
    return mark;
  }

  public String getName() {
    return name;
  }

  public Alts getAlts() {
    return alts;
  }

  @Override
  public void setGrammar(Grammar grammar) {
    this.grammar = grammar;
  }

  @Override
  public Grammar getGrammar() {
    return grammar;
  }

  @Override
  public Node[] toBnf() {
    Node[] bnf = alts.toBnf();
    return Stream.concat(Stream.of(new Rule(mark, name, (Alts) bnf[0])), Arrays.stream(bnf).skip(1)).toArray(Node[]::new);
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    String padding2 = "               ";
    String prefix = mark + name + ": ";
    int padding1Length = padding2.length() - prefix.length();
    String padding1 = padding2.substring(0, Math.max(0, padding1Length));
    if (padding1Length < 0) {
      prefix = prefix.stripTrailing() + "\n" + padding2;
    }
    return alts.getAlts().stream().map(Alt::toString).collect(Collectors.joining(";\n" + padding2, padding1 + prefix, "."));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alts == null) ? 0 : alts.hashCode());
    result = prime * result + ((mark == null) ? 0 : mark.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Rule))
      return false;
    Rule other = (Rule) obj;
    if (alts == null) {
      if (other.alts != null)
        return false;
    }
    else if (!alts.equals(other.alts))
      return false;
    if (mark != other.mark)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    }
    else if (!name.equals(other.name))
      return false;
    return true;
  }
}
