package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Insertion extends Literal {

  public Insertion(String value, boolean isHex) {
    super(false, value, isHex);
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    return "+" + super.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Insertion))
      return false;
    return super.equals(obj);
  }
}