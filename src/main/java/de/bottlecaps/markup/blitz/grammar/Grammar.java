package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.transform.PostProcess;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Grammar extends Node {
  private final Map<String, Rule> rules;

  public Grammar() {
    this.rules = new LinkedHashMap<>();
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public void addRule(Rule rule) {
    rules.put(rule.getName(), rule);
  }

  @Override
  public Node[] toBnf() {
    Grammar grammar = new Grammar();
    rules.values().forEach(rule ->{
      Arrays.stream(rule.toBnf())
        .map(Rule.class::cast)
        .forEach(grammar::addRule);
    });
    new PostProcess(grammar).visit(grammar);
    return new Node[] {grammar};
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
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
