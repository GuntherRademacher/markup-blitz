package de.bottlecaps.markupblitz;

public class Nonterminal extends Term {
  private final Mark mark;
  private final String name;

  public Nonterminal(Mark mark, String name) {
    this.mark = mark;
    this.name = name;
  }

  public Mark getMark() {
    return mark;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return mark + name;
  }

  @Override
  public void accept(Visitor v) {
  }
}