package de.bottlecaps.markup.blitz.transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Occurrence;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class GenerateAdditionalNames extends Visitor {
  private static final Pattern nameCharPattern = Pattern.compile("^([-_.\u00B7\u203F\u2040]|\\p{L}|\\p{Nd}|\\p{Mn})$");

  private Set<String> names;
  private final Map<Alts, String> nameByRhs;
  private String additionalNamePrefix;
  private Map<Term, String[]> additionalNames;

  public GenerateAdditionalNames(Grammar grammar) {
    this.nameByRhs = new HashMap<>();
    this.additionalNames = new IdentityHashMap<>();
  }

  public Map<Term, String[]> getAdditionalNames() {
    return additionalNames;
  }

  @Override
  public void visit(Grammar g) {
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
  }

  @Override
  public void visit(Alts a) {
    super.visit(a);
    if (! (a.getParent() instanceof Rule) && a.getAlts().size() > 1)
      addAdditionalNames(a, getAdditionalName(a, "choice"));
  }

  private void addAdditionalNames(Term t, String... names) {
    additionalNames.put(t, names);
  }

  @Override
  public void visit(Charset c) {
    if (c.isDeleted()) {
      Charset nonDeletedSet = new Charset(false, false);
      nonDeletedSet.getMembers().addAll(c.getMembers());
      nonDeletedSet.setRule(c.getRule());
      c = nonDeletedSet;
    }
    addAdditionalNames(c, getAdditionalName(c, "token"));
  }

  @Override
  public void visit(Control c) {
    super.visit(c);
    switch (c.getOccurrence()) {
    case ONE_OR_MORE:
      addAdditionalNames(c, getAdditionalName(c, "list"));
      break;
    case ZERO_OR_MORE:
      String name0 = getAdditionalName(c, "list_option");
      if (c.getSeparator() != null) {
        Control list = new Control(Occurrence.ONE_OR_MORE, c.getTerm(), c.getSeparator());
        list.setRule(c.getRule());
        String name1 = getAdditionalName(list, "list");
        addAdditionalNames(c, name0, name1);
      }
      else {
        addAdditionalNames(c, name0);
      }
      break;
    case ZERO_OR_ONE:
      addAdditionalNames(c, getAdditionalName(c, "option"));
      break;
    default:
      throw new IllegalStateException();
    }
  }

  public String getAdditionalName(Term term, String suffix) {
    String proposal = term.getRule().getName();
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
//      name = getAdditionalName(term.toString(), suffix);
      name = getAdditionalName(proposal, suffix);
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
      if (sb.length() >= 48)
        break;
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
