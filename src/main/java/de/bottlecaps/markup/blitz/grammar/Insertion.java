package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Insertion extends Term {
  protected final String value;
  protected final boolean isHex;

  public Insertion(String value, boolean isHex) {
    this.value = value;
    this.isHex = isHex;
  }

  public String getValue() {
    return value;
  }

  public boolean isHex() {
    return isHex;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    return "+"
         + (isHex ? value : "'" + value.replace("'", "''") + "'");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isHex ? 1231 : 1237);
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Insertion))
      return false;
    Insertion other = (Insertion) obj;
    if (isHex != other.isHex)
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    }
    else if (!value.equals(other.value))
      return false;
    return true;
  }
}