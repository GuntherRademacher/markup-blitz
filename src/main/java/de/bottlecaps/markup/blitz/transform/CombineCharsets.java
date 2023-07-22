package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.character.RangeSet.Builder;
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

public class CombineCharsets extends Copy {
  /** All (combined) sets that are used in the grammar. */
  private Map<Term, RangeSet> allRangeSets;
  /** Builder for the set of all ranges from all sets. */
  private RangeSet.Builder builder;
  /** All ranges that are used in the grammar . */
  private RangeSet allRanges;
  /** The set of sets using each range. The distinct values of this provide the char classes. */
  private Map<Range, Set<RangeSet>> rangeToUsingSets = new TreeMap<>();
  /** The char classes by their using sets. */
  private Map<Set<RangeSet>, RangeSet> usingSetsToCharclasses;
  /** The originating rules for each charset in the grammar. */
  private Map<RangeSet, Set<String>> usingSetToOrigins;
  /** The characters mentioned in each rule. */
  private Map<String, RangeSet> originToChars;

  private Queue<String> todo;

  private Set<String> done;

  public CombineCharsets() {
  }

  public Grammar combine(Grammar g) {
    todo = new LinkedList<>();
    done = new HashSet<>();
    usingSetToOrigins = new HashMap<>();
    originToChars = new HashMap<>();
    allRangeSets = new HashMap<>();
    builder = new RangeSet.Builder();
    g.setAdditionalNames(new HashMap<>());

    Rule firstRule = g.getRules().values().iterator().next();
    done.add(firstRule.getName());
    visit(firstRule);

    while (! todo.isEmpty()) {
      String name = todo.poll();
      done.add(name);
      visit(g.getRules().get(name));
    }
    copy.setAdditionalNames(g.getAdditionalNames());
    PostProcess.process(copy);

    Map<Charset, Set<RangeSet>> charsetToCharclasses = new HashMap<>();
    collectRanges(charsetToCharclasses);

//    System.out.println();
//    System.out.println("number of charClasses: " + usingSetsToCharclasses.size());
//    System.out.println("----------------------");
//    RangeSet[] charClass = usingSetsToCharclasses.values().stream().sorted().toArray(RangeSet[]::new);
//    for (int i = 0; i < charClass.length; ++i)
//      System.out.println(i + ": " + charClass[i]);

    return ReplaceCharsets.process(copy, charsetToCharclasses);
  }

  private void collectRanges(Map<Charset, Set<RangeSet>> charsetToCharclasses) {
    allRanges = builder.build().split();
    rangeToUsingSets = new TreeMap<>();
    for (RangeSet rangeSet : allRangeSets.values()) {
      for (Range range : allRanges.split(rangeSet)) {
        rangeToUsingSets.compute(range, (k, v) -> {
          if (v == null) v = new HashSet<>();
          v.add(rangeSet);
          return v;
        });
      }
    }

    usingSetsToCharclasses = new HashMap<>();
    rangeToUsingSets.forEach((range, usingSets) -> {
      usingSetsToCharclasses.compute(usingSets, (charclass, ranges) -> {
        Builder builder = new RangeSet.Builder();
        if (ranges != null)
          ranges.forEach(builder::add);
        builder.add(range);
        return builder.build();
      });
    });

    allRangeSets.forEach((term, set) -> {
      Set<RangeSet> charclasses = new TreeSet<>();
      for (Range range : allRanges.split(set))
        charclasses.add(usingSetsToCharclasses.get(rangeToUsingSets.get(range)));
      if (term instanceof Charset)
        charsetToCharclasses.put((Charset) term, charclasses);
    });
  }

  public String smallestUsingNonterminal(Range range) {
    Set<RangeSet> rangeSet = rangeToUsingSets.get(range);
    int smallestEnclosingSetSize = Integer.MAX_VALUE;
    Set<String> originsOfSmallestEnclosingSet = new HashSet<>();
    for (RangeSet set : rangeSet) {
      int setSize = set.stream().mapToInt(Range::size).sum();
      if (setSize <= smallestEnclosingSetSize) {
        if (setSize < smallestEnclosingSetSize) {
          smallestEnclosingSetSize = setSize;
          originsOfSmallestEnclosingSet.clear();
        }
        originsOfSmallestEnclosingSet.addAll(usingSetToOrigins.get(set));
      }
    }
    int minSetSize = Integer.MAX_VALUE;
    String originWithLeastChars = null;
    for (String name : originsOfSmallestEnclosingSet) {
      int size = originToChars.get(name).stream().mapToInt(Range::size).sum();
      if (size < minSetSize)
        originWithLeastChars = name;
    }
    return originWithLeastChars;
  }

  @Override
  public void visit(Charset c) {
    super.visit(c);
    collect(c.getRangeSet().join(), c, c.getRule().getName());
  }

