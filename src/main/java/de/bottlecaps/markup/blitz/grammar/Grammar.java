package de.bottlecaps.markup.blitz.grammar;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Grammar extends Node {
  private static final Pattern nameCharPattern = Pattern.compile("^([-_.\u00B7\u203F\u2040]|\\p{L}|\\p{Nd}|\\p{Mn})$");

  private final Map<String, Rule> rules;
  private final Map<Alts, String> nameByRhs;
  private String additionalNamePrefix;
  private Set<String> names;

  public Grammar() {
    this.rules = new LinkedHashMap<>();
    this.nameByRhs = new HashMap<>();
  }

  public Map<String, Rule> getRules() {
    return rules;
  }

  public void addRule(Rule rule) {
    rules.put(rule.getName(), rule);
    nameByRhs.put(rule.getAlts(), rule.getName());
  }

  public String getAdditionalName(Term term) {
    Alts alts;
    if (term instanceof Alts) {
      alts = (Alts) term;
    }
    else {
      Alt alt = new Alt();
      alt.getTerms().add(term);
      alts = new Alts();
      alts.addAlt(alt);
      term = alts;
    }
    String name = nameByRhs.get(alts);
    if (name == null) {
      name = getAdditionalName(term.toString());
      nameByRhs.put(alts, name);
    }
    return name;
  }

  public void setNames(Set<String> names) {
    this.names = names;
  }

  public void setAdditionalNamePrefix(String additionalNamePrefix) {
    this.additionalNamePrefix = additionalNamePrefix;
  }

  public String getAdditionalNamePrefix() {
    return additionalNamePrefix;
  }

  public String getAdditionalName(String proposal) {
    StringBuilder sb = new StringBuilder();
    char last = '_';
    for (Character chr : proposal.toCharArray()) {
      if (chr == '-' || ! nameCharPattern.matcher(String.valueOf(chr)).matches())
        chr = '_';
      if (chr == '_' && last == '_') continue;
      last = chr;
      sb.append(last);
    }
    while (sb.charAt(sb.length() - 1) == '_')
      sb.setLength(sb.length() - 1);
    for (int i = 0;; ++i) {
      String name = additionalNamePrefix + sb.toString()
                  + (i== 0 && sb.length() > 0 ? "" : "_" + i);
      if (! names.contains(name)) {
        names.add(name);
        return name;
      }
    }
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
