// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import de.bottlecaps.markup.TestBase;

/**
 * Tests for the checkpoint callback that makes {@link Parser#parse} interruptible: a checkpoint
 * that throws aborts the parse, and the callback overloads are transparent otherwise.
 *
 * @author Gunther Rademacher
 */
public class CheckpointTest extends TestBase {
  /** Grammar with ambiguity: without a checkpoint this grows the parse forest until OOM. */
  private static final String AMBIGUOUS = "s: s, s | \"a\".";

  /** Marker thrown by a checkpoint to abort a parse. */
  private static final class Abort extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /** A checkpoint that throws aborts a runaway ambiguous parse, and its exception propagates out. */
  @Test
  public void throwingCheckpointAbortsAmbiguousParse() {
    Parser parser = generate(AMBIGUOUS);
    String input = "a".repeat(20);
    assertTimeoutPreemptively(Duration.ofSeconds(30), () ->
        assertThrows(Abort.class, () -> parser.parse(input, () -> { throw new Abort(); })));
  }

  /** The same holds for the ResultHandler overload. */
  @Test
  public void throwingCheckpointAbortsViaResultHandler() {
    Parser parser = generate(AMBIGUOUS);
    String input = "a".repeat(20);
    ResultHandler discard = new ResultHandler() {
      @Override public void startElement(String name) { }
      @Override public void endElement(String name) { }
      @Override public void attribute(String name, int[] codepoints, int length) { }
      @Override public void text(int[] codepoints, int length) { }
    };
    assertTimeoutPreemptively(Duration.ofSeconds(30), () ->
        assertThrows(Abort.class, () -> parser.parse(input, discard, () -> { throw new Abort(); })));
  }

  /** A long unambiguous parse (handled in the inner loop) is polled and thus interruptible too. */
  @Test
  public void longUnambiguousParseIsPolled() {
    Parser parser = generate("s: \"a\"+.");
    String input = "a".repeat(1_000_000);
    AtomicInteger calls = new AtomicInteger();
    parser.parse(input, calls::incrementAndGet);
    assertTrue(calls.get() > 0, "checkpoint should be polled during a long unambiguous parse");
  }

  /** A non-throwing checkpoint does not change the parse result. */
  @Test
  public void checkpointIsTransparent() {
    Parser parser = generate("s: \"a\"+.");
    String input = "a".repeat(1_000_000);
    String expected = parser.parse(input);
    String withCheckpoint = parser.parse(input, () -> { });
    assertEquals(expected, withCheckpoint);
  }
}
