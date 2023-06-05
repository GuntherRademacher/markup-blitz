package de.bottlecaps.markup.blitz.item;

import java.util.HashSet;

public class TokenSet extends HashSet<Integer> {
  private static final long serialVersionUID = 1L;

  public TokenSet() {
  }

  public static TokenSet of(Integer... tokens) {
    TokenSet tokenSet = new TokenSet();
    for (Integer token : tokens)
      tokenSet.add(token);
    return tokenSet;
  }
}
