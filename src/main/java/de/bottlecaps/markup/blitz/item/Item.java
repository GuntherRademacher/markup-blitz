//package de.bottlecaps.markup.blitz.item;
//
//import java.util.Map;
//import java.util.stream.Collectors;
//
//import de.bottlecaps.markup.blitz.character.Range;
//import de.bottlecaps.markup.blitz.character.RangeSet;
//import de.bottlecaps.markup.blitz.grammar.Alt;
//import de.bottlecaps.markup.blitz.grammar.Node;
//import de.bottlecaps.markup.blitz.grammar.Term;
//
//public abstract class Item {
//  protected Node node;
//  protected TokenSet lookahead;
//
//  public Item(Node node, TokenSet lookahead) {
//    this.node = node;
//    this.lookahead = lookahead;
//  }
//
//  public Node getNode() {
//    return node;
//  }
//
//  public TokenSet getLookahead() {
//    return lookahead;
//  }
//
////  public abstract Set<Item> closure(BiFunction<Node, TokenSet, TokenSet> first);
//
//  @Override
//  public String toString() {
//    return toString(null);
//  }
//
//  public String toString(Map<Integer, RangeSet> rangeSet) {
//    StringBuilder sb = new StringBuilder();
//    sb.append("[").append(node.getRule().getMark()).append(node.getRule().getName()).append(":");
//    Alt alt = (Alt) (node instanceof Alt
//        ? node
//        : node.getParent());
//    for (Term term : alt.getTerms()) {
//      if (term == node)
//        sb.append(" ").append(".");
//      sb.append(" ").append(term);
//    }
//    if (alt == node)
//      sb.append(" ").append(".");
//    sb.append(" | {");
//    sb.append(lookahead.stream()
//      .map(token -> {
//        if (token == 0)
//          return "$";
//        if (rangeSet == null)
//          return Integer.toString(token);
//        int firstCodepoint = rangeSet.get(token).iterator().next().getFirstCodePoint();
//        return new Range(firstCodepoint).toString();
//      })
//      .collect(Collectors.joining(", ")));
//    sb.append("}]");
//    return sb.toString();
//  }
//}
