package de.bottlecaps.markup.blitz.transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Occurrence;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class GenerateAdditionalNames extends Visitor {
  private static final Pattern nameCharPattern = Pattern.compile("^([-_.\u00B7\u203F\u2040]|\\p{L}|\\p{Nd}|\\p{Mn})$");

  private final Grammar grammar;
  private final Set<String> names;
  private final Map<Alts, String> nameByRhs;
  private final String additionalNamePrefix;
  private final Map<RangeSet, String> smallestContext;

  public GenerateAdditionalNames(Grammar grammar) {
    this.grammar = grammar;
    this.nameByRhs = new HashMap<>();

    this.names = new HashSet<String>(grammar.getRules().keySet());
    grammar.getRules().values().forEach(rule -> nameByRhs.put(rule.getAlts(), rule.getName()));

    for (StringBuilder sb = new StringBuilder();; sb.append("_")) {
      String prefix = sb.toString();
      if (names.stream().allMatch(name -> ! name.startsWith(prefix))) {
        this.additionalNamePrefix = prefix;
        break;
      }
    }

    addAdditionalNames(Charset.END, additionalNamePrefix + "end");
    addAdditionalNames(Term.START, additionalNamePrefix + "start");

    final var charsetOrigin = new CharsetOrigin();
    charsetOrigin.visit(grammar);
    this.smallestContext = charsetOrigin.smallestContext;
  }

  @Override
  public void visit(Alts a) {
    super.visit(a);
    if (! (a.getParent() instanceof Rule) && a.getAlts().size() > 1)
      addAdditionalNames(a, getAdditionalName(a.getRule().getName(), a, "choice"));
  }

  private void addAdditionalNames(Term t, String... names) {
    grammar.getAdditionalNames().putIfAbsent(t, names);
  }

  @Override
  public void visit(Charset c) {
    if (needsProposalForName(c)) {
      String suffix = c.isDeleted()
          ? "deleted_chars"
          : "preserved_chars";
      String origin = smallestContext.get(c.getRangeSet());
      if (origin == null)
        origin =  c.getRule().getName();
      String additionalName = getAdditionalName(origin, c, suffix);
      addAdditionalNames(c, additionalName);
    }
  }

  @Override
  public void visit(Insertion i) {
    String additionalName = getAdditionalName(i.getRule().getName(), i, "insertion");
    addAdditionalNames(i, additionalName);
  }

  @Override
  public void visit(Control c) {
    super.visit(c);
    switch (c.getOccurrence()) {
    case ONE_OR_MORE:
      addAdditionalNames(c, getAdditionalName(c.getRule().getName(), c, "list"));
      break;
    case ZERO_OR_MORE:
      String name0 = getAdditionalName(c.getRule().getName(), c, "list_option");
      if (c.getSeparator() != null) {
        Control list = new Control(Occurrence.ONE_OR_MORE, c.getTerm(), c.getSeparator());
        String name1 = getAdditionalName(c.getRule().getName(), list, "list");
        addAdditionalNames(c, name0, name1);
      }
      else {
        addAdditionalNames(c, name0);
      }
      break;
    case ZERO_OR_ONE:
      addAdditionalNames(c, getAdditionalName(c.getRule().getName(), c, "option"));
      break;
    default:
      throw new IllegalStateException();
    }
  }

  public String getAdditionalName(String proposal, Term term, String suffix) {
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

  private boolean needsProposalForName(Charset c) {
    if (grammar.getAdditionalNames().containsKey(c))
      return false;
    RangeSet rangeSet = c.getRangeSet();
    if (rangeSet.isSingleton() && Range.isAscii(rangeSet.iterator().next().getFirstCodepoint()))
      return false;
    return true;
  }

  private class CharsetOrigin extends Visitor {
    Map<RangeSet, String> smallestContext = new HashMap<>();
    Map<RangeSet, Integer> smallestContextSize = new HashMap<>();

    @Override
    public void visit(Charset c) {
      if (needsProposalForName(c)) {
        if (grammar.getAdditionalNames().containsKey(c))
          return;
        final var rangeSet = c.getRangeSet();
        if (rangeSet.isSingleton() && Range.isAscii(rangeSet.iterator().next().getFirstCodepoint()))
          return;
        Node parent = c.getParent();
        Alts alts = null;
        int contextSize;
        if (parent instanceof Alt && ((Alt) parent).getTerms().size() == 1) {
          contextSize = 0;
          alts = (Alts) parent.getParent();
          for (Alt a : alts.getAlts()) {
            if (a.getTerms().size() == 1 && a.getTerms().get(0) instanceof Charset) {
              contextSize += ((Charset) a.getTerms().get(0)).getRangeSet().charCount();
            }
          }
        }
        else {
          contextSize = rangeSet.charCount();
        }
        Integer minContextSize = smallestContextSize.get(rangeSet);
        if (minContextSize == null || minContextSize > contextSize) {
          String name = grammar.getAdditionalNames().containsKey(alts)
                      ? grammar.getAdditionalNames().get(alts)[0]
                      : c.getRule().getName();
          smallestContext.put(rangeSet, name);
          smallestContextSize.put(rangeSet, contextSize);
        }
      }
    }
  }
}
