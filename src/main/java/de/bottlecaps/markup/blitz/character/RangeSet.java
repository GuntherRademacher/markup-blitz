package de.bottlecaps.markup.blitz.character;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RangeSet extends AbstractSet<Range> {
  private final TreeSet<Range> addedRanges = new TreeSet<>();

  private RangeSet() {
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

  public static RangeSet ofUnicodeCharClass(String charClass)
  {
    Builder builder = new Builder();

    int first = 0;
    int last = 0x10FFFF;

    Pattern pattern = Pattern.compile("\\p{" + charClass + "}$");

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
    return builder.build();
  }
}
