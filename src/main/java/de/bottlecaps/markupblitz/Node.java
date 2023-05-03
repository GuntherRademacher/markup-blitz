package de.bottlecaps.markupblitz;

public abstract class Node implements Cloneable {

  public abstract void accept(Visitor v);

  public Node[] toBnf() {
    try {
      return new Node[] {(Node) this.clone()};
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
