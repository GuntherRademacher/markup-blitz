package de.bottlecaps.markup.blitz.grammar;

import java.util.HashSet;

public class PostProcess extends Visitor {
  private Grammar grammar;
  private Rule rule;
  private Node parent;

  public PostProcess(Grammar grammar) {
    this.grammar = grammar;
  }

  @Override
  public void visit(Grammar g) {
    this.parent = g;
    super.visit(g);
    for (StringBuilder sb = new StringBuilder();; sb.append("_")) {
      String prefix = sb.toString();
      if (g.getRules().keySet().stream().allMatch(name -> ! name.startsWith(prefix))) {
        g.setAdditionalNamePrefix(prefix);
        break;
      }
    }
    g.setNames(new HashSet<String>(g.getRules().keySet()));
  }

  @Override
  public void visit(Rule r) {
    this.rule = r;
    super.visit(r);
  }

  @Override
  public void visit(Nonterminal n) {

    super.visit(n);
  }

  @Override
  public void visitPreOrder(Node node) {
    node.setGrammar(grammar);
    node.setRule(rule);
    node.setParent(parent);
    parent = node;
    super.visitPreOrder(node);
  }

  @Override
  public void visitPostOrder(Node node) {
    super.visitPostOrder(node);
    parent = node.getParent();
  }
}
