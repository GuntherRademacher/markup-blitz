package de.bottlecaps.markup.blitz;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import de.bottlecaps.markup.Blitz.Option;
import de.bottlecaps.markup.BlitzException;
import de.bottlecaps.markup.BlitzParseException;
import de.bottlecaps.markup.blitz.codepoints.Codepoint;
import de.bottlecaps.markup.blitz.codepoints.RangeSet;
import de.bottlecaps.markup.blitz.codepoints.UnicodeCategory;
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.parser.Action;
import de.bottlecaps.markup.blitz.parser.ReduceArgument;
import de.bottlecaps.markup.blitz.transform.CompressedMap;

public class Parser
{
  private static int STALL_THRESHOLD = 8;

  private static class ParseException extends Exception
  {
    private static final long serialVersionUID = 1L;
    private int begin, end, offending, state;
    private boolean wasStalled;

    public ParseException(int begin, int end, int state, int offending, boolean wasStalled)
    {
      this.begin = begin;
      this.end = end;
      this.state = state;
      this.offending = offending;
      this.wasStalled = wasStalled;
    }

    @Override
    public String getMessage()
    {
      return offending < 0
           ? "lexical analysis failed"
           : "syntax error";
    }

    public int getBegin() {return begin;}
    public int getEnd() {return end;}
    public int getState() {return state;}
    public int getOffending() {return offending;}
    public boolean wasStalled() {return wasStalled;}
  }

  public interface EventHandler {
    public void startNonterminal(String name);
    public void startAttribute(String name);
    public void endAttribute(String name);
    public void endNonterminal(String name);
    public void terminal(int content);
  }

  public static abstract class Symbol {
    public abstract void send(EventHandler e);
    public abstract void sendContent(EventHandler e);
  }

  public class Terminal extends Symbol {
    public int codepoint;

    public Terminal(int codepoint) {
      this.codepoint = codepoint;
    }

    @Override
    public void send(EventHandler e) {
      e.terminal(codepoint);
    }

    @Override
    public void sendContent(EventHandler e) {
      e.terminal(codepoint);
    }
  }

  public class Insertion extends Symbol {
    public int[] codepoints;

    public Insertion(int[] codepoints) {
      this.codepoints = codepoints;
    }

    @Override
    public void send(EventHandler e) {
      for (int codepoint : codepoints)
        e.terminal(codepoint);
    }

    @Override
    public void sendContent(EventHandler e) {
      for (int codepoint : codepoints)
        e.terminal(codepoint);
    }
  }

  public static class Nonterminal extends Symbol
  {
    private String name;
    private Symbol[] children;
    private boolean isAttribute;

    public Nonterminal(String name, Symbol[] children)
    {
      this.name = name;
      this.children = children;
      this.isAttribute = false;
    }

    public void setAttribute(boolean isAttribute) {
      this.isAttribute = isAttribute;
    }

    @Override
    public void send(EventHandler e)
    {
      if (isAttribute) {
        e.startAttribute(name);
        for (Symbol c : children)
          c.sendContent(e);
        e.endAttribute(name);
      }
      else {
        if (name.charAt(0) == ' ')
          Errors.D03.thro(name);
        e.startNonterminal(name);
        Set<String> names = new HashSet<>();
        for (Symbol c : children)
          if (c instanceof Nonterminal) {
            Nonterminal nonterminal = (Nonterminal) c;
            if (nonterminal.isAttribute) {
              String attributeName = nonterminal.name;
              if (attributeName.equals("xmlns"))
                Errors.D07.thro();
              if (! names.add(attributeName))
                Errors.D02.thro(attributeName);
              if (attributeName.charAt(0) == ' ')
                Errors.D03.thro(attributeName);
              c.send(e);
            }
          }
        for (Symbol c : children)
          if (! (c instanceof Nonterminal) || ! ((Nonterminal) c).isAttribute)
            c.send(e);
        e.endNonterminal(name);
      }
    }

    @Override
    public void sendContent(EventHandler e)
    {
      for (Symbol c : children)
        if (! (c instanceof Nonterminal) || ! ((Nonterminal) c).isAttribute)
          c.sendContent(e);
    }

