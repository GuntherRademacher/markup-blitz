package de.bottlecaps.markup.blitz.parser;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import de.bottlecaps.markup.blitz.BlitzException;
import de.bottlecaps.markup.blitz.BlitzOption;
import de.bottlecaps.markup.blitz.character.RangeSet;
import de.bottlecaps.markup.blitz.grammar.Mark;
import de.bottlecaps.markup.blitz.transform.CompressedMap;

public class Parser
{
  private static class ParseException extends Exception
  {
    private static final long serialVersionUID = 1L;
    private int begin, end, offending, expected, state;
    private boolean ambiguousInput;
    private ParseTreeBuilder ambiguityDescriptor;

    public ParseException(int b, int e, int s, int o, int x)
    {
      begin = b;
      end = e;
      state = s;
      offending = o;
      expected = x;
      ambiguousInput = false;
    }

    public ParseException(int b, int e, ParseTreeBuilder ambiguityDescriptor)
    {
      this(b, e, 1, -1, -1);
      ambiguousInput = true;
      this.ambiguityDescriptor = ambiguityDescriptor;
    }

    @Override
    public String getMessage()
    {
      return ambiguousInput
           ? "ambiguous input"
           : offending < 0
           ? "lexical analysis failed"
           : "syntax error";
    }

    public void serialize(EventHandler eventHandler)
    {
      ambiguityDescriptor.serialize(eventHandler);
    }

    public int getBegin() {return begin;}
    public int getEnd() {return end;}
    public int getState() {return state;}
    public int getOffending() {return offending;}
    public int getExpected() {return expected;}
    public boolean isAmbiguousInput() {return ambiguousInput;}
  }

  public interface EventHandler {
    public void startNonterminal(String name);
    public void startAttribute(String name);
    public void endAttribute(String name);
    public void endNonterminal(String name);
    public void terminal(String content);
  }

  public static abstract class Symbol {
    public abstract void send(EventHandler e);
  }

  public class Terminal extends Symbol {
    public String content;

    public Terminal(int codepoint) {
      content = Character.toString(codepoint);
    }

