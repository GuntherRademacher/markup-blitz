package de.bottlecaps.markup.blitz.transform;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;

public class OrderDepthFirst extends Visitor {
  private Map<String, Rule> rules = new LinkedHashMap<>();
  private Deque<String> rulesToBeDone = new LinkedList<>();
  private Stack<String> depthFirst = new Stack<>();

  @Override
  public void visit(Grammar g) {
    visitPreOrder(g);
    rulesToBeDone.offer(g.getRules().keySet().iterator().next());
    while (! rulesToBeDone.isEmpty()) {
      String nonterminal = rulesToBeDone.poll();
      if (! rules.containsKey(nonterminal))
        visit(g.getRules().get(nonterminal));
    }
    g.getRules().clear();
    rules.values().forEach(rule -> g.addRule(rule));
    visitPostOrder(g);
  }

  @Override
  public void visit(Rule r) {
    rules.put(r.getName(), r);
    super.visit(r);
    while (! depthFirst.isEmpty())
      rulesToBeDone.addFirst(depthFirst.pop());
  }

  @Override
  public void visit(Nonterminal n) {
    depthFirst.push(n.getName());
    super.visit(n);
  }
}
