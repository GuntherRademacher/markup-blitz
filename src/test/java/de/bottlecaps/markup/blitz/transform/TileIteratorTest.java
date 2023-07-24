package de.bottlecaps.markup.blitz.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.TestBase;
import de.bottlecaps.markup.blitz.character.Range;
import de.bottlecaps.markup.blitz.character.RangeSet;

public class TileIteratorTest extends TestBase {
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
    NavigableMap<Range, Integer> codeByRange = new TreeMap<>();
    codeByRange.put(new Range(1, 10), 1);
    codeByRange.put(new Range(21, 30), 2);
    codeByRange.put(new Range(31, 40), 3);
    test(codeByRange);
  }

  @Test
  public void testEmptyMapInput() {
    NavigableMap<Range, Integer> codeByRange = Collections.emptyNavigableMap();
    for (int tileIndexBits = 1; tileIndexBits <= 3; ++ tileIndexBits)
        for (int maxCodepoint = 0; maxCodepoint <= 8; ++maxCodepoint) {
          int defaultValue = tileIndexBits * 100 + maxCodepoint;

          TileIterator it = TileIterator.of(codeByRange, maxCodepoint + 1, tileIndexBits, defaultValue);

          int tileSize = 1 << tileIndexBits;
          int[] tile = new int[tileSize];
          int count = it.next(tile, 0);
          assertEquals((maxCodepoint + tileSize) / tileSize, count);
          for (int i = 0; i < tile.length; ++i) {
            assertEquals(defaultValue, tile[i]);
          }
        }
  }

  @Test
  public void testMapInput() {
    int bits = 8;
    for (int bitPattern = 0; bitPattern < 1 << bits; ++bitPattern) {
      List<Range> bitRanges = transformIntegerToBitRanges(bitPattern);

      int[] uncompressedData = new int[bits];
      int defaultValue = 31 * bitPattern + 7;
      Arrays.fill(uncompressedData, defaultValue);

      NavigableMap<Range, Integer> map = new TreeMap<>();
      int rangeId = 0;
      int value = 0;
      for (Range range : bitRanges) {
        ++rangeId;
        map.put(range,  rangeId);
        for (int bit = range.getFirstCodepoint(); bit <= range.getLastCodepoint(); ++bit) {
          value += 1 << bit;
          uncompressedData[bit] = rangeId;
        }
      }
      assertEquals(bitPattern, value);

//      System.out.println(bitRanges + ", " + Arrays.toString(uncompressedData));

      for (int tileIndexBits = 1; tileIndexBits <= log2(bits) + 1; ++ tileIndexBits) {
        int tileSize = 1 << tileIndexBits;
        for (int maxCodepoint = 0; maxCodepoint <= bits + 1; ++maxCodepoint) {
          int numberOfTiles = maxCodepoint / tileSize + 1;

          TileIterator it = TileIterator.of(map, maxCodepoint + 1, tileIndexBits, defaultValue);
          assertEquals(numberOfTiles, it.numberOfTiles());
          assertEquals(tileSize, it.tileSize());
          int[] reconstruction = reconstruct(it);

//          System.out.println(tileIndexBits +
//                       " " + maxCodepoint +
//                       " " + numberOfTiles +
//                       " " + tileSize +
//                       " " + Arrays.toString(reconstruction));

          assertEquals(numberOfTiles * tileSize, reconstruction.length);
          int[] expectedResult = Arrays.copyOf(uncompressedData, maxCodepoint);
          if (expectedResult.length > uncompressedData.length)
            Arrays.fill(expectedResult, uncompressedData.length, expectedResult.length, defaultValue);
          assertArrayEquals(expectedResult, Arrays.copyOf(reconstruction, maxCodepoint));

          test2D(expectedResult, tileIndexBits, defaultValue);
        }
      }
    }
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
    NavigableMap<Range, Integer> codeByRange = new TreeMap<>();
    for (Range range : rangeSet)
      codeByRange.put(range, random.nextInt(classes));
    test(codeByRange);
  }

  private void test(NavigableMap<Range, Integer> codeByRange) {
    int lastCodepoint = codeByRange.descendingKeySet().iterator().next().getLastCodepoint();
    int[] originalData = new int[lastCodepoint + 1];
    int defaultValue = random.nextInt(1);
    Arrays.fill(originalData, defaultValue);
    for (Map.Entry<Range, Integer> e : codeByRange.entrySet()) {
      Range range = e.getKey();
      int code = e.getValue();
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        originalData[codepoint] = code;
    }
    int maxTileIndexBits = log2(lastCodepoint) + 1;
    for (int tileIndexBits = 1; tileIndexBits <= maxTileIndexBits; ++tileIndexBits) {
      TileIterator it = TileIterator.of(codeByRange, lastCodepoint + 1, tileIndexBits, defaultValue);
      int[] reconstructed = reconstruct(it);
      assertArrayEquals(originalData, Arrays.copyOf(reconstructed, originalData.length), () -> msgPrefix);

      it = TileIterator.of(originalData, originalData.length, tileIndexBits, defaultValue);
      reconstructed = reconstruct(it);
      assertArrayEquals(originalData, Arrays.copyOf(reconstructed, originalData.length), () -> msgPrefix);

      test2D(originalData, tileIndexBits, defaultValue);
    }
  }

  private int[] reconstruct(TileIterator it) {
    int tileIndexBits = it.tileIndexBits();
    int tileSize = 1 << tileIndexBits;
    int[] target = new int[tileSize];
    for (int offset = 0;;) {
      if (offset + tileSize > target.length)
        target = Arrays.copyOf(target, target.length << 1);
      int count = it.next(target, offset);
      if (count == 0)
        return Arrays.copyOf(target, offset);
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

  private static List<Integer> primeFactors(int n) {
    List<Integer> primeFactors = new ArrayList<>();
    for (int i = 2; i <= n; i++)
      while (n % i == 0) {
        primeFactors.add(i);
        n /= i;
      }
    return primeFactors;
  }

  private void test2D(int[] data, int bits, int defaultValue) {
    int endX = 1;
    int endY = 1;
    final var primeFactors = primeFactors(data.length);
    Collections.shuffle(primeFactors, random);
    for (int i = 0; i < primeFactors.size(); ++i)
      if (random.nextBoolean())
        endX *= primeFactors.get(i);
      else
        endY *= primeFactors.get(i);
    Map2D map = new Map2D(endX, endY);
    for (int i = 0; i < data.length; ++i)
      if (data[i] != defaultValue) {
        int x = i / endY;
        int y = i - endY * x;
        map.put(new Map2D.Index(x, y), data[i]);
      }
    int[] reconstructed = reconstruct(TileIterator.of(map, bits, defaultValue));
    assertArrayEquals(data, Arrays.copyOf(reconstructed, data.length));
  }
}
