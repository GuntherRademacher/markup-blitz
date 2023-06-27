package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class CompressedMap {
  private int[] tiles;
  private int tileIndexBits;
  private int depth;

  public int tileSize() {
    return 1 << tileIndexBits;
  }

  public int tileIndexBits() {
    return tileIndexBits;
  }

  public int[] tiles() {
    return tiles;
  }

  public CompressedMap(Function<Integer, TileIterator> it, int maxDepth) {
    this(it, maxDepth, 0);
  }

  private CompressedMap(Function<Integer, TileIterator> iteratorSupplier, int maxDepth, int nesting) {
    int bestTileIndexBits = 0;
    int[] bestTiles = null;
    for (int tileIndexBits = 2;; ++tileIndexBits) {
      optimize(iteratorSupplier.apply(tileIndexBits));

//      System.out.println("  optimized to " + tiles.length + " (tileSize " + (1 << tileIndexBits) + ")");
//      System.out.println("                  " + Arrays.toString(tiles));

      if (bestTiles == null || bestTiles.length > tiles.length) {
        bestTiles = tiles;
        bestTileIndexBits = tileIndexBits;
      }
      else {
        break;
      }
    }
    this.tiles = bestTiles;
    this.tileIndexBits = bestTileIndexBits;


    if (nesting + 1 > maxDepth) {

    }
  }

  private void optimize(TileIterator it) {
    int targetIdOffset = 0;
    int tileSize = it.tileSize();
    int numberOfTiles = it.numberOfTiles();
    int targetTileOffset = numberOfTiles;
    tiles = new int[targetTileOffset + tileSize];
    Map<Integer, Integer> distinctTiles = new TreeMap<>(
        (lhs, rhs) -> Arrays.compare(tiles, lhs, lhs + tileSize, tiles, rhs, rhs + tileSize));
    for (int count, sourceAddress = 0;
         (count = it.next(tiles, targetTileOffset)) != 0;
         sourceAddress += count * tileSize) {
      Integer id = distinctTiles.putIfAbsent(targetTileOffset,  targetTileOffset);
      if (id == null) {

// no overlapping
//        if (sourceAddress > 0)
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
    tiles = Arrays.copyOf(tiles, targetTileOffset);
    assert tiles.length == numberOfTiles + distinctTiles.size() * tileSize;
  }

}
