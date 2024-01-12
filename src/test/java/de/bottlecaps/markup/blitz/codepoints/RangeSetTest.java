// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.codepoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.bottlecaps.markup.TestBase;

public class RangeSetTest extends TestBase {

  @Test
  public void testRangeSet() {
    {
      RangeSet set = RangeSet.of(
        new Range(4, 5),
        new Range(1, 2));
      assertEquals("[#1-#2; #4-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 2),
        new Range(4, 5));
      assertEquals("[#1-#2; #4-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 3),
        new Range(4, 5));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(4, 5),
        new Range(1, 3));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 3),
        new Range(3, 5));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(3, 5),
        new Range(1, 3));
      assertEquals("[#1-#5]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 2),
        new Range(3, 4));
      assertEquals("[#1-#4]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(3, 4),
        new Range(1, 2));
      assertEquals("[#1-#4]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(1, 10),
        new Range(2, 3));
      assertEquals("[#1-#a]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range(2, 3),
        new Range(1, 10));
      assertEquals("[#1-#a]", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z'),
        new Range('c', 'c'),
        new Range('n', 'q'),
        new Range('p', 't'));
      assertEquals("['a'-'z']", set.toString());
    }
    {
      RangeSet set = RangeSet.of(
        new Range('p', 't'),
        new Range('c', 'c'),
        new Range('n', 'q'),
        new Range('a', 'z'));
      assertEquals("['a'-'z']", set.toString());
    }
  }

  @Test
  public void testMinusLetters() {
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z')
      )
      .minus(RangeSet.of(new Range('k'), new Range('p', 'r')));
      assertEquals("['a'-'j'; 'l'-'o'; 's'-'z']", set.toString());
    }
  }

  @Test
  public void testComplementLetters() {
    {
      RangeSet set = RangeSet.of(
        new Range('a', 'z')
      )
      .complement();
      RangeSet complement = RangeSet.builder().add(0x0, '`').add('{', 0xd7ff).add(0xe000, 0xfdcf).add(0xfdf0, 0xfffd).add(0x10000, 0x1fffd).add(0x20000, 0x2fffd).add(0x30000, 0x3fffd).add(0x40000, 0x4fffd).add(0x50000, 0x5fffd).add(0x60000, 0x6fffd).add(0x70000, 0x7fffd).add(0x80000, 0x8fffd).add(0x90000, 0x9fffd).add(0xa0000, 0xafffd).add(0xb0000, 0xbfffd).add(0xc0000, 0xcfffd).add(0xd0000, 0xdfffd).add(0xe0000, 0xefffd).add(0xf0000, 0xffffd).add(0x100000, 0x10fffd).build();
      assertEquals(complement, set);
      assertEquals("['a'-'z']", set.complement().toString());
    }
  }

  @Test
  public void testRandomInsertions() {
    Random random = new Random();
    long seed = random.nextLong();
    random.setSeed(seed);
    String msgPrefix = "With seed " + seed + "L: ";
    for (int iteration = 0; iteration < 128; ++iteration) {
      int numberOfRanges = 1 + random.nextInt(16); // .nextInt(100);
      List<Range> ranges = new ArrayList<>();
      for (int r = 0; r < numberOfRanges; ++r) {
        int f = ' ' + random.nextInt(96);
        int l = f + random.nextInt(128 - f);
        Range range = new Range(f, l);
        ranges.add(range);
      }
      String string = null;
      String complementString = null;
      for (int permutation = 0; permutation < 64; ++permutation) {
        RangeSet.Builder builder = RangeSet.builder();
        Collections.shuffle(ranges);
        for (Range range : ranges)
          builder.add(range);
        RangeSet set = builder.build();
        RangeSet complement = set.complement();
        if (string == null) {
          string = set.toString();
          complementString = complement.toString();
        }
        else {
          assertEquals(string, set.toString(), msgPrefix + "unexpected result");
          assertEquals(complementString, complement.toString(), msgPrefix + "unexpected result");
        }
      }
    }
  }

  @Test
  public void testAlphabet() {
    String stringOfAlphabet = "[#0-#d7ff; #e000-#fdcf; #fdf0-#fffd; #10000-#1fffd; #20000-#2fffd; #30000-#3fffd; #40000-#4fffd; #50000-#5fffd; #60000-#6fffd; #70000-#7fffd; #80000-#8fffd; #90000-#9fffd; #a0000-#afffd; #b0000-#bfffd; #c0000-#cfffd; #d0000-#dfffd; #e0000-#efffd; #f0000-#ffffd; #100000-#10fffd]";
    assertEquals(stringOfAlphabet, UnicodeCategory.ALPHABET.toString());
  }

  @Test
  public void testIntersect() {
    final int bits = 8;
    for (int i = 0; i < 1 << bits; ++i) {
      RangeSet lhs = RangeSet.builder().add(transformIntegerToBitRanges(i)).build();
      for (int j = 0; j < 1 << bits; ++j) {
        RangeSet expected = RangeSet.builder().add(transformIntegerToBitRanges(i & j)).build();
        RangeSet rhs = RangeSet.builder().add(transformIntegerToBitRanges(j)).build();
        RangeSet intersection = lhs.intersection(rhs);
        assertEquals(expected , intersection, lhs + " intersect " + rhs + " returns " + intersection);
      }
    }
  }

  @Test
  public void testUnion() {
    final int bits = 8;
    for (int i = 0; i < 1 << bits; ++i) {
      RangeSet lhs = RangeSet.builder().add(transformIntegerToBitRanges(i)).build();
      for (int j = 0; j < 1 << bits; ++j) {
        RangeSet expected = RangeSet.builder().add(transformIntegerToBitRanges(i | j)).build();
        RangeSet rhs = RangeSet.builder().add(transformIntegerToBitRanges(j)).build();
        RangeSet union = lhs.union(rhs);
        assertEquals(expected , union, lhs + " union " + rhs + " returns " + union);
      }
    }
  }

  @Test
  public void testMinus() {
    final int bits = 8;
    for (int i = 0; i < 1 << bits; ++i) {
      RangeSet lhs = RangeSet.builder().add(transformIntegerToBitRanges(i)).build();
      for (int j = 0; j < 1 << bits; ++j) {
        RangeSet expected1 = RangeSet.builder().add(transformIntegerToBitRanges(i & ~j)).build();
        RangeSet rhs = RangeSet.builder().add(transformIntegerToBitRanges(j)).build();
        RangeSet minus = lhs.minus(rhs);
        assertEquals(expected1 , minus, lhs + " minus " + rhs + " returns " + minus);
      }
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource
  public void testContainsCodepoint(String name, RangeSet rangeSet) {
    for (Range range : rangeSet)
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        assertTrue(rangeSet.containsCodepoint(codepoint));
    RangeSet complement = rangeSet.complement();
    for (Range range : rangeSet)
      for (int codepoint = range.getFirstCodepoint(); codepoint <= range.getLastCodepoint(); ++codepoint)
        assertFalse(complement.containsCodepoint(codepoint));
  }

  public static Stream<Arguments> testContainsCodepoint() {
    return UnicodeCategory.codepointsByCode.entrySet().stream()
      .map(e -> Arguments.of(e.getKey(), e.getValue()));
  }

}
