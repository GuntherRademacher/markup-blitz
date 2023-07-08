package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;
import de.bottlecaps.markup.blitz.item.TokenSet;
import de.bottlecaps.markup.blitz.parser.Action;
import de.bottlecaps.markup.blitz.parser.ReduceArgument;

public class Generator {
  private Grammar grammar;

  private Map<String, Integer> nonterminalCode;
  private String[] nonterminal;

  private Map<RangeSet, Integer> terminalCode;
  private RangeSet[] terminal;
  private NavigableMap<Range, Integer> terminalCodeByRange;

  private Map<Node, TokenSet> first = new IdentityHashMap<>();
  private Map<State, State> states = new LinkedHashMap<>();
  private Deque<State> statesTodo = new LinkedList<>();

  private Map<int[], Integer> forkId;
  private int[][] forks;
  private ReduceArgument[] reduceArguments;

  private Map2D terminalTransitionData;
  private Map2D nonterminalTransitionData;

  private Generator() {
  }

  public static void process(Grammar g) {
    Generator ci  = new Generator();
    ci.grammar = g;

    ci.new SymbolCodeAssigner().visit(g);
    ci.reduceArguments = ci.reduceArguments();
    ci.collectFirst();

    Term startNode = g.getRules().values().iterator().next().getAlts().getAlts().get(0).getTerms().get(0);
    Integer endToken = ci.terminalCode.get(RangeSet.of(Charset.END));
    State initialState = ci.new State();
    initialState.put(startNode, TokenSet.of(endToken));
    initialState.id = 0;
    ci.states.put(initialState, initialState);
    ci.statesTodo.offer(initialState);

    ci.forkId = new TreeMap<>(new Comparator<int[]>() {
      @Override
      public int compare(int[] o1, int[] o2) {
        return Arrays.compare(o1, o2);
      }
    });

    while (! ci.statesTodo.isEmpty()) {
      State s = ci.statesTodo.poll();
      s.close();
      s.transitions();
    }

    ci.forks = new int[ci.forkId.size()][];
    ci.forkId.forEach((k, v) -> ci.forks[v] = k);

    ci.parserData();

    // report status

    System.out.println(ci.states.size() + " states (not counting LR(0) reduce states)");
    System.out.println(ci.reduceArguments.length + " reduce arguments");
    System.out.println(ci.forks.length + " forks");

    for (int i = 0; i < ci.forks.length; ++i) {
      System.out.println("\nfork " + i + ":");
      for (int code : ci.forks[i]) {
        Action action = Action.of(code);
        System.out.print(action);
        if (action.getType() == Action.Type.REDUCE || action.getType() == Action.Type.SHIFT_REDUCE) {
          System.out.print(" (");
          System.out.print(ci.toString(ci.reduceArguments[action.getArgument()]));
          System.out.print(")");
        }
        System.out.println();
      }
    }
    for (State state : ci.states.keySet())
      System.out.println("\nstate " + state.id + ":\n" + state);

    Function<Integer, TileIterator> tokenMapIterator =
        bits -> TileIterator.of(ci.terminalCodeByRange, 0xD800, bits, 0);
    CompressedMap tokenCodeMap = new CompressedMap(tokenMapIterator, 1);
    System.out.println("size of token code map: " + tokenCodeMap.data().length + ", shift: " + Arrays.toString(tokenCodeMap.shift()));
    tokenCodeMap = new CompressedMap(tokenMapIterator, 2);
    System.out.println("size of token code map: " + tokenCodeMap.data().length + ", shift: " + Arrays.toString(tokenCodeMap.shift()));
    tokenCodeMap = new CompressedMap(tokenMapIterator, 3);
    System.out.println("size of token code map: " + tokenCodeMap.data().length + ", shift: " + Arrays.toString(tokenCodeMap.shift()));

    Function<Integer, TileIterator> terminalTransitionIterator =
        bits -> TileIterator.of(ci.terminalTransitionData, bits, 0);
    CompressedMap terminalTransitions = new CompressedMap(terminalTransitionIterator, 1);
    System.out.println("size of terminal transition map: " + terminalTransitions.data().length + ", shift: " + Arrays.toString(terminalTransitions.shift()));
    terminalTransitions = new CompressedMap(terminalTransitionIterator, 2);
    System.out.println("size of terminal transition map: " + terminalTransitions.data().length + ", shift: " + Arrays.toString(terminalTransitions.shift()));
    terminalTransitions = new CompressedMap(terminalTransitionIterator, 3);
    System.out.println("size of terminal transition map: " + terminalTransitions.data().length + ", shift: " + Arrays.toString(terminalTransitions.shift()));

    Function<Integer, TileIterator> nonterminalTransitionIterator =
        bits -> TileIterator.of(ci.nonterminalTransitionData, bits, 0);
    CompressedMap nonterminalTransitions = new CompressedMap(nonterminalTransitionIterator, 1);
    System.out.println("size of nonterminal transition map: " + nonterminalTransitions.data().length + ", shift: " + Arrays.toString(nonterminalTransitions.shift()));
    nonterminalTransitions = new CompressedMap(nonterminalTransitionIterator, 2);
    System.out.println("size of nonterminal transition map: " + nonterminalTransitions.data().length + ", shift: " + Arrays.toString(nonterminalTransitions.shift()));
    nonterminalTransitions = new CompressedMap(nonterminalTransitionIterator, 3);
    System.out.println("size of nonterminal transition map: " + nonterminalTransitions.data().length + ", shift: " + Arrays.toString(nonterminalTransitions.shift()));
  }

