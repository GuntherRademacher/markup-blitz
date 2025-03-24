// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.codepoints;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import de.bottlecaps.markup.blitz.grammar.Charset;
import de.bottlecaps.markup.blitz.grammar.Term;

public class RangeSet extends AbstractSet<Range> implements Comparable<RangeSet> {
  public static final RangeSet EOF = builder().add(Integer.MAX_VALUE).build();

  private final long[] ranges;

  private RangeSet(long[] ranges) {
    this.ranges  = ranges;
  }

  public RangeSet complement() {
    return UnicodeCategory.ALPHABET.minus(this);
  }

  public RangeSet union(RangeSet rangeSet) {
    Builder builder = new Builder();
    for (long range : ranges)
      builder.add(range);
    for (long range : rangeSet.ranges)
      builder.add(range);
    return builder.build();
  }

  public RangeSet intersection(RangeSet rangeSet) {
    Builder builder = new Builder();
    int lhsI = 0;
    long lhsR = lhsI < ranges.length ? ranges[lhsI++] : -1;
    int lhsF = firstCodepoint(lhsR);
    int lhsL = lastCodepoint(lhsR);
    int rhsI = 0;
    final long rhsR = rhsI < rangeSet.ranges.length ? rangeSet.ranges[rhsI++] : -1;
    int rhsF = firstCodepoint(rhsR);
    int rhsL = lastCodepoint(rhsR);
    while (lhsF >= 0 && rhsF >= 0) {
      if (lhsF <= rhsL && lhsL >= rhsF) {
        int rangeF = Math.max(lhsF, rhsF);
        int rangeL = Math.min(lhsL, rhsL);
        builder.add(range(rangeF, rangeL));
        if (lhsL > rangeL) {
          lhsF = rangeL;
        }
        else {
          final long l = lhsI < ranges.length ? ranges[lhsI++] : -1;
          lhsF = firstCodepoint(l);
          lhsL = lastCodepoint(l);
        }
        if (rhsL > rangeL) {
          rhsF = rangeL + 1;
        }
        else {
          final long r = rhsI < rangeSet.ranges.length ? rangeSet.ranges[rhsI++] : -1;
          rhsF = firstCodepoint(r);
          rhsL = lastCodepoint(r);
        }
      }
      else if (lhsL < rhsL) {
        final long l = lhsI < ranges.length ? ranges[lhsI++] : -1;
        lhsF = firstCodepoint(l);
        lhsL = lastCodepoint(l);
      }
      else {
        final long r = rhsI < rangeSet.ranges.length ? rangeSet.ranges[rhsI++] : -1;
        rhsF = firstCodepoint(r);
        rhsL = lastCodepoint(r);
      }
    }
    return builder.build();
  }

  public RangeSet minus(RangeSet rangeSet) {
    int removeI = 0;
    long removeR = removeI < rangeSet.ranges.length ? rangeSet.ranges[removeI++] : -1;
    int removeF = firstCodepoint(removeR);
    int removeL = lastCodepoint(removeR);
    int rangeI = 0;
    long rangeR = rangeI < ranges.length ? ranges[rangeI++] : -1;
    int rangeF = firstCodepoint(rangeR);
    int rangeL = lastCodepoint(rangeR);
    Builder builder = new Builder();
    while (rangeF >= 0) {
      if (removeF == -1 || rangeL < removeF) {
        // no overlap, range smaller
        builder.add(range(rangeF, rangeL));
        final long r = rangeI < ranges.length ? ranges[rangeI++] : -1;
        rangeF = firstCodepoint(r);
        rangeL = lastCodepoint(r);
      }
      else if (rangeF > removeL) {
        // no overlap, remove smaller
        final long r = removeI < rangeSet.ranges.length ? rangeSet.ranges[removeI++] : -1;
        removeF = firstCodepoint(r);
        removeL = lastCodepoint(r);
      }
      else {
        if (rangeF < removeF) {
          // overlap, left residual
          builder.add(rangeF, removeF - 1);
        }
        if (rangeL > removeL) {
          // overlap, right residual
          rangeF = removeL + 1;
          final long r = removeI < rangeSet.ranges.length ? rangeSet.ranges[removeI++] : -1;
          removeF = firstCodepoint(r);
          removeL = lastCodepoint(r);
        }
        else {
          final long r = rangeI < ranges.length ? ranges[rangeI++] : -1;
          rangeF = firstCodepoint(r);
          rangeL = lastCodepoint(r);
        }
      }
    }
    return builder.build();
  }

