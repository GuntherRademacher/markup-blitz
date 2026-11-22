// This file was generated on Sun Nov 22, 2026 20:58 (UTC) by REx v7.0 which is Copyright (c) 1979-2026 by Gunther Rademacher <grd@gmx.net>
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
                                                            grammar.addRule(rule = new Rule(mark, alias, name, alts.peek()));
                                                            // line 514 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 22 "ixml.ebnf"
                                                            rule.incrementCost();
                                                            // line 521 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 23 "ixml.ebnf"
                                                            alts.pop();
                                                            // line 528 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 25 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 535 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 26 "ixml.ebnf"
                                                            nameBuilder.append(input.subSequence(b0, e0));
                                                            // line 542 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 29 "ixml.ebnf"
                                                            name = nameBuilder.toString();
                                                            alias = null;
                                                            // line 550 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 41 "ixml.ebnf"
                                                            if (! grammar.getVersion().isAtLeast(Grammar.Version.V1_1))
                                                              de.bottlecaps.markup.blitz.Errors.S12.thro(grammar.getVersion() + " does not support renaming");
                                                            savedName = name;
                                                            // line 559 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 46 "ixml.ebnf"
                                                            alias = name;
                                                            name = savedName;
                                                            // line 567 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 50 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 574 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 60 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 581 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 62 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 589 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 66 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 597 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 70 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 606 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 76 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 614 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 80 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 623 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 86 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 631 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 89 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 638 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 90 "ixml.ebnf"
                                                            mark = Mark.NODE;
                                                            // line 645 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 91 "ixml.ebnf"
                                                            mark = Mark.DELETE;
                                                            // line 652 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 92 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 659 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 95 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, alias, name);
                                                            // line 666 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 96 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 673 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 99 "ixml.ebnf"
                                                            alts.peek().last().addCharset(new Charset(deleted, exclusion, members));
                                                            // line 680 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 102 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 687 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 103 "ixml.ebnf"
                                                            alts.peek().last().addCodepoint(deleted, codepoint);
                                                            // line 694 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 105 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 701 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 106 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 708 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 107 "ixml.ebnf"
                                                            stringBuilder.append(input.subSequence(b0, e0));
                                                            // line 715 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 112 "ixml.ebnf"
                                                            validateStringChar(input.toString().codePointAt(b0));
                                                            // line 722 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 114 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 729 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 115 "ixml.ebnf"
                                                            codepoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 736 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 120 "ixml.ebnf"
                                                            exclusion = false;
                                                            members = new java.util.ArrayList<>();
                                                            // line 744 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 125 "ixml.ebnf"
                                                            exclusion = true;
                                                            members = new java.util.ArrayList<>();
                                                            // line 752 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 130 "ixml.ebnf"
                                                            members.add(new StringMember(stringBuilder.toString(), false));
                                                            // line 759 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 131 "ixml.ebnf"
                                                            members.add(new StringMember(codepoint, true));
                                                            // line 766 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 132 "ixml.ebnf"
                                                            members.add(new RangeMember(firstCodepoint, lastCodepoint));
                                                            // line 773 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 133 "ixml.ebnf"
                                                            members.add(new ClassMember(clazz));
                                                            // line 780 "Ixml.java"
        }
        break;
      case 40:
        {
                                                            // line 135 "ixml.ebnf"
                                                            firstCodepoint = codepoint;
                                                            // line 787 "Ixml.java"
        }
        break;
      case 41:
        {
                                                            // line 136 "ixml.ebnf"
                                                            lastCodepoint = codepoint;
                                                            // line 794 "Ixml.java"
        }
        break;
      case 42:
        {
                                                            // line 137 "ixml.ebnf"
                                                            codepoint = input.subSequence(b0, e0).toString();
                                                            // line 801 "Ixml.java"
        }
        break;
      case 43:
        {
                                                            // line 142 "ixml.ebnf"
                                                            clazz += input.subSequence(b0, e0);
                                                            // line 808 "Ixml.java"
        }
        break;
      case 44:
        {
                                                            // line 148 "ixml.ebnf"
                                                            clazz = input.subSequence(b0, e0).toString();
                                                            // line 815 "Ixml.java"
        }
        break;
      case 45:
        {
                                                            // line 150 "ixml.ebnf"
                                                            Term operand = alts.peek().last().removeLast();
                                                            alts.peek().last().addNot(operand);
                                                            // line 823 "Ixml.java"
        }
        break;
      case 46:
        {
                                                            // line 153 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 830 "Ixml.java"
        }
        break;
      case 47:
        {
                                                            // line 154 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codepoint);
                                                            // line 837 "Ixml.java"
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
    for (int i = 0; i < 73; i += 32)
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
      /*   0 */ "69, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3",
      /*  34 */ "4, 5, 1, 1, 1, 6, 7, 8, 9, 10, 11, 12, 13, 1, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 16, 17, 18",
      /*  62 */ "19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43",
      /*  87 */ "44, 45, 46, 47, 48, 1, 49, 50, 51, 1, 52, 52, 52, 52, 53, 52, 54, 54, 55, 54, 54, 56, 57, 58, 59, 54",
      /* 113 */ "54, 60, 61, 54, 54, 62, 54, 63, 54, 54, 64, 65, 66, 67, 1"
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
      /* 1569 */ "2317, 1580, 2125, 1843, 2125, 1811, 1861, 1861, 1861, 1863, 69, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 2",
      /* 1592 */ "1, 1, 2, 1, 1, 1, 1, 51, 51, 1, 51, 2, 3, 4, 5, 1, 1, 1, 6, 7, 8, 9, 10, 11, 12, 13, 1, 1, 1, 1, 1",
      /* 1623 */ "51, 1, 68, 68, 68, 68, 1, 1, 51, 51, 51, 51, 14, 14, 14, 14, 14, 14, 14, 14, 15, 16, 17, 18, 19, 20",
      /* 1649 */ "21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45",
      /* 1674 */ "46, 47, 48, 1, 49, 50, 51, 1, 51, 1, 1, 1, 51, 51, 1, 51, 1, 1, 51, 1, 1, 1, 52, 52, 52, 52, 53, 52",
      /* 1702 */ "54, 54, 60, 61, 54, 54, 62, 54, 55, 54, 54, 56, 57, 58, 59, 63, 54, 54, 64, 65, 66, 67, 1, 1, 1, 1",
      /* 1728 */ "1, 68, 1, 68, 1, 1, 68, 68, 68, 1, 1, 1, 1, 51, 51, 51, 51, 1, 68, 68, 68, 68, 68, 1, 68, 1, 68, 68",
      /* 1756 */ "1, 68, 68, 1, 68, 1, 1, 51, 51, 1, 51, 51, 51, 1, 51, 1, 51, 51, 51, 51, 51, 51, 1, 51, 68, 68, 68",
      /* 1783 */ "68, 68, 68, 68, 1, 68, 68, 68, 68, 68, 68, 68, 51, 51, 68, 1, 68, 51, 1, 1, 1, 51, 51, 1, 1, 1, 1, 1",
      /* 1811 */ "68, 68, 51, 51, 51, 51, 51, 51, 68, 68, 68, 68, 68, 68, 68, 51, 68, 68, 68, 68, 68, 68, 1, 51, 51",
      /* 1836 */ "51, 68, 68, 1, 1, 68, 68, 68, 68, 1, 1, 1, 51, 51, 51, 51, 68, 51, 51, 1, 1, 68, 51, 1, 1, 51, 51",
      /* 1863 */ "51, 51, 51, 51, 51, 51, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 68, 51, 1, 51, 51, 51, 51, 1, 1, 1, 1, 51",
      /* 1892 */ "1, 68, 68, 51, 51, 51, 68, 1, 1, 68, 68, 1, 51, 51, 1, 1, 68, 51, 1, 68, 68, 1, 1, 68, 68, 1, 1, 68",
      /* 1920 */ "68, 51, 51, 51, 51, 1, 1, 1, 51, 51, 1, 51, 1, 51, 51, 1, 51, 51, 1, 51, 51, 51, 51, 1, 51, 1, 51",
      /* 1947 */ "68, 51, 51, 68, 68, 68, 68, 51, 51, 51, 1, 1, 51, 1, 1, 68, 68, 1, 68, 68, 51, 1, 1, 1, 1, 1, 1, 51",
      /* 1975 */ "51, 51, 51, 51, 51, 51, 51, 1, 1, 68, 68, 68, 1, 68, 1, 1, 1, 68, 68, 51, 51, 51, 51, 68, 68, 1, 68",
      /* 2002 */ "68, 68, 1, 1, 1, 68, 1, 1, 1, 51, 51, 51, 1, 1, 2, 51, 51, 51, 51, 51, 51, 51, 1, 51, 51, 51, 51, 51",
      /* 2030 */ "51, 1, 51, 1, 1, 51, 51, 68, 68, 68, 1, 1, 1, 1, 68, 51, 51, 68, 68, 1, 1, 1, 1, 1, 68, 68, 68, 68",
      /* 2058 */ "68, 1, 1, 1, 1, 51, 68, 1, 1, 1, 1, 1, 1, 68, 68, 1, 1, 51, 51, 51, 51, 51, 1, 68, 1, 1, 1, 68, 68",
      /* 2087 */ "68, 2, 1, 68, 68, 68, 68, 1, 51, 51, 51, 51, 51, 51, 51, 51, 51, 68, 1, 68, 68, 68, 68, 51, 51, 1, 1",
      /* 2114 */ "1, 1, 51, 51, 68, 68, 1, 1, 1, 68, 1, 68, 68, 68, 68, 68, 68, 68, 68, 1, 1, 1, 1, 1, 1, 1, 68, 68",
      /* 2142 */ "68, 1, 68, 68, 68, 68, 1, 1, 1, 51, 68, 51, 51, 51, 51, 68, 51, 51, 51, 51, 51, 51, 51, 68, 1, 51",
      /* 2168 */ "51, 1, 1, 68, 51, 51, 1, 1, 1, 51, 68, 68, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 51, 1, 1, 1",
      /* 2199 */ "1, 51, 1, 1, 1, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 51, 51, 51, 51, 1, 1, 51, 51, 51, 51, 1, 1, 51",
      /* 2227 */ "51, 51, 51, 1, 1, 51, 51, 51, 1, 1, 68, 68, 1, 1, 51, 51, 51, 1, 68, 1, 1, 1, 1, 51, 1, 1, 1, 1, 1",
      /* 2256 */ "1, 1, 68, 1, 1, 1, 68, 68, 1, 1, 68, 1, 1, 1, 1, 1, 1, 51, 51, 51, 51, 68, 1, 1, 1, 1, 68, 1, 1, 51",
      /* 2286 */ "51, 51, 1, 51, 51, 68, 51, 68, 68, 68, 51, 51, 68, 51, 51, 51, 68, 51, 1, 1, 1, 1, 1, 1, 68, 51, 51",
      /* 2313 */ "51, 51, 51, 68, 68, 68, 68, 68, 1, 1, 68, 68, 51, 51, 51, 1, 68, 68, 1, 1, 1, 1, 68, 51, 51, 51, 1",
      /* 2340 */ "1, 68, 1, 1, 1"
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
      /*  718 */ "655357, 720893, 786429, 851965, 917501, 917759, 917999, 983037, 1048573, 1114109, 1, 51, 1, 51, 1",
      /*  733 */ "51, 1, 51, 1, 51, 68, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1",
      /*  761 */ "68, 1, 68, 1, 51, 1, 51, 1, 68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51",
      /*  790 */ "1, 51, 1, 51, 1, 51, 1, 51, 1, 68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1",
      /*  819 */ "68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 68, 1, 68, 1, 68",
      /*  847 */ "51, 1, 51, 1, 51, 1, 68, 1, 68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 68, 1, 51, 68, 1, 68, 1, 68, 1",
      /*  876 */ "51, 1, 68, 1, 68, 1, 51, 1, 68, 1, 68, 51, 68, 1, 68, 1, 68, 1, 68, 1, 51, 1, 68, 1, 51, 1, 68, 1",
      /*  904 */ "51, 68, 1, 68, 1, 68, 1, 68, 1, 68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 68, 51, 1, 51, 1, 68, 1",
      /*  932 */ "68, 1, 68, 1, 68, 1, 68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51",
      /*  961 */ "1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1",
      /*  990 */ "51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 68, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51",
      /* 1019 */ "1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1",
      /* 1048 */ "51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 51, 1, 1",
      /* 1077 */ "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 68, 1, 1, 1"
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

  private static final int[] TRANSITION = new int[2125];
  static
  {
    final String s1[] =
    {
      /*    0 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*   17 */ "1228, 1307, 1461, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*   34 */ "1120, 1139, 1134, 1160, 1147, 1151, 1159, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307",
      /*   51 */ "1461, 1228, 1228, 1228, 1126, 1168, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1206, 1228, 2010, 1187",
      /*   68 */ "2016, 1205, 1200, 1214, 1215, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1223, 1237",
      /*   85 */ "1227, 1253, 1272, 1274, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1943, 1228, 1934, 1282, 1940, 1942",
      /*  102 */ "1295, 1327, 1328, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1228, 1228, 1228",
      /*  119 */ "1175, 1179, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1244, 1228, 1307, 1501, 1245, 1507, 1228, 1228",
      /*  136 */ "1336, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1228, 1355, 1228, 1228, 1382",
      /*  153 */ "1363, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1228, 1372, 1228, 1343, 1347, 1380",
      /*  170 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1893, 1390, 1228, 1228, 1890, 1228, 1228",
      /*  187 */ "1228, 1228, 1228, 1228, 1228, 1259, 1228, 1307, 1398, 1259, 1264, 1465, 1413, 1414, 1228, 1228, 1228",
      /*  204 */ "1228, 1228, 1228, 1228, 1402, 1228, 1307, 1422, 1405, 1436, 1228, 1228, 1428, 1228, 1228, 1228, 1228",
      /*  221 */ "1228, 1228, 1228, 1444, 1228, 1307, 1461, 1228, 1319, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  238 */ "1228, 1228, 1228, 1228, 1821, 1454, 1228, 1920, 1228, 1228, 1770, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  255 */ "1228, 1228, 1228, 1307, 1473, 1477, 1485, 1228, 1228, 1486, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  272 */ "1228, 1228, 1307, 1461, 1228, 1228, 1228, 1228, 1301, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  289 */ "1228, 1835, 1494, 1228, 1192, 1228, 1228, 2082, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1445, 1228",
      /*  306 */ "1307, 1461, 1228, 1287, 1228, 1228, 1446, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307",
      /*  323 */ "1461, 1228, 1515, 1228, 1228, 1845, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461",
      /*  340 */ "1228, 1228, 1996, 1523, 1524, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1228, 1307, 1461, 1991",
      /*  357 */ "1319, 1532, 1532, 1533, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1228, 1307, 1461, 1991, 1319",
      /*  374 */ "1541, 1541, 1542, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1228, 1307, 1461, 1991, 1319, 1550",
      /*  391 */ "1550, 1551, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1228, 1307, 1461, 1991, 1319, 1559, 1559",
      /*  408 */ "1560, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1228, 1307, 1461, 1991, 1319, 1568, 1568, 1569",
      /*  425 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1228, 1307, 1461, 1991, 1319, 1577, 1577, 1578, 1228",
      /*  442 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1586, 1586, 1587, 1228, 1228",
      /*  459 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1595, 1595, 1596, 1228, 1228, 1228",
      /*  476 */ "1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1604, 1604, 1605, 1228, 1228, 1228, 1228",
      /*  493 */ "1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1613, 1613, 1614, 1228, 1228, 1228, 1228, 1228",
      /*  510 */ "1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1622, 1622, 1623, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  527 */ "1228, 1228, 1228, 1307, 1461, 1991, 1364, 1631, 1631, 1632, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  544 */ "1228, 1228, 1307, 1461, 1991, 1364, 1640, 1640, 1641, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  561 */ "1228, 1307, 1461, 1991, 1364, 1649, 1649, 1650, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  578 */ "1307, 1461, 1991, 1364, 1658, 1658, 1659, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307",
      /*  595 */ "1461, 1991, 1364, 1667, 1667, 1668, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461",
      /*  612 */ "1991, 1364, 1676, 1676, 1677, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991",
      /*  629 */ "1364, 1685, 1685, 1686, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364",
      /*  646 */ "1694, 1694, 1695, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1703",
      /*  663 */ "1703, 1704, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1712, 1712",
      /*  680 */ "1713, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1721, 1721, 1722",
      /*  697 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1730, 1730, 1731, 1228",
      /*  714 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1739, 1739, 1740, 1228, 1228",
      /*  731 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1748, 1748, 1749, 1228, 1228, 1228",
      /*  748 */ "1228, 1228, 1228, 1228, 1228, 1228, 1307, 1461, 1991, 1364, 1757, 1757, 1758, 1228, 1228, 1228, 1228",
      /*  765 */ "1228, 1228, 1228, 1228, 1784, 1307, 1766, 1778, 1784, 1228, 1793, 1795, 1228, 1228, 1228, 1228, 1228",
      /*  782 */ "1228, 1228, 1228, 1228, 1307, 1803, 1807, 1815, 1829, 1228, 1879, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  799 */ "1228, 1228, 1228, 1307, 1461, 1228, 1843, 1962, 1853, 1854, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /*  816 */ "1228, 1228, 1307, 1461, 1228, 1364, 1862, 1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444",
      /*  833 */ "1228, 1307, 1461, 1991, 1319, 1862, 1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1444, 1877",
      /*  850 */ "1307, 1461, 1991, 1319, 1862, 1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1307",
      /*  867 */ "1461, 1991, 1364, 1862, 1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1887, 1307, 1461",
      /*  884 */ "1991, 1364, 1901, 1907, 1908, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1916, 1307, 1461, 1991",
      /*  901 */ "1364, 1862, 1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1957, 1307, 1461, 1991, 1364",
      /*  918 */ "1862, 1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1313, 1307, 1461, 1991, 1364, 1862",
      /*  935 */ "1868, 1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 2098, 1307, 1461, 1991, 1364, 1862, 1868",
      /*  952 */ "1869, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1229, 1307, 1461, 1991, 1364, 1862, 1868, 1869",
      /*  969 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1928, 1461, 1991, 1364, 1862, 1868, 1869, 1228",
      /*  986 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1951, 1461, 1991, 1364, 1970, 1976, 1977, 1228, 1228",
      /* 1003 */ "1228, 1228, 1228, 1228, 1228, 1228, 1228, 1985, 1461, 1991, 1785, 1862, 1868, 1869, 1228, 1228, 1228",
      /* 1020 */ "1228, 1228, 1228, 1228, 1228, 1228, 2004, 2049, 2024, 2026, 2034, 2038, 2046, 1228, 1228, 1228, 1228",
      /* 1037 */ "1228, 1228, 1228, 1228, 1228, 1307, 2057, 2061, 2069, 1228, 1228, 2070, 1228, 1228, 1228, 1228, 1228",
      /* 1054 */ "1228, 1228, 1228, 1228, 1307, 2078, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /* 1071 */ "1228, 1228, 1228, 1307, 1461, 2090, 2096, 1228, 2106, 2108, 1228, 1228, 1228, 1228, 1228, 1228, 1228",
      /* 1088 */ "1228, 1228, 1307, 1461, 1228, 1364, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 2116",
      /* 1105 */ "1228, 1228, 1228, 1228, 1228, 1228, 2117, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 1228, 0, 0, 0",
      /* 1123 */ "5960, 512, 640, 0, 0, 0, 0, 0, 6528, 0, 6528, 5960, 5960, 0, 0, 5960, 5960, 5960, 5960, 256, 0, 0",
      /* 1145 */ "5960, 0, 0, 0, 0, 5960, 5960, 5960, 0, 5960, 0, 0, 5960, 5960, 0, 0, 0, 5960, 5960, 5960, 5960, 5960",
      /* 1167 */ "5960, 6528, 6528, 6528, 6528, 6528, 6528, 6528, 0, 0, 0, 0, 7040, 7040, 7040, 7040, 7040, 7040, 7040",
      /* 1186 */ "0, 0, 0, 0, 256, 6656, 0, 0, 0, 0, 0, 0, 8192, 8192, 0, 6656, 6656, 0, 6656, 6656, 0, 0, 6656, 0, 0",
      /* 1211 */ "0, 0, 0, 0, 6656, 6656, 6656, 6656, 6656, 6656, 6656, 0, 0, 0, 0, 256, 6784, 0, 0, 0, 0, 0, 0, 0, 0",
      /* 1236 */ "5376, 6784, 0, 6784, 0, 0, 0, 6784, 0, 0, 0, 0, 7168, 0, 0, 0, 7168, 0, 6784, 6784, 0, 6784, 6784, 0",
      /* 1260 */ "0, 0, 0, 0, 7552, 0, 0, 0, 0, 7552, 0, 6016, 0, 0, 6784, 6784, 6784, 6784, 6784, 6784, 6784, 0, 0, 0",
      /* 1284 */ "0, 256, 6912, 0, 0, 0, 0, 0, 0, 8320, 8320, 0, 6912, 6912, 0, 6912, 6912, 0, 0, 0, 0, 0, 8064, 0, 0",
      /* 1309 */ "0, 0, 512, 640, 0, 0, 0, 0, 0, 5120, 0, 0, 0, 0, 0, 6144, 0, 6016, 0, 6912, 6912, 6912, 6912, 6912",
      /* 1333 */ "6912, 6912, 0, 7168, 0, 7168, 7168, 7168, 0, 7168, 0, 0, 0, 0, 7296, 7296, 7296, 7296, 7296, 7296",
      /* 1353 */ "7370, 0, 0, 841, 841, 841, 841, 841, 841, 841, 896, 0, 0, 0, 0, 0, 0, 0, 6016, 0, 7370, 7370, 7370",
      /* 1376 */ "7370, 7370, 7370, 7370, 0, 1024, 0, 0, 0, 0, 0, 0, 841, 0, 0, 7424, 7424, 7424, 7424, 7424, 7424",
      /* 1397 */ "7424, 7552, 0, 0, 256, 0, 0, 0, 0, 0, 0, 7680, 0, 0, 0, 7680, 7552, 7552, 7552, 7552, 7552, 7552",
      /* 1419 */ "7552, 7552, 0, 0, 7680, 0, 256, 0, 0, 0, 7680, 7680, 0, 7680, 7680, 7680, 0, 0, 7680, 7680, 7680",
      /* 1440 */ "7680, 7680, 7680, 7680, 6144, 0, 0, 0, 0, 0, 0, 0, 8320, 0, 0, 0, 0, 256, 0, 0, 7808, 0, 0, 0, 256",
      /* 1465 */ "0, 0, 0, 0, 0, 0, 7552, 7552, 0, 0, 0, 256, 0, 7936, 0, 7936, 7936, 7936, 0, 7936, 0, 7936, 7936",
      /* 1488 */ "7936, 7936, 7936, 7936, 7936, 0, 0, 0, 0, 256, 0, 0, 8192, 0, 0, 0, 256, 0, 0, 0, 7168, 7168, 7168",
      /* 1511 */ "7168, 7168, 7168, 7168, 0, 1152, 1152, 1152, 1152, 1152, 1152, 1152, 8448, 8448, 8448, 8448, 8448",
      /* 1528 */ "8448, 8448, 8448, 0, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 1280, 0, 1408, 1408, 1408, 1408, 1408",
      /* 1546 */ "1408, 1408, 1408, 0, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 0, 1664, 1664, 1664, 1664, 1664",
      /* 1564 */ "1664, 1664, 1664, 0, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 1792, 0, 1920, 1920, 1920, 1920, 1920",
      /* 1582 */ "1920, 1920, 1920, 0, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 0, 2176, 2176, 2176, 2176, 2176",
      /* 1600 */ "2176, 2176, 2176, 0, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 0, 2432, 2432, 2432, 2432, 2432",
      /* 1618 */ "2432, 2432, 2432, 0, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 0, 2688, 2688, 2688, 2688, 2688",
      /* 1636 */ "2688, 2688, 2688, 0, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 0, 2944, 2944, 2944, 2944, 2944",
      /* 1654 */ "2944, 2944, 2944, 0, 3072, 3072, 3072, 3072, 3072, 3072, 3072, 3072, 0, 3200, 3200, 3200, 3200, 3200",
      /* 1672 */ "3200, 3200, 3200, 0, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 3328, 0, 3456, 3456, 3456, 3456, 3456",
      /* 1690 */ "3456, 3456, 3456, 0, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 0, 3712, 3712, 3712, 3712, 3712",
      /* 1708 */ "3712, 3712, 3712, 0, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 0, 3968, 3968, 3968, 3968, 3968",
      /* 1726 */ "3968, 3968, 3968, 0, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 4096, 0, 4224, 4224, 4224, 4224, 4224",
      /* 1744 */ "4224, 4224, 4224, 0, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 0, 4480, 4480, 4480, 4480, 4480",
      /* 1762 */ "4480, 4480, 4480, 0, 0, 0, 8576, 256, 0, 0, 0, 0, 0, 0, 7808, 0, 0, 0, 8576, 0, 0, 0, 8576, 0, 0, 0",
      /* 1788 */ "0, 0, 0, 0, 8960, 0, 0, 8576, 8576, 8576, 8576, 8576, 8576, 8576, 0, 0, 0, 0, 256, 0, 4608, 0, 0",
      /* 1811 */ "4608, 4608, 0, 0, 0, 0, 0, 4608, 4608, 4608, 0, 0, 0, 0, 512, 640, 0, 7808, 0, 0, 4608, 0, 0, 4608",
      /* 1835 */ "0, 0, 0, 0, 512, 640, 0, 8192, 8704, 0, 0, 0, 0, 0, 0, 0, 1152, 0, 8704, 8704, 8704, 8704, 8704",
      /* 1858 */ "8704, 8704, 8704, 0, 384, 0, 0, 384, 0, 0, 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 4736, 0, 0",
      /* 1881 */ "0, 0, 0, 0, 4608, 0, 0, 0, 8832, 0, 0, 0, 0, 0, 0, 7424, 0, 0, 0, 7424, 8832, 0, 0, 8832, 0, 0, 8832",
      /* 1908 */ "8832, 8832, 8832, 8832, 8832, 8832, 8832, 0, 0, 0, 0, 4864, 0, 0, 0, 0, 0, 0, 7808, 7808, 5504, 0, 0",
      /* 1931 */ "0, 512, 640, 0, 0, 0, 0, 512, 6912, 6912, 0, 6912, 0, 0, 0, 6912, 0, 0, 0, 0, 0, 5632, 0, 0, 512",
      /* 1956 */ "640, 0, 0, 0, 0, 4992, 0, 0, 0, 0, 0, 0, 8704, 8704, 5632, 0, 0, 5632, 0, 0, 5632, 5632, 5632, 5632",
      /* 1980 */ "5632, 5632, 5632, 5632, 0, 0, 0, 8960, 0, 512, 640, 0, 0, 0, 0, 6272, 0, 0, 0, 0, 0, 0, 8448, 8448",
      /* 2004 */ "0, 0, 0, 9088, 512, 640, 0, 0, 0, 0, 6656, 640, 6656, 0, 6656, 0, 0, 0, 6656, 0, 9088, 9088, 0, 0",
      /* 2028 */ "9088, 9088, 9088, 9088, 9088, 9088, 0, 0, 0, 9088, 9088, 9088, 0, 9088, 0, 0, 9088, 9088, 0, 0, 0",
      /* 2049 */ "9088, 9088, 9088, 9088, 0, 0, 9088, 0, 0, 0, 0, 256, 0, 9216, 0, 9216, 9216, 9216, 0, 9216, 0, 9216",
      /* 2071 */ "9216, 9216, 9216, 9216, 9216, 9216, 0, 0, 0, 0, 5760, 0, 0, 0, 0, 0, 0, 8192, 0, 0, 0, 9344, 0, 0, 0",
      /* 2096 */ "9344, 0, 0, 0, 0, 0, 0, 0, 5248, 0, 0, 0, 9344, 9344, 9344, 9344, 9344, 9344, 9344, 0, 0, 6400, 0, 0",
      /* 2120 */ "0, 0, 0, 0, 0"
    };
    decode(s1, TRANSITION);
  }

  private static final int[] EXPECTED = new int[198];
  static
  {
    final String s1[] =
    {
      /*   0 */ "141, 141, 141, 141, 141, 56, 149, 141, 141, 141, 61, 62, 66, 67, 71, 71, 71, 73, 165, 87, 77, 194, 81",
      /*  23 */ "85, 91, 95, 99, 103, 107, 111, 115, 119, 123, 127, 131, 135, 139, 141, 142, 146, 141, 193, 141, 153",
      /*  44 */ "163, 169, 173, 177, 179, 57, 156, 159, 183, 187, 190, 141, 8, 16, 0, 0, 80, 0, 480, 480, 480, 480",
      /*  66 */ "-508, -512, -512, -508, -508, -508, -508, -508, -508, -28, 0, 134217728, 0, 0, 16, 256, 512, 1024",
      /*  84 */ "2048, 0, 8192, 524288, 2097152, 8388608, 67108864, 2621440, -1879048192, 67117056, 134225920, 8192",
      /*  96 */ "4096, 3670016, 536870920, -1879040000, 679477248, 3678208, 536879112, 3670016, 713031680, 536944648",
      /* 106 */ "603987976, 3678208, 713039872, 70778880, 729808896, 729817088, 730341384, 731914248, 796958728",
      /* 115 */ "-1149231104, -1149214720, 2055, 3670023, 3670031, 10247, 3678215, 3678223, 67110919, 67119111",
      /* 125 */ "67250183, 69740551, 70780935, 70789127, 91752455, 92014599, 91760647, 92022791, 637274119, 763103239",
      /* 135 */ "771491847, 637282311, 771500039, 1836853255, -1073993713, 8192, 0, 0, 0, 0, 1, 4, 0, 16, 0, 0, 0, 2",
      /* 153 */ "64, 64, 68, 64, 64, 26, 90, 90, 286, 350, 0, 128, 64, 128, 32768, 131072, 64, 192, 260, 128, 192, 192",
      /* 175 */ "324, 192, 268, 128, 192, 192, 193, 225, 286, 286, 350, 350, 414, 414, 414, 478, 478, 479, 0, 0, 32",
      /* 196 */ "64, 128"
    };
    decode(s1, EXPECTED);
  }

  private static final int[] CASEID = new int[1831];
  static
  {
    final String s1[] =
    {
      /*    0 */ "733, 636, 600, 731, 604, 624, 610, 614, 622, 628, 634, 636, 640, 644, 648, 663, 1120, 1121, 1120",
      /*   19 */ "999, 668, 671, 675, 1017, 697, 1120, 1120, 1130, 1120, 999, 659, 705, 709, 657, 713, 1120, 1120",
      /*   37 */ "1139, 1148, 729, 719, 722, 726, 1120, 792, 733, 636, 600, 1120, 620, 733, 636, 600, 1120, 620, 733",
      /*   56 */ "636, 600, 1120, 620, 624, 610, 614, 622, 737, 753, 743, 747, 751, 757, 668, 671, 675, 1017, 697, 753",
      /*   76 */ "743, 763, 681, 757, 1120, 1120, 1180, 1051, 999, 789, 1120, 1157, 853, 767, 773, 776, 780, 1120, 814",
      /*   95 */ "811, 1120, 1162, 836, 798, 1120, 1120, 1130, 1120, 999, 733, 636, 600, 804, 808, 733, 636, 600, 1120",
      /*  114 */ "620, 850, 1120, 1167, 894, 820, 826, 829, 833, 840, 844, 857, 860, 864, 868, 872, 881, 884, 888",
      /*  133 */ "1120, 875, 811, 1120, 1162, 836, 798, 668, 671, 675, 898, 902, 719, 722, 726, 1120, 792, 733, 636",
      /*  152 */ "600, 918, 808, 684, 1120, 1180, 654, 1015, 1120, 1120, 1120, 922, 932, 733, 636, 600, 939, 808, 1120",
      /*  171 */ "1120, 1180, 943, 999, 733, 636, 600, 977, 990, 826, 829, 833, 1005, 844, 733, 636, 600, 1009, 990",
      /*  190 */ "1120, 1120, 1180, 678, 1015, 733, 636, 600, 1021, 990, 1120, 1120, 1180, 678, 1015, 733, 636, 600",
      /*  208 */ "1021, 990, 1120, 1120, 1180, 678, 1015, 684, 1120, 1180, 654, 1015, 1120, 1120, 1180, 943, 1043",
      /*  225 */ "1058, 1120, 1194, 1046, 1061, 1120, 1120, 1120, 1120, 1050, 1120, 1120, 1189, 1120, 1120, 1120, 1120",
      /*  242 */ "1180, 1120, 1055, 1071, 1074, 1078, 1082, 1086, 1120, 1120, 1120, 1090, 1120, 668, 671, 675, 1094",
      /*  259 */ "1098, 668, 671, 675, 1102, 1098, 668, 671, 675, 1106, 1098, 668, 671, 675, 1106, 1098, 1120, 1120",
      /*  277 */ "1120, 1090, 1110, 1114, 1120, 1120, 1119, 1120, 905, 1120, 1120, 908, 1120, 715, 636, 1125, 943, 999",
      /*  295 */ "1120, 1120, 1120, 1120, 1129, 826, 829, 833, 1134, 844, 996, 1120, 1203, 986, 993, 983, 1120, 1232",
      /*  313 */ "973, 980, 1138, 1120, 1120, 1138, 1120, 690, 1120, 1120, 687, 1120, 800, 1143, 1147, 1152, 1120",
      /*  330 */ "1120, 1120, 1120, 1120, 1050, 684, 1120, 1180, 654, 1015, 1114, 1120, 1120, 1156, 1120, 905, 1120",
      /*  347 */ "1120, 952, 1120, 1120, 1120, 701, 1115, 1015, 1120, 1120, 1180, 731, 999, 1114, 1120, 1120, 1161",
      /*  364 */ "1120, 905, 1120, 1120, 955, 1120, 684, 1120, 1180, 654, 1015, 1120, 1120, 1130, 1120, 999, 1120",
      /*  381 */ "1120, 1120, 1166, 1120, 1120, 1120, 1120, 961, 1120, 1120, 1120, 1120, 1171, 1120, 715, 636, 1175",
      /*  398 */ "943, 999, 1120, 1120, 1180, 943, 999, 1120, 1120, 664, 606, 617, 800, 1143, 1179, 1152, 1120, 1120",
      /*  416 */ "1120, 1120, 1184, 1120, 1120, 1120, 1180, 699, 999, 1120, 1120, 701, 1115, 1015, 1120, 1120, 1120",
      /*  433 */ "1120, 958, 1120, 1120, 651, 1120, 1120, 1120, 1120, 1120, 1188, 1120, 1120, 1120, 964, 1120, 1120",
      /*  450 */ "1120, 1120, 1120, 1242, 1120, 1120, 1120, 1120, 1120, 1193, 1120, 1120, 759, 1120, 1120, 1120, 1120",
      /*  467 */ "970, 1120, 1120, 1120, 1120, 1120, 816, 1120, 1120, 1120, 1120, 693, 891, 1120, 1120, 822, 1120",
      /*  484 */ "1120, 1120, 1120, 1120, 914, 911, 1012, 1120, 1120, 928, 925, 1120, 1120, 1001, 1120, 1120, 1120",
      /*  501 */ "1120, 1120, 1198, 1120, 1120, 1120, 1120, 1120, 1024, 1120, 1120, 1237, 1120, 1120, 1120, 1120, 877",
      /*  518 */ "1120, 1120, 1120, 1120, 1120, 1202, 1120, 1120, 1120, 1120, 1030, 1120, 949, 1120, 1207, 935, 946",
      /*  535 */ "1120, 1120, 1120, 1027, 1120, 1120, 1120, 1033, 1120, 1120, 1120, 1120, 1211, 739, 1036, 1120, 1120",
      /*  552 */ "1215, 1219, 967, 786, 1120, 1223, 1227, 783, 786, 1120, 1223, 1039, 783, 1120, 1120, 1231, 794, 847",
      /*  570 */ "1120, 1120, 1120, 630, 1120, 1120, 1120, 1120, 1236, 1120, 1120, 1120, 1120, 1064, 1120, 1120, 1120",
      /*  587 */ "1120, 1241, 1120, 1120, 1120, 1120, 1067, 1120, 1120, 1120, 1120, 769, 1120, 1304, 1310, 1301, 1475",
      /*  604 */ "1253, 1278, 1310, 1310, 1262, 1310, 1258, 1258, 1258, 1258, 1363, 1310, 1311, 1310, 1263, 1310, 1310",
      /*  621 */ "1278, 1310, 1310, 1312, 1310, 1362, 1258, 1313, 1268, 1310, 1310, 1281, 1310, 1307, 1303, 1246, 1246",
      /*  638 */ "1246, 1246, 1246, 1310, 1301, 1475, 1302, 1246, 1246, 1248, 1246, 1275, 1309, 1310, 1287, 1310, 1310",
      /*  655 */ "1301, 1248, 1308, 1310, 1270, 1310, 1590, 1350, 1581, 1310, 1310, 1310, 1264, 1738, 1310, 1317, 1322",
      /*  672 */ "1322, 1322, 1322, 1318, 1310, 1737, 1310, 1301, 1253, 1308, 1310, 1327, 1310, 1303, 1309, 1310, 1340",
      /*  689 */ "1310, 1310, 1341, 1310, 1310, 1346, 1346, 1708, 1332, 1325, 1310, 1310, 1301, 1310, 1310, 1475, 1350",
      /*  706 */ "1350, 1350, 1350, 1591, 1310, 1269, 1367, 1271, 1353, 1310, 1310, 1303, 1246, 1601, 1310, 1379, 1384",
      /*  723 */ "1384, 1384, 1384, 1380, 1310, 1600, 1310, 1375, 1310, 1310, 1307, 1310, 1303, 1246, 1313, 1261, 1310",
      /*  740 */ "1310, 1359, 1677, 1392, 1392, 1392, 1392, 1413, 1310, 1326, 1336, 1326, 1328, 1327, 1310, 1412, 1392",
      /*  757 */ "1410, 1395, 1310, 1310, 1369, 1310, 1413, 1310, 1326, 1367, 1309, 1752, 1310, 1310, 1424, 1310, 1547",
      /*  774 */ "1310, 1428, 1433, 1433, 1433, 1433, 1429, 1310, 1546, 1310, 1405, 1310, 1310, 1406, 1805, 1310, 1417",
      /*  791 */ "1423, 1310, 1387, 1310, 1310, 1310, 1812, 1448, 1445, 1310, 1310, 1636, 1641, 1302, 1304, 1249, 1458",
      /*  808 */ "1303, 1275, 1309, 1310, 1445, 1448, 1310, 1436, 1310, 1310, 1441, 1310, 1467, 1481, 1310, 1310, 1716",
      /*  825 */ "1310, 1597, 1310, 1615, 1493, 1493, 1493, 1493, 1616, 1310, 1596, 1310, 1468, 1454, 1454, 1614, 1616",
      /*  842 */ "1491, 1598, 1615, 1613, 1599, 1310, 1477, 1310, 1310, 1485, 1467, 1310, 1449, 1419, 1419, 1438, 1310",
      /*  859 */ "1501, 1497, 1497, 1497, 1497, 1502, 1310, 1437, 1439, 1814, 1502, 1500, 1815, 1501, 1500, 1440, 1310",
      /*  876 */ "1514, 1310, 1310, 1761, 1310, 1556, 1310, 1506, 1511, 1511, 1511, 1511, 1507, 1310, 1555, 1310, 1515",
      /*  893 */ "1310, 1310, 1574, 1487, 1483, 1821, 1519, 1523, 1533, 1527, 1531, 1573, 1310, 1595, 1310, 1310, 1605",
      /*  910 */ "1310, 1310, 1624, 1310, 1310, 1624, 1724, 1726, 1302, 1246, 1249, 1308, 1400, 1539, 1284, 1310, 1632",
      /*  927 */ "1310, 1310, 1632, 1742, 1744, 1296, 1310, 1545, 1310, 1648, 1651, 1776, 1302, 1246, 1254, 1308, 1301",
      /*  944 */ "1305, 1310, 1310, 1649, 1310, 1310, 1650, 1777, 1310, 1655, 1310, 1310, 1661, 1310, 1310, 1673, 1310",
      /*  961 */ "1310, 1681, 1310, 1310, 1687, 1310, 1310, 1690, 1310, 1310, 1703, 1310, 1310, 1710, 1628, 1630, 1301",
      /*  978 */ "1305, 1307, 1310, 1711, 1310, 1310, 1712, 1631, 1310, 1718, 1620, 1622, 1303, 1278, 1309, 1310, 1719",
      /*  995 */ "1310, 1310, 1720, 1623, 1310, 1728, 1310, 1310, 1290, 1310, 1614, 1493, 1491, 1598, 1302, 1304, 1254",
      /* 1012 */ "1310, 1732, 1736, 1310, 1746, 1310, 1310, 1784, 1310, 1301, 1304, 1254, 1310, 1756, 1310, 1310, 1762",
      /* 1029 */ "1310, 1310, 1766, 1310, 1310, 1778, 1310, 1310, 1790, 1310, 1310, 1799, 1802, 1804, 1307, 1728, 1309",
      /* 1046 */ "1310, 1806, 1551, 1553, 1827, 1310, 1310, 1310, 1306, 1307, 1728, 1310, 1310, 1808, 1554, 1310, 1807",
      /* 1063 */ "1310, 1310, 1819, 1310, 1310, 1825, 1310, 1310, 1343, 1310, 1567, 1560, 1560, 1560, 1560, 1568, 1310",
      /* 1080 */ "1342, 1310, 1566, 1560, 1563, 1344, 1567, 1565, 1345, 1310, 1400, 1539, 1310, 1310, 1583, 1572, 1768",
      /* 1097 */ "1310, 1527, 1325, 1573, 1310, 1821, 1578, 1587, 1310, 1583, 1578, 1587, 1310, 1293, 1310, 1545, 1310",
      /* 1114 */ "1535, 1310, 1310, 1310, 1308, 1607, 1310, 1310, 1310, 1310, 1300, 1246, 1310, 1310, 1475, 1356, 1310",
      /* 1131 */ "1310, 1310, 1336, 1614, 1493, 1611, 1598, 1338, 1310, 1310, 1310, 1373, 1641, 1641, 1641, 1641, 1642",
      /* 1148 */ "1310, 1310, 1310, 1374, 1388, 1646, 1310, 1310, 1663, 1310, 1310, 1310, 1398, 1683, 1310, 1310, 1310",
      /* 1165 */ "1453, 1679, 1310, 1310, 1310, 1472, 1400, 1402, 1310, 1310, 1304, 1310, 1310, 1475, 1637, 1310, 1310",
      /* 1182 */ "1310, 1475, 1757, 1667, 1310, 1310, 1461, 1310, 1310, 1310, 1541, 1698, 1310, 1310, 1310, 1553, 1699",
      /* 1199 */ "1750, 1310, 1310, 1704, 1310, 1310, 1310, 1622, 1648, 1310, 1310, 1772, 1675, 1310, 1310, 1782, 1689",
      /* 1216 */ "1310, 1310, 1788, 1794, 1310, 1310, 1788, 1404, 1310, 1310, 1804, 1704, 1404, 1802, 1804, 1669, 1310",
      /* 1233 */ "1310, 1310, 1630, 1795, 1310, 1310, 1310, 1657, 1463, 1310, 1310, 1310, 1694, 65830, 65830, 65830",
      /* 1249 */ "65830, 0, 65830, 65830, 0, 65830, 0, 65830, 0, 4, 4, 4, 4, 0, 0, 0, 131076, 0, 0, 14, 0, 0, 0, 49156",
      /* 1273 */ "0, 49156, 65830, 0, 25266, 65830, 0, 25266, 0, 0, 655378, 0, 0, 720904, 0, 0, 729106, 0, 0, 827410",
      /* 1293 */ "0, 0, 884740, 0, 0, 884740, 737288, 99178, 0, 0, 0, 65830, 65830, 65830, 0, 0, 65830, 0, 0, 0, 0, 4",
      /* 1315 */ "0, 4, 0, 589828, 589828, 589828, 0, 589828, 589828, 589828, 589828, 0, 0, 0, 82454, 0, 0, 0, 73746",
      /* 1334 */ "0, 81938, 0, 107058, 0, 0, 0, 753668, 0, 0, 0, 409604, 0, 0, 0, 345494, 49156, 49156, 49156, 49156",
      /* 1354 */ "0, 25266, 0, 0, 917508, 0, 0, 1097748, 0, 4, 4, 4, 0, 0, 123218, 0, 0, 0, 761874, 0, 180228, 0, 0",
      /* 1377 */ "180228, 0, 0, 245764, 245764, 245764, 0, 245764, 245764, 245764, 245764, 0, 0, 0, 98318, 82454",
      /* 1393 */ "82454, 82454, 82454, 0, 25266, 0, 16398, 0, 0, 0, 770052, 0, 0, 0, 788630, 788630, 788630, 0, 82454",
      /* 1412 */ "0, 82454, 82454, 82454, 0, 0, 213028, 213028, 213028, 0, 213028, 213028, 0, 0, 0, 147720, 0, 278536",
      /* 1430 */ "278536, 278536, 0, 278536, 278536, 278536, 278536, 0, 0, 0, 163844, 0, 0, 0, 197786, 0, 229380",
      /* 1447 */ "229380, 229380, 0, 0, 0, 213028, 0, 229380, 229380, 0, 229380, 0, 65830, 205874, 0, 16650, 0, 0, 0",
      /* 1466 */ "1100666, 263494, 0, 0, 0, 229380, 0, 263494, 294920, 0, 16706, 0, 0, 0, 649842, 0, 294920, 263494",
      /* 1484 */ "263494, 0, 263494, 263494, 263494, 0, 49166, 360452, 0, 360452, 360452, 360452, 360452, 163844",
      /* 1498 */ "163844, 163844, 163844, 0, 163844, 163844, 163844, 0, 0, 311300, 311300, 311300, 0, 311300, 311300",
      /* 1513 */ "311300, 311300, 0, 0, 0, 247218, 655364, 655364, 270354, 376852, 278546, 0, 65550, 376852, 0, 73746",
      /* 1529 */ "655364, 81934, 589828, 0, 0, 376852, 0, 0, 0, 802824, 387442, 770052, 0, 0, 0, 835588, 393234, 0, 0",
      /* 1548 */ "0, 278536, 0, 638980, 638980, 0, 638980, 0, 0, 0, 311300, 0, 409604, 409604, 409604, 409604, 0",
      /* 1565 */ "409604, 0, 0, 409604, 409604, 409604, 0, 655364, 655364, 0, 0, 0, 263494, 655364, 655364, 270354, 0",
      /* 1582 */ "25330, 0, 0, 0, 655364, 278546, 0, 65550, 0, 49156, 49156, 49156, 0, 819208, 0, 0, 0, 360452, 0, 0",
      /* 1602 */ "0, 245764, 0, 0, 862434, 0, 0, 0, 854018, 360452, 0, 360452, 0, 0, 360452, 360452, 360452, 0",
      /* 1620 */ "1196036, 1196036, 0, 1196036, 0, 0, 0, 378470, 1179652, 1179652, 0, 1179652, 0, 0, 0, 393236, 0",
      /* 1637 */ "1146888, 1146888, 1146888, 0, 1146888, 1146888, 1146888, 1146888, 953066, 387442, 114702, 0, 0, 0",
      /* 1651 */ "854470, 854470, 854470, 854470, 0, 919794, 0, 0, 0, 870818, 0, 953298, 0, 0, 0, 911378, 387442",
      /* 1668 */ "600994, 0, 0, 0, 953066, 0, 712722, 0, 0, 0, 999444, 0, 0, 0, 1081348, 0, 0, 0, 944962, 0, 163858, 0",
      /* 1690 */ "0, 0, 1130516, 1130516, 173074, 0, 0, 173026, 180242, 0, 0, 0, 460754, 786450, 0, 0, 0, 518178, 0",
      /* 1709 */ "247202, 0, 0, 0, 1179652, 1179652, 1179652, 0, 811026, 0, 0, 0, 1196036, 1196036, 1196036, 0, 304754",
      /* 1726 */ "0, 378470, 0, 0, 25266, 0, 0, 311314, 319506, 335890, 344082, 0, 0, 0, 589828, 0, 327698, 393236, 0",
      /* 1745 */ "393236, 0, 0, 25266, 65830, 0, 469170, 0, 0, 32782, 213028, 843794, 0, 0, 0, 592658, 884754, 0, 0, 0",
      /* 1765 */ "606226, 0, 526594, 0, 0, 65550, 0, 0, 854470, 0, 870818, 0, 854470, 0, 0, 0, 614418, 0, 131086, 0, 0",
      /* 1786 */ "90130, 0, 0, 1130516, 0, 0, 131086, 999444, 1114136, 0, 0, 0, 625490, 0, 526594, 0, 788630, 788630",
      /* 1804 */ "0, 788630, 0, 0, 0, 638980, 638980, 638980, 0, 649826, 0, 0, 163844, 163844, 0, 0, 633826, 0, 0",
      /* 1823 */ "286738, 655364, 0, 1100810, 0, 0, 477650, 0"
    };
    decode(s1, CASEID);
  }

  private static final int[] TOKENSET = new int[120];
  static
  {
    final String s1[] =
    {
      /*   0 */ "55, 54, 70, 27, 54, 19, 56, 30, 48, 51, 51, 51, 54, 57, 54, 56, 30, 46, 48, 47, 19, 69, 51, 47, 65",
      /*  25 */ "69, 48, 47, 66, 48, 68, 42, 40, 67, 32, 59, 66, 63, 39, 62, 39, 62, 39, 42, 38, 42, 8, 0, 26, 64, 28",
      /*  51 */ "58, 61, 60, 60, 34, 20, 21, 53, 8, 64, 42, 42, 20, 21, 50, 8, 42, 20, 21, 33, 24, 20, 21, 42, 19, 2",
      /*  77 */ "3, 22, 52, 32, 25, 49, 28, 25, 33, 18, 12, 1, 11, 23, 7, 17, 9, 6, 31, 15, 35, 41, 16, 22, 10, 0, 14",
      /* 104 */ "2, 3, 45, 4, 13, 37, 36, 43, 44, 29, 5, 2, 3, 2, 3, 6"
    };
    decode(s1, TOKENSET);
  }

  private static final int[] APPENDIX = new int[18];
  static
  {
    final String s1[] =
    {
      /*  0 */ "2, 352265, 8353, 106514, 12633, 106514, 131747, 147460, 45065, 327682, 40969, 327682, 296329, 385026",
      /* 14 */ "300497, 385026, 499722, 548874"
    };
    decode(s1, APPENDIX);
  }

  private static final int[] LOOKBACK = new int[613];
  static
  {
    final String s1[] =
    {
      /*   0 */ "195, 195, 195, 195, 195, 193, 201, 196, 204, 204, 204, 207, 217, 217, 217, 212, 220, 223, 226, 235",
      /*  20 */ "235, 244, 235, 244, 235, 244, 257, 257, 266, 257, 266, 257, 266, 279, 288, 288, 288, 288, 297, 297",
      /*  40 */ "297, 297, 195, 195, 195, 195, 306, 306, 306, 306, 313, 313, 313, 313, 320, 327, 195, 330, 333, 333",
      /*  60 */ "333, 336, 336, 336, 339, 339, 339, 342, 356, 356, 356, 349, 359, 362, 365, 195, 368, 371, 371, 371",
      /*  80 */ "374, 384, 384, 384, 379, 195, 195, 195, 195, 387, 392, 397, 402, 407, 407, 407, 407, 195, 195, 410",
      /* 100 */ "410, 410, 413, 418, 423, 426, 426, 426, 195, 195, 195, 195, 429, 442, 442, 442, 432, 437, 450, 445",
      /* 120 */ "453, 195, 195, 195, 456, 456, 456, 462, 467, 472, 482, 477, 477, 477, 477, 477, 487, 490, 459, 459",
      /* 140 */ "459, 495, 500, 505, 515, 510, 510, 510, 510, 510, 520, 195, 523, 526, 526, 526, 531, 195, 534, 534",
      /* 160 */ "534, 537, 547, 547, 547, 547, 550, 555, 560, 565, 568, 568, 568, 568, 540, 571, 195, 195, 195, 574",
      /* 180 */ "577, 585, 580, 588, 591, 195, 195, 195, 596, 599, 607, 602, 610, 3, 2, 0, 6, 6, 3, 4, 0, 5, 5, 0, 7",
      /* 205 */ "7, 0, 13, 12, 9, 8, 0, 13, 14, 9, 10, 0, 11, 11, 0, 15, 15, 0, 16, 16, 0, 28, 26, 27, 26, 21, 19, 20",
      /* 233 */ "19, 0, 28, 29, 27, 29, 21, 22, 20, 22, 0, 39, 40, 35, 36, 28, 30, 27, 29, 21, 23, 20, 22, 0, 28, 31",
      /* 259 */ "27, 31, 21, 24, 20, 24, 0, 39, 41, 35, 37, 28, 32, 27, 31, 21, 25, 20, 24, 0, 39, 38, 35, 34, 28, 38",
      /* 285 */ "21, 34, 0, 39, 40, 35, 36, 28, 40, 21, 36, 0, 39, 41, 35, 37, 28, 41, 21, 37, 0, 51, 50, 47, 46, 43",
      /* 311 */ "42, 0, 51, 52, 47, 48, 43, 44, 0, 51, 53, 47, 49, 43, 45, 0, 54, 54, 0, 56, 56, 0, 57, 57, 0, 59, 58",
      /* 338 */ "0, 59, 60, 0, 69, 68, 65, 64, 62, 61, 0, 69, 70, 65, 66, 62, 63, 0, 67, 67, 0, 71, 71, 0, 72, 72, 0",
      /* 365 */ "73, 73, 0, 75, 75, 0, 76, 76, 0, 82, 81, 78, 77, 0, 82, 83, 78, 79, 0, 80, 80, 0, 94, 93, 86, 85, 0",
      /* 392 */ "94, 95, 86, 87, 0, 94, 96, 86, 88, 0, 91, 91, 90, 90, 0, 92, 92, 0, 97, 98, 0, 106, 105, 100, 99, 0",
      /* 418 */ "106, 107, 100, 101, 0, 103, 103, 0, 104, 104, 0, 109, 108, 0, 114, 113, 109, 110, 0, 114, 115, 109",
      /* 440 */ "111, 0, 112, 112, 0, 118, 118, 116, 116, 0, 117, 117, 0, 119, 119, 0, 122, 121, 0, 122, 123, 0, 132",
      /* 463 */ "131, 125, 124, 0, 132, 133, 125, 126, 0, 132, 134, 125, 126, 0, 130, 130, 127, 127, 0, 129, 128, 128",
      /* 485 */ "128, 0, 132, 135, 0, 143, 150, 129, 136, 0, 146, 145, 139, 138, 0, 146, 147, 139, 140, 0, 146, 148",
      /* 507 */ "139, 140, 0, 144, 144, 141, 141, 0, 143, 142, 142, 142, 0, 146, 149, 0, 151, 151, 0, 154, 153, 152",
      /* 529 */ "152, 0, 154, 155, 0, 157, 157, 0, 159, 158, 0, 171, 173, 163, 165, 159, 160, 0, 161, 161, 0, 171, 170",
      /* 552 */ "163, 162, 0, 171, 172, 163, 164, 0, 167, 167, 166, 166, 0, 168, 168, 0, 169, 169, 0, 174, 174, 0, 177",
      /* 575 */ "176, 0, 177, 178, 0, 181, 181, 179, 179, 0, 180, 180, 0, 182, 182, 0, 192, 192, 183, 183, 0, 186, 185",
      /* 598 */ "0, 186, 187, 0, 190, 190, 188, 188, 0, 189, 189, 0, 191, 191, 0"
    };
    decode(s1, LOOKBACK);
  }

  private static final int[] GOTO = new int[954];
  static
  {
    final String s1[] =
    {
      /*   0 */ "344, 393, 393, 393, 544, 393, 364, 393, 535, 393, 393, 393, 490, 393, 393, 393, 532, 499, 393, 393",
      /*  20 */ "350, 393, 393, 393, 356, 393, 393, 393, 393, 393, 450, 393, 456, 393, 394, 393, 362, 393, 393, 393",
      /*  40 */ "368, 393, 393, 393, 374, 393, 393, 393, 393, 393, 364, 393, 508, 393, 393, 393, 487, 499, 393, 393",
      /*  60 */ "517, 393, 393, 393, 380, 393, 393, 393, 386, 393, 393, 393, 569, 393, 393, 393, 393, 393, 393, 392",
      /*  80 */ "398, 393, 393, 393, 404, 393, 393, 393, 410, 393, 393, 393, 370, 393, 393, 393, 417, 393, 393, 416",
      /* 100 */ "393, 393, 551, 393, 427, 393, 393, 421, 393, 393, 393, 426, 388, 431, 406, 567, 480, 393, 394, 393",
      /* 120 */ "437, 393, 393, 393, 443, 393, 393, 393, 393, 433, 449, 529, 454, 393, 393, 393, 460, 393, 393, 393",
      /* 140 */ "466, 393, 393, 393, 422, 393, 393, 416, 472, 393, 393, 393, 478, 393, 393, 393, 484, 393, 393, 393",
      /* 160 */ "496, 393, 393, 393, 505, 393, 393, 393, 514, 393, 393, 393, 523, 393, 393, 393, 541, 393, 393, 393",
      /* 180 */ "393, 393, 393, 558, 393, 393, 550, 393, 393, 393, 393, 439, 555, 393, 393, 393, 393, 393, 393, 573",
      /* 200 */ "393, 346, 393, 526, 382, 581, 393, 567, 400, 431, 406, 567, 382, 578, 474, 567, 382, 588, 474, 567",
      /* 220 */ "393, 445, 449, 529, 393, 519, 393, 393, 393, 574, 393, 393, 592, 393, 393, 393, 393, 393, 393, 546",
      /* 240 */ "601, 393, 393, 416, 393, 393, 393, 492, 393, 393, 393, 537, 393, 393, 393, 561, 393, 393, 393, 564",
      /* 260 */ "393, 376, 596, 526, 393, 393, 600, 393, 605, 393, 393, 393, 393, 519, 393, 393, 393, 574, 393, 393",
      /* 280 */ "609, 393, 393, 393, 613, 393, 393, 393, 393, 412, 393, 393, 393, 501, 393, 393, 617, 393, 393, 393",
      /* 300 */ "621, 393, 393, 393, 393, 393, 393, 462, 393, 393, 393, 468, 393, 510, 393, 526, 625, 393, 393, 393",
      /* 320 */ "629, 393, 393, 393, 393, 393, 352, 393, 393, 376, 633, 526, 393, 358, 584, 393, 637, 393, 393, 393",
      /* 340 */ "641, 393, 393, 393, 645, 688, 689, 689, 660, 854, 705, 709, 689, 689, 689, 933, 805, 714, 689, 689",
      /* 360 */ "689, 939, 681, 688, 689, 689, 690, 689, 678, 688, 689, 689, 715, 689, 675, 688, 689, 689, 751, 906",
      /* 380 */ "727, 688, 689, 689, 789, 689, 730, 688, 689, 689, 789, 865, 719, 689, 689, 689, 689, 710, 850, 709",
      /* 400 */ "689, 689, 789, 887, 896, 688, 689, 689, 802, 689, 950, 688, 689, 689, 810, 689, 833, 689, 689, 689",
      /* 420 */ "724, 894, 689, 689, 689, 733, 784, 689, 689, 689, 779, 794, 798, 689, 689, 814, 823, 839, 688, 689",
      /* 440 */ "689, 849, 689, 760, 688, 689, 689, 877, 823, 826, 689, 689, 689, 817, 845, 688, 689, 689, 880, 689",
      /* 460 */ "856, 688, 689, 689, 902, 689, 908, 688, 689, 689, 917, 689, 929, 688, 689, 689, 941, 689, 654, 688",
      /* 480 */ "689, 689, 948, 689, 935, 688, 689, 689, 700, 699, 689, 694, 689, 689, 689, 901, 663, 688, 689, 689",
      /* 500 */ "704, 689, 689, 689, 927, 648, 688, 689, 689, 709, 689, 689, 763, 689, 666, 688, 689, 689, 714, 689",
      /* 520 */ "689, 780, 689, 766, 688, 689, 689, 736, 689, 689, 736, 831, 689, 695, 699, 689, 688, 689, 689, 689",
      /* 540 */ "900, 651, 688, 689, 689, 757, 689, 689, 672, 689, 843, 689, 689, 689, 882, 657, 688, 689, 689, 837",
      /* 560 */ "689, 689, 785, 689, 689, 790, 689, 689, 809, 689, 689, 819, 689, 720, 689, 689, 689, 886, 864, 869",
      /* 580 */ "689, 689, 860, 689, 689, 945, 689, 689, 864, 873, 689, 689, 669, 688, 689, 689, 684, 912, 916, 689",
      /* 600 */ "921, 689, 689, 689, 891, 754, 688, 689, 689, 739, 688, 689, 689, 745, 688, 689, 689, 923, 688, 689",
      /* 620 */ "689, 827, 709, 689, 689, 769, 688, 689, 689, 772, 688, 689, 689, 775, 912, 916, 689, 748, 688, 689",
      /* 640 */ "689, 742, 688, 689, 689, 6, 0, 4121, 0, 0, 221193, 0, 0, 226193, 0, 0, 237588, 0, 0, 241673, 0, 0",
      /* 662 */ "249865, 0, 0, 253972, 0, 0, 262164, 0, 0, 267513, 0, 0, 270345, 0, 0, 270348, 0, 0, 278540, 0, 0",
      /* 683 */ "286732, 0, 0, 288009, 491524, 8409, 0, 0, 0, 0, 16441, 12697, 0, 0, 0, 24649, 29121, 0, 0, 0, 24681",
      /* 704 */ "32777, 0, 0, 0, 49201, 53561, 0, 0, 0, 74329, 61665, 0, 0, 0, 111233, 94833, 0, 0, 0, 245769, 0",
      /* 725 */ "385601, 389809, 0, 0, 369113, 0, 0, 372745, 0, 0, 389873, 0, 0, 410577, 0, 0, 464153, 0, 0, 464217, 0",
      /* 746 */ "0, 466953, 0, 0, 487433, 0, 0, 507908, 0, 0, 606244, 0, 20521, 57348, 0, 0, 311308, 0, 0, 331785, 0",
      /* 767 */ "0, 336829, 0, 0, 337217, 0, 0, 339977, 0, 0, 349513, 491524, 106540, 0, 0, 0, 259065, 94865, 0, 0, 0",
      /* 788 */ "279585, 126985, 0, 0, 0, 283793, 401417, 196612, 196612, 196612, 32777, 0, 221188, 221188, 0, 196612",
      /* 804 */ "221188, 0, 57433, 360569, 16388, 131945, 0, 0, 0, 312729, 177057, 181121, 352260, 0, 65993, 0, 0",
      /* 821 */ "78433, 0, 0, 344068, 185257, 434180, 0, 0, 0, 319497, 0, 188425, 0, 0, 115465, 0, 0, 177073, 0, 0",
      /* 841 */ "148193, 0, 0, 450572, 0, 0, 200713, 0, 419009, 0, 0, 0, 376841, 0, 253961, 0, 0, 204809, 0, 32777, 0",
      /* 862 */ "581652, 581652, 303108, 0, 0, 0, 398113, 32777, 229404, 221188, 221188, 32777, 245788, 221188, 221188",
      /* 877 */ "177057, 0, 352260, 0, 69641, 0, 0, 86569, 0, 263273, 0, 0, 0, 398161, 0, 438281, 389809, 0, 118793, 0",
      /* 897 */ "0, 98873, 0, 0, 274441, 0, 0, 0, 480689, 0, 446473, 0, 0, 208905, 0, 290825, 0, 524292, 483332",
      /* 916 */ "450569, 0, 0, 0, 484857, 0, 466972, 0, 0, 213036, 0, 316897, 0, 0, 0, 213825, 0, 344073, 0, 0, 0",
      /* 937 */ "217097, 0, 0, 548868, 0, 0, 221188, 0, 0, 516132, 532484, 0, 163852, 0, 0, 106505, 0"
    };
    decode(s1, GOTO);
  }

  private static final int[] REDUCTION = new int[150];
  static
  {
    final String s1[] =
    {
      /*   0 */ "43, 0, 0, -1, 1, -1, 1, 1, 2, -1, 3, -1, 4, -1, 5, -1, 44, 2, 6, -1, 46, 4, 45, 3, 7, 5, 8, -1, 48, 7",
      /*  30 */ "47, 6, 9, 8, 10, -1, 11, -1, 49, 9, 12, 10, 13, -1, 50, 11, 14, -1, 15, -1, 51, 12, 16, 13, 16, -1",
      /*  56 */ "17, 15, 17, 14, 18, 17, 18, 16, 19, 18, 20, 21, 20, 20, 20, 19, 20, 22, 21, -1, 22, 23, 53, 25, 52",
      /*  81 */ "24, 23, -1, 24, 27, 24, 26, 25, 28, 25, -1, 55, 30, 54, 29, 26, -1, 27, 31, 28, 31, 56, 32, 29, 33",
      /* 106 */ "30, -1, 57, 34, 31, -1, 58, 35, 32, -1, 33, -1, 34, 39, 34, 38, 34, 37, 34, 36, 35, -1, 36, 40, 37",
      /* 131 */ "41, 59, 42, 38, -1, 39, 43, 39, -1, 40, 44, 41, 45, 61, 47, 60, 46, 42, -1"
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
    "'<'",
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

                                                            // line 974 "ixml.ebnf"
                                                            private int hexBegin;
                                                              private boolean deleted;
                                                              private boolean exclusion;
                                                              private String codepoint;
                                                              private String firstCodepoint;
                                                              private String lastCodepoint;
                                                              private String clazz;
                                                              private java.util.List<Member> members;
                                                              private Grammar grammar;
                                                              private Rule rule;
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
                                                            // line 1774 "Ixml.java"
// End
