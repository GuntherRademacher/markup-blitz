package de.bottlecaps.markup.blitz.codepoints;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Term;

public class RangeSet extends AbstractSet<Range> implements Comparable<RangeSet> {
  private final NavigableSet<Range> ranges;

  private RangeSet(NavigableSet<Range> ranges) {
    this.ranges  = ranges;
  }

  public RangeSet complement() {
    return UnicodeCategory.ALPHABET.minus(this);
  }

  public RangeSet union(RangeSet rangeSet) {
    Builder builder = new Builder();
    stream().forEach(builder::add);
    rangeSet.stream().forEach(builder::add);
    return builder.build();
  }

  public RangeSet intersection(RangeSet rangeSet) {
    Builder builder = new Builder();
    Iterator<Range> lhsIt = rangeSet.iterator();
    Range lhs = lhsIt.hasNext() ? lhsIt.next() : null;
    Iterator<Range> rhsIt = iterator();
    Range rhs = rhsIt.hasNext() ? rhsIt.next() : null;
    while (lhs != null && rhs != null) {
      if (lhs.overlaps(rhs)) {
        Range range = new Range(Math.max(lhs.getFirstCodepoint(), rhs.getFirstCodepoint()),
                                Math.min(lhs.getLastCodepoint(),  rhs.getLastCodepoint()));
        builder.add(range);
        if (lhs.getLastCodepoint() > range.getLastCodepoint())
          lhs = new Range(range.getLastCodepoint() + 1, lhs.getLastCodepoint());
        else
          lhs = lhsIt.hasNext() ? lhsIt.next() : null;
        if (rhs.getLastCodepoint() > range.getLastCodepoint())
          rhs = new Range(range.getLastCodepoint() + 1, rhs.getLastCodepoint());
        else
          rhs = rhsIt.hasNext() ? rhsIt.next() : null;
      }
      else if (lhs.getLastCodepoint() < rhs.getLastCodepoint()) {
        lhs = lhsIt.hasNext() ? lhsIt.next() : null;
      }
      else {
        rhs = rhsIt.hasNext() ? rhsIt.next() : null;
      }
    }
    return builder.build();
  }

  public RangeSet minus(RangeSet rangeSet) {
    Iterator<Range> removeIt = rangeSet.iterator();
    Range remove = removeIt.hasNext() ? removeIt.next() : null;
    Iterator<Range> rangeIt = iterator();
    Range range = rangeIt.hasNext() ? rangeIt.next() : null;
    Builder builder = new Builder();
    while (range != null) {
      if (remove == null || range.getLastCodepoint() < remove.getFirstCodepoint()) {
        // no overlap, range smaller
        builder.add(range);
        range = rangeIt.hasNext() ? rangeIt.next() : null;
      }
      // range.getLastCodepoint() >= remove.getFirstCodepoint()
      else if (range.getFirstCodepoint() > remove.getLastCodepoint()) {
        // no overlap, remove smaller
        remove = removeIt.hasNext() ? removeIt.next() : null;
      }
      else {
        if (range.getFirstCodepoint() < remove.getFirstCodepoint()) {
          // overlap, left residual
          builder.add(range.getFirstCodepoint(), remove.getFirstCodepoint() - 1);
        }
        if (range.getLastCodepoint() > remove.getLastCodepoint()) {
          // overlap, right residual
          range = new Range(remove.getLastCodepoint() + 1, range.getLastCodepoint());
          remove = removeIt.hasNext() ? removeIt.next() : null;
        }
        else {
          range = rangeIt.hasNext() ? rangeIt.next() : null;
        }
      }
    }
    return builder.build();
  }

  public boolean containsCodepoint(int codepoint) {
    Range floor = ranges.floor(new Range(codepoint));
    if (floor == null)
      floor = ranges.first();
    else if (floor.getLastCodepoint() < codepoint)
      floor = ranges.higher(floor);
    return floor != null && floor.getFirstCodepoint() <= codepoint && codepoint <= floor.getLastCodepoint();
  }

  public int charCount() {
    return stream().mapToInt(Range::size).sum();
  }

  public boolean isSingleton() {
    return ranges.size() == 1
        && ranges.iterator().next().isSingleton();
  }

  @Override
  public String toString() {
    return ranges.stream()
      .map(Range::toString)
      .collect(Collectors.joining("; ", "[", "]"));
  }

  public String toJava() {
    return ranges.stream()
      .map(Range::toJava)
      .collect(Collectors.joining("", "builder()", ".build()"));
  }

  @Override
  public Iterator<Range> iterator() {
    return ranges.iterator();
  }

  @Override
  public int size() {
    return ranges.size();
  }

  @Override
  public boolean add(Range e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends Range> c) {
    throw new UnsupportedOperationException();
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((ranges == null) ? 0 : ranges.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (!(obj instanceof RangeSet))
      return false;
    RangeSet other = (RangeSet) obj;
    if (ranges == null) {
      if (other.ranges != null)
        return false;
    }
    else if (!ranges.equals(other.ranges))
      return false;
    return true;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private NavigableSet<Range> set = new TreeSet<>();

    private Builder() {
    }

    public Builder add(int codepoint) {
      return add(codepoint, codepoint);
    }

    public Builder add(int firstCodepoint, int lastCodepoint) {
      return add(new Range(firstCodepoint, lastCodepoint));
    }

    public Builder add(Range range) {
      set.add(range);
      return this;
    }

    public Builder add(Collection<Range> ranges) {
      set.addAll(ranges);
      return this;
    }

    public Builder add(RangeSet rangeSet) {
      set.addAll(rangeSet.ranges);
      return this;
    }

    public RangeSet build() {
      Range r = null;
      boolean isNormalized = true;
      for (Range range : set) {
        if (r != null && range.getFirstCodepoint() <= r.getLastCodepoint() + 1) {
          isNormalized = false;
          break;
        }
        r = range;
      }
      if (! isNormalized) {
        int firstCodepoint = -1;
        int lastCodepoint = -1;
        NavigableSet<Range> joinedRanges = new TreeSet<>();
        for (Range range : set) {
          if (firstCodepoint < 0) {
            firstCodepoint = range.getFirstCodepoint();
          }
          else if (lastCodepoint + 1 < range.getFirstCodepoint()) {
            joinedRanges.add(new Range(firstCodepoint, lastCodepoint));
            firstCodepoint = range.getFirstCodepoint();
          }
          lastCodepoint = Math.max(lastCodepoint, range.getLastCodepoint());
        }
        if (firstCodepoint >= 0)
          joinedRanges.add(new Range(firstCodepoint, lastCodepoint));
        set = joinedRanges;
      }
      RangeSet result = new RangeSet(set);
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

  public Term toCharset(boolean isDeleted) {
    return new Charset(isDeleted, this);
  }

  public static final RangeSet EMPTY = builder().build();

  public String shortName() {
    return ranges.isEmpty()
         ? "$"
         : new Range(ranges.first().getFirstCodepoint()).toString() + (charCount() == 1 ? "" : "...");
  }

}