package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Nonterminal extends Term {
  private final Mark mark;
  private final String name;

  public Nonterminal(Mark mark, String name) {
    this.mark = mark;
    this.name = name;
  }

  public Mark getMark() {
    return mark;
  }

  public String getName() {
    return name;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    return mark + name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
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