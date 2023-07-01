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
    CompressedMap map = new CompressedMap(bits -> TileIterator.of(zeroes, zeroes.length, bits, 0), 1);
    int[] data = map.tiles();
    assertTrue(map.shift()[0] >= 1 && map.shift()[0] <= 6);
    int[] reconstruction = reconstruct(data, 100, map.shift());
    assertArrayEquals(zeroes, reconstruction);
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

//    //TODO: remove
//    int end = 509;

    int defaultValue = 0;

    int[] originalData = setupOriginalData(codeByRange, end);
    test(originalData, bits -> TileIterator.of(codeByRange, end, bits, defaultValue));
  }

  private void test(int[] originalData, Function<Integer, TileIterator> iteratorSupplier) {
    CompressedMap map = new CompressedMap(iteratorSupplier, 1);
    int[] data = map.tiles();

    validate(data);

    int[] reconstructed = reconstruct(data, originalData.length, map.shift()[0]);
    assertArrayEquals(
        originalData,
        Arrays.copyOf(reconstructed, originalData.length),
        () -> msgPrefix);

    System.out.println("maxDepth " + 1 + ", depth " + map.shift().length + ", size " + data.length);

    boolean multipleValues = false;
    int value = originalData[0];
    for (int i = 1; i < originalData.length; ++i)
      if (originalData[i] != value) {
        multipleValues = true;
        break;
      }

    if (multipleValues) {
      for (int maxDepth = 2; maxDepth <= 6; ++maxDepth) {
        map = new CompressedMap(iteratorSupplier, maxDepth);
        data = map.tiles();
        System.out.println("maxDepth " + maxDepth + ", depth " + map.shift().length + ", size " + data.length + ", shift " + Arrays.toString(map.shift()));
        validate(data);
        reconstructed = reconstruct(data, originalData.length, map.shift());
        assertArrayEquals(originalData, reconstructed);
      }
    }
  }

  private void validate(int[] data) {
    int firstTileOffset = data[0];
    int numberOfTiles = firstTileOffset;
//    assertTrue(numberOfTiles < maxLength);

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
          assertTrue(distinctTiles.add(new Tile(tile)));
        }
        else {
          assertEquals(tileSize, data[t] - lastTileOffset);
        }
        lastTileOffset = data[t];
        assertTrue(lastTileOffset <= data.length - tileSize);
        int[] tile = new int[tileSize];
        for (int i = 0; i < tileSize; ++i)
          tile[i] = data[lastTileOffset + i];
        assertTrue(distinctTiles.add(new Tile(tile)));
      }
      else {
        assertTrue(data[t] >= firstTileOffset);
      }
    }
    if (tileSize == 0) {
      tileSize = data.length - firstTileOffset;
      int[] tile = new int[tileSize];
      for (int i = 0; i < tileSize; ++i)
        tile[i] = data[firstTileOffset + i];
      assertTrue(distinctTiles.add(new Tile(tile)));
    }
    int tileIndexBits = -1;
    for (int i = 0; (1 << i) < lastTileOffset + tileSize; ++i)
      if ((1 << i) == tileSize)
        tileIndexBits = i;
    assertTrue(tileIndexBits > 0, "Unexpected tile size: " + tileSize);

    int length = lastTileOffset + tileSize;
    assertEquals(length - firstTileOffset, distinctTiles.size() * tileSize);
  }

  private static class CompressedMapDescriptor {
    int tileIndexBits;
    int length;
    int uncompressedSize;

    public CompressedMapDescriptor(int tileIndexBits, int length, int uncompressedSize) {
      this.tileIndexBits = tileIndexBits;
      this.length = length;
      this.uncompressedSize = uncompressedSize;
    }

  }
  private int[] setupOriginalData(TreeMap<Range, Integer> codeByRange, int end) {
    int[] originalData = new int[end];
    Arrays.fill(originalData, 0);
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

  private int[] reconstruct(int[] data, int uncompressedSize, int...shift) {
    int[] target = new int[uncompressedSize];
    for (int i = 0; i < target.length; ++i) {
      target[i] = CompressedMap.get(data, i, shift);
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
