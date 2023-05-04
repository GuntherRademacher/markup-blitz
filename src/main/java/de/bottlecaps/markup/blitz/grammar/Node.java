package de.bottlecaps.markup.blitz.grammar;

public abstract class Node implements Cloneable {
  private Grammar grammar;
  private Rule rule;
  private Node parent;

  public abstract void accept(Visitor v);

  public void setGrammar(Grammar grammar) {
    this.grammar = grammar;
  }

  public Grammar getGrammar() {
    return grammar;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }

  public Rule getRule() {
    return rule;
  }

  public void setParent(Node parent) {
    this.parent = parent;
  }

  public Node getParent() {
    return parent;
  }

  public Node[] toBnf() {
    try {
      return new Node[] {(Node) this.clone()};
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
