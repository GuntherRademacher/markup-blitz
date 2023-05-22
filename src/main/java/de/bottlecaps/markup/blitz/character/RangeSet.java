package de.bottlecaps.markup.blitz.character;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.ClassMember;
import de.bottlecaps.markup.blitz.grammar.Literal;
import de.bottlecaps.markup.blitz.grammar.Member;
import de.bottlecaps.markup.blitz.grammar.RangeMember;
import de.bottlecaps.markup.blitz.grammar.StringMember;
import de.bottlecaps.markup.blitz.grammar.Term;

public class RangeSet extends AbstractSet<Range> implements Comparable<RangeSet> {
  public static final RangeSet ALPHABET = RangeSet.of(
      new Range(0x9),
      new Range(0xA),
      new Range(0xD),
      new Range(' ', 0xD7FF),
      new Range(0xE000, 0xFFFD),
      new Range(0x10000, 0x10FFFD));
  public static final Map<String, RangeSet> unicodeClasses = new HashMap<>();

  private final TreeSet<Range> addedRanges = new TreeSet<>();

  private RangeSet() {
  }

  public RangeSet complement() {
    return ALPHABET.minus(this);
  }

  public RangeSet union(RangeSet rangeSet) {
    Builder builder = new Builder();
    stream().forEach(builder::add);
    rangeSet.stream().forEach(builder::add);
    return builder.build().join();
  }

  public RangeSet minus(RangeSet rangeSet) {
    Iterator<Range> removeIt = rangeSet.join().iterator();
    Range remove = removeIt.hasNext() ? removeIt.next() : null;
    Iterator<Range> rangeIt = join().iterator();
    Range range = rangeIt.hasNext() ? rangeIt.next() : null;
    Builder builder = new Builder();
    while (range != null) {
      if (remove == null || range.getLastCodePoint() < remove.getFirstCodePoint()) {
        // no overlap, range smaller
        builder.add(range);
        range = rangeIt.hasNext() ? rangeIt.next() : null;
      }
      // range.getLastCodePoint() >= remove.getFirstCodePoint()
      else if (range.getFirstCodePoint() > remove.getLastCodePoint()) {
        // no overlap, remove smaller
        remove = removeIt.hasNext() ? removeIt.next() : null;
      }
      else {
        if (range.getFirstCodePoint() < remove.getFirstCodePoint()) {
          // overlap, left residual
          builder.add(range.getFirstCodePoint(), remove.getFirstCodePoint() - 1);
        }
        if (range.getLastCodePoint() > remove.getLastCodePoint()) {
          // overlap, right residual
          range = new Range(remove.getLastCodePoint() + 1, range.getLastCodePoint());
          remove = removeIt.hasNext() ? removeIt.next() : null;
        }
        else {
          range = rangeIt.hasNext() ? rangeIt.next() : null;
        }
      }
    }
    return builder.build();
  }

  public RangeSet join() {
    Integer firstCodePoint = null;
    Integer lastCodePoint = null;
    Builder builder = new Builder();
    for (Range range : split(addedRanges)) {
      if (firstCodePoint == null) {
        firstCodePoint = range.getFirstCodePoint();
      }
      else if (lastCodePoint + 1 != range.getFirstCodePoint()) {
        builder.add(new Range(firstCodePoint, lastCodePoint));
        firstCodePoint = range.getFirstCodePoint();
      }
      lastCodePoint = range.getLastCodePoint();
    }
    if (firstCodePoint != null)
      builder.add(new Range(firstCodePoint, lastCodePoint));
    return builder.build();
  }

  public RangeSet split(Set<Range> ranges) {
    TreeSet<Integer> lastCodePoints = new TreeSet<>();
    addedRanges.forEach(r -> {
      lastCodePoints.add(r.getFirstCodePoint() - 1);
      lastCodePoints.add(r.getLastCodePoint());
    });
    Builder builder = new Builder();
    for (Range range : ranges) {
      for (int firstCodePoint = range.getFirstCodePoint(), lastCodePoint;
          firstCodePoint <= range.getLastCodePoint();
          firstCodePoint = lastCodePoint + 1) {
        lastCodePoint = lastCodePoints.ceiling(firstCodePoint);
        builder.add(new Range(firstCodePoint, lastCodePoint));
      }
    }
    return builder.build();
  }

