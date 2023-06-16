package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;

public class TileIteratorTest {
  private Random random;
  private String msgPrefix;

  @BeforeEach
  public void beforeEach() {
    random = new Random();
    long seed = random.nextLong();
    random.setSeed(seed);
    msgPrefix = "While testing with seed=" + seed + "L: ";
  }

  @Test
  public void testTileIterator() {
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    codeByRange.put(new Range(1, 10), 1);
    codeByRange.put(new Range(21, 30), 2);
    codeByRange.put(new Range(31, 40), 3);
    test(codeByRange);
  }

  @Test
  public void testAlphabet() {
    testRangeSet(RangeSet.ALPHABET);
  }

  @Test
  public void testUnicodeClasses() {
    for (RangeSet rangeSet : RangeSet.unicodeClasses.values())
    testRangeSet(rangeSet);
  }

  private void testRangeSet(RangeSet rangeSet) {
    int classes = rangeSet.size() << 1;
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    for (Range range : rangeSet)
      codeByRange.put(range, random.nextInt(classes));
    test(codeByRange);
  }

  private void test(TreeMap<Range, Integer> codeByRange) {
    int lastCodepoint = codeByRange.descendingKeySet().iterator().next().getLastCodepoint();
    int[] originalData = new int[lastCodepoint + 1];
    Arrays.fill(originalData, 0);
    for (Map.Entry<Range, Integer> e : codeByRange.entrySet()) {
      Range range = e.getKey();
      int code = e.getValue();
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        originalData[codepoint] = code;
    }

    int maxLog2 = log2(lastCodepoint) + 1;
    for (int log2 = 1; log2 <= maxLog2; ++log2) {
      int tileSize = 1 << log2;
      TileIterator it = TileIterator.of(codeByRange, log2);
      int[] reconstructed = reconstruct(it, log2);
      assertArrayEquals(originalData, Arrays.copyOf(reconstructed, originalData.length), () -> msgPrefix);
    }
  }

  private int[] reconstruct(TileIterator it, int powerOf2) {
    int tileSize = 1 << powerOf2;
    int[] target = new int[tileSize];
    for (int offset = 0;;) {
      if (offset + tileSize > target.length)
        target = Arrays.copyOf(target, target.length << 1);
      int count = it.next(target, offset);
      if (count == 0)
        return target;
      offset += tileSize;
      for (int i = 1; i < count; ++i) {
        if (offset + tileSize > target.length)
          target = Arrays.copyOf(target, target.length << 1);
        System.arraycopy(target, offset - tileSize, target, offset, tileSize);
        offset += tileSize;
      }
    }
  }

  public static int log2(int value) {
    int log = 0;
    int v = value;
    if ((v & 0xffff0000) != 0) {
      v >>>= 16;
      log = 16;
    }
    if (v >= 256) {
      v >>>= 8;
      log += 8;
    }
    if (v >= 16) {
      v >>>= 4;
      log += 4;
    }
    if (v >= 4) {
      v >>>= 2;
      log += 2;
    }
    return log + (v >>> 1);
  }
}
