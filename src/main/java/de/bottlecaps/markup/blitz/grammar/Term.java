package de.bottlecaps.markup.blitz.grammar;

public abstract class Term extends Node {
  protected String bnfRuleName;

  public void setBnfRuleName(String bnfRuleName) {
    this.bnfRuleName = bnfRuleName;
  }

  public String getBnfRuleName() {
    return bnfRuleName;
  }
}