    public void addChildren(Symbol... newChildren) {
      if (newChildren == null) {
        children = newChildren;
      }
      else {
        int length = children.length;
        children = Arrays.copyOf(children, length + newChildren.length);
        System.arraycopy(newChildren, 0, children, length, newChildren.length);
      }
    }
  }

  public interface BottomUpEventHandler
  {
    public void nonterminal(ReduceArgument reduceArgument);
    public void terminal(int codepoint);
  }

  public static class XmlSerializer implements EventHandler {
    private String delayedTag;
    private Writer out;
    private boolean indent;
    private boolean hasChildElement;
    private int depth;
    private int attributeLevel;

    public XmlSerializer(Writer w, boolean indent)
    {
      this.indent = indent;
      delayedTag = null;
      out = w;
      hasChildElement = false;
      depth = 0;
    }

    @Override
    public void startNonterminal(String name) {
      if (attributeLevel == 0) {
        if (delayedTag != null)
        {
          writeOutput(">");
        }
        delayedTag = name;
        if (indent && depth > 0)
        {
          writeOutput("\n");
          for (int i = 0; i < depth; ++i)
          {
            writeOutput("   ");
          }
        }
        writeOutput("<");
        writeOutput(name);
        hasChildElement = false;
        ++depth;
      }
    }

    @Override
    public void endNonterminal(String name) {
      if (attributeLevel == 0) {
        --depth;
        if (delayedTag != null)
        {
          delayedTag = null;
          writeOutput("/>");
        }
        else
        {
          if (indent)
          {
            if (hasChildElement)
            {
              writeOutput("\n");
              for (int i = 0; i < depth; ++i)
              {
                writeOutput("   ");
              }
            }
          }
          writeOutput("</");
          writeOutput(name);
          writeOutput(">");
        }
        hasChildElement = true;
      }
    }

    @Override
    public void startAttribute(String name) {
      ++attributeLevel;
      writeOutput(" ");
      writeOutput(name);
      writeOutput("=\"");
    }

    @Override
    public void endAttribute(String name) {
      writeOutput("\"");
      --attributeLevel;
    }

    @Override
    public void terminal(int codepoint) {
      if (! UnicodeCategory.xmlChar.containsCodepoint(codepoint))
        Errors.D04.thro(Codepoint.toString(codepoint));
      if (attributeLevel > 0) {
        switch (codepoint) {
        case '&': writeOutput("&amp;"); break;
        case '<': writeOutput("&lt;"); break;
        case '>': writeOutput("&gt;"); break;
        case '"': writeOutput("&quot;"); break;
        case '\n': writeOutput("\n"); break;
        default:
          if (codepoint >= ' ') {
            writeOutput(Character.toString(codepoint));
          }
          else {
            writeOutput("&x");
            writeOutput(Integer.toString(codepoint, 16).toUpperCase());
            writeOutput(";");
          }
        }
      }
      else {
        if (delayedTag != null) {
          writeOutput(">");
          delayedTag = null;
        }
        switch (codepoint) {
        case '&': writeOutput("&amp;"); break;
        case '<': writeOutput("&lt;"); break;
        case '>': writeOutput("&gt;"); break;
        case '\n': writeOutput("\n"); break;
        default:
          if (codepoint >= ' ') {
            writeOutput(Character.toString(codepoint));
          }
          else {
            writeOutput("&#x");
            writeOutput(Integer.toString(codepoint, 16).toUpperCase());
            writeOutput(";");
          }
        }
      }
    }

    public void writeOutput(String content)
    {
      try
      {
        out.write(content);
      }
      catch (IOException e)
      {
        throw new BlitzException(e);
      }
    }
  }

  public class ParseTreeBuilder implements BottomUpEventHandler {
    public Symbol[] stack = new Symbol[64];
    public int top = -1;

