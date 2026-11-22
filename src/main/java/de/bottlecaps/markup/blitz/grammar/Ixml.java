// This file was generated on Sun Nov 22, 2026 20:28 (UTC) by REx v7.0 which is Copyright (c) 1979-2026 by Gunther Rademacher <grd@gmx.net>
// REx command line: ixml.ebnf -glalr 1 -java -a java -name de.bottlecaps.markup.blitz.grammar.Ixml

package de.bottlecaps.markup.blitz.grammar;

import java.util.PriorityQueue;

@SuppressWarnings("all")
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
        int pos = -1;
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
            pos = b1;
            consume(l1);
          }
          else
          {
            pos = 0;
          }
        }

        if (reduce < 0)
        {
          stack = new StackNode(state, lookback, pos, stack);
          state = shift;
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
            int n = symbols;
            if (shift >= 0)
            {
              n -= 1;
            }
            if (n > 0)
            {
              for (int i = 1; i < n; i++)
              {
                stack = stack.link;
              }
              state = stack.state;
              stack = stack.link;
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
                                                            // line 492 "Ixml.java"
        }
        break;
      case 1:
        {
                                                            // line 5 "ixml.ebnf"
                                                            de.bottlecaps.markup.blitz.Errors.S01.thro();
                                                            // line 499 "Ixml.java"
        }
        break;
      case 2:
        {
                                                            // line 14 "ixml.ebnf"
                                                            grammar = new Grammar(stringBuilder.toString());
                                                            // line 506 "Ixml.java"
        }
        break;
      case 3:
        {
                                                            // line 18 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            grammar.addRule(new Rule(mark, alias, name, alts.peek()));
                                                            // line 514 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 21 "ixml.ebnf"
                                                            alts.pop();
                                                            // line 521 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 23 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 528 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 24 "ixml.ebnf"
                                                            nameBuilder.append(input.subSequence(b0, e0));
                                                            // line 535 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 27 "ixml.ebnf"
                                                            name = nameBuilder.toString();
                                                            alias = null;
                                                            // line 543 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 39 "ixml.ebnf"
                                                            if (! grammar.getVersion().isAtLeast(Grammar.Version.V1_1))
                                                              de.bottlecaps.markup.blitz.Errors.S12.thro(grammar.getVersion() + " does not support renaming");
                                                            savedName = name;
                                                            // line 552 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 44 "ixml.ebnf"
                                                            alias = name;
                                                            name = savedName;
                                                            // line 560 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 48 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 567 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 58 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 574 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 60 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 582 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 64 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 590 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 68 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 599 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 74 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 607 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 78 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 616 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 84 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 624 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 87 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 631 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 88 "ixml.ebnf"
                                                            mark = Mark.NODE;
                                                            // line 638 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 89 "ixml.ebnf"
                                                            mark = Mark.DELETE;
                                                            // line 645 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 90 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 652 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 93 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, alias, name);
                                                            // line 659 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 94 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 666 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 97 "ixml.ebnf"
                                                            alts.peek().last().addCharset(new Charset(deleted, exclusion, members));
                                                            // line 673 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 100 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 680 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 101 "ixml.ebnf"
                                                            alts.peek().last().addCodepoint(deleted, codepoint);
                                                            // line 687 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 103 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 694 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 104 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 701 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 105 "ixml.ebnf"
                                                            stringBuilder.append(input.subSequence(b0, e0));
                                                            // line 708 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 110 "ixml.ebnf"
                                                            validateStringChar(input.toString().codePointAt(b0));
                                                            // line 715 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 112 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 722 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 113 "ixml.ebnf"
                                                            codepoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 729 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 118 "ixml.ebnf"
                                                            exclusion = false;
                                                            members = new java.util.ArrayList<>();
                                                            // line 737 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 123 "ixml.ebnf"
                                                            exclusion = true;
                                                            members = new java.util.ArrayList<>();
                                                            // line 745 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 128 "ixml.ebnf"
                                                            members.add(new StringMember(stringBuilder.toString(), false));
                                                            // line 752 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 129 "ixml.ebnf"
                                                            members.add(new StringMember(codepoint, true));
                                                            // line 759 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 130 "ixml.ebnf"
                                                            members.add(new RangeMember(firstCodepoint, lastCodepoint));
                                                            // line 766 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 131 "ixml.ebnf"
                                                            members.add(new ClassMember(clazz));
                                                            // line 773 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 133 "ixml.ebnf"
                                                            firstCodepoint = codepoint;
                                                            // line 780 "Ixml.java"
        }
        break;
      case 40:
        {
                                                            // line 134 "ixml.ebnf"
                                                            lastCodepoint = codepoint;
                                                            // line 787 "Ixml.java"
        }
        break;
      case 41:
        {
                                                            // line 135 "ixml.ebnf"
                                                            codepoint = input.subSequence(b0, e0).toString();
                                                            // line 794 "Ixml.java"
        }
        break;
      case 42:
        {
                                                            // line 140 "ixml.ebnf"
                                                            clazz += input.subSequence(b0, e0);
                                                            // line 801 "Ixml.java"
        }
        break;
      case 43:
        {
                                                            // line 146 "ixml.ebnf"
                                                            clazz = input.subSequence(b0, e0).toString();
                                                            // line 808 "Ixml.java"
        }
        break;
      case 44:
        {
                                                            // line 148 "ixml.ebnf"
                                                            Term operand = alts.peek().last().removeLast();
                                                            alts.peek().last().addNot(operand);
                                                            // line 816 "Ixml.java"
        }
        break;
      case 45:
        {
                                                            // line 151 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 823 "Ixml.java"
        }
        break;
      case 46:
        {
                                                            // line 152 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codepoint);
                                                            // line 830 "Ixml.java"
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
    for (int i = 0; i < 72; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 74 + s - 1;
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

  private static void decode(String[] chunks, int[] target)
  {
    int n = 0;
    for (String chunk : chunks)
    {
      int value = 0;
      boolean negative = false;
      for (int i = 0, length = chunk.length(); i < length; ++i)
      {
        char c = chunk.charAt(i);
        if (c >= '0' && c <= '9')
        {
          value = value * 10 + (c - '0');
        }
        else if (c == '-')
        {
          negative = true;
        }
        else if (c == ',')
        {
          target[n++] = negative ? - value : value;
          value = 0;
          negative = false;
        }
      }
      target[n++] = negative ? - value : value;
    }
  }

  private static final int[] MAP0 = new int[128];
  static
  {
    final String s1[] =
    {
      /*   0 */ "68, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3",
      /*  34 */ "4, 5, 1, 1, 1, 6, 7, 8, 9, 10, 11, 12, 13, 1, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 16, 1, 17",
      /*  62 */ "18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42",
      /*  87 */ "43, 44, 45, 46, 47, 1, 48, 49, 50, 1, 51, 51, 51, 51, 52, 51, 53, 53, 54, 53, 53, 55, 56, 57, 58, 53",
      /* 113 */ "53, 59, 60, 53, 53, 61, 53, 62, 53, 53, 63, 64, 65, 66, 1"
    };
    decode(s1, MAP0);
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
      /* 1569 */ "2317, 1580, 2125, 1843, 2125, 1811, 1861, 1861, 1861, 1863, 68, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 2",
      /* 1592 */ "1, 1, 2, 1, 1, 1, 1, 50, 50, 1, 50, 2, 3, 4, 5, 1, 1, 1, 6, 7, 8, 9, 10, 11, 12, 13, 1, 1, 1, 1, 1",
      /* 1623 */ "50, 1, 67, 67, 67, 67, 1, 1, 50, 50, 50, 50, 14, 14, 14, 14, 14, 14, 14, 14, 15, 16, 1, 17, 18, 19",
      /* 1649 */ "20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44",
      /* 1674 */ "45, 46, 47, 1, 48, 49, 50, 1, 50, 1, 1, 1, 50, 50, 1, 50, 1, 1, 50, 1, 1, 1, 51, 51, 51, 51, 52, 51",
      /* 1702 */ "53, 53, 59, 60, 53, 53, 61, 53, 54, 53, 53, 55, 56, 57, 58, 62, 53, 53, 63, 64, 65, 66, 1, 1, 1, 1",
      /* 1728 */ "1, 67, 1, 67, 1, 1, 67, 67, 67, 1, 1, 1, 1, 50, 50, 50, 50, 1, 67, 67, 67, 67, 67, 1, 67, 1, 67, 67",
      /* 1756 */ "1, 67, 67, 1, 67, 1, 1, 50, 50, 1, 50, 50, 50, 1, 50, 1, 50, 50, 50, 50, 50, 50, 1, 50, 67, 67, 67",
      /* 1783 */ "67, 67, 67, 67, 1, 67, 67, 67, 67, 67, 67, 67, 50, 50, 67, 1, 67, 50, 1, 1, 1, 50, 50, 1, 1, 1, 1, 1",
      /* 1811 */ "67, 67, 50, 50, 50, 50, 50, 50, 67, 67, 67, 67, 67, 67, 67, 50, 67, 67, 67, 67, 67, 67, 1, 50, 50",
      /* 1836 */ "50, 67, 67, 1, 1, 67, 67, 67, 67, 1, 1, 1, 50, 50, 50, 50, 67, 50, 50, 1, 1, 67, 50, 1, 1, 50, 50",
      /* 1863 */ "50, 50, 50, 50, 50, 50, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 67, 50, 1, 50, 50, 50, 50, 1, 1, 1, 1, 50",
      /* 1892 */ "1, 67, 67, 50, 50, 50, 67, 1, 1, 67, 67, 1, 50, 50, 1, 1, 67, 50, 1, 67, 67, 1, 1, 67, 67, 1, 1, 67",
      /* 1920 */ "67, 50, 50, 50, 50, 1, 1, 1, 50, 50, 1, 50, 1, 50, 50, 1, 50, 50, 1, 50, 50, 50, 50, 1, 50, 1, 50",
      /* 1947 */ "67, 50, 50, 67, 67, 67, 67, 50, 50, 50, 1, 1, 50, 1, 1, 67, 67, 1, 67, 67, 50, 1, 1, 1, 1, 1, 1, 50",
      /* 1975 */ "50, 50, 50, 50, 50, 50, 50, 1, 1, 67, 67, 67, 1, 67, 1, 1, 1, 67, 67, 50, 50, 50, 50, 67, 67, 1, 67",
      /* 2002 */ "67, 67, 1, 1, 1, 67, 1, 1, 1, 50, 50, 50, 1, 1, 2, 50, 50, 50, 50, 50, 50, 50, 1, 50, 50, 50, 50, 50",
      /* 2030 */ "50, 1, 50, 1, 1, 50, 50, 67, 67, 67, 1, 1, 1, 1, 67, 50, 50, 67, 67, 1, 1, 1, 1, 1, 67, 67, 67, 67",
      /* 2058 */ "67, 1, 1, 1, 1, 50, 67, 1, 1, 1, 1, 1, 1, 67, 67, 1, 1, 50, 50, 50, 50, 50, 1, 67, 1, 1, 1, 67, 67",
      /* 2087 */ "67, 2, 1, 67, 67, 67, 67, 1, 50, 50, 50, 50, 50, 50, 50, 50, 50, 67, 1, 67, 67, 67, 67, 50, 50, 1, 1",
      /* 2114 */ "1, 1, 50, 50, 67, 67, 1, 1, 1, 67, 1, 67, 67, 67, 67, 67, 67, 67, 67, 1, 1, 1, 1, 1, 1, 1, 67, 67",
      /* 2142 */ "67, 1, 67, 67, 67, 67, 1, 1, 1, 50, 67, 50, 50, 50, 50, 67, 50, 50, 50, 50, 50, 50, 50, 67, 1, 50",
      /* 2168 */ "50, 1, 1, 67, 50, 50, 1, 1, 1, 50, 67, 67, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 50, 1, 1, 1",
      /* 2199 */ "1, 50, 1, 1, 1, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 50, 50, 50, 50, 1, 1, 50, 50, 50, 50, 1, 1, 50",
      /* 2227 */ "50, 50, 50, 1, 1, 50, 50, 50, 1, 1, 67, 67, 1, 1, 50, 50, 50, 1, 67, 1, 1, 1, 1, 50, 1, 1, 1, 1, 1",
      /* 2256 */ "1, 1, 67, 1, 1, 1, 67, 67, 1, 1, 67, 1, 1, 1, 1, 1, 1, 50, 50, 50, 50, 67, 1, 1, 1, 1, 67, 1, 1, 50",
      /* 2286 */ "50, 50, 1, 50, 50, 67, 50, 67, 67, 67, 50, 50, 67, 50, 50, 50, 67, 50, 1, 1, 1, 1, 1, 1, 67, 50, 50",
      /* 2313 */ "50, 50, 50, 67, 67, 67, 67, 67, 1, 1, 67, 67, 50, 50, 50, 1, 67, 67, 1, 1, 1, 1, 67, 50, 50, 50, 1",
      /* 2340 */ "1, 67, 1, 1, 1"
    };
    decode(s1, MAP1);
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
      /*  718 */ "655357, 720893, 786429, 851965, 917501, 917759, 917999, 983037, 1048573, 1114109, 1, 50, 1, 50, 1",
      /*  733 */ "50, 1, 50, 1, 50, 67, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1",
      /*  761 */ "67, 1, 67, 1, 50, 1, 50, 1, 67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50",
      /*  790 */ "1, 50, 1, 50, 1, 50, 1, 50, 1, 67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1",
      /*  819 */ "67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 67, 1, 67, 1, 67",
      /*  847 */ "50, 1, 50, 1, 50, 1, 67, 1, 67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 67, 1, 50, 67, 1, 67, 1, 67, 1",
      /*  876 */ "50, 1, 67, 1, 67, 1, 50, 1, 67, 1, 67, 50, 67, 1, 67, 1, 67, 1, 67, 1, 50, 1, 67, 1, 50, 1, 67, 1",
      /*  904 */ "50, 67, 1, 67, 1, 67, 1, 67, 1, 67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 67, 50, 1, 50, 1, 67, 1",
      /*  932 */ "67, 1, 67, 1, 67, 1, 67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50",
      /*  961 */ "1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1",
      /*  990 */ "50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 67, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50",
      /* 1019 */ "1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1",
      /* 1048 */ "50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 50, 1, 1",
      /* 1077 */ "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 67, 1, 1, 1"
    };
    decode(s1, MAP2);
  }

  private static final int[] INITIAL = new int[71];
  static
  {
    final String s1[] =
    {
      /*  0 */ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28",
      /* 28 */ "29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54",
      /* 54 */ "55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71"
    };
    decode(s1, INITIAL);
  }

  private static final int[] TRANSITION = new int[2103];
  static
  {
    final String s1[] =
    {
      /*    0 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*   17 */ "1212, 1285, 1439, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*   34 */ "1104, 1123, 1118, 1144, 1131, 1135, 1143, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285",
      /*   51 */ "1439, 1212, 1212, 1212, 1110, 1152, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1190, 1212, 1988, 1171",
      /*   68 */ "1994, 1189, 1184, 1198, 1199, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1207, 1221",
      /*   85 */ "1211, 1237, 1256, 1258, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1921, 1212, 1912, 1266, 1918, 1920",
      /*  102 */ "1279, 1305, 1306, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1212, 1212, 1212",
      /*  119 */ "1159, 1163, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1228, 1212, 1285, 1479, 1229, 1485, 1212, 1212",
      /*  136 */ "1314, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1212, 1333, 1212, 1212, 1360",
      /*  153 */ "1341, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1212, 1350, 1212, 1321, 1325, 1358",
      /*  170 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1871, 1368, 1212, 1212, 1868, 1212, 1212",
      /*  187 */ "1212, 1212, 1212, 1212, 1212, 1243, 1212, 1285, 1376, 1243, 1248, 1443, 1391, 1392, 1212, 1212, 1212",
      /*  204 */ "1212, 1212, 1212, 1212, 1380, 1212, 1285, 1400, 1383, 1414, 1212, 1212, 1406, 1212, 1212, 1212, 1212",
      /*  221 */ "1212, 1212, 1212, 1422, 1212, 1285, 1439, 1212, 1297, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  238 */ "1212, 1212, 1212, 1212, 1799, 1432, 1212, 1898, 1212, 1212, 1748, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  255 */ "1212, 1212, 1212, 1285, 1451, 1455, 1463, 1212, 1212, 1464, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  272 */ "1212, 1212, 1813, 1472, 1212, 1176, 1212, 1212, 2060, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1423",
      /*  289 */ "1212, 1285, 1439, 1212, 1271, 1212, 1212, 1424, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  306 */ "1285, 1439, 1212, 1493, 1212, 1212, 1823, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285",
      /*  323 */ "1439, 1212, 1212, 1974, 1501, 1502, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422, 1212, 1285, 1439",
      /*  340 */ "1969, 1297, 1510, 1510, 1511, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422, 1212, 1285, 1439, 1969",
      /*  357 */ "1297, 1519, 1519, 1520, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422, 1212, 1285, 1439, 1969, 1297",
      /*  374 */ "1528, 1528, 1529, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422, 1212, 1285, 1439, 1969, 1297, 1537",
      /*  391 */ "1537, 1538, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422, 1212, 1285, 1439, 1969, 1297, 1546, 1546",
      /*  408 */ "1547, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422, 1212, 1285, 1439, 1969, 1297, 1555, 1555, 1556",
      /*  425 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1564, 1564, 1565, 1212",
      /*  442 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1573, 1573, 1574, 1212, 1212",
      /*  459 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1582, 1582, 1583, 1212, 1212, 1212",
      /*  476 */ "1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1591, 1591, 1592, 1212, 1212, 1212, 1212",
      /*  493 */ "1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1600, 1600, 1601, 1212, 1212, 1212, 1212, 1212",
      /*  510 */ "1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1609, 1609, 1610, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  527 */ "1212, 1212, 1212, 1285, 1439, 1969, 1342, 1618, 1618, 1619, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  544 */ "1212, 1212, 1285, 1439, 1969, 1342, 1627, 1627, 1628, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  561 */ "1212, 1285, 1439, 1969, 1342, 1636, 1636, 1637, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  578 */ "1285, 1439, 1969, 1342, 1645, 1645, 1646, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285",
      /*  595 */ "1439, 1969, 1342, 1654, 1654, 1655, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439",
      /*  612 */ "1969, 1342, 1663, 1663, 1664, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969",
      /*  629 */ "1342, 1672, 1672, 1673, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342",
      /*  646 */ "1681, 1681, 1682, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1690",
      /*  663 */ "1690, 1691, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1699, 1699",
      /*  680 */ "1700, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1708, 1708, 1709",
      /*  697 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1717, 1717, 1718, 1212",
      /*  714 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1726, 1726, 1727, 1212, 1212",
      /*  731 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1285, 1439, 1969, 1342, 1735, 1735, 1736, 1212, 1212, 1212",
      /*  748 */ "1212, 1212, 1212, 1212, 1212, 1762, 1285, 1744, 1756, 1762, 1212, 1771, 1773, 1212, 1212, 1212, 1212",
      /*  765 */ "1212, 1212, 1212, 1212, 1212, 1285, 1781, 1785, 1793, 1807, 1212, 1857, 1212, 1212, 1212, 1212, 1212",
      /*  782 */ "1212, 1212, 1212, 1212, 1285, 1439, 1212, 1821, 1940, 1831, 1832, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  799 */ "1212, 1212, 1212, 1285, 1439, 1212, 1342, 1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  816 */ "1422, 1212, 1285, 1439, 1969, 1297, 1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1422",
      /*  833 */ "1855, 1285, 1439, 1969, 1297, 1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /*  850 */ "1285, 1439, 1969, 1342, 1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1865, 1285",
      /*  867 */ "1439, 1969, 1342, 1879, 1885, 1886, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1894, 1285, 1439",
      /*  884 */ "1969, 1342, 1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1935, 1285, 1439, 1969",
      /*  901 */ "1342, 1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1291, 1285, 1439, 1969, 1342",
      /*  918 */ "1840, 1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 2076, 1285, 1439, 1969, 1342, 1840",
      /*  935 */ "1846, 1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1213, 1285, 1439, 1969, 1342, 1840, 1846",
      /*  952 */ "1847, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1906, 1439, 1969, 1342, 1840, 1846, 1847",
      /*  969 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1929, 1439, 1969, 1342, 1948, 1954, 1955, 1212",
      /*  986 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1963, 1439, 1969, 1763, 1840, 1846, 1847, 1212, 1212",
      /* 1003 */ "1212, 1212, 1212, 1212, 1212, 1212, 1212, 1982, 2027, 2002, 2004, 2012, 2016, 2024, 1212, 1212, 1212",
      /* 1020 */ "1212, 1212, 1212, 1212, 1212, 1212, 1285, 2035, 2039, 2047, 1212, 1212, 2048, 1212, 1212, 1212, 1212",
      /* 1037 */ "1212, 1212, 1212, 1212, 1212, 1285, 2056, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /* 1054 */ "1212, 1212, 1212, 1212, 1285, 1439, 2068, 2074, 1212, 2084, 2086, 1212, 1212, 1212, 1212, 1212, 1212",
      /* 1071 */ "1212, 1212, 1212, 1285, 1439, 1212, 1342, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212",
      /* 1088 */ "2094, 1212, 1212, 1212, 1212, 1212, 1212, 2095, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 1212, 0, 0",
      /* 1106 */ "0, 5960, 512, 640, 0, 0, 0, 0, 0, 6528, 0, 6528, 5960, 5960, 0, 0, 5960, 5960, 5960, 5960, 256, 0, 0",
      /* 1129 */ "5960, 0, 0, 0, 0, 5960, 5960, 5960, 0, 5960, 0, 0, 5960, 5960, 0, 0, 0, 5960, 5960, 5960, 5960, 5960",
      /* 1151 */ "5960, 6528, 6528, 6528, 6528, 6528, 6528, 6528, 0, 0, 0, 0, 7040, 7040, 7040, 7040, 7040, 7040, 7040",
      /* 1170 */ "0, 0, 0, 0, 256, 6656, 0, 0, 0, 0, 0, 0, 8064, 8064, 0, 6656, 6656, 0, 6656, 6656, 0, 0, 6656, 0, 0",
      /* 1195 */ "0, 0, 0, 0, 6656, 6656, 6656, 6656, 6656, 6656, 6656, 0, 0, 0, 0, 256, 6784, 0, 0, 0, 0, 0, 0, 0, 0",
      /* 1220 */ "5376, 6784, 0, 6784, 0, 0, 0, 6784, 0, 0, 0, 0, 7168, 0, 0, 0, 7168, 0, 6784, 6784, 0, 6784, 6784, 0",
      /* 1244 */ "0, 0, 0, 0, 7552, 0, 0, 0, 0, 7552, 0, 6016, 0, 0, 6784, 6784, 6784, 6784, 6784, 6784, 6784, 0, 0, 0",
      /* 1268 */ "0, 256, 6912, 0, 0, 0, 0, 0, 0, 8192, 8192, 0, 6912, 6912, 0, 6912, 6912, 0, 0, 0, 0, 512, 640, 0, 0",
      /* 1293 */ "0, 0, 0, 5120, 0, 0, 0, 0, 0, 6144, 0, 6016, 0, 6912, 6912, 6912, 6912, 6912, 6912, 6912, 0, 7168, 0",
      /* 1316 */ "7168, 7168, 0, 7168, 7168, 0, 0, 0, 0, 7296, 7296, 7296, 7296, 7296, 7296, 7370, 0, 0, 841, 841, 841",
      /* 1337 */ "841, 841, 841, 841, 896, 0, 0, 0, 0, 0, 0, 0, 6016, 0, 7370, 7370, 7370, 7370, 7370, 7370, 7370, 0",
      /* 1359 */ "1024, 0, 0, 0, 0, 0, 0, 841, 0, 0, 7424, 7424, 7424, 7424, 7424, 7424, 7424, 7552, 0, 0, 256, 0, 0",
      /* 1382 */ "0, 0, 0, 0, 7680, 0, 0, 0, 7680, 7552, 7552, 7552, 7552, 7552, 7552, 7552, 7552, 0, 0, 7680, 0, 256",
      /* 1404 */ "0, 0, 0, 7680, 7680, 0, 7680, 7680, 7680, 0, 0, 7680, 7680, 7680, 7680, 7680, 7680, 7680, 6144, 0, 0",
      /* 1425 */ "0, 0, 0, 0, 0, 8192, 0, 0, 0, 0, 256, 0, 0, 7808, 0, 0, 0, 256, 0, 0, 0, 0, 0, 0, 7552, 7552, 0, 0",
      /* 1453 */ "0, 256, 0, 7936, 0, 7936, 7936, 7936, 0, 7936, 0, 7936, 7936, 7936, 7936, 7936, 7936, 7936, 0, 0, 0",
      /* 1474 */ "0, 256, 0, 0, 8064, 0, 0, 0, 256, 0, 0, 0, 7168, 7168, 7168, 7168, 7168, 7168, 7168, 0, 1152, 1152",
      /* 1496 */ "1152, 1152, 1152, 1152, 1152, 8320, 8320, 8320, 8320, 8320, 8320, 8320, 8320, 0, 1280, 1280, 1280",
      /* 1513 */ "1280, 1280, 1280, 1280, 1280, 0, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 1408, 0, 1536, 1536, 1536",
      /* 1531 */ "1536, 1536, 1536, 1536, 1536, 0, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 0, 1792, 1792, 1792",
      /* 1549 */ "1792, 1792, 1792, 1792, 1792, 0, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 0, 2048, 2048, 2048",
      /* 1567 */ "2048, 2048, 2048, 2048, 2048, 0, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 0, 2304, 2304, 2304",
      /* 1585 */ "2304, 2304, 2304, 2304, 2304, 0, 2432, 2432, 2432, 2432, 2432, 2432, 2432, 2432, 0, 2560, 2560, 2560",
      /* 1603 */ "2560, 2560, 2560, 2560, 2560, 0, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 0, 2816, 2816, 2816",
      /* 1621 */ "2816, 2816, 2816, 2816, 2816, 0, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 2944, 0, 3072, 3072, 3072",
      /* 1639 */ "3072, 3072, 3072, 3072, 3072, 0, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 3200, 0, 3328, 3328, 3328",
      /* 1657 */ "3328, 3328, 3328, 3328, 3328, 0, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 0, 3584, 3584, 3584",
      /* 1675 */ "3584, 3584, 3584, 3584, 3584, 0, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 0, 3840, 3840, 3840",
      /* 1693 */ "3840, 3840, 3840, 3840, 3840, 0, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 0, 4096, 4096, 4096",
      /* 1711 */ "4096, 4096, 4096, 4096, 4096, 0, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 4224, 0, 4352, 4352, 4352",
      /* 1729 */ "4352, 4352, 4352, 4352, 4352, 0, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 4480, 0, 0, 0, 8448, 256",
      /* 1748 */ "0, 0, 0, 0, 0, 0, 7808, 0, 0, 0, 8448, 0, 0, 0, 8448, 0, 0, 0, 0, 0, 0, 0, 8832, 0, 0, 8448, 8448",
      /* 1775 */ "8448, 8448, 8448, 8448, 8448, 0, 0, 0, 0, 256, 0, 4608, 0, 0, 4608, 4608, 0, 0, 0, 0, 0, 4608, 4608",
      /* 1798 */ "4608, 0, 0, 0, 0, 512, 640, 0, 7808, 0, 0, 4608, 0, 0, 4608, 0, 0, 0, 0, 512, 640, 0, 8064, 8576, 0",
      /* 1823 */ "0, 0, 0, 0, 0, 0, 1152, 0, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 0, 384, 0, 0, 384, 0, 0",
      /* 1846 */ "384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 4736, 0, 0, 0, 0, 0, 0, 4608, 0, 0, 0, 8704, 0, 0, 0",
      /* 1871 */ "0, 0, 0, 7424, 0, 0, 0, 7424, 8704, 0, 0, 8704, 0, 0, 8704, 8704, 8704, 8704, 8704, 8704, 8704, 8704",
      /* 1893 */ "0, 0, 0, 0, 4864, 0, 0, 0, 0, 0, 0, 7808, 7808, 5504, 0, 0, 0, 512, 640, 0, 0, 0, 0, 512, 6912, 6912",
      /* 1919 */ "0, 6912, 0, 0, 0, 6912, 0, 0, 0, 0, 0, 5632, 0, 0, 512, 640, 0, 0, 0, 0, 4992, 0, 0, 0, 0, 0, 0",
      /* 1946 */ "8576, 8576, 5632, 0, 0, 5632, 0, 0, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 5632, 0, 0, 0, 8832, 0",
      /* 1967 */ "512, 640, 0, 0, 0, 0, 6272, 0, 0, 0, 0, 0, 0, 8320, 8320, 0, 0, 0, 8960, 512, 640, 0, 0, 0, 0, 6656",
      /* 1993 */ "640, 6656, 0, 6656, 0, 0, 0, 6656, 0, 8960, 8960, 0, 0, 8960, 8960, 8960, 8960, 8960, 8960, 0, 0, 0",
      /* 2015 */ "8960, 8960, 8960, 0, 8960, 0, 0, 8960, 8960, 0, 0, 0, 8960, 8960, 8960, 8960, 0, 0, 8960, 0, 0, 0, 0",
      /* 2038 */ "256, 0, 9088, 0, 9088, 9088, 9088, 0, 9088, 0, 9088, 9088, 9088, 9088, 9088, 9088, 9088, 0, 0, 0, 0",
      /* 2059 */ "5760, 0, 0, 0, 0, 0, 0, 8064, 0, 0, 0, 9216, 0, 0, 0, 9216, 0, 0, 0, 0, 0, 0, 0, 5248, 0, 0, 0, 9216",
      /* 2087 */ "9216, 9216, 9216, 9216, 9216, 9216, 0, 0, 6400, 0, 0, 0, 0, 0, 0, 0"
    };
    decode(s1, TRANSITION);
  }

  private static final int[] EXPECTED = new int[197];
  static
  {
    final String s1[] =
    {
      /*   0 */ "58, 58, 58, 58, 58, 56, 59, 58, 58, 58, 63, 64, 68, 69, 73, 73, 73, 75, 193, 89, 79, 191, 83, 87, 93",
      /*  25 */ "97, 101, 105, 109, 113, 117, 121, 125, 129, 133, 137, 141, 58, 58, 147, 58, 143, 58, 153, 163, 167",
      /*  46 */ "171, 175, 177, 149, 156, 159, 181, 185, 188, 58, 8, 16, 0, 0, 0, 0, 2, 0, 480, 480, 480, 480, -508",
      /*  69 */ "-512, -512, -508, -508, -508, -508, -508, -508, -28, 0, 134217728, -2147483648, 0, 16, 256, 512, 1024",
      /*  86 */ "2048, 0, 8192, 524288, 2097152, 8388608, 67108864, 2621440, 1342177280, 67117056, 134225920, 8192",
      /*  98 */ "4096, 3670016, 536870920, 1342185472, 679477248, 3678208, 536879112, 3670016, 713031680, 536944648",
      /* 108 */ "603987976, 3678208, 713039872, 70778880, 729808896, 729817088, 730341384, 731914248, 796958728",
      /* 117 */ "-75489280, -75472896, 2055, 3670023, 3670031, 10247, 3678215, 3678223, 67110919, 67119111, 67250183",
      /* 128 */ "69740551, 70780935, 70789127, 91752455, 92014599, 91760647, 92022791, 637274119, 763103239, 771491847",
      /* 138 */ "637282311, 763111431, 771500039, -251889, 8192, 0, 0, 16, 32, 2, 0, 8, 0, 0, 40, 32, 32, 34, 32, 32",
      /* 158 */ "13, 45, 45, 143, 175, 0, 64, 32, 64, 32, 96, 130, 64, 96, 96, 162, 96, 134, 64, 96, 96, 96, 112, 143",
      /* 182 */ "143, 175, 175, 207, 207, 207, 239, 239, 239, 0, 32, 64, 128, 32768, 131072"
    };
    decode(s1, EXPECTED);
  }

  private static final int[] CASEID = new int[1780];
  static
  {
    final String s1[] =
    {
      /*    0 */ "632, 641, 595, 630, 599, 1022, 607, 611, 1020, 620, 625, 641, 636, 640, 628, 645, 709, 621, 709",
      /*   19 */ "1018, 650, 653, 657, 684, 669, 709, 709, 646, 709, 1018, 772, 674, 678, 770, 682, 709, 709, 670, 710",
      /*   39 */ "614, 688, 691, 695, 709, 697, 632, 641, 595, 709, 702, 632, 641, 595, 709, 702, 632, 641, 595, 709",
      /*   59 */ "702, 1022, 607, 611, 1020, 708, 724, 714, 718, 722, 728, 650, 653, 657, 684, 669, 724, 714, 734, 764",
      /*   79 */ "728, 709, 709, 1126, 962, 1018, 660, 709, 1080, 912, 663, 738, 741, 745, 709, 747, 758, 709, 748",
      /*   98 */ "947, 795, 709, 709, 646, 709, 1018, 632, 641, 595, 752, 628, 632, 641, 595, 709, 702, 792, 709, 815",
      /*  118 */ "978, 838, 776, 785, 789, 801, 779, 805, 808, 812, 709, 814, 758, 709, 748, 947, 795, 650, 653, 657",
      /*  138 */ "819, 823, 688, 691, 695, 709, 697, 632, 641, 595, 829, 628, 835, 709, 1126, 767, 847, 709, 709, 709",
      /*  158 */ "844, 853, 632, 641, 595, 859, 628, 709, 709, 1126, 863, 1018, 632, 641, 595, 869, 897, 776, 785, 789",
      /*  178 */ "916, 779, 632, 641, 595, 920, 897, 709, 709, 1126, 761, 847, 632, 641, 595, 951, 897, 709, 709, 1126",
      /*  198 */ "761, 847, 632, 641, 595, 951, 897, 709, 709, 1126, 761, 847, 835, 709, 1126, 767, 847, 709, 709",
      /*  217 */ "1126, 863, 832, 875, 709, 1085, 893, 878, 709, 709, 709, 709, 961, 709, 709, 1103, 709, 709, 709",
      /*  236 */ "709, 1126, 709, 755, 966, 982, 986, 1002, 969, 709, 709, 709, 1006, 709, 650, 653, 657, 1026, 1033",
      /*  255 */ "650, 653, 657, 1043, 1033, 650, 653, 657, 1056, 1033, 650, 653, 657, 1056, 1033, 709, 709, 709, 1006",
      /*  274 */ "1069, 1079, 709, 709, 1084, 709, 900, 709, 709, 903, 709, 616, 641, 1089, 863, 1018, 709, 709, 709",
      /*  293 */ "709, 1093, 776, 785, 789, 1098, 779, 923, 709, 1153, 1075, 926, 929, 709, 1144, 1065, 932, 1102, 709",
      /*  312 */ "709, 1102, 709, 909, 709, 709, 906, 709, 781, 1107, 1111, 1116, 709, 709, 709, 709, 709, 961, 835",
      /*  331 */ "709, 1126, 767, 847, 1079, 709, 709, 1120, 709, 900, 709, 709, 935, 709, 709, 709, 603, 698, 847",
      /*  350 */ "709, 709, 1126, 630, 1018, 1079, 709, 709, 1125, 709, 900, 709, 709, 938, 709, 835, 709, 1126, 767",
      /*  369 */ "847, 709, 709, 646, 709, 1018, 709, 709, 709, 1130, 709, 709, 709, 709, 1012, 709, 709, 709, 709",
      /*  388 */ "1135, 709, 616, 641, 1139, 863, 1018, 709, 709, 1126, 863, 1018, 709, 709, 1094, 797, 941, 781, 1107",
      /*  407 */ "1143, 1116, 709, 709, 709, 709, 1148, 709, 709, 709, 1126, 601, 1018, 709, 709, 603, 698, 847, 709",
      /*  426 */ "709, 709, 709, 944, 709, 709, 890, 709, 709, 709, 709, 709, 1152, 709, 709, 709, 954, 709, 709, 709",
      /*  446 */ "709, 709, 1112, 709, 709, 709, 709, 1162, 709, 709, 709, 825, 709, 709, 709, 709, 972, 709, 709, 709",
      /*  466 */ "709, 709, 865, 709, 709, 709, 709, 1039, 887, 709, 709, 849, 709, 709, 709, 709, 709, 998, 1009, 975",
      /*  486 */ "709, 709, 1029, 1036, 709, 709, 730, 709, 709, 709, 709, 709, 1157, 709, 709, 709, 709, 709, 1161",
      /*  505 */ "709, 709, 1121, 709, 709, 709, 709, 855, 709, 709, 709, 709, 709, 1166, 709, 709, 709, 709, 989, 709",
      /*  525 */ "992, 709, 1170, 957, 995, 709, 709, 709, 872, 709, 709, 709, 881, 709, 709, 709, 709, 1174, 665",
      /*  544 */ "1015, 709, 709, 1178, 1182, 1046, 1049, 709, 1186, 1190, 1059, 1049, 709, 1186, 1052, 1059, 709, 709",
      /*  562 */ "1194, 1131, 884, 709, 709, 709, 704, 709, 709, 709, 709, 1198, 709, 709, 709, 709, 1062, 709, 709",
      /*  581 */ "709, 709, 1202, 709, 709, 709, 709, 1072, 709, 709, 709, 709, 840, 709, 1369, 1307, 1303, 1694, 1367",
      /*  600 */ "1719, 1307, 1307, 1303, 1307, 1307, 1694, 1206, 1206, 1206, 1206, 1211, 1307, 1308, 1307, 1271, 1307",
      /*  617 */ "1307, 1368, 1362, 1215, 1307, 1307, 1307, 1229, 1304, 1368, 1362, 1362, 1219, 1307, 1307, 1304, 1307",
      /*  634 */ "1368, 1362, 1362, 1307, 1303, 1694, 1360, 1362, 1362, 1362, 1362, 1223, 1307, 1307, 1307, 1247, 1399",
      /*  651 */ "1307, 1235, 1243, 1243, 1243, 1243, 1236, 1307, 1398, 1307, 1295, 1302, 1307, 1300, 1307, 1307, 1488",
      /*  668 */ "1706, 1240, 1307, 1307, 1307, 1271, 1255, 1255, 1255, 1255, 1260, 1307, 1230, 1264, 1258, 1719, 1307",
      /*  685 */ "1307, 1354, 1307, 1631, 1307, 1275, 1280, 1280, 1280, 1280, 1276, 1307, 1630, 1307, 1307, 1307, 1305",
      /*  702 */ "1303, 1719, 1307, 1307, 1455, 1307, 1209, 1307, 1307, 1307, 1307, 1270, 1284, 1284, 1284, 1284, 1289",
      /*  719 */ "1307, 1331, 1247, 1331, 1333, 1332, 1307, 1288, 1284, 1287, 1719, 1307, 1307, 1485, 1307, 1289, 1307",
      /*  736 */ "1331, 1264, 1672, 1307, 1313, 1318, 1318, 1318, 1318, 1314, 1307, 1671, 1307, 1307, 1307, 1328, 1360",
      /*  753 */ "1369, 1367, 1305, 1719, 1307, 1307, 1322, 1330, 1307, 1303, 1366, 1305, 1307, 1332, 1307, 1303, 1364",
      /*  770 */ "1305, 1307, 1231, 1307, 1259, 1255, 1587, 1307, 1411, 1532, 1410, 1307, 1307, 1554, 1559, 1532, 1532",
      /*  787 */ "1532, 1532, 1412, 1307, 1586, 1307, 1341, 1346, 1307, 1323, 1307, 1307, 1503, 1307, 1410, 1412, 1530",
      /*  804 */ "1588, 1625, 1307, 1379, 1384, 1384, 1384, 1384, 1380, 1307, 1624, 1307, 1307, 1307, 1351, 1470, 1388",
      /*  821 */ "1392, 1396, 1403, 1461, 1307, 1307, 1576, 1307, 1360, 1362, 1367, 1305, 1735, 1307, 1307, 1368, 1306",
      /*  838 */ "1307, 1374, 1307, 1307, 1508, 1307, 1566, 1416, 1458, 1307, 1407, 1307, 1307, 1641, 1307, 1422, 1225",
      /*  855 */ "1307, 1307, 1670, 1307, 1360, 1362, 1365, 1305, 1303, 1370, 1307, 1307, 1766, 1307, 1303, 1370, 1304",
      /*  872 */ "1307, 1418, 1307, 1307, 1431, 1502, 1307, 1435, 1307, 1307, 1442, 1307, 1307, 1452, 1307, 1307, 1467",
      /*  889 */ "1307, 1307, 1482, 1307, 1307, 1499, 1436, 1501, 1362, 1735, 1307, 1307, 1507, 1307, 1307, 1512, 1307",
      /*  906 */ "1307, 1522, 1307, 1307, 1523, 1307, 1307, 1524, 1293, 1296, 1410, 1532, 1530, 1588, 1360, 1369, 1365",
      /*  923 */ "1307, 1536, 1775, 1307, 1540, 1307, 1307, 1545, 1765, 1307, 1549, 1307, 1307, 1574, 1307, 1307, 1580",
      /*  940 */ "1307, 1307, 1592, 1307, 1307, 1604, 1307, 1307, 1605, 1324, 1337, 1303, 1369, 1365, 1307, 1609, 1307",
      /*  957 */ "1307, 1620, 1683, 1622, 1440, 1307, 1307, 1307, 1365, 1250, 1307, 1426, 1446, 1425, 1307, 1307, 1629",
      /*  974 */ "1307, 1307, 1651, 1655, 1307, 1656, 1375, 1342, 1446, 1446, 1446, 1446, 1427, 1307, 1249, 1307, 1676",
      /*  991 */ "1307, 1307, 1682, 1623, 1307, 1691, 1307, 1307, 1696, 1647, 1698, 1425, 1446, 1449, 1251, 1566, 1416",
      /* 1008 */ "1307, 1307, 1697, 1307, 1307, 1710, 1307, 1307, 1716, 1307, 1307, 1719, 1307, 1307, 1309, 1307, 1210",
      /* 1025 */ "1206, 1514, 1473, 1357, 1307, 1721, 1660, 1723, 1403, 1514, 1307, 1307, 1722, 1307, 1307, 1728, 1728",
      /* 1042 */ "1635, 1470, 1464, 1479, 1307, 1732, 1307, 1307, 1739, 1585, 1307, 1743, 1746, 1584, 1514, 1464, 1479",
      /* 1059 */ "1307, 1745, 1307, 1307, 1760, 1307, 1307, 1762, 1550, 1764, 1497, 1225, 1307, 1307, 1770, 1307, 1307",
      /* 1076 */ "1772, 1541, 1774, 1712, 1307, 1307, 1307, 1491, 1611, 1307, 1307, 1307, 1501, 1362, 1307, 1307, 1694",
      /* 1093 */ "1518, 1307, 1307, 1307, 1592, 1410, 1532, 1528, 1588, 1520, 1307, 1307, 1307, 1594, 1559, 1559, 1559",
      /* 1110 */ "1559, 1560, 1307, 1307, 1307, 1615, 1347, 1564, 1307, 1307, 1643, 1307, 1307, 1307, 1637, 1678, 1307",
      /* 1127 */ "1307, 1307, 1694, 1708, 1307, 1307, 1307, 1750, 1566, 1568, 1307, 1307, 1369, 1307, 1307, 1694, 1555",
      /* 1144 */ "1307, 1307, 1307, 1764, 1570, 1598, 1307, 1307, 1618, 1307, 1307, 1307, 1774, 1266, 1664, 1307, 1307",
      /* 1161 */ "1600, 1307, 1307, 1307, 1776, 1493, 1307, 1307, 1307, 1620, 1307, 1307, 1687, 1704, 1307, 1307, 1702",
      /* 1178 */ "1752, 1307, 1307, 1754, 1727, 1307, 1307, 1754, 1582, 1307, 1307, 1584, 1493, 1582, 1746, 1584, 1666",
      /* 1195 */ "1307, 1307, 1307, 1475, 1307, 1307, 1307, 1756, 1307, 1307, 1307, 4, 4, 4, 4, 0, 4, 4, 4, 0, 4, 0, 4",
      /* 1218 */ "14, 0, 25266, 65830, 65830, 0, 25330, 0, 0, 0, 385042, 99178, 0, 0, 0, 49156, 0, 0, 573444, 573444",
      /* 1238 */ "573444, 0, 73746, 0, 81938, 573444, 573444, 573444, 573444, 0, 107058, 0, 0, 0, 393220, 0, 0, 49156",
      /* 1256 */ "49156, 49156, 49156, 0, 49156, 49156, 49156, 0, 0, 123218, 0, 0, 0, 452258, 163844, 0, 163844, 0, 0",
      /* 1275 */ "0, 229380, 229380, 229380, 0, 229380, 229380, 229380, 229380, 82454, 82454, 82454, 82454, 0, 82454",
      /* 1290 */ "82454, 82454, 0, 196644, 196644, 0, 196644, 196644, 196644, 65830, 0, 32782, 196644, 0, 0, 0, 65830",
      /* 1307 */ "0, 0, 0, 0, 4, 0, 0, 262152, 262152, 262152, 0, 262152, 262152, 262152, 262152, 0, 212996, 212996",
      /* 1325 */ "212996, 0, 212996, 0, 212996, 212996, 0, 0, 0, 82454, 0, 0, 212996, 212996, 212996, 212996, 0",
      /* 1342 */ "246806, 246806, 246806, 246806, 246806, 0, 0, 0, 98318, 0, 246806, 278536, 0, 0, 90130, 0, 0, 65550",
      /* 1360 */ "0, 0, 65830, 65830, 65830, 65830, 0, 65830, 0, 65830, 65830, 65830, 0, 0, 278536, 246806, 246806, 0",
      /* 1378 */ "49166, 0, 294916, 294916, 294916, 0, 294916, 294916, 294916, 294916, 638980, 638980, 262162, 360468",
      /* 1392 */ "270354, 0, 65550, 360468, 0, 360468, 0, 0, 0, 573444, 0, 73746, 638980, 81934, 573444, 0, 25266",
      /* 1409 */ "65830, 0, 0, 344068, 344068, 344068, 0, 378946, 753668, 0, 0, 0, 598034, 0, 868356, 720904, 0, 0",
      /* 1427 */ "393220, 393220, 393220, 0, 0, 622596, 622596, 622596, 0, 622596, 622596, 0, 622596, 0, 469154, 0, 0",
      /* 1444 */ "0, 606226, 393220, 393220, 393220, 393220, 0, 393220, 0, 0, 641346, 0, 0, 647186, 0, 0, 704520, 0, 0",
      /* 1463 */ "360468, 638980, 638980, 262162, 0, 0, 238722, 0, 0, 278546, 638980, 638980, 0, 0, 0, 616994, 270354",
      /* 1480 */ "0, 65550, 0, 0, 720914, 0, 0, 819218, 0, 0, 1081364, 0, 16398, 0, 0, 0, 509682, 0, 868356, 0, 0, 0",
      /* 1502 */ "622596, 0, 0, 0, 131076, 802824, 0, 0, 0, 147720, 0, 853938, 0, 0, 0, 638980, 0, 901124, 0, 0, 0",
      /* 1523 */ "737284, 0, 0, 0, 196644, 344068, 0, 344068, 0, 344068, 344068, 344068, 344068, 0, 1179652, 1179652",
      /* 1539 */ "1179652, 0, 1179652, 1179652, 0, 1179652, 0, 1163268, 1163268, 1163268, 0, 1163268, 1163268, 0",
      /* 1553 */ "1163268, 0, 1130504, 1130504, 1130504, 0, 1130504, 1130504, 1130504, 1130504, 936378, 378946, 114702",
      /* 1566 */ "0, 0, 0, 753668, 0, 0, 0, 584162, 0, 911298, 0, 0, 0, 753682, 0, 944802, 0, 0, 0, 771942, 0, 0, 0",
      /* 1589 */ "344068, 0, 0, 0, 131076, 0, 0, 0, 819204, 378946, 592498, 0, 0, 0, 835602, 704530, 0, 0, 0, 212996",
      /* 1609 */ "0, 163858, 0, 0, 0, 845522, 172050, 0, 172050, 0, 16650, 0, 0, 0, 837782, 0, 0, 0, 294916, 0, 778258",
      /* 1630 */ "0, 0, 0, 229380, 0, 0, 238706, 0, 0, 0, 862322, 0, 802834, 0, 0, 0, 902882, 0, 296258, 0, 361782, 0",
      /* 1652 */ "303122, 311314, 327698, 335890, 0, 0, 0, 246806, 319506, 376852, 0, 376852, 0, 460674, 0, 0, 0",
      /* 1669 */ "936378, 876562, 0, 0, 0, 262152, 0, 0, 518098, 0, 0, 0, 936466, 0, 837782, 837782, 837782, 837782, 0",
      /* 1688 */ "837782, 0, 862322, 0, 837782, 837782, 0, 16706, 0, 0, 0, 361782, 0, 0, 0, 131086, 0, 0, 0, 983060, 0",
      /* 1709 */ "0, 0, 1064964, 0, 0, 0, 786440, 0, 131086, 983060, 0, 25266, 0, 0, 0, 376852, 0, 0, 1097752, 0, 0, 0",
      /* 1731 */ "328806, 0, 1114132, 1114132, 0, 25266, 0, 65830, 0, 771942, 771942, 771942, 0, 518098, 0, 771942",
      /* 1747 */ "771942, 0, 771942, 0, 641330, 0, 0, 0, 1114132, 0, 0, 0, 1083978, 0, 625330, 0, 0, 0, 1163268, 0, 0",
      /* 1768 */ "0, 180328, 0, 1084122, 0, 0, 0, 1179652, 0, 0, 0, 180242"
    };
    decode(s1, CASEID);
  }

  private static final int[] TOKENSET = new int[119];
  static
  {
    final String s1[] =
    {
      /*   0 */ "55, 54, 70, 27, 54, 19, 56, 30, 48, 51, 51, 51, 54, 57, 54, 56, 30, 46, 48, 47, 19, 68, 51, 47, 65",
      /*  25 */ "48, 47, 66, 48, 69, 42, 40, 67, 32, 59, 66, 63, 39, 62, 39, 62, 39, 42, 38, 42, 8, 0, 26, 64, 28, 58",
      /*  51 */ "61, 60, 60, 34, 20, 21, 53, 8, 64, 42, 42, 20, 21, 50, 8, 42, 20, 21, 33, 24, 20, 21, 42, 19, 2, 3",
      /*  77 */ "22, 52, 32, 25, 49, 28, 25, 33, 18, 12, 1, 11, 23, 7, 17, 9, 6, 31, 15, 35, 41, 16, 22, 10, 0, 14, 2",
      /* 104 */ "3, 45, 4, 13, 37, 36, 43, 44, 29, 5, 2, 3, 2, 3, 6"
    };
    decode(s1, TOKENSET);
  }

  private static final int[] APPENDIX = new int[18];
  static
  {
    final String s1[] =
    {
      /*  0 */ "2, 348169, 8353, 98322, 12633, 98322, 123403, 139268, 45065, 319490, 40969, 319490, 292081, 376834",
      /* 14 */ "296249, 376834, 491530, 540682"
    };
    decode(s1, APPENDIX);
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
    decode(s1, LOOKBACK);
  }

  private static final int[] GOTO = new int[947];
  static
  {
    final String s1[] =
    {
      /*   0 */ "340, 407, 407, 407, 537, 407, 360, 407, 572, 407, 407, 407, 493, 407, 407, 407, 563, 502, 407, 407",
      /*  20 */ "346, 407, 407, 407, 352, 407, 407, 407, 407, 407, 544, 407, 459, 407, 594, 407, 358, 407, 407, 407",
      /*  40 */ "364, 407, 407, 407, 370, 407, 407, 407, 407, 407, 360, 407, 511, 407, 407, 407, 490, 502, 407, 407",
      /*  60 */ "520, 407, 407, 407, 376, 407, 407, 407, 382, 407, 407, 407, 465, 407, 407, 407, 407, 407, 408, 407",
      /*  80 */ "388, 407, 407, 407, 394, 407, 407, 407, 400, 407, 407, 407, 378, 407, 407, 407, 424, 407, 407, 406",
      /* 100 */ "527, 407, 407, 412, 407, 407, 413, 407, 390, 417, 419, 423, 483, 407, 594, 407, 428, 407, 407, 407",
      /* 120 */ "434, 407, 407, 407, 407, 430, 440, 560, 445, 407, 407, 407, 451, 407, 407, 407, 457, 407, 407, 407",
      /* 140 */ "441, 407, 407, 406, 463, 407, 407, 407, 469, 407, 407, 407, 475, 407, 407, 407, 481, 407, 407, 407",
      /* 160 */ "487, 407, 407, 407, 499, 407, 407, 407, 508, 407, 407, 407, 517, 407, 407, 407, 407, 407, 407, 534",
      /* 180 */ "407, 407, 526, 407, 407, 407, 407, 554, 531, 407, 407, 407, 407, 407, 407, 543, 407, 342, 407, 557",
      /* 200 */ "384, 551, 407, 423, 396, 417, 419, 423, 384, 548, 477, 423, 384, 578, 477, 423, 407, 453, 440, 560",
      /* 220 */ "407, 402, 407, 407, 407, 539, 407, 407, 585, 407, 407, 407, 407, 407, 407, 471, 574, 407, 407, 406",
      /* 240 */ "407, 407, 407, 436, 407, 407, 407, 495, 407, 407, 407, 566, 407, 407, 407, 569, 407, 513, 589, 557",
      /* 260 */ "407, 407, 593, 407, 598, 407, 407, 407, 407, 402, 407, 407, 407, 539, 407, 407, 602, 407, 407, 407",
      /* 280 */ "606, 407, 407, 407, 407, 447, 407, 407, 407, 504, 407, 407, 610, 407, 407, 407, 614, 407, 407, 407",
      /* 300 */ "407, 407, 407, 366, 407, 407, 407, 372, 407, 522, 407, 557, 618, 407, 407, 407, 622, 407, 407, 407",
      /* 320 */ "407, 407, 348, 407, 407, 513, 626, 557, 407, 354, 581, 407, 630, 407, 407, 407, 634, 407, 407, 407",
      /* 340 */ "638, 684, 685, 685, 653, 842, 701, 705, 685, 685, 685, 926, 799, 710, 685, 685, 685, 932, 674, 684",
      /* 360 */ "685, 685, 686, 685, 668, 684, 685, 685, 736, 685, 662, 684, 685, 685, 739, 685, 718, 684, 685, 685",
      /* 380 */ "774, 685, 721, 684, 685, 685, 783, 685, 853, 705, 685, 685, 783, 875, 890, 684, 685, 685, 783, 895",
      /* 400 */ "943, 684, 685, 685, 784, 685, 888, 685, 685, 685, 685, 706, 778, 685, 685, 685, 711, 788, 792, 685",
      /* 420 */ "685, 796, 685, 779, 685, 685, 685, 715, 822, 684, 685, 685, 803, 812, 754, 684, 685, 685, 816, 685",
      /* 440 */ "815, 685, 685, 685, 724, 838, 684, 685, 685, 827, 685, 844, 684, 685, 685, 865, 812, 881, 684, 685",
      /* 460 */ "685, 868, 685, 916, 684, 685, 685, 870, 685, 644, 684, 685, 685, 879, 685, 928, 684, 685, 685, 922",
      /* 480 */ "685, 656, 684, 685, 685, 941, 685, 934, 684, 685, 685, 696, 695, 685, 690, 685, 685, 685, 894, 659",
      /* 500 */ "684, 685, 685, 700, 685, 685, 685, 920, 760, 684, 685, 685, 705, 685, 685, 745, 899, 641, 684, 685",
      /* 520 */ "685, 710, 685, 685, 757, 685, 836, 685, 685, 685, 773, 647, 684, 685, 685, 831, 685, 685, 751, 685",
      /* 540 */ "685, 685, 874, 650, 685, 685, 685, 806, 852, 857, 685, 685, 848, 685, 685, 910, 685, 685, 820, 685",
      /* 560 */ "685, 820, 826, 685, 691, 695, 685, 671, 685, 685, 677, 685, 685, 684, 685, 685, 685, 885, 852, 861",
      /* 580 */ "685, 685, 938, 685, 685, 665, 684, 685, 685, 680, 905, 909, 685, 914, 685, 685, 685, 808, 748, 684",
      /* 600 */ "685, 685, 727, 684, 685, 685, 733, 684, 685, 685, 901, 684, 685, 685, 832, 705, 685, 685, 763, 684",
      /* 620 */ "685, 685, 766, 684, 685, 685, 769, 905, 909, 685, 742, 684, 685, 685, 730, 684, 685, 685, 6, 0, 4121",
      /* 641 */ "0, 0, 221945, 0, 0, 229396, 0, 0, 237577, 0, 0, 241673, 0, 0, 245769, 0, 0, 245780, 0, 0, 253972, 0",
      /* 663 */ "0, 262156, 0, 0, 263265, 0, 0, 270348, 0, 0, 275337, 0, 0, 278540, 0, 0, 279545, 0, 0, 283761, 483332",
      /* 684 */ "8409, 0, 0, 0, 0, 16441, 12697, 0, 0, 0, 24649, 28681, 0, 0, 0, 24681, 32777, 0, 0, 0, 49201, 53561",
      /* 706 */ "0, 0, 0, 94681, 61665, 0, 0, 0, 94713, 0, 380937, 385561, 0, 0, 364553, 0, 0, 368649, 0, 0, 385625, 0",
      /* 728 */ "0, 459905, 0, 0, 459969, 0, 0, 462857, 0, 0, 476441, 0, 0, 480609, 0, 0, 483337, 0, 0, 499716, 0, 0",
      /* 750 */ "598052, 0, 20521, 57348, 0, 0, 303116, 0, 0, 327689, 0, 0, 328485, 0, 0, 332969, 0, 0, 335881, 0, 0",
      /* 771 */ "345265, 483332, 98348, 0, 0, 0, 106985, 114697, 0, 0, 0, 127697, 122889, 0, 0, 0, 254817, 397321",
      /* 789 */ "188420, 188420, 188420, 32777, 0, 212996, 212996, 0, 188420, 212996, 0, 57433, 356473, 16388, 172809",
      /* 804 */ "176873, 344068, 0, 65545, 0, 0, 74177, 0, 0, 335876, 181009, 425988, 0, 0, 0, 270345, 0, 406329, 0, 0",
      /* 824 */ "143945, 0, 184329, 0, 0, 0, 308481, 172825, 0, 0, 0, 315401, 0, 442380, 0, 0, 196617, 0, 0, 249865, 0",
      /* 845 */ "0, 200713, 0, 32777, 0, 573460, 573460, 294916, 0, 0, 0, 372745, 32777, 221212, 212996, 212996, 32777",
      /* 862 */ "237596, 212996, 212996, 172809, 0, 344068, 0, 69641, 0, 0, 78281, 0, 259025, 0, 0, 0, 393865, 0",
      /* 880 */ "266249, 0, 0, 204809, 0, 0, 434185, 385561, 0, 111217, 0, 0, 98313, 0, 270345, 0, 0, 0, 393913, 0",
      /* 900 */ "442377, 0, 0, 204844, 0, 286729, 0, 516100, 475140, 446473, 0, 0, 0, 414761, 0, 458780, 0, 0, 209577",
      /* 919 */ "0, 312649, 0, 0, 0, 212996, 0, 339977, 0, 0, 0, 213001, 0, 0, 540676, 0, 0, 217097, 0, 0, 507940",
      /* 940 */ "524292, 0, 155660, 0, 0, 102409, 0"
    };
    decode(s1, GOTO);
  }

  private static final int[] REDUCTION = new int[148];
  static
  {
    final String s1[] =
    {
      /*   0 */ "43, 0, 0, -1, 1, -1, 1, 1, 2, -1, 3, -1, 4, -1, 5, -1, 44, 2, 6, -1, 45, 3, 7, 4, 8, -1, 47, 6, 46, 5",
      /*  30 */ "9, 7, 10, -1, 11, -1, 48, 8, 12, 9, 13, -1, 49, 10, 14, -1, 15, -1, 50, 11, 16, 12, 16, -1, 17, 14",
      /*  56 */ "17, 13, 18, 16, 18, 15, 19, 17, 20, 20, 20, 19, 20, 18, 20, 21, 21, -1, 22, 22, 52, 24, 51, 23, 23",
      /*  81 */ "-1, 24, 26, 24, 25, 25, 27, 25, -1, 54, 29, 53, 28, 26, -1, 27, 30, 28, 30, 55, 31, 29, 32, 30, -1",
      /* 106 */ "56, 33, 31, -1, 57, 34, 32, -1, 33, -1, 34, 38, 34, 37, 34, 36, 34, 35, 35, -1, 36, 39, 37, 40, 58",
      /* 131 */ "41, 38, -1, 39, 42, 39, -1, 40, 43, 41, 44, 60, 46, 59, 45, 42, -1"
    };
    decode(s1, REDUCTION);
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
    "'!'",
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

                                                            // line 972 "ixml.ebnf"
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
                                                            // line 1753 "Ixml.java"
// End
