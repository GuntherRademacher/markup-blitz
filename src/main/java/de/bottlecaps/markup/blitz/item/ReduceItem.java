package de.bottlecaps.markup.blitz.item;

import de.bottlecaps.markup.blitz.grammar.Alt;

public class ReduceItem extends Item {

  public ReduceItem(Alt alt, TokenSet lookahead) {
    super(alt, lookahead);
  }
}
