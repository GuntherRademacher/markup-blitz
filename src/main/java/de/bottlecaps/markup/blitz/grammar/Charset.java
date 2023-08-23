package de.bottlecaps.markup.blitz.grammar;

import java.util.List;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Charset extends Term {
  public static final Charset END = new Charset(true, RangeSet.EOF);

  private final boolean deleted;
  private final RangeSet rangeSet;

  private final boolean exclusion;
  private final List<Member> members;

  private Charset(boolean deleted, RangeSet rangeSet, boolean exclusion, List<Member> members) {
    this.deleted = deleted;
    this.exclusion = exclusion;
    this.members = members;
    this.rangeSet = rangeSet != null
                  ? rangeSet
                  : toRangeSet(exclusion, members);
  }

  public Charset(boolean deleted, boolean exclusion, List<Member> members) {
    this(deleted, null, exclusion, members);
  }

  public Charset(boolean deleted, RangeSet rangeSet) {
    this(deleted, rangeSet, false, null);
  }

  public boolean isDeleted() {
    return deleted;
  }

  public RangeSet getRangeSet() {
    return rangeSet;
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Charset copy() {
    return new Charset(deleted, rangeSet, exclusion, members);
  }

  private static RangeSet toRangeSet(boolean exclusion, List<Member> members) {
    RangeSet.Builder builder = RangeSet.builder();
    for (Member member : members) {
      if (member instanceof StringMember) {
        StringMember m = (StringMember) member;
        String value = m.getValue();
        if (m.isHex()) {
          int codepoint = Codepoint.of(value.substring(1));
          builder.add(codepoint);
        }
        else {
          value.codePoints().forEach(builder::add);
        }
      }
      else if (member instanceof RangeMember) {
        builder.add(((RangeMember) member).getRange());
      }
      else if (member instanceof ClassMember) {
        UnicodeCategory.forCode(((ClassMember) member).getValue()).forEach(builder::add);
      }
      else {
        throw new IllegalStateException();
      }
    }
    return exclusion
         ? UnicodeCategory.ALPHABET.minus(builder.build())
         : builder.build();
  }

  @Override
  public String toString() {
    String prefix = (deleted ? "-" : "")
                  + (exclusion ? "~" : "");
    return prefix + (this.equals(END) && grammar != null && grammar.getAdditionalNames() != null
        ? grammar.getAdditionalNames().get(END)[0]
        : members != null
          ? members.stream().map(Member::toString).collect(Collectors.joining("; ", "[", "]"))
          : rangeSet.toString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (deleted ? 1231 : 1237);
    result = prime * result + ((rangeSet == null) ? 0 : rangeSet.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Charset))
      return false;
    Charset other = (Charset) obj;
    if (deleted != other.deleted)
      return false;
    if (rangeSet == null) {
      if (other.rangeSet != null)
        return false;
    }
    else if (!rangeSet.equals(other.rangeSet))
      return false;
    return true;
  }

}
