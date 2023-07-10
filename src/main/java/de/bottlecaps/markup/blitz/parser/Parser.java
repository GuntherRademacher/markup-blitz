// This file was generated on Sun Jul 9, 2023 22:57 (UTC+02) by REx v5.57 which is Copyright (c) 1979-2023 by Gunther Rademacher <grd@gmx.net>
// REx command line: -glalr 1 -java -tree -name de.bottlecaps.markup.blitz.parser.Parser ..\..\..\..\..\..\..\..\..\rex-parser-benchmark\src\main\rex\json-scannerless-bnf.ebnf -trace

package de.bottlecaps.markup.blitz.parser;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.PriorityQueue;

public class Parser
{
  public static class ParseException extends RuntimeException
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

  public interface EventHandler
  {
    public void reset(CharSequence string);
    public void startNonterminal(String name, int begin);
    public void endNonterminal(String name, int end);
    public void terminal(String name, int begin, int end);
    public void whitespace(int begin, int end);
  }

  public static class TopDownTreeBuilder implements EventHandler
  {
    private CharSequence input = null;
    private Nonterminal[] stack = new Nonterminal[64];
    private int top = -1;

    @Override
    public void reset(CharSequence input)
    {
      this.input = input;
      top = -1;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      Nonterminal nonterminal = new Nonterminal(name, begin, begin, new Symbol[0]);
      if (top >= 0) addChild(nonterminal);
      if (++top >= stack.length) stack = Arrays.copyOf(stack, stack.length << 1);
      stack[top] = nonterminal;
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      stack[top].end = end;
      if (top > 0) --top;
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      addChild(new Terminal(name, begin, end));
    }

    @Override
    public void whitespace(int begin, int end)
    {
    }

    private void addChild(Symbol s)
    {
      Nonterminal current = stack[top];
      current.children = Arrays.copyOf(current.children, current.children.length + 1);
      current.children[current.children.length - 1] = s;
    }

    public void serialize(EventHandler e)
    {
      e.reset(input);
      stack[0].send(e);
    }
  }

  public static abstract class Symbol
  {
    public String name;
    public int begin;
    public int end;

    protected Symbol(String name, int begin, int end)
    {
      this.name = name;
      this.begin = begin;
      this.end = end;
    }

    public abstract void send(EventHandler e);
  }

  public static class Terminal extends Symbol
  {
    public Terminal(String name, int begin, int end)
    {
      super(name, begin, end);
    }

    @Override
    public void send(EventHandler e)
    {
      e.terminal(name, begin, end);
    }
  }

  public static class Nonterminal extends Symbol
  {
    public Symbol[] children;

    public Nonterminal(String name, int begin, int end, Symbol[] children)
    {
      super(name, begin, end);
      this.children = children;
    }

    @Override
    public void send(EventHandler e)
    {
      e.startNonterminal(name, begin);
      int pos = begin;
      for (Symbol c : children)
      {
        if (pos < c.begin) e.whitespace(pos, c.begin);
        c.send(e);
        pos = c.end;
      }
      if (pos < end) e.whitespace(pos, end);
      e.endNonterminal(name, end);
    }
  }

  public interface BottomUpEventHandler
  {
    public void reset(CharSequence string);
    public void nonterminal(String name, int begin, int end, int count);
    public void terminal(String name, int begin, int end);
  }

  public static class XmlSerializer implements EventHandler
  {
    private CharSequence input;
    private String delayedTag;
    private Writer out;
    private boolean indent;
    private boolean hasChildElement;
    private int depth;

    public XmlSerializer(Writer w, boolean indent)
    {
      input = null;
      delayedTag = null;
      out = w;
      this.indent = indent;
    }

