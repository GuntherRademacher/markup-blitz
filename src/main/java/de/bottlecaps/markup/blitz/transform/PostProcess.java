package de.bottlecaps.markup.blitz.transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.CharSet;
import de.bottlecaps.markup.blitz.grammar.ClassMember;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Member;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Occurrence;
import de.bottlecaps.markup.blitz.grammar.RangeMember;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.StringMember;
import de.bottlecaps.markup.blitz.grammar.Term;

public class PostProcess extends Visitor {
  private static final Pattern nameCharPattern = Pattern.compile("^([-_.\u00B7\u203F\u2040]|\\p{L}|\\p{Nd}|\\p{Mn})$");

  private Set<String> names;
  private final Map<Alts, String> nameByRhs;
  private String additionalNamePrefix;
  private Grammar grammar;
  private Rule rule;
  private Node parent;
  private RangeSet.Builder charRanges;

  public PostProcess(Grammar grammar) {
    this.grammar = grammar;
    this.nameByRhs = new HashMap<>();
    this.charRanges = new RangeSet.Builder();
  }

  @Override
  public void visit(Grammar g) {
    this.parent = g;
    names = new HashSet<String>(g.getRules().keySet());
    g.getRules().values().forEach(rule -> nameByRhs.put(rule.getAlts(), rule.getName()));

    for (StringBuilder sb = new StringBuilder();; sb.append("_")) {
      String prefix = sb.toString();
      if (names.stream().allMatch(name -> ! name.startsWith(prefix))) {
        additionalNamePrefix = prefix;
        break;
      }
    }
    super.visit(g);
    grammar.setCharRanges(charRanges.build().split());

// TODO: remove
    System.out.println("-------------------------");
    System.out.println(grammar.getCharRanges());
  }

  @Override
  public void visit(Rule r) {
    this.rule = r;
    super.visit(r);
  }

  @Override
  public void visit(Alts a) {
    super.visit(a);
    if (! (a.getParent() instanceof Rule) && a.getAlts().size() > 1)
      a.setBnfRuleName(getAdditionalName(a, "choice"));
  }

  @Override
  public void visit(CharSet c) {
    super.visit(c);
    if (! (    c.getParent() instanceof Alt
            && ((Alt) c.getParent()).getTerms().size() == 1
          )
        && (   c.isExclusion()
            || c.getMembers().size() > 1
            || c.getMembers().stream().anyMatch(m -> m instanceof StringMember && ((StringMember) m).getValue().length() > 1)
            || c.getMembers().stream().anyMatch(m -> m instanceof ClassMember)
           )
       )
      c.setBnfRuleName(getAdditionalName(c, "choice"));
    RangeSet.Builder builder = new RangeSet.Builder();
    for (Member member : c.getMembers()) {
      if (member instanceof StringMember) {
        StringMember m = (StringMember) member;
        String value = m.getValue();
        if (m.isHex()) {
          int codePoint = Integer.parseInt(value.substring(1), 16);
          builder.add(codePoint);
        }
        else {
          for (char chr : value.toCharArray())
            builder.add(chr);
        }
      }
      else if (member instanceof RangeMember) {
        builder.add(((RangeMember) member).getRange());
      }
      else if (member instanceof ClassMember) {
        // TODO: remove
        System.out.println("unicode char class: " + ((ClassMember) member).getValue());
        // TODO: throw new UnsupportedOperationException();
      }
      else {
        throw new IllegalStateException();
      }
    }
    builder.build().join().forEach(charRanges::add);
  }

  @Override
  public void visit(Control c) {
    super.visit(c);
    switch (c.getOccurrence()) {
    case ONE_OR_MORE:
      c.setBnfRuleName(getAdditionalName(c, "list"));
      break;
    case ZERO_OR_MORE:
      if (c.getSeparator() != null) {
        Control list = new Control(Occurrence.ONE_OR_MORE, c.getTerm(), c.getSeparator());
        c.setListBnfRuleName(getAdditionalName(list, "list"));
      }
      c.setBnfRuleName(getAdditionalName(c, "list_option"));
      break;
    case ZERO_OR_ONE:
      c.setBnfRuleName(getAdditionalName(c, "option"));
      break;
    default:
      throw new IllegalStateException();
    }
  }

  @Override
  public void visit(Nonterminal n) {
    super.visit(n);
  }

  @Override
  public void visit(Literal l) {
    super.visit(l);
    String value = l.getValue();
    if (l.isHex()) {
      int codePoint = Integer.parseInt(value.substring(1), 16);
      charRanges.add(codePoint);
    }
    else {
      for (char chr : l.getValue().toCharArray())
        charRanges.add(chr);
    }
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

  public String getAdditionalName(Term term, String suffix) {
    Alts alts;
    if (term instanceof Alts) {
      alts = (Alts) term;
    }
    else {
      Alt alt = new Alt();
      alt.getTerms().add(term);
      alts = new Alts();
      alts.addAlt(alt);
      term = alts;
    }
    String name = nameByRhs.get(alts);
    if (name == null) {
      name = getAdditionalName(term.toString(), suffix);
      nameByRhs.put(alts, name);
    }
    return name;
  }

  public String getAdditionalName(String proposal, String suffix) {
    StringBuilder sb = new StringBuilder();
    char last = '_';
    for (Character chr : proposal.toCharArray()) {
      if (chr == '-' || ! nameCharPattern.matcher(String.valueOf(chr)).matches())
        chr = '_';
      if (chr == '_' && last == '_') continue;
      last = chr;
      sb.append(last);
    }
    if (sb.length() != 0 && sb.charAt(sb.length() - 1) != '_') {
      sb.append("_");
    }
    sb.append(suffix);
    for (int i = 0;; ++i) {
      String name = additionalNamePrefix + sb.toString()
                  + (i== 0 && sb.length() > 0 ? "" : "_" + i);
      if (! names.contains(name)) {
        names.add(name);
        return name;
      }
    }
  }
}
