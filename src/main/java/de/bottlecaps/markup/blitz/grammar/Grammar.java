package de.bottlecaps.markup.blitz.grammar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.Errors;
import de.bottlecaps.markup.blitz.transform.Visitor;

public final class Grammar extends Node {
  private final Map<String, Rule> rules;
  // metadata
  private Map<Term, String[]> additionalNames;

  public Grammar() {
    this.rules = new LinkedHashMap<>();
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public Map<Term, String[]> getAdditionalNames() {
    return additionalNames;
  }

  public void setAdditionalNames(Map<Term, String[]> additionalNames) {
    this.additionalNames = additionalNames;
  }

  public void addRule(Rule rule) {
    Rule oldRule = rules.put(rule.getName(), rule);
    if (oldRule != null) {
      System.err.println("attempt to replace\n" + oldRule);
      System.err.println("by\n" + rule);
      Errors.S03.thro(rule.getName());
    }
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Grammar copy() {
    Grammar grammar = new Grammar();
    for (Rule rule : rules.values())
      grammar.addRule(rule.copy());
    return grammar;
  }

  @Override
  public String toString() {
    return rules.values().stream().map(Rule::toString).collect(Collectors.joining("\n"));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rules == null) ? 0 : rules.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Grammar))
      return false;
    Grammar other = (Grammar) obj;
    if (rules == null) {
      if (other.rules != null)
        return false;
    }
    else if (!rules.equals(other.rules))
      return false;
    return true;
  }
}
