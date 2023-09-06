// This file was generated on Wed Sep 6, 2023 07:05 (UTC+02) by REx v5.57 which is Copyright (c) 1979-2023 by Gunther Rademacher <grd@gmx.net>
// REx command line: -glalr 1 -main -java -a java -name de.bottlecaps.markup.blitz.grammar.Ixml ixml.ebnf

package de.bottlecaps.markup.blitz.grammar;

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
                                                            if (grammar == null) grammar = new Grammar(null);
                                                            // line 530 "Ixml.java"
        }
        break;
      case 1:
        {
                                                            // line 3 "ixml.ebnf"
                                                            de.bottlecaps.markup.blitz.Errors.S01.thro();
                                                            // line 537 "Ixml.java"
        }
        break;
      case 2:
        {
                                                            // line 12 "ixml.ebnf"
                                                            grammar = new Grammar(stringBuilder.toString());
                                                            // line 544 "Ixml.java"
        }
        break;
      case 3:
        {
                                                            // line 16 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            grammar.addRule(new Rule(mark, nameBuilder.toString(), alts.peek()));
                                                            // line 552 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 19 "ixml.ebnf"
                                                            alts.pop();
                                                            // line 559 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 20 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 566 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 21 "ixml.ebnf"
                                                            mark = Mark.NODE;
                                                            // line 573 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 22 "ixml.ebnf"
                                                            mark = Mark.DELETE;
                                                            // line 580 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 23 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 587 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 26 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 594 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 36 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 601 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 38 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 609 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 42 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 617 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 46 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 626 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 52 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 634 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 56 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 643 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 62 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 651 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 66 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, nameBuilder.toString());
                                                            // line 658 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 68 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 665 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 69 "ixml.ebnf"
                                                            nameBuilder.append(input.subSequence(b0, e0));
                                                            // line 672 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 81 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 679 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 84 "ixml.ebnf"
                                                            alts.peek().last().addCharset(new Charset(deleted, exclusion, members));
                                                            // line 686 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 88 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 693 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 89 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 700 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 90 "ixml.ebnf"
                                                            alts.peek().last().addCodepoint(deleted, codepoint);
                                                            // line 707 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 91 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 714 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 92 "ixml.ebnf"
                                                            stringBuilder.append(input.subSequence(b0, e0));
                                                            // line 721 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 97 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 728 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 98 "ixml.ebnf"
                                                            codepoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 735 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 103 "ixml.ebnf"
                                                            exclusion = false;
                                                            members = new java.util.ArrayList<>();
                                                            // line 743 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 108 "ixml.ebnf"
                                                            exclusion = true;
                                                            members = new java.util.ArrayList<>();
                                                            // line 751 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 113 "ixml.ebnf"
                                                            members.add(new StringMember(stringBuilder.toString(), false));
                                                            // line 758 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 114 "ixml.ebnf"
                                                            members.add(new StringMember(codepoint, true));
                                                            // line 765 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 115 "ixml.ebnf"
                                                            members.add(new RangeMember(firstCodepoint, lastCodepoint));
                                                            // line 772 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 116 "ixml.ebnf"
                                                            members.add(new ClassMember(clazz));
                                                            // line 779 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 118 "ixml.ebnf"
                                                            firstCodepoint = codepoint;
                                                            // line 786 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 119 "ixml.ebnf"
                                                            lastCodepoint = codepoint;
                                                            // line 793 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 120 "ixml.ebnf"
                                                            codepoint = input.subSequence(b0, e0).toString();
                                                            // line 800 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 125 "ixml.ebnf"
                                                            clazz += input.subSequence(b0, e0);
                                                            // line 807 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 127 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 814 "Ixml.java"
        }
        break;
      case 40:
        {
                                                            // line 128 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codepoint);
                                                            // line 821 "Ixml.java"
        }
        break;
      case 41:
        {
                                                            // line 134 "ixml.ebnf"
                                                            clazz = input.subSequence(b0, e0).toString();
                                                            // line 828 "Ixml.java"
        }
        break;
      case 42:
        {
                                                            // line 136 "ixml.ebnf"
                                                            if (0 <= "\r\n".indexOf(input.charAt(b0))) de.bottlecaps.markup.blitz.Errors.S11.thro();
                                                            // line 835 "Ixml.java"
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
    int i0 = (state << 6) + nonterminal;
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
      int i0 = (i >> 5) * 68 + s - 1;
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
      /*   0 */ "66, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2",
      /*  34 */ "3, 4, 2, 2, 2, 5, 6, 7, 8, 9, 10, 11, 12, 2, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 15, 2, 16, 2",
      /*  63 */ "17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41",
      /*  88 */ "42, 43, 44, 45, 2, 46, 47, 48, 2, 49, 49, 49, 49, 50, 49, 51, 51, 52, 51, 51, 53, 54, 55, 56, 51, 51",
      /* 114 */ "57, 58, 51, 51, 59, 51, 60, 51, 51, 61, 62, 63, 64, 2"
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
      /* 1569 */ "2321, 2185, 2118, 1836, 2118, 1804, 1854, 1854, 1854, 1856, 66, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0",
      /* 1592 */ "0, 1, 0, 0, 1, 2, 3, 4, 2, 2, 2, 5, 6, 7, 8, 9, 10, 11, 12, 2, 2, 2, 2, 2, 48, 2, 65, 65, 65, 65, 2",
      /* 1623 */ "2, 48, 48, 48, 48, 13, 13, 13, 13, 13, 13, 13, 13, 14, 15, 2, 16, 2, 17, 18, 19, 20, 21, 22, 23, 24",
      /* 1649 */ "25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 2, 46, 47, 48, 2",
      /* 1675 */ "48, 2, 2, 2, 48, 48, 2, 48, 2, 2, 48, 2, 2, 2, 49, 49, 49, 49, 50, 49, 51, 51, 57, 58, 51, 51, 59",
      /* 1702 */ "51, 52, 51, 51, 53, 54, 55, 56, 60, 51, 51, 61, 62, 63, 64, 2, 2, 2, 2, 2, 65, 2, 65, 2, 2, 65, 65",
      /* 1729 */ "65, 2, 2, 2, 2, 48, 48, 48, 48, 2, 65, 65, 65, 65, 65, 2, 65, 2, 65, 65, 2, 65, 65, 2, 65, 2, 2, 48",
      /* 1757 */ "48, 2, 48, 48, 48, 2, 48, 2, 48, 48, 48, 48, 48, 48, 2, 48, 65, 65, 65, 65, 65, 65, 65, 2, 65, 65",
      /* 1783 */ "65, 65, 65, 65, 65, 48, 48, 65, 2, 65, 48, 2, 2, 2, 48, 48, 2, 2, 2, 2, 2, 65, 65, 48, 48, 48, 48",
      /* 1810 */ "48, 48, 65, 65, 65, 65, 65, 65, 65, 48, 65, 65, 65, 65, 65, 65, 2, 48, 48, 48, 65, 65, 2, 2, 65, 65",
      /* 1836 */ "65, 65, 2, 2, 2, 48, 48, 48, 48, 65, 48, 48, 2, 2, 65, 48, 2, 2, 48, 48, 48, 48, 48, 48, 48, 48, 2",
      /* 1863 */ "2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 65, 48, 2, 48, 48, 48, 48, 2, 2, 2, 2, 48, 2, 65, 65, 48, 48, 48, 65",
      /* 1892 */ "2, 2, 65, 65, 2, 48, 48, 2, 2, 65, 48, 2, 65, 65, 2, 2, 65, 65, 2, 2, 65, 65, 48, 48, 48, 48, 2, 2",
      /* 1920 */ "2, 48, 48, 2, 48, 2, 48, 48, 2, 48, 48, 2, 48, 48, 48, 48, 2, 48, 2, 48, 65, 48, 48, 65, 65, 65, 65",
      /* 1947 */ "48, 48, 48, 2, 2, 48, 2, 2, 65, 65, 2, 65, 65, 48, 2, 2, 2, 2, 2, 2, 48, 48, 48, 48, 48, 48, 48, 48",
      /* 1975 */ "2, 2, 65, 65, 65, 2, 65, 2, 2, 2, 65, 65, 48, 48, 48, 48, 65, 65, 2, 65, 65, 65, 2, 2, 2, 65, 2, 2",
      /* 2003 */ "2, 48, 48, 48, 2, 2, 1, 48, 48, 48, 48, 48, 48, 48, 2, 48, 48, 48, 48, 48, 48, 2, 48, 2, 2, 48, 48",
      /* 2030 */ "65, 65, 65, 2, 2, 2, 2, 65, 48, 48, 65, 65, 2, 2, 2, 2, 2, 65, 65, 65, 65, 65, 2, 2, 2, 2, 48, 65, 2",
      /* 2059 */ "2, 2, 2, 2, 2, 65, 65, 2, 2, 48, 48, 48, 48, 48, 2, 65, 2, 2, 2, 65, 65, 65, 1, 2, 65, 65, 65, 65, 2",
      /* 2088 */ "48, 48, 48, 48, 48, 48, 48, 48, 48, 65, 2, 65, 65, 65, 65, 48, 48, 2, 2, 2, 2, 48, 48, 65, 65, 2, 2",
      /* 2115 */ "2, 65, 2, 65, 65, 65, 65, 65, 65, 65, 65, 2, 2, 2, 2, 2, 2, 2, 65, 2, 2, 2, 65, 65, 65, 2, 65, 65",
      /* 2143 */ "65, 65, 2, 2, 2, 48, 65, 48, 48, 48, 48, 65, 48, 48, 48, 48, 48, 48, 48, 65, 2, 48, 48, 2, 2, 65, 48",
      /* 2170 */ "48, 2, 2, 2, 48, 65, 65, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 48, 2, 2, 2, 2, 48",
      /* 2202 */ "2, 48, 2, 48, 2, 48, 2, 48, 48, 48, 48, 48, 2, 2, 48, 48, 48, 48, 2, 2, 48, 48, 48, 48, 2, 2, 1, 2",
      /* 2230 */ "2, 2, 2, 48, 48, 2, 48, 48, 48, 48, 2, 2, 65, 65, 2, 2, 48, 48, 48, 2, 65, 2, 2, 2, 2, 48, 2, 2, 2",
      /* 2259 */ "2, 2, 2, 2, 48, 2, 2, 65, 65, 2, 2, 65, 2, 2, 2, 2, 2, 2, 48, 48, 48, 48, 65, 2, 2, 2, 2, 65, 2, 2",
      /* 2289 */ "48, 48, 48, 2, 48, 48, 65, 48, 65, 65, 65, 48, 48, 65, 48, 48, 48, 65, 48, 2, 2, 2, 2, 2, 2, 65, 48",
      /* 2316 */ "48, 48, 48, 48, 65, 65, 65, 65, 65, 2, 2, 65, 65, 48, 48, 48, 2, 65, 65, 2, 2, 2, 2, 65, 48, 48, 48",
      /* 2343 */ "2, 2, 65, 2, 2, 2"
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
      /*  694 */ "178205, 194559, 195101, 917759, 917999, 1114111, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 65, 48, 2, 48, 2",
      /*  715 */ "48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 65, 2, 65, 2, 48, 2, 48, 2, 65, 2, 48",
      /*  744 */ "2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 65, 2",
      /*  773 */ "48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 65, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48",
      /*  802 */ "2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 65, 2, 65, 2, 65, 48, 2, 48, 2, 48, 2, 65, 2, 65, 2, 48, 2",
      /*  831 */ "48, 2, 48, 2, 48, 2, 48, 2, 65, 2, 48, 65, 2, 65, 2, 65, 2, 48, 2, 65, 2, 65, 2, 48, 2, 65, 2, 65",
      /*  859 */ "48, 65, 2, 65, 2, 65, 2, 65, 2, 48, 2, 65, 2, 48, 2, 65, 2, 48, 65, 2, 65, 2, 65, 2, 65, 2, 65, 2",
      /*  887 */ "48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 65, 48, 2, 48, 2, 65, 2, 65, 2, 65, 2, 65, 2, 65, 2, 48, 2, 48, 2",
      /*  916 */ "48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48",
      /*  945 */ "2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 65, 2",
      /*  974 */ "48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48",
      /* 1003 */ "2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2",
      /* 1032 */ "48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 48, 2, 65, 2"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1050; ++i) {MAP2[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] INITIAL = new int[65];
  static
  {
    final String s1[] =
    {
      /*  0 */ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28",
      /* 28 */ "29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54",
      /* 54 */ "55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 65; ++i) {INITIAL[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TRANSITION = new int[2269];
  static
  {
    final String s1[] =
    {
      /*    0 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*   17 */ "1156, 1072, 1094, 1106, 1108, 1098, 1078, 1084, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*   34 */ "1957, 2140, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1129, 1156, 1120",
      /*   51 */ "1116, 1140, 1128, 1137, 1148, 1155, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 1165",
      /*   68 */ "1170, 1190, 1185, 1178, 1190, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1212, 1156, 2038, 1199, 1209",
      /*   85 */ "1211, 1206, 1220, 2043, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1156, 1156",
      /*  102 */ "1156, 1228, 1235, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1886, 1156, 1957, 2179, 1887, 1245, 1156",
      /*  119 */ "2183, 1251, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1156, 1261, 1156, 1156",
      /*  136 */ "1267, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1156, 1278, 1156, 1296, 1284",
      /*  153 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 2075, 1304, 1156, 1156, 1310, 1156",
      /*  170 */ "1156, 1156, 1156, 1156, 1156, 1156, 1848, 1156, 1962, 2140, 1848, 1320, 1899, 1340, 1347, 1156, 1156",
      /*  187 */ "1156, 1156, 1156, 1156, 1156, 1653, 1156, 1957, 1357, 1360, 2093, 1156, 2088, 1368, 1156, 1156, 1156",
      /*  204 */ "1156, 1156, 1156, 1156, 1378, 1156, 1957, 2140, 1156, 1270, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  221 */ "1156, 1156, 1156, 1156, 1156, 1989, 2197, 1156, 1670, 1156, 1156, 2202, 1156, 1156, 1156, 1156, 1156",
      /*  238 */ "1156, 1156, 1156, 1156, 1957, 1327, 1332, 1389, 1156, 1388, 1397, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  255 */ "1156, 1156, 1156, 2002, 2224, 1156, 1687, 1156, 1156, 2229, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  272 */ "1156, 1156, 1957, 2140, 1156, 1407, 1156, 1156, 1413, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  289 */ "1156, 1957, 2140, 1156, 1156, 1917, 1423, 1430, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1378, 1156",
      /*  306 */ "1957, 2140, 1867, 1740, 1440, 1440, 1447, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1378, 1156, 1957",
      /*  323 */ "2140, 1867, 1758, 1457, 1457, 1464, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1378, 1156, 1957, 2140",
      /*  340 */ "1867, 1776, 1474, 1474, 1481, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1378, 1156, 1957, 2140, 1867",
      /*  357 */ "1794, 1491, 1491, 1498, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1378, 1156, 1957, 2140, 1867, 1812",
      /*  374 */ "1508, 1508, 1515, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1378, 1156, 1957, 2140, 1867, 1830, 1525",
      /*  391 */ "1525, 1532, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1312, 1542, 1542",
      /*  408 */ "1549, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1349, 1559, 1559, 1566",
      /*  425 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1370, 1576, 1576, 1583, 1156",
      /*  442 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1380, 1593, 1593, 1600, 1156, 1156",
      /*  459 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 2204, 1610, 1610, 1617, 1156, 1156, 1156",
      /*  476 */ "1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1399, 1627, 1627, 1634, 1156, 1156, 1156, 1156",
      /*  493 */ "1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 2231, 1644, 1644, 1651, 1156, 1156, 1156, 1156, 1156",
      /*  510 */ "1156, 1156, 1156, 1156, 1957, 2140, 1867, 1415, 1661, 1661, 1668, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  527 */ "1156, 1156, 1156, 1957, 2140, 1867, 1432, 1678, 1678, 1685, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  544 */ "1156, 1156, 1957, 2140, 1867, 1449, 1695, 1695, 1702, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  561 */ "1156, 1957, 2140, 1867, 1466, 1712, 1712, 1719, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  578 */ "1957, 2140, 1867, 1483, 1730, 1730, 1737, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957",
      /*  595 */ "2140, 1867, 1500, 1748, 1748, 1755, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140",
      /*  612 */ "1867, 1517, 1766, 1766, 1773, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867",
      /*  629 */ "1534, 1784, 1784, 1791, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1551",
      /*  646 */ "1802, 1802, 1809, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1568, 1820",
      /*  663 */ "1820, 1827, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1585, 1838, 1838",
      /*  680 */ "1845, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1602, 1856, 1856, 1863",
      /*  697 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1867, 1619, 1875, 1875, 1882, 1156",
      /*  714 */ "1156, 1156, 1156, 1156, 1156, 1156, 2044, 1156, 1957, 1895, 1907, 1913, 1156, 1925, 1913, 1156, 1156",
      /*  731 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2253, 1937, 2062, 1933, 1156, 2257, 1156, 1156, 1156",
      /*  748 */ "1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 1156, 1952, 2261, 1945, 1952, 1156, 1156, 1156, 1156",
      /*  765 */ "1156, 1156, 1156, 1156, 1156, 1957, 2140, 1156, 1253, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156",
      /*  782 */ "1156, 1156, 1378, 1156, 1957, 2140, 1867, 1722, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  799 */ "1156, 1378, 1997, 1957, 2140, 1867, 1722, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  816 */ "1156, 1156, 1957, 2140, 1867, 1253, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /*  833 */ "2010, 1957, 2140, 1867, 1636, 2019, 2025, 2011, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 2033",
      /*  850 */ "1957, 2140, 1867, 1253, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 2057, 1957",
      /*  867 */ "2140, 1867, 1253, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1288, 1957, 2140",
      /*  884 */ "1867, 1253, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 2143, 1957, 2140, 1867",
      /*  901 */ "1253, 1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1086, 1957, 2140, 1867, 1253",
      /*  918 */ "1970, 1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1157, 1957, 2140, 1867, 1253, 1970",
      /*  935 */ "1976, 1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 2052, 2140, 2070, 1253, 1970, 1976",
      /*  952 */ "1984, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 2083, 2140, 1867, 1704, 1970, 1976, 1984",
      /*  969 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 2101, 2112, 2124, 2126, 2116, 2107, 2134, 1156",
      /*  986 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2151, 2156, 2165, 1156, 2164, 2173, 1156, 1156",
      /* 1003 */ "1156, 1156, 1156, 1156, 1156, 1156, 1156, 1957, 2191, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /* 1020 */ "1156, 1156, 1156, 1156, 1156, 1156, 1957, 2140, 2212, 2218, 1156, 2239, 2218, 1156, 1156, 1156, 1156",
      /* 1037 */ "1156, 1156, 1156, 1156, 1156, 1957, 2140, 1156, 1237, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /* 1054 */ "1156, 1156, 2247, 1156, 1156, 1156, 1156, 1156, 1191, 1156, 1156, 1156, 1156, 1156, 1156, 1156, 1156",
      /* 1071 */ "1156, 0, 0, 5954, 512, 640, 0, 0, 5954, 0, 0, 0, 5954, 5954, 5954, 0, 0, 0, 0, 0, 0, 5376, 0, 5954",
      /* 1095 */ "5954, 256, 0, 0, 5954, 0, 5954, 0, 5954, 5954, 5954, 5954, 5954, 0, 0, 5954, 5954, 5954, 5954, 5954",
      /* 1115 */ "0, 0, 0, 256, 6528, 0, 0, 0, 6528, 640, 6528, 0, 0, 6528, 0, 0, 6528, 0, 0, 0, 0, 0, 6528, 0, 6528",
      /* 1140 */ "6528, 0, 6528, 0, 0, 0, 6528, 0, 6528, 6528, 6528, 6528, 6528, 6528, 6528, 6528, 0, 0, 0, 0, 0, 0, 0",
      /* 1163 */ "0, 5504, 0, 0, 256, 6656, 0, 0, 0, 6656, 0, 0, 0, 6656, 0, 6656, 6656, 6656, 6656, 6656, 6656, 6656",
      /* 1185 */ "6656, 0, 6656, 6656, 0, 6656, 0, 0, 0, 0, 0, 0, 0, 6400, 0, 0, 256, 6784, 0, 0, 0, 6784, 0, 6784",
      /* 1209 */ "6784, 0, 6784, 0, 0, 0, 6784, 0, 0, 0, 0, 6784, 6784, 6784, 6784, 6784, 6784, 6784, 6784, 6912, 6912",
      /* 1230 */ "6912, 6912, 6912, 6912, 6912, 6912, 0, 0, 0, 0, 0, 0, 0, 6016, 0, 0, 7040, 7040, 7040, 7040, 7040",
      /* 1251 */ "7040, 0, 0, 0, 0, 0, 0, 0, 6016, 384, 0, 835, 835, 835, 835, 835, 835, 0, 896, 0, 0, 0, 0, 0, 6144",
      /* 1276 */ "6016, 0, 0, 7236, 7236, 7236, 7236, 7236, 7236, 0, 0, 1024, 0, 0, 0, 0, 5120, 0, 0, 0, 7168, 7168",
      /* 1298 */ "7168, 7168, 7168, 7168, 7168, 7168, 0, 7296, 7296, 7296, 7296, 7296, 7296, 0, 0, 0, 0, 0, 0, 0, 6016",
      /* 1319 */ "2048, 7424, 0, 0, 0, 0, 7424, 6016, 0, 0, 256, 0, 7808, 0, 7808, 0, 7808, 7808, 7808, 0, 7808, 7424",
      /* 1341 */ "7424, 7424, 7424, 7424, 7424, 7424, 7424, 0, 0, 0, 0, 0, 0, 0, 6016, 2176, 7552, 0, 256, 0, 0, 0",
      /* 1363 */ "7552, 0, 0, 0, 7552, 7552, 0, 0, 0, 0, 0, 0, 0, 6016, 2304, 6144, 0, 0, 0, 0, 0, 0, 0, 6016, 2432, 0",
      /* 1389 */ "0, 7808, 7808, 7808, 7808, 7808, 7808, 0, 7808, 0, 0, 0, 0, 0, 0, 0, 6016, 2688, 0, 1152, 1152, 1152",
      /* 1411 */ "1152, 1152, 1152, 0, 0, 0, 0, 0, 0, 0, 6016, 2944, 8064, 8064, 8064, 8064, 8064, 8064, 8064, 8064, 0",
      /* 1432 */ "0, 0, 0, 0, 0, 0, 6016, 3072, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 0, 0, 0, 0, 0, 0, 0",
      /* 1455 */ "6016, 3200, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 0, 0, 0, 0, 0, 0, 0, 6016, 3328, 1536",
      /* 1475 */ "1536, 1536, 1536, 1536, 1536, 1536, 1536, 0, 0, 0, 0, 0, 0, 0, 6016, 3456, 1664, 1664, 1664, 1664",
      /* 1495 */ "1664, 1664, 1664, 1664, 0, 0, 0, 0, 0, 0, 0, 6016, 3584, 1792, 1792, 1792, 1792, 1792, 1792, 1792",
      /* 1515 */ "1792, 0, 0, 0, 0, 0, 0, 0, 6016, 3712, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 0, 0, 0, 0, 0",
      /* 1538 */ "0, 0, 6016, 3840, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 0, 0, 0, 0, 0, 0, 0, 6016, 3968",
      /* 1559 */ "2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 0, 0, 0, 0, 0, 0, 0, 6016, 4096, 2304, 2304, 2304",
      /* 1579 */ "2304, 2304, 2304, 2304, 2304, 0, 0, 0, 0, 0, 0, 0, 6016, 4224, 2432, 2432, 2432, 2432, 2432, 2432",
      /* 1599 */ "2432, 2432, 0, 0, 0, 0, 0, 0, 0, 6016, 4352, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 0, 0, 0",
      /* 1621 */ "0, 0, 0, 0, 6016, 4480, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 0, 0, 0, 0, 0, 0, 0, 6016",
      /* 1643 */ "8448, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 0, 0, 0, 0, 0, 0, 0, 7552, 0, 2944, 2944, 2944",
      /* 1664 */ "2944, 2944, 2944, 2944, 2944, 0, 0, 0, 0, 0, 0, 0, 7680, 0, 3072, 3072, 3072, 3072, 3072, 3072, 3072",
      /* 1685 */ "3072, 0, 0, 0, 0, 0, 0, 0, 7936, 0, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 0, 0, 0, 0, 0, 0",
      /* 1709 */ "0, 8576, 384, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 0, 0, 0, 0, 0, 0, 0, 6144, 6016, 384",
      /* 1730 */ "3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 0, 0, 0, 0, 0, 0, 0, 6144, 6016, 1280, 3584, 3584",
      /* 1750 */ "3584, 3584, 3584, 3584, 3584, 3584, 0, 0, 0, 0, 0, 0, 0, 6144, 6016, 1408, 3712, 3712, 3712, 3712",
      /* 1770 */ "3712, 3712, 3712, 3712, 0, 0, 0, 0, 0, 0, 0, 6144, 6016, 1536, 3840, 3840, 3840, 3840, 3840, 3840",
      /* 1790 */ "3840, 3840, 0, 0, 0, 0, 0, 0, 0, 6144, 6016, 1664, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 0",
      /* 1811 */ "0, 0, 0, 0, 0, 0, 6144, 6016, 1792, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 0, 0, 0, 0, 0, 0",
      /* 1834 */ "0, 6144, 6016, 1920, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 0, 0, 0, 0, 0, 0, 0, 7424, 0, 0",
      /* 1856 */ "4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 0, 0, 0, 0, 0, 0, 0, 6272, 0, 0, 0, 4480, 4480, 4480",
      /* 1878 */ "4480, 4480, 4480, 4480, 4480, 0, 0, 0, 0, 0, 0, 0, 7040, 0, 0, 0, 7040, 0, 8192, 256, 0, 0, 0, 0, 0",
      /* 1903 */ "7424, 0, 7424, 7424, 0, 0, 8192, 0, 0, 0, 8192, 0, 0, 0, 0, 0, 0, 0, 8064, 0, 8064, 8064, 8192, 8192",
      /* 1927 */ "8192, 8192, 8192, 8192, 8192, 8192, 0, 0, 4608, 0, 0, 4608, 0, 0, 4608, 4608, 0, 0, 8320, 8320, 8320",
      /* 1948 */ "8320, 8320, 8320, 8320, 8320, 0, 0, 0, 0, 0, 0, 0, 512, 640, 0, 0, 0, 512, 640, 0, 0, 7424, 0, 384",
      /* 1972 */ "0, 0, 384, 0, 384, 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 0, 0, 0, 0, 0, 512, 640, 0, 7680, 0",
      /* 1997 */ "4736, 0, 0, 0, 0, 0, 0, 0, 512, 640, 0, 7936, 0, 0, 8448, 0, 0, 0, 0, 0, 0, 0, 0, 8448, 0, 0, 8448",
      /* 2024 */ "0, 8448, 8448, 8448, 8448, 8448, 8448, 8448, 8448, 0, 0, 4864, 0, 0, 0, 0, 0, 512, 6784, 6784, 0, 0",
      /* 2046 */ "0, 0, 0, 0, 0, 8192, 5632, 0, 0, 512, 640, 0, 0, 0, 4992, 0, 0, 0, 0, 4608, 4608, 4608, 0, 0, 5632",
      /* 2071 */ "0, 0, 0, 6272, 0, 0, 0, 7296, 0, 0, 0, 7296, 0, 8576, 0, 512, 640, 0, 0, 0, 7552, 7552, 0, 7552",
      /* 2095 */ "7552, 7552, 7552, 7552, 7552, 0, 0, 0, 8704, 512, 640, 0, 0, 8704, 0, 0, 0, 8704, 8704, 8704, 0, 0",
      /* 2117 */ "8704, 0, 8704, 0, 8704, 8704, 8704, 8704, 8704, 0, 0, 8704, 8704, 8704, 8704, 8704, 0, 8704, 0, 0, 0",
      /* 2138 */ "0, 0, 0, 0, 256, 0, 0, 0, 0, 0, 5248, 0, 0, 0, 0, 256, 0, 8832, 0, 8832, 0, 8832, 8832, 8832, 0",
      /* 2163 */ "8832, 0, 0, 8832, 8832, 8832, 8832, 8832, 8832, 0, 8832, 0, 0, 0, 0, 0, 0, 0, 256, 0, 0, 0, 7040, 0",
      /* 2187 */ "7040, 7040, 0, 7040, 0, 0, 5760, 0, 0, 0, 0, 0, 256, 0, 0, 7680, 0, 0, 0, 0, 0, 0, 0, 6016, 2560, 0",
      /* 2213 */ "0, 8960, 0, 0, 0, 8960, 0, 0, 0, 0, 0, 0, 0, 256, 0, 0, 7936, 0, 0, 0, 0, 0, 0, 0, 6016, 2816, 8960",
      /* 2240 */ "8960, 8960, 8960, 8960, 8960, 8960, 8960, 0, 6400, 0, 0, 0, 0, 0, 0, 256, 0, 4608, 0, 0, 0, 0, 0, 0",
      /* 2264 */ "0, 8320, 0, 8320, 8320"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2269; ++i) {TRANSITION[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] EXPECTED = new int[183];
  static
  {
    final String s1[] =
    {
      /*   0 */ "141, 141, 141, 141, 142, 140, 129, 141, 141, 141, 178, 179, 51, 55, 57, 57, 61, 68, 75, 79, 64, 83",
      /*  22 */ "71, 87, 91, 95, 99, 103, 107, 111, 115, 119, 123, 127, 141, 141, 130, 141, 169, 142, 175, 134, 137",
      /*  43 */ "146, 150, 152, 172, 156, 160, 163, 166, -512, -508, -512, -512, -508, -512, -508, -508, -508, -508",
      /*  61 */ "-28, 0, 64, 128, 256, 512, 1024, 32768, 131072, 262144, 1048576, 1310720, 671088640, 33562624",
      /*  75 */ "4194304, 33554432, 67108864, -2147483648, 16, 0, 32, 64, 2048, 0, 8192, 262144, 67117056, -2147475456",
      /*  89 */ "4096, 1835008, 268435464, 671096832, 339738624, 1843200, 1320960, 268443656, -2145648640, 356515840",
      /*  99 */ "268509192, 301998088, -2145640448, 356524032, -2112094208, 364904448, 364912640, 365174792, 365961224",
      /* 108 */ "398499848, 1036017664, 7, 1835015, 8199, 1835023, 1843207, 1107296263, 1843215, 1107304455",
      /* 118 */ "1107435527, -1027866617, -1027858425, -755236857, -692322297, -688127993, -755228665, -692314105",
      /* 126 */ "-688119801, -253937, 8192, 0, 0, 2, 0, 0, 16, 8, 16, 8, 24, 32, 16, 0, 0, 0, 0, 8, 24, 24, 40, 24, 33",
      /* 151 */ "16, 24, 24, 28, 2, 3, 8, 11, 11, 35, 43, 51, 51, 59, 59, 59, 0, 0, 0, 4, 8, 0, 10, 0, 8, 8, 8, 0, 480",
      /* 180 */ "480, 480, -508"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 183; ++i) {EXPECTED[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] CASEID = new int[1655];
  static
  {
    final String s1[] =
    {
      /*    0 */ "721, 809, 565, 727, 569, 574, 577, 581, 601, 586, 592, 809, 595, 808, 599, 605, 611, 582, 611, 610",
      /*   20 */ "616, 619, 623, 761, 628, 574, 577, 581, 601, 633, 721, 809, 565, 727, 569, 638, 641, 645, 735, 649",
      /*   40 */ "654, 657, 661, 611, 666, 721, 809, 565, 611, 671, 721, 809, 565, 611, 671, 721, 809, 565, 611, 671",
      /*   60 */ "616, 619, 623, 761, 628, 683, 686, 676, 680, 690, 611, 611, 612, 893, 695, 700, 703, 707, 611, 712",
      /*   80 */ "611, 611, 570, 588, 610, 965, 611, 713, 1033, 717, 611, 611, 624, 611, 610, 1050, 611, 629, 1043",
      /*   99 */ "725, 611, 611, 771, 731, 610, 721, 809, 565, 739, 599, 965, 611, 713, 1033, 717, 743, 746, 750, 755",
      /*  119 */ "759, 616, 619, 623, 765, 769, 654, 657, 661, 611, 666, 611, 611, 611, 775, 779, 721, 809, 565, 785",
      /*  139 */ "599, 611, 611, 570, 789, 610, 721, 809, 565, 795, 599, 962, 611, 570, 1053, 799, 721, 809, 565, 805",
      /*  159 */ "813, 611, 611, 570, 968, 799, 721, 809, 565, 805, 813, 611, 611, 570, 968, 799, 721, 809, 565, 805",
      /*  179 */ "813, 611, 611, 570, 968, 799, 611, 611, 570, 819, 826, 962, 611, 570, 1053, 799, 956, 611, 662, 942",
      /*  199 */ "832, 611, 611, 611, 667, 611, 611, 611, 696, 611, 611, 611, 611, 570, 587, 610, 838, 841, 845, 851",
      /*  219 */ "855, 611, 611, 611, 861, 611, 743, 746, 750, 867, 759, 616, 619, 623, 871, 875, 616, 619, 623, 871",
      /*  239 */ "875, 616, 619, 623, 871, 875, 611, 611, 611, 881, 933, 885, 611, 611, 891, 611, 917, 611, 611, 930",
      /*  259 */ "611, 815, 809, 595, 789, 610, 611, 611, 611, 634, 611, 743, 746, 750, 897, 759, 1020, 611, 672, 1013",
      /*  279 */ "901, 914, 611, 691, 907, 911, 924, 611, 611, 939, 611, 953, 611, 611, 953, 611, 857, 946, 950, 972",
      /*  299 */ "611, 611, 611, 611, 667, 611, 962, 611, 570, 1053, 799, 611, 611, 624, 611, 610, 885, 611, 611, 985",
      /*  319 */ "611, 917, 611, 611, 978, 611, 611, 611, 822, 606, 799, 611, 611, 570, 733, 610, 885, 611, 611, 998",
      /*  339 */ "611, 917, 611, 611, 991, 611, 962, 611, 570, 1053, 799, 611, 611, 611, 1017, 611, 611, 611, 611",
      /*  358 */ "1027, 611, 611, 611, 611, 959, 611, 611, 611, 650, 801, 1037, 815, 809, 565, 789, 610, 611, 611, 570",
      /*  378 */ "789, 610, 611, 611, 570, 719, 610, 857, 946, 1047, 972, 611, 611, 611, 611, 1057, 611, 611, 611, 822",
      /*  398 */ "606, 799, 611, 611, 611, 611, 1067, 611, 611, 1073, 611, 611, 611, 611, 611, 1083, 611, 611, 611",
      /*  417 */ "1001, 611, 611, 611, 611, 611, 834, 611, 611, 611, 847, 611, 611, 611, 611, 1004, 611, 611, 611, 611",
      /*  437 */ "863, 611, 611, 611, 611, 611, 828, 611, 611, 611, 611, 920, 1007, 611, 611, 791, 611, 611, 611, 611",
      /*  457 */ "611, 1023, 904, 1010, 611, 611, 994, 1030, 611, 611, 611, 611, 1087, 611, 611, 611, 1091, 611, 611",
      /*  476 */ "611, 877, 611, 611, 611, 611, 708, 611, 611, 611, 611, 927, 611, 611, 611, 611, 611, 1095, 611, 611",
      /*  496 */ "611, 611, 1040, 611, 988, 611, 1099, 981, 1103, 611, 611, 611, 1060, 611, 611, 611, 1107, 935, 1111",
      /*  515 */ "611, 611, 1115, 1119, 1123, 975, 611, 1127, 1131, 1135, 975, 611, 1127, 1063, 1135, 611, 611, 1139",
      /*  533 */ "751, 1070, 611, 611, 611, 887, 611, 611, 611, 611, 1143, 611, 611, 611, 611, 1076, 611, 611, 611",
      /*  552 */ "611, 1147, 611, 611, 611, 611, 1079, 611, 611, 611, 611, 781, 611, 1314, 1187, 1187, 1159, 1205",
      /*  570 */ "1187, 1187, 1187, 1159, 1161, 1187, 1519, 1166, 1166, 1166, 1166, 1520, 1187, 1187, 1187, 1182, 1169",
      /*  587 */ "1187, 1187, 1187, 1183, 1185, 1184, 1313, 1151, 1151, 1187, 1187, 1159, 1205, 1315, 1187, 1187, 1162",
      /*  604 */ "1161, 1176, 1187, 1187, 1187, 1186, 1188, 1187, 1187, 1187, 1187, 1250, 1341, 1187, 1192, 1197, 1197",
      /*  621 */ "1197, 1197, 1193, 1187, 1187, 1187, 1273, 1209, 1187, 1187, 1187, 1279, 1521, 1187, 1187, 1187, 1297",
      /*  638 */ "1171, 1187, 1215, 1221, 1221, 1221, 1221, 1216, 1187, 1187, 1225, 1217, 1187, 1187, 1187, 1308, 1561",
      /*  655 */ "1187, 1231, 1236, 1236, 1236, 1236, 1232, 1187, 1187, 1187, 1364, 1562, 1187, 1187, 1187, 1366, 1155",
      /*  672 */ "1187, 1187, 1187, 1416, 1241, 1187, 1187, 1225, 1185, 1187, 1253, 1252, 1187, 1240, 1246, 1246, 1246",
      /*  689 */ "1246, 1242, 1187, 1187, 1187, 1424, 1645, 1187, 1187, 1187, 1448, 1575, 1187, 1259, 1264, 1264, 1264",
      /*  706 */ "1264, 1260, 1187, 1187, 1187, 1466, 1576, 1187, 1187, 1187, 1552, 1551, 1554, 1187, 1187, 1184, 1187",
      /*  723 */ "1313, 1151, 1592, 1305, 1187, 1187, 1185, 1184, 1252, 1254, 1187, 1187, 1185, 1187, 1172, 1171, 1203",
      /*  740 */ "1153, 1204, 1312, 1358, 1187, 1386, 1319, 1319, 1319, 1319, 1387, 1187, 1187, 1187, 1622, 1382, 1384",
      /*  757 */ "1383, 1385, 1388, 1388, 1187, 1187, 1201, 1211, 1624, 1323, 1327, 1329, 1333, 1339, 1187, 1187, 1255",
      /*  774 */ "1273, 1651, 1644, 1345, 1461, 1351, 1356, 1187, 1187, 1275, 1187, 1203, 1151, 1185, 1312, 1203, 1186",
      /*  791 */ "1187, 1187, 1282, 1187, 1203, 1151, 1204, 1312, 1188, 1186, 1187, 1187, 1307, 1187, 1203, 1153, 1185",
      /*  808 */ "1203, 1151, 1151, 1151, 1151, 1205, 1185, 1187, 1187, 1313, 1151, 1203, 1186, 1187, 1183, 1187, 1187",
      /*  825 */ "1159, 1188, 1185, 1187, 1187, 1335, 1187, 1409, 1365, 1187, 1187, 1352, 1483, 1478, 1187, 1486, 1372",
      /*  842 */ "1372, 1372, 1372, 1487, 1187, 1187, 1187, 1426, 1187, 1485, 1372, 1479, 1370, 1488, 1488, 1187, 1187",
      /*  859 */ "1437, 1442, 1651, 1644, 1187, 1187, 1497, 1187, 1382, 1319, 1383, 1385, 1624, 1376, 1380, 1227, 1333",
      /*  876 */ "1392, 1187, 1187, 1559, 1187, 1651, 1644, 1187, 1461, 1539, 1187, 1187, 1187, 1634, 1187, 1285, 1187",
      /*  893 */ "1187, 1187, 1645, 1250, 1382, 1319, 1359, 1385, 1530, 1417, 1187, 1187, 1396, 1187, 1187, 1398, 1423",
      /*  910 */ "1425, 1397, 1425, 1187, 1187, 1399, 1425, 1187, 1403, 1187, 1187, 1347, 1347, 1503, 1418, 1187, 1187",
      /*  927 */ "1187, 1404, 1187, 1187, 1408, 1187, 1187, 1356, 1187, 1187, 1590, 1596, 1419, 1187, 1187, 1187, 1410",
      /*  944 */ "1363, 1365, 1442, 1442, 1442, 1442, 1443, 1187, 1187, 1187, 1430, 1187, 1187, 1411, 1365, 1187, 1296",
      /*  961 */ "1187, 1187, 1313, 1186, 1187, 1269, 1554, 1187, 1184, 1154, 1186, 1178, 1447, 1187, 1187, 1433, 1618",
      /*  978 */ "1187, 1452, 1187, 1187, 1454, 1456, 1573, 1288, 1187, 1187, 1187, 1455, 1573, 1187, 1460, 1187, 1187",
      /*  995 */ "1472, 1536, 1538, 1291, 1187, 1187, 1187, 1476, 1187, 1187, 1492, 1187, 1187, 1508, 1187, 1187, 1525",
      /* 1012 */ "1529, 1187, 1531, 1415, 1417, 1642, 1644, 1187, 1187, 1532, 1417, 1187, 1394, 1513, 1396, 1294, 1187",
      /* 1029 */ "1187, 1187, 1538, 1187, 1187, 1551, 1268, 1553, 1306, 1187, 1187, 1187, 1566, 1187, 1187, 1568, 1301",
      /* 1046 */ "1304, 1438, 1187, 1187, 1187, 1569, 1305, 1187, 1203, 1154, 1186, 1586, 1465, 1187, 1187, 1598, 1187",
      /* 1063 */ "1187, 1613, 1616, 1618, 1630, 1187, 1187, 1187, 1628, 1187, 1187, 1636, 1187, 1187, 1640, 1187, 1187",
      /* 1080 */ "1649, 1187, 1187, 1470, 1187, 1187, 1187, 1543, 1187, 1187, 1187, 1499, 1549, 1187, 1187, 1545, 1187",
      /* 1097 */ "1187, 1187, 1453, 1187, 1187, 1580, 1453, 1573, 1187, 1187, 1504, 1187, 1187, 1584, 1555, 1596, 1187",
      /* 1114 */ "1187, 1509, 1187, 1187, 1602, 1607, 1187, 1187, 1603, 1509, 1603, 1187, 1187, 1431, 1187, 1187, 1617",
      /* 1131 */ "1545, 1432, 1616, 1618, 1431, 1618, 1187, 1187, 1493, 1187, 1187, 1187, 1609, 1187, 1187, 1187, 1516",
      /* 1148 */ "1187, 1187, 1187, 65814, 65814, 65814, 65814, 0, 65814, 0, 25250, 0, 16690, 0, 0, 4, 0, 0, 4, 4, 4",
      /* 1169 */ "4, 14, 0, 0, 49156, 0, 0, 0, 25314, 0, 0, 65550, 337970, 99162, 0, 0, 0, 65814, 0, 0, 0, 0, 25250, 0",
      /* 1193 */ "245764, 245764, 245764, 0, 245764, 245764, 245764, 245764, 0, 90130, 0, 0, 65814, 65814, 0, 25250",
      /* 1209 */ "81938, 245764, 0, 0, 73746, 0, 0, 49156, 49156, 49156, 0, 25250, 49156, 49156, 49156, 49156, 0",
      /* 1226 */ "106818, 0, 0, 73746, 589828, 0, 507908, 507908, 507908, 0, 507908, 507908, 507908, 507908, 0, 82438",
      /* 1242 */ "82438, 82438, 0, 25250, 82438, 82438, 82438, 82438, 0, 163844, 0, 0, 82438, 0, 0, 0, 82438, 0",
      /* 1260 */ "540680, 540680, 540680, 0, 540680, 540680, 540680, 540680, 491524, 0, 491524, 491524, 491524, 0",
      /* 1274 */ "164386, 0, 0, 147720, 0, 0, 525622, 557064, 0, 0, 761874, 0, 0, 804546, 0, 0, 853714, 0, 0, 887298",
      /* 1294 */ "0, 0, 983044, 0, 0, 0, 819204, 525622, 0, 16398, 525622, 525622, 0, 0, 0, 131076, 0, 0, 65814, 0",
      /* 1314 */ "65814, 65814, 65814, 0, 0, 278532, 278532, 278532, 278532, 589828, 221202, 294932, 229394, 0, 32782",
      /* 1329 */ "294932, 0, 73746, 589828, 49166, 245764, 0, 0, 180344, 0, 294932, 589828, 0, 0, 245764, 0, 0, 622600",
      /* 1347 */ "0, 0, 263094, 0, 638984, 0, 0, 0, 172050, 0, 344082, 0, 0, 278532, 0, 0, 573444, 0, 573444, 0, 0, 0",
      /* 1369 */ "428178, 327684, 0, 327684, 327684, 327684, 327684, 589828, 221202, 0, 229394, 0, 32782, 0, 0, 278532",
      /* 1385 */ "278532, 0, 278532, 278532, 278532, 0, 0, 0, 589828, 0, 0, 296070, 0, 0, 0, 1048580, 1048580, 1048580",
      /* 1403 */ "1130504, 0, 0, 0, 507922, 812962, 0, 0, 0, 573444, 573444, 573444, 1064964, 0, 1064964, 0, 0, 0",
      /* 1421 */ "688132, 0, 1048580, 0, 1048580, 0, 0, 0, 704530, 688132, 0, 0, 0, 722774, 722774, 722774, 0, 1097736",
      /* 1439 */ "1097736, 1097736, 0, 1097736, 1097736, 1097736, 1097736, 854442, 81934, 0, 0, 0, 737284, 862130, 0",
      /* 1454 */ "0, 0, 755846, 755846, 755846, 0, 895634, 0, 0, 0, 786436, 559714, 0, 0, 0, 821346, 0, 16634, 0, 0",
      /* 1474 */ "311316, 278546, 0, 147474, 0, 0, 327684, 0, 0, 0, 172050, 0, 0, 327684, 327684, 327684, 0, 0, 712722",
      /* 1493 */ "0, 0, 0, 854442, 0, 737298, 0, 0, 411282, 0, 238530, 0, 0, 0, 901140, 238546, 0, 0, 0, 1032212",
      /* 1513 */ "255122, 0, 296070, 0, 0, 1002042, 0, 4, 4, 4, 0, 0, 0, 262162, 270354, 286738, 294930, 0, 0, 0",
      /* 1533 */ "1064964, 1064964, 1064964, 311316, 0, 311316, 0, 0, 0, 1114120, 0, 778258, 0, 0, 468706, 0, 419698",
      /* 1550 */ "0, 0, 0, 491524, 491524, 0, 0, 0, 98318, 794642, 0, 0, 0, 507908, 0, 0, 477122, 0, 0, 0, 525622",
      /* 1571 */ "525622, 525622, 755846, 0, 0, 0, 540680, 0, 0, 0, 755846, 0, 821346, 0, 98318, 0, 0, 551378, 337970",
      /* 1590 */ "0, 999444, 0, 0, 557064, 525622, 901140, 0, 0, 0, 565266, 0, 0, 1032212, 0, 0, 0, 1015832, 0, 0, 0",
      /* 1611 */ "584210, 0, 477122, 0, 722774, 722774, 0, 722774, 0, 0, 0, 608546, 0, 0, 0, 589828, 589828, 608562, 0",
      /* 1630 */ "0, 0, 663570, 0, 0, 614418, 0, 0, 679954, 0, 592546, 0, 0, 0, 704516, 0, 0, 0, 163844, 1002186, 0, 0",
      /* 1652 */ "0, 704516, 337970"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1655; ++i) {CASEID[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TOKENSET = new int[113];
  static
  {
    final String s1[] =
    {
      /*   0 */ "54, 52, 64, 26, 52, 52, 54, 55, 47, 49, 49, 49, 52, 55, 29, 47, 29, 46, 18, 46, 32, 62, 46, 59, 60",
      /*  25 */ "47, 40, 61, 31, 63, 42, 57, 39, 57, 39, 57, 39, 38, 42, 42, 7, 0, 25, 58, 27, 60, 56, 56, 56, 34, 19",
      /*  51 */ "20, 53, 7, 58, 42, 42, 19, 20, 50, 7, 42, 18, 19, 20, 33, 23, 19, 20, 42, 21, 2, 3, 24, 51, 31, 24",
      /*  77 */ "48, 27, 33, 17, 11, 1, 10, 22, 16, 8, 14, 6, 30, 15, 35, 41, 9, 21, 13, 0, 12, 2, 3, 45, 4, 37, 36",
      /* 104 */ "43, 44, 28, 5, 2, 3, 2, 3, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 113; ++i) {TOKENSET[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] APPENDIX = new int[14];
  static
  {
    final String s1[] =
    {
      /*  0 */ "2, 327689, 262811, 278532, 45065, 294914, 40969, 294914, 275689, 352258, 279857, 352258, 450570",
      /* 13 */ "499722"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 14; ++i) {APPENDIX[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] LOOKBACK = new int[552];
  static
  {
    final String s1[] =
    {
      /*   0 */ "175, 175, 175, 175, 175, 173, 176, 181, 181, 181, 184, 194, 194, 194, 189, 197, 200, 203, 212, 212",
      /*  20 */ "221, 212, 221, 212, 221, 234, 234, 243, 234, 243, 234, 243, 256, 265, 265, 265, 265, 274, 274, 274",
      /*  40 */ "274, 175, 175, 175, 175, 283, 283, 283, 283, 290, 290, 290, 290, 297, 304, 175, 175, 175, 175, 307",
      /*  60 */ "312, 317, 322, 327, 327, 327, 327, 175, 175, 330, 330, 330, 333, 338, 343, 346, 346, 346, 175, 349",
      /*  80 */ "352, 352, 352, 355, 360, 365, 365, 365, 175, 175, 175, 175, 368, 381, 381, 381, 371, 376, 389, 384",
      /* 100 */ "392, 175, 175, 175, 395, 395, 395, 401, 406, 411, 421, 416, 416, 416, 416, 416, 426, 429, 398, 398",
      /* 120 */ "398, 434, 439, 444, 454, 449, 449, 449, 449, 449, 459, 175, 462, 465, 465, 465, 470, 175, 473, 473",
      /* 140 */ "473, 476, 486, 486, 486, 486, 489, 494, 499, 504, 507, 507, 507, 507, 479, 510, 175, 175, 175, 513",
      /* 160 */ "516, 524, 519, 527, 530, 175, 175, 175, 535, 538, 546, 541, 549, 3, 2, 0, 5, 5, 3, 4, 0, 6, 6, 0, 12",
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

  private static final int[] GOTO = new int[867];
  static
  {
    final String s1[] =
    {
      /*   0 */ "320, 386, 386, 386, 477, 386, 432, 386, 421, 386, 386, 386, 400, 386, 386, 386, 412, 386, 386, 386",
      /*  20 */ "386, 386, 432, 386, 326, 386, 386, 386, 332, 386, 386, 386, 386, 338, 542, 386, 343, 386, 386, 386",
      /*  40 */ "349, 386, 386, 386, 355, 386, 386, 386, 409, 386, 386, 386, 430, 386, 386, 386, 386, 386, 479, 386",
      /*  60 */ "386, 439, 386, 386, 361, 386, 386, 386, 386, 386, 511, 386, 367, 386, 386, 386, 386, 492, 386, 386",
      /*  80 */ "448, 386, 386, 386, 373, 386, 386, 386, 386, 386, 516, 386, 423, 386, 386, 386, 402, 379, 464, 386",
      /* 100 */ "386, 385, 542, 386, 386, 457, 386, 391, 397, 386, 386, 386, 406, 386, 386, 386, 427, 386, 386, 386",
      /* 120 */ "436, 386, 386, 386, 445, 386, 386, 386, 454, 386, 386, 386, 461, 386, 386, 386, 468, 386, 386, 386",
      /* 140 */ "483, 386, 386, 386, 498, 386, 386, 386, 502, 386, 386, 386, 506, 386, 386, 386, 386, 386, 386, 510",
      /* 160 */ "386, 387, 386, 386, 386, 386, 386, 515, 520, 386, 386, 386, 386, 386, 525, 386, 386, 351, 386, 524",
      /* 180 */ "393, 386, 386, 386, 328, 379, 464, 386, 334, 529, 464, 386, 334, 533, 464, 386, 386, 345, 386, 391",
      /* 200 */ "386, 386, 471, 386, 386, 386, 441, 386, 537, 386, 386, 386, 386, 386, 386, 486, 363, 386, 386, 386",
      /* 220 */ "386, 386, 386, 474, 386, 386, 386, 369, 386, 386, 386, 541, 386, 386, 386, 546, 386, 494, 550, 524",
      /* 240 */ "386, 339, 386, 386, 554, 386, 386, 386, 558, 386, 386, 386, 386, 386, 471, 386, 386, 386, 441, 386",
      /* 260 */ "562, 386, 386, 386, 566, 386, 386, 386, 386, 386, 489, 386, 386, 386, 375, 386, 570, 386, 386, 386",
      /* 280 */ "386, 381, 386, 524, 386, 386, 386, 415, 386, 386, 386, 418, 386, 386, 322, 386, 574, 386, 386, 386",
      /* 300 */ "578, 386, 386, 386, 582, 386, 386, 386, 386, 450, 550, 524, 386, 357, 586, 386, 590, 386, 386, 386",
      /* 320 */ "594, 697, 698, 698, 657, 698, 838, 697, 698, 698, 732, 794, 852, 713, 698, 698, 732, 798, 709, 698",
      /* 340 */ "698, 698, 672, 600, 697, 698, 698, 745, 749, 597, 697, 698, 698, 776, 698, 820, 697, 698, 698, 782",
      /* 360 */ "698, 666, 697, 698, 698, 824, 698, 790, 723, 698, 698, 828, 698, 800, 697, 698, 698, 856, 698, 740",
      /* 380 */ "609, 698, 698, 861, 698, 724, 698, 698, 698, 698, 669, 755, 761, 698, 698, 863, 698, 606, 697, 698",
      /* 400 */ "698, 703, 698, 698, 732, 737, 612, 697, 698, 698, 704, 708, 698, 699, 708, 698, 684, 698, 698, 687",
      /* 420 */ "698, 698, 697, 698, 698, 728, 698, 615, 697, 698, 698, 713, 698, 698, 788, 698, 642, 697, 698, 698",
      /* 440 */ "718, 698, 698, 813, 698, 618, 697, 698, 698, 723, 698, 698, 833, 857, 627, 697, 698, 698, 733, 745",
      /* 460 */ "749, 621, 697, 698, 698, 741, 698, 719, 633, 697, 698, 698, 762, 698, 698, 767, 698, 698, 779, 698",
      /* 480 */ "698, 714, 698, 624, 697, 698, 698, 818, 698, 698, 829, 698, 698, 836, 698, 698, 833, 772, 639, 697",
      /* 500 */ "698, 698, 630, 697, 698, 698, 654, 697, 698, 698, 766, 698, 698, 698, 751, 809, 698, 698, 698, 757",
      /* 520 */ "636, 697, 698, 698, 755, 698, 698, 698, 771, 849, 609, 698, 698, 804, 609, 698, 698, 645, 697, 698",
      /* 540 */ "698, 648, 698, 698, 698, 807, 651, 698, 698, 698, 842, 846, 698, 698, 785, 697, 698, 698, 814, 723",
      /* 560 */ "698, 698, 675, 697, 698, 698, 681, 697, 698, 698, 603, 697, 698, 698, 660, 697, 698, 698, 663, 697",
      /* 580 */ "698, 698, 690, 697, 698, 698, 693, 698, 698, 698, 678, 697, 698, 698, 6, 0, 4121, 0, 0, 106508, 0, 0",
      /* 602 */ "114700, 0, 0, 172076, 0, 0, 176137, 0, 0, 180228, 0, 0, 180233, 0, 0, 184817, 0, 0, 189009, 0, 0",
      /* 623 */ "192521, 0, 0, 196617, 0, 0, 196628, 0, 0, 201457, 0, 0, 213012, 0, 0, 217097, 0, 0, 221204, 0, 0",
      /* 644 */ "237588, 0, 0, 242777, 0, 0, 258945, 0, 0, 263153, 0, 0, 303901, 0, 0, 311305, 0, 0, 316577, 0, 0",
      /* 665 */ "319497, 0, 0, 344073, 0, 0, 401420, 0, 0, 417820, 0, 0, 435321, 0, 0, 435385, 0, 0, 438281, 0, 0",
      /* 686 */ "451857, 0, 0, 456025, 0, 0, 458761, 0, 0, 466980, 483332, 8401, 0, 0, 0, 0, 28737, 12689, 0, 0, 0",
      /* 707 */ "28769, 32777, 0, 0, 0, 57353, 53465, 0, 0, 0, 65545, 70265, 0, 0, 0, 107209, 82225, 0, 0, 0, 122889",
      /* 728 */ "0, 360457, 98841, 364993, 102409, 0, 0, 0, 152289, 373297, 376841, 155652, 155652, 0, 180228, 0, 0",
      /* 745 */ "156417, 335876, 327684, 160521, 385028, 385028, 0, 0, 78473, 0, 0, 385841, 0, 0, 78513, 0, 163849, 0",
      /* 763 */ "0, 0, 234329, 156433, 0, 0, 0, 249865, 221193, 0, 0, 0, 267369, 0, 225289, 229385, 0, 20521, 24585, 0",
      /* 783 */ "0, 499716, 0, 0, 540708, 0, 16433, 0, 0, 0, 348169, 373345, 376841, 155652, 155652, 0, 229380, 0, 0",
      /* 802 */ "94217, 0, 0, 204828, 180228, 0, 62065, 0, 0, 0, 394273, 238537, 0, 0, 0, 286729, 0, 245769, 0, 0",
      /* 822 */ "98316, 0, 0, 413705, 98841, 364993, 249865, 0, 0, 0, 292089, 0, 458756, 417801, 0, 90785, 0, 0, 57356",
      /* 841 */ "0, 442372, 270345, 0, 475140, 434180, 0, 421897, 0, 188444, 180228, 0, 49233, 335985, 16388, 296257",
      /* 857 */ "0, 0, 0, 324777, 0, 299017, 0, 0, 98841, 365057"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 867; ++i) {GOTO[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] REDUCTION = new int[140];
  static
  {
    final String s1[] =
    {
      /*   0 */ "41, 0, 0, -1, 1, -1, 1, 1, 2, -1, 3, -1, 4, -1, 5, -1, 42, 2, 6, -1, 43, 3, 7, 4, 8, 7, 8, 6, 8, 5, 8",
      /*  31 */ "8, 9, -1, 10, 9, 11, -1, 12, -1, 44, 10, 13, 11, 13, -1, 14, 13, 14, 12, 15, 15, 15, 14, 16, 16, 17",
      /*  57 */ "-1, 18, 17, 46, 19, 45, 18, 19, -1, 20, -1, 21, -1, 48, 21, 47, 20, 22, -1, 23, 22, 23, -1, 24, 24",
      /*  82 */ "24, 23, 50, 26, 49, 25, 25, -1, 51, 27, 26, 28, 27, -1, 52, 29, 28, -1, 53, 30, 29, -1, 30, -1, 31",
      /* 107 */ "34, 31, 33, 31, 32, 31, 31, 32, -1, 33, 35, 34, 36, 54, 37, 35, -1, 36, 38, 36, -1, 56, 40, 55, 39",
      /* 132 */ "37, -1, 38, 41, 39, 42, 40, 42"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 140; ++i) {REDUCTION[i] = Integer.parseInt(s2[i]);}
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
    "rs",
    "s",
    "RS",
    "comment",
    "prolog",
    "version",
    "rule",
    "mark",
    "alts",
    "addAlt",
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
    "dchar",
    "schar",
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
    "IMPLICIT-51",
    "IMPLICIT-52",
    "IMPLICIT-53",
    "IMPLICIT-54",
    "IMPLICIT-55",
    "IMPLICIT-56"
  };

                                                            // line 936 "ixml.ebnf"
                                                            private int hexBegin;
                                                              private boolean deleted;
                                                              private boolean exclusion;
                                                              private String codepoint;
                                                              private String firstCodepoint;
                                                              private String lastCodepoint;
                                                              private String clazz;
                                                              private java.util.List<Member> members;
                                                              private Grammar grammar;
                                                              private Mark mark;
                                                              private java.util.Stack<Alts> alts = new java.util.Stack<>();
                                                              private StringBuilder stringBuilder = new StringBuilder();
                                                              private StringBuilder nameBuilder = new StringBuilder();

                                                              public static Grammar parse(String content) {
                                                                Ixml parser = new Ixml(content);
                                                                try
                                                                {
                                                                  parser.parse_ixml();
                                                                }
                                                                catch (ParseException pe)
                                                                {
                                                                  int offending = pe.getOffending();
                                                                  int begin = pe.getBegin();
                                                                  String prefix = content.substring(0, begin);
                                                                  int line = prefix.replaceAll("[^\n]", "").length() + 1;
                                                                  int column = prefix.length() - prefix.lastIndexOf('\n');
                                                                  throw new de.bottlecaps.markup.BlitzParseException(
                                                                    "Failed to process grammar:\n" + parser.getErrorMessage(pe),
                                                                    offending >= 0 ? TOKEN[offending]
                                                                                   : begin < content.length() ? ("'" + Character.toString(content.codePointAt(begin)) + "'")
                                                                                                              : "$",
                                                                    line,
                                                                    column);
                                                                }
                                                                de.bottlecaps.markup.blitz.transform.PostProcess.process(parser.grammar);
                                                                return parser.grammar;
                                                              }
                                                            }
                                                            // line 1783 "Ixml.java"
// End
