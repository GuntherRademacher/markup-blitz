package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    seed = 1283396090540636249L;
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
  public void testZeroes() {
    int[] zeroes = new int[100];
    for (int depth = 1; depth < 10; ++depth) {
      int[] data = new CompressedMap(bits -> TileIterator.of(zeroes, zeroes.length, bits, 0), 1).tiles();
      System.out.println("depth " + depth + ", size " + data.length + ", " + Arrays.toString(data));
      validate(data);
    }
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
    int[] originalData = setupOriginalData(codeByRange, 0xD7FF);
    test(1, originalData, tileIndexBits -> TileIterator.of(codeByRange, 0xD800, tileIndexBits, 0));
  }

  private void test(int depth, int[] originalData, Function<Integer, TileIterator> iteratorSupplier) {
    CompressedMap map = new CompressedMap(iteratorSupplier, 1);
    int[] data = map.tiles();
    System.out.println("compressed from " + originalData.length + " to " + data.length + " (" + data.length * 100 / originalData.length + "%), tileSize " + map.tileSize());

    validate(data);

    int[] reconstructed = reconstruct(data, originalData.length, map.tileIndexBits());
    assertArrayEquals(
        originalData,
        Arrays.copyOf(reconstructed, originalData.length),
        () -> msgPrefix);

//    map = new CompressedMap(iteratorSupplier, 2);
//    data = map.tiles();
//    validate(data);
  }

  private void validate(int[] data) {
    int firstTileOffset = data[0];
    int numberOfTiles = firstTileOffset;
    assertTrue(numberOfTiles < data.length);

    Set<Tile> distinctTiles = new HashSet<>();
    int lastTileOffset = firstTileOffset;
    int tileSize = 0;
    for (int i = 1; i < numberOfTiles; ++i) {
      if (data[i] > lastTileOffset) {
        if (tileSize == 0) {
          tileSize = data[i] - lastTileOffset;
          assertTrue(distinctTiles.add(new Tile(Arrays.copyOfRange(data, firstTileOffset , firstTileOffset + tileSize))));
        }
        else {
          assertEquals(tileSize, data[i] - lastTileOffset);
        }
        lastTileOffset = data[i];
        assertTrue(lastTileOffset <= data.length - tileSize);
        assertTrue(distinctTiles.add(new Tile(Arrays.copyOfRange(data, lastTileOffset , lastTileOffset + tileSize))));
      }
      else {
        assertTrue(data[i] >= firstTileOffset);
      }
    }
    if (tileSize == 0)
      tileSize = data.length - firstTileOffset;
    int tileIndexBits = -1;
    for (int i = 0; (1 << i) < lastTileOffset + tileSize; ++i)
      if ((1 << i) == tileSize)
        tileIndexBits = i;
    assertTrue(tileIndexBits > 0, "Unexpected tile size: " + tileSize);

    int length = lastTileOffset + tileSize;
    if (length < data.length) {

    }
  }

  private int[] setupOriginalData(TreeMap<Range, Integer> codeByRange, int lastCodepoint) {
    int[] originalData = new int[lastCodepoint + 1];
    Arrays.fill(originalData, 0);
    for (Map.Entry<Range, Integer> e : codeByRange.entrySet()) {
      Range range = e.getKey();
      int code = e.getValue();
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint() && codepoint <= lastCodepoint; ++codepoint)
        originalData[codepoint] = code;
    }
    return originalData;
  }

  private int[] reconstruct(int[] data, int uncompressedSize, int tileIndexBits) {
    int[] target = new int[uncompressedSize];
    int mask = (1 << tileIndexBits) - 1;
    for (int i = 0; i < target.length; ++i) {
      target[i] = data[data[i >> tileIndexBits] + (i & mask)];
    }
    return target;
  }

  private class Tile {
    private int[] value;
    private int hashCode;

    public Tile(int[] value) {
      this.value = value;
      final int prime = 31;
      int hashCode = 1;
      for (int i = 0; i < value.length; ++i)
        hashCode = prime * hashCode + value[i];;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      Tile other = (Tile) obj;
      return Arrays.equals(      value, 0,       value.length,
                           other.value, 0, other.value.length);
    }
  }

}
