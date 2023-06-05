package de.bottlecaps.markup.blitz.item;

import de.bottlecaps.markup.blitz.grammar.Node;

public abstract class Item {
  protected Node node;
  protected TokenSet lookahead;

  public Item(Node node, TokenSet lookahead) {
    this.node = node;
    this.lookahead = lookahead;
  }

  public Node getNode() {
    return node;
  }

  public TokenSet getLookahead() {
    return lookahead;
  }
}
