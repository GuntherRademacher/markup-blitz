// This file was generated on Sat Nov 4, 2023 15:44 (UTC+01) by REx v5.57 which is Copyright (c) 1979-2023 by Gunther Rademacher <grd@gmx.net>
// REx command line: -glalr 1 -java -a java -name de.bottlecaps.markup.blitz.grammar.Ixml ixml.ebnf

package de.bottlecaps.markup.blitz.grammar;

import java.util.PriorityQueue;

public class Ixml
{
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
                                                            // line 485 "Ixml.java"
        }
        break;
      case 1:
        {
                                                            // line 3 "ixml.ebnf"
                                                            de.bottlecaps.markup.blitz.Errors.S01.thro();
                                                            // line 492 "Ixml.java"
        }
        break;
      case 2:
        {
                                                            // line 12 "ixml.ebnf"
                                                            grammar = new Grammar(stringBuilder.toString());
                                                            // line 499 "Ixml.java"
        }
        break;
      case 3:
        {
                                                            // line 16 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            grammar.addRule(new Rule(mark, alias, name, alts.peek()));
                                                            // line 507 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 19 "ixml.ebnf"
                                                            alts.pop();
                                                            // line 514 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 21 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 521 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 22 "ixml.ebnf"
                                                            nameBuilder.append(input.subSequence(b0, e0));
                                                            // line 528 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 25 "ixml.ebnf"
                                                            name = nameBuilder.toString();
                                                            alias = null;
                                                            // line 536 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 36 "ixml.ebnf"
                                                            savedName = name;
                                                            // line 543 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 38 "ixml.ebnf"
                                                            alias = name;
                                                            name = savedName;
                                                            // line 551 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 42 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 558 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 52 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 565 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 54 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 573 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 58 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 581 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 62 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 590 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 68 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 598 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 72 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 607 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 78 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 615 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 81 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 622 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 82 "ixml.ebnf"
                                                            mark = Mark.NODE;
                                                            // line 629 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 83 "ixml.ebnf"
                                                            mark = Mark.DELETE;
                                                            // line 636 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 84 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 643 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 87 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, alias, name);
                                                            // line 650 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 88 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 657 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 91 "ixml.ebnf"
                                                            alts.peek().last().addCharset(new Charset(deleted, exclusion, members));
                                                            // line 664 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 94 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 671 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 95 "ixml.ebnf"
                                                            alts.peek().last().addCodepoint(deleted, codepoint);
                                                            // line 678 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 97 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 685 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 98 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 692 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 99 "ixml.ebnf"
                                                            stringBuilder.append(input.subSequence(b0, e0));
                                                            // line 699 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 104 "ixml.ebnf"
                                                            validateStringChar(input.toString().codePointAt(b0));
                                                            // line 706 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 106 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 713 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 107 "ixml.ebnf"
                                                            codepoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 720 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 112 "ixml.ebnf"
                                                            exclusion = false;
                                                            members = new java.util.ArrayList<>();
                                                            // line 728 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 117 "ixml.ebnf"
                                                            exclusion = true;
                                                            members = new java.util.ArrayList<>();
                                                            // line 736 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 122 "ixml.ebnf"
                                                            members.add(new StringMember(stringBuilder.toString(), false));
                                                            // line 743 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 123 "ixml.ebnf"
                                                            members.add(new StringMember(codepoint, true));
                                                            // line 750 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 124 "ixml.ebnf"
                                                            members.add(new RangeMember(firstCodepoint, lastCodepoint));
                                                            // line 757 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 125 "ixml.ebnf"
                                                            members.add(new ClassMember(clazz));
                                                            // line 764 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 127 "ixml.ebnf"
                                                            firstCodepoint = codepoint;
                                                            // line 771 "Ixml.java"
        }
        break;
      case 40:
        {
                                                            // line 128 "ixml.ebnf"
                                                            lastCodepoint = codepoint;
                                                            // line 778 "Ixml.java"
        }
        break;
      case 41:
        {
                                                            // line 129 "ixml.ebnf"
                                                            codepoint = input.subSequence(b0, e0).toString();
                                                            // line 785 "Ixml.java"
        }
        break;
      case 42:
        {
                                                            // line 134 "ixml.ebnf"
                                                            clazz += input.subSequence(b0, e0);
                                                            // line 792 "Ixml.java"
        }
        break;
      case 43:
        {
                                                            // line 140 "ixml.ebnf"
                                                            clazz = input.subSequence(b0, e0).toString();
                                                            // line 799 "Ixml.java"
        }
        break;
      case 44:
        {
                                                            // line 141 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 806 "Ixml.java"
        }
        break;
      case 45:
        {
                                                            // line 142 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codepoint);
                                                            // line 813 "Ixml.java"
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

          int lo = 0, hi = 363;
          for (int m = 182; ; m = (hi + lo) >> 1)
          {
            if (MAP2[m] > c0) {hi = m - 1;}
            else if (MAP2[364 + m] < c0) {lo = m + 1;}
            else {charclass = MAP2[728 + m]; break;}
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
    for (int i = 0; i < 71; i += 32)
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
      /*   0 */ "67, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1",
      /*  34 */ "3, 4, 1, 1, 1, 5, 6, 7, 8, 9, 10, 11, 12, 1, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 14, 15, 1, 16",
      /*  62 */ "17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41",
      /*  87 */ "42, 43, 44, 45, 46, 1, 47, 48, 49, 1, 50, 50, 50, 50, 51, 50, 52, 52, 53, 52, 52, 54, 55, 56, 57, 52",
      /* 113 */ "52, 58, 59, 52, 52, 60, 52, 61, 52, 52, 62, 63, 64, 65, 1"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 128; ++i) {MAP0[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] MAP1 = new int[2345];
  static
  {
    final String s1[] =
    {
      /*    0 */ "432, 580, 1316, 1316, 1316, 448, 1252, 478, 1316, 534, 518, 493, 642, 1277, 688, 1208, 886, 898, 553",
      /*   19 */ "596, 612, 628, 658, 674, 714, 749, 765, 796, 568, 823, 839, 855, 871, 914, 1316, 1316, 698, 930, 957",
      /*   39 */ "1346, 1315, 1316, 1316, 1316, 537, 973, 989, 1005, 508, 1021, 1488, 1460, 1045, 1061, 1083, 1099",
      /*   56 */ "1563, 1067, 1316, 1115, 1316, 1316, 1380, 1131, 1147, 462, 1163, 1179, 1180, 1180, 1180, 1180, 1180",
      /*   73 */ "1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1180, 1196, 733",
      /*   90 */ "1224, 1240, 807, 1180, 1180, 1180, 1268, 1293, 1309, 1332, 1180, 1180, 1180, 1180, 1316, 1316, 1316",
      /*  107 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  124 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  141 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1029, 1316, 1316",
      /*  158 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  175 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  192 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  209 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  226 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  243 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  260 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  277 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  294 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  311 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1351, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  328 */ "1316, 1367, 1316, 1316, 1396, 1412, 728, 1428, 1451, 780, 1476, 1504, 941, 1520, 1536, 1435, 1316",
      /*  345 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  362 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  379 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  396 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  413 */ "1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316, 1316",
      /*  430 */ "1316, 1552, 1579, 1589, 1580, 1580, 1603, 1611, 1635, 1641, 1649, 1657, 1665, 1673, 1695, 1709, 1702",
      /*  447 */ "1717, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 2110, 1861, 1867, 1580, 1864, 2201, 1580, 1580",
      /*  464 */ "1861, 1864, 1580, 1580, 1580, 1580, 1580, 1580, 2125, 2128, 2080, 2125, 2132, 1580, 2189, 1766, 1861",
      /*  481 */ "1861, 2022, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 2025, 1861, 1580, 2124, 2125, 2125",
      /*  498 */ "2125, 2125, 1781, 1753, 1580, 1861, 1861, 1861, 1866, 1866, 1580, 2082, 2125, 2131, 1861, 1861, 1861",
      /*  515 */ "1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1580, 1860, 1861, 1861, 1861, 1862, 2249, 1860, 1861",
      /*  532 */ "1861, 1861, 1743, 1859, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861",
      /*  549 */ "1861, 2213, 1861, 1861, 2091, 1861, 1861, 1861, 1861, 1861, 1861, 1795, 2124, 2277, 1779, 1861, 1835",
      /*  566 */ "2125, 1860, 1860, 1861, 1861, 1861, 1861, 1861, 1946, 2130, 2158, 2126, 2125, 2131, 1580, 1580, 1580",
      /*  583 */ "1580, 2187, 2248, 1618, 2248, 1861, 1861, 1862, 1861, 1861, 1861, 1862, 1861, 2006, 2213, 1858, 1861",
      /*  600 */ "1861, 2023, 1680, 1853, 2143, 1875, 1580, 1595, 1835, 2125, 1867, 1580, 2236, 1884, 1858, 1861, 1861",
      /*  617 */ "2023, 1931, 2337, 2328, 1731, 2265, 1938, 2065, 2125, 1893, 1580, 2236, 2025, 2022, 1861, 1861, 2023",
      /*  634 */ "1934, 1853, 1745, 2277, 2250, 1580, 1835, 2125, 1580, 1580, 2125, 2130, 1861, 1861, 1861, 1861, 1861",
      /*  651 */ "2313, 2125, 2125, 2125, 1735, 2157, 1861, 2006, 2213, 1858, 1861, 1861, 2023, 1934, 1904, 2143, 2253",
      /*  668 */ "2252, 1595, 1835, 2125, 2249, 1580, 1878, 1922, 1881, 1927, 1801, 1922, 1861, 1867, 2132, 2253, 2250",
      /*  685 */ "1580, 2065, 2125, 1580, 1580, 2156, 1861, 1861, 1861, 2125, 2125, 2125, 1626, 1861, 1861, 1861, 1861",
      /*  702 */ "1861, 1861, 1861, 1861, 1861, 1881, 1862, 1881, 1861, 1861, 1861, 1861, 1969, 2019, 2023, 1861, 1861",
      /*  719 */ "2023, 2020, 2172, 2048, 2142, 2066, 1867, 1835, 2125, 1580, 1580, 2188, 1861, 1859, 1861, 1861, 1861",
      /*  736 */ "1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1864, 1845, 2108, 1580, 1969, 2019, 2023, 1861",
      /*  753 */ "1861, 2023, 2020, 1904, 2252, 2067, 1580, 2189, 1835, 2125, 1803, 1580, 1969, 2019, 2023, 1861, 1861",
      /*  770 */ "1861, 1861, 1954, 2143, 1875, 1580, 1580, 1835, 2125, 1580, 1859, 1861, 1861, 1861, 1861, 1861, 1865",
      /*  787 */ "1580, 2254, 1580, 2125, 2131, 2125, 2125, 1811, 2247, 1969, 1861, 1862, 1859, 1861, 1861, 2022, 2027",
      /*  804 */ "1862, 2264, 1982, 1580, 1580, 1580, 1580, 1580, 2188, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580",
      /*  821 */ "1580, 1580, 1685, 1687, 1970, 1860, 1765, 1761, 1946, 1962, 2026, 2127, 2125, 1627, 1580, 1580, 1580",
      /*  838 */ "1580, 2250, 1580, 1580, 2131, 2125, 2131, 1724, 2265, 1861, 1860, 1861, 1861, 1861, 1864, 2124, 2126",
      /*  855 */ "1782, 1814, 2125, 2124, 2125, 2125, 2125, 2128, 2252, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1861",
      /*  872 */ "1861, 1861, 1861, 1861, 1979, 2123, 1914, 2125, 2131, 1863, 1992, 1799, 1968, 1949, 1861, 1861, 1813",
      /*  889 */ "1824, 1822, 2315, 1580, 1580, 1861, 1861, 1861, 2036, 1580, 1580, 1580, 1580, 2023, 1864, 1580, 1580",
      /*  906 */ "1580, 1580, 1580, 1580, 2050, 2125, 2125, 2126, 1896, 1875, 2125, 2002, 1861, 1861, 1861, 1861, 2025",
      /*  923 */ "2190, 1861, 1861, 1861, 1861, 1861, 2021, 1861, 1881, 1861, 1861, 1861, 1861, 1881, 1862, 1881, 1861",
      /*  940 */ "1862, 1861, 1861, 1861, 1861, 1861, 1826, 1910, 1580, 2154, 2273, 2125, 2131, 1861, 1861, 1862, 2248",
      /*  957 */ "1861, 1861, 1881, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1979, 1580, 1580, 1580, 1580, 2016",
      /*  974 */ "1861, 1861, 1866, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1866, 1580, 1580, 1861, 2019",
      /*  991 */ "2035, 1580, 1861, 1861, 2035, 1580, 1861, 1861, 2045, 1580, 1861, 2019, 2327, 1580, 1861, 1861, 1861",
      /* 1008 */ "1861, 1861, 1861, 1994, 2127, 2252, 2124, 2144, 2059, 2125, 2131, 1580, 1580, 1861, 1861, 1861, 1861",
      /* 1025 */ "1861, 2301, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1863, 1580, 1580, 1580, 1580, 1580, 1580",
      /* 1042 */ "1580, 1580, 1580, 1861, 1861, 2158, 2132, 1861, 1861, 1861, 1861, 1861, 1861, 2075, 2126, 1729, 2128",
      /* 1059 */ "2051, 2316, 2125, 2131, 2125, 2131, 2188, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580",
      /* 1076 */ "1580, 2140, 2125, 2123, 2152, 2167, 1580, 2090, 1861, 1861, 1861, 1861, 1861, 2100, 1984, 2280, 1865",
      /* 1093 */ "2125, 2131, 1580, 2051, 2129, 1580, 2092, 1861, 1861, 1861, 1623, 1757, 2125, 1811, 1861, 1861, 1861",
      /* 1110 */ "1861, 2159, 2118, 2131, 1580, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 2125, 2125, 2125, 2125",
      /* 1127 */ "2126, 1580, 1580, 2050, 1861, 1861, 1861, 1861, 1861, 1861, 2019, 2026, 2283, 1864, 2214, 1865, 1861",
      /* 1144 */ "1864, 2283, 1864, 2180, 2185, 1580, 1580, 1580, 1581, 1580, 2251, 2132, 1580, 1580, 1581, 1580, 1580",
      /* 1161 */ "2249, 2188, 2198, 1859, 2027, 2212, 2201, 1937, 1861, 2222, 1969, 1885, 1580, 1580, 1580, 1580, 1580",
      /* 1178 */ "1580, 1801, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580",
      /* 1195 */ "1580, 1861, 1861, 1861, 1861, 1861, 1862, 1861, 1861, 1861, 1861, 1861, 1862, 1861, 1861, 1861, 1861",
      /* 1212 */ "1813, 2125, 2302, 1580, 2125, 1811, 1861, 1861, 1861, 2313, 2106, 2248, 1861, 1861, 1861, 1861, 2025",
      /* 1229 */ "2190, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 2188, 1580, 2251, 1861, 1861, 1862, 1580, 1862, 1862",
      /* 1246 */ "1862, 1862, 1862, 1862, 1862, 1862, 2125, 2125, 2125, 2125, 2125, 2125, 2125, 2125, 2125, 2125, 2125",
      /* 1263 */ "2125, 2125, 2125, 2019, 2224, 1594, 1580, 1580, 1580, 1580, 1839, 2212, 1801, 1860, 1861, 1861, 1861",
      /* 1280 */ "1861, 1861, 1861, 1861, 1861, 1861, 1861, 1774, 2316, 1790, 2104, 2125, 1952, 1861, 1861, 1862, 2236",
      /* 1297 */ "1860, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 2021, 1969, 1861, 1861, 1861, 1861",
      /* 1314 */ "1863, 1860, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861",
      /* 1331 */ "1861, 1861, 1862, 1580, 1580, 1861, 1861, 1861, 1866, 1580, 1580, 1580, 1580, 1580, 1580, 1861, 1861",
      /* 1348 */ "1580, 1580, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1864, 1580, 1580, 1580, 1580",
      /* 1365 */ "1580, 1580, 1861, 1864, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1861, 1861, 1861, 1861, 1861",
      /* 1382 */ "1863, 1863, 1861, 1861, 1861, 1861, 1863, 1863, 1861, 2204, 1861, 1861, 1861, 1863, 1861, 1864, 1861",
      /* 1399 */ "1861, 2125, 2108, 1580, 1580, 1861, 1861, 1861, 1861, 1861, 2158, 2050, 1827, 1861, 1861, 1861, 2251",
      /* 1416 */ "1861, 1861, 1861, 1861, 1861, 1861, 1861, 1861, 1863, 1580, 2131, 1580, 1861, 2223, 1865, 1580, 1861",
      /* 1433 */ "1866, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1861, 1861, 1861, 1861, 2336, 2277, 2125",
      /* 1450 */ "2131, 2296, 2154, 1861, 1861, 2232, 1580, 1580, 1580, 1861, 1861, 1861, 1861, 1861, 1861, 1865, 1580",
      /* 1467 */ "1580, 1860, 1580, 2125, 2131, 1580, 1580, 1580, 1580, 2125, 1811, 1861, 1861, 1813, 2127, 1861, 1861",
      /* 1484 */ "2158, 2125, 2131, 1580, 1861, 1861, 1861, 1864, 2037, 2132, 2264, 1733, 2065, 2125, 1861, 1861, 1861",
      /* 1501 */ "1863, 1864, 1580, 2091, 1861, 1861, 1861, 1861, 1861, 1895, 2262, 1580, 2188, 2125, 2131, 1580, 1580",
      /* 1518 */ "1580, 1580, 1861, 1861, 1861, 1861, 1861, 1861, 2291, 2310, 2301, 1580, 1580, 2008, 1861, 2325, 2239",
      /* 1535 */ "1580, 2024, 2024, 2024, 1580, 1862, 1862, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580, 1580",
      /* 1552 */ "1861, 1861, 1861, 1861, 1865, 1580, 1861, 1861, 1862, 1971, 1861, 1861, 1861, 1861, 1861, 1865, 2050",
      /* 1569 */ "2317, 1580, 2125, 1843, 2125, 1811, 1861, 1861, 1861, 1863, 67, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 2",
      /* 1592 */ "1, 1, 2, 1, 1, 1, 1, 49, 49, 1, 49, 2, 1, 3, 4, 1, 1, 1, 5, 6, 7, 8, 9, 10, 11, 12, 1, 1, 1, 1, 1",
      /* 1623 */ "49, 1, 66, 66, 66, 66, 1, 1, 49, 49, 49, 49, 13, 13, 13, 13, 13, 13, 13, 13, 14, 15, 1, 16, 17, 18",
      /* 1649 */ "19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43",
      /* 1674 */ "44, 45, 46, 1, 47, 48, 49, 1, 49, 1, 1, 1, 49, 49, 1, 49, 1, 1, 49, 1, 1, 1, 50, 50, 50, 50, 51, 50",
      /* 1702 */ "52, 52, 58, 59, 52, 52, 60, 52, 53, 52, 52, 54, 55, 56, 57, 61, 52, 52, 62, 63, 64, 65, 1, 1, 1, 1",
      /* 1728 */ "1, 66, 1, 66, 1, 1, 66, 66, 66, 1, 1, 1, 1, 49, 49, 49, 49, 1, 66, 66, 66, 66, 66, 1, 66, 1, 66, 66",
      /* 1756 */ "1, 66, 66, 1, 66, 1, 1, 49, 49, 1, 49, 49, 49, 1, 49, 1, 49, 49, 49, 49, 49, 49, 1, 49, 66, 66, 66",
      /* 1783 */ "66, 66, 66, 66, 1, 66, 66, 66, 66, 66, 66, 66, 49, 49, 66, 1, 66, 49, 1, 1, 1, 49, 49, 1, 1, 1, 1, 1",
      /* 1811 */ "66, 66, 49, 49, 49, 49, 49, 49, 66, 66, 66, 66, 66, 66, 66, 49, 66, 66, 66, 66, 66, 66, 1, 49, 49",
      /* 1836 */ "49, 66, 66, 1, 1, 66, 66, 66, 66, 1, 1, 1, 49, 49, 49, 49, 66, 49, 49, 1, 1, 66, 49, 1, 1, 49, 49",
      /* 1863 */ "49, 49, 49, 49, 49, 49, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 66, 49, 1, 49, 49, 49, 49, 1, 1, 1, 1, 49",
      /* 1892 */ "1, 66, 66, 49, 49, 49, 66, 1, 1, 66, 66, 1, 49, 49, 1, 1, 66, 49, 1, 66, 66, 1, 1, 66, 66, 1, 1, 66",
      /* 1920 */ "66, 49, 49, 49, 49, 1, 1, 1, 49, 49, 1, 49, 1, 49, 49, 1, 49, 49, 1, 49, 49, 49, 49, 1, 49, 1, 49",
      /* 1947 */ "66, 49, 49, 66, 66, 66, 66, 49, 49, 49, 1, 1, 49, 1, 1, 66, 66, 1, 66, 66, 49, 1, 1, 1, 1, 1, 1, 49",
      /* 1975 */ "49, 49, 49, 49, 49, 49, 49, 1, 1, 66, 66, 66, 1, 66, 1, 1, 1, 66, 66, 49, 49, 49, 49, 66, 66, 1, 66",
      /* 2002 */ "66, 66, 1, 1, 1, 66, 1, 1, 1, 49, 49, 49, 1, 1, 2, 49, 49, 49, 49, 49, 49, 49, 1, 49, 49, 49, 49, 49",
      /* 2030 */ "49, 1, 49, 1, 1, 49, 49, 66, 66, 66, 1, 1, 1, 1, 66, 49, 49, 66, 66, 1, 1, 1, 1, 1, 66, 66, 66, 66",
      /* 2058 */ "66, 1, 1, 1, 1, 49, 66, 1, 1, 1, 1, 1, 1, 66, 66, 1, 1, 49, 49, 49, 49, 49, 1, 66, 1, 1, 1, 66, 66",
      /* 2087 */ "66, 2, 1, 66, 66, 66, 66, 1, 49, 49, 49, 49, 49, 49, 49, 49, 49, 66, 1, 66, 66, 66, 66, 49, 49, 1, 1",
      /* 2114 */ "1, 1, 49, 49, 66, 66, 1, 1, 1, 66, 1, 66, 66, 66, 66, 66, 66, 66, 66, 1, 1, 1, 1, 1, 1, 1, 66, 66",
      /* 2142 */ "66, 1, 66, 66, 66, 66, 1, 1, 1, 49, 66, 49, 49, 49, 49, 66, 49, 49, 49, 49, 49, 49, 49, 66, 1, 49",
      /* 2168 */ "49, 1, 1, 66, 49, 49, 1, 1, 1, 49, 66, 66, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 49, 1, 1, 1",
      /* 2199 */ "1, 49, 1, 1, 1, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 49, 49, 49, 49, 1, 1, 49, 49, 49, 49, 1, 1, 49",
      /* 2227 */ "49, 49, 49, 1, 1, 49, 49, 49, 1, 1, 66, 66, 1, 1, 49, 49, 49, 1, 66, 1, 1, 1, 1, 49, 1, 1, 1, 1, 1",
      /* 2256 */ "1, 1, 66, 1, 1, 1, 66, 66, 1, 1, 66, 1, 1, 1, 1, 1, 1, 49, 49, 49, 49, 66, 1, 1, 1, 1, 66, 1, 1, 49",
      /* 2286 */ "49, 49, 1, 49, 49, 66, 49, 66, 66, 66, 49, 49, 66, 49, 49, 49, 66, 49, 1, 1, 1, 1, 1, 1, 66, 49, 49",
      /* 2313 */ "49, 49, 49, 66, 66, 66, 66, 66, 1, 1, 66, 66, 49, 49, 49, 1, 66, 66, 1, 1, 1, 1, 66, 49, 49, 49, 1",
      /* 2340 */ "1, 66, 1, 1, 1"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2345; ++i) {MAP1[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] MAP2 = new int[1092];
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
      /*  341 */ "173783, 173824, 177973, 177984, 178206, 194560, 195102, 196608, 262144, 327680, 393216, 458752",
      /*  353 */ "524288, 589824, 655360, 720896, 786432, 851968, 917504, 917760, 918000, 983040, 1048576, 63743",
      /*  365 */ "64109, 64111, 64217, 64255, 64262, 64274, 64279, 64284, 64285, 64286, 64296, 64297, 64310, 64311",
      /*  379 */ "64316, 64317, 64318, 64319, 64321, 64322, 64324, 64325, 64433, 64466, 64829, 64847, 64911, 64913",
      /*  393 */ "64967, 64975, 65019, 65023, 65039, 65055, 65062, 65135, 65140, 65141, 65276, 65295, 65305, 65312",
      /*  407 */ "65338, 65344, 65370, 65381, 65470, 65473, 65479, 65481, 65487, 65489, 65495, 65497, 65500, 65533",
      /*  421 */ "65547, 65548, 65574, 65575, 65594, 65595, 65597, 65598, 65613, 65615, 65629, 65663, 65786, 66044",
      /*  435 */ "66045, 66175, 66204, 66207, 66256, 66303, 66334, 66351, 66368, 66369, 66377, 66431, 66461, 66463",
      /*  449 */ "66499, 66503, 66511, 66559, 66717, 66719, 66729, 67583, 67589, 67591, 67592, 67593, 67637, 67638",
      /*  463 */ "67640, 67643, 67644, 67646, 67669, 67839, 67861, 67871, 67897, 67967, 68023, 68029, 68031, 68095",
      /*  477 */ "68096, 68099, 68100, 68102, 68107, 68111, 68115, 68116, 68119, 68120, 68147, 68151, 68154, 68158",
      /*  491 */ "68159, 68191, 68220, 68351, 68405, 68415, 68437, 68447, 68466, 68607, 68680, 69632, 69633, 69634",
      /*  505 */ "69687, 69702, 69733, 69743, 69759, 69761, 69762, 69807, 69810, 69814, 69816, 69818, 69839, 69864",
      /*  519 */ "69871, 69881, 69887, 69890, 69926, 69931, 69932, 69940, 69941, 69951, 70015, 70017, 70018, 70066",
      /*  533 */ "70069, 70078, 70080, 70084, 70095, 70105, 71295, 71338, 71339, 71340, 71341, 71343, 71349, 71350",
      /*  547 */ "71351, 71359, 71369, 73727, 74606, 77823, 78894, 92159, 92728, 93951, 94020, 94031, 94032, 94094",
      /*  561 */ "94098, 94111, 110591, 110593, 119142, 119145, 119162, 119170, 119172, 119179, 119209, 119213, 119361",
      /*  574 */ "119364, 119807, 119892, 119893, 119964, 119965, 119967, 119969, 119970, 119972, 119974, 119976",
      /*  586 */ "119980, 119981, 119993, 119994, 119995, 119996, 120003, 120004, 120069, 120070, 120074, 120076",
      /*  598 */ "120084, 120085, 120092, 120093, 120121, 120122, 120126, 120127, 120132, 120133, 120134, 120137",
      /*  610 */ "120144, 120145, 120485, 120487, 120512, 120513, 120538, 120539, 120570, 120571, 120596, 120597",
      /*  622 */ "120628, 120629, 120654, 120655, 120686, 120687, 120712, 120713, 120744, 120745, 120770, 120771",
      /*  634 */ "120779, 120781, 120831, 126463, 126467, 126468, 126495, 126496, 126498, 126499, 126500, 126502",
      /*  646 */ "126503, 126504, 126514, 126515, 126519, 126520, 126521, 126522, 126523, 126529, 126530, 126534",
      /*  658 */ "126535, 126536, 126537, 126538, 126539, 126540, 126543, 126544, 126546, 126547, 126548, 126550",
      /*  670 */ "126551, 126552, 126553, 126554, 126555, 126556, 126557, 126558, 126559, 126560, 126562, 126563",
      /*  682 */ "126564, 126566, 126570, 126571, 126578, 126579, 126583, 126584, 126588, 126589, 126590, 126591",
      /*  694 */ "126601, 126602, 126619, 126624, 126627, 126628, 126633, 126634, 126651, 131069, 173782, 173823",
      /*  706 */ "177972, 177983, 178205, 194559, 195101, 196605, 262141, 327677, 393213, 458749, 524285, 589821",
      /*  718 */ "655357, 720893, 786429, 851965, 917501, 917759, 917999, 983037, 1048573, 1114109, 1, 49, 1, 49, 1",
      /*  733 */ "49, 1, 49, 1, 49, 66, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1",
      /*  761 */ "66, 1, 66, 1, 49, 1, 49, 1, 66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49",
      /*  790 */ "1, 49, 1, 49, 1, 49, 1, 49, 1, 66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1",
      /*  819 */ "66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 66, 1, 66, 1, 66",
      /*  847 */ "49, 1, 49, 1, 49, 1, 66, 1, 66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 66, 1, 49, 66, 1, 66, 1, 66, 1",
      /*  876 */ "49, 1, 66, 1, 66, 1, 49, 1, 66, 1, 66, 49, 66, 1, 66, 1, 66, 1, 66, 1, 49, 1, 66, 1, 49, 1, 66, 1",
      /*  904 */ "49, 66, 1, 66, 1, 66, 1, 66, 1, 66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 66, 49, 1, 49, 1, 66, 1",
      /*  932 */ "66, 1, 66, 1, 66, 1, 66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49",
      /*  961 */ "1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1",
      /*  990 */ "49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 66, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49",
      /* 1019 */ "1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1",
      /* 1048 */ "49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 49, 1, 1",
      /* 1077 */ "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 66, 1, 1, 1"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1092; ++i) {MAP2[i] = Integer.parseInt(s2[i]);}
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

  private static final int[] TRANSITION = new int[2255];
  static
  {
    final String s1[] =
    {
      /*    0 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*   17 */ "2246, 1538, 1534, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*   34 */ "1116, 1088, 1102, 1104, 1094, 1112, 1124, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2178, 2246, 1317",
      /*   51 */ "1529, 1756, 2176, 1752, 1779, 1784, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1560",
      /*   68 */ "1136, 2226, 1141, 1810, 1815, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1849, 2246, 1179, 1587, 1845",
      /*   85 */ "1847, 1841, 1872, 1877, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 2246, 2246",
      /*  102 */ "2246, 1903, 1908, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1338, 2246, 1538, 1565, 1338, 1363, 2245",
      /*  119 */ "1359, 2243, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 2246, 1675, 1149, 2246",
      /*  136 */ "1947, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 2246, 1981, 1158, 1935, 1941",
      /*  153 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1384, 1167, 1388, 2246, 1386, 2246",
      /*  170 */ "2246, 2246, 2246, 2246, 2246, 2246, 1246, 2246, 1538, 1175, 1245, 1192, 1187, 1200, 1205, 2246, 2246",
      /*  187 */ "2246, 2246, 2246, 2246, 2246, 1256, 2246, 1538, 1219, 1258, 1226, 1233, 1880, 1231, 2246, 2246, 2246",
      /*  204 */ "2246, 2246, 2246, 2246, 1243, 2246, 1538, 1534, 2246, 1235, 1254, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  221 */ "2246, 2246, 2246, 2246, 2246, 1128, 1592, 2246, 1150, 1598, 2246, 1596, 2246, 2246, 2246, 2246, 2246",
      /*  238 */ "2246, 2246, 2246, 2246, 1538, 1475, 2004, 1480, 2010, 1410, 2008, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  255 */ "2246, 2246, 2246, 1912, 1952, 2246, 1159, 1958, 2246, 1956, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  272 */ "1389, 2246, 1538, 1534, 2246, 1389, 2037, 2246, 2035, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  289 */ "2246, 1538, 1534, 2246, 1723, 1700, 2246, 1698, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  306 */ "1538, 1534, 2246, 2246, 1600, 1266, 1271, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1243, 2246, 1538",
      /*  323 */ "1534, 1818, 1235, 1283, 1284, 1292, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1243, 2246, 1538, 1534",
      /*  340 */ "1818, 1235, 1304, 1305, 1313, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1243, 2246, 1538, 1534, 1818",
      /*  357 */ "1235, 1325, 1326, 1334, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1243, 2246, 1538, 1534, 1818, 1235",
      /*  374 */ "1346, 1347, 1355, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1243, 2246, 1538, 1534, 1818, 1235, 1371",
      /*  391 */ "1372, 1380, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1243, 2246, 1538, 1534, 1818, 1235, 1397, 1398",
      /*  408 */ "1406, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1418, 1419, 1427",
      /*  425 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1439, 1440, 1448, 2246",
      /*  442 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1461, 1462, 1470, 2246, 2246",
      /*  459 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1488, 1489, 1497, 2246, 2246, 2246",
      /*  476 */ "2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1515, 1516, 1524, 2246, 2246, 2246, 2246",
      /*  493 */ "2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1546, 1547, 1555, 2246, 2246, 2246, 2246, 2246",
      /*  510 */ "2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1573, 1574, 1582, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  527 */ "2246, 2246, 2246, 1538, 1534, 1818, 2246, 1608, 1609, 1617, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  544 */ "2246, 2246, 1538, 1534, 1818, 2246, 1660, 1661, 1669, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  561 */ "2246, 1538, 1534, 1818, 2246, 1683, 1684, 1692, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  578 */ "1538, 1534, 1818, 2246, 1708, 1709, 1717, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538",
      /*  595 */ "1534, 1818, 2246, 1731, 1732, 1740, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534",
      /*  612 */ "1818, 2246, 1764, 1765, 1773, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818",
      /*  629 */ "2246, 1795, 1796, 1804, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246",
      /*  646 */ "1826, 1827, 1835, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1857",
      /*  663 */ "1858, 1866, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1888, 1889",
      /*  680 */ "1897, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1920, 1921, 1929",
      /*  697 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1966, 1967, 1975, 2246",
      /*  714 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1534, 1818, 2246, 1989, 1990, 1998, 2246, 2246",
      /*  731 */ "2246, 2246, 2246, 2246, 2246, 2246, 2021, 1538, 1637, 1632, 2020, 2246, 2071, 2076, 2246, 2246, 2246",
      /*  748 */ "2246, 2246, 2246, 2246, 2246, 2246, 1538, 1453, 1211, 1275, 1209, 2246, 1746, 2246, 2246, 2246, 2246",
      /*  765 */ "2246, 2246, 2246, 2246, 2246, 1538, 1534, 2246, 2029, 2012, 2045, 2050, 2246, 2246, 2246, 2246, 2246",
      /*  782 */ "2246, 2246, 2246, 2246, 1538, 1534, 2246, 2246, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  799 */ "2246, 1243, 2246, 1538, 1534, 1818, 1235, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  816 */ "1243, 2065, 1538, 1534, 1818, 1235, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /*  833 */ "2246, 1538, 1534, 1818, 2246, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2101",
      /*  850 */ "1538, 1534, 1818, 2246, 2097, 2109, 2114, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1622, 1538",
      /*  867 */ "1534, 1818, 2246, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1296, 1538, 1534",
      /*  884 */ "1818, 2246, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 1787, 1538, 1534, 1818",
      /*  901 */ "2246, 2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2228, 1538, 1534, 1818, 2246",
      /*  918 */ "2058, 2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2247, 1538, 1534, 1818, 2246, 2058",
      /*  935 */ "2084, 2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2122, 1534, 1818, 2246, 2058, 2084",
      /*  952 */ "2089, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2155, 1534, 2162, 2246, 2058, 2084, 2089",
      /*  969 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2128, 1534, 1818, 2246, 2148, 2084, 2089, 2246",
      /*  986 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2211, 2186, 2197, 2199, 2189, 2207, 2219, 2246, 2246",
      /* 1003 */ "2246, 2246, 2246, 2246, 2246, 2246, 2246, 1538, 1502, 2134, 1507, 2140, 1431, 2138, 2246, 2246, 2246",
      /* 1020 */ "2246, 2246, 2246, 2246, 2246, 2246, 1538, 1627, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /* 1037 */ "2246, 2246, 2246, 2246, 2246, 1538, 1534, 1642, 2236, 2246, 1647, 1652, 2246, 2246, 2246, 2246, 2246",
      /* 1054 */ "2246, 2246, 2246, 2246, 1538, 1534, 2246, 2246, 1254, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /* 1071 */ "2246, 2169, 2246, 2246, 2246, 2246, 2246, 2246, 2169, 2246, 2246, 2246, 2246, 2246, 2246, 2246, 2246",
      /* 1088 */ "5956, 5956, 5956, 256, 0, 0, 5956, 0, 0, 5956, 0, 5956, 0, 5956, 5956, 5956, 5956, 0, 0, 5956, 5956",
      /* 1109 */ "5956, 5956, 5956, 5956, 5956, 0, 5956, 0, 0, 0, 5956, 512, 640, 0, 0, 5956, 5956, 5956, 5956, 0, 0",
      /* 1130 */ "0, 0, 512, 640, 0, 7680, 6656, 0, 0, 6656, 0, 0, 0, 6656, 0, 6656, 6656, 0, 6656, 837, 0, 0, 0, 0, 0",
      /* 1155 */ "0, 0, 7680, 7238, 0, 0, 0, 0, 0, 0, 0, 7936, 7296, 0, 7296, 7296, 7296, 7296, 7296, 7296, 7424, 0, 0",
      /* 1178 */ "256, 0, 0, 0, 0, 512, 6784, 6784, 0, 6016, 0, 0, 0, 0, 0, 7424, 0, 0, 0, 0, 7424, 0, 7424, 7424",
      /* 1202 */ "7424, 7424, 7424, 7424, 7424, 7424, 0, 0, 0, 0, 0, 4608, 0, 0, 4608, 4608, 0, 0, 7552, 0, 256, 0, 0",
      /* 1225 */ "0, 7552, 0, 7552, 7552, 7552, 7552, 7552, 7552, 0, 0, 0, 0, 0, 0, 0, 6144, 0, 6144, 0, 0, 0, 0, 0, 0",
      /* 1250 */ "0, 7424, 0, 0, 6016, 0, 0, 0, 0, 0, 0, 0, 7552, 0, 0, 0, 8192, 8192, 8192, 8192, 8192, 8192, 8192",
      /* 1273 */ "8192, 0, 0, 0, 0, 0, 4608, 4608, 4608, 0, 6016, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280",
      /* 1293 */ "1280, 1280, 0, 0, 0, 0, 0, 4992, 0, 0, 0, 6016, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408",
      /* 1314 */ "1408, 1408, 0, 0, 0, 0, 0, 6528, 640, 6528, 0, 6016, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536",
      /* 1334 */ "1536, 1536, 1536, 0, 0, 0, 0, 0, 7040, 0, 0, 0, 6016, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664",
      /* 1355 */ "1664, 1664, 1664, 0, 0, 0, 0, 0, 7040, 0, 7040, 7040, 7040, 7040, 7040, 7040, 6016, 1792, 1792, 1792",
      /* 1375 */ "1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 0, 0, 0, 0, 0, 7296, 0, 0, 0, 0, 0, 0, 0, 8064, 6016",
      /* 1398 */ "1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 0, 0, 0, 0, 0, 7808, 7808, 7808",
      /* 1417 */ "7808, 6016, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 0, 0, 0, 0, 0, 8960",
      /* 1436 */ "8960, 8960, 8960, 6016, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 0, 0, 0, 0",
      /* 1455 */ "0, 256, 0, 4608, 0, 0, 6016, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 0, 0",
      /* 1475 */ "0, 0, 0, 256, 0, 7808, 0, 7808, 7808, 7808, 7808, 7808, 7808, 6016, 2432, 2432, 2432, 2432, 2432",
      /* 1494 */ "2432, 2432, 2432, 2432, 2432, 2432, 0, 0, 0, 0, 0, 256, 0, 8960, 0, 8960, 8960, 8960, 8960, 8960",
      /* 1514 */ "8960, 6016, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 0, 0, 0, 0, 0, 256",
      /* 1533 */ "6528, 0, 0, 0, 256, 0, 0, 0, 0, 512, 640, 0, 0, 6016, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688",
      /* 1555 */ "2688, 2688, 2688, 0, 0, 0, 0, 0, 256, 6656, 0, 0, 0, 256, 0, 0, 0, 7040, 6016, 2816, 2816, 2816",
      /* 1577 */ "2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 0, 0, 0, 0, 0, 256, 6784, 0, 0, 0, 256, 0, 0, 7680",
      /* 1599 */ "0, 0, 0, 0, 0, 0, 0, 8192, 0, 6016, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944",
      /* 1620 */ "0, 0, 0, 0, 0, 4864, 0, 0, 0, 0, 5760, 0, 0, 0, 0, 8320, 0, 0, 0, 8320, 256, 0, 0, 0, 0, 9088, 0, 0",
      /* 1648 */ "0, 9088, 9088, 9088, 9088, 9088, 9088, 0, 0, 0, 0, 0, 6016, 3072, 3072, 3072, 3072, 3072, 3072, 3072",
      /* 1668 */ "3072, 3072, 3072, 3072, 0, 0, 0, 0, 0, 837, 837, 837, 837, 837, 837, 6016, 3200, 3200, 3200, 3200",
      /* 1688 */ "3200, 3200, 3200, 3200, 3200, 3200, 3200, 0, 0, 0, 0, 0, 1152, 0, 0, 0, 0, 0, 0, 0, 6016, 3328, 3328",
      /* 1711 */ "3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 0, 0, 0, 0, 0, 1152, 1152, 1152, 1152, 1152",
      /* 1730 */ "1152, 6016, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 0, 0, 0, 0, 0, 4608, 0",
      /* 1750 */ "0, 0, 0, 0, 6528, 0, 6528, 6528, 0, 6528, 0, 0, 0, 6528, 6016, 3584, 3584, 3584, 3584, 3584, 3584",
      /* 1771 */ "3584, 3584, 3584, 3584, 3584, 0, 0, 0, 0, 0, 6528, 6528, 6528, 6528, 6528, 6528, 0, 0, 0, 0, 0, 5120",
      /* 1793 */ "0, 0, 6016, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 0, 0, 0, 0, 0, 6656",
      /* 1813 */ "6656, 6656, 6656, 6656, 6656, 0, 0, 0, 0, 0, 6272, 0, 0, 6016, 3840, 3840, 3840, 3840, 3840, 3840",
      /* 1833 */ "3840, 3840, 3840, 3840, 3840, 0, 0, 0, 0, 0, 6784, 0, 6784, 6784, 0, 6784, 0, 0, 0, 6784, 0, 0, 0, 0",
      /* 1857 */ "6016, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 0, 0, 0, 0, 0, 6784, 6784",
      /* 1876 */ "6784, 6784, 6784, 6784, 0, 0, 0, 0, 0, 7552, 7552, 0, 6016, 4096, 4096, 4096, 4096, 4096, 4096, 4096",
      /* 1896 */ "4096, 4096, 4096, 4096, 0, 0, 0, 0, 0, 6912, 6912, 6912, 6912, 6912, 6912, 0, 0, 0, 0, 0, 512, 640",
      /* 1918 */ "0, 7936, 6016, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 0, 0, 0, 0, 0, 7168",
      /* 1938 */ "7168, 7168, 7168, 7168, 7168, 7238, 0, 0, 1024, 0, 0, 837, 0, 896, 0, 0, 0, 256, 0, 0, 7936, 0, 0, 0",
      /* 1962 */ "0, 0, 0, 0, 6016, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 0, 0, 0, 0, 0",
      /* 1983 */ "7238, 7238, 7238, 7238, 7238, 7238, 6016, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480",
      /* 2000 */ "4480, 0, 0, 0, 0, 0, 7808, 0, 7808, 7808, 7808, 0, 0, 0, 0, 0, 0, 0, 8448, 0, 0, 8320, 0, 0, 0, 0, 0",
      /* 2027 */ "0, 0, 0, 8448, 0, 0, 0, 0, 0, 0, 8064, 0, 0, 0, 0, 0, 0, 0, 8448, 8448, 8448, 8448, 8448, 8448, 8448",
      /* 2052 */ "8448, 0, 0, 0, 0, 0, 6016, 384, 0, 384, 0, 0, 384, 0, 4736, 0, 0, 0, 0, 0, 0, 8320, 8320, 8320, 8320",
      /* 2077 */ "8320, 8320, 0, 0, 0, 0, 0, 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 0, 0, 0, 6016, 8576, 0",
      /* 2100 */ "8576, 0, 0, 8576, 0, 0, 0, 0, 0, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 0, 0, 0, 0, 0, 5504",
      /* 2123 */ "0, 0, 0, 512, 640, 0, 0, 8704, 0, 512, 640, 0, 0, 8960, 0, 8960, 8960, 8960, 0, 0, 0, 0, 0, 0, 0",
      /* 2148 */ "8704, 384, 0, 384, 0, 0, 384, 0, 5632, 0, 0, 512, 640, 0, 0, 5632, 0, 0, 0, 6272, 0, 0, 6400, 0, 0",
      /* 2173 */ "0, 0, 0, 0, 6528, 0, 0, 6528, 0, 0, 0, 0, 0, 8832, 8832, 8832, 8832, 0, 0, 8832, 0, 8832, 0, 8832",
      /* 2197 */ "8832, 8832, 8832, 0, 0, 8832, 8832, 8832, 8832, 8832, 8832, 8832, 0, 8832, 0, 0, 0, 8832, 512, 640",
      /* 2217 */ "0, 0, 8832, 8832, 8832, 0, 0, 0, 0, 0, 6656, 0, 0, 0, 0, 0, 0, 5248, 0, 0, 9088, 0, 0, 0, 0, 0, 0",
      /* 2244 */ "7040, 7040, 0, 0, 0, 0, 0, 0, 0, 0, 5376"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2255; ++i) {TRANSITION[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] EXPECTED = new int[186];
  static
  {
    final String s1[] =
    {
      /*   0 */ "138, 138, 138, 138, 138, 136, 139, 138, 138, 138, 146, 148, 53, 62, 56, 56, 58, 74, 84, 66, 72, 78",
      /*  22 */ "82, 88, 92, 96, 100, 104, 108, 112, 116, 120, 124, 128, 132, 138, 138, 143, 138, 134, 138, 152, 182",
      /*  43 */ "155, 164, 162, 159, 168, 68, 172, 176, 179, 138, 480, -508, -512, -508, -508, -508, -508, -28, 0",
      /*  62 */ "-512, -512, -508, -512, 67108864, 1073741824, 0, 16, 6, 16, 0, 32, 64, 128, 32768, 131072, 256, 512",
      /*  80 */ "1024, 2048, 0, 8192, 262144, 1048576, 4194304, 33554432, 1310720, 671088640, 33562624, 67117056, 8192",
      /*  93 */ "4096, 1835008, 268435464, 671096832, 339738624, 1843200, 1320960, 268443656, 1835008, 356515840",
      /* 103 */ "268509192, 301998088, 1843200, 356524032, 35389440, 364904448, 364912640, 365174792, 365961224",
      /* 112 */ "398499848, 2109743104, 2109759488, 7, 1835015, 8199, 1835023, 1843207, -2113929209, 1843215",
      /* 122 */ "-2113921017, -2113789945, -2101608441, -2101600249, -1828978681, -1766064121, -1761869817",
      /* 129 */ "-1828970489, -1766055929, -1761861625, -253937, 8192, 0, 0, 8, 16, 0, 0, 0, 0, 2, 1, 0, 4, 0, 0, 480",
      /* 149 */ "480, 480, 480, 16, 16, 17, 16, 16, 48, 65, 48, 48, 48, 48, 67, 32, 48, 48, 81, 56, 4, 0, 20, 22, 22",
      /* 174 */ "71, 87, 103, 103, 103, 119, 119, 119, 0, 32, 16, 32"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 186; ++i) {EXPECTED[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] CASEID = new int[1749];
  static
  {
    final String s1[] =
    {
      /*    0 */ "975, 620, 585, 671, 589, 595, 598, 602, 694, 607, 612, 620, 615, 619, 624, 630, 631, 603, 631, 971",
      /*   20 */ "636, 639, 643, 715, 648, 595, 598, 602, 694, 653, 975, 620, 585, 671, 589, 658, 661, 665, 817, 669",
      /*   40 */ "631, 631, 644, 748, 813, 675, 678, 682, 631, 687, 975, 620, 585, 631, 692, 975, 620, 585, 631, 692",
      /*   60 */ "975, 620, 585, 631, 692, 636, 639, 643, 715, 648, 698, 701, 705, 709, 713, 631, 631, 727, 626, 971",
      /*   80 */ "965, 631, 649, 929, 1062, 719, 722, 726, 631, 731, 878, 631, 766, 955, 736, 631, 631, 683, 631, 971",
      /*  100 */ "975, 620, 585, 742, 624, 975, 620, 585, 631, 692, 1015, 631, 688, 992, 746, 631, 631, 803, 752, 971",
      /*  120 */ "758, 761, 765, 770, 774, 780, 783, 787, 631, 792, 878, 631, 766, 955, 736, 636, 639, 643, 797, 801",
      /*  140 */ "675, 678, 682, 631, 687, 968, 631, 727, 912, 903, 631, 631, 631, 807, 821, 975, 620, 585, 827, 624",
      /*  160 */ "631, 631, 727, 831, 971, 975, 620, 585, 837, 624, 975, 620, 585, 841, 848, 631, 631, 727, 1021, 903",
      /*  180 */ "975, 620, 585, 841, 848, 631, 631, 727, 1021, 903, 975, 620, 585, 841, 848, 631, 631, 727, 1021, 903",
      /*  200 */ "968, 631, 727, 912, 903, 631, 631, 727, 831, 915, 1059, 631, 732, 1120, 942, 631, 631, 631, 631, 854",
      /*  220 */ "631, 631, 793, 631, 631, 631, 631, 727, 631, 1024, 859, 862, 866, 871, 875, 631, 631, 631, 885, 631",
      /*  240 */ "758, 761, 765, 892, 774, 636, 639, 643, 896, 900, 636, 639, 643, 896, 900, 636, 639, 643, 896, 900",
      /*  260 */ "631, 631, 631, 885, 909, 921, 631, 631, 926, 631, 962, 631, 631, 1036, 631, 591, 620, 615, 831, 971",
      /*  280 */ "631, 631, 631, 631, 933, 758, 761, 765, 948, 774, 1018, 631, 788, 881, 989, 936, 631, 855, 1039",
      /*  299 */ "1012, 952, 631, 631, 959, 631, 1033, 631, 631, 1033, 631, 823, 979, 983, 996, 631, 631, 631, 631",
      /*  318 */ "631, 854, 968, 631, 727, 912, 903, 921, 631, 631, 1003, 631, 962, 631, 631, 1049, 631, 631, 631, 844",
      /*  338 */ "608, 903, 631, 631, 727, 815, 971, 921, 631, 631, 1030, 631, 962, 631, 631, 1046, 631, 968, 631, 727",
      /*  358 */ "912, 903, 631, 631, 683, 631, 971, 631, 631, 631, 1043, 631, 631, 631, 631, 1099, 631, 631, 631, 631",
      /*  378 */ "1056, 631, 591, 620, 585, 831, 971, 631, 631, 727, 831, 971, 631, 631, 632, 1064, 810, 823, 979",
      /*  397 */ "1068, 996, 631, 631, 631, 631, 1087, 631, 631, 631, 727, 973, 971, 631, 631, 844, 608, 903, 631, 631",
      /*  417 */ "631, 631, 1124, 631, 631, 1074, 631, 631, 631, 631, 631, 1131, 631, 631, 631, 1102, 631, 631, 631",
      /*  436 */ "631, 631, 754, 631, 631, 631, 631, 654, 631, 631, 631, 905, 631, 631, 631, 631, 1105, 631, 631, 631",
      /*  456 */ "631, 631, 776, 631, 631, 631, 631, 1052, 1108, 631, 631, 833, 631, 631, 631, 631, 631, 1127, 1134",
      /*  475 */ "1114, 631, 631, 888, 1117, 631, 631, 944, 631, 631, 631, 631, 631, 1141, 631, 631, 631, 631, 631",
      /*  494 */ "1145, 631, 631, 867, 631, 631, 631, 631, 850, 631, 631, 631, 631, 631, 1149, 631, 631, 631, 631",
      /*  513 */ "1071, 631, 1006, 631, 1153, 999, 1009, 631, 631, 631, 1111, 631, 631, 631, 939, 631, 631, 631, 631",
      /*  532 */ "1157, 917, 1077, 631, 631, 1161, 1165, 1080, 986, 631, 1169, 1173, 1090, 986, 631, 1169, 1083, 1090",
      /*  550 */ "631, 631, 1177, 922, 1093, 631, 631, 631, 1026, 631, 631, 631, 631, 1181, 631, 631, 631, 631, 1096",
      /*  569 */ "631, 631, 631, 631, 1185, 631, 631, 631, 631, 1137, 631, 631, 631, 631, 738, 631, 1190, 1224, 1224",
      /*  588 */ "1382, 1403, 1200, 1224, 1224, 1189, 1196, 1226, 1224, 1254, 1207, 1207, 1207, 1207, 1255, 1224, 1224",
      /*  605 */ "1224, 1219, 1426, 1224, 1224, 1224, 1223, 1221, 1189, 1196, 1196, 1224, 1224, 1382, 1402, 1196, 1196",
      /*  622 */ "1196, 1196, 1190, 1211, 1224, 1224, 1220, 1222, 1394, 1224, 1224, 1224, 1224, 1243, 1605, 1224, 1231",
      /*  639 */ "1236, 1236, 1236, 1236, 1232, 1224, 1224, 1224, 1269, 1248, 1224, 1224, 1224, 1296, 1251, 1224, 1224",
      /*  656 */ "1224, 1298, 1202, 1224, 1618, 1259, 1259, 1259, 1259, 1619, 1224, 1224, 1263, 1423, 1200, 1224, 1224",
      /*  673 */ "1222, 1220, 1416, 1224, 1275, 1280, 1280, 1280, 1280, 1276, 1224, 1224, 1224, 1332, 1416, 1224, 1224",
      /*  690 */ "1224, 1338, 1221, 1200, 1224, 1224, 1227, 1225, 1384, 1224, 1284, 1289, 1289, 1289, 1289, 1285, 1224",
      /*  707 */ "1224, 1263, 1222, 1224, 1385, 1387, 1293, 1200, 1224, 1224, 1240, 1271, 1474, 1224, 1314, 1319, 1319",
      /*  724 */ "1319, 1319, 1315, 1224, 1224, 1224, 1382, 1474, 1224, 1224, 1224, 1435, 1448, 1326, 1224, 1224, 1265",
      /*  741 */ "1224, 1402, 1191, 1403, 1401, 1673, 1446, 1224, 1224, 1310, 1269, 1384, 1386, 1224, 1224, 1328, 1585",
      /*  758 */ "1560, 1224, 1351, 1356, 1356, 1356, 1356, 1352, 1224, 1224, 1224, 1450, 1563, 1565, 1564, 1562, 1352",
      /*  775 */ "1564, 1224, 1224, 1334, 1224, 1510, 1224, 1360, 1365, 1365, 1365, 1365, 1361, 1224, 1224, 1224, 1502",
      /*  792 */ "1510, 1224, 1224, 1224, 1504, 1654, 1369, 1373, 1375, 1379, 1391, 1224, 1224, 1387, 1332, 1689, 1672",
      /*  809 */ "1407, 1224, 1244, 1224, 1224, 1270, 1224, 1224, 1222, 1224, 1203, 1201, 1414, 1571, 1224, 1224, 1538",
      /*  826 */ "1543, 1402, 1196, 1222, 1401, 1402, 1223, 1224, 1224, 1609, 1224, 1402, 1196, 1403, 1401, 1402, 1191",
      /*  843 */ "1222, 1220, 1224, 1224, 1382, 1190, 1420, 1224, 1224, 1646, 1224, 1454, 1224, 1224, 1224, 1515, 1577",
      /*  860 */ "1224, 1459, 1464, 1464, 1464, 1464, 1460, 1224, 1224, 1224, 1523, 1580, 1464, 1578, 1579, 1460, 1581",
      /*  877 */ "1224, 1224, 1324, 1327, 1224, 1214, 1501, 1503, 1689, 1672, 1224, 1224, 1343, 1633, 1635, 1563, 1356",
      /*  894 */ "1564, 1562, 1654, 1468, 1472, 1271, 1379, 1648, 1224, 1224, 1400, 1224, 1224, 1485, 1224, 1478, 1571",
      /*  911 */ "1224, 1224, 1402, 1192, 1223, 1420, 1224, 1224, 1668, 1678, 1497, 1224, 1224, 1224, 1713, 1715, 1224",
      /*  928 */ "1224, 1224, 1409, 1302, 1304, 1496, 1224, 1224, 1224, 1430, 1516, 1224, 1437, 1224, 1224, 1441, 1224",
      /*  945 */ "1224, 1695, 1224, 1563, 1356, 1561, 1562, 1479, 1224, 1224, 1224, 1449, 1323, 1325, 1480, 1224, 1224",
      /*  962 */ "1224, 1484, 1224, 1224, 1410, 1309, 1224, 1189, 1223, 1224, 1200, 1224, 1224, 1221, 1224, 1189, 1196",
      /*  979 */ "1543, 1543, 1543, 1543, 1544, 1224, 1224, 1224, 1492, 1701, 1224, 1508, 1224, 1224, 1443, 1347, 1445",
      /*  996 */ "1396, 1548, 1224, 1224, 1518, 1520, 1522, 1721, 1224, 1224, 1224, 1519, 1522, 1224, 1521, 1224, 1224",
      /* 1013 */ "1527, 1224, 1224, 1444, 1447, 1224, 1215, 1503, 1224, 1221, 1192, 1223, 1200, 1224, 1224, 1725, 1224",
      /* 1030 */ "1727, 1224, 1224, 1224, 1533, 1224, 1224, 1489, 1224, 1224, 1429, 1514, 1516, 1733, 1224, 1224, 1224",
      /* 1047 */ "1558, 1224, 1224, 1553, 1224, 1224, 1529, 1529, 1597, 1670, 1672, 1224, 1224, 1642, 1436, 1224, 1308",
      /* 1064 */ "1224, 1224, 1242, 1224, 1539, 1224, 1224, 1224, 1652, 1224, 1224, 1664, 1224, 1224, 1677, 1224, 1224",
      /* 1081 */ "1693, 1224, 1224, 1705, 1699, 1701, 1599, 1569, 1224, 1224, 1707, 1224, 1224, 1719, 1224, 1224, 1731",
      /* 1098 */ "1224, 1224, 1735, 1224, 1224, 1575, 1224, 1224, 1591, 1224, 1224, 1603, 1224, 1224, 1611, 1224, 1224",
      /* 1115 */ "1623, 1627, 1224, 1634, 1224, 1224, 1641, 1434, 1436, 1455, 1224, 1224, 1224, 1737, 1615, 1739, 1341",
      /* 1132 */ "1224, 1224, 1224, 1738, 1224, 1224, 1743, 1224, 1224, 1587, 1639, 1224, 1224, 1709, 1224, 1224, 1224",
      /* 1149 */ "1593, 1224, 1224, 1224, 1517, 1224, 1224, 1658, 1549, 1224, 1224, 1662, 1554, 1224, 1224, 1682, 1687",
      /* 1166 */ "1224, 1224, 1683, 1490, 1224, 1224, 1700, 1593, 1491, 1699, 1701, 1534, 1224, 1224, 1224, 1629, 1224",
      /* 1183 */ "1224, 1224, 1745, 1224, 1224, 1224, 0, 65814, 65814, 65814, 0, 65814, 0, 65814, 65814, 65814, 65814",
      /* 1200 */ "25250, 0, 0, 0, 49156, 0, 0, 4, 4, 4, 4, 25250, 65814, 65814, 0, 0, 1163268, 1163268, 1163268, 99162",
      /* 1220 */ "0, 0, 0, 65814, 0, 0, 0, 0, 4, 0, 0, 0, 573444, 573444, 573444, 0, 573444, 573444, 573444, 573444, 0",
      /* 1241 */ "98322, 0, 0, 131076, 0, 0, 0, 0, 90130, 573444, 0, 4, 4, 0, 4, 4, 4, 0, 49156, 49156, 49156, 49156",
      /* 1263 */ "0, 115010, 0, 0, 147720, 0, 0, 163844, 0, 0, 0, 81938, 0, 229380, 229380, 229380, 0, 229380, 229380",
      /* 1282 */ "229380, 229380, 0, 82438, 82438, 82438, 0, 82438, 82438, 82438, 82438, 0, 82438, 82438, 0, 16398, 0",
      /* 1299 */ "0, 172050, 0, 196644, 0, 196644, 196644, 65814, 0, 32782, 196644, 0, 0, 0, 163844, 0, 262152, 262152",
      /* 1317 */ "262152, 0, 262152, 262152, 262152, 262152, 212996, 0, 212996, 212996, 212996, 0, 0, 0, 163858, 0",
      /* 1333 */ "188962, 0, 0, 180328, 0, 0, 246790, 278536, 0, 16634, 0, 0, 376852, 303122, 246790, 0, 49166, 246790",
      /* 1351 */ "0, 344068, 344068, 344068, 0, 344068, 344068, 344068, 344068, 0, 294916, 294916, 294916, 0, 294916",
      /* 1366 */ "294916, 294916, 294916, 638980, 253970, 360468, 262162, 0, 65550, 360468, 0, 0, 81938, 638980, 81934",
      /* 1381 */ "573444, 0, 16690, 0, 0, 82438, 0, 0, 0, 82438, 0, 360468, 638980, 0, 25314, 0, 0, 98318, 362546",
      /* 1400 */ "25250, 65814, 0, 0, 65814, 65814, 0, 0, 704520, 0, 0, 196644, 196644, 196644, 868356, 720904, 0, 0",
      /* 1418 */ "229380, 0, 25250, 0, 65814, 0, 49156, 49156, 0, 4, 14, 0, 0, 1146884, 1146884, 1146884, 622596, 0",
      /* 1436 */ "622596, 0, 0, 0, 589842, 622596, 622596, 0, 0, 246790, 246790, 246790, 0, 0, 0, 212996, 212996, 0",
      /* 1454 */ "452754, 0, 0, 0, 688146, 0, 393220, 393220, 393220, 0, 393220, 393220, 393220, 393220, 638980",
      /* 1469 */ "253970, 0, 262162, 0, 65550, 0, 0, 262152, 0, 868356, 0, 0, 0, 737284, 0, 802824, 0, 0, 0, 737298",
      /* 1489 */ "837538, 0, 0, 0, 771926, 771926, 771926, 901124, 0, 0, 0, 786440, 1163268, 0, 1163268, 0, 0, 0",
      /* 1507 */ "819204, 1163268, 1163268, 0, 0, 294916, 0, 1146884, 0, 1146884, 0, 0, 0, 837766, 837766, 837766, 0",
      /* 1524 */ "0, 0, 845922, 1146884, 1146884, 0, 0, 328790, 0, 737284, 0, 0, 0, 936362, 0, 1130504, 1130504",
      /* 1541 */ "1130504, 0, 1130504, 1130504, 1130504, 1130504, 936362, 114702, 0, 0, 0, 983060, 894898, 0, 0, 0",
      /* 1557 */ "1114132, 928402, 0, 0, 0, 344068, 0, 0, 344068, 344068, 0, 344068, 576098, 0, 0, 0, 368658, 0, 0",
      /* 1576 */ "155666, 0, 0, 393220, 0, 0, 393220, 393220, 0, 0, 163858, 0, 0, 435858, 0, 761874, 0, 0, 0, 493282",
      /* 1596 */ "0, 271458, 0, 0, 0, 567762, 362546, 0, 271474, 0, 0, 573444, 0, 0, 786450, 0, 0, 581650, 0, 279858",
      /* 1616 */ "0, 361766, 0, 49156, 49156, 49156, 0, 0, 286738, 294930, 311314, 319506, 0, 0, 0, 600594, 0, 376852",
      /* 1634 */ "0, 376852, 0, 0, 0, 444274, 0, 0, 0, 622596, 622596, 622596, 860178, 0, 0, 0, 638980, 0, 501698, 0",
      /* 1654 */ "0, 0, 638980, 638980, 0, 837766, 0, 845922, 0, 131086, 0, 0, 704530, 0, 0, 1081364, 0, 0, 753668, 0",
      /* 1674 */ "0, 0, 278536, 131086, 983060, 0, 0, 0, 0, 1114132, 0, 0, 0, 1097752, 0, 0, 0, 753668, 362546",
      /* 1693 */ "1114132, 1114132, 0, 0, 802834, 0, 771926, 0, 771926, 0, 0, 0, 501698, 0, 771926, 771926, 0, 0",
      /* 1711 */ "819218, 0, 624930, 0, 0, 0, 829122, 0, 0, 624946, 0, 0, 886482, 0, 0, 630802, 0, 0, 920066, 0",
      /* 1731 */ "608930, 0, 0, 0, 1064964, 0, 0, 0, 361766, 0, 0, 0, 1084106, 0, 0, 0, 1083962, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1749; ++i) {CASEID[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TOKENSET = new int[117];
  static
  {
    final String s1[] =
    {
      /*   0 */ "56, 54, 66, 27, 54, 54, 56, 57, 30, 49, 51, 51, 51, 54, 57, 30, 47, 49, 48, 19, 64, 51, 48, 33, 61",
      /*  25 */ "49, 48, 62, 49, 43, 41, 63, 32, 65, 59, 40, 59, 40, 59, 40, 43, 39, 43, 8, 0, 26, 60, 28, 62, 58, 58",
      /*  51 */ "58, 35, 20, 21, 55, 8, 60, 43, 43, 20, 21, 52, 8, 43, 20, 21, 34, 24, 20, 21, 43, 19, 2, 3, 22, 53",
      /*  77 */ "32, 25, 50, 28, 25, 34, 18, 12, 1, 11, 23, 7, 17, 9, 6, 31, 15, 36, 42, 16, 22, 10, 0, 14, 2, 3, 46",
      /* 104 */ "4, 13, 38, 37, 44, 45, 29, 5, 2, 3, 2, 3, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 117; ++i) {TOKENSET[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] APPENDIX = new int[18];
  static
  {
    final String s1[] =
    {
      /*  0 */ "2, 339977, 8345, 98322, 12625, 98322, 123395, 139268, 49161, 319490, 45065, 319490, 283881, 376834",
      /* 14 */ "288049, 376834, 491530, 540682"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 18; ++i) {APPENDIX[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] LOOKBACK = new int[552];
  static
  {
    final String s1[] =
    {
      /*   0 */ "175, 175, 175, 175, 175, 173, 176, 181, 181, 181, 184, 194, 194, 194, 189, 197, 200, 203, 212, 212",
      /*  20 */ "221, 212, 221, 212, 221, 234, 234, 243, 234, 243, 234, 243, 256, 265, 265, 265, 265, 274, 274, 274",
      /*  40 */ "274, 175, 175, 175, 175, 283, 283, 283, 283, 290, 290, 290, 290, 297, 304, 175, 307, 310, 310, 310",
      /*  60 */ "313, 323, 323, 323, 318, 175, 175, 175, 175, 326, 331, 336, 341, 346, 346, 346, 346, 175, 175, 349",
      /*  80 */ "349, 349, 352, 357, 362, 365, 365, 365, 175, 175, 175, 175, 368, 381, 381, 381, 371, 376, 389, 384",
      /* 100 */ "392, 175, 175, 175, 395, 395, 395, 401, 406, 411, 421, 416, 416, 416, 416, 416, 426, 429, 398, 398",
      /* 120 */ "398, 434, 439, 444, 454, 449, 449, 449, 449, 449, 459, 175, 462, 465, 465, 465, 470, 175, 473, 473",
      /* 140 */ "473, 476, 486, 486, 486, 486, 489, 494, 499, 504, 507, 507, 507, 507, 479, 510, 175, 175, 175, 513",
      /* 160 */ "516, 524, 519, 527, 530, 175, 175, 175, 535, 538, 546, 541, 549, 3, 2, 0, 5, 5, 3, 4, 0, 6, 6, 0, 12",
      /* 185 */ "11, 8, 7, 0, 12, 13, 8, 9, 0, 10, 10, 0, 14, 14, 0, 15, 15, 0, 27, 25, 26, 25, 20, 18, 19, 18, 0, 27",
      /* 213 */ "28, 26, 28, 20, 21, 19, 21, 0, 38, 39, 34, 35, 27, 29, 26, 28, 20, 22, 19, 21, 0, 27, 30, 26, 30, 20",
      /* 239 */ "23, 19, 23, 0, 38, 40, 34, 36, 27, 31, 26, 30, 20, 24, 19, 23, 0, 38, 37, 34, 33, 27, 37, 20, 33, 0",
      /* 265 */ "38, 39, 34, 35, 27, 39, 20, 35, 0, 38, 40, 34, 36, 27, 40, 20, 36, 0, 50, 49, 46, 45, 42, 41, 0, 50",
      /* 291 */ "51, 46, 47, 42, 43, 0, 50, 52, 46, 48, 42, 44, 0, 53, 53, 0, 55, 55, 0, 56, 56, 0, 62, 61, 58, 57, 0",
      /* 318 */ "62, 63, 58, 59, 0, 60, 60, 0, 74, 73, 66, 65, 0, 74, 75, 66, 67, 0, 74, 76, 66, 68, 0, 71, 71, 70, 70",
      /* 345 */ "0, 72, 72, 0, 77, 78, 0, 86, 85, 80, 79, 0, 86, 87, 80, 81, 0, 83, 83, 0, 84, 84, 0, 89, 88, 0, 94",
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

  private static final int[] GOTO = new int[918];
  static
  {
    final String s1[] =
    {
      /*   0 */ "332, 399, 399, 399, 477, 399, 497, 399, 465, 399, 399, 399, 407, 399, 399, 399, 462, 486, 399, 399",
      /*  20 */ "399, 399, 497, 399, 338, 399, 399, 399, 344, 399, 399, 399, 399, 399, 414, 399, 376, 399, 425, 399",
      /*  40 */ "350, 399, 399, 399, 356, 399, 399, 399, 362, 399, 399, 399, 474, 486, 399, 399, 495, 399, 399, 399",
      /*  60 */ "368, 399, 399, 399, 374, 399, 399, 399, 420, 399, 399, 399, 399, 399, 400, 399, 380, 399, 399, 399",
      /*  80 */ "386, 399, 399, 399, 392, 399, 399, 399, 382, 399, 399, 399, 504, 399, 399, 399, 525, 399, 399, 399",
      /* 100 */ "549, 399, 399, 398, 399, 399, 520, 399, 388, 404, 334, 413, 506, 399, 425, 399, 418, 399, 399, 399",
      /* 120 */ "399, 515, 424, 513, 429, 399, 399, 399, 435, 399, 399, 399, 441, 399, 399, 399, 447, 399, 399, 399",
      /* 140 */ "453, 399, 399, 399, 459, 399, 399, 399, 471, 399, 399, 399, 483, 399, 399, 399, 492, 399, 399, 399",
      /* 160 */ "501, 399, 399, 399, 510, 399, 399, 399, 399, 399, 399, 519, 399, 399, 524, 399, 399, 399, 399, 532",
      /* 180 */ "529, 399, 399, 399, 399, 399, 399, 548, 399, 394, 399, 535, 574, 399, 399, 399, 388, 553, 334, 413",
      /* 200 */ "388, 557, 334, 413, 388, 561, 334, 413, 399, 431, 424, 513, 399, 409, 399, 399, 399, 488, 399, 399",
      /* 220 */ "565, 399, 399, 399, 399, 399, 399, 437, 340, 399, 399, 399, 399, 399, 399, 467, 399, 399, 399, 479",
      /* 240 */ "399, 399, 399, 538, 399, 399, 399, 541, 399, 370, 569, 535, 399, 399, 573, 399, 578, 399, 399, 399",
      /* 260 */ "399, 409, 399, 399, 399, 488, 399, 399, 582, 399, 399, 399, 586, 399, 399, 399, 399, 346, 399, 399",
      /* 280 */ "399, 352, 399, 399, 590, 399, 399, 399, 594, 399, 399, 399, 399, 399, 399, 449, 399, 399, 399, 455",
      /* 300 */ "399, 443, 399, 535, 598, 399, 399, 399, 602, 399, 399, 399, 399, 399, 358, 399, 399, 370, 606, 535",
      /* 320 */ "399, 364, 544, 399, 610, 399, 399, 399, 614, 399, 399, 399, 618, 632, 633, 633, 624, 633, 714, 632",
      /* 340 */ "633, 633, 633, 824, 703, 707, 633, 633, 633, 850, 668, 632, 633, 633, 633, 855, 665, 632, 633, 633",
      /* 360 */ "633, 873, 662, 632, 633, 633, 633, 878, 884, 632, 633, 633, 699, 893, 887, 632, 633, 633, 718, 633",
      /* 380 */ "874, 724, 633, 633, 734, 633, 767, 632, 633, 633, 743, 633, 777, 632, 633, 633, 744, 650, 738, 633",
      /* 400 */ "633, 633, 633, 708, 748, 751, 626, 633, 687, 633, 633, 633, 808, 815, 633, 633, 633, 712, 671, 632",
      /* 420 */ "633, 633, 757, 633, 765, 633, 633, 633, 720, 830, 632, 633, 633, 804, 683, 836, 632, 633, 633, 819",
      /* 440 */ "633, 846, 632, 633, 633, 820, 633, 857, 632, 633, 633, 861, 633, 647, 632, 633, 633, 867, 633, 863",
      /* 460 */ "632, 633, 633, 688, 692, 633, 632, 633, 633, 628, 633, 653, 632, 633, 633, 693, 692, 633, 638, 633",
      /* 480 */ "633, 627, 633, 621, 632, 633, 633, 697, 633, 633, 633, 813, 656, 632, 633, 633, 707, 633, 633, 634",
      /* 500 */ "633, 680, 632, 633, 633, 724, 633, 633, 755, 633, 641, 632, 633, 633, 771, 633, 633, 761, 683, 739",
      /* 520 */ "633, 633, 633, 725, 896, 633, 633, 633, 729, 644, 632, 633, 633, 890, 633, 633, 781, 633, 633, 828",
      /* 540 */ "633, 633, 834, 633, 633, 914, 633, 633, 775, 633, 633, 633, 733, 786, 751, 626, 633, 790, 796, 626",
      /* 560 */ "633, 790, 800, 626, 633, 659, 632, 633, 633, 782, 840, 844, 633, 908, 633, 633, 633, 792, 881, 632",
      /* 580 */ "633, 633, 899, 632, 633, 633, 905, 632, 633, 633, 869, 632, 633, 633, 809, 724, 633, 633, 674, 632",
      /* 600 */ "633, 633, 677, 632, 633, 633, 851, 840, 844, 633, 911, 632, 633, 633, 902, 632, 633, 633, 6, 0, 4121",
      /* 621 */ "0, 0, 208905, 0, 0, 212996, 0, 0, 0, 262153, 0, 8401, 0, 0, 0, 0, 16433, 0, 20521, 24585, 0, 0",
      /* 643 */ "213745, 0, 0, 229385, 0, 0, 229396, 0, 0, 241673, 0, 0, 245780, 0, 0, 253972, 0, 0, 255065, 0, 0",
      /* 664 */ "262156, 0, 0, 270348, 0, 0, 278540, 0, 0, 303116, 0, 0, 324769, 0, 0, 327689, 0, 0, 328477, 0, 0",
      /* 685 */ "335876, 172809, 12689, 0, 0, 0, 28737, 32777, 0, 0, 0, 28769, 0, 36873, 0, 0, 0, 499716, 0, 53329",
      /* 705 */ "348273, 16388, 57561, 0, 0, 0, 90577, 0, 61449, 0, 0, 57356, 0, 0, 65545, 0, 0, 70073, 0, 94513, 0, 0",
      /* 727 */ "0, 90609, 0, 372745, 111209, 377361, 98348, 0, 0, 0, 106977, 114697, 0, 0, 0, 164625, 118793, 0, 0, 0",
      /* 747 */ "237577, 385665, 389129, 188420, 188420, 36873, 0, 212996, 0, 155660, 0, 0, 74177, 0, 0, 164609",
      /* 763 */ "168673, 344068, 425988, 425988, 0, 0, 98313, 0, 398129, 0, 0, 176137, 0, 233481, 0, 0, 102409, 0",
      /* 781 */ "398129, 0, 0, 0, 275561, 385713, 389129, 188420, 188420, 0, 294916, 0, 0, 111209, 377425, 0, 36873",
      /* 798 */ "221212, 212996, 0, 36873, 237596, 212996, 0, 164609, 0, 344068, 246617, 0, 0, 0, 307209, 0, 250825, 0",
      /* 816 */ "0, 123593, 0, 258057, 0, 0, 0, 319497, 0, 425993, 111209, 377361, 0, 267137, 0, 0, 188425, 0, 0",
      /* 835 */ "271345, 0, 0, 192521, 0, 483332, 278537, 0, 516100, 475140, 438281, 0, 0, 197185, 0, 300281, 0, 0, 0",
      /* 854 */ "337065, 0, 304449, 0, 0, 201377, 0, 0, 468241, 0, 0, 204809, 0, 0, 472409, 0, 0, 204844, 0, 331785, 0",
      /* 875 */ "0, 0, 364553, 0, 0, 540676, 0, 0, 589860, 0, 0, 356361, 0, 0, 360457, 0, 0, 406561, 0, 0, 434185, 0",
      /* 897 */ "0, 442380, 0, 0, 451705, 0, 0, 451769, 0, 0, 454665, 0, 0, 458780, 0, 0, 475145, 0, 0, 507940, 524292"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 918; ++i) {GOTO[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] REDUCTION = new int[146];
  static
  {
    final String s1[] =
    {
      /*   0 */ "43, 0, 0, -1, 1, -1, 1, 1, 2, -1, 3, -1, 4, -1, 5, -1, 44, 2, 6, -1, 45, 3, 7, 4, 8, -1, 47, 6, 46, 5",
      /*  30 */ "9, 7, 10, -1, 11, -1, 48, 8, 12, 9, 13, -1, 14, 10, 15, -1, 16, -1, 49, 11, 17, 12, 17, -1, 18, 14",
      /*  56 */ "18, 13, 19, 16, 19, 15, 20, 17, 21, 20, 21, 19, 21, 18, 21, 21, 22, -1, 23, 22, 51, 24, 50, 23, 24",
      /*  81 */ "-1, 25, 26, 25, 25, 26, 27, 26, -1, 53, 29, 52, 28, 27, -1, 28, 30, 29, 30, 54, 31, 30, 32, 31, -1",
      /* 106 */ "55, 33, 32, -1, 56, 34, 33, -1, 34, -1, 35, 38, 35, 37, 35, 36, 35, 35, 36, -1, 37, 39, 38, 40, 57",
      /* 131 */ "41, 39, -1, 40, 42, 40, -1, 41, 43, 59, 45, 58, 44, 42, -1"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 146; ++i) {REDUCTION[i] = Integer.parseInt(s2[i]);}
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
    "'>'",
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
    "naming",
    "name",
    "namestart",
    "namefollower",
    "alias",
    "alts",
    "addAlt",
    "alt",
    "term",
    "factor",
    "repeat0",
    "repeat1",
    "option",
    "mark",
    "sep",
    "nonterminal",
    "terminal",
    "literal",
    "tmark",
    "string",
    "dchar",
    "schar",
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
    "capital",
    "insertion",
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
    "IMPLICIT-56",
    "IMPLICIT-57",
    "IMPLICIT-58",
    "IMPLICIT-59"
  };

                                                            // line 962 "ixml.ebnf"
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
                                                              private String name;
                                                              private String alias;
                                                              private String savedName;
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

                                                              private static void validateStringChar(int codepoint) {
                                                                if (codepoint >= 0x00 && codepoint <= 0x1f
                                                                 || codepoint >= 0x7f && codepoint <= 0x9f)
                                                                 de.bottlecaps.markup.blitz.Errors.S11.thro("#" + Integer.toHexString(codepoint));
                                                              }
                                                            }
                                                            // line 1785 "Ixml.java"
// End
