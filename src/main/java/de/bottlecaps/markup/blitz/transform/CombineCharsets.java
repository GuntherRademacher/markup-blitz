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
    RangeCollector.process(copy, new TreeMap<>());

    return copy;
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
    Charset charset = CharsetCollector.collect(a);
    boolean hasDeletedChars = false;
    Charset deletedChars = new Charset(true, false);
    boolean hasPreservedChars = false;
    Charset preservedChars = new Charset(false, false);;
    Alts other = new Alts();
    for (Alt alt : a.getAlts()) {
      charset = CharsetCollector.collect(alt);
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
    private Set<RangeSet> allRangeSets;
    /** Builder for the set of all ranges from all sets. */
    private RangeSet.Builder builder;
    /** All ranges that are used in the grammar . */
    private RangeSet allRanges;
    /** The set of sets using each range. The distinct values of this provide the char classes. */
    private Map<Range, Set<RangeSet>> rangeToUsingSets = new TreeMap<>();
    /** The char classes by their using sets. */
    private Map<Set<RangeSet>, RangeSet> usingSetsToCharclasses;

    /**
     * Process a grammar and populate the mapping from charsets to charclass sets.
     *
     * @param g the grammar.
     * @param charsetToCharclasses the resulting mapping from original sets to charclass
     *   sets. Should be passed as an empty map.
     */
    public static void process(Grammar g, Map<RangeSet, Set<RangeSet>> charsetToCharclasses) {
      RangeCollector cr = new RangeCollector();
      cr.allRangeSets = new HashSet<>();
      cr.builder = new RangeSet.Builder();
      cr.visit(g);

      System.out.println("========= " + cr.allRangeSets.size() + " distinct range sets");
      cr.allRangeSets.forEach(v -> System.out.println("     " + v));

      cr.allRanges = cr.builder.build().split();

      System.out.println("========= " + cr.allRanges.size() + " distinct ranges:");
      cr.allRanges.forEach(v -> System.out.println("     " + v));

      cr.rangeToUsingSets = new TreeMap<>();
      for (RangeSet rangeSet : cr.allRangeSets) {
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
      cr.usingSetsToCharclasses.values().stream().sorted().forEach(v -> System.out.println("     " + v));

      charsetToCharclasses.clear();
      for (RangeSet set : cr.allRangeSets) {
        Set<RangeSet> charclasses = new TreeSet<>();
        for (Range range : cr.allRanges.split(set))
          charclasses.add(cr.usingSetsToCharclasses.get(cr.rangeToUsingSets.get(range)));
        charsetToCharclasses.put(set, charclasses);
      }

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
      allRangeSets.add(set);
      set.forEach(builder::add);
    }

    @Override
    public void visit(Literal l) {
      if (l.isHex()) {
        Range range = new Range(Integer.parseInt(l.getValue().substring(1), 16));
        allRangeSets.add(RangeSet.of(range));
        builder.add(range);
      }
      else {
        for (char c : l.getValue().toCharArray()) {
          Range range = new Range(c);
          allRangeSets.add(RangeSet.of(range));
          builder.add(range);
        }
      }
    }
  }

  private static class ReplaceCharsets extends Copy {

  }
}