    @Override
    public void nonterminal(ReduceArgument reduceArgument) {
      Mark[] marks = reduceArgument.getMarks();
      int count = marks.length;
      top -= count;
      int from = top + 1;
      int to = top + count + 1;

      List<Symbol> children = null;

      for (int i = from; i < to; ++i) {
        Symbol symbol = stack[i];
        if (symbol instanceof Terminal) {
          Mark mark = marks[i - top - 1];
          switch (mark) {
          case NODE:
            if (children == null)
              children = new ArrayList<>();
            children.add(symbol);
            break;
          case DELETE:
            break;
          case ATTRIBUTE:
            throw new IllegalStateException("cannot promote a terminal to an attribute");
          default:
            throw new IllegalStateException("unexpected mark: " + mark);
          }
        }
        else {
          Nonterminal nonterminal = (Nonterminal) symbol;
          switch (marks[i - top - 1]) {
          case ATTRIBUTE:
            nonterminal.setAttribute(true);
            // fall through
          case NODE:
            if (children == null)
              children = new ArrayList<>();
            children.add(nonterminal);
            break;
          case DELETE:
            if (children == null)
              children = new ArrayList<>();
            children.addAll(Arrays.asList(nonterminal.children));
            break;
          case NONE:
            throw new IllegalStateException();
          }
        }
      }

      int[] insertion = reduceArgument.getInsertion();
      if (insertion != null) {
        if (children == null)
          children = new ArrayList<>();
        children.add(new Insertion(insertion));
      }

      push(new Nonterminal(nonterminal[reduceArgument.getNonterminalId()],
          children == null ? new Symbol[0] : children.toArray(Symbol[]::new)
      ));
    }

    @Override
    public void terminal(int codepoint) {
      push(new Terminal(codepoint));
    }

    public void serialize(EventHandler e) {
      ((Nonterminal) stack[0]).children[0].send(e);
    }

