// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.codepoints.Range;

public class RangeMember extends Member {
  private final Range range;

  public RangeMember(Range range) {
    this.range = range;
  }

  public RangeMember(String firstValue, String lastValue) {
    this(new Range(codepoint(firstValue), codepoint(lastValue)));
  }

  public Range getRange() {
    return range;
  }

  private static int codepoint(String value) {
    return isHex(value) ? Codepoint.of(value.substring(1)) : value.codePointAt(0);
  }

  private static boolean isHex(String value) {
    return value.startsWith("#") && value.length() > 1;
  }

  @Override
  public String toString() {
    return range.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((range == null) ? 0 : range.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof RangeMember))
      return false;
    RangeMember other = (RangeMember) obj;
    if (range == null) {
      if (other.range != null)
        return false;
    }
    else if (!range.equals(other.range))
      return false;
    return true;
  }
}