  private String toString(ReduceArgument reduceArgument) {
    final int nonterminalId = reduceArgument.getNonterminalId();
    Mark[] marks = reduceArgument.getMarks();
    String insertion = reduceArgument.getInsertion();
    return "pop " + marks.length
        + ", id " + nonterminalId
        + ", nonterminal " + nonterminal[nonterminalId]
        + (marks.length == 0 ? "" : ", marks " + Arrays.stream(marks).map(Mark::toString).collect(Collectors.joining()))
        + (insertion == null ? "" : ", insert " + escapeNonAscii(insertion));
  }

  public static String escapeNonAscii(String input) {
    StringBuilder sb = new StringBuilder("\"");
    for (char c : input.toCharArray()) {
      if (' ' <= c && c <= '~' && c != '"')
        sb.append(c);
      else
        sb.append("\\u").append(String.format("%04x", (int) c));
    }
    sb.append("\"");
    return sb.toString();
  }

  private class State {
    private int id;
    /** Kernel items, position and lookahead. */
    private Map<Node, TokenSet> kernel;
    /** Closure items, position and lookahead. */
    private Map<Node, TokenSet> closure;
    /** Terminal transitions, token code and target state */
    private Map<Integer, State> terminalTransitions;
    /** Nonterminal transitions, token code and target state */
    private Map<Integer, State> nonterminalTransitions;
    /** Reductions, token code and reduced alternative. */
    private Map<Integer, List<Alt>> reductions;
    /** Conflicts, token codes and fork id. */
    private Map<Integer, Integer> conflicts;

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

      // calculate follow-up states