    public void push(Symbol s)
    {
      if (++top >= stack.length)
      {
        stack = Arrays.copyOf(stack, stack.length << 1);
      }
      stack[top] = s;
    }
  }

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
      boolean isVersionMismatch)
  {
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
  }

  public RangeSet getOffendingToken(ParseException e) {
    return e.getOffending() < 0 ? null : terminal[e.getOffending()];
  }

  public String[] getExpectedTokenSet(ParseException e) {
    return getTokenSet(e.getState());
  }

  public String getErrorMessage(ParseException e) {
    String message = e.getMessage();
    String[] tokenSet = getExpectedTokenSet(e);
    String found = e.getOffending() < 0
                 ? null
                 : terminal[e.getOffending()].shortName();
    int size = e.getEnd() - e.getBegin();
    message += (found == null ? "" : ", found " + found)
            + "\nwhile expecting "
            + (tokenSet.length == 1 ? tokenSet[0] : Arrays.toString(tokenSet))
            + "\n"
            + (size == 0 || found != null ? "" : "after successfully scanning " + size + " characters beginning ");
    message += "at " + lineAndColumn(e.getBegin()) + ":\n..."
            + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64))
            + "...";
    if (e.wasStalled())
      message += "\nHowever, some alternatives were discarded while parsing because they were"
              + "suspected to be involved in infinite ambiguity.";
    return message;
  }

  private String lineAndColumn(int pos) {
    String prefix = input.subSequence(0, pos).toString();
    int line = prefix.replaceAll("[^\n]", "").length() + 1;
    int column = prefix.length() - prefix.lastIndexOf('\n');
    return "line " + line + ", column " + column;
  }

  /**
   * Parse the given input.
   *
   * @param input the input string
   * @param options options for use at parsing time. If absent, any options passed at generation time will be in effect
   * @return the resulting XML
   * @throws BlitzException if any error is detected while parsing
   */
  public String parse(String input, Option... options) throws BlitzException {
    Set<Option> currentOptions = options.length == 0
        ? defaultOptions
        : Set.of(options);

    long t0 = System.currentTimeMillis();

    boolean indent = currentOptions.contains(Option.INDENT);
    StringWriter w = new StringWriter(input.length());
    XmlSerializer s = new XmlSerializer(w, indent);
    ParseTreeBuilder b = new ParseTreeBuilder();

    trace = currentOptions.contains(Option.TRACE);
    if (trace)
      writeTrace("<?xml version=\"1.0\" encoding=\"UTF-8\"?" + ">\n<trace>\n");

    eventHandler = b;
    this.input = input;
    size = input.length();
    maxId = 0;

    ParsingThread thread;
    try {
      thread = parse();
    }
    catch (ParseException pe) {
      int begin = pe.getBegin();
      String prefix = input.substring(0, begin);
      int offending = pe.getOffending();
      int line = prefix.replaceAll("[^\n]", "").length() + 1;
      int column = prefix.length() - prefix.lastIndexOf('\n');
      throw new BlitzParseException(
          "Failed to parse input:\n" + getErrorMessage(pe),
          offending >= 0 ? terminal[offending].shortName()
                         : begin < input.length() ? ("'" + Character.toString(input.codePointAt(begin)) + "'")
                                                   : "$",
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
          throw new BlitzException(e);
        }
      }
      w.flush();
    }

    Nonterminal startSymbol = ((Nonterminal) b.stack[0]);
    if (startSymbol.children == null || startSymbol.children.length == 0)
      Errors.D01.thro(); // not well-formed
    if (! (startSymbol.children[0] instanceof Nonterminal))
      Errors.D06.thro(); // not exactly one element
    Nonterminal nonterminal = (Nonterminal) startSymbol.children[0];
    if (nonterminal.isAttribute)
      Errors.D05.thro(); // attribute as root
    if (startSymbol.children.length != 1)
      Errors.D06.thro(); // not exactly one element

    if (thread.isAmbiguous || isVersionMismatch) {
      nonterminal.addChildren(attribute("xmlns:ixml", "http://invisiblexml.org/NS"));
      List<String> state = new ArrayList<>();
      if (thread.isAmbiguous)
        state.add("ambiguous");
//    if (...)
//        state.add("stalled");
      if (isVersionMismatch)
        state.add("version-mismatch");
      nonterminal.addChildren(attribute("ixml:state", String.join(" ", state)));
    }

    b.serialize(s);
    String result = w.toString();

    if (currentOptions.contains(Option.TIMING)) {
      long t1 = System.currentTimeMillis();
      System.err.println("        ixml parsing time: " + (t1 - t0) + " msec");
    }
    return result;
  }

  private Nonterminal attribute(String name, String value) {
    Nonterminal attribute = new Nonterminal(
        name,
        value.codePoints().mapToObj(Terminal::new).toArray(Symbol[]::new));
    attribute.setAttribute(true);
    return attribute;
  }

  private static class StackNode {
    public int state;
    public StackNode link;

    public StackNode(int state, StackNode link) {
      this.state = state;
      this.link = link;
    }

    @Override
    public boolean equals(Object obj) {
      StackNode lhs = this;
      StackNode rhs = (StackNode) obj;
      while (lhs != null && rhs != null) {
        if (lhs == rhs) return true;
        if (lhs.state != rhs.state) return false;
        lhs = lhs.link;
        rhs = rhs.link;
      }
      return lhs == rhs;
    }
  }

  private abstract static class DeferredEvent {
    public DeferredEvent link;
    public int queueSize;
    public int begin;
    public int end;

    public DeferredEvent(DeferredEvent link, int begin, int end) {
      this.link = link;
      this.queueSize = link == null ? 0 : link.queueSize + 1;
      this.begin = begin;
      this.end = end;
    }

    public abstract void execute(BottomUpEventHandler eventHandler);

    public void release(BottomUpEventHandler eventHandler) {
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
        current.execute(eventHandler);
        current = current.link;
      }
      while (current != null);
    }

    public void show(BottomUpEventHandler eventHandler) {
      Stack<DeferredEvent> stack = new Stack<>();
      for (DeferredEvent current = this; current != null; current = current.link) {
        stack.push(current);
      }
      while (! stack.isEmpty()) {
        stack.pop().execute(eventHandler);
      }
    }
  }

  public static class TerminalEvent extends DeferredEvent {
    private int codepoint;

    public TerminalEvent(DeferredEvent link, int codepoint) {
      super(link, 0, 0);
      this.codepoint = codepoint;
    }

    @Override
    public void execute(BottomUpEventHandler eventHandler) {
      eventHandler.terminal(codepoint);
    }
  }

  public static class NonterminalEvent extends DeferredEvent {
    public ReduceArgument reduceArgument;

    public NonterminalEvent(DeferredEvent link, ReduceArgument reduceArgument) {
      super(link, 0, 0);
      this.reduceArgument = reduceArgument;
    }

    @Override
    public void execute(BottomUpEventHandler eventHandler) {
      eventHandler.nonterminal(reduceArgument);
    }
  }

  private ParsingThread parse() throws ParseException {
    Queue<ParsingThread> currentThreads = new LinkedList<>();
    Queue<ParsingThread> otherThreads = new PriorityQueue<>();
    ParsingThread thread = new ParsingThread();
    int pos = 0;
    boolean stalled = false;

    for (;;) {

      while (thread.equals(otherThreads.peek())) {
        if (trace)
          writeTrace("  <parse thread=\"" + thread.id + "\" offset=\"" + thread.e0 + "\" state=\"" + thread.state + "\" action=\"discard\"/>\n");
        ParsingThread t = otherThreads.remove();
        if (t.deferredEvent == null || t.deferredEvent.queueSize < thread.deferredEvent.queueSize)
          thread = t;
        thread.isAmbiguous = true;
        t = otherThreads.peek();
      }

      boolean isUnambiguous = otherThreads.isEmpty();

      if (isUnambiguous && thread.deferredEvent != null) {
        thread.deferredEvent.release(eventHandler);
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
            otherThreads.add(thread);
            otherThreads.add(new ParsingThread(thread, forks[2 * fork + 1]));
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
          otherThreads.add(thread);
        }
        else if (otherThreads.isEmpty() && currentThreads.isEmpty()) {
          throw new ParseException(thread.b1, thread.e1, thread.state, thread.l1, stalled);
        }
      }
      while((thread = currentThreads.poll()) != null);

      thread = otherThreads.remove();
      if (thread.e0 > pos)
        pos = thread.e0;
    }
  }

  private BottomUpEventHandler eventHandler;
  private String input = null;
  private int size = 0;
  private int maxId = 0;
  private Writer err = new OutputStreamWriter(System.err, StandardCharsets.UTF_8);
  private boolean trace;

  private enum Status {
    PARSING,
    ACCEPTED,
    ERROR,
  };

  private class ParsingThread implements Comparable<ParsingThread> {
    private byte[] forkCount;
    public Status status;
    public StackNode stack;
    public int state;
    public int action;
    public DeferredEvent deferredEvent;
    public int id;
    public boolean isAmbiguous;
    public int group;

    private int b0, e0;
    private int b1, e1;
    private int c1, l1;

    private int begin;
    private int end;

    public ParsingThread() {
      forkCount = new byte[forks.length / 2];
      b0 = 0;
      e0 = 0;
      b1 = 0;
      e1 = 0;
      l1 = 0;
      end = 0;
      maxId = 0;
      id = maxId;
      isAmbiguous = false;
      status = Status.PARSING;
      deferredEvent = null;
      stack = new StackNode(-1, null);
      state = 0;
      action = predict(state);
      group = id;
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
      end = other.end;
      isAmbiguous = other.isAmbiguous;
      group = other.group;
    }

    @Override
    public int compareTo(ParsingThread other) {
      if (status != other.status)
        return status == Status.ACCEPTED ? 1 : -1;
      int comp = e0 - other.e0;
      if (comp != 0)
        return comp;
      comp = group - other.group;
      if (comp != 0)
        return comp;
      return other.id - id;
    }

    @Override
    public boolean equals(Object obj) {
      ParsingThread other = (ParsingThread) obj;
      if (other == null) return false;
      if (status != other.status) return false;
      if (b1 != other.b1) return false;
      if (e1 != other.e1) return false;
      if (l1 != other.l1) return false;
      if (state != other.state) return false;
      if (action != other.action) return false;
      if (! stack.equals(other.stack)) return false;
      return true;
    }

    public int parse(boolean isUnambiguous) throws ParseException {
      int nonterminalId = -1;
      int pos = isUnambiguous ? Integer.MAX_VALUE : e0;
      for (;;) {
        if (trace) {
          writeTrace("  <parse thread=\"" + id + "\" offset=\"" + e0 + "\" state=\"" + state + "\" input=\"");
          if (nonterminalId >= 0) {
            writeTrace(xmlEscape(nonterminal[nonterminalId]));
            if (l1 != 0)
              writeTrace(" ");
          }
          writeTrace(xmlEscape(lookaheadString()) + "\" action=\"");
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
            if (eventHandler != null) {
              if (isUnambiguous) {
                eventHandler.terminal(c1);
              }
              else {
                deferredEvent = new TerminalEvent(deferredEvent, c1);
              }
            }
            stack = new StackNode(state, stack);
            b0 = b1;
            e0 = e1;
            c1 = -1;
            l1 = 0;
          }
          else {
            stack = new StackNode(state, stack);
          }
          state = shift;
        }

        if (reduce < 0)
        {
          if (trace)
            writeTrace("\"/>\n");
          action = predict(state);
          if (e0 > pos)
            return -1;
          nonterminalId = -1;
        }
        else
        {
          ReduceArgument reduceArgument = reduceArguments[reduce];
          int symbols = reduceArgument.getMarks().length;
          nonterminalId = reduceArgument.getNonterminalId();
          if (trace) {
            if (shift >= 0)
              writeTrace(" ");
            writeTrace("reduce\" nonterminal=\"" + xmlEscape(nonterminal[nonterminalId]) + "\" count=\"" + symbols + "\"/>\n");
          }
          if (symbols > 0)
          {
            for (int i = 1; i < symbols; i++)
            {
              stack = stack.link;
            }
            state = stack.state;
            stack = stack.link;
          }
          if (eventHandler != null)
          {
            if (isUnambiguous)
            {
              eventHandler.nonterminal(reduceArgument);
            }
            else
            {
              deferredEvent = new NonterminalEvent(deferredEvent, reduceArgument);
            }
          }
          action = nonterminalTransitions.get(state * numberOfNonterminals + nonterminalId);
        }
      }
    }

    private String lookaheadString()
    {
      String result = "";
      if (l1 > 0)
      {
        result += terminal[l1].shortName();
      }
      return result;
    }

    public int predict(int state) {
      if (l1 == 0) {
        l1 = match();
        b1 = begin;
        e1 = end;
      }
      return l1 < 0
           ? 0
           : terminalTransitions.get(state * numberOfTokens + l1);
    }

    private int match() {
      if (trace)
        writeTrace("  <tokenize thread=\"" + id + "\" offset=\"" + end + "\"");

      begin = end;
      final int charclass;
      if (end >= size) {
        c1 = -1;
        charclass = 0;
      }
      else {
        c1 = input.charAt(end++);
        if (c1 < 0x80) {
          if (trace)
            if (c1 >= 32 && c1 <= 126)
              writeTrace(" char=\"" + xmlEscape(String.valueOf((char) c1)) + "\"");
          charclass = asciiMap[c1];
        }
        else if (c1 < 0xd800) {
          charclass = bmpMap.get(c1);
        }
        else
        {
          if (c1 < 0xdc00) {
            int lowSurrogate = end < size ? input.charAt(end) : 0;
            if (lowSurrogate >= 0xdc00 && lowSurrogate < 0xe000) {
              ++end;
              c1 = ((c1 & 0x3ff) << 10) + (lowSurrogate & 0x3ff) + 0x10000;
            }
          }

          final var smpMapSize = smpMap.length / 3;
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
            writeTrace(" status=\"fail\" end=\"" + end + "\"/>\n");
          end = begin;
          return -1;
        }
      }

      if (trace) {
        writeTrace(" class=\"" + charclass + "\"");
        writeTrace(" status=\"success\" result=\"");
        writeTrace(xmlEscape(terminal[charclass].shortName()));
        writeTrace("\" end=\"" + end + "\"/>\n");
      }
      return charclass;
    }
  }

  private static String xmlEscape(String s)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); ++i)
    {
      char c = s.charAt(i);
      switch (c)
      {
      case '<': sb.append("&lt;"); break;
      case '"': sb.append("&quot;"); break;
      case '&': sb.append("&amp;"); break;
      default : sb.append(c); break;
      }
    }
    return sb.toString();
  }

  public void setTraceWriter(Writer w) {
    err = w;
  }

  private void writeTrace(String content)
  {
    try {
      err.write(content);
    }
    catch (IOException e) {
      throw new BlitzException(e);
    }
  }

  private String[] getTokenSet(int tokenSetId)
  {
    List<String> expected = new ArrayList<>();
    BitSet tokens = expectedTokens[tokenSetId];
    for (int i = tokens.nextSetBit(0); i >= 0; i = tokens.nextSetBit(i + 1)) {
      expected.add(terminal[i].shortName());
    }
    return expected.toArray(String[]::new);
  }

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
}
