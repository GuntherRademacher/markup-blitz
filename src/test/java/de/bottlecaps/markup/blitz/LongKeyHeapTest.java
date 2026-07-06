// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class LongKeyHeapTest {

  // Semantic ordering of Parser's ParsingThread priority:
  // ascending by e0, ties broken by descending id. Used here as an independent
  // oracle to verify that the primitive key encoding produces this same order.
  private static final Comparator<int[]> COMPARATOR =
      (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]);

  // Priority key encoding, matching Parser.ParsingThread.priority().
  private static long key(int e0, int id) {
    return (long) e0 << 32 | ~id & 0xFFFFFFFFL;
  }

  private static int[] entry(int e0, int id) {
    return new int[] {e0, id};
  }

  // Add an entry under its encoded key and return it.
  private static int[] add(LongKeyHeap<int[]> heap, int e0, int id) {
    int[] e = entry(e0, id);
    heap.add(key(e0, id), e);
    return e;
  }

  @Test
  void emptyHeap() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    assertTrue(heap.isEmpty());
    assertNull(heap.peek());
    assertThrows(NoSuchElementException.class, heap::remove);
  }

  @Test
  void singleElement() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    add(heap, 5, 1);
    assertFalse(heap.isEmpty());
    assertEquals(5, heap.peek()[0]);
    assertEquals(5, heap.remove()[0]);
    assertTrue(heap.isEmpty());
  }

  @Test
  void ordersByE0Ascending() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    add(heap, 30, 1);
    add(heap, 10, 2);
    add(heap, 20, 3);
    assertEquals(10, heap.remove()[0]);
    assertEquals(20, heap.remove()[0]);
    assertEquals(30, heap.remove()[0]);
    assertTrue(heap.isEmpty());
  }

  @Test
  void tieBreakByIdDescending() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    add(heap, 5, 1);
    add(heap, 5, 3);
    add(heap, 5, 2);
    assertEquals(3, heap.remove()[1]);
    assertEquals(2, heap.remove()[1]);
    assertEquals(1, heap.remove()[1]);
  }

  @Test
  void mixedE0AndIdOrdering() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    add(heap, 10, 1);
    add(heap, 5, 2);
    add(heap, 5, 5);
    add(heap, 10, 3);
    // e0=5 entries come first, highest id first; then e0=10
    int[] first = heap.remove();
    assertEquals(5, first[0]);
    assertEquals(5, first[1]);
    int[] second = heap.remove();
    assertEquals(5, second[0]);
    assertEquals(2, second[1]);
    int[] third = heap.remove();
    assertEquals(10, third[0]);
    assertEquals(3, third[1]);
    int[] fourth = heap.remove();
    assertEquals(10, fourth[0]);
    assertEquals(1, fourth[1]);
    assertTrue(heap.isEmpty());
  }

  @Test
  void removeIfEqualRemovesMatchingRoot() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    int[] root = add(heap, 1, 1);
    int[] other = add(heap, 2, 2);
    assertSame(root, heap.removeIfEqual(root));
    assertSame(other, heap.peek());
  }

  @Test
  void removeIfEqualLeavesNonMatchingRoot() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    int[] root = add(heap, 1, 1);
    int[] other = add(heap, 2, 2);
    assertNull(heap.removeIfEqual(other));
    assertSame(root, heap.peek());
  }

  @Test
  void rejectsNonPositiveInitialCapacity() {
    assertThrows(IllegalArgumentException.class, () -> new LongKeyHeap<>(0));
    assertThrows(IllegalArgumentException.class, () -> new LongKeyHeap<>(-1));
    // capacity 1 is the smallest valid value; its grow path (1 << 1 == 2) must work
    LongKeyHeap<int[]> heap = assertDoesNotThrow(() -> new LongKeyHeap<int[]>(1));
    add(heap, 2, 1);
    add(heap, 1, 2);
    assertEquals(1, heap.remove()[0]);
    assertEquals(2, heap.remove()[0]);
  }

  @Test
  void resizesBeyondInitialCapacity() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>(4);
    for (int i = 20; i >= 1; i--)
      add(heap, i, i);
    for (int i = 1; i <= 20; i++)
      assertEquals(i, heap.remove()[0]);
    assertTrue(heap.isEmpty());
  }

  @Test
  void randomizedOrderMatchesSortedBaseline() {
    Random rng = new Random(42);
    int n = 200;
    List<int[]> entries = new ArrayList<>();
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    for (int i = 0; i < n; i++)
      entries.add(add(heap, rng.nextInt(50), i));
    entries.sort(COMPARATOR);
    for (int[] expected : entries) {
      int[] actual = heap.remove();
      assertEquals(expected[0], actual[0], "e0 mismatch");
      assertEquals(expected[1], actual[1], "id mismatch");
    }
    assertTrue(heap.isEmpty());
  }

  @Test
  void interleavedAddRemoveMatchesPriorityQueueOracle() {
    Random rng = new Random(7);
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    PriorityQueue<int[]> oracle = new PriorityQueue<>(COMPARATOR);
    int id = 0;
    for (int step = 0; step < 10000; step++) {
      if (oracle.isEmpty() || rng.nextBoolean()) {
        int[] e = add(heap, rng.nextInt(30), id++);
        oracle.add(e);
      }
      else {
        int[] expected = oracle.poll();
        int[] actual = heap.remove();
        assertEquals(expected[0], actual[0], "e0 mismatch");
        assertEquals(expected[1], actual[1], "id mismatch");
      }
      assertEquals(oracle.isEmpty(), heap.isEmpty());
      if (! oracle.isEmpty())
        assertEquals(oracle.peek()[1], heap.peek()[1], "peek id mismatch");
    }
  }

  @Test
  void peekDoesNotRemove() {
    LongKeyHeap<int[]> heap = new LongKeyHeap<>();
    add(heap, 1, 1);
    add(heap, 2, 2);
    assertEquals(1, heap.peek()[0]);
    assertEquals(1, heap.peek()[0]);
    heap.remove();
    assertEquals(2, heap.peek()[0]);
  }
}
