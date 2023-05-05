package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

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
    return new Node[] {(Node) this.clone()};
  }

  @SuppressWarnings("unchecked")
  public <T extends Node> T copy() {
    return (T) this.clone();
  }

  @Override
  protected Object clone() {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
