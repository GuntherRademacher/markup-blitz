package de.bottlecaps.markup.blitz.grammar;

public abstract class Term extends Node {
  public static final Term START = new Nonterminal(Mark.DELETE, null, null);
}