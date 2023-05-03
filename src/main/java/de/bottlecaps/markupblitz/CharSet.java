package de.bottlecaps.markupblitz;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CharSet extends Term {
  private final boolean deleted;
  private final boolean exclusion;
  private final List<Member> members;

  public CharSet(boolean deleted, boolean exclusion) {
    this.deleted = deleted;
    this.exclusion = exclusion;
    members = new ArrayList<>();
  }

  public boolean isDeleted() {
    return deleted;
  }

  public boolean isExclusion() {
    return exclusion;
  }

  public List<Member> getMembers() {
    return members;
  }

  public void addLiteral(String literal, boolean isHex) {
    members.add(new StringMember(literal, isHex));
  }

  public void addRange(String firstCodePoint, String lastCodePoint) {
    members.add(new RangeMember(firstCodePoint, lastCodePoint));
  }

  @Override
  public void accept(Visitor v) {
    for (Member member : members)
      member.accept(v);
  }

  public void addClass(String clazz) {
    members.add(new ClassMember(clazz));
  }

  @Override
  public String toString() {
    String prefix = (deleted ? "-" : "")
                  + (exclusion ? "~" : "")
                  + "[";
    return members.stream().map(Member::toString).collect(Collectors.joining("; ", prefix, "]"));
  }
}
