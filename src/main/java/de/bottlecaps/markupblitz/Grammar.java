package de.bottlecaps.markupblitz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Grammar extends Node {
  private List<Rule> rules;

  public Grammar() {
    this.rules = new ArrayList<>();
  }

  @Override
  public String toString() {
    return rules.stream().map(Rule::toString).collect(Collectors.joining("\n"));
  }

  public void addRule(Rule rule) {
//    rule.setGrammar(this);
    rules.add(rule);
  }

  @Override
  public Node[] toBnf() {
    Grammar grammar = new Grammar();
    rules.forEach(rule ->{
      Arrays.stream(rule.toBnf())
        .map(Rule.class::cast)
        .forEach(grammar::addRule);
    });
    return new Node[] {grammar};
  }
}