    @Override
    public void reset(CharSequence string)
    {
      writeOutput("<?xml version=\"1.0\" encoding=\"UTF-8\"?" + ">");
      input = string;
      delayedTag = null;
      hasChildElement = false;
      depth = 0;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      if (delayedTag != null)
      {
        writeOutput("<");
        writeOutput(delayedTag);
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
      hasChildElement = false;
      ++depth;
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      --depth;
      if (delayedTag != null)
      {
        delayedTag = null;
        writeOutput("<");
        writeOutput(name);
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

    @Override
    public void terminal(String name, int begin, int end)
    {
      if (name.charAt(0) == '\'')
      {
        name = "TOKEN";
      }
      startNonterminal(name, begin);
      characters(begin, end);
      endNonterminal(name, end);
    }

    @Override
    public void whitespace(int begin, int end)
    {
      characters(begin, end);
    }

    private void characters(int begin, int end)
    {
      if (begin < end)
      {
        if (delayedTag != null)
        {
          writeOutput("<");
          writeOutput(delayedTag);
          writeOutput(">");
          delayedTag = null;
        }
        writeOutput(input.subSequence(begin, end)
                         .toString()
                         .replace("&", "&amp;")
                         .replace("<", "&lt;")
                         .replace(">", "&gt;"));
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
        throw new RuntimeException(e);
      }
    }
  }

  public static class ParseTreeBuilder implements BottomUpEventHandler
  {
    private CharSequence input;
    public Symbol[] stack = new Symbol[64];
    public int top = -1;

    @Override
    public void reset(CharSequence input)
    {
      this.input = input;
      top = -1;
    }

    @Override
    public void nonterminal(String name, int begin, int end, int count)
    {
      if (count > top + 1)
      {
        Symbol[] content = pop(top + 1);
        nonterminal("UNAMBIGUOUS", begin, content.length == 0 ? end : content[0].begin, 0);
        for (Symbol symbol : content)
        {
          push(symbol);
        }
        count = top + 1;
      }
      push(new Nonterminal(name, begin, end, pop(count)));
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      push(new Terminal(name, begin, end));
    }

    public void serialize(EventHandler e)
    {
      e.reset(input);
      for (int i = 0; i <= top; ++i)
      {
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

    public Symbol[] pop(int count)
    {
      top -= count;
      return Arrays.copyOfRange(stack, top + 1, top + count + 1);
    }
  }

  public Parser(CharSequence string, BottomUpEventHandler t)
  {
    initialize(string, t);
  }

  public void initialize(CharSequence source, BottomUpEventHandler parsingEventHandler)
  {
    eventHandler = parsingEventHandler;
    input = source;
    size = source.length();
    maxId = 0;
    thread = new ParsingThread();
    thread.reset(0, 0, 0);
  }

  public static String getOffendingToken(ParseException e)
  {
    return e.getOffending() < 0 ? null : TOKEN[e.getOffending()];
  }

  public static String[] getExpectedTokenSet(ParseException e)
  {
    String[] expected = {};
    if (e.getExpected() >= 0)
    {
      expected = new String[]{TOKEN[e.getExpected()]};
    }
    else if (! e.isAmbiguousInput())
    {
      expected = getTokenSet(- e.getState());
    }
    return expected;
  }

  public String getErrorMessage(ParseException e)
  {
    String message = e.getMessage();
    if (e.isAmbiguousInput())
    {
      message += "\n";
    }
    else
    {
      String[] tokenSet = getExpectedTokenSet(e);
      String found = getOffendingToken(e);
      int size = e.getEnd() - e.getBegin();
      message += (found == null ? "" : ", found " + found)
              + "\nwhile expecting "
              + (tokenSet.length == 1 ? tokenSet[0] : java.util.Arrays.toString(tokenSet))
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

  public void parse_json()
  {
    thread = parse(0, 0, eventHandler, thread);
    flushTrace();
  }

  private static class StackNode
  {
    public int state;
    public int pos;
    public StackNode link;

    public StackNode(int state, int pos, StackNode link)
    {
      this.state = state;
      this.pos = pos;
      this.link = link;
    }

    @Override
    public boolean equals(Object obj)
    {
      StackNode lhs = this;
      StackNode rhs = (StackNode) obj;
      while (lhs != null && rhs != null)
      {
        if (lhs == rhs) return true;
        if (lhs.state != rhs.state) return false;
        if (lhs.pos != rhs.pos) return false;
        lhs = lhs.link;
        rhs = rhs.link;
      }
      return lhs == rhs;
    }

  }

  private abstract static class DeferredEvent
  {
    public DeferredEvent link;
    public String name;
    public int begin;
    public int end;

    public DeferredEvent(DeferredEvent link, String name, int begin, int end)
    {
      this.link = link;
      this.name = name;
      this.begin = begin;
      this.end = end;
    }

    public abstract void execute(BottomUpEventHandler eventHandler);

    public void release(BottomUpEventHandler eventHandler)
    {
      DeferredEvent current = this;
      DeferredEvent predecessor = current.link;
      current.link = null;
      while (predecessor != null)
      {
        DeferredEvent next = predecessor.link;
        predecessor.link = current;
        current = predecessor;
        predecessor = next;
      }
      do
      {
        current.execute(eventHandler);
        current = current.link;
      }
      while (current != null);
    }

    public void show(BottomUpEventHandler eventHandler)
    {
      java.util.Stack<DeferredEvent> stack = new java.util.Stack<>();
      for (DeferredEvent current = this; current != null; current = current.link)
      {
        stack.push(current);
      }
      while (! stack.isEmpty())
      {
        stack.pop().execute(eventHandler);
      }
    }
  }

  public static class TerminalEvent extends DeferredEvent
  {
    public TerminalEvent(DeferredEvent link, String name, int begin, int end)
    {
      super(link, name, begin, end);
    }

    @Override
    public void execute(BottomUpEventHandler eventHandler)
    {
      eventHandler.terminal(name, begin, end);
    }

    @Override
    public String toString()
    {
      return "terminal(" + name + ", " + begin + ", " + end + ")";
    }
  }

  public static class NonterminalEvent extends DeferredEvent
  {
    public int count;

    public NonterminalEvent(DeferredEvent link, String name, int begin, int end, int count)
    {
      super(link, name, begin, end);
      this.count = count;
    }

    @Override
    public void execute(BottomUpEventHandler eventHandler)
    {
      eventHandler.nonterminal(name, begin, end, count);
    }

    @Override
    public String toString()
    {
      return "nonterminal(" + name + ", " + begin + ", " + end + ", " + count + ")";
    }
  }

  private static final int PARSING = 0;
  private static final int ACCEPTED = 1;
  private static final int ERROR = 2;

  private ParsingThread parse(int target, int initialState, BottomUpEventHandler eventHandler, ParsingThread thread)
  {
    PriorityQueue<ParsingThread> threads = thread.open(initialState, eventHandler, target);
    for (;;)
    {
      thread = threads.poll();
      if (thread.accepted)
      {
        ParsingThread other = null;
        while (! threads.isEmpty())
        {
          other = threads.poll();
          if (thread.e0 < other.e0)
          {
            thread = other;
            other = null;
          }
        }
        if (other != null)
        {
          rejectAmbiguity(thread.stack.pos, thread.e0, thread.deferredEvent, other.deferredEvent);
        }
        if (thread.deferredEvent != null)
        {
          thread.deferredEvent.release(eventHandler);
          thread.deferredEvent = null;
        }
        return thread;
      }

      if (! threads.isEmpty())
      {
        if (threads.peek().equals(thread))
        {
          rejectAmbiguity(thread.stack.pos, thread.e0, thread.deferredEvent, threads.peek().deferredEvent);
        }
      }
      else
      {
        if (thread.deferredEvent != null)
        {
          thread.deferredEvent.release(eventHandler);
          thread.deferredEvent = null;
        }
      }

      int status;
      for (;;)
      {
        if ((status = thread.parse()) != PARSING) break;
        if (! threads.isEmpty()) break;
      }

      if (status != ERROR)
      {
        threads.offer(thread);
      }
      else if (threads.isEmpty())
      {
        throw new ParseException(thread.b1,
                                 thread.e1,
                                 TOKENSET[thread.state] + 1,
                                 thread.l1,
                                 -1
                                );
      }
    }
  }

  private void rejectAmbiguity(int begin, int end, DeferredEvent first, DeferredEvent second)
  {
    ParseTreeBuilder treeBuilder = new ParseTreeBuilder();
    treeBuilder.reset(input);
    second.show(treeBuilder);
    treeBuilder.nonterminal("ALTERNATIVE", treeBuilder.stack[0].begin, treeBuilder.stack[treeBuilder.top].end, treeBuilder.top + 1);
    Symbol secondTree = treeBuilder.pop(1)[0];
    first.show(treeBuilder);
    treeBuilder.nonterminal("ALTERNATIVE", treeBuilder.stack[0].begin, treeBuilder.stack[treeBuilder.top].end, treeBuilder.top + 1);
    treeBuilder.push(secondTree);
    treeBuilder.nonterminal("AMBIGUOUS", treeBuilder.stack[0].begin, treeBuilder.stack[treeBuilder.top].end, 2);
    throw new ParseException(begin, end, treeBuilder);
  }

  private ParsingThread thread = new ParsingThread();
  private BottomUpEventHandler eventHandler;
  private CharSequence input = null;
  private int size = 0;
  private int maxId = 0;
  private Writer err;
  {
    try
    {
      err = new OutputStreamWriter(System.err, "UTF-8");
    }
    catch (UnsupportedEncodingException uee)
    {}
  }

  private class ParsingThread implements Comparable<ParsingThread>
  {
    public PriorityQueue<ParsingThread> threads;
    public boolean accepted;
    public StackNode stack;
    public int state;
    public int action;
    public int target;
    public DeferredEvent deferredEvent;
    public int id;

    public PriorityQueue<ParsingThread> open(int initialState, BottomUpEventHandler eh, int t)
    {
      accepted = false;
      target = t;
      eventHandler = eh;
      if (eventHandler != null)
      {
        eventHandler.reset(input);
      }
      deferredEvent = null;
      stack = new StackNode(-1, e0, null);
      state = initialState;
      action = predict(initialState);
      bw = e0;
      bs = e0;
      es = e0;
      threads = new PriorityQueue<>();
      threads.offer(this);
      return threads;
    }

    public ParsingThread copy(ParsingThread other, int action)
    {
      this.action = action;
      accepted = other.accepted;
      target = other.target;
      bs = other.bs;
      es = other.es;
      bw = other.bw;
      eventHandler = other.eventHandler;
      deferredEvent = other.deferredEvent;
      id = ++maxId;
      threads = other.threads;
      state = other.state;
      stack = other.stack;
      b0 = other.b0;
      e0 = other.e0;
      l1 = other.l1;
      b1 = other.b1;
      e1 = other.e1;
      end = other.end;
      return this;
    }

    @Override
    public int compareTo(ParsingThread other)
    {
      if (accepted != other.accepted)
        return accepted ? 1 : -1;
      int comp = e0 - other.e0;
      return comp == 0 ? id - other.id : comp;
    }

    @Override
    public boolean equals(Object obj)
    {
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

    public int parse()
    {
      int nonterminalId = -1;
      for (;;)
      {
        writeTrace("  <parse thread=\"" + id + "\" offset=\"" + e0 + "\" state=\"" + state + "\" input=\"");
        if (nonterminalId >= 0)
        {
          writeTrace(xmlEscape(NONTERMINAL[nonterminalId]));
          if (l1 != 0)
          {
            writeTrace(" ");
          }
        }
        writeTrace(xmlEscape(lookaheadString()) + "\" action=\"");
        int argument = action >> 7;
        int lookback = (action >> 3) & 15;
        int shift = -1;
        int reduce = -1;
        int symbols = -1;
        switch (action & 7)
        {
        case 1: // SHIFT
          shift = argument;
          break;

        case 2: // REDUCE
          reduce = argument;
          symbols = lookback;
          break;

        case 4: // SHIFT+REDUCE
          shift = state;
          reduce = argument;
          symbols = lookback + 1;
          break;

        case 6: // SHIFT_ACCEPT
          writeTrace("accept\"/>\n");
          accepted = true;
          action = 0;
          return ACCEPTED;

        case 7: // FORK
          writeTrace("fork\"/>\n");
          threads.offer(new ParsingThread().copy(this, APPENDIX[argument]));
          action = APPENDIX[argument + 1];
          return PARSING;

        default: // ERROR
          writeTrace("fail\"/>\n");
          return ERROR;
        }

        if (shift >= 0)
        {
          writeTrace("shift");
          if (nonterminalId < 0)
          {
            if (eventHandler != null)
            {
              if (isUnambiguous())
              {
                eventHandler.terminal(TOKEN[l1], b1, e1);
              }
              else
              {
                deferredEvent = new TerminalEvent(deferredEvent, TOKEN[l1], b1, e1);
              }
            }
            es = e1;
            stack = new StackNode(state, b1, stack);
            consume(l1);
          }
          else
          {
            stack = new StackNode(state, bs, stack);
          }
          state = shift;
        }

        if (reduce < 0)
        {
          writeTrace("\"/>\n");
          action = predict(state);
          return PARSING;
        }
        else
        {
          nonterminalId = reduce;
          if (shift >= 0)
          {
            writeTrace(" ");
          }
          writeTrace("reduce\" nonterminal=\"" + xmlEscape(NONTERMINAL[nonterminalId]) + "\" count=\"" + symbols + "\"/>\n");
          if (symbols > 0)
          {
            for (int i = 1; i < symbols; i++)
            {
              stack = stack.link;
            }
            state = stack.state;
            bs = stack.pos;
            stack = stack.link;
          }
          else
          {
            bs = b1;
            es = b1;
          }
          if (nonterminalId == target && stack.link == null)
          {
            bs = bw;
            es = b1;
            bw = b1;
          }
          if (eventHandler != null)
          {
            if (isUnambiguous())
            {
              eventHandler.nonterminal(NONTERMINAL[nonterminalId], bs, es, symbols);
            }
            else
            {
              deferredEvent = new NonterminalEvent(deferredEvent, NONTERMINAL[nonterminalId], bs, es, symbols);
            }
          }
          action = goTo(nonterminalId, state);
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

    private void consume(int t)
    {
      if (l1 == t)
      {
        b0 = b1; e0 = e1; l1 = 0;
      }
      else
      {
        error(b1, e1, 0, l1, t);
      }
    }

    private int error(int b, int e, int s, int l, int t)
    {
      flushTrace();
      throw new ParseException(b, e, s, l, t);
    }

    private String lookaheadString()
    {
      String result = "";
      if (l1 > 0)
      {
        result += TOKEN[l1];
      }
      return result;
    }

    private int     b0, e0;
    private int l1, b1, e1;
    private int bw, bs, es;
    private BottomUpEventHandler eventHandler = null;

    private int begin = 0;
    private int end = 0;

    public int predict(int dpi)
    {
      int d = dpi;
      if (l1 == 0)
      {
        l1 = match(TOKENSET[d]);
        b1 = begin;
        e1 = end;
      }
      if (l1 < 0)
        return 0;
      int j10 = (d << 5) + l1;
      int j11 = j10 >> 2;
      int action = CASEID[(j10 & 3) + CASEID[(j11 & 3) + CASEID[j11 >> 2]]];
      return action >> 1;
    }

    private int match(int tokenSetId)
    {
      writeTrace("  <tokenize thread=\"" + id + "\" tokenset=\"" + tokenSetId + "\">\n");

      begin = end;
      int current = end;
      int result = INITIAL[tokenSetId];
      int state = 0;

      writeTrace("    <next state=\"" + (result & 31) + "\"");
      for (int code = result & 31; code != 0; )
      {
        int charclass;
        int c0 = current < size ? input.charAt(current) : 0;
        writeTrace(" offset=\"" + current + "\"");
        ++current;
        if (c0 < 0x80)
        {
          if (c0 >= 32 && c0 <= 126)
          {
            writeTrace(" char=\"" + xmlEscape(String.valueOf((char) c0)) + "\"");
          }
          charclass = MAP0[c0];
        }
        else if (c0 < 0xd800)
        {
          int c1 = c0 >> 3;
          charclass = MAP1[(c0 & 7) + MAP1[(c1 & 15) + MAP1[c1 >> 4]]];
        }
        else
        {
          if (c0 < 0xdc00)
          {
            int c1 = current < size ? input.charAt(current) : 0;
            if (c1 >= 0xdc00 && c1 < 0xe000)
            {
              ++current;
              c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000;
            }
          }

          int lo = 0, hi = 314;
          for (int m = 157; ; m = (hi + lo) >> 1)
          {
            if (MAP2[m] > c0) {hi = m - 1;}
            else if (MAP2[315 + m] < c0) {lo = m + 1;}
            else {charclass = MAP2[630 + m]; break;}
            if (lo > hi) {charclass = 0; break;}
          }
        }
        writeTrace(" codepoint=\"" + c0 + "\" class=\"" + charclass + "\"");

        state = code;
        int i0 = (charclass << 5) + code - 1;
        code = TRANSITION[(i0 & 3) + TRANSITION[i0 >> 2]];

        if (code > 31)
        {
          result = code;
          writeTrace(" result=\"" + xmlEscape(TOKEN[((result >> 5) & 31) - 1]) + "\"");
          code &= 31;
          end = current;
        }
        writeTrace("/>\n");
        if (code != 0)
        {
          writeTrace("    <next state=\"" + code + "\"");
        }
      }

      result >>= 5;
      if (result == 0)
      {
        end = current - 1;
        int c1 = end < size ? input.charAt(end) : 0;
        if (c1 >= 0xdc00 && c1 < 0xe000)
        {
          --end;
        }
        writeTrace("    <fail begin=\"" + begin + "\" end=\"" + end + "\" state=\"" + state + "\"/>\n");
        writeTrace("  </tokenize>\n");
        end = begin;
        return -1;
      }

      if (end > size) end = size;
      writeTrace("    <done result=\"" + xmlEscape(TOKEN[(result & 31) - 1]) + "\" begin=\"" + begin + "\" end=\"" + end + "\"/>\n");
      writeTrace("  </tokenize>\n");
      return (result & 31) - 1;
    }

  }

  private static int goTo(int nonterminal, int state)
  {
    int i0 = 40 * state + nonterminal;
    int i1 = i0 >> 2;
    return GOTO[(i0 & 3) + GOTO[(i1 & 3) + GOTO[i1 >> 2]]];
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

  public void setTraceWriter(Writer w)
  {
    err = w;
  }

  private void writeTrace(String content)
  {
    try
    {
      err.write(content);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void flushTrace()
  {
    try
    {
      err.flush();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private static String[] getTokenSet(int tokenSetId)
  {
    java.util.ArrayList<String> expected = new java.util.ArrayList<>();
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
          expected.add(TOKEN[j]);
        }
      }
    }
    return expected.toArray(new String[]{});
  }

  private static final int[] MAP0 =
  {
    /*   0 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 1, 1, 29, 29, 1, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /*  27 */ 29, 29, 29, 29, 29, 2, 29, 3, 29, 29, 29, 29, 29, 29, 29, 29, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 10, 10,
    /*  55 */ 10, 10, 10, 11, 29, 29, 29, 29, 29, 29, 12, 12, 12, 12, 13, 12, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /*  81 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 14, 15, 16, 29, 29, 29, 17, 18, 12, 12, 19, 20, 29, 29, 29, 29,
    /* 107 */ 29, 21, 29, 22, 29, 29, 29, 23, 24, 25, 26, 29, 29, 29, 29, 29, 27, 29, 28, 29, 29
  };

  private static final int[] MAP1 =
  {
    /*    0 */ 683, 1097, 1097, 1097, 1097, 1097, 437, 432, 1097, 1097, 1026, 453, 728, 1097, 469, 495, 521, 609, 1097,
    /*   19 */ 537, 566, 582, 636, 652, 668, 699, 715, 748, 597, 764, 1000, 780, 1097, 796, 1097, 1097, 816, 844, 877,
    /*   39 */ 1067, 1097, 1097, 1097, 1097, 1097, 889, 1288, 1128, 905, 505, 1364, 921, 944, 960, 976, 828, 1016, 1055,
    /*   58 */ 1097, 1098, 1097, 1097, 1079, 479, 732, 1042, 1097, 1095, 1097, 1097, 1097, 1097, 1114, 1097, 1097, 1097,
    /*   76 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 933, 550, 1144, 620, 800, 1172, 1205, 1221,
    /*   94 */ 1097, 1392, 1238, 1257, 1254, 853, 1152, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  111 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  128 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  145 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1414, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  162 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  179 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  196 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  213 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  230 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  247 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  264 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  281 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  298 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  315 */ 1097, 1097, 1097, 1097, 1184, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 990, 1097, 1097,
    /*  332 */ 1230, 1156, 1097, 1273, 1304, 1320, 1336, 1352, 1380, 1189, 1408, 1126, 1097, 1097, 1097, 1097, 1097,
    /*  349 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  366 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  383 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  400 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097,
    /*  417 */ 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 1097, 861, 1687, 1653,
    /*  434 */ 1623, 1623, 1620, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  451 */ 1623, 1689, 1623, 1562, 1622, 1623, 1623, 1623, 1623, 1623, 1623, 1631, 1623, 1623, 1623, 1628, 1626,
    /*  468 */ 1631, 1623, 1691, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1669, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  485 */ 1481, 1623, 1481, 1623, 1668, 1482, 1623, 1623, 1677, 1624, 1623, 1623, 1623, 1623, 1623, 1623, 1629,
    /*  502 */ 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1628, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  519 */ 1625, 1631, 1623, 1623, 1623, 1623, 1623, 1625, 1623, 1624, 1623, 1623, 1623, 1456, 1623, 1628, 1631,
    /*  536 */ 1631, 1482, 1455, 1534, 1623, 1623, 1621, 1588, 1563, 1455, 1564, 1632, 1576, 1668, 1623, 1623, 1625,
    /*  553 */ 1623, 1623, 1623, 1623, 1553, 1623, 1621, 1628, 1631, 1631, 1687, 1631, 1631, 1678, 1518, 1534, 1623,
    /*  570 */ 1623, 1621, 1440, 1436, 1518, 1497, 1572, 1584, 1574, 1623, 1625, 1631, 1678, 1691, 1620, 1623, 1623,
    /*  587 */ 1621, 1603, 1563, 1691, 1605, 1630, 1631, 1668, 1623, 1629, 1622, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  604 */ 1518, 1623, 1623, 1623, 1627, 1631, 1631, 1631, 1631, 1623, 1623, 1481, 1625, 1631, 1631, 1687, 1623,
    /*  621 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1627, 1622, 1678, 1455,
    /*  638 */ 1534, 1623, 1623, 1621, 1603, 1563, 1455, 1497, 1574, 1576, 1668, 1623, 1623, 1631, 1618, 1552, 1666,
    /*  655 */ 1599, 1591, 1552, 1623, 1519, 1552, 1666, 1573, 1631, 1574, 1623, 1623, 1628, 1482, 1481, 1621, 1623,
    /*  672 */ 1623, 1621, 1623, 1553, 1481, 1666, 1575, 1628, 1668, 1623, 1631, 1623, 1430, 1623, 1623, 1452, 1464,
    /*  689 */ 1472, 1478, 1490, 1623, 1623, 1506, 1527, 1538, 1512, 1546, 1482, 1481, 1621, 1623, 1623, 1621, 1482,
    /*  706 */ 1563, 1481, 1666, 1575, 1633, 1668, 1623, 1444, 1631, 1482, 1481, 1621, 1623, 1623, 1623, 1623, 1623,
    /*  723 */ 1481, 1621, 1687, 1623, 1668, 1623, 1623, 1623, 1481, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  740 */ 1623, 1623, 1623, 1623, 1481, 1623, 1563, 1623, 1618, 1623, 1624, 1689, 1623, 1623, 1620, 1693, 1624,
    /*  757 */ 1613, 1692, 1623, 1574, 1623, 1498, 1631, 1642, 1644, 1687, 1622, 1652, 1618, 1623, 1605, 1692, 1625,
    /*  774 */ 1623, 1563, 1631, 1631, 1631, 1631, 1623, 1623, 1623, 1622, 1623, 1623, 1623, 1481, 1623, 1481, 1623,
    /*  791 */ 1628, 1631, 1631, 1631, 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1691, 1634, 1623, 1623,
    /*  808 */ 1623, 1623, 1623, 1623, 1623, 1632, 1630, 1632, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  825 */ 1666, 1624, 1666, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  842 */ 1627, 1687, 1623, 1666, 1623, 1623, 1623, 1623, 1666, 1624, 1666, 1623, 1624, 1623, 1623, 1623, 1623,
    /*  859 */ 1623, 1628, 1623, 1623, 1623, 1623, 1627, 1631, 1623, 1623, 1624, 1688, 1623, 1623, 1623, 1623, 1623,
    /*  876 */ 1627, 1623, 1623, 1666, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1669, 1623, 1623, 1623, 1626,
    /*  893 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1630, 1623, 1624, 1623, 1629, 1623,
    /*  910 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1631, 1623, 1623, 1623, 1623, 1623, 1627,
    /*  927 */ 1623, 1623, 1623, 1629, 1623, 1552, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /*  944 */ 1623, 1623, 1623, 1668, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1624, 1623, 1623, 1623, 1455, 1623,
    /*  961 */ 1629, 1623, 1629, 1623, 1625, 1623, 1624, 1631, 1631, 1631, 1631, 1631, 1631, 1631, 1631, 1623, 1623,
    /*  978 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1627, 1623, 1623, 1623, 1623, 1623, 1626, 1623, 1623, 1623,
    /*  995 */ 1623, 1623, 1623, 1624, 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1622, 1623, 1623,
    /* 1012 */ 1623, 1626, 1622, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1688, 1623, 1553, 1623, 1623, 1623,
    /* 1029 */ 1623, 1623, 1623, 1622, 1623, 1623, 1623, 1624, 1622, 1622, 1623, 1623, 1623, 1623, 1624, 1623, 1626,
    /* 1046 */ 1623, 1623, 1623, 1623, 1631, 1631, 1623, 1623, 1623, 1623, 1630, 1631, 1631, 1631, 1631, 1631, 1631,
    /* 1063 */ 1623, 1631, 1623, 1623, 1623, 1623, 1623, 1629, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1080 */ 1623, 1625, 1625, 1623, 1623, 1623, 1623, 1625, 1625, 1623, 1661, 1623, 1623, 1623, 1625, 1623, 1627,
    /* 1097 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1620,
    /* 1114 */ 1623, 1623, 1623, 1623, 1624, 1631, 1631, 1631, 1623, 1628, 1631, 1631, 1623, 1623, 1623, 1623, 1623,
    /* 1131 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1625, 1623, 1629, 1623, 1629, 1623, 1623, 1623, 1623,
    /* 1148 */ 1623, 1624, 1623, 1623, 1623, 1623, 1623, 1624, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1165 */ 1623, 1623, 1623, 1623, 1623, 1623, 1631, 1623, 1623, 1624, 1631, 1624, 1624, 1624, 1624, 1624, 1624,
    /* 1182 */ 1624, 1624, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1628, 1631,
    /* 1199 */ 1631, 1688, 1623, 1623, 1624, 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1629, 1631,
    /* 1216 */ 1631, 1631, 1631, 1631, 1631, 1623, 1623, 1623, 1620, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1233 */ 1623, 1623, 1627, 1631, 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1622, 1623, 1623, 1623,
    /* 1250 */ 1623, 1623, 1623, 1623, 1686, 1623, 1623, 1623, 1623, 1624, 1622, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1267 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1624, 1623, 1631, 1631, 1631, 1631,
    /* 1284 */ 1631, 1631, 1631, 1632, 1623, 1481, 1626, 1631, 1623, 1623, 1624, 1631, 1623, 1623, 1627, 1631, 1623,
    /* 1301 */ 1481, 1443, 1631, 1623, 1623, 1623, 1623, 1623, 1627, 1623, 1629, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1318 */ 1623, 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1625, 1574, 1623, 1629, 1623, 1623, 1623,
    /* 1335 */ 1625, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1627, 1632, 1623, 1623, 1623, 1626,
    /* 1352 */ 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1691, 1623, 1519, 1623, 1623, 1623, 1624, 1623,
    /* 1369 */ 1627, 1623, 1627, 1554, 1623, 1623, 1623, 1623, 1625, 1626, 1631, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1386 */ 1624, 1631, 1623, 1625, 1623, 1563, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1625,
    /* 1403 */ 1631, 1631, 1631, 1623, 1627, 1690, 1690, 1690, 1631, 1624, 1624, 1623, 1623, 1623, 1623, 1623, 1623,
    /* 1420 */ 1625, 1631, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 1623, 29, 1, 1, 29, 29, 1, 29, 29, 0, 0, 29, 0, 29,
    /* 1443 */ 29, 0, 29, 29, 0, 0, 0, 0, 0, 2, 29, 3, 29, 29, 29, 29, 29, 0, 0, 29, 0, 29, 29, 29, 4, 5, 6, 7, 8, 9,
    /* 1473 */ 10, 10, 10, 10, 10, 10, 10, 11, 29, 29, 29, 29, 29, 0, 29, 29, 29, 29, 12, 12, 12, 12, 13, 12, 29, 0, 0,
    /* 1500 */ 29, 29, 29, 0, 0, 0, 29, 29, 29, 14, 15, 16, 29, 29, 23, 24, 25, 26, 29, 29, 29, 0, 0, 0, 0, 29, 29, 29,
    /* 1528 */ 17, 18, 12, 12, 19, 20, 29, 0, 0, 29, 29, 29, 29, 29, 21, 29, 22, 29, 29, 29, 29, 27, 29, 28, 29, 29, 29,
    /* 1555 */ 0, 0, 0, 29, 29, 29, 29, 0, 29, 29, 0, 0, 29, 29, 29, 29, 0, 0, 29, 0, 0, 0, 0, 0, 0, 29, 29, 0, 29, 0,
    /* 1585 */ 29, 29, 29, 29, 0, 29, 0, 0, 0, 29, 29, 0, 0, 0, 0, 29, 29, 0, 29, 0, 29, 29, 0, 29, 29, 29, 0, 0, 0, 0,
    /* 1615 */ 29, 0, 0, 0, 0, 29, 29, 0, 29, 29, 29, 29, 29, 29, 29, 29, 0, 0, 0, 0, 0, 0, 0, 0, 29, 0, 0, 0, 29, 29,
    /* 1645 */ 0, 29, 0, 0, 29, 0, 0, 0, 29, 29, 29, 0, 29, 0, 29, 29, 0, 29, 0, 29, 0, 29, 0, 29, 29, 29, 29, 0, 0, 29,
    /* 1675 */ 29, 29, 0, 0, 29, 29, 29, 0, 29, 29, 29, 0, 0, 0, 0, 0, 29, 29, 29, 29, 29, 29, 0, 29, 0, 0
  };

  private static final int[] MAP2 =
  {
    /*   0 */ 57344, 64112, 64256, 64275, 64285, 64312, 64318, 64320, 64323, 64326, 64467, 64848, 64914, 65008, 65024,
    /*  15 */ 65056, 65108, 65128, 65136, 65142, 65279, 65281, 65474, 65482, 65490, 65498, 65504, 65512, 65529, 65536,
    /*  30 */ 65549, 65576, 65596, 65599, 65616, 65664, 65792, 65799, 65847, 65936, 65952, 66000, 66176, 66208, 66272,
    /*  45 */ 66304, 66349, 66384, 66432, 66463, 66504, 66560, 66720, 66736, 66776, 66816, 66864, 66927, 67072, 67392,
    /*  60 */ 67424, 67584, 67592, 67594, 67639, 67644, 67647, 67671, 67751, 67808, 67828, 67835, 67871, 67903, 67968,
    /*  75 */ 68028, 68050, 68101, 68108, 68117, 68121, 68152, 68159, 68176, 68192, 68288, 68331, 68352, 68409, 68440,
    /*  90 */ 68472, 68505, 68521, 68608, 68736, 68800, 68858, 69216, 69632, 69714, 69759, 69840, 69872, 69888, 69942,
    /* 105 */ 69968, 70016, 70096, 70113, 70144, 70163, 70272, 70280, 70282, 70287, 70303, 70320, 70384, 70400, 70405,
    /* 120 */ 70415, 70419, 70442, 70450, 70453, 70460, 70471, 70475, 70480, 70487, 70493, 70502, 70512, 70656, 70747,
    /* 135 */ 70749, 70784, 70864, 71040, 71096, 71168, 71248, 71264, 71296, 71360, 71424, 71453, 71472, 71840, 71935,
    /* 150 */ 72192, 72272, 72326, 72350, 72384, 72704, 72714, 72760, 72784, 72816, 72850, 72873, 72960, 72968, 72971,
    /* 165 */ 73018, 73020, 73023, 73040, 73728, 74752, 74864, 74880, 77824, 82944, 92160, 92736, 92768, 92782, 92880,
    /* 180 */ 92912, 92928, 93008, 93019, 93027, 93053, 93952, 94032, 94095, 94176, 94208, 100352, 110592, 110960,
    /* 194 */ 113664, 113776, 113792, 113808, 113820, 118784, 119040, 119081, 119296, 119552, 119648, 119808, 119894,
    /* 207 */ 119966, 119970, 119973, 119977, 119982, 119995, 119997, 120005, 120071, 120077, 120086, 120094, 120123,
    /* 220 */ 120128, 120134, 120138, 120146, 120488, 120782, 121499, 121505, 122880, 122888, 122907, 122915, 122918,
    /* 233 */ 124928, 125127, 125184, 125264, 125278, 126464, 126469, 126497, 126500, 126503, 126505, 126516, 126521,
    /* 246 */ 126523, 126530, 126535, 126537, 126539, 126541, 126545, 126548, 126551, 126553, 126555, 126557, 126559,
    /* 259 */ 126561, 126564, 126567, 126572, 126580, 126585, 126590, 126592, 126603, 126625, 126629, 126635, 126704,
    /* 272 */ 126976, 127024, 127136, 127153, 127169, 127185, 127232, 127248, 127280, 127344, 127462, 127504, 127552,
    /* 285 */ 127568, 127584, 127744, 128736, 128752, 128768, 128896, 129024, 129040, 129104, 129120, 129168, 129280,
    /* 298 */ 129296, 129344, 129360, 129408, 129472, 129488, 131072, 173824, 177984, 178208, 183984, 194560, 917505,
    /* 311 */ 917536, 917760, 983040, 1048576, 64109, 64217, 64262, 64279, 64310, 64316, 64318, 64321, 64324, 64449,
    /* 325 */ 64831, 64911, 64967, 65021, 65049, 65106, 65126, 65131, 65140, 65276, 65279, 65470, 65479, 65487, 65495,
    /* 340 */ 65500, 65510, 65518, 65533, 65547, 65574, 65594, 65597, 65613, 65629, 65786, 65794, 65843, 65934, 65947,
    /* 355 */ 65952, 66045, 66204, 66256, 66299, 66339, 66378, 66426, 66461, 66499, 66517, 66717, 66729, 66771, 66811,
    /* 370 */ 66855, 66915, 66927, 67382, 67413, 67431, 67589, 67592, 67637, 67640, 67644, 67669, 67742, 67759, 67826,
    /* 385 */ 67829, 67867, 67897, 67903, 68023, 68047, 68099, 68102, 68115, 68119, 68147, 68154, 68167, 68184, 68255,
    /* 400 */ 68326, 68342, 68405, 68437, 68466, 68497, 68508, 68527, 68680, 68786, 68850, 68863, 69246, 69709, 69743,
    /* 415 */ 69825, 69864, 69881, 69940, 69955, 70006, 70093, 70111, 70132, 70161, 70206, 70278, 70280, 70285, 70301,
    /* 430 */ 70313, 70378, 70393, 70403, 70412, 70416, 70440, 70448, 70451, 70457, 70468, 70472, 70477, 70480, 70487,
    /* 445 */ 70499, 70508, 70516, 70745, 70747, 70749, 70855, 70873, 71093, 71133, 71236, 71257, 71276, 71351, 71369,
    /* 460 */ 71449, 71467, 71487, 71922, 71935, 72263, 72323, 72348, 72354, 72440, 72712, 72758, 72773, 72812, 72847,
    /* 475 */ 72871, 72886, 72966, 72969, 73014, 73018, 73021, 73031, 73049, 74649, 74862, 74868, 75075, 78894, 83526,
    /* 490 */ 92728, 92766, 92777, 92783, 92909, 92917, 92997, 93017, 93025, 93047, 93071, 94020, 94078, 94111, 94177,
    /* 505 */ 100332, 101106, 110878, 111355, 113770, 113788, 113800, 113817, 113827, 119029, 119078, 119272, 119365,
    /* 518 */ 119638, 119665, 119892, 119964, 119967, 119970, 119974, 119980, 119993, 119995, 120003, 120069, 120074,
    /* 531 */ 120084, 120092, 120121, 120126, 120132, 120134, 120144, 120485, 120779, 121483, 121503, 121519, 122886,
    /* 544 */ 122904, 122913, 122916, 122922, 125124, 125142, 125258, 125273, 125279, 126467, 126495, 126498, 126500,
    /* 557 */ 126503, 126514, 126519, 126521, 126523, 126530, 126535, 126537, 126539, 126543, 126546, 126548, 126551,
    /* 570 */ 126553, 126555, 126557, 126559, 126562, 126564, 126570, 126578, 126583, 126588, 126590, 126601, 126619,
    /* 583 */ 126627, 126633, 126651, 126705, 127019, 127123, 127150, 127167, 127183, 127221, 127244, 127278, 127339,
    /* 596 */ 127404, 127490, 127547, 127560, 127569, 127589, 128724, 128748, 128760, 128883, 128980, 129035, 129095,
    /* 609 */ 129113, 129159, 129197, 129291, 129342, 129356, 129387, 129431, 129472, 129510, 173782, 177972, 178205,
    /* 622 */ 183969, 191456, 195101, 917505, 917631, 917999, 1048573, 1114109, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 640 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 666 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 692 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 718 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 744 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 770 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 796 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 822 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 848 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 874 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 900 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    /* 926 */ 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29
  };

  private static final int[] INITIAL =
  {
    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 712, 9, 10, 11, 12, 13, 14, 15, 16, 17, 722, 723, 20, 725, 22, 727, 728, 25, 26, 27
  };

  private static final int[] TRANSITION =
  {
    /*   0 */ 261, 261, 261, 261, 261, 261, 261, 261, 261, 359, 240, 240, 244, 243, 245, 261, 261, 373, 249, 249, 255,
    /*  21 */ 254, 250, 261, 261, 261, 260, 259, 261, 259, 267, 261, 261, 261, 261, 261, 275, 261, 273, 261, 261, 261,
    /*  42 */ 303, 413, 414, 281, 302, 261, 261, 261, 261, 261, 287, 261, 285, 261, 261, 261, 261, 261, 261, 294, 295,
    /*  63 */ 261, 261, 261, 261, 261, 261, 331, 330, 261, 261, 299, 261, 261, 310, 307, 309, 261, 261, 410, 261, 261,
    /*  84 */ 430, 427, 429, 261, 261, 261, 327, 261, 261, 261, 327, 261, 261, 261, 261, 261, 262, 261, 263, 261, 261,
    /* 105 */ 261, 261, 261, 314, 320, 318, 261, 261, 261, 261, 261, 261, 261, 324, 261, 261, 261, 261, 261, 261, 367,
    /* 126 */ 366, 261, 261, 261, 394, 395, 336, 335, 336, 261, 340, 261, 261, 261, 276, 261, 277, 261, 261, 261, 261,
    /* 147 */ 261, 288, 290, 289, 261, 353, 261, 261, 261, 345, 348, 352, 261, 261, 261, 261, 261, 341, 357, 363, 261,
    /* 168 */ 380, 261, 261, 261, 261, 261, 380, 261, 261, 261, 261, 261, 261, 371, 377, 261, 386, 261, 261, 261, 261,
    /* 189 */ 388, 387, 261, 261, 385, 261, 261, 261, 261, 383, 261, 261, 261, 261, 261, 261, 392, 399, 261, 261, 403,
    /* 210 */ 261, 261, 261, 403, 402, 261, 261, 261, 261, 261, 261, 261, 407, 261, 261, 261, 261, 418, 420, 419, 424,
    /* 231 */ 261, 261, 261, 261, 261, 261, 261, 270, 261, 736, 736, 736, 736, 0, 736, 736, 0, 0, 800, 800, 800, 800, 0,
    /* 254 */ 800, 0, 800, 800, 0, 0, 128, 0, 0, 0, 0, 96, 0, 128, 128, 128, 0, 0, 64, 0, 0, 160, 0, 0, 0, 384, 0, 192,
    /* 282 */ 0, 192, 192, 224, 224, 224, 0, 0, 0, 416, 0, 0, 832, 0, 0, 832, 0, 0, 0, 864, 0, 0, 192, 0, 0, 0, 0, 864,
    /* 310 */ 864, 864, 0, 864, 0, 0, 896, 896, 0, 0, 896, 0, 896, 896, 320, 320, 320, 0, 0, 288, 0, 0, 256, 0, 0, 928,
    /* 336 */ 0, 928, 928, 0, 384, 0, 0, 0, 448, 0, 0, 960, 960, 0, 960, 960, 0, 0, 960, 0, 0, 0, 448, 0, 0, 0, 736,
    /* 363 */ 448, 448, 448, 0, 0, 352, 0, 0, 0, 512, 0, 0, 0, 800, 512, 512, 512, 0, 0, 480, 0, 0, 576, 0, 0, 0, 544,
    /* 390 */ 0, 0, 0, 608, 0, 0, 0, 928, 0, 608, 608, 608, 0, 0, 640, 0, 0, 672, 672, 672, 0, 0, 768, 0, 0, 192, 192,
    /* 417 */ 0, 992, 992, 0, 992, 992, 0, 0, 0, 992, 0, 0, 768, 768, 768, 0, 768
  };

  private static final int[] EXPECTED =
  {
    /*  0 */ 2048, 536870912, 16384, 65536, 131072, 524288, 75497472, 20971520, 20971528, 20971552, 20971776, 289406976,
    /* 12 */ 1094713344, 1094713352, 289407008, 1094713376, 75497552, 1438646304, 2034237472, 746600452, 2067791904,
    /* 21 */ 898184, 2109734944, 2143289376, 97821256, 366256712, 2141192190
  };

  private static final int[] CASEID =
  {
    /*   0 */ 102, 106, 121, 125, 136, 109, 136, 112, 135, 275, 102, 185, 141, 144, 136, 148, 115, 155, 208, 236, 159,
    /*  21 */ 128, 199, 226, 136, 182, 121, 125, 199, 164, 136, 285, 168, 171, 272, 175, 136, 179, 282, 189, 117, 196,
    /*  42 */ 193, 128, 136, 131, 205, 128, 136, 151, 214, 218, 223, 233, 230, 240, 201, 128, 135, 196, 102, 106, 244,
    /*  63 */ 251, 136, 179, 102, 106, 159, 128, 121, 125, 244, 251, 248, 255, 121, 125, 244, 251, 244, 251, 210, 136,
    /*  84 */ 136, 259, 136, 264, 137, 136, 136, 269, 160, 136, 136, 279, 136, 219, 265, 136, 136, 260, 344, 345, 346,
    /* 105 */ 306, 345, 300, 307, 348, 301, 347, 348, 312, 469, 348, 328, 348, 348, 347, 348, 349, 507, 339, 289, 421,
    /* 126 */ 293, 332, 348, 330, 432, 348, 330, 432, 403, 344, 348, 348, 348, 348, 424, 321, 316, 316, 316, 319, 316,
    /* 147 */ 322, 348, 430, 432, 348, 330, 432, 454, 348, 418, 326, 336, 349, 348, 348, 348, 427, 348, 446, 448, 372,
    /* 168 */ 389, 379, 384, 379, 387, 379, 380, 348, 415, 397, 393, 348, 433, 434, 348, 345, 347, 345, 297, 305, 311,
    /* 189 */ 348, 407, 408, 412, 348, 438, 348, 348, 345, 347, 348, 346, 348, 348, 489, 348, 348, 444, 348, 348, 353,
    /* 210 */ 348, 348, 455, 348, 449, 449, 450, 459, 460, 348, 348, 348, 467, 348, 464, 348, 348, 355, 357, 475, 348,
    /* 231 */ 473, 348, 348, 358, 359, 348, 363, 368, 364, 348, 479, 485, 483, 374, 348, 373, 505, 348, 512, 348, 348,
    /* 252 */ 373, 503, 375, 348, 494, 498, 511, 516, 348, 348, 348, 501, 490, 348, 348, 348, 523, 517, 348, 348, 348,
    /* 273 */ 396, 348, 348, 341, 343, 440, 521, 348, 348, 348, 401, 348, 348, 345, 347, 347, 0, 10514, 0, 11026, 1042,
    /* 294 */ 0, 520, 2820, 260, 0, 526, 260, 0, 260, 260, 0, 526, 0, 260, 0, 260, 0, 2308, 0, 0, 0, 2322, 3844, 3844,
    /* 318 */ 3844, 3844, 0, 0, 3844, 3844, 3844, 0, 3076, 4626, 0, 3076, 0, 0, 520, 0, 2820, 0, 3076, 3076, 3076, 0,
    /* 340 */ 1298, 0, 0, 14, 0, 0, 0, 260, 0, 0, 0, 0, 1554, 0, 5380, 0, 0, 1038, 0, 0, 0, 6916, 0, 0, 5380, 5380,
    /* 366 */ 5380, 0, 5380, 5380, 5380, 5380, 2324, 0, 0, 0, 8456, 0, 0, 7176, 7176, 7176, 7176, 0, 7176, 7176, 6418,
    /* 387 */ 7176, 0, 0, 7176, 7176, 3624, 3332, 6408, 3332, 0, 3332, 0, 0, 6408, 0, 4900, 0, 0, 1080, 0, 0, 4900,
    /* 409 */ 4900, 5128, 4900, 4900, 4900, 4900, 0, 3332, 3332, 0, 3076, 3076, 0, 0, 10770, 0, 0, 12050, 0, 0, 12562,
    /* 430 */ 0, 52, 520, 0, 0, 0, 5128, 0, 0, 7442, 0, 0, 1540, 0, 0, 7698, 0, 0, 1550, 0, 0, 0, 7688, 0, 1848, 0, 0,
    /* 457 */ 0, 11282, 7688, 7688, 0, 7688, 7954, 6664, 0, 6664, 0, 4152, 0, 0, 4872, 0, 0, 5668, 0, 0, 1556, 0, 0,
    /* 480 */ 5668, 5668, 5128, 5668, 5668, 5668, 0, 5128, 5668, 8466, 0, 0, 0, 11794, 0, 6196, 6196, 5128, 6196, 0,
    /* 500 */ 5128, 0, 4168, 0, 0, 8456, 8456, 0, 0, 2824, 0, 6196, 0, 6196, 0, 0, 11538, 0, 0, 0, 12306, 0, 12818, 0,
    /* 524 */ 0, 4408, 0
  };

  private static final int[] TOKENSET =
  {
    /*  0 */ 24, 24, 7, 6, 13, 25, 26, 7, 20, 23, 8, 15, 12, 24, 14, 11, 26, 18, 6, 23, 10, 9, 12, 9, 11, 21, 16, 22,
    /* 28 */ 10, 8, 24, 19, 6, 24, 8, 24, 19, 17, 24, 19, 19, 0, 3, 5, 2, 5, 2, 4, 1, 2, 1
  };

  private static final int[] APPENDIX =
  {
    /* 0 */ 130, 770, 130, 1154, 130, 778, 130, 1162
  };

  private static final int[] GOTO =
  {
    /*   0 */ 103, 109, 218, 202, 109, 108, 109, 109, 210, 109, 114, 109, 127, 120, 109, 121, 109, 206, 109, 109, 104,
    /*  21 */ 125, 109, 156, 109, 131, 135, 174, 109, 109, 139, 109, 116, 202, 109, 144, 109, 215, 109, 109, 109, 110,
    /*  42 */ 109, 149, 155, 109, 160, 109, 140, 109, 166, 109, 206, 109, 109, 208, 109, 206, 109, 109, 208, 109, 109,
    /*  63 */ 109, 172, 109, 186, 109, 145, 109, 208, 109, 162, 109, 109, 178, 109, 109, 109, 168, 109, 184, 180, 109,
    /*  84 */ 109, 131, 190, 151, 202, 109, 109, 109, 194, 145, 109, 200, 204, 109, 109, 196, 109, 109, 214, 222, 241,
    /* 105 */ 241, 241, 240, 349, 241, 241, 241, 241, 246, 254, 346, 241, 241, 256, 229, 234, 241, 241, 241, 247, 241,
    /* 126 */ 318, 241, 241, 276, 241, 224, 241, 241, 269, 296, 241, 241, 294, 316, 241, 241, 241, 264, 325, 241, 241,
    /* 147 */ 241, 273, 241, 267, 241, 241, 278, 229, 251, 241, 241, 241, 285, 241, 260, 241, 241, 282, 241, 343, 241,
    /* 168 */ 241, 241, 300, 241, 241, 336, 241, 241, 303, 241, 291, 241, 241, 241, 313, 241, 241, 309, 241, 241, 327,
    /* 189 */ 241, 305, 241, 241, 294, 322, 241, 241, 241, 331, 241, 236, 229, 287, 230, 228, 241, 241, 241, 224, 241,
    /* 210 */ 241, 241, 242, 241, 340, 241, 241, 334, 241, 241, 351, 229, 6, 137, 0, 0, 140, 0, 388, 388, 0, 0, 388, 0,
    /* 234 */ 1801, 1929, 0, 0, 140, 2340, 2185, 0, 0, 0, 0, 1033, 1932, 0, 0, 0, 2057, 1668, 3337, 0, 0, 1289, 0, 0,
    /* 258 */ 140, 1036, 2948, 0, 0, 3465, 2700, 0, 0, 0, 1308, 0, 0, 3716, 0, 2956, 0, 0, 0, 1673, 0, 0, 140, 1060, 0,
    /* 283 */ 4361, 0, 0, 2441, 0, 0, 388, 393, 0, 4489, 0, 0, 2569, 0, 0, 652, 0, 3980, 4617, 0, 0, 2697, 0, 0, 676, 0,
    /* 309 */ 2948, 0, 0, 4745, 0, 4873, 0, 0, 2825, 0, 0, 1540, 0, 0, 5001, 0, 0, 2953, 0, 0, 3460, 4105, 0, 5129, 0,
    /* 334 */ 0, 3081, 0, 0, 3596, 3844, 0, 4124, 0, 0, 3593, 0, 0, 1417, 1545, 0, 905, 0, 0, 140, 265
  };

  private static final String[] TOKEN =
  {
    "(0)",
    "_char_preserved_chars",
    "_HEXDIG_preserved_chars",
    "'\"'",
    "'+'",
    "','",
    "'-'",
    "'/'",
    "':'",
    "'['",
    "'\\'",
    "'a'",
    "'b'",
    "'f'",
    "'l'",
    "'n'",
    "'r'",
    "'s'",
    "'t'",
    "'u'",
    "'{'",
    "END",
    "_ws_deleted_chars",
    "_digit1_9_preserved_chars",
    "(24)",
    "'.'",
    "'0'",
    "'E'",
    "']'",
    "'e'",
    "'}'"
  };

  private static final String[] NONTERMINAL =
  {
    "json",
    "ws",
    "_ws_choice",
    "value",
    "map",
    "_map_list",
    "_map_list_option",
    "array",
    "_array_list",
    "_array_list_option",
    "number",
    "_number_option",
    "_number_option_1",
    "_number_option_2",
    "string",
    "_string_list_option",
    "boolean",
    "null",
    "member",
    "int",
    "_int_choice",
    "_int_list_option",
    "frac",
    "_frac_list",
    "exp",
    "_exp_choice",
    "_exp_choice_1",
    "_exp_option",
    "char",
    "key",
    "escaped",
    "unicode",
    "code",
    "_code_choice"
  };
}

// End
