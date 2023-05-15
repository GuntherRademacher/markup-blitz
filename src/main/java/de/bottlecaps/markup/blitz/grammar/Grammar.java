package de.bottlecaps.markup.blitz.grammar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Grammar extends Node {
  private final Map<String, Rule> rules;
  // metadata
  private RangeSet charRanges = null;
  private Map<Range, Integer> charClasses = null;

  public Grammar() {
    this.rules = new LinkedHashMap<>();
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public void setCharRanges(RangeSet charRanges) {
    this.charRanges = charRanges;
  }

  public RangeSet getCharRanges() {
    return charRanges;
  }

  public Map<Range, Integer> getCharClasses() {
    return charClasses;
  }

  public void addRule(Rule rule) {
    rules.put(rule.getName(), rule);
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
      grammar.getRules().put(rule.getName(), rule.copy());
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
