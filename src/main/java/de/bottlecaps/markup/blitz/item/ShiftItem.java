//package de.bottlecaps.markup.blitz.item;
//
//import de.bottlecaps.markup.blitz.grammar.Alt;
//import de.bottlecaps.markup.blitz.grammar.Node;
//import de.bottlecaps.markup.blitz.grammar.Term;
//
//public class ShiftItem extends Item {
//  public ShiftItem(Term node, TokenSet lookahead) {
//    super(node, lookahead);
//  }
//
//  public Item shift() {
//    Node next = node.getNext();
//    if (next == null)
//      return new ReduceItem((Alt) node.getParent(), lookahead);
//    else
//      return new ShiftItem((Term) next, lookahead);
//  }
//
////  // TODO: a list would be sufficient, as long as this is just the first level
////  // TODO: "closure" is not an adequate name, if we just get the first level
////  @Override
////  public Set<Item> closure(BiFunction<Node, TokenSet, TokenSet> first) {
////    if (! (node instanceof Nonterminal))
////      return Collections.emptySet();
////    Nonterminal nonterminal = (Nonterminal) node;
////    Set<Item> closure = new HashSet<>();
////    for (Alt alt : nonterminal.getGrammar().getRules().get(nonterminal.getName()).getAlts().getAlts()) {
////      if (alt.getTerms().isEmpty()) {
////        closure.add(new ReduceItem(alt, lookahead));
////      }
////      else {
////        Term firstNode = alt.getTerms().get(0);
////        closure.add(new ShiftItem(firstNode, first.apply(node.getNext(), lookahead)));
////      }
////    }
////    return closure;
////  }
//}
