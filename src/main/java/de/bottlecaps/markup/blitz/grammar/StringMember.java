// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

public class StringMember extends Member {
  private final boolean isHex;
  private final String value;

  public StringMember(String value, boolean isHex) {
    this.isHex = isHex;
    this.value = value;
  }

  public boolean isHex() {
    return isHex;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return isHex ? value : "'" + value.replace("'", "''") + "'";
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
    if (!(obj instanceof StringMember))
      return false;
    StringMember other = (StringMember) obj;
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