    @Override
    public void send(EventHandler e) {
      e.terminal(content);
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
          c.send(e);
        e.endAttribute(name);
      }
      else {
        e.startNonterminal(name);
        for (Symbol c : children)
          if (c instanceof Nonterminal && ((Nonterminal) c).isAttribute)
            c.send(e);
        for (Symbol c : children)
          if (! (c instanceof Nonterminal) || ! ((Nonterminal) c).isAttribute)
            c.send(e);
        e.endNonterminal(name);
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
        if (indent)
        {
          writeOutput("\n");
          for (int i = 0; i < depth; ++i)
          {
            writeOutput("  ");
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
                writeOutput("  ");
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
    public void terminal(String content) {
      if (! content.isEmpty()) {
        if (attributeLevel > 0) {
          writeOutput(content
              .replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;")
              .replace("\"", "&quot;")
              .replace("\r", "&#xA;"));
        }
        else {
          if (delayedTag != null) {
            writeOutput(">");
            delayedTag = null;
          }
          writeOutput(content
                           .replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\r", "&#xA;"));
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
          switch (marks[i - top - 1]) {
          case NODE:
            if (children == null)
              children = new ArrayList<>();
            children.add(symbol);
            break;
          case DELETE:
            break;
          case ATTRIBUTE:
            throw new IxmlException("cannot promote a terminal to an attribute");
          case NONE:
            throw new IllegalStateException();
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

      push(new Nonterminal(nonterminal[reduceArgument.getNonterminalId()],
          children == null ? new Symbol[0] : children.toArray(Symbol[]::new)
      ));
    }

    @Override
    public void terminal(int codepoint) {
      push(new Terminal(codepoint));
    }

    public void serialize(EventHandler e) {
      for (int i = 0; i <= top; ++i) {
        stack[i].send(e);
      }
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
      Set<BlitzOption> defaultOptions,
      int[] asciiMap, CompressedMap bmpMap, int[] smpMap,
      CompressedMap terminalTransitions, int numberOfTokens,
      CompressedMap nonterminalTransitions, int numberOfNonterminals,
      ReduceArgument[] reduceArguments,
      String[] nonterminal,
      RangeSet[] terminal,
      int[] forks)
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
  }

  public RangeSet getOffendingToken(ParseException e) {
    return e.getOffending() < 0 ? null : terminal[e.getOffending()];
  }

  public String[] getExpectedTokenSet(ParseException e) {
    String[] expected = {};
    if (e.getExpected() >= 0) {
      expected = new String[]{terminal[e.getExpected()].shortName()};
    }
    else if (! e.isAmbiguousInput()) {
      expected = getTokenSet(- e.getState());
    }
    return expected;
  }

  public String getErrorMessage(ParseException e) {
    String message = e.getMessage();
    if (e.isAmbiguousInput()) {
      message += "\n";
    }
    else {
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
    }
    String prefix = input.subSequence(0, e.getBegin()).toString();
    int line = prefix.replaceAll("[^\n]", "").length() + 1;
    int column = prefix.length() - prefix.lastIndexOf('\n');
    return message
         + "at line " + line + ", column " + column + ":\n..."
         + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64))
         + "...";
  }

  public String parse(String string, BlitzOption... options) throws BlitzException {
    Set<BlitzOption> currentOptions = options.length == 0
        ? defaultOptions
        : Set.of(options);

    boolean indent = currentOptions.contains(BlitzOption.INDENT);
    StringWriter w = new StringWriter(string.length());
    XmlSerializer s = new XmlSerializer(w, indent);
    ParseTreeBuilder b = new ParseTreeBuilder();

    trace = currentOptions.contains(BlitzOption.TRACE);
    if (trace)
      writeTrace("<?xml version=\"1.0\" encoding=\"UTF-8\"?" + ">\n<trace>\n");

    eventHandler = b;
    input = string;
    size = string.length();
    maxId = 0;
    thread = new ParsingThread();
    thread.reset(0, 0, 0);

    try {
      thread = parse(0, eventHandler, thread);
    }
    catch (ParseException pe) {
      if (pe.isAmbiguousInput()) {
        pe.serialize(s);
        w.write("\n");
      }
      throw new BlitzException("Failed to parse input:\n" + getErrorMessage(pe));
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

    b.serialize(s);
    return w.toString();
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
    public int begin;
    public int end;

    public DeferredEvent(DeferredEvent link, int begin, int end) {
      this.link = link;
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

  private static final int PARSING = 0;
  private static final int ACCEPTED = 1;
  private static final int ERROR = 2;

  private ParsingThread parse(int target, BottomUpEventHandler eventHandler, ParsingThread thread) throws ParseException {
    PriorityQueue<ParsingThread> threads = thread.open(0, eventHandler, target);
    for (;;) {
      thread = threads.poll();
      if (thread.accepted) {
        ParsingThread other = null;
        while (! threads.isEmpty()) {
          other = threads.poll();
          if (thread.e0 < other.e0)
          {
            thread = other;
            other = null;
          }
        }
        if (other != null) {
          rejectAmbiguity(0, thread.e0, thread.deferredEvent, other.deferredEvent);
        }
        if (thread.deferredEvent != null) {
          thread.deferredEvent.release(eventHandler);
          thread.deferredEvent = null;
        }
        return thread;
      }

      if (! threads.isEmpty()) {
        if (threads.peek().equals(thread)) {
          rejectAmbiguity(0, thread.e0, thread.deferredEvent, threads.peek().deferredEvent);
        }
      }
      else {
        if (thread.deferredEvent != null) {
          thread.deferredEvent.release(eventHandler);
          thread.deferredEvent = null;
        }
      }

      int status;
      for (;;) {
        if ((status = thread.parse()) != PARSING) break;
        if (! threads.isEmpty()) break;
      }

      if (status != ERROR) {
        threads.offer(thread);
      }
      else if (threads.isEmpty()) {
        throw new ParseException(thread.b1,
                                 thread.e1,
                                 TOKENSET[thread.state] + 1,
                                 thread.l1,
                                 -1
                                );
      }
    }
  }

  private void rejectAmbiguity(int begin, int end, DeferredEvent first, DeferredEvent second) {
    throw new UnsupportedOperationException();
//    ParseTreeBuilder treeBuilder = new ParseTreeBuilder();
//    treeBuilder.reset(input);
//    second.show(treeBuilder);
//    treeBuilder.nonterminal("ALTERNATIVE", treeBuilder.stack[0].begin, treeBuilder.stack[treeBuilder.top].end, treeBuilder.top + 1);
//    Symbol secondTree = treeBuilder.pop(1)[0];
//    first.show(treeBuilder);
//    treeBuilder.nonterminal("ALTERNATIVE", treeBuilder.stack[0].begin, treeBuilder.stack[treeBuilder.top].end, treeBuilder.top + 1);
//    treeBuilder.push(secondTree);
//    treeBuilder.nonterminal("AMBIGUOUS", treeBuilder.stack[0].begin, treeBuilder.stack[treeBuilder.top].end, 2);
//    throw new ParseException(begin, end, treeBuilder);
  }

  private ParsingThread thread = new ParsingThread();
  private BottomUpEventHandler eventHandler;
  private String input = null;
  private int size = 0;
  private int maxId = 0;
  private Writer err = new OutputStreamWriter(System.err, StandardCharsets.UTF_8);

  private class ParsingThread implements Comparable<ParsingThread> {
    public PriorityQueue<ParsingThread> threads;
    public boolean accepted;
    public StackNode stack;
    public int state;
    public int action;
    public int target;
    public DeferredEvent deferredEvent;
    public int id;

    public PriorityQueue<ParsingThread> open(int initialState, BottomUpEventHandler eh, int t) {
      accepted = false;
      target = t;
      eventHandler = eh;
      deferredEvent = null;
      stack = new StackNode(-1, null);
      state = initialState;
      action = predict(initialState);
      threads = new PriorityQueue<>();
      threads.offer(this);
      return threads;
    }

    public ParsingThread copy(ParsingThread other, int action) {
      this.action = action;
      accepted = other.accepted;
      target = other.target;
      eventHandler = other.eventHandler;
      deferredEvent = other.deferredEvent;
      id = ++maxId;
      threads = other.threads;
      state = other.state;
      stack = other.stack;
      b0 = other.b0;
      e0 = other.e0;
      c1 = other.c1;
      l1 = other.l1;
      b1 = other.b1;
      e1 = other.e1;
      end = other.end;
      return this;
    }

    @Override
    public int compareTo(ParsingThread other) {
      if (accepted != other.accepted)
        return accepted ? 1 : -1;
      int comp = e0 - other.e0;
      return comp == 0 ? id - other.id : comp;
    }

    @Override
    public boolean equals(Object obj) {
      ParsingThread other = (ParsingThread) obj;
      if (accepted != other.accepted) return false;
      if (b1 != other.b1) return false;
      if (e1 != other.e1) return false;
      if (l1 != other.l1) return false;
      if (state != other.state) return false;
      if (action != other.action) return false;
      if (! stack.equals(other.stack)) return false;
      return true;
    }

    public int parse() throws ParseException {
      int nonterminalId = -1;
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
          threads.offer(new ParsingThread().copy(this, forks[argument]));
          action = forks[argument + 1];
          return PARSING;

        case 5: // ACCEPT
          if (trace)
            writeTrace("accept\"/>\n");
          accepted = true;
          action = 0;
          return ACCEPTED;

        default: // ERROR
          if (trace)
            writeTrace("fail\"/>\n");
          return ERROR;
        }

        if (shift >= 0) {
          if (trace)
            writeTrace("shift");
          if (nonterminalId < 0) {
            if (eventHandler != null) {
              if (isUnambiguous()) {
                eventHandler.terminal(c1);
              }
              else {
                deferredEvent = new TerminalEvent(deferredEvent, c1);
              }
            }
            stack = new StackNode(state, stack);
            consume(l1);
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
          return PARSING;
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
            if (isUnambiguous())
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

    public boolean isUnambiguous()
    {
      return threads.isEmpty();
    }

    public final void reset(int l, int b, int e)
    {
              b0 = b; e0 = b;
      l1 = l; b1 = b; e1 = e;
      end = e;
      maxId = 0;
      id = maxId;
    }

    private void consume(int t) throws ParseException
    {
      if (l1 == t)
      {
        b0 = b1; e0 = e1; c1 = -1; l1 = 0;
      }
      else
      {
        error(b1, e1, 0, l1, t);
      }
    }

    private int error(int b, int e, int s, int l, int t) throws ParseException
    {
      throw new ParseException(b, e, s, l, t);
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

    private int         b0, e0;
    private int c1, l1, b1, e1;
    private BottomUpEventHandler eventHandler = null;

    private int begin = 0;
    private int end = 0;

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
      if (trace) {
        writeTrace("  <tokenize thread=\"" + id + "\">\n");
        writeTrace("    <next");
        writeTrace(" offset=\"" + end + "\"");
      }

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
      }

      if (trace) {
        if (c1 >= 0)
          writeTrace(" codepoint=\"" + c1 + "\"");
        writeTrace(" class=\"" + charclass + "\"");
        writeTrace("/>\n");
      }

      if (charclass < 0) {
        if (trace) {
          writeTrace("    <fail begin=\"" + begin + "\" end=\"" + end + "\"/>\n");
          writeTrace("  </tokenize>\n");
        }
        end = begin;
        return -1;
      }

      if (trace) {
        writeTrace("    <done result=\"" + xmlEscape(terminal[charclass].shortName()) + "\" begin=\"" + begin + "\" end=\"" + end + "\"/>\n");
        writeTrace("  </tokenize>\n");
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
    int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 31;
    for (int i = 0; i < 31; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 27 + s - 1;
      int f = EXPECTED[i0];
      for ( ; f != 0; f >>>= 1, ++j)
      {
        if ((f & 1) != 0)
        {
          expected.add(terminal[j].shortName());
        }
      }
    }
    return expected.toArray(new String[]{});
  }

  private final Set<BlitzOption> defaultOptions;
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

  private boolean trace;

  private static final int[] INITIAL =
  {
    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 712, 9, 10, 11, 12, 13, 14, 15, 16, 17, 722, 723, 20, 725, 22, 727, 728, 25, 26, 27
  };

  private static final int[] EXPECTED =
  {
    /*  0 */ 2048, 536870912, 16384, 65536, 131072, 524288, 75497472, 20971520, 20971528, 20971552, 20971776, 289406976,
    /* 12 */ 1094713344, 1094713352, 289407008, 1094713376, 75497552, 1438646304, 2034237472, 746600452, 2067791904,
    /* 21 */ 898184, 2109734944, 2143289376, 97821256, 366256712, 2141192190
  };

  private static final int[] TOKENSET =
  {
    /*  0 */ 24, 24, 7, 6, 13, 25, 26, 7, 20, 23, 8, 15, 12, 24, 14, 11, 26, 18, 6, 23, 10, 9, 12, 9, 11, 21, 16, 22,
    /* 28 */ 10, 8, 24, 19, 6, 24, 8, 24, 19, 17, 24, 19, 19, 0, 3, 5, 2, 5, 2, 4, 1, 2, 1
  };

}
