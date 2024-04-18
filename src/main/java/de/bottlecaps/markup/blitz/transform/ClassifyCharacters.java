// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import de.bottlecaps.markup.blitz.Option;
import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.codepoints.RangeSet.Builder;
import de.bottlecaps.markup.blitz.grammar.Alt;
import de.bottlecaps.markup.blitz.grammar.Alts;
import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Control;
import de.bottlecaps.markup.blitz.grammar.Grammar;
import de.bottlecaps.markup.blitz.grammar.Insertion;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.Term;

public class ClassifyCharacters extends Copy {
  /** All sets that are used in the grammar. */
  private Set<RangeSet> allSets;

  private Queue<String> todo;
  private Set<String> done;
  private CharsetCollector charsetCollector;

  public ClassifyCharacters(Grammar g) {
    super(g);
  }

  public Grammar combine(Grammar g, Map<Option, Object> options) {
    List<Long> t = new ArrayList<>();
    t.add(System.currentTimeMillis());

    todo = new LinkedList<>();
    done = new HashSet<>();
    charsetCollector = new CharsetCollector();

    allSets = new HashSet<>();
    g.setAdditionalNames(new HashMap<>());

    t.add(System.currentTimeMillis());

    String firstRuleName = g.getRules().values().iterator().next().getName();
    todo.add(firstRuleName);
    done.add(firstRuleName);
    while (! todo.isEmpty()) {
      String name = todo.poll();
      visit(g.getRule(name));
    }

    t.add(System.currentTimeMillis()); // ---> 25 ms

    copy.setAdditionalNames(g.getAdditionalNames());
    PostProcess.process(copy);

    t.add(System.currentTimeMillis());

    Set<RangeSet> charClasses = classify(allSets);

    t.add(System.currentTimeMillis());

    HashMap<RangeSet, Set<RangeSet>> charsetToClasses = mapToClasses(allSets, charClasses);

    t.add(System.currentTimeMillis());

    Grammar result = ReplaceCharsets.process(copy, charsetToClasses);

    t.add(System.currentTimeMillis());

    if (Option.TIMING.is(true, options))
      for (int i = 1; i < t.size(); ++i)
        System.err.println("                                                                   time: " + (t.get(i) - t.get(i - 1)) + " msec");

    return result;
  }

  @Override
  public void visit(Charset c) {
    super.visit(c);
    allSets.add(c.getRangeSet());
  }

  @Override
  public void visit(Nonterminal n) {
    Charset charset = charsetCollector.collectCharset(n);
    if (charset != null) {
      alts.peek().last().getTerms().add(charset);
      allSets.add(charset.getRangeSet());
      n.getGrammar().getAdditionalNames().put(charset, new String[] {n.getName()});
    }
    else {
      if (! done.contains(n.getName())) {
        done.add(n.getName());
        todo.offer(n.getName());
      }
      super.visit(n);
    }
  }

  @Override
  public void visit(Alts a) {
    boolean hasDeletedChars = false;
    RangeSet deletedChars = RangeSet.EMPTY;
    boolean hasPreservedChars = false;
    RangeSet preservedChars = RangeSet.EMPTY;
    Alts other = new Alts();
    for (Alt alt : a.getAlts()) {
      Charset charset = charsetCollector.collectCharset(alt);
      if (charset == null) {
        other.addAlt(alt);
      }
      else if (charset.isDeleted()) {
        hasDeletedChars = true;
        deletedChars = deletedChars.union(charset.getRangeSet());
      }
      else {
        hasPreservedChars = true;
        preservedChars = preservedChars.union(charset.getRangeSet());
      }
    }

    if (other.equals(a)) {
      super.visit(a);
    }
    else {
      Alts replacement = new Alts();
      if (hasDeletedChars) {
        Charset charset = new Charset(true, deletedChars);
        replacement.addAlt(new Alt().addCharset(charset));
        allSets.add(deletedChars);
      }
      if (hasPreservedChars) {
        Charset charset = new Charset(false, preservedChars);
        replacement.addAlt(new Alt().addCharset(charset));
        allSets.add(preservedChars);
      }

      boolean topLevel = alts.isEmpty();
      alts.push(replacement);
      for (Alt alt : other.getAlts())
        alt.accept(this);
      // if not rule level, integrate into enclosing term
      if (! topLevel) {
        Alts nested = alts.pop();
        alts.peek().last().addAlts(nested);
      }
    }
  }

