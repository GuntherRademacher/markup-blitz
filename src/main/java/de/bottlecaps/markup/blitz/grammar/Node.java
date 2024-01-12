// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public abstract class Node implements Cloneable {
  protected Grammar grammar;
  protected Rule rule;
  protected Node parent;
  protected Node next;

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

  public Node getNext() {
    return next;
  }

  public void setNext(Node next) {
    this.next = next;
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
