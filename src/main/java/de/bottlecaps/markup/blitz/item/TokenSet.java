// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.item;

import java.util.BitSet;

public class TokenSet {
  private BitSet set;

  public TokenSet() {
    set = new BitSet();
  }

  public TokenSet(int token) {
    this();
    set.set(token + 1);
  }

  public TokenSet(TokenSet other) {
    set = (BitSet) other.set.clone();
  }

  public boolean contains(int token) {
    return set.get(token + 1);
  }

  public boolean containsAll(TokenSet other) {
    BitSet intersection = (BitSet) set.clone();
    intersection.and(other.set);
    return intersection.equals(other.set);
  }

  public void add(int token) {
    set.set(token + 1);
  }

  public void addAll(TokenSet other) {
    set.or(other.set);
  }

  public void remove(int token) {
    set.clear(token + 1);
  }

  public int nextToken(int token) {
    return set.nextSetBit(token + 1) - 1;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    String delimiter = "";
    for (int token = set.nextSetBit(0); token >= 0; token = set.nextSetBit(token + 1)) {
      sb.append(delimiter).append(token == 0 ? "\u03b5" : token);
      delimiter = ", ";
    }
    return sb.append("}").toString();
  }
}