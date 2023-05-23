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
import de.bottlecaps.markup.blitz.grammar.Member;
import de.bottlecaps.markup.blitz.grammar.Node;
import de.bottlecaps.markup.blitz.grammar.Nonterminal;
import de.bottlecaps.markup.blitz.grammar.RangeMember;
import de.bottlecaps.markup.blitz.grammar.Rule;
import de.bottlecaps.markup.blitz.grammar.StringMember;
import de.bottlecaps.markup.blitz.grammar.Term;

public class CombineCharsets extends Copy {
  private Queue<String> todo;
  private Set<String> done;

  public CombineCharsets() {
  }

  public Grammar combine(Grammar g) {
    todo = new LinkedList<>();
    done = new HashSet<>();
    Rule firstRule = g.getRules().values().iterator().next();
    done.add(firstRule.getName());
    firstRule.accept(this);
    while (! todo.isEmpty()) {
      String name = todo.poll();
      done.add(name);
      g.getRules().get(name).accept(this);
    }
    PostProcess.process(copy);
    Map<Charset, Set<RangeSet>> charsetToCharclasses = new HashMap<>();
    RangeCollector.process(copy, charsetToCharclasses);

    System.out.println("-------- before replace:\n" + copy);
    return ReplaceCharsets.process(copy, charsetToCharclasses);
  }