  @Override
  public void visit(Nonterminal n) {
    Charset charset = CharsetCollector.collect(n);
    if (charset != null) {
      alts.peek().last().getTerms().add(charset);
      collect(charset.getRangeSet(), charset, n.getName());
      n.getGrammar().getAdditionalNames().put(charset, new String[] {n.getName()});
    }
    else {
      if (! done.contains(n.getName()))
        todo.offer(n.getName());
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
      Charset charset = CharsetCollector.collect(alt);
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

//    System.out.println(a.getRule());
//    System.out.println("    preserved: " + preservedChars);
//    System.out.println("      deleted: -" + deletedChars);
//    System.out.println("        other: " + other);

    if (other.equals(a)) {
      super.visit(a);
    }
    else {
      Alts replacement = new Alts();
      if (hasDeletedChars) {
        Charset charset = new Charset(true, deletedChars);
        replacement.addAlt(new Alt().addCharset(charset));
        collect(deletedChars.join(), charset, a.getRule().getName());
      }
      if (hasPreservedChars) {
        Charset charset = new Charset(false, preservedChars);
        replacement.addAlt(new Alt().addCharset(charset));
        collect(preservedChars.join(), charset, a.getRule().getName());
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
    for (Term term : alt.getTerms()) {
      if (term instanceof Literal) {
        List<Charset> charsets = literalToCharsets((Literal) term);
        alts.peek().last().getTerms().addAll(charsets);
      }
      else {
        term.accept(this);
      }
    }
  }

  private List<Charset> literalToCharsets(Literal l) {
    List<Charset> charsets = new ArrayList<>();
    if (l.isHex()) {
      int c = Integer.parseInt(l.getValue().substring(1), 16);
      RangeSet rangeSet = RangeSet.builder().add(c).build();
      Charset charset = new Charset(l.isDeleted(), rangeSet);
      collect(rangeSet, charset, l.getRule().getName());
      charsets.add(charset);
    }
    else {
      l.getValue().codePoints().forEach(codepoint -> {
        RangeSet rangeSet = RangeSet.builder().add(codepoint).build();
        Charset charset = new Charset(l.isDeleted(), rangeSet);
        collect(rangeSet, charset, l.getRule().getName());
        charsets.add(charset);
      });
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
    alts.peek().last().getTerms().add(new Control(c.getOccurrence(), term, separator));
  }

  private void collect(RangeSet set, Term origin, String name) {
    usingSetToOrigins.compute(set, (k, v) -> {
      if (v == null) v = new HashSet<>();
      v.add(name);
      return v;
    });
    originToChars.compute(name, (k, v) -> {
      if (v == null)
        return set;
      RangeSet.Builder builder = new RangeSet.Builder();
      set.forEach(builder::add);
      v.forEach(builder::add);
      return builder.build();
    });
    allRangeSets.put(origin, set);
    set.forEach(builder::add);
  }

  private static class CharsetCollector extends Visitor {
    private boolean isDeleted;
    private boolean isPreserved;
    private RangeSet rangeSet;

    private CharsetCollector() {
    }

    public static Charset collect(Node node) {
      CharsetCollector cc = new CharsetCollector();
      cc.rangeSet = RangeSet.EMPTY;
      cc.isDeleted = true;
      cc.isPreserved = true;
      node.accept(cc);
      if (cc.rangeSet == null || cc.isPreserved == cc.isDeleted)
        return null;
      return new Charset(cc.isDeleted, cc.rangeSet);
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
        if (l.isHex()) {
          rangeSet = rangeSet.union(RangeSet.builder().add(new Range(Integer.parseInt(l.getValue().substring(1), 16))).build());
          isPreserved = isPreserved && ! l.isDeleted();
          isDeleted = isDeleted && l.isDeleted();
        }
        else {
          int[] codepoints = l.getValue().codePoints().toArray();
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
    }

    @Override
    public void visit(Nonterminal n) {
      if (rangeSet != null) {
        if (n.getEffectiveMark() != Mark.DELETE) {
          rangeSet = null;
        }
        else {
          n.getGrammar().getRules().get(n.getName()).getAlts().accept(this);
        }
      }
    }

    @Override
    public void visit(Rule r) {
      rangeSet = null;
    }
  }

  private static class ReplaceCharsets extends Copy {
    private Map<Charset, Set<RangeSet>> charsetToCharclasses;

    private ReplaceCharsets() {
    }

    public static Grammar process(Grammar g, Map<Charset, Set<RangeSet>> charsetToCharclasses) {
      ReplaceCharsets rc = new ReplaceCharsets();
      rc.charsetToCharclasses = charsetToCharclasses;
      rc.visit(g);
      rc.copy.setAdditionalNames(g.getAdditionalNames());
      PostProcess.process(rc.copy);
      return rc.copy;
    }

    @Override
    public void visit(Charset c) {
      Set<RangeSet> rangeSets = charsetToCharclasses.get(c);
      if (rangeSets.size() == 1) {
        RangeSet firstSet = rangeSets.iterator().next();
        alts.peek().last().getTerms().add(firstSet.toCharset(c.isDeleted()));
      }
      else {
        Alts a = new Alts();
        for (RangeSet rangeSet : rangeSets) {
          Alt alt = new Alt();
          alt.getTerms().add(rangeSet.toCharset(c.isDeleted()));
          a.addAlt(alt);
        }
        alts.peek().last().getTerms().add(a);
        String[] name = c.getGrammar().getAdditionalNames().get(c);
        if (name != null)
          c.getGrammar().getAdditionalNames().put(a, name);
      }
    }
  }
}
