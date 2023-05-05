package de.bottlecaps.markup.blitz.grammar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.bottlecaps.markup.blitz.transform.Visitor;

public class Alts extends Term {
  protected final List<Alt> alts;

  public Alts() {
    alts = new ArrayList<>();
  }

  public List<Alt> getAlts() {
    return alts;
  }

  public void addAlt(Alt alt) {
    alts.add(alt);
  }

  public Alt last() {
    return alts.get(alts.size() - 1);
  }

  @Override
  public Node[] toBnf() {
    Alts a = new Alts();
    List<Node> rules = new ArrayList<>();
    alts.forEach(alt -> {
      Node[] bnf = alt.toBnf();
      a.alts.add((Alt) bnf[0]);
      Arrays.stream(bnf)
        .skip(1)
        .forEach(r -> rules.add(r));
    });
    return Stream.concat(Stream.of(a), rules.stream()).toArray(Node[]::new);
  }

  @Override
  public void accept(Visitor v) {
    v.visit(this);
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
