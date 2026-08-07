// Copyright (c) 2023-2026 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import de.bottlecaps.markup.Blitz.Option;
import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.BlitzIxmlException;
import de.bottlecaps.markup.BlitzParseException;
import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.codepoints.Range;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.parser.Action;
import de.bottlecaps.markup.blitz.parser.ReduceArgument;
import de.bottlecaps.markup.blitz.transform.CompressedMap;

public class Parser
{
  public static final String IXML_NAMESPACE = "http://invisiblexml.org/NS";

  private static final int STALL_THRESHOLD = 8;

  private final Set<Option> defaultOptions;
  private final int[] asciiMap;
  private final CompressedMap bmpMap;
  private final int[] smpMap;
  private final CompressedMap terminalTransitions;
  private final int numberOfTokens;
  private final CompressedMap nonterminalTransitions;
  private final int numberOfNonterminals;
  private final ReduceArgument[] reduceArguments;
  private final String[] nonterminal;
  private final RangeSet[] terminal;
  private final int[] forks;
  private final BitSet[] expectedTokens;
  private final boolean isVersionMismatch;
  private final boolean normalizeEol;

  private Writer err = new OutputStreamWriter(System.err, StandardCharsets.UTF_8);

  public Parser(
      Set<Option> defaultOptions,
      int[] asciiMap, CompressedMap bmpMap, int[] smpMap,
      CompressedMap terminalTransitions, int numberOfTokens,
      CompressedMap nonterminalTransitions, int numberOfNonterminals,
      ReduceArgument[] reduceArguments,
      String[] nonterminal,
      RangeSet[] terminal,
      int[] forks,
      BitSet[] expectedTokens,
      boolean isVersionMismatch,
      boolean normalizeEol) {

    this.defaultOptions = defaultOptions;
    this.asciiMap = asciiMap;
    this.bmpMap = bmpMap;
    this.smpMap = smpMap;
    this.terminalTransitions = terminalTransitions;
    this.numberOfTokens = numberOfTokens;
    this.nonterminalTransitions = nonterminalTransitions;
    this.numberOfNonterminals = numberOfNonterminals;
    this.reduceArguments = reduceArguments;
    this.nonterminal = nonterminal;
    this.terminal = terminal;
    this.forks = forks;
    this.expectedTokens = expectedTokens;
    this.isVersionMismatch = isVersionMismatch;
    this.normalizeEol = normalizeEol;
  }

  /**
   * Parse the given input, returning the resulting XML as a string.
   *
   * @param input the input string
   * @param options options for use at parsing time. If absent, any options passed at generation time will be in effect
   * @return the resulting XML
   */
  public String parse(String input, Option... options) {
    return new ParsingContext(input).parse(options);
  }

  /**
   * Parse the given input, reporting the result to the given handler.
   *
   * @param input the input string
   * @param resultHandler the handler that receives the parse result
   * @param options options for use at parsing time. If absent, any options passed at generation time will be in effect
   */
  public void parse(String input, ResultHandler resultHandler, Option... options) {
    new ParsingContext(input).parse(resultHandler, options);
  }

  public void setTraceWriter(Writer w) {
    err = w;
  }

  private void writeTrace(String content) {
    try {
      err.write(content);
    }
    catch (IOException e) {
      throw new BlitzException(e);
    }
  }

  private String[] getExpectedTokenSet(ParseException e) {
    BitSet tokens = expectedTokens[e.getState()];
    RangeSet.Builder ranges = RangeSet.builder();
    for (int i = tokens.nextSetBit(0); i >= 0; i = tokens.nextSetBit(i + 1))
      ranges.add(terminal[i]);
    List<String> expected = new ArrayList<>();
    for (Range range : ranges.build()) {
      int last = range.getLastCodepoint();
      for (int codepoint = range.getFirstCodepoint(); codepoint <= last && codepoint >= 0; ++codepoint) {
        if (printRange(codepoint) && codepoint + 2 <= last && printRange(codepoint + 2)) {
          int endRange = codepoint + 2;
          while (endRange + 1 <= last && printRange(endRange + 1))
            ++endRange;
          expected.add(Codepoint.toString(codepoint) + "-" + Codepoint.toString(endRange));
          codepoint = endRange;
        }
        else {
          expected.add(Codepoint.toString(codepoint));
        }
      }
    }
    return expected.toArray(String[]::new);
  }