  @Override
  public void visit(Alt alt) {
    alts.peek().addAlt(new Alt());
    final var terms = alt.getTerms();
    for (int t = 0; t < terms.size(); ++t) {
      Term term = terms.get(t);
      if (term instanceof Literal) {
        List<Charset> charsets = literalToCharsets((Literal) term);
        alts.peek().last().getTerms().addAll(charsets);
      }
      else if (term instanceof Insertion && term.getNext() instanceof Insertion) {
        Insertion i = (Insertion) term;
        int[] ic = i.getCodepoints();
        while (i.getNext() instanceof Insertion) {
          ++t;
          Insertion n = (Insertion) i.getNext();
          int[] nc = n.getCodepoints();
          int[] cc = Arrays.copyOf(ic, ic.length + nc.length);
          System.arraycopy(nc, 0, cc, ic.length, nc.length);
          i = n;
          ic = cc;
        }
        Insertion combinedInsertion = new Insertion(ic);
        alts.peek().last().getTerms().add(combinedInsertion);
      }
      else {
        term.accept(this);
      }
    }
  }

  private List<Charset> literalToCharsets(Literal l) {
    List<Charset> charsets = new ArrayList<>();
    for (int codepoint : l.getCodepoints()) {
      RangeSet rangeSet = RangeSet.builder().add(codepoint).build();
      Charset charset = new Charset(l.isDeleted(), rangeSet);
      allSets.add(rangeSet);
      charsets.add(charset);
    }
    return charsets;
  }

  private Term literalToTerm(Literal l) {
    List<Charset> charsets = literalToCharsets(l);
    if (charsets.size() == 1)
      return charsets.get(0);
    Alts alts = new Alts();
    charsets.stream().forEach(c -> alts.addAlt(new Alt().addCharset(c)));
    return alts;
  }

  @Override
  public void visit(Literal l) {
    throw new IllegalStateException();
  }

  @Override
  public void visit(Control c) {
    if (c.getTerm() instanceof Literal) {
      alts.peek().last().getTerms().add(literalToTerm((Literal) c.getTerm()));
    }
    else {
      c.getTerm().accept(this);
    }
    if (c.getSeparator() instanceof Literal) {
      alts.peek().last().getTerms().add(literalToTerm((Literal) c.getSeparator()));
    }
    else if (c.getSeparator() != null) {
      c.getSeparator().accept(this);
    }
    Term separator = c.getSeparator() == null
        ? null
        : alts.peek().last().removeLast();
    Term term = alts.peek().last().removeLast();
    alts.peek().last().getTerms().add(new Control(c.getOccurrence(), term(term), term(separator)));
  }

  private Term term(Term term) {
    while (term instanceof Alts
        && ((Alts) term).getAlts().size() == 1
        && ((Alts) term).getAlts().get(0).getTerms().size() == 1)
      term = ((Alts) term).getAlts().get(0).getTerms().get(0);
    return term;
  }

  private static class CharsetCollector extends Visitor {
    private boolean isDeleted;
    private boolean isPreserved;
    private RangeSet rangeSet;
    private Set<String> active = new HashSet<>();

    public Charset collectCharset(Node node) {
      isDeleted = true;
      isPreserved = true;
      rangeSet = RangeSet.EMPTY;
      active.clear();

      node.accept(this);
      Charset charset = rangeSet == null || isPreserved == isDeleted
                      ? null
                      : new Charset(isDeleted, rangeSet);
      return charset;
    }

    @Override
    public void visit(Alt a) {
      if (a.getTerms().size() != 1)
        rangeSet = null;
      else
        super.visit(a);
    }

    @Override
    public void visit(Alts a) {
      super.visit(a);
    }

    @Override
    public void visit(Charset c) {
      if (rangeSet != null) {
        rangeSet = rangeSet.union(c.getRangeSet());
        isPreserved = isPreserved && ! c.isDeleted();
        isDeleted = isDeleted && c.isDeleted();
      }
    }

    @Override
    public void visit(Control c) {
      rangeSet = null;
    }

    @Override
    public void visit(Grammar g) {
      rangeSet = null;
    }

    @Override
    public void visit(Insertion i) {
      rangeSet = null;
    }

    @Override
    public void visit(Literal l) {
      if (rangeSet != null) {
        int[] codepoints = l.getCodepoints();
        if (codepoints.length == 1) {
          rangeSet = rangeSet.union(RangeSet.builder().add(codepoints[0]).build());
          isPreserved = isPreserved && ! l.isDeleted();
          isDeleted = isDeleted && l.isDeleted();
        }
        else {
          rangeSet = null;
        }
      }
    }

