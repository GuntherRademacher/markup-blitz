// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Rule extends Node {
  private final Mark mark;
  private final String alias;
  private final String name;
  private final Alts alts;

  public Rule(Mark mark, String alias, String name, Alts alts) {
    this.mark = mark;
    this.alias = alias;
    this.name = name;
    this.alts = alts;
  }

  public Mark getMark() {
    return mark;
  }

  public String getAlias() {
    return alias;
  }

  public String getName() {
    return name;
  }

  public Alts getAlts() {
    return alts;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Rule copy() {
    return new Rule(mark, alias, name, alts.copy());
  }

  @Override
  public String toString() {
    String padding2 = "               ";
    String prefix = mark + name + (alias != null ? ">" + alias : "") + ": ";
    int padding1Length = padding2.length() - prefix.length();
    String padding1 = padding2.substring(0, Math.max(0, padding1Length));
    if (padding1Length < 0) {
      prefix = prefix.stripTrailing() + "\n" + padding2;
    }
    return alts.getAlts().stream().map(Alt::toString).collect(Collectors.joining(";\n" + padding2, padding1 + prefix, "."));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alias == null) ? 0 : alias.hashCode());
    result = prime * result + ((alts == null) ? 0 : alts.hashCode());
    result = prime * result + ((mark == null) ? 0 : mark.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Rule))
      return false;
    Rule other = (Rule) obj;
    if (alias == null) {
      if (other.alias != null)
        return false;
    }
    else if (!alias.equals(other.alias))
      return false;
    if (alts == null) {
      if (other.alts != null)
        return false;
    }
    else if (!alts.equals(other.alts))
      return false;
    if (mark != other.mark)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    }
    else if (!name.equals(other.name))
      return false;
    return true;
  }
}
