package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;
import de.bottlecaps.markup.blitz.item.Item;
import de.bottlecaps.markup.blitz.item.ReduceItem;
import de.bottlecaps.markup.blitz.item.ShiftItem;
import de.bottlecaps.markup.blitz.item.TokenSet;

public class CreateItems extends Visitor {
  private Grammar grammar;
  private Map<Integer, RangeSet> rangeSet = new LinkedHashMap<>();
  private Map<RangeSet, Integer> token = new LinkedHashMap<>();
  private Map<Node, TokenSet> first = new IdentityHashMap<>();

  private CreateItems() {
  }

  public static void process(Grammar g) {
    CreateItems ci  = new CreateItems();
    ci.grammar = g;
    ci.new TokenCollector().visit(g);

    ci.collectFirst();

    ci.first.forEach((k, v) -> {
      if (k instanceof Rule) {
        System.out.println(((Rule) k).getName() + ":");
        for (Integer t : v)
          System.out.println("        " + (t == null ? "<epsilon>" : ci.rangeSet.get(t)));
      }
    });

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

    ItemSet itemSet = ci.new ItemSet();
    Term startNode = g.getRules().values().iterator().next().getAlts().getAlts().get(0).getTerms().get(0);
    Integer endToken = ci.token.get(RangeSet.of(Charset.END));
    itemSet.add(new ShiftItem(startNode, TokenSet.of(endToken)));

    ci.visit(g);
  }

  @Override
  public void visit(Alt a) {
    // TODO Auto-generated method stub
    super.visit(a);
  }

  private class ItemSet {
    private LinkedHashSet<Item> kernel = new LinkedHashSet<>();
    private LinkedHashSet<Item> closure;

    private boolean closed = false;

    public void close() {
      closure = new LinkedHashSet<>();
      for (Item item : kernel) {
        Node node = item.getNode();
        if (node instanceof Nonterminal) {
          for (Alt alt : node.getGrammar().getRules().get(((Nonterminal) node).getName()).getAlts().getAlts()) {
            if (alt.getTerms().isEmpty()) {
              new ReduceItem(alt, item.getLookahead());
            }
            else {
              TokenSet lookahead = item.getLookahead();
              Node next = node.getNext();
              if (next != null)
                lookahead = first(next, lookahead);
              new ShiftItem(alt.getTerms().get(0), lookahead);
            }
          }
        }
      }
    }

    void add(Item item) {
      kernel.add(item);
    }
  }

  private TokenSet first(Node node, TokenSet lookahead) {
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
    int pass = 0;
    for (boolean initial = true, changed = true; changed; initial = false) {
      System.out.println("pass: " + pass++);
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
                  first.put(t, TokenSet.of(token.get(RangeSet.of((Charset) t))));
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
    @Override
    public void visit(Charset c) {
      RangeSet r = RangeSet.of(c);
      if (! token.containsKey(r)) {
        int code = token.size() + 1;
        token.put(r, code);
        rangeSet.put(code, r);
      }
    }
  }
}