    @Override
    public void visit(Nonterminal n) {
      if (active.contains(n.getName())) {
        rangeSet = null;
        return;
      }
      active.add(n.getName());
      if (rangeSet != null) {
        if (n.getEffectiveMark() != Mark.DELETE) {
          rangeSet = null;
        }
        else {
          n.getGrammar().getRule(n.getName()).getAlts().accept(this);
        }
      }
      active.remove(n.getName());
    }

    @Override
    public void visit(Rule r) {
      rangeSet = null;
    }
  }

  private static class ReplaceCharsets extends Copy {
    private Map<RangeSet, Set<RangeSet>> charsetToCharclasses;

    private ReplaceCharsets(Grammar grammar) {
      super(grammar);
    }

    public static Grammar process(Grammar g, Map<RangeSet, Set<RangeSet>> charsetToCharclasses) {
      ReplaceCharsets rc = new ReplaceCharsets(new Grammar(g));
      rc.charsetToCharclasses = charsetToCharclasses;
      rc.visit(g);
      rc.copy.setAdditionalNames(g.getAdditionalNames());
      PostProcess.process(rc.copy);
      return rc.copy;
    }

    @Override
    public void visit(Charset c) {
      Set<RangeSet> charClass = charsetToCharclasses.get(c.getRangeSet());
      if (charClass.size() <= 1) {
        // c.getRangeSet is equal to charClass
        alts.peek().last().getTerms().add(c.copy());
      }
      else {
        Alts a = new Alts();
        for (RangeSet rangeSet : charClass) {
          Alt alt = new Alt();
          alt.getTerms().add(rangeSet.toCharset(c.isDeleted()));
          a.addAlt(alt);
        }
        alts.peek().last().getTerms().add(a);

        // preserve charset name for choice of charclasses
        String[] name = c.getGrammar().getAdditionalNames().get(c);
        if (name != null)
          c.getGrammar().getAdditionalNames().put(a, name);
      }
    }
  }

  public static Set<RangeSet> classify(Collection<RangeSet> allRangeSets) {
    if (allRangeSets.isEmpty())
      return Collections.emptySet();
    Builder builder = RangeSet.builder();
    allRangeSets.forEach(builder::add);
    RangeSet[] charClasses = new RangeSet[allRangeSets.size()];
    int classCount = 1;
    charClasses[0] = builder.build();
    for (RangeSet rangeSet : allRangeSets) {
      RangeSet divisor = rangeSet;
      int classesToCheck = classCount;
      for (int i = 0; i < classesToCheck; ++i) {
        RangeSet intersection = divisor.intersection(charClasses[i]);
        if (! intersection.isEmpty()) {
          if (! intersection.equals(charClasses[i])) {
            if (classCount == charClasses.length)
              charClasses = Arrays.copyOf(charClasses, charClasses.length << 1);
            charClasses[classCount++] = intersection;
            charClasses[i] = charClasses[i].minus(intersection);
          }
          divisor = divisor.minus(intersection);
          if (divisor.isEmpty())
            break;
        }
      }
    }
    return new TreeSet<>(Arrays.asList(charClasses).subList(0, classCount));
  }

  private static HashMap<RangeSet, Set<RangeSet>> mapToClasses(Set<RangeSet> allSets, Set<RangeSet> charClasses) {
    HashMap<RangeSet, Set<RangeSet>> charsetToClasses = new HashMap<>();
    allSets.forEach(s -> charsetToClasses.put(s, charClasses(s, charClasses)));
    return charsetToClasses;
  }

  public static Set<RangeSet> charClasses(RangeSet characters, Set<RangeSet> charClasses) {
    if (characters.isEmpty())
      return Collections.emptySet();
    Iterator<Range> iterator = characters.iterator();
    Range firstRange = iterator.next();
    int firstCodepoint= firstRange.getFirstCodepoint();
    if (firstCodepoint == firstRange.getLastCodepoint() && ! iterator.hasNext())
      return Set.of(characters);
    Set<RangeSet> result = new TreeSet<>();
    for (RangeSet charClass : charClasses) {
      if (charClass.containsCodepoint(firstCodepoint)) {
        result.add(charClass);
        characters = characters.minus(charClass);
        if (characters.isEmpty())
          return result;
        firstCodepoint= characters.iterator().next().getFirstCodepoint();
      }
    }
    throw new IllegalStateException();
  }

}
