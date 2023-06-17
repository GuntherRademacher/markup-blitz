package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
  public void testCompressedMap(int log2) {
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    codeByRange.put(new Range(1, 10), 1);
    codeByRange.put(new Range(21, 30), 2);
    codeByRange.put(new Range(31, 40), 3);
    test(codeByRange, log2);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
  public void testAlphabet(int log2) {
    testRangeSet(RangeSet.ALPHABET, log2);
  }

  @ParameterizedTest
  @MethodSource("testUnicodeClassesArgs")
  public void testUnicodeClasses(RangeSet rangeSet, int log2) {
    testRangeSet(rangeSet, log2);
  }

  static Stream<Arguments> testUnicodeClassesArgs() {
    return RangeSet.unicodeClasses.entrySet().stream()
      .flatMap(e -> Stream.of(0, 1, 2, 3, 4, 5, 6, 7)
          .map(log2 -> Arguments.of(Named.of(e.getKey(), e.getValue()), log2)));
  }

  private void testRangeSet(RangeSet rangeSet, int log2) {
    int classes = rangeSet.size() << 1;
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    for (Range range : rangeSet)
      codeByRange.put(range, random.nextInt(classes));
    test(codeByRange, log2);
  }

  private void test(TreeMap<Range, Integer> codeByRange, int log2) {
    int lastCodepoint = codeByRange.descendingKeySet().iterator().next().getLastCodepoint();
    int[] originalData = new int[lastCodepoint + 1];
    Arrays.fill(originalData, 0);
    for (Map.Entry<Range, Integer> e : codeByRange.entrySet()) {
      Range range = e.getKey();
      int code = e.getValue();
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        originalData[codepoint] = code;
    }

    int tileSize = 1 << log2;
    TileIterator it = TileIterator.of(codeByRange, log2);
    int[] reconstructed = reconstruct(it, new CompressedMap(tileSize), log2);
    assertArrayEquals(
        Arrays.copyOf(originalData, reconstructed.length),
        reconstructed,
        () -> msgPrefix);
  }

  private int[] reconstruct(TileIterator it, CompressedMap map, int log2) {
    int[] data = map.process(it);
    int[] target = new int[map.numberOfTiles() * map.tileSize()];
    int mask = (1 << log2) - 1;
    for (int i = 0; i < target.length; ++i) {
      target[i] = data[data[i >> log2] + (i & mask)];
    }
    return target;
  }

}
