package de.bottlecaps.markupblitz;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CharSet extends Alt.Term {
  private boolean deleted;
  private boolean exclusion;
  private List<Member> members;

  public CharSet(boolean deleted) {
    this.deleted = deleted;
    this.exclusion = false;
    members = new ArrayList<>();
  }

  public void setExclusion() {
    exclusion = true;
  }

  public void addLiteral(String literal, boolean isHex) {
    members.add(new StringMember(literal, isHex));
  }

  public void addRange(String firstCodePoint, String lastCodePoint) {
    members.add(new RangeMember(firstCodePoint, lastCodePoint));
  }

  public void addClass(String clazz) {
    members.add(new ClassMember(clazz));
  }

  private static abstract class Member {
  }

  private static class StringMember extends Member {
    private boolean isHex;
    private String value;

    public StringMember(String value, boolean isHex) {
      this.isHex = isHex;
      this.value = value;
    }

    @Override
    public String toString() {
      return isHex ? value : "'" + value.replace("'", "''") + "'";
    }
  }

  @Override
  public String toString() {
    String prefix = (deleted ? "-" : "")
                  + (exclusion ? "~" : "")
                  + "[";
    return members.stream().map(Member::toString).collect(Collectors.joining("; ", prefix, "]"));
  }

  private static class RangeMember extends Member {
    String firstValue;
    String lastValue;
    int firstCodePoint;
    int lastCodePoint;

    RangeMember(String firstValue, String lastValue) {
      this.firstValue = firstValue;
      this.lastValue = lastValue;
      firstCodePoint = codePoint(firstValue);
      lastCodePoint = codePoint(lastValue);
    }

    private int codePoint(String firstValue) {
      return isHex(firstValue) ? Integer.parseInt(firstValue.substring(1), 16) : firstValue.codePointAt(0);
    }

    @Override
    public String toString() {
      return toString(firstValue) + "-" + toString(lastValue);
    }

    private String toString(String value) {
      if (isHex(value))
        return value;
      else
        return "'" + value.replace("'", "''") + "'";
    }

    private boolean isHex(String value) {
      return value.startsWith("#") && value.length() > 1;
    }
  }

  private static class ClassMember extends Member {
    String value;

    public ClassMember(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
