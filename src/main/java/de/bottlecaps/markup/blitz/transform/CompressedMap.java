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
    int[] bestShift = null;
    int[] bestTiles = null;

    for (int tileIndexBits = 2;; ++tileIndexBits) {
      optimize(iteratorSupplier.apply(tileIndexBits), maxDepth);

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

  private void optimize(TileIterator it, int maxDepth) {
    int targetIdOffset = 0;
    int tileSize = it.tileSize();
    int numberOfTiles = it.numberOfTiles();
    int targetTileOffset = numberOfTiles;
    tiles = new int[targetTileOffset + tileSize];
    Map<Integer, Integer> distinctTiles = new TreeMap<>(
        (lhs, rhs) -> Arrays.compare(tiles, lhs, lhs + tileSize, tiles, rhs, rhs + tileSize));
    for (int count;
         (count = it.next(tiles, targetTileOffset)) != 0;
        ) {
      Integer id = distinctTiles.putIfAbsent(targetTileOffset,  targetTileOffset);
      if (id == null) {

// no overlapping
//        if (targetTileOffset > numberOfTiles)
//          for (int i = targetTileOffset - tileSize + 1; i < targetTileOffset; ++i)
//            distinctTiles.putIfAbsent(i, i);

        id = targetTileOffset;
        targetTileOffset += tileSize;
        if (targetTileOffset + tileSize > tiles.length)
          tiles = Arrays.copyOf(tiles, tiles.length << 1);
      }
      for (int i = 0; i < count && targetIdOffset < numberOfTiles; ++i)
        tiles[targetIdOffset++] = id;
    }

    int distinctTileSize = targetTileOffset - numberOfTiles;
    assert distinctTileSize == distinctTiles.size() * tileSize;
    distinctTiles = null;

    if (maxDepth > 1) {
      Function<Integer, TileIterator> indexIterator = bits -> TileIterator.of(tiles, numberOfTiles, bits, 0);
      CompressedMap nestedMap = new CompressedMap(indexIterator, maxDepth - 1);
      int[] nestedTiles = nestedMap.tiles();
      if (nestedTiles.length <= numberOfTiles >> 1) {
        int[] combinedTiles = new int[nestedTiles.length + distinctTileSize];
        System.arraycopy(nestedMap.tiles(), 0, combinedTiles, 0, nestedTiles[0]);
        int displacement = nestedTiles.length - tiles[0];
        for (int i = nestedTiles[0]; i < nestedMap.tiles().length; ++i)
          combinedTiles[i] = nestedMap.tiles()[i] + displacement;
        System.arraycopy(nestedMap.tiles(), 0, combinedTiles, 0, nestedTiles[0]);
        System.arraycopy(tiles, numberOfTiles, combinedTiles, nestedMap.tiles().length, distinctTileSize);

        shift = new int[nestedMap.shift().length + 1];
        shift[0] = it.tileIndexBits();
        System.arraycopy(nestedMap.shift(), 0, shift, 1, nestedMap.shift().length);

        System.out.println("shift[" + shift.length + "]: " + Arrays.toString(shift));
        System.out.println("ti[" + tiles.length + "]: " + Arrays.toString(tiles));
        System.out.println("nt[" + nestedTiles.length + "]: " + Arrays.toString(nestedTiles));
        System.out.println("ct[" + combinedTiles.length + "]: " + Arrays.toString(combinedTiles));

        for (int i = 0; i < combinedTiles.length; ++i) {
          if (combinedTiles[i] < 0)
            assert false;
        }

//TODO: remove 1-level verification code
//        for (int i = 0; i < numberOfTiles * tileSize; ++i) {
//          int v0 = get(tiles, i, it.tileIndexBits());
//          int v1 = getV(combinedTiles, i, shift);
//          if (v0 != v1)
//            throw new IllegalStateException();
//          if (v0 != getV(tiles, i, it.tileIndexBits()))
//            throw new IllegalStateException();
//        }

        tiles = combinedTiles;
        return;
      }
    }

    tiles = Arrays.copyOf(tiles, targetTileOffset);
    shift = new int[] {it.tileIndexBits()};
  }

//  public static int get(int[] map, int i0, int shift0) {
//    int mask = (1 << shift0) - 1;
//    return map[(i0 & mask)
//         + map[i0 >> shift0]];
//  }
//
//  public static int get(int[] map, int i0, int shift0, int shift1) {
//    int i1 = i0 >> shift0;
//    return map[(i0 & (1 << shift0) - 1)
//         + map[(i1 & (1 << shift1) - 1)
//         + map[i1 >> shift1]]];
//  }
//
//  public static int get(int[] map, int i0, int shift0, int shift1, int shift2) {
//    int i1 = i0 >> shift0;
//    int i2 = i1 >> shift2;
//    return map[(i0 & (1 << shift0) - 1)
//         + map[(i1 & (1 << shift1) - 1)
//         + map[(i2 & (1 << shift2) - 1) + map[i2 >> shift2]]]];
//  }

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
