// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
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

import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;

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
  public void testSimpledMap() {
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    codeByRange.put(new Range(1, 10), 1);
    codeByRange.put(new Range(21, 30), 2);
    codeByRange.put(new Range(31, 40), 3);
    test(codeByRange);
  }

  @Test
  public void testZeroes() {
    testRangeSet(RangeSet.builder().build());
  }

  @Test
  public void testAlphabet() {
    testRangeSet(UnicodeCategory.ALPHABET);
  }

  @ParameterizedTest
  @MethodSource("testUnicodeClassesArgs")
  public void testUnicodeClasses(RangeSet rangeSet) {
    testRangeSet(rangeSet);
  }

  static Stream<Arguments> testUnicodeClassesArgs() {
    return UnicodeCategory.codepointsByCode.entrySet().stream()
      .map(e -> Arguments.of(Named.of(e.getKey(), e.getValue())));
  }

  private void testRangeSet(RangeSet rangeSet) {
    TreeMap<Range, Integer> codeByRange = codeByRange(rangeSet);
    test(codeByRange);
  }

  private TreeMap<Range, Integer> codeByRange(RangeSet rangeSet) {
    int classes = rangeSet.size() << 1;
    TreeMap<Range, Integer> codeByRange = new TreeMap<>();
    for (Range range : rangeSet)
      codeByRange.put(range, random.nextInt(classes));
    return codeByRange;
  }

  private void test(TreeMap<Range, Integer> codeByRange) {
    int end = 0xD800;
    int defaultValue = random.nextInt(end);
    int[] originalData = setupOriginalData(codeByRange, end, defaultValue);
    test(originalData, bits -> TileIterator.of(codeByRange, end, bits, defaultValue));
  }

  private void test(int[] originalData, Function<Integer, TileIterator> iteratorSupplier) {
    int[] randomValues = new int[1000000];
    for (int i = 0; i < randomValues.length; ++i)
      randomValues[i] = random.nextInt(originalData.length);

    Map<Integer, Integer> hashMap = new HashMap<>();
    int defaultValue = iteratorSupplier.apply(2).defaultValue();
    for (int i = 0; i < originalData.length; ++i)
      if (originalData[i] != defaultValue)
        hashMap.put(i, originalData[i]);

    for (int maxDepth = 1; maxDepth <= 8; ++maxDepth) {
      CompressedMap map = new CompressedMap(iteratorSupplier, maxDepth);
      int[] data = map.data();
      if (map.shift().length == maxDepth) {
        validate(data);
        int[] reconstructed = reconstruct(map, originalData.length);
        assertArrayEquals(originalData, reconstructed, msgPrefix);

        int expectedSum = 0;
        for (int v : randomValues) {
          Integer code = hashMap.get(v);
          if (code != null)
            expectedSum += code;
          else
            expectedSum += defaultValue;
        }

        int sum = 0;
        for (int v : randomValues)
          sum += map.get(v);
        assertEquals(expectedSum, sum, msgPrefix);
      }
    }
  }

  private void validate(int[] data) {
    int firstTileOffset = data[0];
    int numberOfTiles = firstTileOffset;

    Set<Tile> distinctTiles = new HashSet<>();
    int lastTileOffset = firstTileOffset;
    int tileSize = 0;
    for (int t = 0; t < numberOfTiles; ++t) {
      if (data[t] > lastTileOffset) {
        if (tileSize == 0) {
          tileSize = data[t] - lastTileOffset;
          int[] tile = new int[tileSize];
          for (int i = 0; i < tileSize; ++i)
            tile[i] = data[firstTileOffset + i];
          assertTrue(distinctTiles.add(new Tile(tile)), msgPrefix);
        }
        else {
          assertEquals(tileSize, data[t] - lastTileOffset, msgPrefix);
        }
        lastTileOffset = data[t];
        assertTrue(lastTileOffset <= data.length - tileSize, msgPrefix);
        int[] tile = new int[tileSize];
        for (int i = 0; i < tileSize; ++i)
          tile[i] = data[lastTileOffset + i];
        assertTrue(distinctTiles.add(new Tile(tile)), msgPrefix);
      }
      else {
        assertTrue(data[t] >= firstTileOffset, msgPrefix);
      }
    }
    if (tileSize == 0) {
      tileSize = data.length - firstTileOffset;
      int[] tile = new int[tileSize];
      for (int i = 0; i < tileSize; ++i)
        tile[i] = data[firstTileOffset + i];
      assertTrue(distinctTiles.add(new Tile(tile)), msgPrefix);
    }
  }

  private int[] setupOriginalData(TreeMap<Range, Integer> codeByRange, int end, int defaultValue) {
    int[] originalData = new int[end];
    Arrays.fill(originalData, defaultValue);
    for (Map.Entry<Range, Integer> e : codeByRange.entrySet()) {
      Range range = e.getKey();
      int code = e.getValue();
      for (int codepoint = range.getFirstCodepoint();
           codepoint <= range.getLastCodepoint() && codepoint < end;
           ++codepoint)
        originalData[codepoint] = code;
    }
    return originalData;
  }

  private int[] reconstruct(CompressedMap map, int uncompressedSize) {
    int[] target = new int[uncompressedSize];
    for (int i = 0; i < target.length; ++i) {
      target[i] = map.get(i);
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
