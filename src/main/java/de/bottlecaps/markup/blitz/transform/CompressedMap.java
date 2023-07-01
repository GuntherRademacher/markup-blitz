package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class CompressedMap {
  private int[] tiles;
  private int[] shift;

  public int[] tiles() {
    return tiles;
  }

  public int[] shift() {
    return shift;
  }

  public CompressedMap(Function<Integer, TileIterator> iteratorSupplier, int maxDepth) {
    this(iteratorSupplier, maxDepth, false);
  }

  private CompressedMap(Function<Integer, TileIterator> iteratorSupplier, int maxDepth, boolean isNested) {
    int[] bestShift = null;
    int[] bestTiles = null;

    for (int tileIndexBits = 2;; ++tileIndexBits) {
      optimize(iteratorSupplier.apply(tileIndexBits), maxDepth, isNested);

//      System.out.println("  optimized to " + tiles.length + " (tileSize " + (1 << tileIndexBits) + ")");
//      System.out.println("                  " + Arrays.toString(tiles));

      if (bestTiles == null || bestTiles.length > tiles.length) {
        bestTiles = tiles;
        bestShift = shift;
      }
      else {
        break;
      }
    }
    this.tiles = bestTiles;
    this.shift = bestShift;
  }

  private void optimize(TileIterator it, int maxDepth, boolean isNested) {
    int tileSize = it.tileSize();
    int numberOfTiles = it.numberOfTiles();
    int end = numberOfTiles;
    int idOffset = 0;
    tiles = new int[end + tileSize];

    Map<Integer, Integer> distinctTiles = new TreeMap<>(
        (lhs, rhs) -> Arrays.compare(tiles, lhs, lhs + tileSize, tiles, rhs, rhs + tileSize));

    for (int count; (count = it.next(tiles, end)) != 0; ) {
      Integer id = distinctTiles.putIfAbsent(end,  end);
      if (id == null) {

// no overlapping
//        if (targetTileOffset > numberOfTiles)
//          for (int i = end - tileSize + 1; i < end; ++i)
//            distinctTiles.putIfAbsent(i, i);

        id = end;
        end += tileSize;
        if (end + tileSize > tiles.length)
          tiles = Arrays.copyOf(tiles, tiles.length << 1);
      }
      for (int i = 0; i < count && idOffset < numberOfTiles; ++i)
        tiles[idOffset++] = id;
    }

    distinctTiles = null;
    int distinctTileSize = end - numberOfTiles;
    shift = new int[] {it.tileIndexBits()};

    if (maxDepth > 1) {
      Function<Integer, TileIterator> indexIterator = bits -> TileIterator.of(tiles, numberOfTiles, bits, 0);
      CompressedMap nestedMap = new CompressedMap(indexIterator, maxDepth - 1, true);
      if (nestedMap.tiles().length <= numberOfTiles >> 1) {
        shift = Arrays.copyOf(shift, nestedMap.shift().length + 1);
        System.arraycopy(nestedMap.shift(), 0, shift, 1, nestedMap.shift().length);
        System.arraycopy(nestedMap.tiles(), 0, tiles, 0, nestedMap.tiles().length);
        System.arraycopy(tiles, numberOfTiles, tiles, nestedMap.tiles().length, distinctTileSize);
        end = nestedMap.tiles().length + distinctTileSize;
      }
    }

    if (isNested) {
      int displacement = end - it.end();
      for (int i = end - distinctTileSize; i < end; ++i)
        tiles[i] += displacement;
    }

    tiles = Arrays.copyOf(tiles, end);
  }

  public static int get1(int[] map, int i0, int shift0) {
    int mask = (1 << shift0) - 1;
    return map[(i0 & mask)
         + map[i0 >> shift0]];
  }

  public static int get2(int[] map, int i0, int shift0, int shift1) {
    int i1 = i0 >> shift0;
    return map[(i0 & (1 << shift0) - 1)
         + map[(i1 & (1 << shift1) - 1)
         + map[i1 >> shift1]]];
  }

  public static int get3(int[] map, int i0, int shift0, int shift1, int shift2) {
    int i1 = i0 >> shift0;
    int i2 = i1 >> shift2;
    return map[(i0 & (1 << shift0) - 1)
         + map[(i1 & (1 << shift1) - 1)
         + map[(i2 & (1 << shift2) - 1) + map[i2 >> shift2]]]];
  }

  public static int get(int[] map, int i0, int... shift) {
    final int length = shift.length;
    int[] mask = new int[length];
    int[] index = new int[length];
    mask[0] = (1 << shift[0]) - 1;
    index[0] = i0;
    for (int i = 1; i < length; ++i) {
      mask[i] = (1 << shift[i]) - 1;
      index[i] = index[i - 1] >> shift[i - 1];
    }
    int value = map[index[length - 1] >> shift[length - 1]];
    for (int i = length - 1; i >= 0; --i)
      value = map[(index[i] & mask[i]) + value];
    return value;
  }
}
