package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import de.bottlecaps.markup.blitz.character.Range;

public interface TileIterator {
  public int next(int[] target, int offset);
  public int numberOfTiles();
  public int tileSize();

  public static TileIterator of(TreeMap<Range, Integer> terminalCodeByRange, int log2OfTileSize) {
    return new TileIterator() {
      int defaultValue = 0;
      int tileSize = 1 << log2OfTileSize;
      int numberOfTiles = (terminalCodeByRange.descendingKeySet().iterator().next().getLastCodepoint() + tileSize)
                        / tileSize;

      Iterator<Map.Entry<Range, Integer>> it = terminalCodeByRange.entrySet().iterator();
      int currentCp = 0;

      Range currentRange;
      int firstCp;
      int lastCp;
      int tokenCode;

      {
        nextRange();
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
      public int next(int[] target, int offset) {
        if (currentCp < firstCp - tileSize) {
          return many(target, offset, firstCp - currentCp, defaultValue);
        }
        else if (currentCp >= firstCp && currentCp <= lastCp - tileSize) {
          int result = many(target, offset, lastCp - currentCp + 1, tokenCode);
          if (currentCp > lastCp)
            nextRange();
          return result;
        }
        for (int size = 0;; nextRange()) {
          if (firstCp < 0) {
            if (size == 0)
              return 0;
            Arrays.fill(target, offset + size, offset + tileSize, defaultValue);
            return 1;
          }
          while (currentCp < firstCp) {
            ++currentCp;
            target[offset + size++] = defaultValue;
            if (size == tileSize)
              return 1;
          }
          while (currentCp <= lastCp) {
            ++currentCp;
            target[offset + size++] = tokenCode;
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
          return;
        }
        Map.Entry<Range, Integer> entry = it.next();
        tokenCode = entry.getValue();
        currentRange = entry.getKey();
        firstCp = currentRange.getFirstCodepoint();
        lastCp = currentRange.getLastCodepoint();
      }

      private int many(int[] target, int offset, int n, int value) {
        for (int i = 0; i < tileSize; ++i)
          target[offset + i] = value;
        int nt = n / tileSize;
        currentCp += nt * tileSize;
        return nt;
      }
    };
  }

  public static TileIterator of(int[] array, int log2OfTileSize) {
    return new TileIterator() {
      int defaultValue = 0;
      int tileSize = 1 << log2OfTileSize;
      int numberOfTiles = (array.length + tileSize - 1) / tileSize;
      int nextOffset = 0;

      @Override
      public int next(int[] target, int targetOffset) {
        int remainingSize = array.length - nextOffset;
        if (remainingSize <= 0)
          return 0;
        if (remainingSize < tileSize) {
          System.arraycopy(array, nextOffset, target, targetOffset, remainingSize);
          Arrays.fill(target, targetOffset + remainingSize, targetOffset + tileSize, defaultValue);
          nextOffset += remainingSize;
          return 1;
        }
        System.arraycopy(array, nextOffset, target, targetOffset, tileSize);
        int count = 1;
        nextOffset += tileSize;
        while (array.length - nextOffset >= tileSize
            && 0 == Arrays.compare(
              target, targetOffset, targetOffset + tileSize,
              array, nextOffset, nextOffset + tileSize)) {
          ++count;
          nextOffset += tileSize;
        }
        return count;
      }

      @Override
      public int numberOfTiles() {
        return numberOfTiles;
      }

      @Override
      public int tileSize() {
        return tileSize;
      }
    };
  }
}