  private static boolean printRange(int chr) {
    return chr >= '0' && chr <= '9'
        || chr >= 'A' && chr <= 'Z'
        || chr >= 'a' && chr <= 'z'
        || ! Codepoint.isAscii(chr) && chr <= 0x10ffff;
  }

  private static String xmlEscape(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); ++i) {
      char c = s.charAt(i);
      switch (c) {
      case '<': sb.append("&lt;"); break;
      case '"': sb.append("&quot;"); break;
      case '&': sb.append("&amp;"); break;
      default : sb.append(c); break;
      }
    }
    return sb.toString();
  }

  private static class ParseException extends Exception {
    private static final long serialVersionUID = 1L;
    private int begin, offending, state;
    private boolean wasStalled;

    public ParseException(int begin, int state, int offending, boolean wasStalled) {
      this.begin = begin;
      this.state = state;
      this.offending = offending;
      this.wasStalled = wasStalled;
    }

    @Override
    public String getMessage() {
      return offending < 0
           ? "lexical analysis failed"
           : "syntax error";
    }

    public int getBegin() {return begin;}
    public int getState() {return state;}
    public boolean wasStalled() {return wasStalled;}
  }

  private static abstract class Symbol {
    abstract void send(ResultHandler resultHandler);
    abstract void validate();
  }

  private static class Terminal extends Symbol {
    private final int first;
    private int[] all;
    private int length;

    public Terminal(int codepoint) {
      first = codepoint;
      length = 1;
    }

    public Terminal(int[] codepoints) {
      first = codepoints[0];
      all = codepoints;
      length = codepoints.length;
    }

    public void append(Terminal other) {
      ensureMore(length + other.length);
      if (other.length == 1)
        all[length] = other.first;
      else
        System.arraycopy(other.all, 0, all, length, other.length);
      length += other.length;
    }

    private void ensureMore(int items) {
      if (all == null) {
        all = new int[Math.max(4, items)];
        all[0] = first;
      }
      else if (all.length < items) {
        int capacity = all.length;
        do capacity <<= 1; while (capacity < items);
        all = Arrays.copyOf(all, capacity);
      }
    }

    @Override
    void validate() {
      if (all == null) {
        if (! UnicodeCategory.xmlChar.containsCodepoint(first))
          Errors.D04.thro(Codepoint.toString(first));
      }
      else {
        for (int i = 0; i < length; ++i)
          if (! UnicodeCategory.xmlChar.containsCodepoint(all[i]))
            Errors.D04.thro(Codepoint.toString(all[i]));
      }
    }

    @Override
    public void send(ResultHandler resultHandler) {
      if (all == null)
        resultHandler.text(new int[] {first}, 1);
      else
        resultHandler.text(all, length);
    }
  }

  private static class Nonterminal extends Symbol {
    private static final Symbol[] NO_CHILDREN = new Symbol[] {};

    private Symbol[] children;
    private int childCount;
    private String name;

    public Nonterminal(String name) {
      this(name, NO_CHILDREN);
    }

    public Nonterminal(String name, Symbol... children) {
      this.name = name;
      this.children = children;
      this.childCount = children.length;
    }

    public void setName(String newName) {
      name = newName;
    }

    // turn this node into an attribute: collapse it to the concatenated terminal codepoints
    public Attribute toAttribute() {
      Attribute attribute = new Attribute(name);
      collect(attribute);
      return attribute;
    }

    // append the codepoints of all terminals in this subtree to the attribute value
    private void collect(Attribute attribute) {
      for (int i = 0; i < childCount; ++i) {
        Symbol child = children[i];
        if (child instanceof Nonterminal nonterminal)
          nonterminal.collect(attribute);
        else if (child instanceof Terminal terminal)
          attribute.append(terminal);
        else
          attribute.append((Attribute) child);
      }
    }

    public void addChildren(Symbol... newChildren) {
      if (childCount == 0) {
        children = newChildren;
      }
      else {
        Symbol[] combined = new Symbol[childCount + newChildren.length];
        System.arraycopy(children, 0, combined, 0, childCount);
        System.arraycopy(newChildren, 0, combined, childCount, newChildren.length);
        children = combined;
      }
      childCount = children.length;
    }

    @Override
    void validate() {
      if (name.charAt(0) == ' ')
        Errors.D03.thro(name.substring(1));
      Set<String> names = null;
      for (int i = 0; i < childCount; ++i) {
        final Symbol symbol = children[i];
        if (symbol instanceof Attribute attribute) {
          if (names == null)
            names = new HashSet<>();
          if (! names.add(attribute.name))
            Errors.D02.thro(attribute.name);
        }
        symbol.validate();
      }
    }

    @Override
    public void send(ResultHandler resultHandler) {
      resultHandler.startElement(name);
      for (int i = 0; i < childCount; ++i)
        if (children[i] instanceof Attribute)
          children[i].send(resultHandler);
      for (int i = 0; i < childCount; ++i)
        if (! (children[i] instanceof Attribute))
          children[i].send(resultHandler);
      resultHandler.endElement(name);
    }
  }

  private static final class Attribute extends Symbol {
    private final String name;
    private int[] value;
    private int length;

    public Attribute(String name) {
      this.name = name;
      value = new int[8];
    }

    public Attribute(String name, String value) {
      this.name = name;
      this.value = value.codePoints().toArray();
      length = this.value.length;
    }

    public void append(Terminal terminal) {
      if (terminal.all == null)
        append(terminal.first);
      else
        append(terminal.all, 0, terminal.length);
    }

    public void append(Attribute other) {
      append(other.value, 0, other.length);
    }

    private void append(int codepoint) {
      if (length == value.length)
        value = Arrays.copyOf(value, length << 1);
      value[length++] = codepoint;
    }

    private void append(int[] codepoints, int offset, int count) {
      int needed = length + count;
      if (needed > value.length) {
        int capacity = value.length;
        do capacity <<= 1; while (capacity < needed);
        value = Arrays.copyOf(value, capacity);
      }
      System.arraycopy(codepoints, offset, value, length, count);
      length = needed;
    }

    @Override
    void validate() {
      if (name.charAt(0) == ' ')
        Errors.D03.thro(name.substring(1));
      if (name.equals("xmlns"))
        Errors.D07.thro();
      for (int i = 0; i < length; ++i)
        if (! UnicodeCategory.xmlChar.containsCodepoint(value[i]))
          Errors.D04.thro(Codepoint.toString(value[i]));
    }

    @Override
    public void send(ResultHandler resultHandler) {
      resultHandler.attribute(name, value, length);
    }
  }

  private static class XmlSerializer implements ResultHandler {
    private static final String INDENTATION = "   ";

    private StringBuilder out;
    private int depth;
    private boolean delayedTag;
    private boolean indent;
    private boolean hasChildElement;

    public XmlSerializer(StringBuilder out, boolean indent) {
      this.out = out;
      this.indent = indent;
      depth = 0;
      delayedTag = false;
      hasChildElement = false;
    }

    public void startElement(String name) {
      if (delayedTag)
        out.append('>');
      delayedTag = true;
      if (indent && depth > 0) {
        out.append('\n');
        for (int i = 0; i < depth; ++i)
          out.append(INDENTATION);
      }
      out.append('<');
      out.append(name);
      hasChildElement = false;
      ++depth;
    }

    public void endElement(String name) {
      --depth;
      if (delayedTag) {
        delayedTag = false;
        out.append("/>");
      }
      else {
        if (indent) {
          if (hasChildElement) {
            out.append('\n');
            for (int i = 0; i < depth; ++i)
              out.append(INDENTATION);
          }
        }
        out.append("</");
        out.append(name);
        out.append('>');
      }
      hasChildElement = true;
    }

    public void attribute(String name, int[] codepoints, int length) {
      out.append(' ');
      out.append(name);
      out.append("=\"");
      for (int i = 0; i < length; ++i) {
        int codepoint = codepoints[i];
        switch (codepoint) {
        case '&': out.append("&amp;"); break;
        case '<': out.append("&lt;"); break;
        case '>': out.append("&gt;"); break;
        case '"': out.append("&quot;"); break;
        default:
          if (codepoint >= ' ') {
            out.appendCodePoint(codepoint);
          }
          else {
            out.append("&#x");
            out.append(Integer.toString(codepoint, 16).toUpperCase());
            out.append(';');
          }
        }
      }
      out.append('\"');
    }

    public void text(int[] codepoints, int length) {
      if (delayedTag) {
        out.append('>');
        delayedTag = false;
      }
      for (int i = 0; i < length; ++i) {
        int codepoint = codepoints[i];
        switch (codepoint) {
        case '&': out.append("&amp;"); break;
        case '<': out.append("&lt;"); break;
        case '>': out.append("&gt;"); break;
        case '\n': out.append('\n'); break;
        default:
          if (codepoint >= ' ') {
            out.appendCodePoint(codepoint);
          }
          else {
            out.append("&#x");
            out.append(Integer.toString(codepoint, 16).toUpperCase());
            out.append(';');
          }
        }          
      }
    }
  }

  private static class StackNode {
    private final StackNode link;
    private final int state;

    public StackNode() {
      link = null;
      state = -1;
    }

    private StackNode(final int state, final StackNode link) {
      this.link = link;
      this.state = state;
    }

    public int getState() {
      return state;
    }

    public StackNode push(final int state) {
      return new StackNode(state, this);
    }

    public StackNode pop() {
      return link;
    }

    @Override
    public boolean equals(final Object obj) {
      StackNode lhs = this;
      StackNode rhs = (StackNode) obj;
      while (lhs != rhs) {
        if (lhs.state != rhs.state) return false;
        lhs = lhs.link;
        rhs = rhs.link;
      }
      return true;
    }
  }

  private static abstract class DeferredEvent {
    private DeferredEvent link;
    private final int queueSize;

    public DeferredEvent(DeferredEvent link) {
      this.link = link;
      queueSize = link == null ? 0 : link.queueSize + 1;
    }

    public int getQueueSize() {
      return queueSize;
    }

    public abstract void execute(ParseTreeBuilder parseTreeBuilder);

    public void release(ParseTreeBuilder parseTreeBuilder) {
      DeferredEvent current = this;
      DeferredEvent predecessor = current.link;
      current.link = null;
      while (predecessor != null) {
        DeferredEvent next = predecessor.link;
        predecessor.link = current;
        current = predecessor;
        predecessor = next;
      }
      do {
        current.execute(parseTreeBuilder);
        current = current.link;
      }
      while (current != null);
    }
  }

  private static class TerminalEvent extends DeferredEvent {
    private int codepoint;

    public TerminalEvent(DeferredEvent link, int codepoint) {
      super(link);
      this.codepoint = codepoint;
    }

    @Override
    public void execute(ParseTreeBuilder parseTreeBuilder) {
      parseTreeBuilder.terminal(codepoint);
    }
  }

  private static class NonterminalEvent extends DeferredEvent {
    private ReduceArgument reduceArgument;

    public NonterminalEvent(DeferredEvent link, ReduceArgument reduceArgument) {
      super(link);
      this.reduceArgument = reduceArgument;
    }

    @Override
    public void execute(ParseTreeBuilder parseTreeBuilder) {
      parseTreeBuilder.nonterminal(reduceArgument);
    }
  }

  private static enum Status {
    PARSING,
    ERROR,
    ACCEPTED,
  };

  private class ParseTreeBuilder {
    private Symbol[] stack = new Symbol[64];
    private int top = -1;

    ParseTreeBuilder() {
      stack = new Symbol[64];
      top = -1;
    }

    public void nonterminal(ReduceArgument reduceArgument) {
      final Mark[] marks = reduceArgument.getMarks();
      final int[] aliases = reduceArgument.getAliases();
      final int count = marks.length;

      top -= count;
      final int from = top + 1;
      final int to = top + count + 1;
      final Nonterminal nt = new Nonterminal(nonterminal[reduceArgument.getNonterminalId()]);
      Symbol[] buffer;

      Terminal run;
      int i;
      int c;

      // reuse a hoisted child's array for left-recursive accumulation, otherwise collect into a fresh array
      if (count != 0 && marks[0] == Mark.DELETE && stack[from] instanceof Nonterminal accumulator
          && accumulator.childCount != 0) {
        i = from + 1;
        buffer = accumulator.children;
        c = accumulator.childCount;
        run = buffer[c - 1] instanceof Terminal terminal ? terminal : null;
      }
      else {
        i = from;
        buffer = Nonterminal.NO_CHILDREN;
        c = 0;
        run = null;
      }

      // collect children, combining adjacent terminals by extending the trailing run in place
      for (; i < to; ++i) {
        Symbol symbol = stack[i];
        Mark mark = marks[i - from];
        if (symbol instanceof Terminal terminal) {
          if (mark == Mark.NODE) {
            if (run != null)
              run.append(terminal);
            else {
              buffer = ensure(buffer, c + 1);
              buffer[c++] = run = terminal;
            }
          }
        }
        else {
          Nonterminal n = (Nonterminal) symbol;
          int alias = aliases[i - from];
          if (alias >= 0)
            n.setName(nonterminal[alias]);
          switch (mark) {
          case ATTRIBUTE:
            buffer = ensure(buffer, c + 1);
            buffer[c++] = n.toAttribute();
            run = null;
            break;
          case NODE:
            buffer = ensure(buffer, c + 1);
            buffer[c++] = n;
            run = null;
            break;
          case DELETE:
            // splice hoisted children; only the boundary need be combined
            Symbol[] grandchildren = n.children;
            int length = n.childCount;
            if (length != 0) {
              int start = 0;
              if (run != null && grandchildren[0] instanceof Terminal terminal) {
                run.append(terminal);
                start = 1;
              }
              int add = length - start;
              if (add != 0) {
                buffer = ensure(buffer, c + add);
                System.arraycopy(grandchildren, start, buffer, c, add);
                c += add;
                Symbol last = grandchildren[length - 1];
                run = last instanceof Terminal terminal ? terminal : null;
              }
            }
            break;
          default:
            throw new IllegalStateException("Unexpected mark: " + mark);
          }
        }
      }

      int[] insertion = reduceArgument.getInsertion();
      if (insertion != null) {
        if (run != null)
          run.append(new Terminal(insertion));
        else {
          buffer = ensure(buffer, c + 1);
          buffer[c++] = new Terminal(insertion);
        }
      }

      nt.children = c == 0 ? Nonterminal.NO_CHILDREN : buffer;
      nt.childCount = c;
      push(nt);
    }

    public void terminal(int codepoint) {
      push(new Terminal(codepoint));
    }

    public void serialize(ResultHandler resultHandler) {
      ((Nonterminal) stack[0]).children[0].send(resultHandler);
    }

    public void push(Symbol s) {
      if (++top >= stack.length)
        stack = Arrays.copyOf(stack, stack.length << 1);
      stack[top] = s;
    }

    private static Symbol[] ensure(Symbol[] buffer, int capacity) {
      if (buffer.length < capacity) {
        int size = Math.max(buffer.length, 1);
        while (size < capacity) size <<= 1;
        return buffer.length == 0 ? new Symbol[size] : Arrays.copyOf(buffer, size);
      }
      return buffer;
    }
  }

  private class ParsingContext {
    private String input = null;

    private ParseTreeBuilder parseTreeBuilder;
    private int size = 0;
    private int maxId = 0;
    private boolean trace;

    public ParsingContext(String input) {
      this.input = input;
    }

    public String parse(Option... options) {
      return run(currentOptions -> serialize(currentOptions.contains(Option.INDENT)), options);
    }

    public void parse(ResultHandler resultHandler, Option... options) {
      run(currentOptions -> {
        parseTreeBuilder.serialize(resultHandler);
        return null;
      }, options);
    }

    private <T> T run(java.util.function.Function<Set<Option>, T> emit, Option... options) {
      long t0 = System.currentTimeMillis();

      Set<Option> currentOptions = options.length == 0
          ? defaultOptions
          : new HashSet<>(Arrays.asList(options));
      parseTreeBuilder = new ParseTreeBuilder();
      try {
        trace = currentOptions.contains(Option.TRACE);
        if (trace)
          writeTrace("<?xml version=\"1.0\" encoding=\"UTF-8\"?" + ">\n<trace>\n");
        size = input.length();
        maxId = 0;
        ParsingThread thread;
        try {
          thread = parse();
        }
        catch (ParseException pe) {
          int begin = pe.getBegin();
          String prefix = input.substring(0, begin);
          String[] tokenSet = getExpectedTokenSet(pe);
          int line = prefix.replaceAll("[^\n]+", "").length() + 1;
          int column = prefix.length() - prefix.lastIndexOf('\n');
          throw new BlitzParseException(
              "Failed to parse input:\n" + getErrorMessage(pe, tokenSet),
              begin < input.length() ? ("'" + Character.toString(input.codePointAt(begin)) + "'")
                                     : Codepoint.toString(Codepoint.EOI),
              tokenSet,
              line,
              column
          );
        }
        finally {
          if (trace) {
            writeTrace("</trace>\n");
            try {
              err.flush();
            }
            catch (IOException e) {
            }
          }
        }

        Nonterminal startSymbol = ((Nonterminal) parseTreeBuilder.stack[0]);
        if (startSymbol.childCount == 0)
          Errors.D01.thro(); // not well-formed
        if (startSymbol.children[0] instanceof Attribute)
          Errors.D05.thro(); // attribute as root
        if (! (startSymbol.children[0] instanceof Nonterminal))
          Errors.D06.thro(); // not exactly one element
        Nonterminal nonterminal = (Nonterminal) startSymbol.children[0];
        if (startSymbol.childCount != 1)
          Errors.D06.thro(); // not exactly one element

        if (thread.isAmbiguous || isVersionMismatch) {
          String state = thread.isAmbiguous && isVersionMismatch
              ? "ambiguous version-mismatch"
              : thread.isAmbiguous
                  ? "ambiguous"
                  : "version-mismatch";
          nonterminal.addChildren(
              new Attribute("xmlns:ixml", IXML_NAMESPACE),
              new Attribute("ixml:state", state));
        }
        startSymbol.validate();
        return emit.apply(currentOptions);
      }
      catch (BlitzIxmlException e) {
        if (currentOptions.contains(Option.FAIL_ON_ERROR))
          throw e;
        Nonterminal ixml = new Nonterminal("ixml",
            new Attribute("xmlns:ixml", IXML_NAMESPACE),
            new Attribute("ixml:state", "failed"),
            new Attribute("ixml:error-code", e.getError().name()),
            new Terminal(e.getMessage().codePoints().toArray()));
        parseTreeBuilder.stack[0] = new Nonterminal("root", ixml);
      }
      catch (BlitzException e) {
        if (currentOptions.contains(Option.FAIL_ON_ERROR))
          throw e;
        Nonterminal ixml = new Nonterminal("ixml",
            new Attribute("xmlns:ixml", IXML_NAMESPACE),
            new Attribute("ixml:state", "failed"),
            new Terminal(e.getMessage().codePoints().toArray()));
        parseTreeBuilder.stack[0] = new Nonterminal("root", ixml);
      }
      finally {
        if (currentOptions.contains(Option.TIMING)) {
          long t1 = System.currentTimeMillis();
          System.err.println("        ixml parsing time: " + (t1 - t0) + " msec");
        }
      }
      return emit.apply(currentOptions);
    }

    private String serialize(boolean indent) {
      StringBuilder w = new StringBuilder();
      parseTreeBuilder.serialize(new XmlSerializer(w, indent));
      return w.toString();
    }

    private ParsingThread parse() throws ParseException {
      Queue<ParsingThread> currentThreads = new ArrayDeque<>();
      LongKeyHeap<ParsingThread> otherThreads = new LongKeyHeap<>();
      ParsingThread thread = new ParsingThread();
      int pos = 0;
      boolean stalled = false;

      for (;;) {
        for (ParsingThread t; (t = otherThreads.removeIfEqual(thread)) != null;) {
          if (trace)
            writeTrace("  <parse thread=\"" + thread.id + "\" offset=\"" + thread.e0 + "\" state=\"" + thread.state + "\" action=\"discard\"/>\n");
          if (t.deferredEvent == null || t.deferredEvent.getQueueSize() < thread.deferredEvent.getQueueSize())
            thread = t;
          thread.isAmbiguous = true;
        }

        boolean isUnambiguous = otherThreads.isEmpty();

        if (isUnambiguous && thread.deferredEvent != null) {
          thread.deferredEvent.release(parseTreeBuilder);
          thread.deferredEvent = null;
        }

        if (thread.status == Status.ACCEPTED) {
          if (! isUnambiguous)
            throw new IllegalStateException();
          return thread;
        }

        Arrays.fill(thread.forkCount, (byte) 0);
        int repeatedForks = 0;
        do {
          int fork = thread.parse(isUnambiguous);
          if (fork >= 0) {
            isUnambiguous = false;
            thread.action = forks[2 * fork];
            if (thread.e0 > pos) {
              otherThreads.add(thread.priority(), thread);
              ParsingThread forkedThread = new ParsingThread(thread, forks[2 * fork + 1]);
              otherThreads.add(forkedThread.priority(), forkedThread);
            }
            else if (thread.forkCount[fork] > 0 && repeatedForks >= STALL_THRESHOLD) {
              stalled = true;
              if (trace)
                writeTrace("  <parse thread=\"" + thread.id + "\" offset=\"" + thread.e0 + "\" state=\"" + thread.state + "\" action=\"stalled\"/>\n");
            }
            else {
              if (thread.forkCount[fork]++ > 1)
                ++repeatedForks;
              currentThreads.add(thread);
              currentThreads.add(new ParsingThread(thread, forks[2 * fork + 1]));
            }
          }
          else if (thread.status != Status.ERROR) {
            otherThreads.add(thread.priority(), thread);
          }
          else if (otherThreads.isEmpty() && currentThreads.isEmpty()) {
            throw new ParseException(thread.b1, thread.state, thread.l1, stalled);
          }
        }
        while ((thread = currentThreads.poll()) != null);

        thread = otherThreads.remove();
        if (thread.e0 > pos)
          pos = thread.e0;
      }
    }

    private String getErrorMessage(ParseException e, String[] tokenSet) {
      boolean eoi = e.getBegin() >= input.length();
      String found = eoi
                   ? RangeSet.EOI.shortName()
                   : ("'" + Character.toString(input.codePointAt(e.getBegin())) + "'");
      int limit = 16;
      String[] expectedTokens = Stream.concat(
            Arrays.stream(tokenSet)
            .sorted((s1, s2) -> Boolean.compare(s1.startsWith("#"), s2.startsWith("#")))
            .limit(limit),
            tokenSet.length > limit ? Stream.of("...") : Stream.empty()
          )
          .toArray(String[]::new);
      String message = e.getMessage() + ", found " + found
                     + "\nwhile expecting "
                     + (expectedTokens.length == 1 ? expectedTokens[0] : ("one of " + Arrays.toString(expectedTokens)))
                     + "\nat " + lineAndColumn(e.getBegin());
      if (!eoi)
        message += ":\n..." + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64)) + "...";
      if (e.wasStalled())
        message += "\nHowever, some alternatives were discarded while parsing because they were "
                + "suspected to be involved in infinite ambiguity.";
      return message;
    }

    private String lineAndColumn(int pos) {
      String prefix = input.subSequence(0, pos).toString();
      int line = prefix.replaceAll("[^\n]", "").length() + 1;
      int column = prefix.length() - prefix.lastIndexOf('\n');
      return "line " + line + ", column " + column;
    }

    private class ParsingThread {
      public final byte[] forkCount;
      public DeferredEvent deferredEvent;
      public Status status;
      public final int id;
      public int state;
      public int action;
      public int b0, e0;
      public int b1, e1;
      public int c1, l1;
      public boolean isAmbiguous;

      private StackNode stack;

      public ParsingThread() {
        forkCount = new byte[forks.length / 2];
        b0 = 0;
        e0 = 0;
        b1 = 0;
        e1 = 0;
        maxId = 0;
        id = maxId;
        isAmbiguous = false;
        status = Status.PARSING;
        deferredEvent = null;
        stack = new StackNode();
        state = 0;
        l1 = match();
        action = l1 < 0
               ? 0
               : terminalTransitions.get(state * numberOfTokens + l1);
      }

      public ParsingThread(ParsingThread other, int action) {
        forkCount = Arrays.copyOf(other.forkCount, other.forkCount.length);
        this.action = action;
        status = other.status;
        deferredEvent = other.deferredEvent;
        id = ++maxId;
        state = other.state;
        stack = other.stack;
        b0 = other.b0;
        e0 = other.e0;
        c1 = other.c1;
        l1 = other.l1;
        b1 = other.b1;
        e1 = other.e1;
        isAmbiguous = other.isAmbiguous;
      }

      public long priority() {
        return (long) e0 << 32 | ~id & 0xFFFFFFFFL;
      }

      @Override
      public boolean equals(Object obj) {
        if (obj == null) return false;
        ParsingThread other = (ParsingThread) obj;
        if (state != other.state) return false;
        if (action != other.action) return false;
        if (e0 != other.e0) return false;
        if (e1 != other.e1) return false;
        if (l1 != other.l1) return false;
        if (status != other.status) return false;
        return stack.equals(other.stack);
      }

      public int parse(boolean isUnambiguous) throws ParseException {
        int nonterminalId = -1;
        int limit = isUnambiguous ? Integer.MAX_VALUE : e0;
        for (;;) {
          if (trace) {
            writeTrace("  <parse thread=\"" + id + "\" offset=\"" + e0 + "\" state=\"" + state + "\" input=\"");
            if (nonterminalId >= 0) {
              writeTrace(xmlEscape(nonterminal[nonterminalId]));
              if (l1 > 0)
                writeTrace(" ");
            }
            if (l1 > 0)
              writeTrace(xmlEscape(terminal[l1].shortName()));
            writeTrace("\" action=\"");
          }

          int argument = action >> Action.Type.BITS;
          int shift = -1;
          int reduce = -1;
          switch (action & ((1 << Action.Type.BITS) - 1)) {
          case 1: // SHIFT
            shift = argument;
            break;

          case 2: // SHIFT+REDUCE
            shift = state;
            // fall through

          case 3: // REDUCE
            reduce = argument;
            break;

          case 4: // FORK
            if (trace)
              writeTrace("fork\"/>\n");
            return argument;

          case 5: // ACCEPT
            if (trace)
              writeTrace("accept\"/>\n");
            status = Status.ACCEPTED;
            action = 0;
            ++e0;
            return -1;

          default: // ERROR
            if (trace)
              writeTrace("fail\"/>\n");
            status = Status.ERROR;
            return -1;
          }

          if (shift >= 0) {
            if (trace)
              writeTrace("shift");
            if (nonterminalId < 0) {
              if (isUnambiguous)
                parseTreeBuilder.terminal(c1);
              else
                deferredEvent = new TerminalEvent(deferredEvent, c1);
              b0 = b1;
              e0 = e1;
              c1 = -1;
              l1 = 0;
            }
            stack = stack.push(state);
            state = shift;
          }

          if (reduce < 0) {
            if (trace)
              writeTrace("\"/>\n");
            if (l1 == 0)
              l1 = match();
            action = l1 < 0
               ? 0
               : terminalTransitions.get(state * numberOfTokens + l1);
            if (e0 > limit)
              return -1;
            nonterminalId = -1;
          }
          else {
            ReduceArgument reduceArgument = reduceArguments[reduce];
            int symbols = reduceArgument.getMarks().length;
            nonterminalId = reduceArgument.getNonterminalId();
            if (trace) {
              if (shift >= 0)
                writeTrace(" ");
              writeTrace("reduce\" nonterminal=\"" + xmlEscape(nonterminal[nonterminalId]) + "\" count=\"" + symbols + "\"/>\n");
            }
            if (symbols > 0) {
              for (int i = 1; i < symbols; i++)
                stack = stack.pop();
              state = stack.getState();
              stack = stack.pop();
            }
            if (isUnambiguous)
              parseTreeBuilder.nonterminal(reduceArgument);
            else
              deferredEvent = new NonterminalEvent(deferredEvent, reduceArgument);
            action = nonterminalTransitions.get(state * numberOfNonterminals + nonterminalId);
          }
        }
      }

      private int match() {
        if (trace)
          writeTrace("  <tokenize thread=\"" + id + "\" offset=\"" + e1 + "\"");

        b1 = e1;
        final int charclass;
        if (e1 >= size) {
          c1 = -1;
          charclass = 0;
        }
        else {
          c1 = input.charAt(e1++);
          if (c1 < 0x80) {
            if (trace)
              if (c1 >= 32 && c1 <= 126)
                writeTrace(" char=\"" + xmlEscape(String.valueOf((char) c1)) + "\"");
            if (c1 == 0xD && normalizeEol) {
              if (e1 < size && input.charAt(e1) == 0xA)
                ++e1;
              c1 = 0xA;
            }
            charclass = asciiMap[c1];
          }
          else if (c1 < 0xd800) {
            charclass = bmpMap.get(c1);
          }
          else {
            if (c1 < 0xdc00) {
              final int lowSurrogate = e1 < size ? input.charAt(e1) : 0;
              if (lowSurrogate >= 0xdc00 && lowSurrogate < 0xe000) {
                ++e1;
                c1 = ((c1 & 0x3ff) << 10) + (lowSurrogate & 0x3ff) + 0x10000;
              }
            }

            final int smpMapSize = smpMap.length / 3;
            int lo = 0, hi = smpMapSize - 1;
            for (int m = hi >> 1; ; m = (hi + lo) >> 1) {
              if (smpMap[m] > c1) {hi = m - 1;}
              else if (smpMap[smpMapSize + m] < c1) {lo = m + 1;}
              else {charclass = smpMap[2 * smpMapSize + m]; break;}
              if (lo > hi) {charclass = -1; break;}
            }
          }
          if (trace && c1 >= 0)
            writeTrace(" codepoint=\"" + c1 + "\"");
          if (charclass <= 0) {
            if (trace)
              writeTrace(" status=\"fail\" end=\"" + e1 + "\"/>\n");
            e1 = b1;
            return -1;
          }
        }

        if (trace) {
          writeTrace(" class=\"" + charclass + "\"");
          writeTrace(" status=\"success\" result=\"");
          writeTrace(xmlEscape(terminal[charclass].shortName()));
          writeTrace("\" end=\"" + e1 + "\"/>\n");
        }
        return charclass;
        }
      }
    }
  }
