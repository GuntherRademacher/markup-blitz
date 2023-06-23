package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;

public class CompressedMapTest {
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
  public void testCompressedMap() {
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

  @ParameterizedTest
  @MethodSource("testUnicodeClassesArgs")
  public void testUnicodeClasses(RangeSet rangeSet) {
    testRangeSet(rangeSet);
  }

  static Stream<Arguments> testUnicodeClassesArgs() {
    return RangeSet.unicodeClasses.entrySet().stream()
      .map(e -> Arguments.of(Named.of(e.getKey(), e.getValue())));
  }

  private void testRangeSet(RangeSet rangeSet) {
    int classes = rangeSet.size() << 1;
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    for (Range range : rangeSet)
      codeByRange.put(range, random.nextInt(classes));
    test(codeByRange);
  }

  private void test(TreeMap<Range, Integer> codeByRange) {
    int[] originalData = setupOriginalData(codeByRange);
    test(1, originalData, log2 -> TileIterator.of(codeByRange, log2));
  }

  private void test(int depth, int[] originalData, Function<Integer, TileIterator> iteratorSupplier) {
    boolean foundBest = false;
    int bestLog2 = 0;
    Integer bestLength = null;

    for (int log2 = 0; log2 < 8; ++log2) {

      int tileSize = 1 << log2;
      TileIterator it = iteratorSupplier.apply(log2);

      CompressedMap map = new CompressedMap(tileSize);
      int[] data = map.process(it);
      for (int i = 0; i < depth - 1; ++i)
        System.out.print("  ");
      System.out.println("compressed from " + originalData.length + " to " + data.length + " (" + data.length * 100 / originalData.length + "%), tileSize " + map.tileSize());

      assertTrue(map.tileSize() == 1 << log2);
      assertTrue(map.numberOfTiles() == (originalData.length + map.tileSize() - 1) / map.tileSize());
      assertTrue(data.length >= map.numberOfTiles() + map.tileSize());

      int uncompressedSize = map.numberOfTiles() * map.tileSize();
      int[] reconstructed = reconstruct(data, uncompressedSize, log2);
      assertArrayEquals(
          originalData,
          Arrays.copyOf(reconstructed, originalData.length),
          () -> msgPrefix);

      if (bestLength == null) {
        bestLength = data.length;
        bestLog2 = log2;
      }
      else if (! foundBest) {
        if (data.length < bestLength) {
          bestLength = data.length;
          bestLog2 = log2;
        }
        else {
          foundBest = true;
        }
      }
      else if (data.length < 0.9 * originalData.length){
        assertTrue(data.length >= bestLength);
      }
    }

    if (bestLength < 0.75 * originalData.length) {
      CompressedMap map = new CompressedMap(1 << bestLog2);
      int[] data = map.process(iteratorSupplier.apply(bestLog2));
      test(depth + 1, data, log2 -> TileIterator.of(data, log2));
    }
    else {
      for (int i = 0; i < depth - 1; ++i)
        System.out.print("  ");
      System.out.println("use depth " + (depth - 1) + " for a compressed size of " + (originalData.length * 100 + (CompressedMap.END >> 1)) / CompressedMap.END + " %");
    }
  }

  private int[] setupOriginalData(TreeMap<Range, Integer> codeByRange) {
    int lastCodepoint = codeByRange.descendingKeySet().iterator().next().getLastCodepoint();
    int[] originalData = new int[lastCodepoint + 1];
    Arrays.fill(originalData, 0);
    for (Map.Entry<Range, Integer> e : codeByRange.entrySet()) {
      Range range = e.getKey();
      int code = e.getValue();
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint) {
        if (codepoint >= CompressedMap.END)
          return Arrays.copyOf(originalData, CompressedMap.END);
        originalData[codepoint] = code;
      }
    }
    return originalData;
  }

  private int[] reconstruct(int[] data, int uncompressedSize, int log2) {
    int[] target = new int[uncompressedSize];
    int mask = (1 << log2) - 1;
    for (int i = 0; i < target.length; ++i) {
      target[i] = data[data[i >> log2] + (i & mask)];
    }
    return target;
  }

}
