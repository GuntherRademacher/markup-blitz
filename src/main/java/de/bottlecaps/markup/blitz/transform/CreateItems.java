package de.bottlecaps.markup.blitz.transform;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class CreateItems extends Visitor {
  private Grammar grammar;

  private CreateItems() {
  }

  public static void process(Grammar g) {

    for (Rule r : g.getRules().values()) {
      for (Alt a : r.getAlts().getAlts()) {
        for (Term t : a.getTerms()) {

        }
      }
    }




    CreateItems ci  = new CreateItems();
    ci.grammar = g;
    ci.visit(g);
  }

  @Override
  public void visit(Alt a) {
    // TODO Auto-generated method stub
    super.visit(a);
  }
}
