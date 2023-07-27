package de.bottlecaps.markup.blitz.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import de.bottlecaps.markup.BlitzOption;
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

public class ClassifyCharacters extends Copy {
  /** All sets that are used in the grammar. */
  private Set<RangeSet> allSets;

  /** Builder for the set of all ranges from all sets. */
  private RangeSet.Builder builder;

  private Queue<String> todo;

  private Set<String> done;

  public Grammar combine(Grammar g, Set<BlitzOption> options) {
    List<Long> t = new ArrayList<>();
    t.add(System.currentTimeMillis());

    todo = new LinkedList<>();
    done = new HashSet<>();
    allSets = new HashSet<>();
    builder = RangeSet.builder();
    g.setAdditionalNames(new HashMap<>());

    t.add(System.currentTimeMillis());

    Rule firstRule = g.getRules().values().iterator().next();
    for (todo.add(firstRule.getName()); ! todo.isEmpty(); ) {
      String name = todo.poll();
      done.add(name);
      visit(g.getRules().get(name));
    }

    t.add(System.currentTimeMillis()); // ---> 80 ms

    copy.setAdditionalNames(g.getAdditionalNames());
    PostProcess.process(copy);

    t.add(System.currentTimeMillis());

    Set<RangeSet> charClasses = classify(allSets);

    t.add(System.currentTimeMillis());

    HashMap<RangeSet, Set<RangeSet>> charsetToClasses = mapToClasses(allSets, charClasses);

    t.add(System.currentTimeMillis());

//    System.out.println();
//    System.out.println("number of charClasses: " + usingSetsToCharclasses.size());
//    System.out.println("----------------------");
//    RangeSet[] charClass = usingSetsToCharclasses.values().stream().sorted().toArray(RangeSet[]::new);
//    for (int i = 0; i < charClass.length; ++i)
//      System.out.println(i + ": " + charClass[i]);

    Grammar result = ReplaceCharsets.process(copy, charsetToClasses);

    t.add(System.currentTimeMillis());

    if (options.contains(BlitzOption.TIMING))
      for (int i = 1; i < t.size(); ++i)
        System.err.println("                                                                   time: " + (t.get(i) - t.get(i - 1)) + " msec");

    return result;
  }

  @Override
  public void visit(Charset c) {
    super.visit(c);
    collect(c.getRangeSet(), c, c.getRule().getName());
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
        collect(deletedChars, charset, a.getRule().getName());
      }
      if (hasPreservedChars) {
        Charset charset = new Charset(false, preservedChars);
        replacement.addAlt(new Alt().addCharset(charset));
        collect(preservedChars, charset, a.getRule().getName());
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
    allSets.add(set);
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
    private Map<RangeSet, Set<RangeSet>> charsetToCharclasses;

    private ReplaceCharsets() {
    }

    public static Grammar process(Grammar g, Map<RangeSet, Set<RangeSet>> charsetToCharclasses) {
      ReplaceCharsets rc = new ReplaceCharsets();
      rc.charsetToCharclasses = charsetToCharclasses;
      rc.visit(g);
      rc.copy.setAdditionalNames(g.getAdditionalNames());
      PostProcess.process(rc.copy);
      return rc.copy;
    }

    @Override
    public void visit(Charset c) {
      Set<RangeSet> charClass = charsetToCharclasses.get(c.getRangeSet());
      if (charClass.size() == 1) {
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
    Builder builder = RangeSet.builder();
    allRangeSets.forEach(builder::addAll);
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
