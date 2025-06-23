// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.bottlecaps.markup.Blitz.Option;
import de.bottlecaps.markup.blitz.Parser;
import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;
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
  private static final int EPSILON = -1;

  private Grammar grammar;

  private Map<String, Integer> nonterminalCode;
  private String[] nonterminal;

  private Map<RangeSet, Integer> terminalCode;
  private RangeSet[] terminal;
  private NavigableMap<Range, Integer> terminalCodeByRange;

  private Map<Node, TokenSet> first = new IdentityHashMap<>();
  private Map<State, State> states = new LinkedHashMap<>();
  private Set<State> statesTodo = new LinkedHashSet<>();

  private Map<Integer, Integer> forkId;
  private int[] forks;

  private ReduceArgument[] reduceArguments;

  private Map2D terminalTransitionData;
  private Map2D nonterminalTransitionData;

  private boolean verbose;

  private Generator() {
  }

  public static Parser generate(Grammar g) {
    return generate(g,  Collections.emptySet());
  }

  public static Parser generate(Grammar g, Set<Option> options) {
    Generator ci  = new Generator();
    ci.verbose = options.contains(Option.VERBOSE);
    ci.grammar = g;

    if (ci.verbose) {
      System.err.println();
      System.err.println("BNF grammar:");
      System.err.println("------------");
      System.err.println(g);
    }

    ci.new SymbolCodeAssigner().visit(g);

    if (options.contains(Option.VERBOSE)) {
      System.err.println();
      System.err.println("Number of charClasses: " + ci.terminalCode.size());
      System.err.println("----------------------");
      ci.terminalCode.forEach((k, v) -> System.err.println(v + ": " + k));
    }

    ci.reduceArguments = ci.reduceArguments();
    ci.collectFirst();

    Term startNode = g.getRules().values().iterator().next().getAlts().getAlts().get(0).getTerms().get(0);
    Integer endToken = ci.terminalCode.get(Charset.END.getRangeSet());
    State initialState = ci.new State();
    initialState.put(startNode, new TokenSet(endToken));
    initialState.id = 0;
    ci.states.put(initialState, initialState);
    ci.statesTodo.add(initialState);

    ci.forks = new int[32];
    Comparator<Integer> forkComparator = (lhs, rhs) ->
      Arrays.compare(ci.forks, 2 * lhs, 2 * lhs + 2, ci.forks, 2 * rhs, 2 * rhs + 2);
    ci.forkId = new TreeMap<>(forkComparator);

    while (! ci.statesTodo.isEmpty()) {
      State s = ci.statesTodo.iterator().next();
      ci.statesTodo.remove(s);
      s.close();
      s.transitions();
    }

    ci.forks = Arrays.copyOf(ci.forks, 2 * ci.forkId.size());

    ci.parserData();

    // report status

    if (ci.verbose) {
      System.err.println();
      System.err.println(ci.states.size() + " states (not counting LR(0) reduce states)");
      System.err.println(ci.reduceArguments.length + " reduce arguments");
      System.err.println(ci.forks.length / 2 + " forks");

      for (int i = 0; i < ci.forks.length / 2; ++i) {
        System.err.println("\nfork " + i + ":");
        for (int j = 0; j < 2; ++j) {
          int code = ci.forks[2 * i + j];
          Action action = Action.of(code);
          System.err.print(action);
          if (action.getType() == Action.Type.REDUCE || action.getType() == Action.Type.SHIFT_REDUCE) {
            System.err.print(" (");
            System.err.print(ci.toString(ci.reduceArguments[action.getArgument()]));
            System.err.print(")");
          }
          System.err.println();
        }
      }

      for (State state : ci.states.keySet())
        System.err.println("\nstate " + state.id + ":\n" + state);
    }

    final BitSet[] expectedTokens = new BitSet[ci.states.size()];
    for (State state : ci.states.keySet()) {
      expectedTokens[state.id] = new BitSet(ci.terminalCode.size());
      state.terminalTransitions.keySet().forEach(expectedTokens[state.id]::set);
      state.reductions.keySet().forEach(expectedTokens[state.id]::set);
    }

    final int bmpMapEnd = 0xD800;
    Function<Integer, TileIterator> tokenMapIterator =
        bits -> TileIterator.of(ci.terminalCodeByRange, bmpMapEnd, bits, 0);
    CompressedMap bmpMap = new CompressedMap(tokenMapIterator, 3);

    int[] asciiMap = ci.asciiMap(bmpMap);
    int[] smpMap = ci.supplementaryMap(bmpMapEnd);

    Function<Integer, TileIterator> terminalTransitionIterator =
        bits -> TileIterator.of(ci.terminalTransitionData, bits, 0);
    CompressedMap terminalTransitions = new CompressedMap(terminalTransitionIterator, 3);

    Function<Integer, TileIterator> nonterminalTransitionIterator =
        bits -> TileIterator.of(ci.nonterminalTransitionData, bits, 0);
    CompressedMap nonterminalTransitions = new CompressedMap(nonterminalTransitionIterator, 3);

    if (ci.verbose) {
      System.err.println();
      System.err.println("size of token code map: " + bmpMap.data().length + ", shift: " + Arrays.toString(bmpMap.shift()));
      System.err.println("size of terminal transition map: " + terminalTransitions.data().length + ", shift: " + Arrays.toString(terminalTransitions.shift()));
      System.err.println("size of nonterminal transition map: " + nonterminalTransitions.data().length + ", shift: " + Arrays.toString(nonterminalTransitions.shift()));
      System.err.println();
    }

    return new Parser(options,
        asciiMap, bmpMap, smpMap,
        terminalTransitions, ci.terminalTransitionData.getEndY(),
        nonterminalTransitions, ci.nonterminalTransitionData.getEndY(),
        ci.reduceArguments,
        ci.nonterminal,
        ci.terminal,
        ci.forks,
        expectedTokens,
        ci.grammar.isMismatch(),
        ci.grammar.getVersion().isAtLeast(Grammar.Version.V1_1));
  }

  private int[] asciiMap(CompressedMap bmpMap) {
    int[] asciiMap = new int[128];
    for (int i = 0; i < asciiMap.length; ++i)
      asciiMap[i] = bmpMap.get(i);
    return asciiMap;
  }

  private int[] supplementaryMap(final int firstValue) {
    Range value = new Range(firstValue);
    Range firstKey = terminalCodeByRange.floorKey(value);
    if (firstKey == null || firstValue > firstKey.getLastCodepoint())
      firstKey = terminalCodeByRange.higherKey(value);
    if (firstKey == null)
      return new int[] {firstValue, Character.MAX_CODE_POINT, -1};
    SortedMap<Range, Integer> tailMap = terminalCodeByRange.tailMap(firstKey);
    final var count = tailMap.size();
    int[] rangeMap = new int[3 * count];
    int i = 0;
    for (Iterator<Entry<Range, Integer>> iterator = tailMap.entrySet().iterator(); iterator.hasNext();  ++i) {
      Entry<Range, Integer> entry = iterator.next();
      Range range = entry.getKey();
      rangeMap[i] = Math.max(firstValue, range.getFirstCodepoint());
      rangeMap[count + i] = range.getLastCodepoint();
      rangeMap[count + count + i] = entry.getValue();
    }
    return rangeMap;
  }

  private String toString(ReduceArgument reduceArgument) {
    final int nonterminalId = reduceArgument.getNonterminalId();
    Mark[] marks = reduceArgument.getMarks();
    int[] insertion = reduceArgument.getInsertion();
    return "pop " + marks.length
        + ", id " + nonterminalId
        + ", nonterminal " + nonterminal[nonterminalId]
        + (marks.length == 0 ? "" : ", marks " + Arrays.stream(marks).map(Mark::toString).collect(Collectors.joining()))
        + (insertion == null ? "" : ", insert " + new Insertion(insertion));
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
      if (kernel.size() != 1)
        return false;
      Node node = kernel.keySet().iterator().next();
      if (! (node instanceof Alt || node instanceof Insertion))
        return false;
      Alt alt = node instanceof Alt
              ? (Alt) node
              : (Alt) node.getParent();
      return reduceArguments[alt.getReductionId()].getNonterminalId() != 0;
    }

    public void close() {
      closure = new IdentityHashMap<>();
      Deque<Map.Entry<Node, TokenSet>> todo = kernel.entrySet().stream()
          .filter(e -> e.getKey() instanceof Nonterminal)
          .collect(Collectors.toCollection(LinkedList::new));
      for (Map.Entry<Node, TokenSet> item; null != (item = todo.poll()); ) {
        Nonterminal nonterminal = (Nonterminal) item.getKey();
        TokenSet lookahead = item.getValue();
        Node next = nonterminal.getNext();
        if (next != null)
          lookahead = first(next, lookahead);
        for (Alt alt : nonterminal.getGrammar().getRule(nonterminal.getName()).getAlts().getAlts()) {
          Node closureItemNode = alt.getTerms().isEmpty()
                               ? alt
                               : alt.getTerms().get(0);
          TokenSet closureLookahead = closure.get(closureItemNode);
          if (closureLookahead != null) {
            if (! closureLookahead.containsAll(lookahead)) {
              // existing node, new lookahead
              closureLookahead.addAll(lookahead);
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
            for (int token = lookahead.nextToken(EPSILON); token >= EPSILON; token = lookahead.nextToken(token + 1))
              reductions.compute(token, (k, v) -> {
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
              code = terminalCode.get(((Charset) node).getRangeSet());
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
                TokenSet lookahead = state.kernel.get(k.getKey());
                if (! lookahead.containsAll(k.getValue())) {
                  lookahead.addAll(k.getValue());
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

        Integer id = -1;
        for (int i = forkList.size() - 2; i >= 0; --i) {
          int newId = forkId.size();
          if (newId << 1 + 2 > forks.length)
            forks = Arrays.copyOf(forks, forks.length << 1);
          forks[2 * newId    ] = forkList.get(i);
          forks[2 * newId + 1] = id < 0
                               ? forkList.get(i + 1)
                               : Action.code(Action.Type.FORK, id);
          id = forkId.putIfAbsent(newId, newId);
          id = id != null ? id : newId;
        }

        conflicts.put(conflictToken, id);
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
          final int code;
          if (state.isLr0ReduceState()) {
            Node node = state.kernel.keySet().iterator().next();
            Alt alt = node instanceof Alt
                    ? (Alt) node
                    : (Alt) node.getParent();
            code = Action.code(Action.Type.SHIFT_REDUCE, alt.getReductionId());
          }
          else {
            code = Action.code(Action.Type.SHIFT, state.id);
          }
          terminalTransitionData.put(new Map2D.Index(id , terminalId), code);
        }
      });
      nonterminalTransitions.forEach((nonterminalId, state) -> {
        final int code;
        if (state.isLr0ReduceState()) {
          Node node = state.kernel.keySet().iterator().next();
          Alt alt = (Alt)  (node instanceof Alt ? node : node.getParent());
          int argument = alt.getReductionId();
          code = Action.code(Action.Type.SHIFT_REDUCE, argument);
        }
        else {
          code = Action.code(Action.Type.SHIFT, state.id);
        }
        nonterminalTransitionData.put(new Map2D.Index(id , nonterminalId), code);
      });
      reductions.forEach((terminalId, alts) -> {
        if (! conflicts.containsKey(terminalId)) {
          if (alts.size() != 1)
            throw new IllegalStateException();
          final int reductionId = alts.get(0).getReductionId();
          final int code = Action.code(Action.Type.REDUCE, reductionId);
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
      Alt alt = node instanceof Alt
             ? (Alt) node
             : (Alt) (node.getParent());
      if (node instanceof Alt || node instanceof Insertion) {
        return new Action(Action.Type.REDUCE, alt.getReductionId());
      }
      State toState;
      if (node instanceof Nonterminal) {
        int code = nonterminalCode.get(((Nonterminal) node).getName());
        toState = nonterminalTransitions.get(code);
      }
      else if (node instanceof Charset) {
        int code = terminalCode.get(((Charset) node).getRangeSet());
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
      String delimiter = "";
      for (int token = lookahead.nextToken(EPSILON); token >= EPSILON; token = lookahead.nextToken(token + 1)) {
        sb.append(delimiter).append(toString(token));
        delimiter = ", ";
      }
      sb.append("}] ");
      if (nonterminalTransitions != null &&
          terminalTransitions != null
          ) {
        final Action action = action(node);
        sb.append(action);
        if (action.getType() == Action.Type.REDUCE || action.getType() == Action.Type.SHIFT_REDUCE)
          sb.append(" (")
            .append(Generator.this.toString(reduceArguments[action.getArgument()]))
            .append(")");
      }
      return sb.toString();
    }

    private String toString(Integer token) {
      if (token == 0)
        return Codepoint.toString(Codepoint.EOI);
      if (terminal == null)
        return Integer.toString(token);
      return terminal[token].shortName();
    }
  }

  private TokenSet first(Node node, TokenSet lookahead) {
    if (node == null || node instanceof Insertion)
      return lookahead;
    TokenSet tokens = first.get(node);
    if (! tokens.contains(EPSILON))
      return tokens;
    TokenSet nonNullTokens = new TokenSet();
    nonNullTokens.addAll(tokens);
    nonNullTokens.remove(EPSILON);
    nonNullTokens.addAll(lookahead);
    return nonNullTokens;
  }

  private void collectFirst() {
    for (boolean initial = true, changed = true; changed; initial = false) {
      changed = false;
      for (Rule r : grammar.getRules().values()) {
        for (Alt a : r.getAlts().getAlts()) {
          if (a.getTerms().isEmpty() || a.getTerms().get(0) instanceof Insertion) {
            if (initial) {
              changed = true;
              first.put(a, new TokenSet(EPSILON));
            }
          }
          else {
            List<Term> terms = new ArrayList<>(a.getTerms());
            for (int i = terms.size() - 1; i >= 0; --i) {
              Term t = terms.get(i);
              if (t instanceof Charset) {
                if (initial) {
                  changed = true;
                  first.put(t, new TokenSet(terminalCode.get(((Charset) t).getRangeSet())));
                }
              }
              else if (t instanceof Nonterminal) {
                Rule rule = grammar.getRule(((Nonterminal) t).getName());
                TokenSet firstOfRule = first.get(rule);
                TokenSet tokens = new TokenSet();
                if (firstOfRule != null) {
                  tokens.addAll(firstOfRule);
                  if (tokens.contains(EPSILON) && i + 1 != terms.size() && ! (terms.get(i + 1) instanceof Insertion)) {
                    tokens.remove(EPSILON);
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
                  if (! f.containsAll(tokens)) {
                    f.addAll(tokens);
                    changed = true;
                  }
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
    nonterminalTransitionData = new Map2D(states.size(), grammar.getRules().size());
    nonterminalTransitionData.put(new Map2D.Index(0, 0), Action.code(Action.Type.ACCEPT, 0));
    states.keySet().forEach(State::parserData);
  }

  private ReduceArgument[] reduceArguments() {
    Map<ReduceArgument, Integer> reductionId = new LinkedHashMap<>();
    for (Rule rule : grammar.getRules().values()) {
      int code = nonterminalCode.get(rule.getName());
      for (Alt alt : rule.getAlts().getAlts()) {
        List<Mark> marks = new ArrayList<>();
        List<Integer> aliases = new ArrayList<>();
        int[] insertion = null;
        for (Term term : alt.getTerms()) {
          if (term instanceof Insertion) {
            insertion = (((Insertion) term).getCodepoints());
          }
          else if (term instanceof Nonterminal) {
            final Nonterminal n = (Nonterminal) term;
            marks.add(n.getMark());
            aliases.add(n.getAlias() == null || n.getName().equals(n.getAlias())
                ? -1
                : nonterminalCode.get(n.getAlias()));
          }
          else if (! (term instanceof Charset)) {
            throw new IllegalStateException();
          }
          else if (((Charset) term).isDeleted()) {
            marks.add(Mark.DELETE);
            aliases.add(-1);
          }
          else {
            marks.add(Mark.NODE);
            aliases.add(-1);
          }
        }
        ReduceArgument reduction = new ReduceArgument(
            marks.toArray(Mark[]::new),
            aliases.stream().mapToInt(Integer::intValue).toArray(),
            insertion,
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
      g.getRules().keySet().forEach(name ->
        nonterminalCode.put(name, nonterminalCode.size())
      );
      terminalCode.put(Charset.END.getRangeSet(), terminalCode.size());
      super.visit(g);
      nonterminal = nonterminalCode.keySet().stream()
          .map(name -> UnicodeCategory.isXmlName(name) ? name : " " + name)
          .toArray(String[]::new);
      terminal = terminalCode.keySet().toArray(RangeSet[]::new);
    }

    @Override
    public void visit(Nonterminal n) {
      if (n.getAlias() != null && ! nonterminalCode.containsKey(n.getAlias())) {
        int code = nonterminalCode.size();
        nonterminalCode.put(n.getAlias(), code);
      }
    }

    @Override
    public void visit(Charset c) {
      RangeSet r = c.getRangeSet();
      if (! terminalCode.containsKey(r)) {
        int code = terminalCode.size();
        terminalCode.put(r, code);
        for (Range range : r)
          terminalCodeByRange.put(range, code);
      }
    }
  }

}
