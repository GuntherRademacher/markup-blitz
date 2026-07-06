// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A min-heap whose entries are ordered by a primitive long key that is passed
 * alongside each entry. Sift operations compare keys from a flat long array,
 * avoiding object dereferences during heap maintenance.
 */
class LongKeyHeap<T> {
  private long[] keys;
  private Object[] entries;
  private int size = 0;

  LongKeyHeap() {
    this(16);
  }

  LongKeyHeap(int initialCapacity) {
    if (initialCapacity < 1)
      throw new IllegalArgumentException("initialCapacity: " + initialCapacity);
    keys = new long[initialCapacity];
    entries = new Object[initialCapacity];
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void add(long key, T entry) {
    int index = size++;
    if (index == keys.length) {
      keys = Arrays.copyOf(keys, keys.length << 1);
      entries = Arrays.copyOf(entries, entries.length << 1);
    }
    while (index != 0) {
      int parentIndex = (index - 1) >> 1;
      long parentKey = keys[parentIndex];
      if (key >= parentKey)
        break;
      keys[index] = parentKey;
      entries[index] = entries[parentIndex];
      index = parentIndex;
    }
    keys[index] = key;
    entries[index] = entry;
  }

  public T peek() {
    return size == 0 ? null : entry(0);
  }

  public T remove() {
    if (size == 0)
      throw new NoSuchElementException();
    return removeRoot();
  }

  public T removeIfEqual(T entry) {
    if (size == 0 || ! Objects.equals(entry, entries[0]))
      return null;
    return removeRoot();
  }

  private T removeRoot() {
    T min = entry(0);
    --size;
    long key = keys[size];
    Object entry = entries[size];
    entries[size] = null;
    if (size != 0) {
      int index = 0;
      int half = size >>> 1;
      while (index < half) {
        int childIndex = (index << 1) + 1;
        long childKey = keys[childIndex];
        int rightIndex = childIndex + 1;
        if (rightIndex < size && keys[rightIndex] < childKey) {
          childIndex = rightIndex;
          childKey = keys[childIndex];
        }
        if (childKey >= key)
          break;
        keys[index] = childKey;
        entries[index] = entries[childIndex];
        index = childIndex;
      }
      keys[index] = key;
      entries[index] = entry;
    }
    return min;
  }

  @SuppressWarnings("unchecked")
  private T entry(int index) {
    return (T) entries[index];
  }
}
