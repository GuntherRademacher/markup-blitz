package de.bottlecaps.markupblitz;

import java.util.stream.Collectors;

public class Rule extends Alts {
  private Mark mark;
  private String name;

  public Rule(Mark mark, String name) {
    this.mark = mark;
    this.name = name;
  }

  @Override
  public Node[] toBnf() {
    return toBnf(new Rule(mark, name));
  }

  @Override
  public String toString() {
    String padding2 = "               ";
    String prefix = mark + name + ": ";
    int padding1Length = padding2.length() - prefix.length();
    String padding1 = padding2.substring(0, Math.max(0, padding1Length));
    if (padding1Length < 0)
      prefix += "\n" + padding2;
    return alts.stream().map(Alt::toString).collect(Collectors.joining(";\n" + padding2, padding1 + prefix, "."));
  }
}