  public RangeSet split() {
    return split(addedRanges);
  }

  @Override
  public String toString() {
    return addedRanges.stream()
      .map(Range::toString)
      .collect(Collectors.joining("; ", "[", "]"));
  }

  @Override
  public Iterator<Range> iterator() {
    return addedRanges.iterator();
  }

  @Override
  public int size() {
    return addedRanges.size();
  }

  @Override
  public boolean add(Range e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends Range> c) {
    throw new UnsupportedOperationException();
  }

  public static final class Builder {
    private RangeSet set = new RangeSet();

    public Builder add(int codePoint) {
      return add(codePoint, codePoint);
    }

    public Builder add(int firstCodePoint, int lastCodePoint) {
      return add(new Range(firstCodePoint, lastCodePoint));
    }

    public Builder add(Range range) {
      set.addedRanges.add(range);
      return this;
    }

    public RangeSet build() {
      RangeSet result = set;
      set = null;
      return result;
    }
  }

  public static RangeSet of(Range... ranges) {
    Builder builder = new Builder();
    for (Range range : ranges)
      builder.add(range);
    return builder.build();
  }

  public static RangeSet of(String unicodeCharClassName) {
    if (unicodeClasses.containsKey(unicodeCharClassName))
      return unicodeClasses.get(unicodeCharClassName);

    Builder builder = new Builder();

    int first = 0;
    int last = 0x10FFFF;

    Pattern pattern = Pattern.compile("\\p{" + unicodeCharClassName + "}$");

    int lo = 0;
    int hi = 0;
    Boolean matches = null;

    for (int i = first; i <= last + 1; ++i)
    {
      boolean inClass = i > last || i > 0xD7FF && i < 0xE000 || i > 0xFFFD && i < 0x10000
          ? false
          : pattern.matcher(new String(Character.toChars(i))).matches();

      if (matches == null)
      {
        lo = i;
      }
      else if (matches != inClass)
      {
        if (matches)
          builder.add(new Range(lo, hi));
        lo = i;
      }
      hi = i;
      matches = inClass;
    }
    RangeSet unicodeClass = builder.build();
    unicodeClasses.put(unicodeCharClassName, unicodeClass);
    return unicodeClass;
  }

  public static RangeSet of(Charset c) {
    Builder builder = new Builder();
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
        of(((ClassMember) member).getValue()).forEach(builder::add);
      }
      else {
        throw new IllegalStateException();
      }
    }
    return c.isExclusion()
         ? ALPHABET.minus(builder.build())
         : builder.build();
  }

  public Term toTerm(boolean isDeleted) {
    if (addedRanges.size() == 1 && addedRanges.iterator().next().size() == 1) {
      int codePoint = addedRanges.iterator().next().getFirstCodePoint();
      return Range.isAscii(codePoint)
          ? new Literal(isDeleted, Character.toString(codePoint), false)
          : new Literal(isDeleted, "#" + Integer.toHexString(codePoint), true);
    }
    else {
      Charset set = new Charset(isDeleted, false);
      for (Range range : addedRanges)
        set.getMembers().add(new RangeMember(range));
      return set;
    }
  }

  @Override
  public int compareTo(RangeSet o) {
    Iterator<Range> li = iterator();
    Iterator<Range> ri = o.iterator();
    while (li.hasNext() && ri.hasNext()) {
      Range ln = li.next();
      Range rn = ri.next();
      int c = ln.compareTo(rn);
      if (c != 0)
        return c;
    }
    if (! li.hasNext() && ! ri.hasNext())
      return 0;
    if (! li.hasNext())
      return -1;
    return 1;
  }

}