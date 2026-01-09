// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class CompressedMap {
  private int[] data;
  private int[] shift;

  public int[] data() {
    return data;
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
      create(iteratorSupplier.apply(tileIndexBits), maxDepth, isNested);
      if (bestTiles != null && bestTiles.length <= data.length)
        break;
      bestTiles = data;
      bestShift = shift;
    }
    this.data = bestTiles;
    this.shift = bestShift;
  }

  private void create(TileIterator it, int maxDepth, boolean isNested) {
    int tileSize = it.tileSize();
    int numberOfTiles = it.numberOfTiles();
    int end = numberOfTiles;
    int idOffset = 0;
    data = new int[(end + tileSize) + 1];

    Comparator<Integer> indexComparator = (lhs, rhs) ->
      Arrays.compare(data, lhs, lhs + tileSize, data, rhs, rhs + tileSize);
    Map<Integer, Integer> distinctTiles = new TreeMap<>(indexComparator);

    for (int count; (count = it.next(data, end)) != 0; ) {
      Integer id = distinctTiles.putIfAbsent(end,  end);
      if (id == null) {
// no tile overlapping - advantage would be marginal
//        if (end > numberOfTiles)
//          for (int i = end - tileSize + 1; i < end; ++i)
//            distinctTiles.putIfAbsent(i, i);
        id = end;
        end += tileSize;
        if (end + tileSize > data.length)
          data = Arrays.copyOf(data, data.length << 1);
      }
      for (int i = 0; i < count; ++i) //  && idOffset < numberOfTiles; ++i)
        data[idOffset++] = id;
    }

    distinctTiles = null;
    int distinctTileSize = end - numberOfTiles;
    shift = new int[] {it.tileIndexBits()};

    if (maxDepth > 1) {
      Function<Integer, TileIterator> indexIterator = bits -> TileIterator.of(data, numberOfTiles, bits, 0);
      CompressedMap nestedMap = new CompressedMap(indexIterator, maxDepth - 1, true);
      if (nestedMap.data().length <= numberOfTiles >> 1) {
        shift = Arrays.copyOf(shift, nestedMap.shift().length + 1);
        System.arraycopy(nestedMap.shift(), 0, shift, 1, nestedMap.shift().length);
        System.arraycopy(nestedMap.data(), 0, data, 0, nestedMap.data().length);
        System.arraycopy(data, numberOfTiles, data, nestedMap.data().length, distinctTileSize);
        end = nestedMap.data().length + distinctTileSize;
      }
    }

    if (isNested) {
      int displacement = end - it.end();
      for (int i = end - distinctTileSize; i < end; ++i)
        data[i] += displacement;
    }

    data = Arrays.copyOf(data, end);
  }

  public int get(int i0) {
    switch (shift.length) {
    case 1: {
        return data[(i0 & (1 << shift[0]) - 1) + data[i0 >> shift[0]]];
      }
    case 2: {
        int i1 = i0 >> shift[0];
        return data[(i0 & (1 << shift[0]) - 1)
             + data[(i1 & (1 << shift[1]) - 1) + data[i1 >> shift[1]]]];
      }
    case 3: {
        int i1 = i0 >> shift[0];
        int i2 = i1 >> shift[1];
        return data[(i0 & (1 << shift[0]) - 1)
             + data[(i1 & (1 << shift[1]) - 1)
             + data[(i2 & (1 << shift[2]) - 1) + data[i2 >> shift[2]]]]];
      }
    case 4: {
        int i1 = i0 >> shift[0];
        int i2 = i1 >> shift[1];
        int i3 = i2 >> shift[2];
        return data[(i0 & (1 << shift[0]) - 1)
             + data[(i1 & (1 << shift[1]) - 1)
             + data[(i2 & (1 << shift[2]) - 1)
             + data[(i3 & (1 << shift[3]) - 1) + data[i3 >> shift[3]]]]]];
      }
    case 5: {
        int i1 = i0 >> shift[0];
        int i2 = i1 >> shift[1];
        int i3 = i2 >> shift[2];
        int i4 = i3 >> shift[3];
        return data[(i0 & (1 << shift[0]) - 1)
             + data[(i1 & (1 << shift[1]) - 1)
             + data[(i2 & (1 << shift[2]) - 1)
             + data[(i3 & (1 << shift[3]) - 1)
             + data[(i4 & (1 << shift[4]) - 1) + data[i4 >> shift[4]]]]]]];
      }
    case 6: {
        int i1 = i0 >> shift[0];
        int i2 = i1 >> shift[1];
        int i3 = i2 >> shift[2];
        int i4 = i3 >> shift[3];
        int i5 = i4 >> shift[4];
        return data[(i0 & (1 << shift[0]) - 1)
             + data[(i1 & (1 << shift[1]) - 1)
             + data[(i2 & (1 << shift[2]) - 1)
             + data[(i3 & (1 << shift[3]) - 1)
             + data[(i4 & (1 << shift[4]) - 1)
             + data[(i5 & (1 << shift[5]) - 1) + data[i5 >> shift[5]]]]]]]];
      }
    default: {
        final int length = shift.length;
        int[] index = new int[length];
        index[0] = i0;
        for (int i = 1; i < length; ++i) {
          index[i] = index[i - 1] >> shift[i - 1];
        }
        int value = data[index[length - 1] >> shift[length - 1]];
        for (int i = length - 1; i >= 0; --i)
          value = data[value + (index[i] & (1 << shift[i]) - 1)];
        return value;
      }
    }
  }

}
