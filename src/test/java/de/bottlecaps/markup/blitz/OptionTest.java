// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.bottlecaps.markup.Blitz;
import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.TestBase;

public class OptionTest extends TestBase {

  @Test
  public void testOptionMap() throws IOException {
    Map<String, String> options = Map.of(
      Option.INDENT.externalName(), Boolean.TRUE.toString(),
      Option.TRACE.externalName(), Boolean.FALSE.toString(),
      Option.TIMING.externalName(), Boolean.FALSE.toString(),
      Option.VERBOSE.externalName(), Boolean.FALSE.toString(),
      Option.FAIL_ON_ERROR.externalName(), Boolean.FALSE.toString(),
      Option.LEADING_CONTENT_POLICY.externalName(), Option.Value.FIRST_MATCH.externalName(),
      Option.TRAILING_CONTENT_POLICY.externalName(), Option.Value.LONGEST_MATCH.externalName());
    Parser parser = Blitz.generate("S:A+. A:[Ll].", Option.optionMap(options));
    String xml = parser.parse("...abc...");
    assertEquals(
        "<S xmlns:ixml=\"http://invisiblexml.org/NS\" ixml:state=\"prefix\" xmlns:blitz=\"http://de.bottlecaps/markup/blitz/NS\" blitz:offset=\"3\" blitz:length=\"3\">\n"
      + "   <A>a</A>\n"
      + "   <A>b</A>\n"
      + "   <A>c</A>\n"
      + "</S>",
      xml);
  }

  @ParameterizedTest
  @MethodSource
  public void testOptionValue(Option option, Object value) {
    Map<Option, Object> optionMap = Map.of(option, value);
    Option.validate(optionMap);

    String externalValue = value.toString().toLowerCase().replace('_', '-');
    assertEquals(optionMap, Option.optionMap(Map.of(option.externalName(), externalValue)));

    Map<Option, Object> commandLineMap = new HashMap<>();
    assertTrue(Option.addTo(commandLineMap, "--" + option.externalName() + ":" + externalValue));
    assertEquals(optionMap, commandLineMap);
  }

  public static Stream<Arguments> testOptionValue() {
    return Stream.of(
        Arguments.of(Option.INDENT, true),
        Arguments.of(Option.INDENT, false),
        Arguments.of(Option.TRACE, true),
        Arguments.of(Option.TRACE, false),
        Arguments.of(Option.TIMING, true),
        Arguments.of(Option.TIMING, false),
        Arguments.of(Option.VERBOSE, true),
        Arguments.of(Option.VERBOSE, false),
        Arguments.of(Option.FAIL_ON_ERROR, true),
        Arguments.of(Option.FAIL_ON_ERROR, false),
        Arguments.of(Option.LEADING_CONTENT_POLICY, Option.Value.COMPLETE_MATCH),
        Arguments.of(Option.LEADING_CONTENT_POLICY, Option.Value.FIRST_MATCH),
        Arguments.of(Option.TRAILING_CONTENT_POLICY, Option.Value.COMPLETE_MATCH),
        Arguments.of(Option.TRAILING_CONTENT_POLICY, Option.Value.LONGEST_MATCH),
        Arguments.of(Option.TRAILING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH)
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testBadOptionValue(Option option, Object value, boolean validAsString) {
    try {
      Map<Option, Object> map = new HashMap<>();
      map.put(option, value);
      Option.validate(map);
      fail("Expected Option.validate to fail, but it succeeded.");
    }
    catch (BlitzException e) {
      String msg = e.getMessage();
      if (! msg.contains("Option." + option.name()) || ! msg.contains(value == null ? "null" : value.toString()))
        throw e;
    }

    if (validAsString)
      return;

    try {
      Map<String, String> map = new HashMap<>();
      map.put(option.toString(), value == null ? null : value.toString());
      Option.optionMap(map);
      fail("Expected Option.optionMap to fail, but it succeeded.");
    }
    catch (BlitzException e) {
      String msg = e.getMessage();
      if (! msg.contains(option.toString()) || ! msg.contains(value == null ? "null" : value.toString()))
        throw e;
    }

    Map<Option, Object> commandLineMap = new HashMap<>();
    assertFalse(Option.addTo(commandLineMap, "--" + option + ":" + value));
  }

  public static Stream<Arguments> testBadOptionValue() {
    return Stream.of(
        Arguments.of(Option.INDENT, 0, false),
        Arguments.of(Option.INDENT, "false", true),
        Arguments.of(Option.TRACE, "yes", false),
        Arguments.of(Option.TRACE, 1, false),
        Arguments.of(Option.TIMING, null, false),
        Arguments.of(Option.TIMING, "true", true),
        Arguments.of(Option.VERBOSE, "", false),
        Arguments.of(Option.VERBOSE, "-", false),
        Arguments.of(Option.FAIL_ON_ERROR, Option.Value.COMPLETE_MATCH, false),
        Arguments.of(Option.FAIL_ON_ERROR, Option.FAIL_ON_ERROR, false),
        Arguments.of(Option.LEADING_CONTENT_POLICY, Option.Value.LONGEST_MATCH, false),
        Arguments.of(Option.LEADING_CONTENT_POLICY, Option.Value.SHORTEST_MATCH, false),
        Arguments.of(Option.TRAILING_CONTENT_POLICY, Option.Value.FIRST_MATCH, false),
        Arguments.of(Option.TRAILING_CONTENT_POLICY, "longest-match", true),
        Arguments.of(Option.TRAILING_CONTENT_POLICY, false, false)
      );
  }

}
