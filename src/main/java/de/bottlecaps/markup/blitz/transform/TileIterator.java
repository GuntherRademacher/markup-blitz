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
  public int tileIndexBits();
  public int defaultValue();
  public int end();

  public static TileIterator of(NavigableMap<Range, Integer> terminalCodeByRange, int end, int tileIndexBits, int defaultValue) {
    return new TileIterator() {
      int tileSize = 1 << tileIndexBits;
      int numberOfTiles;

      Iterator<Map.Entry<Range, Integer>> it;
      int currentValue;

      Range currentRange;
      int firstValue;
      int lastValue;
      int tokenCode;

      {
        if (terminalCodeByRange.isEmpty()) {
          firstValue = -1;
          lastValue = -1;
        }
        else {
          it = terminalCodeByRange.entrySet().iterator();
          nextRange();
        }
        numberOfTiles = (end - 1 + tileSize) / tileSize;
        currentValue = 0;
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
      public int tileIndexBits() {
        return tileIndexBits;
      }

      @Override
      public int defaultValue() {
        return defaultValue;
      }

      @Override
      public int end() {
        return end;
      }

      @Override
      public int next(int[] tiles, int offset) {
        if (currentValue < firstValue - tileSize)
          return many(tiles, offset, firstValue - currentValue, defaultValue);
        if (currentValue >= firstValue && currentValue <= lastValue - tileSize) {
          int count = many(tiles, offset, lastValue - currentValue + 1, tokenCode);
          if (currentValue > lastValue)
            nextRange();
          return count;
        }
        for (int size = 0;; nextRange()) {
          if (firstValue < 0) {
            if (size != 0) {
              Arrays.fill(tiles, offset + size, offset + tileSize, defaultValue);
              currentValue += tileSize - size;
              return 1;
            }
            return many(tiles, offset, numberOfTiles * tileSize - currentValue, defaultValue);
          }
          while (currentValue < firstValue) {
            ++currentValue;
            tiles[offset + size++] = defaultValue;
            if (size == tileSize)
              return 1;
          }
          while (currentValue <= lastValue) {
            ++currentValue;
            tiles[offset + size++] = tokenCode;
            if (size == tileSize) {
              if (currentValue > lastValue)
                nextRange();
              return 1;
            }
          }
        }
      }

      private void nextRange() {
        if (! it.hasNext()) {
          firstValue = -1;
          lastValue = -1;
        }
        else {
          Map.Entry<Range, Integer> entry = it.next();
          currentRange = entry.getKey();
          firstValue = currentRange.getFirstCodepoint();
          if (firstValue >= end) {
            firstValue = -1;
            lastValue = -1;
          }
          else {
            tokenCode = entry.getValue();
            lastValue = currentRange.getLastCodepoint();
            if (lastValue >= end)
              lastValue = end - 1;
          }
        }
      }

      private int many(int[] target, int offset, int n, int value) {
        Arrays.fill(target, offset, offset + tileSize, value);
        int nt = n / tileSize;
        currentValue += nt * tileSize;
        return nt;
      }
    };
  }

  public static TileIterator of(int[] array, int end, int tileIndexBits, int defaultValue) {
    return new TileIterator() {
      int tileSize = 1 << tileIndexBits;
      int numberOfTiles = (end - 1 + tileSize) / tileSize;
      int nextOffset = 0;

      @Override
      public int next(int[] target, int targetOffset) {
        int remainingSize = end - nextOffset;
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
        while (end - nextOffset >= tileSize
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

      @Override
      public int tileIndexBits() {
        return tileIndexBits;
      }

      @Override
      public int defaultValue() {
        return defaultValue;
      }

      @Override
      public int end() {
        return end;
      }
    };
  }

}