      Stream.concat(
          kernel.entrySet().stream(),
          closure.entrySet().stream()
        )
        .forEach(e -> {
          Node node = e.getKey();
          TokenSet lookahead = e.getValue();
          if (node instanceof Alt || node instanceof Insertion) {
            Alt alt = node instanceof Alt
                    ? (Alt) node
                    : (Alt) node.getParent();
            for (int code : lookahead)
              reductions.compute(code, (k, v) -> {
                if (v == null)
                  v = new ArrayList<>();
                v.add(alt);
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

      // resolve follow-up states

      for (Map<Integer, State> transitions : Arrays.asList(
          nonterminalTransitions,
          terminalTransitions
      )) {
        for (Map.Entry<Integer, State> e : transitions.entrySet()) {
          State newState = e.getValue();
          if (! newState.isLr0ReduceState()) {
            State state = states.putIfAbsent(newState, newState);
            if (state == null) {
              newState.id = states.size() - 1;
              statesTodo.add(newState);
            }
            else {
              Integer code = e.getKey();
              transitions.put(code, state);
              for (Map.Entry<Node, TokenSet> k : newState.kernel.entrySet()) {
                if (state.kernel.get(k.getKey()).addAll(k.getValue())) {
                  if (state.closure != null)
                    state.closure = null;
                    state.nonterminalTransitions = null;
                    state.terminalTransitions = null;
                    statesTodo.add(state);
                }
              }
            }
          }
        }
      }

      conflicts = new LinkedHashMap<>();
      Set<Integer> conflictTokens = new HashSet<>(terminalTransitions.keySet());
      conflictTokens.retainAll(reductions.keySet());
      reductions.forEach((k, v) -> {
        if (v.size() > 1)
          conflictTokens.add(k);
      });
      for (int conflictToken : conflictTokens) {
        List<Integer> forkList = new ArrayList<>();
        State state = terminalTransitions.get(conflictToken);
        if (terminalTransitions.containsKey(conflictToken)) {
          if (state.isLr0ReduceState()) {
            int argument = ((Alt) state.kernel.keySet().iterator().next()).getReductionId();
            forkList.add(Action.code(Action.Type.SHIFT_REDUCE, argument));
          }
          else {
            forkList.add(Action.code(Action.Type.SHIFT, state.id));
          }
        }
        for (Alt alt : reductions.get(conflictToken))
          forkList.add(Action.code(Action.Type.REDUCE, alt.getReductionId()));
        int[] fork = forkList.stream().mapToInt(Integer::intValue).toArray();
        Integer newId = forkId.size();
        Integer id = forkId.putIfAbsent(fork, forkId.size());
        conflicts.put(conflictToken, id == null ? newId : id);
      }
    }

    void put(Node node, TokenSet lookahead) {
      kernel.put(node, lookahead);
    }

    void parserData() {
      conflicts.forEach((terminalId, forkId) -> {
        final int code = Action.code(Action.Type.FORK, forkId);
        terminalTransitionData.put(new Map2D.Index(id , terminalId), code);
      });
      terminalTransitions.forEach((terminalId, state) -> {
        if (! conflicts.containsKey(terminalId)) {
          if (state.isLr0ReduceState()) {
            int argument = ((Alt) state.kernel.keySet().iterator().next()).getReductionId();
            final int code = Action.code(Action.Type.SHIFT_REDUCE, argument);
            terminalTransitionData.put(new Map2D.Index(id , terminalId), code);
          }
          else {
            final int code = Action.code(Action.Type.SHIFT, state.id);
            terminalTransitionData.put(new Map2D.Index(id , terminalId), code);
          }
        }
      });
      nonterminalTransitions.forEach((nonterminalId, state) -> {
        final int code = Action.code(Action.Type.SHIFT, state.id);
        nonterminalTransitionData.put(new Map2D.Index(id , nonterminalId), code);
      });
      reductions.forEach((terminalId, alt) -> {
        if (! conflicts.containsKey(terminalId)) {
          if (alt.size() != 1)
            throw new IllegalStateException();
          final int code = Action.code(Action.Type.REDUCE, alt.get(0).getReductionId());
          terminalTransitionData.put(new Map2D.Index(id , terminalId), code);
        }
      });
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
      String conflictsString = conflicts.entrySet().stream()
        .map(e -> {
          int t = e.getKey();
          StringBuilder sb = new StringBuilder("\n");
          if (terminalTransitions.containsKey(t))
            sb.append("shift");
          else
            sb.append("reduce");
          sb.append("-reduce conflict on ");
          sb.append(toString(t));
          sb.append(" fork ");
          sb.append(e.getValue());
          return sb.toString();
        })
        .collect(Collectors.joining());
      return itemsString + conflictsString;
    }

    private Action action(Node node) {
      if (node instanceof Alt)
        return new Action(Action.Type.REDUCE, ((Alt) node).getReductionId());
      if (node instanceof Insertion)
        return new Action(Action.Type.REDUCE, ((Alt) node.getParent()).getReductionId());
      Alt alt = (Alt) (node.getParent());
      State toState;
      if (node instanceof Nonterminal) {
        int code = nonterminalCode.get(((Nonterminal) node).getName());
        toState = nonterminalTransitions.get(code);
      }
      else if (node instanceof Charset) {
        //TODO: get rid of transformation to RangeSet
        int code = terminalCode.get(RangeSet.of((Charset) node));
        toState = terminalTransitions.get(code);
      }
      else {
        throw new IllegalStateException("Unexpected type: " + node.getClass().getSimpleName());
      }
      if (toState.isLr0ReduceState())
        return new Action(Action.Type.SHIFT_REDUCE, alt.getReductionId());
      else
        return new Action(Action.Type.SHIFT, toState.id);
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
      sb.append("}] ");
      final Action action = action(node);
      sb.append(action);
      if (action.getType() == Action.Type.REDUCE || action.getType() == Action.Type.SHIFT_REDUCE)
        sb.append(" (")
          .append(Generator.this.toString(reduceArguments[action.getArgument()]))
          .append(")");
      return sb.toString();
    }

    private String toString(Integer token) {
      if (token == 0)
        return "$";
      if (terminal == null)
        return Integer.toString(token);
      int firstCodepoint = terminal[token].iterator().next().getFirstCodepoint();
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

  private void parserData() {
    terminalTransitionData = new Map2D(states.size(), terminal.length);
    nonterminalTransitionData = new Map2D(states.size(), nonterminal.length);
    states.keySet().forEach(State::parserData);
  }

  private ReduceArgument[] reduceArguments() {
    Map<ReduceArgument, Integer> reductionId = new LinkedHashMap<>();
    for (Rule rule : grammar.getRules().values()) {
      int code = nonterminalCode.get(rule.getName());
      for (Alt alt : rule.getAlts().getAlts()) {
        List<Mark> marks = new ArrayList<>();
        String insertion = "";
        for (Term term : alt.getTerms()) {
          if (term instanceof Insertion)
            insertion += (((Insertion) term).getValue());
          else if (term instanceof Nonterminal)
            marks.add(((Nonterminal) term).getMark());
          else if (! (term instanceof Charset))
            throw new IllegalStateException();
          else if (((Charset) term).isDeleted())
            marks.add(Mark.DELETE);
          else
            marks.add(Mark.NODE);
        }
        ReduceArgument reduction = new ReduceArgument(
            marks.toArray(Mark[]::new),
            insertion.isEmpty() ? null : insertion,
            code);
        int newId = reductionId.size();
        Integer id = reductionId.putIfAbsent(reduction, newId);
        alt.setReductionId(id == null ? newId : id);
      }
    }
    return reductionId.keySet().toArray(ReduceArgument[]::new);
  }

  private class SymbolCodeAssigner extends Visitor {
    @Override
    public void visit(Grammar g) {
      nonterminalCode = new LinkedHashMap<>();
      terminalCode = new LinkedHashMap<>();
      terminalCodeByRange = new TreeMap<>();

      nonterminalCode.put(g.getRules().keySet().iterator().next(), nonterminalCode.size());
      terminalCode.put(RangeSet.of(Charset.END), terminalCode.size());
      super.visit(g);
      nonterminal = nonterminalCode.keySet().toArray(String[]::new);
      terminal = terminalCode.keySet().toArray(RangeSet[]::new);
    }

    @Override
    public void visit(Nonterminal n) {
      if (! nonterminalCode.containsKey(n.getName())) {
        int code = nonterminalCode.size();
        nonterminalCode.put(n.getName(), code);
      }
    }

    @Override
    public void visit(Charset c) {
      // TODO: avoid transformation to RangeSet
      RangeSet r = RangeSet.of(c);
      if (! terminalCode.containsKey(r)) {
        int code = terminalCode.size();
        terminalCode.put(r, code);
        for (Range range : r)
          terminalCodeByRange.put(range, code);
      }
    }
  }

}
