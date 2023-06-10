package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;
import de.bottlecaps.markup.blitz.item.TokenSet;

public class CreateItems extends Visitor {
  private Grammar grammar;
  private Map<Integer, RangeSet> rangeSet = new LinkedHashMap<>();
  private Map<String, Integer> nonterminal = new LinkedHashMap<>();
  private Map<RangeSet, Integer> terminal = new LinkedHashMap<>();
  private Map<Node, TokenSet> first = new IdentityHashMap<>();

  private CreateItems() {
  }

  public static void process(Grammar g) {
    CreateItems ci  = new CreateItems();
    ci.grammar = g;
    ci.new TokenCollector().visit(g);
    ci.collectFirst();

//    Item firstItem = new ShiftItem(
//        g.getRules().values().iterator().next().getAlts().getAlts().get(0).getTerms().get(0),
//        TokenSet.of(ci.token.get(RangeSet.of(Charset.END))));
//
//    System.out.println("firstItem: " + firstItem.toString(ci.rangeSet));
//    for (Item nextItem = firstItem; ! (nextItem instanceof ReduceItem); ) {
//      nextItem = nextItem instanceof ShiftItem
//          ? ((ShiftItem) nextItem).shift()
//          : null;
//      System.out.println("nextItem: " + nextItem.toString(ci.rangeSet));
//    }
//
//    for (Item item1 : firstItem.closure(ci::first)) {
//      System.out.println("closureItem: " + item1.toString(ci.rangeSet));
//      for (Item item2 : item1.closure(ci::first)) {
//        System.out.println("closureItem: " + item2.toString(ci.rangeSet));
//      }
//    }

//    ci.first.forEach((k, v) -> {
//      if (k instanceof Rule) {
//        System.out.println(((Rule) k).getName() + ":");
//        for (Integer t : v)
//          System.out.println("        " + (t == null ? "<epsilon>" : ci.rangeSet.get(t)));
//      }
//    });

//    for (Rule r : g.getRules().values()) {
//      for (Alt a : r.getAlts().getAlts()) {
//        for (Term t : a.getTerms()) {
//          if (t instanceof Nonterminal) {
//            new ShiftItem(t, new TokenSet(0));
//          }
//          else if (t instanceof Charset) {
//            new ShiftItem(t, null);
//          }
//          else {
//            throw new IllegalStateException();
//          }
//        }
//      }
//    }

    State state = ci.new State();
    Term startNode = g.getRules().values().iterator().next().getAlts().getAlts().get(0).getTerms().get(0);
    Integer endToken = ci.terminal.get(RangeSet.of(Charset.END));
    state.put(startNode, TokenSet.of(endToken));

    state.close();

    System.out.println("state: \n" + state);
  }

  private class State {
    private Map<Node, TokenSet> kernel;
    private Map<Node, TokenSet> closure;
    private Map<Integer, State> terminalTransitions;
    private Map<String, State> nonterminalTransitions;

    public State() {
      kernel = new IdentityHashMap<>();
    }

    public void close() {
      closure = new IdentityHashMap<>();
      Deque<Map.Entry<Node, TokenSet>> todo = kernel.entrySet().stream()
          .filter(e -> e.getKey() instanceof Nonterminal)
          .collect(Collectors.toCollection(LinkedList::new));
      for (Map.Entry<Node, TokenSet> item; null != (item = todo.poll()); ) {
        Node node = item.getKey();
        if (node instanceof Nonterminal) {
          TokenSet lookahead = item.getValue();
          Node next = node.getNext();
          if (next != null)
            lookahead = first(next, lookahead);
          for (Alt alt : node.getGrammar().getRules().get(((Nonterminal) node).getName()).getAlts().getAlts()) {
            Node closureItemNode;
            if (alt.getTerms().isEmpty()) {
              closureItemNode = alt;
            }
            else {
              closureItemNode = alt.getTerms().get(0);
            }
            TokenSet closureLookahead = closure.get(closureItemNode);
            if (closureLookahead != null) {
              if (closureLookahead.addAll(lookahead)) {
                // existing node, new lookahead
                if (closureItemNode instanceof Nonterminal)
                  todo.add(Map.entry(closureItemNode, lookahead));
              }
            }
            else if (closureItemNode == alt) {
              closure.put(alt, new TokenSet(lookahead));
            }
            else {
              closure.put(closureItemNode, new TokenSet(lookahead));
              // new node
              if (closureItemNode instanceof Nonterminal)
                todo.add(Map.entry(closureItemNode, new TokenSet(lookahead)));
            }
          }
        }
      }
    }

