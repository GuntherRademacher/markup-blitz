// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.grammar;

public abstract class Term extends Node {
  public static final Term START = new Nonterminal(Mark.DELETE, null, null);
}