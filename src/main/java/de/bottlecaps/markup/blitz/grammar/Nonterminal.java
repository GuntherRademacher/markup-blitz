// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Nonterminal extends Term {
  private final Mark mark;
  private final String alias;
  private final String name;

  public Nonterminal(Mark mark, String alias, String name) {
    this.mark = mark;
    this.alias = alias;
    this.name = name;
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

  public Mark getEffectiveMark() {
    if (mark != Mark.NONE)
      return mark;
    Rule definition = grammar.getRule(name);
    return definition.getMark() == Mark.NONE
        ? Mark.NODE
        : definition.getMark();
  }

  public String getEffectiveAlias() {
    return alias != null
         ? alias
         : grammar.getRule(name).getAlias();
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    return mark + name + (alias != null ? ">" + alias : "");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alias == null) ? 0 : alias.hashCode());
    result = prime * result + ((mark == null) ? 0 : mark.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Nonterminal))
      return false;
    Nonterminal other = (Nonterminal) obj;
    if (alias == null) {
      if (other.alias != null)
        return false;
    }
    else if (!alias.equals(other.alias))
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