  @Override
  public void visit(Nonterminal n) {
    Charset charset = CharsetCollector.collect(n);
    if (charset != null) {
      System.out.println(">>>>>> replacement for " + n);
      System.out.println("               chars: " + charset);
      alts.peek().last().getTerms().add(charset);
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
    Charset deletedChars = new Charset(true, false);
    boolean hasPreservedChars = false;
    Charset preservedChars = new Charset(false, false);;
    Alts other = new Alts();
    for (Alt alt : a.getAlts()) {
      Charset charset = CharsetCollector.collect(alt);
      if (charset == null) {
        other.addAlt(alt);
      }
      else if (charset.isDeleted()) {
        hasDeletedChars = true;
        deletedChars.getMembers().addAll(charset.getMembers());
      }
      else {
        hasPreservedChars = true;
        preservedChars.getMembers().addAll(charset.getMembers());
      }
    }
    if (other.equals(a)) {
      super.visit(a);
    }
    else {
      System.out.println(">>>>>> replacements for " + a);
      if (deletedChars.getMembers().size() > 0)
        System.out.println("        deletedChars: " + deletedChars);
      if (preservedChars.getMembers().size() > 0)
        System.out.println("      preservedChars: " + preservedChars);
      if (other.getAlts().size() > 0)
        System.out.println("               other: " + other);

      Alts replacement = new Alts();
      if (hasDeletedChars)
        replacement.addAlt(new Alt().addCharset(deletedChars));
      if (hasPreservedChars)
        replacement.addAlt(new Alt().addCharset(preservedChars));

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

  private static class CharsetCollector extends Visitor {
    private boolean isDeleted;
    private boolean isPreserved;
    private List<Member> members;

    private CharsetCollector() {
    }

    public static Charset collect(Node node) {
      CharsetCollector cc = new CharsetCollector();
      cc.members = new ArrayList<>();
      cc.isDeleted = true;
      cc.isPreserved = true;
      node.accept(cc);
      if (cc.members == null || cc.isPreserved == cc.isDeleted)
        return null;
      Charset charset = new Charset(cc.isDeleted, false);
      cc.members.forEach(member -> charset.getMembers().add(member));
      return charset;
    }

    @Override
    public void visit(Alt a) {
      if (a.getTerms().size() != 1)
        members = null;
      else
        super.visit(a);
    }

    @Override
    public void visit(Alts a) {
      super.visit(a);
    }

    @Override
    public void visit(Charset c) {
      if (members != null) {
        if (c.isExclusion()) {
          RangeSet.of(c).forEach(range -> members.add(new RangeMember(range)));
        }
        else {
          members.addAll(c.getMembers());
        }
        isPreserved = isPreserved && ! c.isDeleted();
        isDeleted = isDeleted && c.isDeleted();
      }
    }

    @Override
    public void visit(Control c) {
      members = null;
    }

    @Override
    public void visit(Grammar g) {
      members = null;
    }

    @Override
    public void visit(Insertion i) {
      members = null;
    }

    @Override
    public void visit(Literal l) {
      if (members != null) {
        if (l.isHex() || l.getValue().length() == 1) {
          members.add(new StringMember(l.getValue(), l.isHex()));
          isPreserved = isPreserved && ! l.isDeleted();
          isDeleted = isDeleted && l.isDeleted();
        }
        else {
          members = null;
        }
      }
    }

    @Override
    public void visit(Nonterminal n) {
      if (members != null) {
        if (n.getEffectiveMark() != Mark.DELETED) {
          members = null;
        }
        else {
          n.getGrammar().getRules().get(n.getName()).getAlts().accept(this);
        }
      }
    }

    @Override
    public void visit(Rule r) {
      members = null;
    }
  }

  private static class RangeCollector extends Visitor {
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

    /**
     * Process a grammar and populate the mapping from charsets to charclass sets.
     *
     * @param g the grammar.
     * @param charsetToCharclasses the resulting mapping from original sets to charclass
     *   sets. Should be passed as an empty map.
     */
    public static void process(Grammar g, Map<Charset, Set<RangeSet>> charsetToCharclasses) {
      RangeCollector cr = new RangeCollector();
      cr.usingSetToOrigins = new HashMap<>();
      cr.originToChars = new HashMap<>();
      cr.allRangeSets = new HashMap<>();
      cr.builder = new RangeSet.Builder();
      cr.visit(g);

      System.out.println("========= " + cr.allRangeSets.values().size() + " distinct range sets");
      cr.allRangeSets.values().forEach(v -> System.out.println("     " + v));

      cr.allRanges = cr.builder.build().split();

      System.out.println("========= " + cr.allRanges.size() + " distinct ranges:");
      cr.allRanges.forEach(v -> System.out.println("     " + v));

      cr.rangeToUsingSets = new TreeMap<>();
      for (RangeSet rangeSet : cr.allRangeSets.values()) {
        for (Range range : cr.allRanges.split(rangeSet)) {
          cr.rangeToUsingSets.compute(range, (k, v) -> {
            if (v == null) v = new HashSet<>();
            v.add(rangeSet);
            return v;
          });
        }
      }

      System.out.println("========= " + cr.rangeToUsingSets.size() + " ranges and using range sets:");
      cr.rangeToUsingSets.forEach((k, v) -> System.out.println("     " + k + " is used in:" + v));

      cr.usingSetsToCharclasses = new HashMap<>();
      cr.rangeToUsingSets.forEach((range, usingSets) -> {
        cr.usingSetsToCharclasses.compute(usingSets, (charclass, ranges) -> {
          Builder builder = new RangeSet.Builder();
          if (ranges != null)
            ranges.forEach(builder::add);
          builder.add(range);
          return builder.build();
        });
      });

      System.out.println("========= " + cr.usingSetsToCharclasses.size() + " charclasses:");
      cr.usingSetsToCharclasses.forEach((k, v) -> {
        int smallestEnclosingSetSize = Integer.MAX_VALUE;
        Set<String> originsOfSmallestEnclosingSet = new HashSet<>();
        for (RangeSet set : k) {
          int setSize = set.stream().mapToInt(Range::size).sum();
          if (setSize <= smallestEnclosingSetSize) {
            if (setSize < smallestEnclosingSetSize) {
              smallestEnclosingSetSize = setSize;
              originsOfSmallestEnclosingSet.clear();
            }
            originsOfSmallestEnclosingSet.addAll(cr.usingSetToOrigins.get(set));
          }
        }
        int minSetSize = Integer.MAX_VALUE;
        String originWithLeastChars = null;
        for (String name : originsOfSmallestEnclosingSet) {
          int size = cr.originToChars.get(name).stream().mapToInt(Range::size).sum();
          if (size < minSetSize)
            originWithLeastChars = name;
        }
        System.out.println("     from " + originWithLeastChars + ": " + v);
      });

      cr.allRangeSets.forEach((term, set) -> {
        Set<RangeSet> charclasses = new TreeSet<>();
        for (Range range : cr.allRanges.split(set))
          charclasses.add(cr.usingSetsToCharclasses.get(cr.rangeToUsingSets.get(range)));
        if (term instanceof Charset)
          charsetToCharclasses.put((Charset) term, charclasses);
      });

      System.out.println("========= " + charsetToCharclasses.size() + " set to charclasses mappings:");
      charsetToCharclasses.forEach((k, v) -> System.out.println("     " + k + " =>\n         " + v));

      System.out.println("=========");

      // TODO: create mapping each original (combined) charset to a choice over a set of charclasses.
      // Then replace in the grammar. After that each Literal should be a single char and each
      // Charset should represent a charclass. The only thing left to do is to separate out
      // hex Literals and Charsets and non-singular Charsets into token rules.
    }

    @Override
    public void visit(Charset c) {
      RangeSet set = RangeSet.of(c).join();
      Term term = c;
      collect(set, term);
    }

    @Override
    public void visit(Literal l) {
      if (l.isHex()) {
        Range range = new Range(Integer.parseInt(l.getValue().substring(1), 16));
        collect(RangeSet.of(range), l);
      }
      else {
        for (char c : l.getValue().toCharArray()) {
          Literal origin = new Literal(l.isDeleted(), Character.toString(c), false);
          origin.setRule(l.getRule());
          collect(RangeSet.of(new Range(c)), origin);
        }
      }
    }

    private void collect(RangeSet set, Term origin) {
      String name = origin.getRule().getName();
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
  }

  private static class ReplaceCharsets extends Copy {
    private Map<Charset, Set<RangeSet>> charsetToCharclasses;

    private ReplaceCharsets() {
    }

    public static Grammar process(Grammar g, Map<Charset, Set<RangeSet>> charsetToCharclasses) {
      ReplaceCharsets rc = new ReplaceCharsets();
      rc.charsetToCharclasses = charsetToCharclasses;
      rc.visit(g);
      PostProcess.process(rc.copy);
      return rc.copy;
    }

    @Override
    public void visit(Charset c) {
      Set<RangeSet> rangeSets = charsetToCharclasses.get(c);

      if (rangeSets.size() == 1) {
        RangeSet firstSet = rangeSets.iterator().next();
        Range firstRange = firstSet.iterator().next();
        if (firstSet.size() == 1 && firstRange.size() == 1) {
          // single character, represent as Literal
          int codePoint = firstRange.getFirstCodePoint();
          Literal literal = Range.isAscii(codePoint)
              ? new Literal(c.isDeleted(), Character.toString(codePoint), false)
              : new Literal(c.isDeleted(), "#" + Integer.toHexString(codePoint), true);
          alts.peek().last().getTerms().add(literal);
        }
        else {
          alts.peek().last().getTerms().add(firstSet.toTerm(c.isDeleted()));
        }
      }
      else {
        Alts a = new Alts();
        for (RangeSet rangeSet : rangeSets) {
          Alt alt = new Alt();
          alt.getTerms().add(rangeSet.toTerm(c.isDeleted()));
          a.addAlt(alt);
        }
        alts.peek().last().getTerms().add(a);
      }
    }
  }
}