  public boolean containsCodepoint(int codepoint) {
    int lo = 0;
    int hi = ranges.length - 1;
    while (lo <= hi) {
      int m = (hi + lo) >> 1;
      long range = ranges[m];
      if (firstCodepoint(range) > codepoint)
        hi = m - 1;
      else if (lastCodepoint(range) < codepoint)
        lo = m + 1;
      else
        return true;
    }
    return false;
  }

  private static int firstCodepoint(long range) {
    return (int) (range >>> 32);
  }

  private static int lastCodepoint(long range) {
    return (int) (range & 0xffffffff);
  }

  private static long range(int firstCodepoint, int lastCodepoint) {
    return ((long) firstCodepoint << 32) | lastCodepoint;
  }

  public int charCount() {
    int charCount = 0;
    for (long range : ranges)
      charCount += lastCodepoint(range) - firstCodepoint(range) + 1;
    return charCount;
  }

  public boolean isSingleton() {
    if (ranges.length != 1)
      return false;
    long range = ranges[0];
    return firstCodepoint(range) == lastCodepoint(range);
  }

  @Override
  public String toString() {
    return this == EOF
        ? shortName()
        : rangesAsStream()
          .map(Range::toString)
          .collect(Collectors.joining("; ", "[", "]"));
  }

  private Stream<Range> rangesAsStream() {
    return LongStream.of(ranges)
        .mapToObj(range -> new Range(firstCodepoint(range), lastCodepoint(range)));
  }

  public String toJava() {
    return rangesAsStream()
      .map(Range::toJava)
      .collect(Collectors.joining("", "builder()", ".build()"));
  }

  @Override
  public Iterator<Range> iterator() {
    return rangesAsStream().iterator();
  }

  @Override
  public int size() {
    return ranges.length;
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
  public int compareTo(RangeSet other) {
    return Arrays.compare(ranges, other.ranges);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(ranges);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof RangeSet))
      return false;
    return Arrays.equals(ranges, ((RangeSet) obj).ranges);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private long[] ranges;
    private int size;

    private Builder() {
      ranges = new long[16];
      size = 0;
    }

    public Builder add(int codepoint) {
      return add(codepoint, codepoint);
    }

    public Builder add(long range) {
      if (ranges.length == size)
        ranges = Arrays.copyOf(ranges, size << 1);
      ranges[size++] = range;
      return this;
    }

    public Builder add(int firstCodepoint, int lastCodepoint) {
      return add(range(firstCodepoint, lastCodepoint));
    }

    public Builder add(Range range) {
      return add(range.getFirstCodepoint(), range.getLastCodepoint());
    }

    public Builder add(Collection<Range> ranges) {
      for (Range range : ranges)
        add(range);
      return this;
    }

    public Builder add(RangeSet rangeSet) {
      for (long range : rangeSet.ranges)
        add(range);
      return this;
    }

    private boolean isNormalized() {
      for (int i = 1; i < size; ++i)
        if (firstCodepoint(ranges[i]) <= lastCodepoint(ranges[i - 1]) + 1)
          return false;
      return true;
    }

    public RangeSet build() {
      if (! isNormalized()) {
        Arrays.sort(ranges, 0, size);
        long range = ranges[0];
        int first = firstCodepoint(range);
        int last = lastCodepoint(range);
        int s = 0;
        for (int r = 1; r < size; ++r) {
          range = ranges[r];
          int f = firstCodepoint(range);
          int l = lastCodepoint(range);
          if (f > last + 1) {
            ranges[s++] = range(first, last);
            first = f;
            last = l;
          }
          else if (l > last) {
            last = l;
          }
        }
        ranges[s++] = range(first, last);
        size = s;
      }
      RangeSet result = new RangeSet(Arrays.copyOf(ranges, size));
      ranges = null;
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
    return this == EOF
         ? "end of input"
         : ranges.length == 0
           ? "[]"
           : new Range(firstCodepoint(ranges[0])).toString() + (charCount() == 1 ? "" : "...");
  }

}