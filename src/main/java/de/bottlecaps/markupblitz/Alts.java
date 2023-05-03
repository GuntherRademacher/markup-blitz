package de.bottlecaps.markupblitz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Alts extends Alt.Term {
  protected List<Alt> alts;

  public Alts() {
    alts = new ArrayList<>();
  }

  public void addAlt(Alt alt) {
    alts.add(alt);
  }

  public Alt last() {
    return alts.get(alts.size() - 1);
  }

  @Override
  public Node[] toBnf() {
    return toBnf(new Alts());
  }

  protected Node[] toBnf(Alts a) {
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
  public String toString() {
    return alts.stream().map(Alt::toString).collect(Collectors.joining("; ", "(", ")"));
  }
}
