package de.bottlecaps.markup.blitz.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Alts extends Term {
  protected final List<Alt> alts;
  protected String bnfRuleName;

  public Alts() {
    alts = new ArrayList<>();
  }

  public List<Alt> getAlts() {
    return alts;
  }

  public void setBnfRuleName(String bnfRuleName) {
    this.bnfRuleName = bnfRuleName;
  }

  public String getBnfRuleName() {
    return bnfRuleName;
  }

  public void addAlt(Alt alt) {
    alts.add(alt);
  }

  public Alt last() {
    return alts.get(alts.size() - 1);
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Alts copy() {
    Alts alts = new Alts();
    for (Alt alt : this.alts)
      alts.getAlts().add(alt.copy());
    return alts;
  }

  @Override
  public String toString() {
    return alts.stream().map(Alt::toString).collect(Collectors.joining("; ", "(", ")"));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alts == null) ? 0 : alts.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof Alts))
      return false;
    Alts other = (Alts) obj;
    if (alts == null) {
      if (other.alts != null)
        return false;
    }
    else if (!alts.equals(other.alts))
      return false;
    return true;
  }
}
