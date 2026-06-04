// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;

public class Map2D {
  private long[] entries;
  private int size;
  private boolean sorted;
  private final int endX;
  private final int endY;

  public Map2D(int endX, int endY) {
    this.endX = endX;
    this.endY = endY;
    this.entries = new long[16];
    this.size = 0;
    this.sorted = true;
  }

  public void put(int x, int y, int value) {
    if (x >= endX || y >= endY)
      throw new IllegalArgumentException();
    if (size == entries.length)
      entries = Arrays.copyOf(entries, size * 2);
    entries[size++] = ((long) (x * endY + y) << 32) | (value & 0xFFFFFFFFL);
    sorted = false;
  }

  public long[] sortedEntries() {
    if (!sorted) {
      Arrays.sort(entries, 0, size);
      sorted = true;
      for (int i = 1; i < size; ++i) {
        int key = (int) (entries[i] >>> 32);
        if (key == (int) (entries[i - 1] >>> 32))
          throw new IllegalStateException(
              "Duplicate key in Map2D at (" + key / endY + ", " + key % endY + ")");
      }
    }
    return entries;
  }

  public int size() {
    return size;
  }

  public int getEndX() {
    return endX;
  }

  public int getEndY() {
    return endY;
  }
}
