// This file was generated on Tue May 2, 2023 21:34 (UTC+02) by REx v5.57 which is Copyright (c) 1979-2023 by Gunther Rademacher <grd@gmx.net>
// REx command line: -glalr 1 -main -java -a java -name de.bottlecaps.markupblitz.Ixml ixml.ebnf

package de.bottlecaps.markupblitz;

import java.util.PriorityQueue;

public class Ixml
{
  public static void main(String args[]) throws Exception
  {
    if (args.length == 0)
    {
      System.out.println("Usage: java Ixml INPUT...");
      System.out.println();
      System.out.println("  parse INPUT, which is either a filename or literal text enclosed in curly braces");
    }
    else
    {
      for (String arg : args)
      {
        String input = read(arg);
        Ixml parser = new Ixml(input);
        try
        {
          parser.parse_ixml();
        }
        catch (ParseException pe)
        {
          throw new RuntimeException("ParseException while processing " + arg + ":\n" + parser.getErrorMessage(pe));
        }
      }
    }
  }

  public static class ParseException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;
    private int begin, end, offending, expected, state;
    private boolean ambiguousInput;

    public ParseException(int b, int e, int s, int o, int x)
    {
      begin = b;
      end = e;
      state = s;
      offending = o;
      expected = x;
      ambiguousInput = false;
    }

    public ParseException(int b, int e)
    {
      this(b, e, 1, -1, -1);
      ambiguousInput = true;
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

    public int getBegin() {return begin;}
    public int getEnd() {return end;}
    public int getState() {return state;}
    public int getOffending() {return offending;}
    public int getExpected() {return expected;}
    public boolean isAmbiguousInput() {return ambiguousInput;}
  }

  private static String read(String input) throws Exception
  {
    if (input.startsWith("{") && input.endsWith("}"))
    {
      return input.substring(1, input.length() - 1);
    }
    else
    {
      byte buffer[] = new byte[(int) new java.io.File(input).length()];
      java.io.FileInputStream stream = new java.io.FileInputStream(input);
      stream.read(buffer);
      stream.close();
      String content = new String(buffer, System.getProperty("file.encoding"));
      return content.length() > 0 && content.charAt(0) == '\uFEFF'
           ? content.substring(1)
           : content;
    }
  }

  public Ixml(CharSequence string)
  {
    initialize(string);
  }

  public void initialize(CharSequence source)
  {
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

  public void parse_ixml()
  {
    thread = parse(0, 0, thread);
  }

  private static class StackNode
  {
    public int state;
    public int code;
    public int pos;
    public StackNode link;

    public StackNode(int state, int code, int pos, StackNode link)
    {
      this.state = state;
      this.code = code;
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
        if (lhs.code != rhs.code) return false;
        if (lhs.pos != rhs.pos) return false;
        lhs = lhs.link;
        rhs = rhs.link;
      }
      return lhs == rhs;
    }

    public int lookback(int x, int y)
    {
      int i = LOOKBACK[y];
      int l = LOOKBACK[i];
      while (l > x)
      {
        i += 2;
        l = LOOKBACK[i];
      }
      if (l < x)
      {
        return 0;
      }
      else
      {
        return LOOKBACK[i + 1];
      }
    }

    public int count(int code)
    {
      int count = 0;
      for (StackNode node = this; node.state >= 0; node = node.link)
      {
        code = lookback(node.code, code);
        if (code == 0)
        {
          break;
        }
        count += 1;
      }
      return count;
    }

  }

  private static class DeferredCode
  {
    public DeferredCode link;
    public int codeId;
    public int b0;
    public int e0;

    public DeferredCode(DeferredCode link, int codeId, int b0, int e0)
    {
      this.link = link;
      this.codeId = codeId;
      this.b0 = b0;
      this.e0 = e0;
    }
  }

  private static final int PARSING = 0;
  private static final int ACCEPTED = 1;
  private static final int ERROR = 2;

