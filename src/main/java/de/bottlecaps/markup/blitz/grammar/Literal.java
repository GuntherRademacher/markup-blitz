package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Literal extends Term {
  protected final boolean deleted;
  protected final String value;
  protected final Boolean isHex;

  public boolean isDeleted() {
    return deleted;
  }

  public String getValue() {
    return value;
  }

  public Boolean getIsHex() {
    return isHex;
  }

  public Literal(boolean deleted, String value, boolean isHex) {
    this.deleted = deleted;
    this.value = value;
    this.isHex = isHex;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @Override
  public String toString() {
    return
        (deleted ? "-" : "") +
        (isHex ? value : "'" + value.replace("'", "''") + "'");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (deleted ? 1231 : 1237);
    result = prime * result + ((isHex == null) ? 0 : isHex.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Literal))
      return false;
    Literal other = (Literal) obj;
    if (deleted != other.deleted)
      return false;
    if (isHex == null) {
      if (other.isHex != null)
        return false;
    }
    else if (!isHex.equals(other.isHex))
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