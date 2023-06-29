package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;
import de.bottlecaps.markup.blitz.item.TokenSet;

public class CreateItems extends Visitor {
  private Grammar grammar;
  private Map<Integer, RangeSet> rangeSetByTerminalCode = new LinkedHashMap<>();
  private Map<Integer, String> nonterminal = new LinkedHashMap<>();
  private Map<String, Integer> nonterminalCode = new LinkedHashMap<>();
  private Map<RangeSet, Integer> terminalCode = new LinkedHashMap<>();
  private TreeMap<Range, Integer> terminalCodeByRange = new TreeMap<>();
  private Map<Node, TokenSet> first = new IdentityHashMap<>();
  private Map<State, State> states = new LinkedHashMap<>();
  private Deque<State> statesTodo = new LinkedList<>();

  private CreateItems() {
  }

  public static void process(Grammar g) {
    CreateItems ci  = new CreateItems();
    ci.grammar = g;
    ci.new TokenCollector().visit(g);
    ci.collectFirst();

    Term startNode = g.getRules().values().iterator().next().getAlts().getAlts().get(0).getTerms().get(0);
    Integer endToken = ci.terminalCode.get(RangeSet.of(Charset.END));
    State state = ci.new State();
    state.put(startNode, TokenSet.of(endToken));
    ci.states.put(state, state);
    ci.statesTodo.offer(state);

    while (! ci.statesTodo.isEmpty()) {
      State s = ci.statesTodo.poll();
      s.close();
      s.transitions();
    }

    System.out.println(ci.states.size() + " states (not counting LR(0) reduce states");

    for (State s : ci.states.keySet()) {
      System.out.println("\nstate:\n" + s);
    }

    Function<Integer, TileIterator> it = bits -> TileIterator.of(ci.terminalCodeByRange, 0xD7FF, bits, 0);
    int size = new CompressedMap(it, 3).tiles().length;
    System.out.println("size of token code map: " + size);
  }

  private class State {
    private Map<Node, TokenSet> kernel;
    private Map<Node, TokenSet> closure;
    private Map<Integer, State> terminalTransitions;
    private Map<Integer, State> nonterminalTransitions;
    private Map<Integer, List<Node>> reductions;
    private Set<Integer> conflicts;

    public State() {
      kernel = new IdentityHashMap<>();
    }

    public boolean isLr0ReduceState() {
      return kernel.size() == 1
          && (  kernel.keySet().iterator().next() instanceof Alt
             || kernel.keySet().iterator().next() instanceof Insertion);
    }

    public void close() {
      if (closure != null)
        return;
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

    public void transitions() {
      terminalTransitions = new HashMap<>();
      nonterminalTransitions = new HashMap<>();
      reductions = new HashMap<>();
      Stream.concat(
          kernel.entrySet().stream(),
          closure.entrySet().stream()
        )
        .forEach(e -> {
          Node node = e.getKey();
          TokenSet lookahead = e.getValue();
          if (node instanceof Alt || node instanceof Insertion) {
            for (int code : lookahead)
              reductions.compute(code, (k, v) -> {
                if (v == null)
                  v = new ArrayList<>();
                v.add(node);
                return v;
              });
          }
          else {
            Node next = node.getNext() != null
                ? node.getNext()
                : node.getParent();
            Map<Integer, State> transitions;
            Integer code;
            if (node instanceof Nonterminal) {
              code = nonterminalCode.get(((Nonterminal) node).getName());
              transitions = nonterminalTransitions;
            }
            else if (node instanceof Charset) {
              code = terminalCode.get(RangeSet.of((Charset) node));
              transitions = terminalTransitions;
            }
            else {
              throw new IllegalStateException("Unexpected type: " + node.getClass().getSimpleName());
            }
            transitions.compute(code, (k, v) -> {
              if (v == null) {
                v = new State();
                v.put(next, new TokenSet(lookahead));
              }
              else {
                TokenSet tokenSet = v.kernel.get(next);
                if (tokenSet == null)
                  v.put(next, new TokenSet(lookahead));
                else
                  tokenSet.addAll(lookahead);
              }
              return v;
            });
          }
        });

      conflicts = new HashSet<>(terminalTransitions.keySet());
      conflicts.retainAll(reductions.keySet());
      reductions.forEach((k, v) -> {
        if (v.size() > 1)
          conflicts.add(k);
      });

      for (Map<Integer, State> transitions : Arrays.asList(
          nonterminalTransitions,
          terminalTransitions
      )) {
        for (Map.Entry<Integer, State> e : transitions.entrySet()) {
          State newState = e.getValue();
          if (! newState.isLr0ReduceState()) {
            State oldState = states.putIfAbsent(newState, newState);
            if (oldState == null) {
              statesTodo.add(newState);
            }
            else {
              Integer code = e.getKey();
              transitions.put(code, oldState);
              for (Map.Entry<Node, TokenSet> k : newState.kernel.entrySet()) {
                if (oldState.kernel.get(k.getKey()).addAll(k.getValue())) {
                  if (oldState.closure != null)
                    oldState.closure = null;
                    oldState.nonterminalTransitions = null;
                    oldState.terminalTransitions = null;
                    statesTodo.add(oldState);
                }
              }
            }
          }
        }
      }
    }

    void put(Node node, TokenSet lookahead) {
      kernel.put(node, lookahead);
    }

    @Override
    public int hashCode() {
      return kernel.keySet().hashCode();
    }

    @Override
    public boolean equals(Object other) {
      return kernel.keySet().equals(((State) other).kernel.keySet());
    }

    @Override
    public String toString() {
      String itemsString = Stream.concat(
          kernel.entrySet().stream(),
          closure == null
            ? Stream.empty()
            : closure.entrySet().stream()
        )
        .map(item -> toString(item))
        .collect(Collectors.joining("\n"));
      String conflictsString = conflicts.stream()
        .map(t -> {
          StringBuilder sb = new StringBuilder();
          if (terminalTransitions.containsKey(t))
            sb.append("\nshift-reduce conflict on " + toString(t));
          if (reductions.get(t).size() > 1)
            sb.append("\nreduce-reduce conflict on " + toString(t));
          return sb.toString();
        })
        .collect(Collectors.joining());
      return itemsString + conflictsString;
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
          return toString(token);
        })
        .collect(Collectors.joining(", ")));
      sb.append("}]");
      return sb.toString();
    }

    private String toString(Integer token) {
      if (token == 0)
        return "$";
      if (rangeSetByTerminalCode == null)
        return Integer.toString(token);
      int firstCodepoint = rangeSetByTerminalCode.get(token).iterator().next().getFirstCodepoint();
      return new Range(firstCodepoint).toString();
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
                  first.put(t, TokenSet.of(terminalCode.get(RangeSet.of((Charset) t))));
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
      nonterminalCode.clear();
      terminalCode.clear();
      terminalCode.put(RangeSet.of(Charset.END), terminalCode.size());
    }

    @Override
    public void visit(Nonterminal n) {
      if (! nonterminalCode.containsKey(n.getName())) {
        int code = nonterminalCode.size();
        nonterminalCode.put(n.getName(), code);
        nonterminal.put(code, n.getName());
      }
    }

    @Override
    public void visit(Charset c) {
      RangeSet r = RangeSet.of(c);
      if (! terminalCode.containsKey(r)) {
        int code = terminalCode.size();
        rangeSetByTerminalCode.put(code, r);
        terminalCode.put(r, code);
        for (Range range : r)
          terminalCodeByRange.put(range, code);
      }
    }
  }

}
