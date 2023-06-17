package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CompressedMap {
  public static final int END = 0xD800;

  private int tileSize;
  private int numberOfTiles;
  private int[] tiles;

  public CompressedMap(int tileSize) {
    this.tileSize = tileSize;
  }

  public int tileSize() {
    return tileSize;
  }

  public int numberOfTiles() {
    return numberOfTiles;
  }

  public int[] process(TileIterator it) {
    numberOfTiles = Math.min(it.numberOfTiles(), (END + tileSize - 1) / tileSize);
    Map<Tile, Integer> distinctTiles = new HashMap<>();
    int targetPointerOffset = 0;
    int targetTileOffset = numberOfTiles;
    tiles = new int[targetTileOffset + tileSize];
    int count, sourceAddress = 0;
    for (;
         sourceAddress < END && (count = it.next(tiles, targetTileOffset)) != 0;
         sourceAddress += count * tileSize) {
      Integer id = distinctTiles.putIfAbsent(new Tile(targetTileOffset),  targetTileOffset);
      if (id == null) {
//        if (sourceAddress > 0)
//          for (int i = targetTileOffset - tileSize + 1; i < targetTileOffset; ++i)
//            distinctTiles.putIfAbsent(new Tile(i), i);
        id = targetTileOffset;
        targetTileOffset += tileSize;
        if (targetTileOffset + tileSize > tiles.length)
          tiles = Arrays.copyOf(tiles, tiles.length << 1);
      }
      for (int i = 0; i < count && targetPointerOffset < numberOfTiles; ++i)
        tiles[targetPointerOffset++] = id;
    }
    return Arrays.copyOf(tiles, targetTileOffset);
  }

  private class Tile {
    private int offset;
    private int hashCode;

    public Tile(int offset) {
      this.offset = offset;
      final int prime = 31;
      int hashCode = 1;
      for (int i = 0; i < tileSize; ++i)
        hashCode = prime * hashCode + tiles[offset + i];;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      Tile other = (Tile) obj;
      return Arrays.equals(tiles,       offset,       offset + tileSize,
                           tiles, other.offset, other.offset + tileSize);
    }
  }
}
