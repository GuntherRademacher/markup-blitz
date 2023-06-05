package de.bottlecaps.markup.blitz.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Charset extends Term {
  public static final Charset END = new Charset(true, false);
  private final boolean deleted;
  private final boolean exclusion;
  private final List<Member> members;
  protected String bnfRuleName;

  public Charset(boolean deleted, boolean exclusion) {
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

  public void setBnfRuleName(String bnfRuleName) {
    this.bnfRuleName = bnfRuleName;
  }

  public String getBnfRuleName() {
    return bnfRuleName;
  }

  public void addLiteral(String literal, boolean isHex) {
    members.add(new StringMember(literal, isHex));
  }

  public void addRange(String firstCodePoint, String lastCodePoint) {
    members.add(new RangeMember(firstCodePoint, lastCodePoint));
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  public void addClass(String clazz) {
    members.add(new ClassMember(clazz));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Charset copy() {
    Charset charset = new Charset(deleted, exclusion);
    for (Member member : members)
      charset.getMembers().add(member.copy());
    return charset;
  }

  @Override
  public String toString() {
    String prefix = (deleted ? "-" : "")
                  + (exclusion ? "~" : "");
    return prefix + (this.equals(END) && grammar.getAdditionalNames() != null
        ? grammar.getAdditionalNames().get(END)[0]
        : members.stream().map(Member::toString).collect(Collectors.joining("; ", "[", "]")));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (deleted ? 1231 : 1237);
    result = prime * result + (exclusion ? 1231 : 1237);
    result = prime * result + ((members == null) ? 0 : members.hashCode());
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
    if (exclusion != other.exclusion)
      return false;
    if (members == null) {
      if (other.members != null)
        return false;
    }
    else if (!members.equals(other.members))
      return false;
    return true;
  }
}