  private ParsingThread parse(int target, int initialState, ParsingThread thread)
  {
    PriorityQueue<ParsingThread> threads = thread.open(initialState, target);
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
          rejectAmbiguity(thread.stack.pos, thread.e0);
        }
        thread.executeDeferredCode();
        return thread;
      }

      if (! threads.isEmpty())
      {
        if (threads.peek().equals(thread))
        {
          rejectAmbiguity(thread.stack.pos, thread.e0);
        }
      }
      else
      {
        thread.executeDeferredCode();
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

  private void rejectAmbiguity(int begin, int end)
  {
    throw new ParseException(begin, end);
  }

  private ParsingThread thread = new ParsingThread();
  private CharSequence input = null;
  private int size = 0;
  private int maxId = 0;

  private class ParsingThread implements Comparable<ParsingThread>
  {
    public PriorityQueue<ParsingThread> threads;
    public boolean accepted;
    public StackNode stack;
    public int state;
    public int action;
    public int target;
    public DeferredCode deferredCode;
    public int id;

    public PriorityQueue<ParsingThread> open(int initialState, int t)
    {
      accepted = false;
      target = t;
      deferredCode = null;
      stack = new StackNode(-1, 0, e0, null);
      state = initialState;
      action = predict(initialState);
      threads = new PriorityQueue<>();
      threads.offer(this);
      return threads;
    }

    public ParsingThread copy(ParsingThread other, int action)
    {
      this.action = action;
      accepted = other.accepted;
      target = other.target;
      deferredCode = other.deferredCode;
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
        int argument = action >> 12;
        int lookback = (action >> 3) & 511;
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

        case 3: // REDUCE+LOOKBACK
          reduce = argument;
          symbols = stack.count(lookback);
          break;

        case 4: // SHIFT+REDUCE
          shift = state;
          reduce = argument;
          symbols = lookback + 1;
          break;

        case 5: // SHIFT+REDUCE+LOOKBACK
          shift = state;
          reduce = argument;
          symbols = stack.count(lookback) + 1;
          break;

        case 6: // ACCEPT
          accepted = true;
          action = 0;
          return ACCEPTED;

        case 7: // FORK
          threads.offer(new ParsingThread().copy(this, APPENDIX[argument]));
          action = APPENDIX[argument + 1];
          return PARSING;

        default: // ERROR
          return ERROR;
        }

        if (shift >= 0)
        {
          if (nonterminalId < 0)
          {
            stack = new StackNode(state, lookback, b1, stack);
            consume(l1);
          }
          else
          {
            stack = new StackNode(state, lookback, 0, stack);
          }
          state = shift;
        }

        if (reduce < 0)
        {
          action = predict(state);
          return PARSING;
        }
        else
        {
          nonterminalId = REDUCTION[reduce];
          reduce = REDUCTION[reduce + 1];
          if (reduce >= 0)
          {
            if (isUnambiguous())
          {
              execute(reduce);
            }
          else
          {
              deferredCode = new DeferredCode(deferredCode, reduce, b0, e0);
            }
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
          else
          {
            bs = b1;
          }
          action = goTo(nonterminalId, state);
        }
      }
    }

    public boolean isUnambiguous()
    {
      return threads.isEmpty();
    }

    public void executeDeferredCode()
    {
      if (deferredCode != null)
      {
        DeferredCode predecessor = deferredCode.link;
        deferredCode.link = null;
        while (predecessor != null)
        {
          DeferredCode nextCode = predecessor.link;
          predecessor.link = deferredCode;
          deferredCode = predecessor;
          predecessor = nextCode;
        }
        int b0t = b0;
        int e0t = e0;
        while (deferredCode != null)
        {
          b0 = deferredCode.b0;
          e0 = deferredCode.e0;
          execute(deferredCode.codeId);
          deferredCode = deferredCode.link;
        }
        b0 = b0t;
        e0 = e0t;
      }
    }

    public void execute(int reduce)
    {
      switch (reduce)
      {
      case 0:
        {
                                                            // line 1 "ixml.ebnf"
                                                            grammar = new Grammar();
                                                            // line 530 "Ixml.java"
        }
        break;
      case 1:
        {
                                                            // line 11 "ixml.ebnf"
                                                            alts.push(new Rule(mark, nameBuilder.toString()));
                                                            // line 537 "Ixml.java"
        }
        break;
      case 2:
        {
                                                            // line 12 "ixml.ebnf"
                                                            grammar.addRule((Rule) alts.pop());
                                                            // line 544 "Ixml.java"
        }
        break;
      case 3:
        {
                                                            // line 13 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 551 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 14 "ixml.ebnf"
                                                            mark = Mark.ELEMENT;
                                                            // line 558 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 15 "ixml.ebnf"
                                                            mark = Mark.DELETED;
                                                            // line 565 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 16 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 572 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 19 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 579 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 28 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 586 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 30 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 594 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 34 "ixml.ebnf"
                                                            Alt.Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 602 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 38 "ixml.ebnf"
                                                            Alt.Term sep = alts.peek().last().removeLast();
                                                            Alt.Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 611 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 44 "ixml.ebnf"
                                                            Alt.Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 619 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 48 "ixml.ebnf"
                                                            Alt.Term sep = alts.peek().last().removeLast();
                                                            Alt.Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 628 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 54 "ixml.ebnf"
                                                            Alt.Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 636 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 58 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, nameBuilder.toString());
                                                            // line 643 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 60 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 650 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 61 "ixml.ebnf"
                                                            nameBuilder.append(input.charAt(b0));
                                                            // line 657 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 73 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 664 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 76 "ixml.ebnf"
                                                            alts.peek().last().addCharSet(charSet);
                                                            // line 671 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 80 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 678 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 81 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 685 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 82 "ixml.ebnf"
                                                            alts.peek().last().addCodePoint(deleted, codePoint);
                                                            // line 692 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 83 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 699 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 84 "ixml.ebnf"
                                                            stringBuilder.append(input.charAt(b0));
                                                            // line 706 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 89 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 713 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 90 "ixml.ebnf"
                                                            codePoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 720 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 92 "ixml.ebnf"
                                                            charSet = new CharSet(deleted);
                                                            // line 727 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 93 "ixml.ebnf"
                                                            charSet.setExclusion();
                                                            // line 734 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 98 "ixml.ebnf"
                                                            charSet.addLiteral(stringBuilder.toString(), false);
                                                            // line 741 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 99 "ixml.ebnf"
                                                            charSet.addLiteral(codePoint, true);
                                                            // line 748 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 100 "ixml.ebnf"
                                                            charSet.addRange(firstCodePoint, lastCodePoint);
                                                            // line 755 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 101 "ixml.ebnf"
                                                            charSet.addClass(clazz);
                                                            // line 762 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 103 "ixml.ebnf"
                                                            firstCodePoint = codePoint;
                                                            // line 769 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 104 "ixml.ebnf"
                                                            lastCodePoint = codePoint;
                                                            // line 776 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 105 "ixml.ebnf"
                                                            codePoint = input.subSequence(b0, e0).toString();
                                                            // line 783 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 110 "ixml.ebnf"
                                                            clazz += input.charAt(b0);
                                                            // line 790 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 112 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 797 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 113 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codePoint);
                                                            // line 804 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 119 "ixml.ebnf"
                                                            clazz = Character.toString(input.charAt(b0));
                                                            // line 811 "Ixml.java"
        }
        break;
      default:
        break;
      }
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
      throw new ParseException(b, e, s, l, t);
    }

    private int     b0, e0;
    private int l1, b1, e1;
    private int bw, bs;

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
      int j10 = 80 * d + l1;
      int j11 = j10 >> 2;
      int action = CASEID[(j10 & 3) + CASEID[(j11 & 3) + CASEID[j11 >> 2]]];
      return action >> 1;
    }

    private int match(int tokenSetId)
    {
      begin = end;
      int current = end;
      int result = INITIAL[tokenSetId];

      for (int code = result & 127; code != 0; )
      {
        int charclass;
        int c0 = current < size ? input.charAt(current) : 0;
        ++current;
        if (c0 < 0x80)
        {
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

          int lo = 0, hi = 349;
          for (int m = 175; ; m = (hi + lo) >> 1)
          {
            if (MAP2[m] > c0) {hi = m - 1;}
            else if (MAP2[350 + m] < c0) {lo = m + 1;}
            else {charclass = MAP2[700 + m]; break;}
            if (lo > hi) {charclass = 0; break;}
          }
        }

        int i0 = (charclass << 7) + code - 1;
        code = TRANSITION[(i0 & 7) + TRANSITION[i0 >> 3]];

        if (code > 127)
        {
          result = code;
          code &= 127;
          end = current;
        }
      }

      result >>= 7;
      if (result == 0)
      {
        end = current - 1;
        int c1 = end < size ? input.charAt(end) : 0;
        if (c1 >= 0xdc00 && c1 < 0xe000)
        {
          --end;
        }
        end = begin;
        return -1;
      }

      if (end > size) end = size;
      return (result & 127) - 1;
    }

  }

  private static int goTo(int nonterminal, int state)
  {
    int i0 = 56 * state + nonterminal;
    int i1 = i0 >> 2;
    return GOTO[(i0 & 3) + GOTO[(i1 & 3) + GOTO[i1 >> 2]]];
  }

  private static String[] getTokenSet(int tokenSetId)
  {
    java.util.ArrayList<String> expected = new java.util.ArrayList<>();
    int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 127;
    for (int i = 0; i < 70; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 70 + s - 1;
      int f = EXPECTED[(i0 & 3) + EXPECTED[i0 >> 2]];
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

  private static final int[] MAP0 = new int[128];
  static
  {
    final String s1[] =
    {
      /*   0 */ "67, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3",
      /*  34 */ "4, 5, 3, 3, 3, 6, 7, 8, 9, 10, 11, 12, 13, 3, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 16, 3, 17",
      /*  62 */ "3, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42",
      /*  88 */ "43, 44, 45, 46, 3, 47, 48, 49, 3, 50, 50, 50, 50, 51, 50, 52, 52, 53, 52, 52, 54, 55, 56, 57, 52, 52",
      /* 114 */ "58, 59, 52, 52, 60, 52, 61, 52, 52, 62, 63, 64, 65, 3"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 128; ++i) {MAP0[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] MAP1 = new int[2349];
  static
  {
    final String s1[] =
    {
      /*    0 */ "432, 580, 1309, 1309, 1309, 463, 1245, 448, 1309, 493, 537, 512, 642, 1270, 688, 1201, 1339, 1351",
      /*   18 */ "553, 596, 612, 628, 658, 674, 714, 749, 765, 796, 568, 823, 839, 855, 871, 902, 1309, 1309, 698, 918",
      /*   38 */ "945, 961, 1308, 1309, 1309, 1309, 496, 982, 998, 1014, 527, 1030, 1488, 1460, 1054, 1070, 1092, 1108",
      /*   56 */ "1563, 1076, 1309, 886, 1309, 1309, 1380, 1124, 1140, 477, 1156, 1172, 1173, 1173, 1173, 1173, 1173",
      /*   73 */ "1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1173, 1189, 733",
      /*   90 */ "1217, 1233, 807, 1173, 1173, 1173, 1261, 1286, 1302, 1325, 1173, 1173, 1173, 1173, 1309, 1309, 1309",
      /*  107 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  124 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  141 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1038, 1309, 1309",
      /*  158 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  175 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  192 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  209 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  226 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  243 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  260 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  277 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  294 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  311 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 966, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  328 */ "1309, 1367, 1309, 1309, 1396, 1412, 728, 1428, 1451, 780, 1476, 1504, 929, 1520, 1536, 1435, 1309",
      /*  345 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  362 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  379 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  396 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  413 */ "1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309, 1309",
      /*  430 */ "1309, 1552, 1579, 1588, 1580, 1580, 1596, 1604, 1628, 1634, 1642, 1650, 1658, 1666, 1688, 1702, 1695",
      /*  447 */ "1710, 2257, 1759, 1854, 1854, 2015, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 2018, 1854",
      /*  464 */ "1854, 1854, 1854, 1854, 1854, 1854, 1854, 2103, 1854, 1860, 2185, 1857, 2197, 2185, 2185, 1854, 1857",
      /*  481 */ "2185, 2185, 2185, 2185, 2185, 2185, 2118, 2121, 2073, 2118, 2125, 2185, 1736, 1852, 1854, 1854, 1854",
      /*  498 */ "1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 2209, 1854, 1854, 1854, 2185, 2117",
      /*  515 */ "2118, 2118, 2118, 2118, 1774, 1746, 2185, 1854, 1854, 1854, 1859, 1859, 2185, 2075, 2118, 2124, 1854",
      /*  532 */ "1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 2185, 1853, 1854, 1854, 1854, 1855, 2254",
      /*  549 */ "1853, 1854, 1854, 1854, 2084, 1854, 1854, 1854, 1854, 1854, 1854, 1788, 2117, 2281, 1772, 1854, 1828",
      /*  566 */ "2118, 1853, 1853, 1854, 1854, 1854, 1854, 1854, 1939, 2123, 2155, 2119, 2118, 2124, 2185, 2185, 2185",
      /*  583 */ "2185, 2184, 2253, 1611, 2253, 1854, 1854, 1855, 1854, 1854, 1854, 1855, 1854, 1999, 2209, 1851, 1854",
      /*  600 */ "1854, 2016, 1673, 1846, 2140, 1868, 2185, 2229, 1828, 2118, 1860, 2185, 2241, 1877, 1851, 1854, 1854",
      /*  617 */ "2016, 1924, 2341, 2332, 1724, 2269, 1931, 2058, 2118, 1886, 2185, 2241, 2018, 2015, 1854, 1854, 2016",
      /*  634 */ "1927, 1846, 1738, 2281, 2255, 2185, 1828, 2118, 2185, 2185, 2118, 2123, 1854, 1854, 1854, 1854, 1854",
      /*  651 */ "2317, 2118, 2118, 2118, 1728, 2154, 1854, 1999, 2209, 1851, 1854, 1854, 2016, 1927, 1897, 2140, 2128",
      /*  668 */ "2127, 2229, 1828, 2118, 2254, 2185, 1871, 1915, 1874, 1920, 1794, 1915, 1854, 1860, 2125, 2128, 2255",
      /*  685 */ "2185, 2058, 2118, 2185, 2185, 2153, 1854, 1854, 1854, 2118, 2118, 2118, 1619, 1854, 1854, 1854, 1854",
      /*  702 */ "1854, 1854, 1854, 1854, 1854, 1874, 1855, 1874, 1854, 1854, 1854, 1854, 1962, 2012, 2016, 1854, 1854",
      /*  719 */ "2016, 2013, 2169, 2041, 2139, 2059, 1860, 1828, 2118, 2185, 2185, 2256, 1854, 1852, 1854, 1854, 1854",
      /*  736 */ "1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1857, 1838, 2101, 2185, 1962, 2012, 2016, 1854",
      /*  753 */ "1854, 2016, 2013, 1897, 2127, 2060, 2185, 2257, 1828, 2118, 1796, 2185, 1962, 2012, 2016, 1854, 1854",
      /*  770 */ "1854, 1854, 1947, 2140, 1868, 2185, 2185, 1828, 2118, 2185, 1852, 1854, 1854, 1854, 1854, 1854, 1858",
      /*  787 */ "2185, 2129, 2185, 2118, 2124, 2118, 2118, 1804, 2252, 1962, 1854, 1855, 1852, 1854, 1854, 2015, 2020",
      /*  804 */ "1855, 2268, 1975, 2185, 2185, 2185, 2185, 2185, 2256, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185",
      /*  821 */ "2185, 2185, 1678, 1680, 1963, 1853, 1758, 1754, 1939, 1955, 2019, 2120, 2118, 1620, 2185, 2185, 2185",
      /*  838 */ "2185, 2255, 2185, 2185, 2124, 2118, 2124, 1717, 2269, 1854, 1853, 1854, 1854, 1854, 1857, 2117, 2119",
      /*  855 */ "1775, 1807, 2118, 2117, 2118, 2118, 2118, 2121, 2127, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 1854",
      /*  872 */ "1854, 1854, 1854, 1854, 1972, 2116, 1907, 2118, 2124, 1856, 1985, 1792, 1961, 1942, 1854, 1854, 1854",
      /*  889 */ "1854, 1854, 1854, 1854, 1854, 2118, 2118, 2118, 2118, 2119, 2185, 2185, 2043, 1889, 1868, 2118, 1995",
      /*  906 */ "1854, 1854, 1854, 1854, 2018, 2258, 1854, 1854, 1854, 1854, 1854, 2014, 1854, 1874, 1854, 1854, 1854",
      /*  923 */ "1854, 1874, 1855, 1874, 1854, 1855, 1854, 1854, 1854, 1854, 1854, 1819, 1903, 2185, 2151, 2277, 2118",
      /*  940 */ "2124, 1854, 1854, 1855, 2253, 1854, 1854, 1874, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1972",
      /*  957 */ "2185, 2185, 2185, 2185, 1854, 1854, 2185, 2185, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854",
      /*  974 */ "1854, 1857, 2185, 2185, 2185, 2185, 2185, 2185, 2009, 1854, 1854, 1859, 1854, 1854, 1854, 1854, 1854",
      /*  991 */ "1854, 1854, 1854, 1854, 1859, 2185, 2185, 1854, 2012, 2028, 2185, 1854, 1854, 2028, 2185, 1854, 1854",
      /* 1008 */ "2038, 2185, 1854, 2012, 2331, 2185, 1854, 1854, 1854, 1854, 1854, 1854, 1987, 2120, 2127, 2117, 2141",
      /* 1025 */ "2052, 2118, 2124, 2185, 2185, 1854, 1854, 1854, 1854, 1854, 2305, 1854, 1854, 1854, 1854, 1854, 1854",
      /* 1042 */ "1854, 1854, 1856, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 1854, 1854, 2155, 2125, 1854",
      /* 1059 */ "1854, 1854, 1854, 1854, 1854, 2068, 2119, 1722, 2121, 2044, 2320, 2118, 2124, 2118, 2124, 2256, 2185",
      /* 1076 */ "2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2137, 2118, 2116, 2149, 2164, 2185, 2083",
      /* 1093 */ "1854, 1854, 1854, 1854, 1854, 2093, 1977, 2284, 1858, 2118, 2124, 2185, 2044, 2122, 2185, 2085, 1854",
      /* 1110 */ "1854, 1854, 1616, 1750, 2118, 1804, 1854, 1854, 1854, 1854, 2156, 2111, 2124, 2185, 1854, 1854, 1854",
      /* 1127 */ "1854, 1854, 1854, 2012, 2019, 2287, 1857, 2210, 1858, 1854, 1857, 2287, 1857, 2177, 2182, 2185, 2185",
      /* 1144 */ "2185, 2186, 2185, 2126, 2125, 2185, 2185, 2186, 2185, 2185, 2254, 2256, 2194, 1852, 2020, 2208, 2197",
      /* 1161 */ "1930, 1854, 2218, 1962, 1878, 2185, 2185, 2185, 2185, 2185, 2185, 1794, 2185, 2185, 2185, 2185, 2185",
      /* 1178 */ "2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 1854, 1854, 1854, 1854, 1854, 1855",
      /* 1195 */ "1854, 1854, 1854, 1854, 1854, 1855, 1854, 1854, 1854, 1854, 1806, 2118, 2306, 2185, 2118, 1804, 1854",
      /* 1212 */ "1854, 1854, 2317, 2099, 2253, 1854, 1854, 1854, 1854, 2018, 2258, 1854, 1854, 1854, 1854, 1854, 1854",
      /* 1229 */ "1854, 2256, 2185, 2126, 1854, 1854, 1855, 2185, 1855, 1855, 1855, 1855, 1855, 1855, 1855, 1855, 2118",
      /* 1246 */ "2118, 2118, 2118, 2118, 2118, 2118, 2118, 2118, 2118, 2118, 2118, 2118, 2118, 2012, 2220, 2228, 2185",
      /* 1263 */ "2185, 2185, 2185, 1832, 2208, 1794, 1853, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854",
      /* 1280 */ "1767, 2320, 1783, 2097, 2118, 1945, 1854, 1854, 1855, 2241, 1853, 1854, 1854, 1854, 1854, 1854, 1854",
      /* 1297 */ "1854, 1854, 1854, 1854, 2014, 1962, 1854, 1854, 1854, 1854, 1856, 1853, 1854, 1854, 1854, 1854, 1854",
      /* 1314 */ "1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1855, 2185, 2185, 1854, 1854",
      /* 1331 */ "1854, 1859, 2185, 2185, 2185, 2185, 2185, 2185, 1854, 1854, 1806, 1817, 1815, 2319, 2185, 2185, 1854",
      /* 1348 */ "1854, 1854, 2029, 2185, 2185, 2185, 2185, 2016, 1857, 2185, 2185, 2185, 2185, 2185, 2185, 2043, 2118",
      /* 1365 */ "2118, 2119, 1854, 1857, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 1854, 1854, 1854, 1854, 1854",
      /* 1382 */ "1856, 1856, 1854, 1854, 1854, 1854, 1856, 1856, 1854, 2200, 1854, 1854, 1854, 1856, 1854, 1857, 1854",
      /* 1399 */ "1854, 2118, 2101, 2185, 2185, 1854, 1854, 1854, 1854, 1854, 2155, 2043, 1820, 1854, 1854, 1854, 2126",
      /* 1416 */ "1854, 1854, 1854, 1854, 1854, 1854, 1854, 1854, 1856, 2185, 2124, 2185, 1854, 2219, 1858, 2185, 1854",
      /* 1433 */ "1859, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 1854, 1854, 1854, 1854, 2340, 2281, 2118",
      /* 1450 */ "2124, 2300, 2151, 1854, 1854, 2237, 2185, 2185, 2185, 1854, 1854, 1854, 1854, 1854, 1854, 1858, 2185",
      /* 1467 */ "2185, 1853, 2185, 2118, 2124, 2185, 2185, 2185, 2185, 2118, 1804, 1854, 1854, 1806, 2120, 1854, 1854",
      /* 1484 */ "2155, 2118, 2124, 2185, 1854, 1854, 1854, 1857, 2030, 2125, 2268, 1726, 2058, 2118, 1854, 1854, 1854",
      /* 1501 */ "1856, 1857, 2185, 2084, 1854, 1854, 1854, 1854, 1854, 1888, 2266, 2185, 2256, 2118, 2124, 2185, 2185",
      /* 1518 */ "2185, 2185, 1854, 1854, 1854, 1854, 1854, 1854, 2295, 2314, 2305, 2185, 2185, 2001, 1854, 2329, 2244",
      /* 1535 */ "2185, 2017, 2017, 2017, 2185, 1855, 1855, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185, 2185",
      /* 1552 */ "1854, 1854, 1854, 1854, 1858, 2185, 1854, 1854, 1855, 1964, 1854, 1854, 1854, 1854, 1854, 1858, 2043",
      /* 1569 */ "2321, 2185, 2118, 1836, 2118, 1804, 1854, 1854, 1854, 1856, 67, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0",
      /* 1592 */ "0, 2, 0, 0, 1, 3, 4, 5, 3, 3, 3, 6, 7, 8, 9, 10, 11, 12, 13, 3, 3, 3, 3, 3, 49, 3, 66, 66, 66, 66, 3",
      /* 1623 */ "3, 49, 49, 49, 49, 14, 14, 14, 14, 14, 14, 14, 14, 15, 16, 3, 17, 3, 18, 19, 20, 21, 22, 23, 24, 25",
      /* 1649 */ "26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 3, 47, 48, 49, 3",
      /* 1675 */ "49, 3, 3, 3, 49, 49, 3, 49, 3, 3, 49, 3, 3, 3, 50, 50, 50, 50, 51, 50, 52, 52, 58, 59, 52, 52, 60",
      /* 1702 */ "52, 53, 52, 52, 54, 55, 56, 57, 61, 52, 52, 62, 63, 64, 65, 3, 3, 3, 3, 3, 66, 3, 66, 3, 3, 66, 66",
      /* 1729 */ "66, 3, 3, 3, 3, 49, 49, 49, 49, 3, 66, 66, 66, 66, 66, 3, 66, 3, 66, 66, 3, 66, 66, 3, 66, 3, 3, 49",
      /* 1757 */ "49, 3, 49, 49, 49, 3, 49, 3, 49, 49, 49, 49, 49, 49, 3, 49, 66, 66, 66, 66, 66, 66, 66, 3, 66, 66",
      /* 1783 */ "66, 66, 66, 66, 66, 49, 49, 66, 3, 66, 49, 3, 3, 3, 49, 49, 3, 3, 3, 3, 3, 66, 66, 49, 49, 49, 49",
      /* 1810 */ "49, 49, 66, 66, 66, 66, 66, 66, 66, 49, 66, 66, 66, 66, 66, 66, 3, 49, 49, 49, 66, 66, 3, 3, 66, 66",
      /* 1836 */ "66, 66, 3, 3, 3, 49, 49, 49, 49, 66, 49, 49, 3, 3, 66, 49, 3, 3, 49, 49, 49, 49, 49, 49, 49, 49, 3",
      /* 1863 */ "3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 66, 49, 3, 49, 49, 49, 49, 3, 3, 3, 3, 49, 3, 66, 66, 49, 49, 49, 66",
      /* 1892 */ "3, 3, 66, 66, 3, 49, 49, 3, 3, 66, 49, 3, 66, 66, 3, 3, 66, 66, 3, 3, 66, 66, 49, 49, 49, 49, 3, 3",
      /* 1920 */ "3, 49, 49, 3, 49, 3, 49, 49, 3, 49, 49, 3, 49, 49, 49, 49, 3, 49, 3, 49, 66, 49, 49, 66, 66, 66, 66",
      /* 1947 */ "49, 49, 49, 3, 3, 49, 3, 3, 66, 66, 3, 66, 66, 49, 3, 3, 3, 3, 3, 3, 49, 49, 49, 49, 49, 49, 49, 49",
      /* 1975 */ "3, 3, 66, 66, 66, 3, 66, 3, 3, 3, 66, 66, 49, 49, 49, 49, 66, 66, 3, 66, 66, 66, 3, 3, 3, 66, 3, 3",
      /* 2003 */ "3, 49, 49, 49, 3, 3, 1, 49, 49, 49, 49, 49, 49, 49, 3, 49, 49, 49, 49, 49, 49, 3, 49, 3, 3, 49, 49",
      /* 2030 */ "66, 66, 66, 3, 3, 3, 3, 66, 49, 49, 66, 66, 3, 3, 3, 3, 3, 66, 66, 66, 66, 66, 3, 3, 3, 3, 49, 66, 3",
      /* 2059 */ "3, 3, 3, 3, 3, 66, 66, 3, 3, 49, 49, 49, 49, 49, 3, 66, 3, 3, 3, 66, 66, 66, 1, 3, 66, 66, 66, 66, 3",
      /* 2088 */ "49, 49, 49, 49, 49, 49, 49, 49, 49, 66, 3, 66, 66, 66, 66, 49, 49, 3, 3, 3, 3, 49, 49, 66, 66, 3, 3",
      /* 2115 */ "3, 66, 3, 66, 66, 66, 66, 66, 66, 66, 66, 3, 3, 3, 3, 3, 3, 3, 66, 3, 3, 3, 66, 66, 66, 3, 66, 66",
      /* 2143 */ "66, 66, 3, 3, 3, 49, 66, 49, 49, 49, 49, 66, 49, 49, 49, 49, 49, 49, 49, 66, 3, 49, 49, 3, 3, 66, 49",
      /* 2170 */ "49, 3, 3, 3, 49, 66, 66, 1, 1, 1, 1, 1, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 3, 1, 3, 3, 49, 3, 3, 3, 3, 49",
      /* 2202 */ "3, 49, 3, 49, 3, 49, 3, 49, 49, 49, 49, 49, 3, 3, 49, 49, 49, 49, 3, 3, 49, 49, 49, 49, 3, 3, 1, 3",
      /* 2230 */ "3, 3, 3, 49, 49, 3, 49, 49, 49, 49, 3, 3, 66, 66, 3, 3, 49, 49, 49, 3, 66, 3, 3, 3, 3, 49, 3, 3, 3",
      /* 2259 */ "3, 3, 3, 3, 49, 3, 3, 66, 66, 3, 3, 66, 3, 3, 3, 3, 3, 3, 49, 49, 49, 49, 66, 3, 3, 3, 3, 66, 3, 3",
      /* 2289 */ "49, 49, 49, 3, 49, 49, 66, 49, 66, 66, 66, 49, 49, 66, 49, 49, 49, 66, 49, 3, 3, 3, 3, 3, 3, 66, 49",
      /* 2316 */ "49, 49, 49, 49, 66, 66, 66, 66, 66, 3, 3, 66, 66, 49, 49, 49, 3, 66, 66, 3, 3, 3, 3, 66, 49, 49, 49",
      /* 2343 */ "3, 3, 66, 3, 3, 3"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2349; ++i) {MAP1[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] MAP2 = new int[1050];
  static
  {
    final String s1[] =
    {
      /*    0 */ "57344, 63744, 64110, 64112, 64218, 64256, 64263, 64275, 64280, 64285, 64286, 64287, 64297, 64298",
      /*   14 */ "64311, 64312, 64317, 64318, 64319, 64320, 64322, 64323, 64325, 64326, 64434, 64467, 64830, 64848",
      /*   28 */ "64912, 64914, 64968, 65008, 65020, 65024, 65040, 65056, 65063, 65136, 65141, 65142, 65277, 65296",
      /*   42 */ "65306, 65313, 65339, 65345, 65371, 65382, 65471, 65474, 65480, 65482, 65488, 65490, 65496, 65498",
      /*   56 */ "65501, 65536, 65548, 65549, 65575, 65576, 65595, 65596, 65598, 65599, 65614, 65616, 65630, 65664",
      /*   70 */ "65787, 66045, 66046, 66176, 66205, 66208, 66257, 66304, 66335, 66352, 66369, 66370, 66378, 66432",
      /*   84 */ "66462, 66464, 66500, 66504, 66512, 66560, 66718, 66720, 66730, 67584, 67590, 67592, 67593, 67594",
      /*   98 */ "67638, 67639, 67641, 67644, 67645, 67647, 67670, 67840, 67862, 67872, 67898, 67968, 68024, 68030",
      /*  112 */ "68032, 68096, 68097, 68100, 68101, 68103, 68108, 68112, 68116, 68117, 68120, 68121, 68148, 68152",
      /*  126 */ "68155, 68159, 68160, 68192, 68221, 68352, 68406, 68416, 68438, 68448, 68467, 68608, 68681, 69633",
      /*  140 */ "69634, 69635, 69688, 69703, 69734, 69744, 69760, 69762, 69763, 69808, 69811, 69815, 69817, 69819",
      /*  154 */ "69840, 69865, 69872, 69882, 69888, 69891, 69927, 69932, 69933, 69941, 69942, 69952, 70016, 70018",
      /*  168 */ "70019, 70067, 70070, 70079, 70081, 70085, 70096, 70106, 71296, 71339, 71340, 71341, 71342, 71344",
      /*  182 */ "71350, 71351, 71352, 71360, 71370, 73728, 74607, 77824, 78895, 92160, 92729, 93952, 94021, 94032",
      /*  196 */ "94033, 94095, 94099, 94112, 110592, 110594, 119143, 119146, 119163, 119171, 119173, 119180, 119210",
      /*  209 */ "119214, 119362, 119365, 119808, 119893, 119894, 119965, 119966, 119968, 119970, 119971, 119973",
      /*  221 */ "119975, 119977, 119981, 119982, 119994, 119995, 119996, 119997, 120004, 120005, 120070, 120071",
      /*  233 */ "120075, 120077, 120085, 120086, 120093, 120094, 120122, 120123, 120127, 120128, 120133, 120134",
      /*  245 */ "120135, 120138, 120145, 120146, 120486, 120488, 120513, 120514, 120539, 120540, 120571, 120572",
      /*  257 */ "120597, 120598, 120629, 120630, 120655, 120656, 120687, 120688, 120713, 120714, 120745, 120746",
      /*  269 */ "120771, 120772, 120780, 120782, 120832, 126464, 126468, 126469, 126496, 126497, 126499, 126500",
      /*  281 */ "126501, 126503, 126504, 126505, 126515, 126516, 126520, 126521, 126522, 126523, 126524, 126530",
      /*  293 */ "126531, 126535, 126536, 126537, 126538, 126539, 126540, 126541, 126544, 126545, 126547, 126548",
      /*  305 */ "126549, 126551, 126552, 126553, 126554, 126555, 126556, 126557, 126558, 126559, 126560, 126561",
      /*  317 */ "126563, 126564, 126565, 126567, 126571, 126572, 126579, 126580, 126584, 126585, 126589, 126590",
      /*  329 */ "126591, 126592, 126602, 126603, 126620, 126625, 126628, 126629, 126634, 126635, 126652, 131072",
      /*  341 */ "173783, 173824, 177973, 177984, 178206, 194560, 195102, 917760, 918000, 63743, 64109, 64111, 64217",
      /*  354 */ "64255, 64262, 64274, 64279, 64284, 64285, 64286, 64296, 64297, 64310, 64311, 64316, 64317, 64318",
      /*  368 */ "64319, 64321, 64322, 64324, 64325, 64433, 64466, 64829, 64847, 64911, 64913, 64967, 65007, 65019",
      /*  382 */ "65023, 65039, 65055, 65062, 65135, 65140, 65141, 65276, 65295, 65305, 65312, 65338, 65344, 65370",
      /*  396 */ "65381, 65470, 65473, 65479, 65481, 65487, 65489, 65495, 65497, 65500, 65533, 65547, 65548, 65574",
      /*  410 */ "65575, 65594, 65595, 65597, 65598, 65613, 65615, 65629, 65663, 65786, 66044, 66045, 66175, 66204",
      /*  424 */ "66207, 66256, 66303, 66334, 66351, 66368, 66369, 66377, 66431, 66461, 66463, 66499, 66503, 66511",
      /*  438 */ "66559, 66717, 66719, 66729, 67583, 67589, 67591, 67592, 67593, 67637, 67638, 67640, 67643, 67644",
      /*  452 */ "67646, 67669, 67839, 67861, 67871, 67897, 67967, 68023, 68029, 68031, 68095, 68096, 68099, 68100",
      /*  466 */ "68102, 68107, 68111, 68115, 68116, 68119, 68120, 68147, 68151, 68154, 68158, 68159, 68191, 68220",
      /*  480 */ "68351, 68405, 68415, 68437, 68447, 68466, 68607, 68680, 69632, 69633, 69634, 69687, 69702, 69733",
      /*  494 */ "69743, 69759, 69761, 69762, 69807, 69810, 69814, 69816, 69818, 69839, 69864, 69871, 69881, 69887",
      /*  508 */ "69890, 69926, 69931, 69932, 69940, 69941, 69951, 70015, 70017, 70018, 70066, 70069, 70078, 70080",
      /*  522 */ "70084, 70095, 70105, 71295, 71338, 71339, 71340, 71341, 71343, 71349, 71350, 71351, 71359, 71369",
      /*  536 */ "73727, 74606, 77823, 78894, 92159, 92728, 93951, 94020, 94031, 94032, 94094, 94098, 94111, 110591",
      /*  550 */ "110593, 119142, 119145, 119162, 119170, 119172, 119179, 119209, 119213, 119361, 119364, 119807",
      /*  562 */ "119892, 119893, 119964, 119965, 119967, 119969, 119970, 119972, 119974, 119976, 119980, 119981",
      /*  574 */ "119993, 119994, 119995, 119996, 120003, 120004, 120069, 120070, 120074, 120076, 120084, 120085",
      /*  586 */ "120092, 120093, 120121, 120122, 120126, 120127, 120132, 120133, 120134, 120137, 120144, 120145",
      /*  598 */ "120485, 120487, 120512, 120513, 120538, 120539, 120570, 120571, 120596, 120597, 120628, 120629",
      /*  610 */ "120654, 120655, 120686, 120687, 120712, 120713, 120744, 120745, 120770, 120771, 120779, 120781",
      /*  622 */ "120831, 126463, 126467, 126468, 126495, 126496, 126498, 126499, 126500, 126502, 126503, 126504",
      /*  634 */ "126514, 126515, 126519, 126520, 126521, 126522, 126523, 126529, 126530, 126534, 126535, 126536",
      /*  646 */ "126537, 126538, 126539, 126540, 126543, 126544, 126546, 126547, 126548, 126550, 126551, 126552",
      /*  658 */ "126553, 126554, 126555, 126556, 126557, 126558, 126559, 126560, 126562, 126563, 126564, 126566",
      /*  670 */ "126570, 126571, 126578, 126579, 126583, 126584, 126588, 126589, 126590, 126591, 126601, 126602",
      /*  682 */ "126619, 126624, 126627, 126628, 126633, 126634, 126651, 131071, 173782, 173823, 177972, 177983",
      /*  694 */ "178205, 194559, 195101, 917759, 917999, 1114111, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 66, 49, 3, 49, 3",
      /*  715 */ "49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 66, 3, 66, 3, 49, 3, 49, 3, 66, 3, 49",
      /*  744 */ "3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 66, 3",
      /*  773 */ "49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 66, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49",
      /*  802 */ "3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 66, 3, 66, 3, 66, 49, 3, 49, 3, 49, 3, 66, 3, 66, 3, 49, 3",
      /*  831 */ "49, 3, 49, 3, 49, 3, 49, 3, 66, 3, 49, 66, 3, 66, 3, 66, 3, 49, 3, 66, 3, 66, 3, 49, 3, 66, 3, 66",
      /*  859 */ "49, 66, 3, 66, 3, 66, 3, 66, 3, 49, 3, 66, 3, 49, 3, 66, 3, 49, 66, 3, 66, 3, 66, 3, 66, 3, 66, 3",
      /*  887 */ "49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 66, 49, 3, 49, 3, 66, 3, 66, 3, 66, 3, 66, 3, 66, 3, 49, 3, 49, 3",
      /*  916 */ "49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49",
      /*  945 */ "3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 66, 3",
      /*  974 */ "49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49",
      /* 1003 */ "3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3",
      /* 1032 */ "49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 49, 3, 66, 3"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1050; ++i) {MAP2[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] INITIAL = new int[67];
  static
  {
    final String s1[] =
    {
      /*  0 */ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28",
      /* 28 */ "29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54",
      /* 54 */ "55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 67; ++i) {INITIAL[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TRANSITION = new int[2258];
  static
  {
    final String s1[] =
    {
      /*    0 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*   17 */ "2249, 1125, 1088, 1110, 1112, 1104, 1120, 1173, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*   34 */ "1093, 1088, 1110, 1112, 1104, 1120, 1173, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130",
      /*   51 */ "1177, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1188, 2249, 1560, 1319",
      /*   68 */ "1691, 1185, 1688, 1714, 1719, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1340, 2125",
      /*   85 */ "1756, 1751, 1782, 1787, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1582, 2249, 1923, 1361, 1817, 1819",
      /*  102 */ "1814, 1842, 1847, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 2249",
      /*  119 */ "1874, 1879, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1200, 2249, 1130, 1177, 1199, 1449, 2248, 1446",
      /*  136 */ "2246, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 1957, 1208, 2249, 1918",
      /*  153 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 1608, 1217, 1906, 1912, 2249",
      /*  170 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 1722, 2145, 1727, 2249, 1725, 2249, 2249",
      /*  187 */ "2249, 2249, 2249, 2249, 2249, 1227, 2249, 1130, 1952, 1218, 1946, 1226, 1235, 1240, 2249, 2249, 2249",
      /*  204 */ "2249, 2249, 2249, 2249, 1280, 2249, 1130, 1980, 1252, 2195, 2202, 1759, 2200, 2249, 2249, 2249, 2249",
      /*  221 */ "2249, 2249, 2249, 1268, 2249, 1130, 1177, 2249, 1209, 1278, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  238 */ "2249, 2249, 2249, 2249, 1165, 1791, 2249, 2249, 2010, 2249, 2008, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  255 */ "2249, 2249, 2249, 1130, 1883, 1288, 1290, 1300, 1470, 1298, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  272 */ "2249, 2249, 2130, 1586, 2249, 2249, 2028, 2249, 2026, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  289 */ "2249, 1130, 1177, 2249, 1985, 1260, 2249, 1258, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  306 */ "1130, 1177, 2249, 2249, 2012, 1310, 1315, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1268, 2249, 1130",
      /*  323 */ "1177, 2249, 1209, 1327, 1328, 1336, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1268, 2249, 1130, 1177",
      /*  340 */ "2249, 1209, 1348, 1349, 1357, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1268, 2249, 1130, 1177, 2249",
      /*  357 */ "1209, 1369, 1370, 1378, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1268, 2249, 1130, 1177, 2249, 1209",
      /*  374 */ "1391, 1392, 1400, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1268, 2249, 1130, 1177, 2249, 1209, 1412",
      /*  391 */ "1413, 1421, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1268, 2249, 1130, 1177, 2249, 1209, 1433, 1434",
      /*  408 */ "1442, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1457, 1458, 1466",
      /*  425 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1478, 1479, 1487, 2249",
      /*  442 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1499, 1500, 1508, 2249, 2249",
      /*  459 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1520, 1521, 1529, 2249, 2249, 2249",
      /*  476 */ "2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1541, 1542, 1550, 2249, 2249, 2249, 2249",
      /*  493 */ "2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1568, 1569, 1577, 2249, 2249, 2249, 2249, 2249",
      /*  510 */ "2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1594, 1595, 1603, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  527 */ "2249, 2249, 2249, 1130, 1177, 2249, 2249, 1616, 1617, 1625, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  544 */ "2249, 2249, 1130, 1177, 2249, 2249, 1638, 1639, 1647, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  561 */ "2249, 1130, 1177, 2249, 2249, 1673, 1674, 1682, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  578 */ "1130, 1177, 2249, 2249, 1699, 1700, 1708, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130",
      /*  595 */ "1177, 2249, 2249, 1736, 1737, 1745, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177",
      /*  612 */ "2249, 2249, 1767, 1768, 1776, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249",
      /*  629 */ "2249, 1799, 1800, 1808, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249",
      /*  646 */ "1827, 1828, 1836, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1859",
      /*  663 */ "1860, 1868, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1891, 1892",
      /*  680 */ "1900, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1931, 1932, 1940",
      /*  697 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1965, 1966, 1974, 2249",
      /*  714 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1993, 1994, 2002, 2249, 2249",
      /*  731 */ "2249, 2249, 2249, 2249, 2249, 1728, 2249, 1135, 1630, 1491, 2020, 2249, 1140, 1145, 2249, 2249, 2249",
      /*  748 */ "2249, 2249, 2249, 2249, 2249, 2249, 1130, 1851, 1383, 1096, 1382, 2249, 1653, 2249, 2249, 2249, 2249",
      /*  765 */ "2249, 2249, 2249, 2249, 2249, 1130, 1177, 2249, 2084, 1302, 2036, 2041, 2249, 2249, 2249, 2249, 2249",
      /*  782 */ "2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  799 */ "2249, 1268, 2249, 1130, 1177, 1270, 1209, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  816 */ "1268, 2078, 1130, 1177, 1270, 1209, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /*  833 */ "2249, 1130, 1177, 1270, 2249, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2097",
      /*  850 */ "1130, 1177, 1270, 2249, 2092, 2105, 2110, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1659, 1130",
      /*  867 */ "1177, 1270, 2249, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1555, 1130, 1177",
      /*  884 */ "1270, 2249, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1404, 1130, 1177, 1270",
      /*  901 */ "2249, 2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 1191, 1130, 1177, 1270, 2249",
      /*  918 */ "2049, 2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2204, 1130, 1177, 1270, 2249, 2049",
      /*  935 */ "2065, 2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2250, 1130, 1177, 1270, 2249, 2049, 2065",
      /*  952 */ "2070, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2118, 1177, 1665, 2249, 2049, 2065, 2070",
      /*  969 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2239, 1177, 1270, 2249, 2138, 2065, 2070, 2249",
      /*  986 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2180, 2153, 2165, 2167, 2157, 2175, 2188, 2249, 2249",
      /* 1003 */ "2249, 2249, 2249, 2249, 2249, 2249, 2249, 1130, 1244, 2212, 2214, 2224, 1512, 2222, 2249, 2249, 2249",
      /* 1020 */ "2249, 2249, 2249, 2249, 2249, 2249, 1130, 1425, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /* 1037 */ "2249, 2249, 2249, 2249, 2249, 1150, 1177, 1533, 2232, 2249, 1155, 1160, 2249, 2249, 2249, 2249, 2249",
      /* 1054 */ "2249, 2249, 2249, 2249, 1130, 1177, 2249, 2249, 1278, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /* 1071 */ "2249, 2056, 2249, 2249, 2057, 2249, 2249, 2249, 2056, 2249, 2249, 2249, 2249, 2249, 2249, 2249, 2249",
      /* 1088 */ "5956, 5956, 5956, 5956, 256, 0, 0, 5956, 0, 0, 0, 0, 0, 4608, 4608, 4608, 5956, 0, 0, 5956, 0, 5956",
      /* 1110 */ "0, 5956, 5956, 5956, 0, 0, 5956, 5956, 5956, 5956, 5956, 5956, 0, 5956, 0, 0, 0, 5956, 512, 640, 0",
      /* 1131 */ "0, 0, 512, 640, 0, 0, 0, 512, 640, 0, 0, 8192, 8192, 8192, 8192, 8192, 8192, 0, 0, 0, 0, 0, 512, 640",
      /* 1155 */ "0, 0, 8960, 8960, 8960, 8960, 8960, 8960, 0, 0, 0, 0, 0, 512, 640, 0, 7680, 0, 5956, 5956, 5956",
      /* 1176 */ "5956, 0, 0, 0, 0, 256, 0, 0, 0, 6528, 0, 6528, 0, 0, 6528, 0, 0, 0, 0, 0, 5248, 0, 0, 7040, 0, 0, 0",
      /* 1203 */ "0, 7040, 0, 0, 0, 837, 0, 0, 0, 0, 0, 0, 0, 6144, 7238, 0, 0, 0, 0, 0, 0, 0, 7424, 6016, 0, 0, 0, 0",
      /* 1231 */ "0, 7424, 0, 0, 7424, 7424, 7424, 7424, 7424, 7424, 7424, 7424, 0, 0, 0, 0, 0, 256, 0, 8832, 0, 7552",
      /* 1253 */ "0, 0, 0, 0, 7552, 0, 0, 1152, 0, 0, 0, 0, 0, 0, 0, 6144, 0, 0, 0, 0, 0, 0, 0, 6272, 0, 6016, 0, 0, 0",
      /* 1282 */ "0, 0, 0, 0, 7552, 0, 7808, 0, 0, 7808, 0, 7808, 7808, 7808, 7808, 7808, 7808, 7808, 7808, 0, 0, 0, 0",
      /* 1305 */ "0, 0, 0, 8320, 0, 8064, 8064, 8064, 8064, 8064, 8064, 8064, 8064, 0, 0, 0, 0, 0, 256, 6528, 0, 0",
      /* 1327 */ "6016, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 0, 0, 0, 0, 0, 256, 6656, 0",
      /* 1347 */ "0, 6016, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 0, 0, 0, 0, 0, 256, 6784",
      /* 1367 */ "0, 0, 6016, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 0, 0, 0, 0, 0, 4608, 0",
      /* 1388 */ "0, 4608, 4608, 6016, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 0, 0, 0, 0, 0",
      /* 1408 */ "5120, 0, 0, 0, 6016, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 0, 0, 0, 0, 0",
      /* 1429 */ "5760, 0, 0, 0, 6016, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 0, 0, 0, 0, 0",
      /* 1450 */ "7040, 0, 7040, 7040, 7040, 7040, 7040, 6016, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048",
      /* 1467 */ "2048, 2048, 0, 0, 0, 0, 0, 7808, 7808, 7808, 7808, 6016, 2176, 2176, 2176, 2176, 2176, 2176, 2176",
      /* 1486 */ "2176, 2176, 2176, 2176, 0, 0, 0, 0, 0, 8192, 0, 0, 0, 6016, 2304, 2304, 2304, 2304, 2304, 2304, 2304",
      /* 1507 */ "2304, 2304, 2304, 2304, 0, 0, 0, 0, 0, 8832, 8832, 8832, 8832, 6016, 2432, 2432, 2432, 2432, 2432",
      /* 1526 */ "2432, 2432, 2432, 2432, 2432, 2432, 0, 0, 0, 0, 0, 8960, 0, 0, 0, 6016, 2560, 2560, 2560, 2560, 2560",
      /* 1547 */ "2560, 2560, 2560, 2560, 2560, 2560, 0, 0, 0, 0, 0, 4992, 0, 0, 0, 0, 6528, 640, 6528, 0, 0, 6016",
      /* 1569 */ "2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 0, 0, 0, 0, 0, 6784, 0, 0, 0, 0",
      /* 1590 */ "256, 0, 0, 7936, 6016, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 0, 0, 0, 0",
      /* 1610 */ "0, 7238, 7238, 7238, 7238, 7238, 6016, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944",
      /* 1627 */ "2944, 0, 0, 0, 0, 0, 8192, 256, 0, 0, 0, 6016, 3072, 3072, 3072, 3072, 3072, 3072, 3072, 3072, 3072",
      /* 1648 */ "3072, 3072, 0, 0, 0, 0, 0, 4608, 0, 0, 0, 0, 0, 4864, 0, 0, 0, 0, 0, 5632, 0, 0, 0, 6272, 0, 6016",
      /* 1674 */ "3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 0, 0, 0, 0, 0, 6528, 0, 6528, 6528",
      /* 1694 */ "0, 6528, 0, 0, 0, 6016, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 0, 0, 0, 0",
      /* 1715 */ "0, 6528, 6528, 6528, 6528, 6528, 6528, 0, 0, 0, 0, 0, 7296, 0, 0, 0, 0, 0, 0, 0, 8192, 6016, 3456",
      /* 1738 */ "3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 0, 0, 0, 0, 0, 6656, 0, 6656, 6656, 0",
      /* 1758 */ "6656, 0, 0, 0, 0, 0, 7552, 7552, 0, 6016, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 3584",
      /* 1778 */ "3584, 0, 0, 0, 0, 0, 6656, 6656, 6656, 6656, 6656, 6656, 0, 0, 0, 0, 0, 256, 0, 0, 7680, 6016, 3712",
      /* 1801 */ "3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 0, 0, 0, 0, 0, 6784, 0, 6784, 6784, 0",
      /* 1821 */ "6784, 0, 0, 0, 6784, 0, 6016, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 0, 0",
      /* 1841 */ "0, 0, 0, 6784, 6784, 6784, 6784, 6784, 6784, 0, 0, 0, 0, 0, 256, 0, 4608, 0, 6016, 3968, 3968, 3968",
      /* 1863 */ "3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 0, 0, 0, 0, 0, 6912, 6912, 6912, 6912, 6912, 6912, 0",
      /* 1883 */ "0, 0, 0, 0, 256, 0, 7808, 0, 6016, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 4096",
      /* 1903 */ "0, 0, 0, 0, 0, 7168, 7168, 7168, 7168, 7168, 7168, 7238, 0, 0, 1024, 0, 0, 837, 0, 896, 0, 0, 0, 512",
      /* 1927 */ "6784, 6784, 0, 0, 6016, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 0, 0, 0, 0",
      /* 1947 */ "0, 7424, 0, 0, 0, 0, 7424, 0, 0, 256, 0, 0, 0, 837, 837, 837, 837, 837, 6016, 4352, 4352, 4352, 4352",
      /* 1970 */ "4352, 4352, 4352, 4352, 4352, 4352, 4352, 0, 0, 0, 0, 0, 7552, 0, 256, 0, 0, 0, 1152, 1152, 1152",
      /* 1991 */ "1152, 1152, 6016, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 0, 0, 0, 0, 0",
      /* 2010 */ "7680, 0, 0, 0, 0, 0, 0, 0, 8064, 0, 8192, 0, 8192, 0, 0, 0, 0, 0, 7936, 0, 0, 0, 0, 0, 0, 0, 8320",
      /* 2037 */ "8320, 8320, 8320, 8320, 8320, 8320, 8320, 0, 0, 0, 0, 0, 6016, 384, 0, 384, 0, 0, 384, 0, 6400, 0, 0",
      /* 2060 */ "0, 0, 0, 0, 0, 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 0, 0, 0, 4736, 0, 0, 0, 0, 0, 0, 0",
      /* 2086 */ "8320, 0, 0, 0, 0, 0, 6016, 8448, 0, 8448, 0, 0, 8448, 0, 0, 0, 0, 0, 0, 8448, 8448, 8448, 8448, 8448",
      /* 2110 */ "8448, 8448, 8448, 0, 0, 0, 0, 0, 5632, 0, 0, 512, 640, 0, 0, 0, 6656, 0, 0, 6656, 0, 0, 0, 512, 640",
      /* 2135 */ "0, 7936, 0, 8576, 384, 0, 384, 0, 0, 384, 0, 7296, 0, 7296, 7296, 7296, 7296, 7296, 8704, 8704, 8704",
      /* 2156 */ "8704, 8704, 0, 0, 8704, 0, 8704, 0, 8704, 0, 8704, 8704, 8704, 0, 0, 8704, 8704, 8704, 8704, 8704",
      /* 2176 */ "8704, 0, 8704, 0, 0, 0, 8704, 512, 640, 0, 0, 0, 8704, 8704, 8704, 0, 0, 0, 0, 0, 7552, 0, 7552",
      /* 2199 */ "7552, 7552, 7552, 7552, 0, 0, 0, 0, 0, 0, 0, 5376, 0, 8832, 0, 0, 8832, 0, 8832, 8832, 8832, 8832",
      /* 2221 */ "8832, 8832, 8832, 8832, 0, 0, 0, 0, 0, 0, 0, 8960, 0, 8960, 0, 0, 0, 0, 0, 8576, 0, 512, 640, 0, 0",
      /* 2246 */ "0, 7040, 7040, 0, 0, 0, 0, 0, 0, 0, 0, 5504"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2258; ++i) {TRANSITION[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] EXPECTED = new int[191];
  static
  {
    final String s1[] =
    {
      /*   0 */ "80, 80, 80, 80, 81, 79, 80, 93, 80, 80, 148, 53, 56, 65, 59, 59, 61, 69, 73, 77, 85, 89, 98, 102, 106",
      /*  25 */ "110, 114, 118, 122, 126, 130, 134, 138, 142, 146, 80, 80, 92, 80, 183, 94, 169, 185, 187, 152, 158",
      /*  46 */ "154, 162, 166, 173, 177, 180, 80, 480, 480, 480, 480, -508, -512, -508, -508, -508, -508, -28, 0",
      /*  65 */ "-512, -512, -508, -512, 64, 128, 32768, 131072, 262144, 1048576, 4194304, 33554432, 67108864",
      /*  78 */ "-2147483648, 16, 0, 0, 0, 0, 8, 32, 64, 128, 256, 512, 1024, 2048, 0, 2, 0, 0, 0, 32, 8192, 262144",
      /* 100 */ "1048576, 1310720, 671088640, -2147483648, 139264, 33562624, 67117056, -2147475456, 4096, 1835008",
      /* 110 */ "268435464, 671096832, 339738624, 1843200, 1320960, 268443656, -2145648640, 356515840, 268509192",
      /* 119 */ "301998088, -2145640448, 356524032, -2112094208, 364904448, 364912640, 365174792, 365961224, 398499848",
      /* 128 */ "1036017664, 7, 1835015, 8199, 1835023, 1843207, 1107296263, 1843215, 1107304455, 1107435527",
      /* 138 */ "-1027866617, -1027858425, -755236857, -692322297, -688127993, -755228665, -692314105, -688119801",
      /* 146 */ "-253937, 8192, 0, 0, 0, 480, 32, 16, 24, 24, 24, 24, 40, 24, 33, 16, 28, 2, 0, 10, 0, 8, 3, 8, 8, 8",
      /* 172 */ "8, 11, 11, 35, 43, 51, 51, 51, 59, 59, 59, 0, 4, 8, 0, 16, 8, 8, 24"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 191; ++i) {EXPECTED[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] CASEID = new int[1619];
  static
  {
    final String s1[] =
    {
      /*    0 */ "555, 558, 562, 563, 567, 698, 784, 572, 732, 576, 581, 584, 588, 609, 595, 600, 784, 603, 783, 607",
      /*   20 */ "613, 589, 577, 589, 618, 581, 584, 588, 609, 623, 698, 784, 572, 732, 576, 589, 589, 614, 789, 618",
      /*   40 */ "628, 631, 635, 589, 640, 698, 784, 572, 589, 645, 698, 784, 572, 589, 645, 698, 784, 572, 589, 645",
      /*   60 */ "581, 584, 588, 609, 623, 650, 653, 657, 710, 661, 589, 589, 619, 766, 666, 671, 674, 678, 589, 683",
      /*   80 */ "589, 589, 568, 591, 618, 915, 589, 679, 931, 688, 967, 589, 624, 960, 694, 589, 589, 790, 589, 618",
      /*  100 */ "698, 784, 572, 702, 607, 915, 589, 679, 931, 688, 589, 589, 690, 706, 618, 714, 717, 721, 726, 730",
      /*  120 */ "581, 584, 588, 736, 740, 698, 784, 572, 746, 607, 628, 631, 635, 589, 640, 589, 589, 589, 750, 754",
      /*  140 */ "698, 784, 572, 760, 607, 589, 589, 568, 764, 618, 714, 717, 721, 770, 730, 912, 589, 568, 918, 774",
      /*  160 */ "698, 784, 572, 780, 788, 589, 589, 568, 970, 774, 698, 784, 572, 780, 788, 589, 589, 568, 970, 774",
      /*  180 */ "698, 784, 572, 780, 788, 589, 589, 568, 970, 774, 589, 589, 568, 794, 801, 912, 589, 568, 918, 774",
      /*  200 */ "906, 589, 641, 895, 807, 589, 589, 589, 636, 990, 589, 589, 684, 589, 589, 813, 816, 820, 826, 830",
      /*  220 */ "589, 589, 589, 836, 589, 581, 584, 588, 842, 846, 581, 584, 588, 842, 846, 581, 584, 588, 842, 846",
      /*  240 */ "589, 589, 589, 852, 863, 589, 589, 568, 590, 618, 803, 784, 603, 764, 618, 714, 717, 721, 856, 730",
      /*  260 */ "1021, 589, 662, 1002, 860, 987, 589, 667, 980, 869, 876, 589, 589, 886, 589, 892, 589, 589, 892, 589",
      /*  280 */ "589, 589, 589, 636, 589, 809, 899, 903, 922, 589, 912, 589, 568, 918, 774, 589, 589, 797, 596, 774",
      /*  300 */ "589, 589, 568, 708, 618, 912, 589, 568, 918, 774, 589, 589, 790, 589, 618, 589, 589, 589, 935, 589",
      /*  320 */ "589, 589, 589, 909, 589, 589, 589, 589, 945, 589, 803, 784, 572, 764, 618, 589, 589, 568, 764, 618",
      /*  340 */ "589, 589, 568, 696, 618, 809, 899, 964, 922, 589, 589, 589, 589, 974, 589, 589, 589, 797, 596, 774",
      /*  360 */ "589, 589, 589, 589, 984, 589, 589, 589, 996, 589, 589, 589, 1018, 589, 589, 589, 589, 928, 589, 589",
      /*  380 */ "589, 589, 589, 838, 589, 589, 589, 756, 589, 589, 589, 589, 938, 589, 589, 589, 589, 589, 776, 589",
      /*  400 */ "589, 589, 589, 1024, 951, 589, 589, 992, 589, 589, 589, 589, 589, 872, 879, 954, 589, 589, 882, 957",
      /*  420 */ "589, 589, 848, 589, 589, 589, 589, 589, 1006, 589, 589, 589, 589, 589, 1028, 1041, 589, 589, 1054",
      /*  439 */ "589, 977, 589, 589, 999, 589, 589, 589, 646, 589, 589, 589, 589, 822, 589, 589, 589, 589, 589, 1058",
      /*  459 */ "589, 589, 589, 589, 1009, 589, 948, 589, 1062, 941, 1066, 589, 589, 589, 1012, 589, 589, 589, 889",
      /*  478 */ "589, 589, 1041, 589, 589, 1070, 589, 977, 589, 589, 1015, 589, 589, 589, 1074, 832, 1078, 589, 589",
      /*  497 */ "1082, 1086, 1090, 1094, 589, 589, 1098, 589, 1031, 589, 589, 1034, 589, 925, 589, 1102, 1106, 1110",
      /*  515 */ "925, 589, 1102, 1037, 1110, 589, 589, 1114, 722, 1044, 589, 589, 589, 865, 589, 589, 589, 589, 1118",
      /*  534 */ "589, 589, 589, 589, 1047, 589, 589, 589, 589, 1122, 589, 589, 589, 589, 1050, 589, 589, 589, 589",
      /*  553 */ "742, 589, 1138, 1178, 1459, 1126, 1126, 1126, 1126, 1460, 1178, 1178, 1139, 1138, 1461, 1178, 1178",
      /*  570 */ "1178, 1136, 1265, 1178, 1178, 1136, 1132, 1178, 1178, 1178, 1177, 1286, 1178, 1143, 1148, 1148, 1148",
      /*  587 */ "1148, 1144, 1178, 1178, 1178, 1178, 1179, 1181, 1162, 1178, 1178, 1178, 1182, 1180, 1264, 1130, 1130",
      /*  604 */ "1178, 1178, 1136, 1132, 1266, 1178, 1178, 1152, 1189, 1171, 1178, 1178, 1178, 1193, 1183, 1178, 1178",
      /*  621 */ "1178, 1218, 1187, 1178, 1178, 1178, 1238, 1448, 1178, 1199, 1204, 1204, 1204, 1204, 1200, 1178, 1178",
      /*  638 */ "1178, 1304, 1449, 1178, 1178, 1178, 1321, 1158, 1178, 1178, 1178, 1332, 1164, 1178, 1208, 1214, 1214",
      /*  655 */ "1214, 1214, 1209, 1178, 1178, 1193, 1210, 1178, 1178, 1178, 1360, 1253, 1178, 1178, 1178, 1367, 1509",
      /*  672 */ "1178, 1224, 1229, 1229, 1229, 1229, 1225, 1178, 1178, 1178, 1415, 1510, 1178, 1178, 1178, 1429, 1414",
      /*  689 */ "1417, 1178, 1178, 1167, 1257, 1522, 1252, 1178, 1178, 1180, 1178, 1264, 1130, 1154, 1156, 1155, 1263",
      /*  706 */ "1164, 1166, 1178, 1178, 1181, 1178, 1165, 1164, 1298, 1178, 1314, 1270, 1270, 1270, 1270, 1315, 1178",
      /*  723 */ "1178, 1178, 1589, 1310, 1312, 1311, 1313, 1316, 1316, 1178, 1178, 1181, 1180, 1538, 1274, 1278, 1280",
      /*  740 */ "1284, 1290, 1178, 1178, 1220, 1178, 1154, 1130, 1155, 1263, 1570, 1563, 1296, 1442, 1303, 1308, 1178",
      /*  757 */ "1178, 1244, 1178, 1154, 1130, 1181, 1263, 1154, 1182, 1178, 1178, 1253, 1218, 1310, 1270, 1311, 1313",
      /*  774 */ "1183, 1182, 1178, 1178, 1259, 1178, 1154, 1156, 1181, 1154, 1130, 1130, 1130, 1130, 1132, 1181, 1178",
      /*  791 */ "1178, 1178, 1257, 1154, 1182, 1178, 1179, 1178, 1178, 1136, 1183, 1181, 1178, 1178, 1264, 1130, 1374",
      /*  808 */ "1322, 1178, 1178, 1380, 1385, 1354, 1178, 1406, 1338, 1338, 1338, 1338, 1407, 1178, 1178, 1178, 1507",
      /*  825 */ "1178, 1405, 1338, 1355, 1336, 1408, 1408, 1178, 1178, 1542, 1548, 1570, 1563, 1178, 1178, 1564, 1422",
      /*  842 */ "1538, 1342, 1346, 1195, 1284, 1352, 1178, 1178, 1576, 1178, 1570, 1563, 1178, 1442, 1310, 1270, 1299",
      /*  859 */ "1313, 1501, 1361, 1178, 1178, 1308, 1178, 1178, 1601, 1178, 1494, 1368, 1178, 1178, 1329, 1453, 1331",
      /*  876 */ "1391, 1178, 1178, 1178, 1331, 1178, 1178, 1348, 1474, 1476, 1392, 1178, 1178, 1178, 1362, 1178, 1178",
      /*  893 */ "1373, 1178, 1178, 1375, 1320, 1322, 1385, 1385, 1385, 1385, 1386, 1178, 1178, 1178, 1376, 1322, 1178",
      /*  910 */ "1243, 1178, 1178, 1264, 1182, 1178, 1234, 1417, 1178, 1154, 1157, 1182, 1173, 1390, 1178, 1178, 1399",
      /*  927 */ "1585, 1178, 1412, 1178, 1178, 1414, 1233, 1416, 1241, 1178, 1178, 1178, 1428, 1178, 1178, 1435, 1437",
      /*  944 */ "1520, 1561, 1563, 1178, 1178, 1436, 1520, 1178, 1441, 1178, 1178, 1465, 1469, 1178, 1476, 1178, 1178",
      /*  961 */ "1488, 1248, 1251, 1381, 1178, 1178, 1178, 1489, 1252, 1178, 1180, 1157, 1182, 1603, 1396, 1178, 1178",
      /*  978 */ "1493, 1178, 1178, 1495, 1366, 1368, 1544, 1178, 1178, 1178, 1496, 1368, 1178, 1327, 1178, 1178, 1446",
      /*  995 */ "1178, 1403, 1178, 1178, 1178, 1500, 1178, 1178, 1502, 1359, 1361, 1591, 1481, 1178, 1178, 1514, 1178",
      /* 1012 */ "1178, 1516, 1178, 1178, 1530, 1178, 1178, 1550, 1178, 1178, 1503, 1361, 1178, 1292, 1292, 1433, 1486",
      /* 1029 */ "1178, 1178, 1178, 1568, 1178, 1178, 1574, 1178, 1178, 1580, 1583, 1585, 1323, 1178, 1178, 1178, 1595",
      /* 1046 */ "1178, 1178, 1607, 1178, 1178, 1613, 1178, 1178, 1597, 1178, 1178, 1178, 1424, 1178, 1178, 1178, 1434",
      /* 1063 */ "1178, 1178, 1526, 1434, 1520, 1178, 1178, 1609, 1178, 1178, 1178, 1477, 1178, 1178, 1536, 1418, 1548",
      /* 1080 */ "1178, 1178, 1482, 1178, 1178, 1554, 1559, 1178, 1178, 1555, 1482, 1555, 1178, 1178, 1369, 1178, 1178",
      /* 1097 */ "1178, 1615, 1178, 1178, 1178, 1397, 1178, 1178, 1584, 1424, 1398, 1583, 1585, 1397, 1585, 1178, 1178",
      /* 1114 */ "1470, 1178, 1178, 1178, 1532, 1178, 1178, 1178, 1456, 1178, 1178, 1178, 4, 4, 4, 4, 33046, 33046",
      /* 1132 */ "33046, 33046, 0, 33442, 0, 24882, 0, 0, 4, 0, 0, 0, 196612, 196612, 196612, 0, 196612, 196612",
      /* 1150 */ "196612, 196612, 0, 90130, 0, 0, 33046, 33046, 0, 33046, 0, 33442, 81938, 14, 0, 0, 49670, 0, 0, 0",
      /* 1170 */ "49670, 0, 33506, 0, 0, 65550, 346162, 66394, 0, 0, 0, 0, 33046, 0, 0, 0, 33442, 81938, 196612, 0, 0",
      /* 1191 */ "73746, 0, 0, 106818, 0, 0, 73746, 540676, 0, 458756, 458756, 458756, 0, 458756, 458756, 458756",
      /* 1207 */ "458756, 0, 49670, 49670, 49670, 0, 33442, 49670, 49670, 49670, 49670, 0, 114692, 0, 0, 98552, 0, 0",
      /* 1225 */ "491528, 491528, 491528, 0, 491528, 491528, 491528, 491528, 442372, 0, 442372, 442372, 442372, 0",
      /* 1239 */ "476470, 507912, 0, 0, 933892, 0, 0, 0, 638994, 476470, 0, 16398, 476470, 476470, 0, 0, 0, 114692, 0",
      /* 1258 */ "180770, 0, 0, 131192, 0, 33046, 0, 33046, 33046, 33046, 0, 0, 229380, 229380, 229380, 229380, 540676",
      /* 1275 */ "229394, 245764, 237586, 0, 32782, 245764, 0, 73746, 540676, 49166, 196612, 0, 0, 196612, 0, 245764",
      /* 1291 */ "540676, 0, 0, 213942, 0, 0, 573448, 0, 0, 229380, 0, 0, 589832, 0, 0, 0, 411794, 0, 720900, 0, 0",
      /* 1312 */ "229380, 229380, 0, 229380, 229380, 229380, 0, 0, 524292, 0, 524292, 0, 0, 0, 444082, 0, 401426, 0, 0",
      /* 1331 */ "246918, 0, 0, 0, 764002, 278532, 0, 278532, 278532, 278532, 278532, 540676, 229394, 0, 237586, 0",
      /* 1347 */ "32782, 0, 0, 262164, 286738, 0, 540676, 0, 0, 278532, 0, 0, 1015812, 0, 1015812, 0, 0, 0, 507922",
      /* 1366 */ "999428, 0, 999428, 0, 0, 0, 518642, 638980, 0, 0, 0, 524292, 524292, 524292, 0, 1048584, 1048584",
      /* 1383 */ "1048584, 0, 1048584, 1048584, 1048584, 1048584, 805290, 81934, 0, 0, 0, 638980, 0, 830050, 0, 0, 0",
      /* 1400 */ "673622, 673622, 673622, 0, 16634, 0, 0, 278532, 278532, 278532, 0, 0, 0, 155666, 0, 0, 442372",
      /* 1417 */ "442372, 0, 0, 0, 98318, 0, 163858, 0, 0, 444130, 0, 663570, 0, 0, 0, 688132, 205762, 0, 0, 0, 706694",
      /* 1438 */ "706694, 706694, 0, 205778, 0, 0, 0, 720900, 0, 688146, 0, 0, 458756, 0, 0, 263314, 0, 246918, 0, 0",
      /* 1458 */ "952890, 0, 4, 4, 4, 0, 4, 0, 270354, 278546, 294930, 303122, 0, 0, 0, 805290, 262164, 0, 262164, 0",
      /* 1478 */ "0, 0, 851988, 722802, 0, 0, 0, 983060, 0, 737298, 0, 0, 476470, 476470, 476470, 452498, 0, 0, 0",
      /* 1497 */ "999428, 999428, 999428, 755618, 0, 0, 0, 1015812, 1015812, 1015812, 778258, 0, 0, 0, 491528, 0, 0",
      /* 1514 */ "452546, 0, 0, 0, 499730, 0, 706694, 0, 0, 0, 507912, 476470, 0, 706694, 0, 764002, 845746, 0, 0, 0",
      /* 1534 */ "518674, 0, 0, 98318, 0, 0, 540676, 540676, 0, 950292, 0, 0, 606226, 0, 851988, 0, 0, 0, 614418, 0, 0",
      /* 1555 */ "983060, 0, 0, 0, 966680, 0, 0, 0, 655364, 0, 0, 0, 163858, 526978, 0, 0, 0, 655364, 346162, 879250",
      /* 1575 */ "0, 0, 0, 704530, 0, 452546, 0, 673622, 673622, 0, 673622, 0, 0, 0, 543010, 0, 0, 0, 714386, 0",
      /* 1595 */ "543026, 0, 0, 0, 747202, 0, 0, 548882, 0, 0, 821714, 346162, 527010, 0, 0, 0, 837330, 0, 953034, 0",
      /* 1615 */ "0, 0, 870914, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1619; ++i) {CASEID[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TOKENSET = new int[111];
  static
  {
    final String s1[] =
    {
      /*   0 */ "56, 56, 54, 66, 28, 54, 56, 24, 49, 51, 51, 51, 54, 57, 31, 49, 31, 48, 48, 18, 64, 48, 34, 61, 62",
      /*  25 */ "65, 49, 42, 63, 33, 62, 44, 59, 41, 59, 41, 59, 41, 40, 44, 44, 23, 0, 60, 29, 58, 58, 58, 36, 27, 55",
      /*  51 */ "60, 44, 44, 19, 20, 7, 52, 44, 35, 25, 44, 18, 2, 3, 21, 53, 33, 26, 50, 29, 35, 17, 1, 11, 10, 22",
      /*  77 */ "16, 8, 6, 32, 14, 37, 43, 15, 21, 9, 19, 20, 0, 13, 2, 3, 47, 4, 12, 19, 20, 39, 38, 19, 20, 45, 46",
      /* 104 */ "30, 5, 2, 3, 2, 3, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 111; ++i) {TOKENSET[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] APPENDIX = new int[14];
  static
  {
    final String s1[] =
    {
      /*  0 */ "98306, 294921, 238235, 253956, 45065, 270338, 40969, 270338, 327682, 410857, 327682, 415025, 425994",
      /* 13 */ "475146"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 14; ++i) {APPENDIX[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] LOOKBACK = new int[552];
  static
  {
    final String s1[] =
    {
      /*   0 */ "175, 175, 175, 173, 173, 173, 176, 179, 179, 179, 184, 194, 194, 194, 189, 197, 200, 203, 212, 212",
      /*  20 */ "221, 212, 221, 212, 221, 234, 234, 243, 234, 243, 234, 243, 256, 265, 265, 265, 265, 274, 274, 274",
      /*  40 */ "274, 175, 175, 175, 175, 283, 283, 283, 283, 290, 290, 290, 290, 297, 304, 175, 175, 175, 175, 307",
      /*  60 */ "312, 317, 322, 327, 327, 327, 327, 175, 175, 330, 330, 330, 333, 338, 343, 346, 346, 346, 175, 349",
      /*  80 */ "352, 352, 352, 355, 360, 365, 365, 365, 175, 175, 175, 175, 368, 381, 381, 381, 371, 376, 389, 384",
      /* 100 */ "392, 175, 175, 175, 395, 395, 395, 401, 406, 411, 421, 416, 416, 416, 416, 416, 426, 429, 398, 398",
      /* 120 */ "398, 434, 439, 444, 454, 449, 449, 449, 449, 449, 459, 175, 462, 465, 465, 465, 470, 175, 473, 473",
      /* 140 */ "473, 476, 486, 486, 486, 486, 489, 494, 499, 504, 507, 507, 507, 507, 479, 510, 175, 175, 175, 513",
      /* 160 */ "516, 524, 519, 527, 530, 175, 175, 175, 535, 538, 546, 541, 549, 2, 2, 0, 4, 3, 0, 6, 6, 4, 5, 0, 12",
      /* 185 */ "11, 8, 7, 0, 12, 13, 8, 9, 0, 10, 10, 0, 14, 14, 0, 15, 15, 0, 27, 25, 26, 25, 20, 18, 19, 18, 0, 27",
      /* 213 */ "28, 26, 28, 20, 21, 19, 21, 0, 38, 39, 34, 35, 27, 29, 26, 28, 20, 22, 19, 21, 0, 27, 30, 26, 30, 20",
      /* 239 */ "23, 19, 23, 0, 38, 40, 34, 36, 27, 31, 26, 30, 20, 24, 19, 23, 0, 38, 37, 34, 33, 27, 37, 20, 33, 0",
      /* 265 */ "38, 39, 34, 35, 27, 39, 20, 35, 0, 38, 40, 34, 36, 27, 40, 20, 36, 0, 50, 49, 46, 45, 42, 41, 0, 50",
      /* 291 */ "51, 46, 47, 42, 43, 0, 50, 52, 46, 48, 42, 44, 0, 53, 53, 0, 64, 63, 56, 55, 0, 64, 65, 56, 57, 0, 64",
      /* 318 */ "66, 56, 58, 0, 61, 61, 60, 60, 0, 62, 62, 0, 67, 68, 0, 76, 75, 70, 69, 0, 76, 77, 70, 71, 0, 73, 73",
      /* 345 */ "0, 74, 74, 0, 78, 78, 0, 79, 79, 0, 86, 85, 81, 80, 0, 86, 87, 81, 82, 0, 84, 84, 0, 89, 88, 0, 94",
      /* 372 */ "93, 89, 90, 0, 94, 95, 89, 91, 0, 92, 92, 0, 98, 98, 96, 96, 0, 97, 97, 0, 99, 99, 0, 102, 101, 0",
      /* 398 */ "102, 103, 0, 112, 111, 105, 104, 0, 112, 113, 105, 106, 0, 112, 114, 105, 106, 0, 110, 110, 107, 107",
      /* 420 */ "0, 109, 108, 108, 108, 0, 112, 115, 0, 123, 130, 109, 116, 0, 126, 125, 119, 118, 0, 126, 127, 119",
      /* 442 */ "120, 0, 126, 128, 119, 120, 0, 124, 124, 121, 121, 0, 123, 122, 122, 122, 0, 126, 129, 0, 131, 131, 0",
      /* 465 */ "134, 133, 132, 132, 0, 134, 135, 0, 137, 137, 0, 139, 138, 0, 151, 153, 143, 145, 139, 140, 0, 141",
      /* 487 */ "141, 0, 151, 150, 143, 142, 0, 151, 152, 143, 144, 0, 147, 147, 146, 146, 0, 148, 148, 0, 149, 149, 0",
      /* 510 */ "154, 154, 0, 157, 156, 0, 157, 158, 0, 161, 161, 159, 159, 0, 160, 160, 0, 162, 162, 0, 172, 172, 163",
      /* 533 */ "163, 0, 166, 165, 0, 166, 167, 0, 170, 170, 168, 168, 0, 169, 169, 0, 171, 171, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 552; ++i) {LOOKBACK[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] GOTO = new int[804];
  static
  {
    final String s1[] =
    {
      /*   0 */ "252, 253, 473, 463, 253, 253, 253, 461, 253, 253, 475, 253, 253, 253, 258, 253, 253, 291, 253, 253",
      /*  20 */ "253, 263, 253, 253, 280, 253, 253, 253, 253, 268, 286, 394, 253, 253, 253, 273, 253, 253, 414, 253",
      /*  40 */ "253, 253, 278, 253, 253, 486, 253, 253, 253, 253, 253, 284, 253, 499, 253, 253, 290, 253, 253, 253",
      /*  60 */ "253, 253, 295, 253, 300, 253, 386, 253, 253, 253, 305, 253, 253, 253, 253, 253, 310, 315, 253, 253",
      /*  80 */ "253, 320, 301, 253, 326, 392, 410, 332, 253, 253, 253, 253, 330, 286, 253, 264, 484, 336, 342, 253",
      /* 100 */ "253, 344, 253, 253, 253, 350, 253, 348, 356, 253, 253, 253, 354, 253, 253, 362, 253, 253, 253, 360",
      /* 120 */ "253, 253, 368, 253, 253, 253, 366, 253, 253, 374, 253, 253, 253, 372, 253, 253, 380, 253, 253, 253",
      /* 140 */ "253, 253, 296, 253, 253, 378, 253, 253, 253, 259, 253, 253, 253, 384, 253, 470, 274, 306, 390, 408",
      /* 160 */ "412, 398, 392, 410, 306, 402, 408, 412, 253, 482, 274, 338, 253, 253, 253, 406, 253, 253, 253, 418",
      /* 180 */ "301, 253, 253, 253, 253, 424, 253, 253, 322, 253, 253, 311, 253, 253, 253, 430, 253, 316, 253, 253",
      /* 200 */ "269, 436, 497, 440, 253, 253, 420, 253, 253, 253, 446, 253, 253, 426, 253, 253, 253, 452, 253, 253",
      /* 220 */ "253, 253, 253, 432, 253, 253, 253, 458, 254, 253, 497, 467, 253, 253, 442, 253, 253, 253, 479, 253",
      /* 240 */ "253, 253, 269, 490, 497, 253, 448, 494, 454, 253, 253, 253, 503, 504, 504, 504, 504, 566, 578, 504",
      /* 260 */ "504, 504, 588, 523, 504, 504, 504, 592, 531, 504, 504, 504, 598, 548, 504, 504, 504, 602, 504, 623",
      /* 280 */ "504, 504, 527, 504, 504, 701, 504, 504, 537, 504, 556, 504, 504, 504, 604, 769, 504, 504, 504, 663",
      /* 300 */ "664, 504, 504, 504, 678, 560, 504, 504, 504, 683, 796, 504, 504, 504, 705, 673, 504, 504, 504, 719",
      /* 320 */ "564, 504, 504, 504, 539, 504, 504, 683, 736, 570, 586, 504, 504, 504, 582, 504, 504, 602, 608, 504",
      /* 340 */ "691, 504, 613, 504, 504, 504, 617, 504, 504, 678, 504, 504, 621, 504, 631, 504, 504, 504, 627, 504",
      /* 360 */ "639, 504, 504, 504, 635, 504, 647, 504, 504, 504, 643, 504, 655, 504, 504, 504, 651, 504, 668, 672",
      /* 380 */ "504, 504, 659, 504, 677, 504, 504, 504, 715, 504, 687, 570, 577, 577, 504, 504, 544, 504, 504, 683",
      /* 400 */ "511, 741, 511, 790, 577, 577, 695, 504, 504, 504, 725, 504, 720, 504, 504, 504, 552, 504, 699, 504",
      /* 420 */ "504, 504, 749, 504, 540, 504, 504, 504, 757, 504, 504, 712, 504, 504, 761, 504, 724, 729, 733, 740",
      /* 440 */ "745, 504, 504, 504, 777, 504, 753, 504, 504, 504, 789, 504, 708, 504, 504, 504, 800, 504, 767, 504",
      /* 460 */ "504, 504, 519, 504, 504, 515, 504, 773, 504, 504, 504, 533, 682, 504, 509, 504, 504, 505, 504, 781",
      /* 480 */ "504, 504, 504, 573, 596, 504, 504, 504, 609, 504, 724, 785, 733, 740, 794, 504, 504, 504, 602, 504",
      /* 500 */ "504, 763, 504, 6, 0, 0, 0, 0, 12497, 0, 4113, 0, 0, 0, 204804, 0, 8225, 0, 12497, 20529, 24585, 28737",
      /* 522 */ "32777, 0, 40972, 0, 12497, 0, 299121, 49233, 53465, 0, 57353, 0, 0, 0, 213001, 0, 62065, 0, 0, 0",
      /* 542 */ "237577, 0, 0, 90124, 0, 12497, 0, 81932, 0, 12497, 0, 73740, 0, 12497, 0, 311305, 0, 12497, 0, 94217",
      /* 562 */ "0, 12497, 323593, 328129, 0, 0, 0, 278537, 131076, 131076, 131076, 0, 0, 160513, 311300, 155652, 0, 0",
      /* 580 */ "0, 16785, 0, 123377, 0, 12497, 0, 126985, 0, 0, 0, 365601, 0, 156385, 160513, 311300, 303108, 164617",
      /* 598 */ "0, 0, 0, 434180, 0, 348977, 0, 0, 28737, 32777, 167945, 0, 0, 0, 53465, 0, 176137, 0, 12497, 0",
      /* 618 */ "180233, 0, 12497, 0, 328193, 0, 0, 28769, 32777, 0, 213012, 0, 12497, 0, 184913, 0, 12497, 0, 172052",
      /* 637 */ "0, 12497, 0, 188425, 0, 12497, 0, 188436, 0, 12497, 0, 192521, 0, 12497, 0, 196628, 0, 12497, 0",
      /* 656 */ "197361, 0, 12497, 0, 279325, 0, 12497, 160529, 0, 0, 0, 86689, 0, 0, 376844, 368652, 385028, 0, 0, 0",
      /* 676 */ "90417, 208905, 0, 0, 0, 98841, 217097, 0, 0, 0, 106505, 0, 0, 336481, 339977, 0, 229385, 0, 12497, 0",
      /* 696 */ "234585, 0, 12497, 385033, 328129, 0, 0, 65545, 0, 0, 0, 394113, 0, 0, 266249, 90417, 0, 0, 398321, 0",
      /* 716 */ "0, 315401, 90417, 393236, 0, 0, 0, 111305, 401417, 0, 0, 0, 155652, 0, 242793, 417796, 245769, 0",
      /* 734 */ "450564, 409604, 0, 0, 336433, 339977, 405513, 0, 0, 0, 163868, 0, 516132, 0, 12497, 0, 427129, 0",
      /* 752 */ "12497, 0, 430089, 0, 12497, 0, 147500, 0, 12497, 0, 443665, 0, 0, 70265, 0, 0, 447833, 0, 0, 74377, 0",
      /* 773 */ "0, 283809, 0, 12497, 0, 286729, 0, 12497, 0, 450569, 0, 12497, 0, 292009, 417796, 245769, 475140, 0",
      /* 791 */ "0, 0, 180252, 442404, 458756, 0, 0, 74417, 0, 0, 427193, 0, 12497"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 804; ++i) {GOTO[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] REDUCTION = new int[130];
  static
  {
    final String s1[] =
    {
      /*   0 */ "37, 0, 0, -1, 1, -1, 2, -1, 3, -1, 4, -1, 5, -1, 38, 1, 6, 2, 7, 5, 7, 4, 7, 3, 7, 6, 8, -1, 39, 7, 9",
      /*  31 */ "-1, 10, -1, 40, 8, 11, 9, 11, -1, 12, 11, 12, 10, 13, 13, 13, 12, 14, 14, 15, -1, 16, 15, 42, 17, 41",
      /*  57 */ "16, 17, -1, 18, -1, 19, -1, 44, 19, 43, 18, 20, -1, 21, 20, 21, -1, 22, 22, 22, 21, 46, 24, 45, 23",
      /*  82 */ "23, -1, 47, 25, 24, 26, 48, 27, 25, 28, 25, -1, 26, -1, 27, -1, 28, -1, 29, 32, 29, 31, 29, 30, 29",
      /* 107 */ "29, 30, -1, 31, 33, 32, 34, 49, 35, 33, -1, 34, 36, 34, -1, 51, 38, 50, 37, 35, -1, 36, 39"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 130; ++i) {REDUCTION[i] = Integer.parseInt(s2[i]);}
  }

  private static final String[] TOKEN =
  {
    "(0)",
    "cchar",
    "namestart",
    "dchar",
    "schar",
    "'*'",
    "'**'",
    "'++'",
    "'?'",
    "'A'",
    "'B'",
    "'C'",
    "'D'",
    "'E'",
    "'F'",
    "'G'",
    "'H'",
    "'I'",
    "'J'",
    "'K'",
    "'L'",
    "'M'",
    "'N'",
    "'O'",
    "'P'",
    "'Q'",
    "'R'",
    "'S'",
    "'T'",
    "'U'",
    "'V'",
    "'W'",
    "'X'",
    "'Y'",
    "'Z'",
    "']'",
    "'e'",
    "'l'",
    "'m'",
    "'n'",
    "'o'",
    "'r'",
    "'s'",
    "'v'",
    "'}'",
    "whitespace",
    "namefollower",
    "hexdigit",
    "letter",
    "eof",
    "'\"'",
    "'#'",
    "''''",
    "'('",
    "')'",
    "'+'",
    "','",
    "'-'",
    "'.'",
    "':'",
    "';'",
    "'='",
    "'@'",
    "'['",
    "'^'",
    "'i'",
    "'x'",
    "'{'",
    "'|'",
    "'~'"
  };

  private static final String[] NONTERMINAL =
  {
    "ixml",
    "s",
    "RS",
    "comment",
    "prolog",
    "version",
    "rule",
    "mark",
    "alts",
    "alt",
    "term",
    "factor",
    "repeat0",
    "repeat1",
    "option",
    "sep",
    "nonterminal",
    "name",
    "namestart",
    "namefollower",
    "terminal",
    "tmark",
    "literal",
    "string",
    "hex",
    "charset",
    "inclusion",
    "exclusion",
    "set",
    "member",
    "range",
    "from",
    "to",
    "character",
    "class",
    "insertion",
    "capital",
    "IMPLICIT-37",
    "IMPLICIT-38",
    "IMPLICIT-39",
    "IMPLICIT-40",
    "IMPLICIT-41",
    "IMPLICIT-42",
    "IMPLICIT-43",
    "IMPLICIT-44",
    "IMPLICIT-45",
    "IMPLICIT-46",
    "IMPLICIT-47",
    "IMPLICIT-48",
    "IMPLICIT-49",
    "IMPLICIT-50",
    "IMPLICIT-51"
  };

                                                            // line 918 "ixml.ebnf"
                                                            private int hexBegin;
                                                              private boolean deleted;
                                                              private String codePoint;
                                                              private String firstCodePoint;
                                                              private String lastCodePoint;
                                                              private String clazz;
                                                              private CharSet charSet;
                                                              private Alt alt;
                                                              private Grammar grammar;
                                                              private Mark mark;
                                                              private java.util.Stack<Alts> alts = new java.util.Stack<>();
                                                              private StringBuilder stringBuilder = new StringBuilder();
                                                              private StringBuilder nameBuilder = new StringBuilder();

                                                              public Grammar grammar() {
                                                                return grammar;
                                                              }
                                                            }
                                                            // line 1728 "Ixml.java"
// End