    void put(Node node, TokenSet lookahead) {
      kernel.put(node, lookahead);
    }

    @Override
    public String toString() {
      return Stream.concat(kernel.entrySet().stream(), closure.entrySet().stream())
        .map(item -> toString(item))
        .collect(Collectors.joining("\n"));
    }

    private String toString(Map.Entry<Node, TokenSet> item) {
      StringBuilder sb = new StringBuilder();
      Node node = item.getKey();
      TokenSet lookahead = item.getValue();
      sb.append("[").append(node.getRule().getMark()).append(node.getRule().getName()).append(":");
      Alt alt = (Alt) (node instanceof Alt
          ? node
          : node.getParent());
      for (Term term : alt.getTerms()) {
        if (term == node)
          sb.append(" ").append(".");
        sb.append(" ").append(term);
      }
      if (alt == node)
        sb.append(" ").append(".");
      sb.append(" | {");
      sb.append(lookahead.stream()
        .map(token -> {
          if (token == 0)
            return "$";
          if (rangeSet == null)
            return Integer.toString(token);
          int firstCodepoint = rangeSet.get(token).iterator().next().getFirstCodePoint();
          return new Range(firstCodepoint).toString();
        })
        .collect(Collectors.joining(", ")));
      sb.append("}]");
      return sb.toString();
    }
  }

  private TokenSet first(Node node, TokenSet lookahead) {
    if (node == null)
      return lookahead;
    TokenSet tokens = first.get(node);
    if (! tokens.contains(null))
      return tokens;
    TokenSet nonNullTokens = new TokenSet();
    nonNullTokens.addAll(tokens);
    nonNullTokens.remove(null);
    nonNullTokens.addAll(lookahead);
    return nonNullTokens;
  }

  private void collectFirst() {
    for (boolean initial = true, changed = true; changed; initial = false) {
      changed = false;
      for (Rule r : grammar.getRules().values()) {
        for (Alt a : r.getAlts().getAlts()) {
          if (a.getTerms().isEmpty()) {
            if (initial) {
              changed = true;
              first.put(a, TokenSet.of((Integer) null));
            }
          }
          else {
            List<Term> terms = new ArrayList<>(a.getTerms());
            for (int i = terms.size() - 1; i >= 0; --i) {
              Term t = terms.get(i);
              if (t instanceof Charset) {
                if (initial) {
                  changed = true;
                  first.put(t, TokenSet.of(terminal.get(RangeSet.of((Charset) t))));
                }
              }
              else if (t instanceof Nonterminal) {
                Rule rule = grammar.getRules().get(((Nonterminal) t).getName());
                TokenSet tokenSet = first.get(rule);
                boolean hasNull = false;
                TokenSet tokens = new TokenSet();
                if (tokenSet != null) {
                  for (Integer token : tokenSet) {
                    if (token == null)
                      hasNull = true;
                    else
                      tokens.add(token);
                  }
                }
                if (hasNull) {
                  if (i + 1 == terms.size()) {
                    tokens.add(null);
                  }
                  else {
                    TokenSet f = first.get(terms.get(i + 1));
                    tokens.addAll(f);
                  }
                }
                if (initial) {
                  changed = true;
                  first.put(t, tokens);
                }
                else {
                  TokenSet f = first.get(t);
                  changed = f.addAll(tokens) || changed;
                }
              }
            }
            // propagate from first Term to Alt
            first.put(a, first.get(a.getTerms().iterator().next()));
          }
        }
        // propagate from Alt to Rule
        TokenSet tokens = new TokenSet();
        for (Alt a : r.getAlts().getAlts())
          tokens.addAll(first.get(a));
        first.put(r, tokens);
      }
    }
  }

  private class TokenCollector extends Visitor {
    public TokenCollector() {
      terminal.clear();
      terminal.put(RangeSet.of(Charset.END), terminal.size());
    }

    @Override
    public void visit(Charset c) {
      RangeSet r = RangeSet.of(c);
      if (! terminal.containsKey(r)) {
        int code = terminal.size();
        terminal.put(r, code);
        rangeSet.put(code, r);
      }
    }
  }
}
