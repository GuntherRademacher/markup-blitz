// Copyright (c) 2023-2024 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bottlecaps.markup.BlitzException;

/** Markup Blitz Options. */
public enum Option {
  // Purpose                                    Name                    Valid values
  // -------                                    ----                    ------------
  /** Generate XML with indentation.         */ INDENT                 (false, true),
  /** Print parser trace.                    */ TRACE                  (false, true),
  /** Print timing information.              */ TIMING                 (false, true),
  /** Print intermediate results.            */ VERBOSE                (false, true),
  /** Fail on parsing error.                 */ FAIL_ON_ERROR          (false, true),
  /** Handling of leading unmatched content. */ LEADING_CONTENT_POLICY (Value.COMPLETE_MATCH,
                                                                        Value.FIRST_MATCH),
  /** Handling of trailing unmatched content.*/ TRAILING_CONTENT_POLICY(Value.COMPLETE_MATCH,
                                                                        Value.LONGEST_MATCH,
                                                                        Value.SHORTEST_MATCH);
  /** Option values. */
  public static enum Value {
    //  Purpose                                            Name
    //  -------                                            ----
    /** Disallow unmatched content.                     */ COMPLETE_MATCH,
    /** Ignore leading content, accept first match.     */ FIRST_MATCH,
    /** Accept longest match, ignore trailing content.  */ LONGEST_MATCH,
    /** Accept shortest match, ignore trailing content. */ SHORTEST_MATCH;

    public String externalName() {
      return name().toLowerCase().replace('_', '-');
    }
  }

  private static final Pattern commandLineOptionPattern = Pattern.compile("--([-a-z]+)(?::([-a-z]+))?");
  private final Object[] values;

  private Option(Object... values) {
    this.values = values.length != 0 ? values : new Object[] {false, true};
  }

  public String externalName() {
    return name().toLowerCase().replace('_', '-');
  }

  public boolean is(Object value, Map<?, ?> options) {
    return value.equals(options.get(this));
  }

  public static void validate(Map<Option, Object> options) {
    options.forEach((option, value) -> {
      Arrays.stream(option.values)
          .filter(v -> v.equals(value))
          .findFirst()
          .orElseThrow(() -> {
            Object badValue = value instanceof String
                ? "\"" + value.toString().replace("\"", "\\\"") + "\""
                : value;
            return new BlitzException(
                "Unsupported value for " + Option.class.getSimpleName() + "." + option.name() + ": " + badValue);
          });
    });
  }

  public static Map<Option, Object> optionMap(Map<String, String> stringMap) {
    Map<Option, Object> options = new HashMap<>();
    stringMap.forEach((key, val) -> {
      if (! addTo(options, key, val)) {
        throw new BlitzException("Unsupported option \"" + key + "\": \"" + val + "\"");
      }
    });
    return options;
  }

  public static boolean addTo(Map<Option, Object> options, String commandLineArg) {
    Matcher matcher = commandLineOptionPattern.matcher(commandLineArg);
    if (! matcher.matches())
      return false;
    String key = matcher.group(1);
    String val = matcher.group(2) != null
        ? matcher.group(2)
        : Boolean.TRUE.toString();
    return addTo(options, key, val);
  }

  private static boolean addTo(Map<Option, Object> options, String key, String val) {
    Option option = Arrays.stream(values())
        .filter(v -> v.externalName().equals(key))
        .findFirst()
        .orElse(null);
    if (option == null)
      return false;
    Object value = Arrays.stream(option.values)
      .filter(v -> (v instanceof Value ? ((Value) v).externalName() : v.toString()).equals(val))
      .findFirst()
      .orElse(null);
    if (value == null)
      return false;
    options.put(option, value);
    return true;
  }
 }
