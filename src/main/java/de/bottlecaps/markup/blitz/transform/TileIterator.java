package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;

import de.bottlecaps.markup.blitz.character.Range;

public interface TileIterator {
  public int next(int[] tiles, int offset);
  public int numberOfTiles();
  public int tileSize();
  public int defaultValue();

  public static TileIterator of(NavigableMap<Range, Integer> terminalCodeByRange, int maxCodepoint, int tileIndexBits, int defaultValue) {
    return new TileIterator() {
      int tileSize = 1 << tileIndexBits;
      int numberOfTiles;

      Iterator<Map.Entry<Range, Integer>> it;
      int currentCp;

      Range currentRange;
      int firstCp;
      int lastCp;
      int tokenCode;

      {
        if (terminalCodeByRange.isEmpty()) {
          firstCp = -1;
          lastCp = -1;
        }
        else {
          it = terminalCodeByRange.entrySet().iterator();
          nextRange();
        }
        numberOfTiles = (maxCodepoint + tileSize) / tileSize;
        currentCp = 0;
      }

      @Override
      public int numberOfTiles() {
        return numberOfTiles;
      }

      @Override
      public int tileSize() {
        return tileSize;
      }

      @Override
      public int defaultValue() {
        return defaultValue;
      }

      @Override
      public int next(int[] tiles, int offset) {
        if (currentCp < firstCp - tileSize)
          return many(tiles, offset, firstCp - currentCp, defaultValue);
        if (currentCp >= firstCp && currentCp <= lastCp - tileSize) {
          int count = many(tiles, offset, lastCp - currentCp + 1, tokenCode);
          if (currentCp > lastCp)
            nextRange();
          return count;
        }
        for (int size = 0;; nextRange()) {
          if (firstCp < 0) {
            if (size != 0) {
              Arrays.fill(tiles, offset + size, offset + tileSize, defaultValue);
              currentCp += tileSize - size;
              return 1;
            }
            return many(tiles, offset, numberOfTiles * tileSize - currentCp, defaultValue);
          }
          while (currentCp < firstCp) {
            ++currentCp;
            tiles[offset + size++] = defaultValue;
            if (size == tileSize)
              return 1;
          }
          while (currentCp <= lastCp) {
            ++currentCp;
            tiles[offset + size++] = tokenCode;
            if (size == tileSize) {
              if (currentCp > lastCp)
                nextRange();
              return 1;
            }
          }
        }
      }

      private void nextRange() {
        if (! it.hasNext()) {
          firstCp = -1;
          lastCp = -1;
        }
        else {
          Map.Entry<Range, Integer> entry = it.next();
          currentRange = entry.getKey();
          firstCp = currentRange.getFirstCodepoint();
          if (firstCp > maxCodepoint) {
            firstCp = -1;
            lastCp = -1;
          }
          else {
            tokenCode = entry.getValue();
            lastCp = currentRange.getLastCodepoint();
            if (lastCp > maxCodepoint)
              lastCp = maxCodepoint;
          }
        }
      }

      private int many(int[] target, int offset, int n, int value) {
        Arrays.fill(target, offset, offset + tileSize, value);
        int nt = n / tileSize;
        currentCp += nt * tileSize;
        return nt;
      }
    };
  }
}
