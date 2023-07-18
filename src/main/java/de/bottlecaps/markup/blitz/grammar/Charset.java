package de.bottlecaps.markup.blitz.grammar;

import java.util.List;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.character.RangeSet.Builder;
import de.bottlecaps.markup.blitz.transform.Visitor;

public class Charset extends Term {
  public static final Charset END = new Charset(true, RangeSet.builder().build());

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
    return this;
  }

  private static RangeSet toRangeSet(boolean exclusion, List<Member> members) {
    Builder builder = new Builder();
    for (Member member : members) {
      if (member instanceof StringMember) {
        StringMember m = (StringMember) member;
        String value = m.getValue();
        if (m.isHex()) {
          int codepoint = Integer.parseInt(value.substring(1), 16);
          builder.add(codepoint);
        }
        else {
          for (char chr : value.toCharArray())
            builder.add(chr);
        }
      }
      else if (member instanceof RangeMember) {
        builder.add(((RangeMember) member).getRange());
      }
      else if (member instanceof ClassMember) {
        RangeSet.of(((ClassMember) member).getValue()).forEach(builder::add);
      }
      else {
        throw new IllegalStateException();
      }
    }
    return exclusion
         ? RangeSet.ALPHABET.minus(builder.build())
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
