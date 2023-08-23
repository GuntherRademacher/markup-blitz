package de.bottlecaps.markup.blitz.grammar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.Errors;
import de.bottlecaps.markup.blitz.transform.Visitor;

public final class Grammar extends Node {
  private final String version;
  private final Map<String, Rule> rules;
  // metadata
  private Map<Term, String[]> additionalNames;

  public Grammar(String version) {
    this.version = version;
    this.rules = new LinkedHashMap<>();
  }

  public String getVersion() {
    return version;
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public Rule getRule(String name) {
    Rule r = rules.get(name);
    if (r == null)
      Errors.S02.thro(name);
    return r;
  }

  public Map<Term, String[]> getAdditionalNames() {
    return additionalNames;
  }

  public void setAdditionalNames(Map<Term, String[]> additionalNames) {
    this.additionalNames = additionalNames;
  }

  public void addRule(Rule rule) {
    Rule oldRule = rules.put(rule.getName(), rule);
    if (oldRule != null)
      Errors.S03.thro(rule.getName());
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Grammar copy() {
    Grammar grammar = new Grammar(version);
    for (Rule rule : rules.values())
      grammar.addRule(rule.copy());
    return grammar;
  }

  @Override
  public String toString() {
    return (version == null ? "" : "ixml version '" + version.replace("'", "''") + "'\n")
         + rules.values().stream().map(Rule::toString).collect(Collectors.joining("\n"));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rules == null) ? 0 : rules.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
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
    if (version == null) {
      if (other.version != null)
        return false;
    }
    else if (!version.equals(other.version))
      return false;
    return true;
  }

}
