package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.transform.Map2D.Index;

public interface TileIterator {
  public int next(int[] tiles, int offset);
  public int numberOfTiles();
  public int tileSize();
  public int tileIndexBits();
  public int defaultValue();
  public int end();

  public static TileIterator of(NavigableMap<Range, Integer> codeByRange, int end, int tileIndexBits, int defaultValue) {
    return new TileIterator() {
      int tileSize = 1 << tileIndexBits;
      int numberOfTiles = (end - 1 + tileSize) / tileSize;
      int currentIndex = 0;

      Iterator<Map.Entry<Range, Integer>> it;
      Range currentRange;
      int firstIndex = -1;
      int lastIndex = -1;
      int value;
      {
        if (! codeByRange.isEmpty()) {
          it = codeByRange.entrySet().iterator();
          nextRange();
        }
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
      public int next(int[] target, int offset) {
        if (currentIndex < firstIndex - tileSize)
          return many(target, offset, firstIndex - currentIndex, defaultValue);
        if (currentIndex >= firstIndex && currentIndex <= lastIndex - tileSize) {
          int count = many(target, offset, lastIndex - currentIndex + 1, value);
          if (currentIndex > lastIndex)
            nextRange();
          return count;
        }
        for (int size = 0;; nextRange()) {
          if (firstIndex < 0) {
            if (size != 0) {
              Arrays.fill(target, offset + size, offset + tileSize, defaultValue);
              currentIndex += tileSize - size;
              return 1;
            }
            return many(target, offset, numberOfTiles * tileSize - currentIndex, defaultValue);
          }
          while (currentIndex < firstIndex) {
            ++currentIndex;
            target[offset + size] = defaultValue;
            if (++size == tileSize)
              return 1;
          }
          while (currentIndex <= lastIndex) {
            ++currentIndex;
            target[offset + size] = value;
            if (++size == tileSize) {
              if (currentIndex > lastIndex)
                nextRange();
              return 1;
            }
          }
        }
      }

      private void nextRange() {
        if (! it.hasNext()) {
          firstIndex = -1;
          lastIndex = -1;
        }
        else {
          Map.Entry<Range, Integer> entry = it.next();
          currentRange = entry.getKey();
          firstIndex = currentRange.getFirstCodepoint();
          if (firstIndex >= end) {
            firstIndex = -1;
            lastIndex = -1;
          }
          else {
            value = entry.getValue();
            lastIndex = currentRange.getLastCodepoint();
            if (lastIndex >= end)
              lastIndex = end - 1;
          }
        }
      }

      private int many(int[] target, int offset, int n, int value) {
        Arrays.fill(target, offset, offset + tileSize, value);
        int nt = n / tileSize;
        currentIndex += nt * tileSize;
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
    };
  }

  public static TileIterator of(Map2D map, int tileIndexBits, int defaultValue) {
    return new TileIterator() {
      int tileSize = 1 << tileIndexBits;
      int end = map.getEndX() * map.getEndY();
      int numberOfTiles = (end - 1 + tileSize) / tileSize;
      int currentIndex = 0;

      Iterator<Entry<Index, Integer>> it;
      int index = -1;
      int value;
      {
        if (! map.isEmpty()) {
          it = map.entrySet().iterator();
          nextEntry();
        }
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
      public int next(int[] target, int offset) {
        if (currentIndex < index - tileSize)
          return many(target, offset, index - currentIndex);
        for (int size = 0;; nextEntry()) {
          if (index < 0) {
            if (size != 0) {
              Arrays.fill(target, offset + size, offset + tileSize, defaultValue);
              currentIndex += tileSize - size;
              return 1;
            }
            return many(target, offset, numberOfTiles * tileSize - currentIndex);
          }
          while (currentIndex < index) {
            ++currentIndex;
            target[offset + size] = defaultValue;
            if (++size == tileSize)
              return 1;
          }
          if (currentIndex == index) {
            ++currentIndex;
            target[offset + size] = value;
            if (++size == tileSize) {
              nextEntry();
              return 1;
            }
          }
        }
      }

      private void nextEntry() {
        if (! it.hasNext()) {
          index = -1;
        }
        else {
          Map.Entry<Index, Integer> entry = it.next();
          index = entry.getKey().getX() * map.getEndY() + entry.getKey().getY();
          value = entry.getValue();
        }
      }

      private int many(int[] target, int offset, int n) {
        Arrays.fill(target, offset, offset + tileSize, defaultValue);
        int nt = n / tileSize;
        currentIndex += nt * tileSize;
        return nt;
      }
    };
  }
}
