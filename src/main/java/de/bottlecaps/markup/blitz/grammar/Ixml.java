// This file was generated on Fri Jan 9, 2026 20:34 (UTC+01) by REx v6.2-SNAPSHOT which is Copyright (c) 1979-2025 by Gunther Rademacher <grd@gmx.net>
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
                                                            // line 3 "ixml.ebnf"
                                                            if (grammar == null) grammar = new Grammar((String) null);
                                                            // line 485 "Ixml.java"
        }
        break;
      case 1:
        {
                                                            // line 5 "ixml.ebnf"
                                                            de.bottlecaps.markup.blitz.Errors.S01.thro();
                                                            // line 492 "Ixml.java"
        }
        break;
      case 2:
        {
                                                            // line 14 "ixml.ebnf"
                                                            grammar = new Grammar(stringBuilder.toString());
                                                            // line 499 "Ixml.java"
        }
        break;
      case 3:
        {
                                                            // line 18 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            grammar.addRule(new Rule(mark, alias, name, alts.peek()));
                                                            // line 507 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 21 "ixml.ebnf"
                                                            alts.pop();
                                                            // line 514 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 23 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 521 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 24 "ixml.ebnf"
                                                            nameBuilder.append(input.subSequence(b0, e0));
                                                            // line 528 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 27 "ixml.ebnf"
                                                            name = nameBuilder.toString();
                                                            alias = null;
                                                            // line 536 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 39 "ixml.ebnf"
                                                            if (! grammar.getVersion().isAtLeast(Grammar.Version.V1_1))
                                                              de.bottlecaps.markup.blitz.Errors.S12.thro(grammar.getVersion() + " does not support renaming");
                                                            savedName = name;
                                                            // line 545 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 44 "ixml.ebnf"
                                                            alias = name;
                                                            name = savedName;
                                                            // line 553 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 48 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 560 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 57 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 567 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 59 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 575 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 63 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 583 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 67 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 592 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 73 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 600 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 77 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 609 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 83 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 617 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 86 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 624 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 87 "ixml.ebnf"
                                                            mark = Mark.NODE;
                                                            // line 631 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 88 "ixml.ebnf"
                                                            mark = Mark.DELETE;
                                                            // line 638 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 89 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 645 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 92 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, alias, name);
                                                            // line 652 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 93 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 659 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 96 "ixml.ebnf"
                                                            alts.peek().last().addCharset(new Charset(deleted, exclusion, members));
                                                            // line 666 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 99 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 673 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 100 "ixml.ebnf"
                                                            alts.peek().last().addCodepoint(deleted, codepoint);
                                                            // line 680 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 102 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 687 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 103 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 694 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 104 "ixml.ebnf"
                                                            stringBuilder.append(input.subSequence(b0, e0));
                                                            // line 701 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 109 "ixml.ebnf"
                                                            validateStringChar(input.toString().codePointAt(b0));
                                                            // line 708 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 111 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 715 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 112 "ixml.ebnf"
                                                            codepoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 722 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 117 "ixml.ebnf"
                                                            exclusion = false;
                                                            members = new java.util.ArrayList<>();
                                                            // line 730 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 122 "ixml.ebnf"
                                                            exclusion = true;
                                                            members = new java.util.ArrayList<>();
                                                            // line 738 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 127 "ixml.ebnf"
                                                            members.add(new StringMember(stringBuilder.toString(), false));
                                                            // line 745 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 128 "ixml.ebnf"
                                                            members.add(new StringMember(codepoint, true));
                                                            // line 752 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 129 "ixml.ebnf"
                                                            members.add(new RangeMember(firstCodepoint, lastCodepoint));
                                                            // line 759 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 130 "ixml.ebnf"
                                                            members.add(new ClassMember(clazz));
                                                            // line 766 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 132 "ixml.ebnf"
                                                            firstCodepoint = codepoint;
                                                            // line 773 "Ixml.java"
        }
        break;
      case 40:
        {
                                                            // line 133 "ixml.ebnf"
                                                            lastCodepoint = codepoint;
                                                            // line 780 "Ixml.java"
        }
        break;
      case 41:
        {
                                                            // line 134 "ixml.ebnf"
                                                            codepoint = input.subSequence(b0, e0).toString();
                                                            // line 787 "Ixml.java"
        }
        break;
      case 42:
        {
                                                            // line 139 "ixml.ebnf"
                                                            clazz += input.subSequence(b0, e0);
                                                            // line 794 "Ixml.java"
        }
        break;
      case 43:
        {
                                                            // line 145 "ixml.ebnf"
                                                            clazz = input.subSequence(b0, e0).toString();
                                                            // line 801 "Ixml.java"
        }
        break;
      case 44:
        {
                                                            // line 146 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 808 "Ixml.java"
        }
        break;
      case 45:
        {
                                                            // line 147 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codepoint);
                                                            // line 815 "Ixml.java"
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

  private static final int[] TRANSITION = new int[2133];
  static
  {
    final String s1[] =
    {
      /*    0 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*   17 */ "2092, 1594, 1590, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*   34 */ "1701, 1088, 1117, 1119, 1693, 1697, 1127, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1166, 2092, 1426",
      /*   51 */ "1585, 1139, 1165, 1160, 1174, 1179, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1612",
      /*   68 */ "1193, 2091, 1224, 2084, 2089, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1254, 2092, 1243, 1635, 1251",
      /*   85 */ "1253, 1262, 1280, 1285, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 2092, 2092",
      /*  102 */ "2092, 1101, 1106, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1443, 2092, 1594, 1617, 1444, 1296, 2092",
      /*  119 */ "1461, 1304, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 2092, 2054, 2092, 2092",
      /*  136 */ "1230, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 2092, 1315, 2092, 1987, 1993",
      /*  153 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1719, 1323, 2092, 2092, 1724, 2092",
      /*  170 */ "2092, 2092, 2092, 2092, 2092, 2092, 1647, 2092, 1594, 1331, 1647, 1343, 1216, 1351, 1356, 2092, 2092",
      /*  187 */ "2092, 2092, 2092, 2092, 2092, 1182, 2092, 1594, 1367, 1185, 1373, 2092, 1401, 1378, 2092, 2092, 2092",
      /*  204 */ "2092, 2092, 2092, 2092, 1389, 2092, 1594, 1590, 2092, 1381, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  221 */ "2092, 2092, 2092, 2092, 2092, 1131, 1640, 2092, 1288, 2092, 2092, 1644, 2092, 2092, 2092, 2092, 2092",
      /*  238 */ "2092, 2092, 2092, 2092, 1594, 1541, 1545, 1481, 2092, 1478, 1398, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  255 */ "2092, 2092, 2092, 1335, 1235, 2092, 1109, 2092, 2092, 1239, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  272 */ "2115, 2092, 1594, 1590, 2092, 2116, 2092, 2092, 2016, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  289 */ "2092, 1594, 1590, 2092, 1409, 2092, 2092, 1268, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  306 */ "1594, 1590, 2092, 2092, 1307, 1417, 1422, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1389, 2092, 1594",
      /*  323 */ "1590, 1272, 1381, 1434, 1434, 1439, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1389, 2092, 1594, 1590",
      /*  340 */ "1272, 1381, 1452, 1452, 1457, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1389, 2092, 1594, 1590, 1272",
      /*  357 */ "1381, 1469, 1469, 1474, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1389, 2092, 1594, 1590, 1272, 1381",
      /*  374 */ "1489, 1489, 1494, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1389, 2092, 1594, 1590, 1272, 1381, 1509",
      /*  391 */ "1509, 1514, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1389, 2092, 1594, 1590, 1272, 1381, 1531, 1531",
      /*  408 */ "1536, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1553, 1553, 1558",
      /*  425 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1575, 1575, 1580, 2092",
      /*  442 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1602, 1602, 1607, 2092, 2092",
      /*  459 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1625, 1625, 1630, 2092, 2092, 2092",
      /*  476 */ "2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1655, 1655, 1660, 2092, 2092, 2092, 2092",
      /*  493 */ "2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1673, 1673, 1678, 2092, 2092, 2092, 2092, 2092",
      /*  510 */ "2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1709, 1709, 1714, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  527 */ "2092, 2092, 2092, 1594, 1590, 1272, 1390, 1735, 1735, 1740, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  544 */ "2092, 2092, 1594, 1590, 1272, 1390, 1761, 1761, 1766, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  561 */ "2092, 1594, 1590, 1272, 1390, 1774, 1774, 1779, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  578 */ "1594, 1590, 1272, 1390, 1787, 1787, 1792, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594",
      /*  595 */ "1590, 1272, 1390, 1800, 1800, 1805, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590",
      /*  612 */ "1272, 1390, 1813, 1813, 1818, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272",
      /*  629 */ "1390, 1826, 1826, 1831, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390",
      /*  646 */ "1839, 1839, 1844, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1852",
      /*  663 */ "1852, 1857, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1865, 1865",
      /*  680 */ "1870, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1878, 1878, 1883",
      /*  697 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1891, 1891, 1896, 2092",
      /*  714 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1590, 1272, 1390, 1904, 1904, 1909, 2092, 2092",
      /*  731 */ "2092, 2092, 2092, 2092, 2092, 2092, 2114, 1594, 2078, 1152, 2114, 2092, 2107, 2112, 2092, 2092, 2092",
      /*  748 */ "2092, 2092, 2092, 2092, 2092, 2092, 1594, 1519, 1523, 1665, 1999, 2092, 2002, 2092, 2092, 2092, 2092",
      /*  765 */ "2092, 2092, 2092, 2092, 2092, 1594, 1590, 2092, 1924, 1727, 1917, 1922, 2092, 2092, 2092, 2092, 2092",
      /*  782 */ "2092, 2092, 2092, 2092, 1594, 1590, 2092, 1390, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  799 */ "2092, 1389, 2092, 1594, 1590, 1272, 1381, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  816 */ "1389, 1952, 1594, 1590, 1272, 1381, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /*  833 */ "2092, 1594, 1590, 1272, 1390, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1095",
      /*  850 */ "1594, 1590, 1272, 1390, 1962, 1968, 1973, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1683, 1594",
      /*  867 */ "1590, 1272, 1390, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2020, 1594, 1590",
      /*  884 */ "1272, 1390, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1359, 1594, 1590, 1272",
      /*  901 */ "1390, 1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1954, 1594, 1590, 1272, 1390",
      /*  918 */ "1933, 1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2093, 1594, 1590, 1272, 1390, 1933",
      /*  935 */ "1939, 1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1981, 1590, 1272, 1390, 1933, 1939",
      /*  952 */ "1944, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2010, 1590, 1272, 1390, 2028, 2034, 2039",
      /*  969 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1146, 1590, 1272, 1925, 1933, 1939, 1944, 2092",
      /*  986 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 1753, 2047, 2062, 2064, 1745, 1749, 2072, 2092, 2092",
      /* 1003 */ "2092, 2092, 2092, 2092, 2092, 2092, 2092, 1594, 1563, 1567, 1501, 2092, 1498, 2101, 2092, 2092, 2092",
      /* 1020 */ "2092, 2092, 2092, 2092, 2092, 2092, 1594, 1688, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /* 1037 */ "2092, 2092, 2092, 2092, 2092, 1594, 1590, 1200, 1214, 2092, 1207, 1212, 2092, 2092, 2092, 2092, 2092",
      /* 1054 */ "2092, 2092, 2092, 2092, 1594, 1590, 2092, 1390, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /* 1071 */ "2092, 2124, 2092, 2092, 2092, 2092, 2092, 2092, 2125, 2092, 2092, 2092, 2092, 2092, 2092, 2092, 2092",
      /* 1088 */ "5956, 5956, 5956, 256, 0, 0, 5956, 0, 0, 8576, 0, 0, 0, 0, 0, 6912, 6912, 6912, 6912, 6912, 6912, 0",
      /* 1110 */ "0, 0, 0, 0, 0, 7936, 7936, 5956, 5956, 0, 0, 5956, 5956, 5956, 5956, 5956, 5956, 5956, 5956, 5956",
      /* 1130 */ "5956, 0, 0, 0, 0, 512, 640, 0, 7680, 6528, 0, 6528, 0, 0, 0, 6528, 0, 0, 8704, 0, 512, 640, 0, 0",
      /* 1154 */ "8320, 0, 0, 0, 8320, 0, 0, 6528, 6528, 0, 6528, 6528, 0, 0, 6528, 0, 0, 0, 0, 0, 0, 6528, 6528, 6528",
      /* 1178 */ "6528, 6528, 6528, 6528, 0, 0, 0, 0, 0, 0, 7552, 0, 0, 0, 7552, 6656, 0, 6656, 0, 0, 0, 6656, 0, 0",
      /* 1202 */ "9088, 0, 0, 0, 9088, 0, 0, 9088, 9088, 9088, 9088, 9088, 9088, 0, 0, 0, 0, 0, 0, 0, 7424, 7424, 0",
      /* 1225 */ "6656, 6656, 0, 6656, 6656, 0, 0, 837, 0, 896, 0, 0, 0, 256, 0, 0, 7936, 0, 0, 0, 0, 0, 512, 6784",
      /* 1249 */ "6784, 0, 6784, 0, 6784, 0, 0, 0, 6784, 0, 0, 0, 0, 0, 6784, 6784, 0, 6784, 6784, 0, 0, 1152, 0, 0, 0",
      /* 1274 */ "0, 0, 6272, 0, 0, 0, 0, 6784, 6784, 6784, 6784, 6784, 6784, 6784, 0, 0, 0, 0, 0, 0, 7680, 7680, 0",
      /* 1297 */ "7040, 7040, 7040, 7040, 7040, 7040, 7040, 0, 7040, 7040, 0, 0, 0, 0, 0, 0, 8192, 8192, 0, 7238, 7238",
      /* 1318 */ "7238, 7238, 7238, 7238, 7238, 0, 7296, 7296, 7296, 7296, 7296, 7296, 7296, 7424, 0, 0, 256, 0, 0, 0",
      /* 1338 */ "0, 512, 640, 0, 7936, 7424, 0, 0, 0, 0, 7424, 0, 6016, 7424, 7424, 7424, 7424, 7424, 7424, 7424",
      /* 1358 */ "7424, 0, 0, 0, 0, 0, 5120, 0, 0, 0, 7552, 0, 256, 0, 0, 0, 7552, 7552, 7552, 7552, 7552, 7552, 7552",
      /* 1381 */ "0, 0, 0, 0, 0, 6144, 0, 6016, 6144, 0, 0, 0, 0, 0, 0, 0, 6016, 7808, 7808, 7808, 0, 0, 0, 0, 0, 7552",
      /* 1407 */ "7552, 0, 0, 1152, 1152, 1152, 1152, 1152, 1152, 1152, 8192, 8192, 8192, 8192, 8192, 8192, 8192, 8192",
      /* 1425 */ "0, 0, 0, 0, 0, 6528, 640, 6528, 0, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 0, 0, 0, 0, 0",
      /* 1447 */ "7040, 0, 0, 0, 7040, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 0, 0, 0, 0, 0, 7040, 0, 7040",
      /* 1468 */ "7040, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 0, 0, 0, 0, 0, 7808, 7808, 7808, 7808, 7808",
      /* 1487 */ "7808, 7808, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 0, 0, 0, 0, 0, 8960, 8960, 8960, 8960",
      /* 1506 */ "8960, 8960, 8960, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 0, 0, 0, 0, 0, 256, 0, 4608, 0, 0",
      /* 1527 */ "4608, 4608, 0, 0, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 0, 0, 0, 0, 0, 256, 0, 7808, 0",
      /* 1548 */ "7808, 7808, 7808, 0, 7808, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 0, 0, 0, 0, 0, 256, 0",
      /* 1568 */ "8960, 0, 8960, 8960, 8960, 0, 8960, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 0, 0, 0, 0, 0",
      /* 1588 */ "256, 6528, 0, 0, 0, 256, 0, 0, 0, 0, 512, 640, 0, 0, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304",
      /* 1610 */ "0, 0, 0, 0, 0, 256, 6656, 0, 0, 0, 256, 0, 0, 0, 7040, 2432, 2432, 2432, 2432, 2432, 2432, 2432",
      /* 1632 */ "2432, 0, 0, 0, 0, 0, 256, 6784, 0, 0, 0, 256, 0, 0, 7680, 0, 0, 0, 0, 0, 7424, 0, 0, 2560, 2560",
      /* 1657 */ "2560, 2560, 2560, 2560, 2560, 2560, 0, 0, 0, 0, 0, 4608, 4608, 4608, 0, 0, 2688, 2688, 2688, 2688",
      /* 1677 */ "2688, 2688, 2688, 2688, 0, 0, 0, 0, 0, 4864, 0, 0, 0, 0, 5760, 0, 0, 0, 0, 5956, 5956, 5956, 0, 5956",
      /* 1701 */ "0, 0, 0, 5956, 512, 640, 0, 0, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 0, 0, 0, 0, 0, 7296",
      /* 1723 */ "0, 0, 0, 7296, 0, 0, 0, 0, 0, 0, 8448, 8448, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 0, 0, 0",
      /* 1746 */ "0, 0, 8832, 8832, 8832, 0, 8832, 0, 0, 0, 8832, 512, 640, 0, 0, 3072, 3072, 3072, 3072, 3072, 3072",
      /* 1767 */ "3072, 3072, 0, 0, 0, 0, 0, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 0, 0, 0, 0, 0, 3328, 3328",
      /* 1789 */ "3328, 3328, 3328, 3328, 3328, 3328, 0, 0, 0, 0, 0, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 0",
      /* 1809 */ "0, 0, 0, 0, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 0, 0, 0, 0, 0, 3712, 3712, 3712, 3712",
      /* 1830 */ "3712, 3712, 3712, 3712, 0, 0, 0, 0, 0, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 0, 0, 0, 0, 0",
      /* 1852 */ "3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 0, 0, 0, 0, 0, 4096, 4096, 4096, 4096, 4096, 4096",
      /* 1871 */ "4096, 4096, 0, 0, 0, 0, 0, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 0, 0, 0, 0, 0, 4352, 4352",
      /* 1893 */ "4352, 4352, 4352, 4352, 4352, 4352, 0, 0, 0, 0, 0, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 0",
      /* 1913 */ "0, 0, 0, 0, 8448, 8448, 8448, 8448, 8448, 8448, 8448, 8448, 0, 0, 0, 0, 0, 0, 0, 8704, 384, 0, 0",
      /* 1936 */ "384, 0, 0, 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 0, 0, 0, 0, 4736, 0, 0, 0, 0, 0, 0, 5248, 0",
      /* 1962 */ "8576, 0, 0, 8576, 0, 0, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 0, 0, 0, 0, 0, 5504, 0, 0, 0",
      /* 1985 */ "512, 640, 0, 0, 7168, 7168, 7168, 7168, 7168, 7168, 7238, 0, 0, 1024, 0, 0, 4608, 0, 0, 4608, 0, 0",
      /* 2007 */ "0, 0, 0, 0, 5632, 0, 0, 512, 640, 0, 0, 8064, 0, 0, 0, 0, 0, 4992, 0, 0, 0, 5632, 0, 0, 5632, 0, 0",
      /* 2034 */ "5632, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 0, 0, 0, 0, 0, 8832, 8832, 8832, 8832, 0, 0, 8832, 0",
      /* 2055 */ "837, 837, 837, 837, 837, 837, 837, 8832, 8832, 0, 0, 8832, 8832, 8832, 8832, 8832, 8832, 8832, 8832",
      /* 2074 */ "8832, 0, 0, 0, 0, 0, 8320, 256, 0, 0, 0, 0, 6656, 6656, 6656, 6656, 6656, 6656, 0, 0, 0, 0, 0, 0, 0",
      /* 2099 */ "0, 5376, 8960, 8960, 8960, 0, 0, 0, 0, 0, 8320, 8320, 8320, 8320, 8320, 8320, 0, 0, 0, 0, 0, 0, 0",
      /* 2122 */ "8064, 8064, 0, 6400, 0, 0, 0, 0, 0, 0, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2133; ++i) {TRANSITION[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] EXPECTED = new int[183];
  static
  {
    final String s1[] =
    {
      /*   0 */ "145, 145, 145, 145, 145, 143, 146, 145, 145, 145, 53, 54, 58, 59, 63, 63, 65, 81, 91, 69, 79, 85, 89",
      /*  23 */ "95, 99, 103, 107, 111, 115, 119, 123, 127, 131, 135, 139, 145, 145, 150, 145, 141, 145, 156, 179, 159",
      /*  44 */ "163, 167, 169, 152, 72, 75, 173, 176, 145, 0, 480, 480, 480, 480, -508, -512, -512, -508, -508, -508",
      /*  64 */ "-508, -508, -508, -28, 0, 67108864, 1073741824, 0, 16, 16, 6, 22, 22, 71, 87, 0, 32, 64, 128, 32768",
      /*  84 */ "131072, 256, 512, 1024, 2048, 0, 8192, 262144, 1048576, 4194304, 33554432, 1310720, 671088640",
      /*  97 */ "33562624, 67117056, 8192, 4096, 1835008, 268435464, 671096832, 339738624, 1843200, 268443656, 1835008",
      /* 108 */ "356515840, 268509192, 301998088, 1843200, 356524032, 35389440, 364904448, 364912640, 365174792",
      /* 117 */ "365961224, 398499848, 2109743104, 2109759488, 2055, 1835015, 1835023, 10247, 1843207, 1843215",
      /* 127 */ "-2113927161, -2113918969, -2113787897, -2112608249, -2101606393, -2101598201, -1828976633",
      /* 134 */ "-1766062073, -1761867769, -1828968441, -1766053881, -1761859577, -251889, 8192, 0, 0, 8, 16, 0, 0, 0",
      /* 148 */ "0, 2, 1, 0, 4, 0, 0, 20, 16, 16, 17, 16, 48, 65, 32, 48, 48, 81, 48, 67, 32, 48, 48, 48, 56, 103, 103",
      /* 175 */ "103, 119, 119, 119, 0, 32, 16, 32"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 183; ++i) {EXPECTED[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] CASEID = new int[1757];
  static
  {
    final String s1[] =
    {
      /*    0 */ "612, 629, 585, 682, 589, 595, 598, 602, 710, 616, 621, 629, 624, 628, 633, 639, 664, 617, 664, 608",
      /*   20 */ "644, 647, 651, 736, 663, 664, 664, 715, 664, 608, 669, 672, 676, 765, 680, 664, 664, 836, 786, 761",
      /*   40 */ "686, 689, 693, 664, 703, 612, 629, 585, 664, 708, 612, 629, 585, 664, 708, 612, 629, 585, 664, 708",
      /*   60 */ "595, 598, 602, 710, 714, 719, 722, 726, 730, 734, 644, 647, 651, 736, 663, 719, 722, 740, 744, 734",
      /*   80 */ "664, 664, 960, 635, 608, 1096, 664, 965, 857, 926, 748, 751, 755, 664, 769, 923, 664, 974, 935, 774",
      /*  100 */ "664, 664, 715, 664, 608, 612, 629, 585, 780, 633, 612, 629, 585, 664, 708, 920, 664, 895, 909, 784",
      /*  120 */ "790, 793, 797, 806, 810, 816, 819, 823, 664, 835, 923, 664, 974, 935, 774, 644, 647, 651, 840, 844",
      /*  140 */ "686, 689, 693, 664, 703, 612, 629, 585, 850, 633, 605, 664, 960, 826, 800, 664, 664, 664, 854, 861",
      /*  160 */ "612, 629, 585, 867, 633, 664, 664, 960, 871, 608, 790, 793, 797, 877, 810, 612, 629, 585, 881, 888",
      /*  180 */ "664, 664, 960, 654, 800, 612, 629, 585, 881, 888, 664, 664, 960, 654, 800, 612, 629, 585, 881, 888",
      /*  200 */ "664, 664, 960, 654, 800, 605, 664, 960, 826, 800, 664, 664, 960, 871, 829, 1093, 664, 983, 1051, 946",
      /*  220 */ "664, 664, 664, 664, 894, 664, 664, 1038, 664, 664, 664, 664, 960, 664, 657, 899, 902, 906, 913, 917",
      /*  240 */ "664, 664, 664, 932, 664, 644, 647, 651, 939, 943, 644, 647, 651, 939, 943, 644, 647, 651, 939, 943",
      /*  260 */ "664, 664, 664, 932, 952, 959, 664, 664, 964, 664, 1128, 664, 664, 1131, 664, 591, 629, 969, 871, 608",
      /*  280 */ "664, 664, 664, 664, 973, 790, 793, 797, 978, 810, 758, 664, 988, 1105, 1137, 1125, 664, 997, 1118",
      /*  299 */ "1140, 982, 664, 664, 987, 664, 1004, 664, 664, 1004, 664, 802, 992, 996, 1001, 664, 664, 664, 664",
      /*  318 */ "664, 894, 605, 664, 960, 826, 800, 959, 664, 664, 1032, 664, 1128, 664, 664, 1007, 664, 664, 664",
      /*  337 */ "884, 640, 800, 664, 664, 960, 763, 608, 959, 664, 664, 1037, 664, 1128, 664, 664, 1013, 664, 605",
      /*  356 */ "664, 960, 826, 800, 664, 664, 715, 664, 608, 664, 664, 664, 1042, 664, 664, 664, 664, 1090, 664, 664",
      /*  376 */ "664, 664, 1055, 664, 591, 629, 1077, 871, 608, 664, 664, 960, 871, 608, 664, 664, 665, 928, 696, 802",
      /*  396 */ "992, 1081, 1001, 664, 664, 664, 664, 1109, 664, 664, 664, 960, 610, 608, 664, 664, 884, 640, 800",
      /*  415 */ "664, 664, 664, 664, 1122, 664, 664, 1064, 664, 664, 664, 664, 664, 1147, 664, 664, 664, 1016, 664",
      /*  434 */ "664, 664, 664, 664, 812, 664, 664, 664, 664, 770, 664, 664, 664, 863, 664, 664, 664, 664, 1019, 664",
      /*  454 */ "664, 664, 664, 664, 846, 664, 664, 664, 664, 955, 1022, 664, 664, 873, 664, 664, 664, 664, 664, 1028",
      /*  474 */ "1045, 1048, 664, 664, 1143, 1058, 664, 664, 948, 664, 664, 664, 664, 664, 1151, 664, 664, 664, 664",
      /*  493 */ "664, 1155, 664, 664, 704, 664, 664, 664, 664, 890, 664, 664, 664, 664, 664, 1159, 664, 664, 664, 664",
      /*  513 */ "1061, 664, 1099, 664, 1163, 699, 1102, 664, 664, 664, 1025, 664, 664, 664, 1134, 664, 664, 664, 664",
      /*  532 */ "1167, 831, 1067, 664, 664, 1171, 1175, 1070, 1010, 664, 1179, 1183, 1084, 1010, 664, 1179, 1073",
      /*  549 */ "1084, 664, 664, 1187, 1033, 1087, 664, 664, 664, 659, 664, 664, 664, 664, 1191, 664, 664, 664, 664",
      /*  568 */ "1112, 664, 664, 664, 664, 1195, 664, 664, 664, 664, 1115, 664, 664, 664, 664, 776, 664, 1200, 1237",
      /*  587 */ "1233, 1361, 1426, 1210, 1237, 1237, 1199, 1206, 1239, 1237, 1227, 1217, 1217, 1217, 1217, 1228, 1237",
      /*  604 */ "1238, 1237, 1199, 1236, 1237, 1210, 1237, 1237, 1234, 1237, 1199, 1206, 1271, 1237, 1237, 1237, 1232",
      /*  621 */ "1234, 1199, 1206, 1206, 1237, 1233, 1361, 1425, 1206, 1206, 1206, 1206, 1200, 1221, 1237, 1237, 1233",
      /*  638 */ "1235, 1405, 1237, 1237, 1237, 1236, 1519, 1237, 1244, 1249, 1249, 1249, 1249, 1245, 1237, 1518, 1237",
      /*  655 */ "1234, 1202, 1236, 1210, 1237, 1237, 1735, 1237, 1268, 1237, 1237, 1237, 1237, 1256, 1212, 1237, 1453",
      /*  672 */ "1285, 1285, 1285, 1285, 1454, 1237, 1211, 1289, 1414, 1210, 1237, 1237, 1235, 1233, 1677, 1237, 1301",
      /*  689 */ "1306, 1306, 1306, 1306, 1302, 1237, 1676, 1237, 1257, 1237, 1237, 1259, 1261, 1263, 1677, 1237, 1237",
      /*  706 */ "1237, 1264, 1234, 1210, 1237, 1237, 1240, 1238, 1224, 1237, 1237, 1237, 1279, 1363, 1237, 1310, 1315",
      /*  723 */ "1315, 1315, 1315, 1311, 1237, 1366, 1279, 1363, 1365, 1364, 1366, 1319, 1210, 1237, 1237, 1253, 1297",
      /*  740 */ "1311, 1237, 1366, 1289, 1235, 1237, 1364, 1366, 1462, 1237, 1340, 1345, 1345, 1345, 1345, 1341, 1237",
      /*  757 */ "1461, 1237, 1275, 1525, 1237, 1296, 1237, 1237, 1235, 1237, 1213, 1211, 1462, 1237, 1237, 1237, 1291",
      /*  774 */ "1444, 1352, 1237, 1237, 1281, 1237, 1425, 1201, 1426, 1424, 1473, 1442, 1237, 1237, 1336, 1295, 1498",
      /*  791 */ "1237, 1374, 1379, 1379, 1379, 1379, 1375, 1237, 1497, 1237, 1423, 1237, 1237, 1555, 1560, 1501, 1503",
      /*  808 */ "1502, 1500, 1375, 1502, 1237, 1237, 1354, 1593, 1492, 1237, 1383, 1388, 1388, 1388, 1388, 1384, 1237",
      /*  825 */ "1491, 1237, 1425, 1202, 1236, 1450, 1237, 1237, 1681, 1688, 1492, 1237, 1237, 1237, 1295, 1657, 1392",
      /*  842 */ "1396, 1398, 1402, 1411, 1237, 1237, 1419, 1237, 1425, 1206, 1426, 1424, 1683, 1675, 1430, 1237, 1432",
      /*  859 */ "1328, 1330, 1437, 1532, 1237, 1237, 1566, 1237, 1425, 1206, 1235, 1424, 1425, 1236, 1237, 1237, 1615",
      /*  876 */ "1237, 1501, 1379, 1502, 1500, 1425, 1201, 1235, 1233, 1237, 1237, 1361, 1200, 1450, 1237, 1237, 1655",
      /*  893 */ "1237, 1472, 1237, 1237, 1237, 1358, 1509, 1237, 1477, 1482, 1482, 1482, 1482, 1478, 1237, 1508, 1237",
      /*  910 */ "1439, 1370, 1441, 1512, 1482, 1510, 1511, 1478, 1513, 1237, 1237, 1440, 1443, 1237, 1350, 1353, 1237",
      /*  927 */ "1334, 1237, 1237, 1255, 1237, 1683, 1675, 1237, 1237, 1445, 1349, 1351, 1657, 1486, 1624, 1297, 1402",
      /*  944 */ "1651, 1237, 1237, 1466, 1237, 1237, 1699, 1237, 1490, 1532, 1237, 1237, 1468, 1468, 1604, 1578, 1237",
      /*  961 */ "1237, 1237, 1361, 1719, 1237, 1237, 1237, 1417, 1206, 1237, 1237, 1361, 1517, 1237, 1237, 1237, 1446",
      /*  978 */ "1501, 1379, 1499, 1500, 1550, 1237, 1237, 1237, 1459, 1551, 1237, 1237, 1237, 1524, 1560, 1560, 1560",
      /*  995 */ "1560, 1561, 1237, 1237, 1237, 1537, 1407, 1565, 1237, 1237, 1549, 1237, 1237, 1570, 1237, 1237, 1573",
      /* 1012 */ "1711, 1237, 1577, 1237, 1237, 1587, 1237, 1237, 1599, 1237, 1237, 1609, 1237, 1237, 1611, 1237, 1237",
      /* 1029 */ "1626, 1621, 1628, 1725, 1237, 1237, 1237, 1723, 1731, 1237, 1237, 1237, 1740, 1737, 1237, 1237, 1237",
      /* 1046 */ "1627, 1237, 1237, 1632, 1636, 1237, 1638, 1458, 1460, 1673, 1675, 1237, 1237, 1644, 1237, 1237, 1661",
      /* 1063 */ "1237, 1237, 1663, 1237, 1237, 1687, 1237, 1237, 1703, 1237, 1237, 1715, 1709, 1711, 1200, 1237, 1237",
      /* 1080 */ "1361, 1556, 1237, 1237, 1237, 1717, 1237, 1237, 1729, 1237, 1237, 1739, 1237, 1237, 1639, 1460, 1237",
      /* 1097 */ "1433, 1335, 1237, 1260, 1263, 1237, 1262, 1237, 1237, 1274, 1523, 1525, 1595, 1582, 1237, 1237, 1744",
      /* 1114 */ "1237, 1237, 1750, 1237, 1237, 1752, 1536, 1538, 1539, 1237, 1237, 1237, 1753, 1538, 1237, 1496, 1237",
      /* 1131 */ "1237, 1507, 1237, 1237, 1526, 1237, 1237, 1530, 1237, 1237, 1543, 1237, 1237, 1545, 1643, 1645, 1322",
      /* 1148 */ "1237, 1237, 1237, 1324, 1649, 1237, 1237, 1705, 1237, 1237, 1237, 1589, 1237, 1237, 1237, 1258, 1237",
      /* 1165 */ "1237, 1667, 1600, 1237, 1237, 1671, 1605, 1237, 1237, 1692, 1697, 1237, 1237, 1693, 1571, 1237, 1237",
      /* 1182 */ "1710, 1589, 1572, 1709, 1711, 1583, 1237, 1237, 1237, 1617, 1237, 1237, 1237, 1746, 1237, 1237, 1237",
      /* 1199 */ "0, 65830, 65830, 65830, 0, 65830, 0, 65830, 65830, 65830, 65830, 25266, 0, 0, 0, 49156, 0, 0, 4, 4",
      /* 1219 */ "4, 4, 25266, 65830, 65830, 0, 4, 4, 0, 4, 4, 4, 0, 99178, 0, 0, 0, 65830, 0, 0, 0, 0, 4, 0, 0, 0",
      /* 1245 */ "573444, 573444, 573444, 0, 573444, 573444, 573444, 573444, 0, 90130, 0, 0, 131076, 0, 0, 0, 837782",
      /* 1262 */ "837782, 837782, 0, 0, 0, 845938, 0, 81938, 573444, 0, 4, 14, 0, 0, 1163268, 1163268, 1163268, 0",
      /* 1280 */ "107058, 0, 0, 147720, 0, 49156, 49156, 49156, 49156, 0, 123218, 0, 0, 180242, 0, 0, 163844, 0, 0, 0",
      /* 1300 */ "73746, 0, 229380, 229380, 229380, 0, 229380, 229380, 229380, 229380, 0, 82454, 82454, 82454, 0",
      /* 1315 */ "82454, 82454, 82454, 82454, 0, 82454, 82454, 0, 16650, 0, 0, 435874, 0, 196644, 0, 196644, 196644",
      /* 1332 */ "65830, 0, 32782, 196644, 0, 0, 0, 163844, 0, 262152, 262152, 262152, 0, 262152, 262152, 262152",
      /* 1348 */ "262152, 212996, 0, 212996, 212996, 212996, 0, 0, 0, 172050, 0, 246806, 278536, 0, 16706, 0, 0, 82454",
      /* 1366 */ "0, 0, 0, 82454, 246806, 0, 49166, 246806, 0, 344068, 344068, 344068, 0, 344068, 344068, 344068",
      /* 1382 */ "344068, 0, 294916, 294916, 294916, 0, 294916, 294916, 294916, 294916, 638980, 262162, 360468, 270354",
      /* 1396 */ "0, 65550, 360468, 0, 0, 73746, 638980, 81934, 573444, 0, 25330, 0, 0, 98318, 370754, 0, 360468",
      /* 1413 */ "638980, 0, 49156, 49156, 0, 16398, 0, 0, 180328, 0, 25266, 65830, 0, 0, 65830, 65830, 0, 0, 704520",
      /* 1432 */ "0, 0, 196644, 196644, 196644, 868356, 720904, 0, 0, 246806, 246806, 246806, 0, 0, 0, 212996, 212996",
      /* 1449 */ "0, 25266, 0, 65830, 0, 49156, 49156, 49156, 0, 622596, 0, 622596, 0, 0, 0, 262152, 0, 622596, 622596",
      /* 1468 */ "0, 0, 328806, 0, 452770, 0, 0, 0, 278536, 0, 393220, 393220, 393220, 0, 393220, 393220, 393220",
      /* 1485 */ "393220, 638980, 262162, 0, 270354, 868356, 0, 0, 0, 294916, 0, 802824, 0, 0, 0, 344068, 0, 0, 344068",
      /* 1504 */ "344068, 0, 344068, 837554, 0, 0, 0, 393220, 0, 0, 393220, 393220, 0, 901124, 0, 0, 0, 573444, 0",
      /* 1523 */ "1163268, 0, 1163268, 0, 0, 0, 589842, 1163268, 1163268, 0, 0, 376850, 0, 1146884, 0, 1146884, 0, 0",
      /* 1541 */ "0, 688146, 1146884, 1146884, 0, 0, 376852, 311314, 737284, 0, 0, 0, 737284, 0, 0, 1130504, 1130504",
      /* 1558 */ "1130504, 0, 1130504, 1130504, 1130504, 1130504, 936378, 114702, 0, 0, 0, 737298, 894914, 0, 0, 0",
      /* 1574 */ "771942, 771942, 771942, 928418, 0, 0, 0, 786440, 576114, 0, 0, 0, 936378, 0, 163858, 0, 0, 493298, 0",
      /* 1593 */ "0, 172050, 0, 0, 567778, 370754, 761874, 0, 0, 0, 983060, 238706, 0, 0, 0, 1114132, 0, 238722, 0, 0",
      /* 1613 */ "581650, 0, 0, 786450, 0, 0, 600610, 0, 288066, 0, 361782, 0, 65550, 0, 0, 361782, 0, 0, 0, 0, 294930",
      /* 1634 */ "303122, 319506, 327698, 0, 0, 0, 622596, 622596, 622596, 376852, 0, 376852, 0, 0, 0, 444290, 0, 0, 0",
      /* 1653 */ "638980, 0, 860178, 0, 0, 0, 638980, 638980, 501714, 0, 0, 0, 704530, 0, 0, 837782, 0, 845938, 0",
      /* 1672 */ "131086, 0, 0, 753668, 0, 0, 0, 229380, 0, 0, 1081364, 0, 0, 753668, 370754, 131086, 983060, 0, 0, 0",
      /* 1692 */ "0, 1114132, 0, 0, 0, 1097752, 0, 0, 0, 802834, 0, 1114132, 1114132, 0, 0, 819218, 0, 771942, 0",
      /* 1711 */ "771942, 0, 0, 0, 501714, 0, 771942, 771942, 0, 0, 829138, 0, 624946, 0, 0, 0, 886498, 0, 0, 624962",
      /* 1731 */ "0, 0, 920082, 0, 0, 630802, 0, 0, 1064964, 0, 0, 0, 819204, 608946, 0, 0, 0, 1083978, 0, 1084122, 0",
      /* 1752 */ "0, 0, 1146884, 1146884, 1146884"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1757; ++i) {CASEID[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TOKENSET = new int[117];
  static
  {
    final String s1[] =
    {
      /*   0 */ "55, 54, 66, 27, 54, 19, 56, 30, 48, 51, 51, 51, 54, 57, 54, 56, 30, 46, 48, 47, 19, 64, 51, 47, 61",
      /*  25 */ "48, 47, 62, 48, 65, 42, 40, 63, 32, 62, 59, 39, 59, 39, 59, 39, 42, 38, 42, 8, 0, 26, 60, 28, 58, 58",
      /*  51 */ "58, 34, 20, 21, 53, 8, 60, 42, 42, 20, 21, 50, 8, 42, 20, 21, 33, 24, 20, 21, 42, 19, 2, 3, 22, 52",
      /*  77 */ "32, 25, 49, 28, 25, 33, 18, 12, 1, 11, 23, 7, 17, 9, 6, 31, 15, 35, 41, 16, 22, 10, 0, 14, 2, 3, 45",
      /* 104 */ "4, 13, 37, 36, 43, 44, 29, 5, 2, 3, 2, 3, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 117; ++i) {TOKENSET[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] APPENDIX = new int[18];
  static
  {
    final String s1[] =
    {
      /*  0 */ "2, 339977, 8353, 98322, 12633, 98322, 123403, 139268, 45065, 319490, 40969, 319490, 283889, 376834",
      /* 14 */ "288057, 376834, 491530, 540682"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 18; ++i) {APPENDIX[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] LOOKBACK = new int[556];
  static
  {
    final String s1[] =
    {
      /*   0 */ "176, 176, 176, 176, 176, 174, 182, 177, 185, 185, 185, 188, 198, 198, 198, 193, 201, 204, 207, 216",
      /*  20 */ "216, 225, 216, 225, 216, 225, 238, 238, 247, 238, 247, 238, 247, 260, 269, 269, 269, 269, 278, 278",
      /*  40 */ "278, 278, 176, 176, 176, 176, 287, 287, 287, 287, 294, 294, 294, 294, 301, 308, 176, 311, 314, 314",
      /*  60 */ "314, 317, 327, 327, 327, 322, 176, 176, 176, 176, 330, 335, 340, 345, 350, 350, 350, 350, 176, 176",
      /*  80 */ "353, 353, 353, 356, 361, 366, 369, 369, 369, 176, 176, 176, 176, 372, 385, 385, 385, 375, 380, 393",
      /* 100 */ "388, 396, 176, 176, 176, 399, 399, 399, 405, 410, 415, 425, 420, 420, 420, 420, 420, 430, 433, 402",
      /* 120 */ "402, 402, 438, 443, 448, 458, 453, 453, 453, 453, 453, 463, 176, 466, 469, 469, 469, 474, 176, 477",
      /* 140 */ "477, 477, 480, 490, 490, 490, 490, 493, 498, 503, 508, 511, 511, 511, 511, 483, 514, 176, 176, 176",
      /* 160 */ "517, 520, 528, 523, 531, 534, 176, 176, 176, 539, 542, 550, 545, 553, 3, 2, 0, 6, 6, 3, 4, 0, 5, 5, 0",
      /* 185 */ "7, 7, 0, 13, 12, 9, 8, 0, 13, 14, 9, 10, 0, 11, 11, 0, 15, 15, 0, 16, 16, 0, 28, 26, 27, 26, 21, 19",
      /* 213 */ "20, 19, 0, 28, 29, 27, 29, 21, 22, 20, 22, 0, 39, 40, 35, 36, 28, 30, 27, 29, 21, 23, 20, 22, 0, 28",
      /* 239 */ "31, 27, 31, 21, 24, 20, 24, 0, 39, 41, 35, 37, 28, 32, 27, 31, 21, 25, 20, 24, 0, 39, 38, 35, 34, 28",
      /* 265 */ "38, 21, 34, 0, 39, 40, 35, 36, 28, 40, 21, 36, 0, 39, 41, 35, 37, 28, 41, 21, 37, 0, 51, 50, 47, 46",
      /* 291 */ "43, 42, 0, 51, 52, 47, 48, 43, 44, 0, 51, 53, 47, 49, 43, 45, 0, 54, 54, 0, 56, 56, 0, 57, 57, 0, 63",
      /* 318 */ "62, 59, 58, 0, 63, 64, 59, 60, 0, 61, 61, 0, 75, 74, 67, 66, 0, 75, 76, 67, 68, 0, 75, 77, 67, 69, 0",
      /* 345 */ "72, 72, 71, 71, 0, 73, 73, 0, 78, 79, 0, 87, 86, 81, 80, 0, 87, 88, 81, 82, 0, 84, 84, 0, 85, 85, 0",
      /* 372 */ "90, 89, 0, 95, 94, 90, 91, 0, 95, 96, 90, 92, 0, 93, 93, 0, 99, 99, 97, 97, 0, 98, 98, 0, 100, 100, 0",
      /* 399 */ "103, 102, 0, 103, 104, 0, 113, 112, 106, 105, 0, 113, 114, 106, 107, 0, 113, 115, 106, 107, 0, 111",
      /* 421 */ "111, 108, 108, 0, 110, 109, 109, 109, 0, 113, 116, 0, 124, 131, 110, 117, 0, 127, 126, 120, 119, 0",
      /* 443 */ "127, 128, 120, 121, 0, 127, 129, 120, 121, 0, 125, 125, 122, 122, 0, 124, 123, 123, 123, 0, 127, 130",
      /* 465 */ "0, 132, 132, 0, 135, 134, 133, 133, 0, 135, 136, 0, 138, 138, 0, 140, 139, 0, 152, 154, 144, 146, 140",
      /* 488 */ "141, 0, 142, 142, 0, 152, 151, 144, 143, 0, 152, 153, 144, 145, 0, 148, 148, 147, 147, 0, 149, 149, 0",
      /* 511 */ "150, 150, 0, 155, 155, 0, 158, 157, 0, 158, 159, 0, 162, 162, 160, 160, 0, 161, 161, 0, 163, 163, 0",
      /* 534 */ "173, 173, 164, 164, 0, 167, 166, 0, 167, 168, 0, 171, 171, 169, 169, 0, 170, 170, 0, 172, 172, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 556; ++i) {LOOKBACK[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] GOTO = new int[920];
  static
  {
    final String s1[] =
    {
      /*   0 */ "332, 399, 399, 399, 552, 399, 434, 399, 486, 399, 399, 399, 471, 399, 399, 399, 483, 495, 399, 399",
      /*  20 */ "338, 399, 399, 399, 344, 399, 399, 399, 399, 399, 411, 399, 542, 399, 488, 399, 350, 399, 399, 399",
      /*  40 */ "356, 399, 399, 399, 362, 399, 399, 399, 399, 399, 434, 399, 504, 399, 399, 399, 480, 495, 399, 399",
      /*  60 */ "526, 399, 399, 399, 368, 399, 399, 399, 374, 399, 399, 399, 446, 399, 399, 399, 399, 399, 533, 399",
      /*  80 */ "380, 399, 399, 399, 386, 399, 399, 399, 392, 399, 399, 399, 346, 399, 399, 399, 428, 399, 399, 398",
      /* 100 */ "516, 399, 400, 399, 399, 399, 340, 399, 382, 404, 394, 410, 464, 399, 488, 399, 415, 399, 399, 399",
      /* 120 */ "421, 399, 399, 399, 399, 406, 427, 540, 432, 399, 399, 399, 438, 399, 399, 399, 511, 399, 399, 398",
      /* 140 */ "444, 399, 399, 399, 450, 399, 399, 399, 456, 399, 399, 399, 462, 399, 399, 399, 468, 399, 399, 399",
      /* 160 */ "477, 399, 399, 399, 492, 399, 399, 399, 501, 399, 399, 399, 399, 399, 399, 510, 399, 399, 515, 399",
      /* 180 */ "399, 399, 399, 523, 520, 399, 399, 399, 399, 399, 399, 532, 399, 334, 399, 549, 388, 404, 394, 410",
      /* 200 */ "376, 537, 394, 410, 376, 546, 394, 410, 399, 423, 427, 540, 399, 352, 399, 399, 399, 576, 399, 399",
      /* 220 */ "558, 399, 399, 399, 399, 399, 399, 440, 554, 399, 399, 398, 399, 399, 399, 528, 399, 399, 399, 506",
      /* 240 */ "399, 399, 399, 561, 399, 399, 399, 564, 399, 358, 571, 549, 399, 399, 575, 399, 580, 399, 399, 399",
      /* 260 */ "399, 352, 399, 399, 399, 576, 399, 399, 584, 399, 399, 399, 588, 399, 399, 399, 399, 370, 399, 399",
      /* 280 */ "399, 473, 399, 399, 592, 399, 399, 399, 596, 399, 399, 399, 399, 399, 399, 452, 399, 399, 399, 458",
      /* 300 */ "399, 364, 399, 549, 600, 399, 399, 399, 604, 399, 399, 399, 399, 399, 417, 399, 399, 358, 608, 549",
      /* 320 */ "399, 497, 567, 399, 612, 399, 399, 399, 616, 399, 399, 399, 620, 645, 646, 646, 623, 800, 657, 666",
      /* 340 */ "646, 646, 646, 916, 772, 671, 646, 646, 662, 646, 717, 645, 646, 646, 677, 646, 638, 645, 646, 646",
      /* 360 */ "708, 865, 635, 645, 646, 646, 723, 646, 684, 645, 646, 646, 746, 646, 687, 645, 646, 646, 750, 646",
      /* 380 */ "807, 666, 646, 646, 750, 812, 790, 645, 646, 646, 750, 834, 796, 645, 646, 646, 763, 646, 739, 646",
      /* 400 */ "646, 646, 646, 667, 755, 759, 646, 646, 769, 776, 802, 646, 646, 646, 676, 840, 645, 646, 646, 780",
      /* 420 */ "646, 720, 645, 646, 646, 824, 776, 779, 646, 646, 646, 681, 855, 645, 646, 646, 829, 646, 861, 645",
      /* 440 */ "646, 646, 838, 646, 867, 645, 646, 646, 849, 646, 907, 645, 646, 646, 893, 646, 877, 645, 646, 646",
      /* 460 */ "899, 646, 626, 645, 646, 646, 914, 646, 889, 645, 646, 646, 651, 646, 646, 646, 887, 629, 645, 646",
      /* 480 */ "646, 652, 656, 646, 647, 656, 646, 645, 646, 646, 646, 847, 732, 645, 646, 646, 661, 646, 646, 646",
      /* 500 */ "905, 895, 645, 646, 646, 666, 646, 646, 740, 646, 672, 646, 646, 646, 690, 788, 646, 646, 646, 745",
      /* 520 */ "901, 645, 646, 646, 693, 646, 646, 671, 646, 646, 741, 646, 794, 646, 646, 646, 765, 811, 816, 646",
      /* 540 */ "646, 784, 646, 646, 827, 646, 811, 820, 646, 646, 806, 646, 646, 714, 646, 646, 646, 844, 632, 645",
      /* 560 */ "646, 646, 853, 646, 646, 859, 646, 646, 911, 646, 646, 641, 871, 875, 646, 881, 646, 646, 646, 833",
      /* 580 */ "711, 645, 646, 646, 696, 645, 646, 646, 702, 645, 646, 646, 883, 645, 646, 646, 751, 666, 646, 646",
      /* 600 */ "726, 645, 646, 646, 729, 645, 646, 646, 735, 871, 875, 646, 705, 645, 646, 646, 699, 645, 646, 646, 6",
      /* 621 */ "0, 4121, 0, 0, 237577, 0, 0, 245780, 0, 0, 253972, 0, 0, 255073, 0, 0, 262156, 0, 0, 270348, 0, 0",
      /* 643 */ "275569, 483332, 8409, 0, 0, 0, 0, 24649, 12697, 0, 0, 0, 24681, 28681, 0, 0, 0, 49201, 32777, 0, 0, 0",
      /* 665 */ "106985, 53561, 0, 0, 0, 114697, 61665, 0, 0, 0, 168729, 65545, 0, 0, 0, 246625, 0, 372745, 377369, 0",
      /* 685 */ "0, 356361, 0, 0, 360457, 0, 0, 377433, 0, 0, 406569, 0, 0, 451713, 0, 0, 451777, 0, 0, 454665, 0, 0",
      /* 707 */ "475145, 0, 0, 499716, 0, 0, 589860, 0, 20521, 57348, 0, 0, 278540, 0, 0, 303116, 0, 0, 319497, 0, 0",
      /* 728 */ "324777, 0, 0, 327689, 0, 0, 328485, 0, 0, 337073, 483332, 111217, 0, 0, 0, 262153, 0, 98348, 0, 0, 0",
      /* 749 */ "300289, 122889, 0, 0, 0, 307209, 389129, 188420, 188420, 188420, 32777, 0, 212996, 212996, 0, 212996",
      /* 765 */ "0, 0, 94681, 0, 168713, 172777, 344068, 0, 57433, 348281, 16388, 0, 335876, 176913, 425988, 0, 0, 0",
      /* 783 */ "331785, 398137, 0, 0, 180233, 0, 442380, 0, 0, 98313, 0, 0, 233481, 0, 0, 102409, 0, 0, 241673, 0, 0",
      /* 804 */ "127697, 0, 398137, 0, 0, 0, 364553, 294916, 0, 0, 0, 385673, 32777, 221212, 212996, 212996, 32777",
      /* 821 */ "237596, 212996, 212996, 168713, 0, 344068, 0, 69641, 0, 0, 16441, 0, 250833, 0, 0, 0, 385721, 258057",
      /* 839 */ "0, 0, 0, 139849, 0, 0, 425993, 377369, 0, 74177, 0, 0, 78281, 0, 0, 267145, 0, 0, 192521, 0, 0",
      /* 860 */ "271353, 0, 0, 196617, 0, 0, 434185, 0, 0, 201385, 0, 278537, 0, 516100, 475140, 438281, 0, 0, 0",
      /* 879 */ "204809, 0, 0, 458780, 0, 0, 204844, 0, 304457, 0, 0, 0, 208905, 0, 0, 468249, 0, 0, 213753, 0, 0",
      /* 900 */ "472417, 0, 0, 229385, 0, 0, 540676, 0, 0, 229396, 0, 0, 507940, 524292, 0, 155660, 0, 0, 94713, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 920; ++i) {GOTO[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] REDUCTION = new int[146];
  static
  {
    final String s1[] =
    {
      /*   0 */ "42, 0, 0, -1, 1, -1, 1, 1, 2, -1, 3, -1, 4, -1, 5, -1, 43, 2, 6, -1, 44, 3, 7, 4, 8, -1, 46, 6, 45, 5",
      /*  30 */ "9, 7, 10, -1, 11, -1, 47, 8, 12, 9, 13, -1, 48, 10, 14, -1, 15, -1, 49, 11, 16, 12, 16, -1, 17, 14",
      /*  56 */ "17, 13, 18, 16, 18, 15, 19, 17, 20, 20, 20, 19, 20, 18, 20, 21, 21, -1, 22, 22, 51, 24, 50, 23, 23",
      /*  81 */ "-1, 24, 26, 24, 25, 25, 27, 25, -1, 53, 29, 52, 28, 26, -1, 27, 30, 28, 30, 54, 31, 29, 32, 30, -1",
      /* 106 */ "55, 33, 31, -1, 56, 34, 32, -1, 33, -1, 34, 38, 34, 37, 34, 36, 34, 35, 35, -1, 36, 39, 37, 40, 57",
      /* 131 */ "41, 38, -1, 39, 42, 39, -1, 40, 43, 59, 45, 58, 44, 41, -1"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 146; ++i) {REDUCTION[i] = Integer.parseInt(s2[i]);}
  }

  private static final String[] TOKEN =
  {
    "%ERROR",
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
    "end-of-input",
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
    "IMPLICIT-56",
    "IMPLICIT-57",
    "IMPLICIT-58",
    "IMPLICIT-59"
  };

                                                            // line 967 "ixml.ebnf"
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
                                                                                                              : de.bottlecaps.markup.blitz.codepoints.Codepoint.toString(de.bottlecaps.markup.blitz.codepoints.Codepoint.EOI),
                                                                    getExpectedTokenSet(pe),
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
                                                            // line 1783 "Ixml.java"
// End
