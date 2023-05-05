// This file was generated on Wed May 3, 2023 22:48 (UTC+02) by REx v5.57 which is Copyright (c) 1979-2023 by Gunther Rademacher <grd@gmx.net>
// REx command line: -glalr 1 -main -java -a java -name de.bottlecaps.markup.blitz.grammar.Ixml ixml.ebnf

package de.bottlecaps.markup.blitz.grammar;

import java.util.PriorityQueue;

import de.bottlecaps.markup.blitz.transform.PostProcess;

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
                                                            // line 12 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            grammar.addRule(new Rule(mark, nameBuilder.toString(), alts.peek()));
                                                            // line 538 "Ixml.java"
        }
        break;
      case 2:
        {
                                                            // line 15 "ixml.ebnf"
                                                            alts.pop();
                                                            // line 545 "Ixml.java"
        }
        break;
      case 3:
        {
                                                            // line 16 "ixml.ebnf"
                                                            mark = Mark.ATTRIBUTE;
                                                            // line 552 "Ixml.java"
        }
        break;
      case 4:
        {
                                                            // line 17 "ixml.ebnf"
                                                            mark = Mark.ELEMENT;
                                                            // line 559 "Ixml.java"
        }
        break;
      case 5:
        {
                                                            // line 18 "ixml.ebnf"
                                                            mark = Mark.DELETED;
                                                            // line 566 "Ixml.java"
        }
        break;
      case 6:
        {
                                                            // line 19 "ixml.ebnf"
                                                            mark = Mark.NONE;
                                                            // line 573 "Ixml.java"
        }
        break;
      case 7:
        {
                                                            // line 22 "ixml.ebnf"
                                                            alts.peek().addAlt(new Alt());
                                                            // line 580 "Ixml.java"
        }
        break;
      case 8:
        {
                                                            // line 31 "ixml.ebnf"
                                                            alts.push(new Alts());
                                                            // line 587 "Ixml.java"
        }
        break;
      case 9:
        {
                                                            // line 33 "ixml.ebnf"
                                                            Alts nested = alts.pop();
                                                            alts.peek().last().addAlts(nested);
                                                            // line 595 "Ixml.java"
        }
        break;
      case 10:
        {
                                                            // line 37 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, null);
                                                            // line 603 "Ixml.java"
        }
        break;
      case 11:
        {
                                                            // line 41 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_MORE, term, sep);
                                                            // line 612 "Ixml.java"
        }
        break;
      case 12:
        {
                                                            // line 47 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, null);
                                                            // line 620 "Ixml.java"
        }
        break;
      case 13:
        {
                                                            // line 51 "ixml.ebnf"
                                                            Term sep = alts.peek().last().removeLast();
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ONE_OR_MORE, term, sep);
                                                            // line 629 "Ixml.java"
        }
        break;
      case 14:
        {
                                                            // line 57 "ixml.ebnf"
                                                            Term term = alts.peek().last().removeLast();
                                                            alts.peek().last().addControl(Occurrence.ZERO_OR_ONE, term, null);
                                                            // line 637 "Ixml.java"
        }
        break;
      case 15:
        {
                                                            // line 61 "ixml.ebnf"
                                                            alts.peek().last().addNonterminal(mark, nameBuilder.toString());
                                                            // line 644 "Ixml.java"
        }
        break;
      case 16:
        {
                                                            // line 63 "ixml.ebnf"
                                                            nameBuilder.setLength(0);
                                                            // line 651 "Ixml.java"
        }
        break;
      case 17:
        {
                                                            // line 64 "ixml.ebnf"
                                                            nameBuilder.append(input.charAt(b0));
                                                            // line 658 "Ixml.java"
        }
        break;
      case 18:
        {
                                                            // line 76 "ixml.ebnf"
                                                            deleted = false;
                                                            // line 665 "Ixml.java"
        }
        break;
      case 19:
        {
                                                            // line 79 "ixml.ebnf"
                                                            alts.peek().last().addCharSet(charSet);
                                                            // line 672 "Ixml.java"
        }
        break;
      case 20:
        {
                                                            // line 83 "ixml.ebnf"
                                                            deleted = true;
                                                            // line 679 "Ixml.java"
        }
        break;
      case 21:
        {
                                                            // line 84 "ixml.ebnf"
                                                            alts.peek().last().addString(deleted, stringBuilder.toString());
                                                            // line 686 "Ixml.java"
        }
        break;
      case 22:
        {
                                                            // line 85 "ixml.ebnf"
                                                            alts.peek().last().addCodePoint(deleted, codePoint);
                                                            // line 693 "Ixml.java"
        }
        break;
      case 23:
        {
                                                            // line 86 "ixml.ebnf"
                                                            stringBuilder.setLength(0);
                                                            // line 700 "Ixml.java"
        }
        break;
      case 24:
        {
                                                            // line 87 "ixml.ebnf"
                                                            stringBuilder.append(input.charAt(b0));
                                                            // line 707 "Ixml.java"
        }
        break;
      case 25:
        {
                                                            // line 92 "ixml.ebnf"
                                                            hexBegin = b0;
                                                            // line 714 "Ixml.java"
        }
        break;
      case 26:
        {
                                                            // line 93 "ixml.ebnf"
                                                            codePoint = input.subSequence(hexBegin, e0).toString();
                                                            // line 721 "Ixml.java"
        }
        break;
      case 27:
        {
                                                            // line 97 "ixml.ebnf"
                                                            charSet = new CharSet(deleted, false);
                                                            // line 728 "Ixml.java"
        }
        break;
      case 28:
        {
                                                            // line 99 "ixml.ebnf"
                                                            charSet = new CharSet(deleted, true);
                                                            // line 735 "Ixml.java"
        }
        break;
      case 29:
        {
                                                            // line 102 "ixml.ebnf"
                                                            charSet.addLiteral(stringBuilder.toString(), false);
                                                            // line 742 "Ixml.java"
        }
        break;
      case 30:
        {
                                                            // line 103 "ixml.ebnf"
                                                            charSet.addLiteral(codePoint, true);
                                                            // line 749 "Ixml.java"
        }
        break;
      case 31:
        {
                                                            // line 104 "ixml.ebnf"
                                                            charSet.addRange(firstCodePoint, lastCodePoint);
                                                            // line 756 "Ixml.java"
        }
        break;
      case 32:
        {
                                                            // line 105 "ixml.ebnf"
                                                            charSet.addClass(clazz);
                                                            // line 763 "Ixml.java"
        }
        break;
      case 33:
        {
                                                            // line 107 "ixml.ebnf"
                                                            firstCodePoint = codePoint;
                                                            // line 770 "Ixml.java"
        }
        break;
      case 34:
        {
                                                            // line 108 "ixml.ebnf"
                                                            lastCodePoint = codePoint;
                                                            // line 777 "Ixml.java"
        }
        break;
      case 35:
        {
                                                            // line 109 "ixml.ebnf"
                                                            codePoint = input.subSequence(b0, e0).toString();
                                                            // line 784 "Ixml.java"
        }
        break;
      case 36:
        {
                                                            // line 114 "ixml.ebnf"
                                                            clazz += input.charAt(b0);
                                                            // line 791 "Ixml.java"
        }
        break;
      case 37:
        {
                                                            // line 116 "ixml.ebnf"
                                                            alts.peek().last().addStringInsertion(stringBuilder.toString());
                                                            // line 798 "Ixml.java"
        }
        break;
      case 38:
        {
                                                            // line 117 "ixml.ebnf"
                                                            alts.peek().last().addHexInsertion(codePoint);
                                                            // line 805 "Ixml.java"
        }
        break;
      case 39:
        {
                                                            // line 123 "ixml.ebnf"
                                                            clazz = Character.toString(input.charAt(b0));
                                                            // line 812 "Ixml.java"
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
      int i0 = (i >> 5) * 69 + s - 1;
      int f = EXPECTED[(i0 & 7) + EXPECTED[i0 >> 3]];
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

  private static final int[] INITIAL = new int[66];
  static
  {
    final String s1[] =
    {
      /*  0 */ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28",
      /* 28 */ "29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54",
      /* 54 */ "55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 66; ++i) {INITIAL[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TRANSITION = new int[2180];
  static
  {
    final String s1[] =
    {
      /*    0 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*   17 */ "1330, 1765, 1088, 1100, 1102, 1770, 1110, 1116, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*   34 */ "1092, 1088, 1100, 1102, 1770, 1110, 1116, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508",
      /*   51 */ "1513, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 2104, 1330, 1632, 1416",
      /*   68 */ "2158, 2102, 2155, 1127, 1133, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1444, 1143",
      /*   85 */ "1154, 1149, 1164, 1170, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1187, 1330, 1574, 1476, 1183, 1185",
      /*  102 */ "1180, 1195, 1578, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 1330, 1330",
      /*  119 */ "1203, 1209, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1271, 1330, 1508, 1421, 1271, 1219, 1330, 1425",
      /*  136 */ "1225, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 1719, 1330, 1330, 1948",
      /*  153 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 1790, 1330, 1235, 1242, 1330",
      /*  170 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1985, 1259, 1330, 1330, 1988, 1330, 1330",
      /*  187 */ "1330, 1330, 1330, 1330, 1330, 1991, 1330, 1508, 1267, 1990, 1279, 1457, 1287, 1293, 1330, 1330, 1330",
      /*  204 */ "1330, 1330, 1330, 1330, 1295, 1330, 1508, 1305, 1297, 1312, 1330, 1349, 1318, 1330, 1330, 1330, 1330",
      /*  221 */ "1330, 1330, 1330, 1329, 1330, 1508, 1513, 1330, 1227, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  238 */ "1330, 1330, 1330, 1330, 1536, 1449, 1330, 1135, 1330, 1330, 1454, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  255 */ "1330, 1330, 1330, 1508, 1368, 1810, 1373, 1330, 1651, 1815, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  272 */ "1330, 1330, 1555, 1481, 1330, 1156, 1330, 1330, 1486, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  289 */ "1330, 1508, 1513, 1330, 1739, 1330, 1330, 1981, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  306 */ "1508, 1513, 1330, 1330, 1818, 1339, 1345, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1329, 1330, 1508",
      /*  323 */ "1513, 1330, 1227, 1357, 1357, 1363, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1329, 1330, 1508, 1513",
      /*  340 */ "1330, 1227, 1381, 1381, 1387, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1329, 1330, 1508, 1513, 1330",
      /*  357 */ "1227, 1405, 1405, 1411, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1329, 1330, 1508, 1513, 1330, 1227",
      /*  374 */ "1433, 1433, 1439, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1329, 1330, 1508, 1513, 1330, 1227, 1465",
      /*  391 */ "1465, 1471, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1329, 1330, 1508, 1513, 1330, 1227, 1497, 1497",
      /*  408 */ "1503, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1525, 1525, 1531",
      /*  425 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1544, 1544, 1550, 1330",
      /*  442 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1563, 1563, 1569, 1330, 1330",
      /*  459 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1588, 1588, 1594, 1330, 1330, 1330",
      /*  476 */ "1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1611, 1611, 1617, 1330, 1330, 1330, 1330",
      /*  493 */ "1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1640, 1640, 1646, 1330, 1330, 1330, 1330, 1330",
      /*  510 */ "1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1659, 1659, 1665, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  527 */ "1330, 1330, 1330, 1508, 1513, 1330, 2035, 1688, 1688, 1694, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  544 */ "1330, 1330, 1508, 1513, 1330, 2035, 1707, 1707, 1713, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  561 */ "1330, 1508, 1513, 1330, 2035, 1727, 1727, 1733, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  578 */ "1508, 1513, 1330, 2035, 1747, 1747, 1753, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508",
      /*  595 */ "1513, 1330, 2035, 1778, 1778, 1784, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513",
      /*  612 */ "1330, 2035, 1798, 1798, 1804, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330",
      /*  629 */ "2035, 1826, 1826, 1832, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035",
      /*  646 */ "1846, 1846, 1852, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1871",
      /*  663 */ "1871, 1877, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1885, 1885",
      /*  680 */ "1891, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1899, 1899, 1905",
      /*  697 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1913, 1913, 1919, 1330",
      /*  714 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1927, 1927, 1933, 1330, 1330",
      /*  731 */ "1330, 1330, 1330, 1330, 1330, 1172, 1330, 1508, 1675, 1670, 1941, 1330, 1960, 1966, 1330, 1330, 1330",
      /*  748 */ "1330, 1330, 1330, 1330, 1330, 1330, 1508, 1247, 1603, 1517, 1599, 1330, 1251, 1330, 1330, 1330, 1330",
      /*  765 */ "1330, 1330, 1330, 1330, 1330, 1508, 1513, 1330, 1974, 1489, 1999, 2005, 1330, 1330, 1330, 1330, 1330",
      /*  782 */ "1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  799 */ "1330, 1329, 1330, 1508, 1513, 1321, 1227, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  816 */ "1329, 2034, 1508, 1513, 1321, 1227, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /*  833 */ "1330, 1508, 1513, 1321, 2035, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 2049",
      /*  850 */ "1508, 1513, 1321, 2035, 2043, 2057, 2063, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1759, 1508",
      /*  867 */ "1513, 1321, 2035, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1622, 1508, 1513",
      /*  884 */ "1321, 2035, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1952, 1508, 1513, 1321",
      /*  901 */ "2035, 2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1119, 1508, 1513, 1321, 2035",
      /*  918 */ "2013, 2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1211, 1508, 1513, 1321, 2035, 2013",
      /*  935 */ "2020, 2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1331, 1508, 1513, 1321, 2035, 2013, 2020",
      /*  952 */ "2026, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 2071, 1513, 2078, 2035, 2013, 2020, 2026",
      /*  969 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 2095, 1513, 1321, 1580, 2013, 2020, 2026, 1330",
      /*  986 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1838, 2112, 2124, 2126, 2116, 2134, 2140, 1330, 1330",
      /* 1003 */ "1330, 1330, 1330, 1330, 1330, 1330, 1330, 1508, 1392, 1858, 1397, 1330, 1680, 1863, 1330, 1330, 1330",
      /* 1020 */ "1330, 1330, 1330, 1330, 1330, 1330, 1508, 1627, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /* 1037 */ "1330, 1330, 1330, 1330, 1330, 1508, 1513, 1699, 2148, 1330, 2166, 2172, 1330, 1330, 1330, 1330, 1330",
      /* 1054 */ "1330, 1330, 1330, 1330, 1508, 1513, 1330, 2035, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /* 1071 */ "1330, 2085, 1330, 2087, 1330, 1330, 1330, 1330, 2086, 1330, 1330, 1330, 1330, 1330, 1330, 1330, 1330",
      /* 1088 */ "5955, 5955, 5955, 256, 0, 0, 5955, 0, 0, 0, 0, 5955, 5955, 5955, 5955, 0, 0, 5955, 5955, 5955, 5955",
      /* 1109 */ "5955, 5955, 0, 5955, 0, 0, 0, 5955, 5955, 5955, 0, 0, 0, 0, 0, 5248, 0, 0, 0, 6528, 6528, 6528, 6528",
      /* 1132 */ "6528, 6528, 6528, 0, 0, 0, 0, 0, 0, 0, 7680, 6656, 0, 0, 6656, 0, 0, 0, 6656, 0, 6656, 6656, 0, 6656",
      /* 1156 */ "0, 0, 0, 0, 0, 0, 0, 7936, 0, 6656, 6656, 6656, 6656, 6656, 6656, 6656, 0, 0, 0, 0, 0, 0, 0, 8192, 0",
      /* 1181 */ "6784, 0, 6784, 6784, 0, 6784, 0, 0, 0, 6784, 0, 0, 0, 0, 0, 6784, 6784, 6784, 6784, 6784, 6784, 6784",
      /* 1203 */ "0, 6912, 6912, 6912, 6912, 6912, 6912, 6912, 0, 0, 0, 0, 0, 0, 5376, 0, 7040, 0, 7040, 7040, 7040",
      /* 1224 */ "7040, 7040, 7040, 0, 0, 0, 0, 0, 0, 6144, 6016, 0, 7168, 7168, 7168, 7168, 7168, 7168, 7168, 7237, 0",
      /* 1245 */ "0, 1024, 0, 0, 0, 256, 0, 4608, 0, 0, 0, 0, 0, 0, 7296, 0, 7296, 7296, 7296, 7296, 7296, 7296, 7424",
      /* 1268 */ "0, 0, 256, 0, 0, 0, 0, 7040, 0, 0, 0, 0, 7424, 0, 0, 0, 0, 7424, 6016, 7424, 7424, 7424, 7424, 7424",
      /* 1292 */ "7424, 7424, 7424, 0, 0, 0, 0, 0, 0, 7552, 0, 0, 0, 0, 7552, 0, 256, 0, 0, 0, 7552, 0, 7552, 7552",
      /* 1316 */ "7552, 7552, 7552, 7552, 0, 0, 0, 0, 0, 0, 6272, 0, 0, 6144, 0, 0, 0, 0, 0, 0, 0, 0, 5504, 8064, 8064",
      /* 1341 */ "8064, 8064, 8064, 8064, 8064, 8064, 0, 0, 0, 0, 0, 0, 7552, 7552, 0, 7552, 1280, 1280, 1280, 1280",
      /* 1361 */ "1280, 1280, 1280, 1280, 0, 0, 0, 0, 0, 0, 256, 0, 7808, 0, 7808, 7808, 7808, 7808, 7808, 7808, 1408",
      /* 1382 */ "1408, 1408, 1408, 1408, 1408, 1408, 1408, 0, 0, 0, 0, 0, 0, 256, 0, 8832, 0, 8832, 8832, 8832, 8832",
      /* 1403 */ "8832, 8832, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 1536, 0, 0, 0, 0, 0, 0, 256, 6528, 0, 0, 0",
      /* 1424 */ "256, 0, 0, 0, 7040, 0, 7040, 7040, 0, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 1664, 0, 0, 0, 0, 0",
      /* 1446 */ "0, 256, 6656, 0, 0, 0, 256, 0, 0, 7680, 0, 0, 0, 0, 0, 0, 7424, 0, 7424, 1792, 1792, 1792, 1792",
      /* 1469 */ "1792, 1792, 1792, 1792, 0, 0, 0, 0, 0, 0, 256, 6784, 0, 0, 0, 256, 0, 0, 7936, 0, 0, 0, 0, 0, 0",
      /* 1494 */ "8320, 0, 8320, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 1920, 0, 0, 0, 0, 0, 0, 512, 640, 0, 0, 0",
      /* 1516 */ "256, 0, 0, 0, 0, 4608, 4608, 4608, 0, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 2048, 0, 0, 0, 0, 0",
      /* 1538 */ "0, 512, 640, 0, 7680, 0, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 2176, 0, 0, 0, 0, 0, 0, 512, 640",
      /* 1560 */ "0, 7936, 0, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 2304, 0, 0, 0, 0, 0, 0, 512, 6784, 6784, 0, 0",
      /* 1582 */ "0, 0, 0, 0, 0, 8576, 2432, 2432, 2432, 2432, 2432, 2432, 2432, 2432, 0, 0, 0, 0, 0, 0, 4608, 0, 0",
      /* 1605 */ "4608, 0, 0, 4608, 4608, 0, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 2560, 0, 0, 0, 0, 0, 0, 4992, 0",
      /* 1627 */ "0, 0, 0, 5760, 0, 0, 0, 0, 6528, 640, 6528, 0, 0, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 2688, 0",
      /* 1649 */ "0, 0, 0, 0, 0, 7808, 7808, 7808, 7808, 7808, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 2816, 0, 0, 0",
      /* 1670 */ "0, 0, 0, 8192, 0, 0, 0, 8192, 256, 0, 0, 0, 0, 8832, 8832, 8832, 8832, 8832, 2944, 2944, 2944, 2944",
      /* 1692 */ "2944, 2944, 2944, 2944, 0, 0, 0, 0, 0, 0, 8960, 0, 0, 0, 8960, 3072, 3072, 3072, 3072, 3072, 3072",
      /* 1713 */ "3072, 3072, 0, 0, 0, 0, 0, 0, 836, 836, 836, 836, 836, 836, 3200, 3200, 3200, 3200, 3200, 3200, 3200",
      /* 1734 */ "3200, 0, 0, 0, 0, 0, 0, 1152, 1152, 1152, 1152, 1152, 1152, 3328, 3328, 3328, 3328, 3328, 3328, 3328",
      /* 1754 */ "3328, 0, 0, 0, 0, 0, 0, 4864, 0, 0, 0, 0, 0, 5955, 512, 640, 0, 0, 5955, 0, 5955, 0, 5955, 5955",
      /* 1778 */ "3456, 3456, 3456, 3456, 3456, 3456, 3456, 3456, 0, 0, 0, 0, 0, 0, 7237, 7237, 7237, 7237, 7237, 7237",
      /* 1798 */ "3584, 3584, 3584, 3584, 3584, 3584, 3584, 3584, 0, 0, 0, 0, 0, 0, 7808, 0, 7808, 7808, 7808, 0, 0, 0",
      /* 1820 */ "0, 0, 0, 8064, 0, 8064, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 3712, 0, 0, 0, 0, 0, 0, 8704, 512",
      /* 1842 */ "640, 0, 0, 8704, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 3840, 0, 0, 0, 0, 0, 0, 8832, 0, 8832",
      /* 1863 */ "8832, 8832, 0, 0, 0, 0, 0, 0, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 3968, 0, 0, 0, 0, 0, 0, 4096",
      /* 1886 */ "4096, 4096, 4096, 4096, 4096, 4096, 4096, 0, 0, 0, 0, 0, 0, 4224, 4224, 4224, 4224, 4224, 4224, 4224",
      /* 1906 */ "4224, 0, 0, 0, 0, 0, 0, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 4352, 0, 0, 0, 0, 0, 0, 4480, 4480",
      /* 1929 */ "4480, 4480, 4480, 4480, 4480, 4480, 0, 0, 0, 0, 0, 0, 0, 8192, 0, 0, 0, 0, 0, 0, 836, 0, 896, 0, 0",
      /* 1954 */ "0, 0, 5120, 0, 0, 0, 0, 8192, 8192, 8192, 8192, 8192, 8192, 8192, 0, 0, 0, 0, 0, 0, 0, 8320, 0, 0, 0",
      /* 1979 */ "0, 0, 0, 1152, 0, 0, 0, 0, 0, 0, 7296, 0, 0, 0, 0, 0, 0, 7424, 0, 0, 8320, 8320, 8320, 8320, 8320",
      /* 2004 */ "8320, 8320, 8320, 0, 0, 0, 0, 0, 0, 384, 0, 384, 0, 0, 384, 0, 384, 384, 384, 384, 384, 384, 384",
      /* 2027 */ "384, 0, 0, 0, 0, 0, 0, 4736, 0, 0, 0, 0, 0, 0, 0, 6016, 8448, 0, 8448, 0, 0, 8448, 0, 8448, 0, 0, 0",
      /* 2054 */ "0, 0, 0, 8448, 8448, 8448, 8448, 8448, 8448, 8448, 8448, 0, 0, 0, 0, 0, 0, 5632, 0, 0, 512, 640, 0",
      /* 2077 */ "0, 0, 5632, 0, 0, 0, 6272, 0, 0, 6400, 0, 0, 0, 0, 0, 0, 0, 6400, 0, 8576, 0, 512, 640, 0, 0, 0",
      /* 2103 */ "6528, 0, 0, 6528, 0, 0, 0, 0, 0, 8704, 8704, 8704, 8704, 0, 0, 8704, 0, 8704, 0, 8704, 8704, 8704",
      /* 2125 */ "8704, 8704, 0, 0, 8704, 8704, 8704, 8704, 8704, 8704, 0, 8704, 0, 0, 0, 8704, 8704, 0, 0, 0, 0, 0, 0",
      /* 2148 */ "0, 8960, 0, 0, 0, 0, 0, 0, 6528, 0, 6528, 6528, 0, 6528, 0, 0, 0, 6528, 0, 8960, 8960, 8960, 8960",
      /* 2171 */ "8960, 8960, 8960, 0, 0, 0, 0, 0, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 2180; ++i) {TRANSITION[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] EXPECTED = new int[184];
  static
  {
    final String s1[] =
    {
      /*   0 */ "176, 176, 167, 172, 176, 26, 34, 41, 48, 63, 71, 55, 79, 87, 95, 103, 111, 176, 172, 162, 119, 127",
      /*  22 */ "142, 134, 150, 158, 0, 0, 480, 480, 480, 480, 480, 480, -508, -512, -508, -512, -512, -508, -512",
      /*  41 */ "-508, -508, -508, -508, -508, -508, -508, -508, -28, 0, 64, 128, 32768, 131072, 262144, 1048576",
      /*  57 */ "1310720, 671088640, 139264, 33562624, 67117056, -2147475456, 1048576, 4194304, 33554432, 67108864",
      /*  67 */ "-2147483648, 16, 0, 32, 64, 128, 256, 512, 1024, 2048, 0, 8192, 4096, 1835008, 268435464, 671096832",
      /*  83 */ "339738624, 1843200, 1320960, 268443656, -2145648640, 356515840, 268509192, 301998088, -2145640448",
      /*  92 */ "356524032, -2112094208, 364904448, 364912640, 365174792, 365961224, 398499848, 1036017664, 7, 1835015",
      /* 102 */ "8199, 1835023, 1843207, 1107296263, 1843215, 1107304455, 1107435527, -1027866617, -1027858425",
      /* 111 */ "-755236857, -692322297, -688127993, -755228665, -692314105, -688119801, -253937, 8192, 0, 8, 8, 8, 8",
      /* 124 */ "8, 0, 16, 8, 16, 8, 8, 24, 32, 16, 24, 28, 2, 0, 10, 0, 8, 3, 24, 40, 24, 33, 16, 24, 24, 24, 8, 11",
      /* 152 */ "11, 35, 43, 51, 51, 51, 59, 59, 59, 59, 0, 0, 0, 4, 8, 0, 0, 0, 8, 16, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0",
      /* 182 */ "0, 0"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 184; ++i) {EXPECTED[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] CASEID = new int[1621];
  static
  {
    final String s1[] =
    {
      /*    0 */ "560, 563, 567, 568, 572, 703, 789, 577, 737, 581, 586, 589, 593, 614, 600, 605, 789, 608, 788, 612",
      /*   20 */ "618, 594, 582, 594, 623, 586, 589, 593, 614, 628, 703, 789, 577, 737, 581, 594, 594, 619, 794, 623",
      /*   40 */ "633, 636, 640, 594, 645, 703, 789, 577, 594, 650, 703, 789, 577, 594, 650, 703, 789, 577, 594, 650",
      /*   60 */ "586, 589, 593, 614, 628, 655, 658, 662, 715, 666, 594, 594, 624, 771, 671, 676, 679, 683, 594, 688",
      /*   80 */ "594, 594, 573, 596, 623, 909, 594, 684, 938, 693, 1009, 594, 629, 1034, 699, 594, 594, 795, 594, 623",
      /*  100 */ "703, 789, 577, 707, 612, 909, 594, 684, 938, 693, 594, 594, 695, 711, 623, 719, 722, 726, 731, 735",
      /*  120 */ "586, 589, 593, 741, 745, 703, 789, 577, 751, 612, 633, 636, 640, 594, 645, 594, 594, 594, 755, 759",
      /*  140 */ "703, 789, 577, 765, 612, 594, 594, 573, 769, 623, 719, 722, 726, 775, 735, 906, 594, 573, 912, 779",
      /*  160 */ "703, 789, 577, 785, 793, 594, 594, 573, 1012, 779, 703, 789, 577, 785, 793, 594, 594, 573, 1012, 779",
      /*  180 */ "703, 789, 577, 785, 793, 594, 594, 573, 1012, 779, 594, 594, 573, 799, 806, 906, 594, 573, 912, 779",
      /*  200 */ "900, 594, 646, 925, 812, 594, 594, 594, 641, 594, 594, 594, 689, 594, 594, 594, 594, 573, 595, 623",
      /*  220 */ "818, 821, 825, 830, 834, 594, 594, 594, 840, 594, 586, 589, 593, 846, 850, 586, 589, 593, 846, 850",
      /*  240 */ "586, 589, 593, 846, 850, 594, 594, 594, 856, 983, 808, 789, 608, 769, 623, 594, 594, 594, 651, 594",
      /*  260 */ "719, 722, 726, 860, 735, 999, 594, 667, 992, 864, 980, 594, 672, 973, 870, 876, 594, 594, 883, 594",
      /*  280 */ "919, 594, 594, 919, 594, 814, 893, 897, 916, 594, 594, 594, 594, 641, 594, 906, 594, 573, 912, 779",
      /*  300 */ "594, 594, 802, 601, 779, 594, 594, 573, 713, 623, 906, 594, 573, 912, 779, 594, 594, 795, 594, 623",
      /*  320 */ "594, 594, 594, 929, 594, 594, 594, 594, 903, 594, 594, 594, 594, 942, 594, 808, 789, 577, 769, 623",
      /*  340 */ "594, 594, 573, 769, 623, 594, 594, 573, 701, 623, 814, 893, 952, 916, 594, 594, 594, 594, 977, 594",
      /*  360 */ "594, 594, 802, 601, 779, 594, 594, 594, 594, 989, 594, 594, 594, 996, 594, 594, 594, 1028, 594, 594",
      /*  380 */ "594, 594, 935, 594, 594, 594, 594, 594, 985, 594, 594, 594, 761, 594, 594, 594, 594, 945, 594, 594",
      /*  400 */ "594, 594, 594, 781, 594, 594, 594, 594, 1002, 958, 594, 594, 836, 594, 594, 594, 594, 594, 879, 886",
      /*  420 */ "961, 594, 594, 889, 964, 594, 594, 866, 594, 594, 594, 594, 594, 1006, 594, 594, 594, 594, 594, 1016",
      /*  440 */ "1038, 594, 594, 1048, 594, 967, 594, 594, 970, 594, 594, 594, 727, 594, 594, 594, 594, 842, 594, 594",
      /*  460 */ "594, 594, 594, 1061, 594, 594, 594, 594, 1031, 594, 955, 594, 1065, 948, 1069, 594, 594, 594, 1022",
      /*  479 */ "594, 594, 594, 922, 594, 594, 1038, 594, 594, 1073, 594, 967, 594, 594, 1019, 594, 594, 594, 1077",
      /*  498 */ "852, 1081, 594, 594, 1085, 1089, 1093, 1097, 594, 594, 1101, 594, 1025, 594, 594, 1041, 594, 932",
      /*  516 */ "594, 1105, 1109, 1113, 932, 594, 1105, 1044, 1113, 594, 594, 1117, 826, 1051, 594, 594, 594, 872",
      /*  534 */ "594, 594, 594, 594, 1121, 594, 594, 594, 594, 1054, 594, 594, 594, 594, 1125, 594, 594, 594, 594",
      /*  553 */ "1057, 594, 594, 594, 594, 747, 594, 1141, 1181, 1461, 1129, 1129, 1129, 1129, 1462, 1181, 1181, 1142",
      /*  571 */ "1141, 1463, 1181, 1181, 1181, 1139, 1274, 1181, 1181, 1139, 1135, 1181, 1181, 1181, 1180, 1295, 1181",
      /*  588 */ "1146, 1151, 1151, 1151, 1151, 1147, 1181, 1181, 1181, 1181, 1182, 1184, 1165, 1181, 1181, 1181, 1185",
      /*  605 */ "1183, 1273, 1133, 1133, 1181, 1181, 1139, 1135, 1275, 1181, 1181, 1155, 1192, 1174, 1181, 1181, 1181",
      /*  622 */ "1196, 1186, 1181, 1181, 1181, 1221, 1190, 1181, 1181, 1181, 1241, 1490, 1181, 1202, 1207, 1207, 1207",
      /*  639 */ "1207, 1203, 1181, 1181, 1181, 1313, 1491, 1181, 1181, 1181, 1330, 1161, 1181, 1181, 1181, 1351, 1167",
      /*  656 */ "1181, 1211, 1217, 1217, 1217, 1217, 1212, 1181, 1181, 1196, 1213, 1181, 1181, 1181, 1362, 1262, 1181",
      /*  673 */ "1181, 1181, 1369, 1523, 1181, 1227, 1232, 1232, 1232, 1232, 1228, 1181, 1181, 1181, 1424, 1524, 1181",
      /*  690 */ "1181, 1181, 1432, 1423, 1426, 1181, 1181, 1170, 1266, 1534, 1261, 1181, 1181, 1183, 1181, 1273, 1133",
      /*  707 */ "1157, 1159, 1158, 1272, 1167, 1169, 1181, 1181, 1184, 1181, 1168, 1167, 1307, 1181, 1323, 1279, 1279",
      /*  724 */ "1279, 1279, 1324, 1181, 1181, 1181, 1472, 1319, 1321, 1320, 1322, 1325, 1325, 1181, 1181, 1184, 1183",
      /*  741 */ "1552, 1283, 1287, 1289, 1293, 1299, 1181, 1181, 1223, 1181, 1157, 1133, 1158, 1272, 1593, 1577, 1305",
      /*  758 */ "1445, 1312, 1317, 1181, 1181, 1253, 1181, 1157, 1133, 1184, 1272, 1157, 1185, 1181, 1181, 1262, 1221",
      /*  775 */ "1319, 1279, 1320, 1322, 1186, 1185, 1181, 1181, 1268, 1181, 1157, 1159, 1184, 1157, 1133, 1133, 1133",
      /*  792 */ "1133, 1135, 1184, 1181, 1181, 1181, 1266, 1157, 1185, 1181, 1182, 1181, 1181, 1139, 1186, 1184, 1181",
      /*  809 */ "1181, 1273, 1133, 1371, 1331, 1181, 1181, 1382, 1387, 1407, 1181, 1415, 1338, 1338, 1338, 1338, 1416",
      /*  826 */ "1181, 1181, 1181, 1591, 1414, 1338, 1408, 1336, 1417, 1417, 1181, 1181, 1449, 1181, 1593, 1577, 1181",
      /*  843 */ "1181, 1507, 1181, 1552, 1342, 1346, 1198, 1293, 1355, 1181, 1181, 1544, 1550, 1593, 1577, 1181, 1445",
      /*  860 */ "1319, 1279, 1308, 1322, 1508, 1363, 1181, 1181, 1599, 1181, 1501, 1370, 1181, 1181, 1603, 1181, 1393",
      /*  877 */ "1181, 1181, 1181, 1348, 1455, 1350, 1394, 1181, 1181, 1181, 1350, 1181, 1181, 1357, 1476, 1478, 1387",
      /*  894 */ "1387, 1387, 1387, 1388, 1181, 1181, 1181, 1373, 1331, 1181, 1252, 1181, 1181, 1273, 1185, 1181, 1237",
      /*  911 */ "1426, 1181, 1157, 1160, 1185, 1176, 1392, 1181, 1181, 1377, 1181, 1181, 1364, 1181, 1181, 1372, 1329",
      /*  928 */ "1331, 1250, 1181, 1181, 1181, 1401, 1587, 1181, 1412, 1181, 1181, 1423, 1236, 1425, 1575, 1577, 1181",
      /*  945 */ "1181, 1431, 1181, 1181, 1438, 1440, 1521, 1383, 1181, 1181, 1181, 1439, 1521, 1181, 1444, 1181, 1181",
      /*  962 */ "1467, 1471, 1181, 1478, 1181, 1181, 1495, 1181, 1181, 1500, 1181, 1181, 1502, 1368, 1370, 1617, 1398",
      /*  979 */ "1181, 1181, 1503, 1370, 1181, 1317, 1181, 1181, 1578, 1421, 1563, 1181, 1181, 1181, 1509, 1361, 1363",
      /*  996 */ "1405, 1181, 1181, 1181, 1510, 1363, 1181, 1301, 1301, 1436, 1605, 1483, 1181, 1181, 1517, 1261, 1181",
      /* 1013 */ "1183, 1160, 1185, 1488, 1181, 1181, 1181, 1532, 1181, 1181, 1540, 1181, 1181, 1567, 1181, 1181, 1569",
      /* 1030 */ "1181, 1181, 1514, 1181, 1181, 1516, 1257, 1260, 1332, 1181, 1181, 1181, 1573, 1181, 1181, 1582, 1585",
      /* 1047 */ "1587, 1611, 1181, 1181, 1181, 1597, 1181, 1181, 1609, 1181, 1181, 1615, 1181, 1181, 1451, 1181, 1181",
      /* 1064 */ "1181, 1437, 1181, 1181, 1528, 1437, 1521, 1181, 1181, 1244, 1181, 1181, 1181, 1484, 1181, 1181, 1538",
      /* 1081 */ "1427, 1550, 1181, 1181, 1496, 1181, 1181, 1556, 1561, 1181, 1181, 1557, 1496, 1557, 1181, 1181, 1378",
      /* 1098 */ "1181, 1181, 1181, 1247, 1181, 1181, 1181, 1399, 1181, 1181, 1586, 1451, 1400, 1585, 1587, 1399, 1587",
      /* 1115 */ "1181, 1181, 1479, 1181, 1181, 1181, 1546, 1181, 1181, 1181, 1458, 1181, 1181, 1181, 4, 4, 4, 4",
      /* 1133 */ "33046, 33046, 33046, 33046, 0, 33442, 0, 24882, 0, 0, 4, 0, 0, 0, 196612, 196612, 196612, 0, 196612",
      /* 1152 */ "196612, 196612, 196612, 0, 90130, 0, 0, 33046, 33046, 0, 33046, 0, 33442, 81938, 14, 0, 0, 49670, 0",
      /* 1171 */ "0, 0, 49670, 0, 33506, 0, 0, 65550, 346162, 66394, 0, 0, 0, 0, 33046, 0, 0, 0, 33442, 81938, 196612",
      /* 1192 */ "0, 0, 73746, 0, 0, 106818, 0, 0, 73746, 540676, 0, 458756, 458756, 458756, 0, 458756, 458756, 458756",
      /* 1210 */ "458756, 0, 49670, 49670, 49670, 0, 33442, 49670, 49670, 49670, 49670, 0, 114692, 0, 0, 98552, 0, 0",
      /* 1228 */ "491528, 491528, 491528, 0, 491528, 491528, 491528, 491528, 442372, 0, 442372, 442372, 442372, 0",
      /* 1242 */ "476470, 507912, 0, 0, 845522, 0, 0, 879106, 0, 0, 933892, 0, 0, 0, 647186, 476470, 0, 16398, 476470",
      /* 1261 */ "476470, 0, 0, 0, 114692, 0, 180770, 0, 0, 131192, 0, 33046, 0, 33046, 33046, 33046, 0, 0, 229380",
      /* 1280 */ "229380, 229380, 229380, 540676, 229394, 245764, 237586, 0, 32782, 245764, 0, 73746, 540676, 49166",
      /* 1294 */ "196612, 0, 0, 196612, 0, 245764, 540676, 0, 0, 213942, 0, 0, 573448, 0, 0, 229380, 0, 0, 589832, 0",
      /* 1314 */ "0, 0, 411794, 0, 352274, 0, 0, 229380, 229380, 0, 229380, 229380, 229380, 0, 0, 524292, 0, 524292, 0",
      /* 1333 */ "0, 0, 452274, 278532, 0, 278532, 278532, 278532, 278532, 540676, 229394, 0, 237586, 0, 32782, 0, 0",
      /* 1350 */ "246918, 0, 0, 0, 770052, 0, 540676, 0, 0, 262164, 286738, 1015812, 0, 1015812, 0, 0, 0, 516114",
      /* 1368 */ "999428, 0, 999428, 0, 0, 0, 524292, 524292, 524292, 638980, 0, 0, 0, 526834, 0, 1048584, 1048584",
      /* 1385 */ "1048584, 0, 1048584, 1048584, 1048584, 1048584, 805290, 81934, 0, 0, 0, 638980, 0, 838242, 0, 0, 0",
      /* 1402 */ "673622, 673622, 673622, 0, 16634, 0, 0, 278532, 0, 0, 0, 155666, 0, 0, 278532, 278532, 278532, 0, 0",
      /* 1421 */ "0, 163858, 0, 0, 442372, 442372, 0, 0, 0, 98318, 671762, 0, 0, 0, 688132, 205762, 0, 0, 0, 706694",
      /* 1441 */ "706694, 706694, 0, 205778, 0, 0, 0, 737284, 0, 696338, 0, 0, 452322, 0, 263314, 0, 246918, 0, 0",
      /* 1460 */ "952890, 0, 4, 4, 4, 0, 4, 0, 270354, 278546, 294930, 303122, 0, 0, 0, 772194, 262164, 0, 262164, 0",
      /* 1480 */ "0, 0, 805290, 730994, 0, 0, 0, 851988, 0, 745490, 0, 0, 458756, 0, 0, 460690, 0, 0, 0, 983060",
      /* 1500 */ "763810, 0, 0, 0, 999428, 999428, 999428, 786450, 0, 0, 0, 1015812, 1015812, 1015812, 460738, 0, 0, 0",
      /* 1518 */ "476470, 476470, 476470, 706694, 0, 0, 0, 491528, 0, 0, 0, 706694, 0, 772194, 853938, 0, 0, 0, 507912",
      /* 1537 */ "476470, 0, 98318, 0, 0, 507922, 0, 0, 950292, 0, 0, 526866, 0, 851988, 0, 0, 0, 540676, 540676, 0",
      /* 1557 */ "983060, 0, 0, 0, 966680, 0, 0, 0, 614418, 0, 535170, 0, 0, 0, 622610, 0, 887442, 0, 0, 0, 655364, 0",
      /* 1579 */ "0, 0, 163858, 460738, 0, 673622, 673622, 0, 673622, 0, 0, 0, 551202, 0, 0, 0, 655364, 346162, 551218",
      /* 1598 */ "0, 0, 0, 712722, 0, 0, 557074, 0, 0, 722578, 0, 535202, 0, 0, 0, 755394, 0, 953034, 0, 0, 0, 829906",
      /* 1620 */ "346162"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1621; ++i) {CASEID[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TOKENSET = new int[112];
  static
  {
    final String s1[] =
    {
      /*   0 */ "55, 55, 53, 65, 27, 53, 55, 23, 48, 50, 50, 50, 53, 56, 30, 48, 30, 47, 47, 18, 63, 47, 33, 60, 61",
      /*  25 */ "64, 48, 41, 62, 32, 61, 43, 58, 40, 58, 40, 58, 40, 39, 43, 43, 7, 0, 26, 59, 28, 57, 57, 57, 35, 54",
      /*  51 */ "7, 59, 43, 43, 19, 20, 51, 7, 43, 34, 24, 43, 18, 2, 3, 21, 52, 32, 25, 49, 28, 34, 17, 1, 11, 10, 22",
      /*  78 */ "16, 8, 6, 31, 14, 36, 42, 15, 21, 9, 19, 20, 0, 13, 2, 3, 46, 4, 12, 19, 20, 38, 37, 19, 20, 44, 45",
      /* 105 */ "29, 5, 2, 3, 2, 3, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 112; ++i) {TOKENSET[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] APPENDIX = new int[14];
  static
  {
    final String s1[] =
    {
      /*  0 */ "98306, 299017, 238235, 253956, 45065, 270338, 40969, 270338, 327682, 414953, 327682, 419121, 425994",
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

  private static final int[] GOTO = new int[812];
  static
  {
    final String s1[] =
    {
      /*   0 */ "256, 257, 357, 500, 257, 257, 257, 498, 257, 257, 359, 257, 257, 257, 262, 257, 257, 300, 257, 257",
      /*  20 */ "257, 267, 257, 257, 284, 257, 257, 257, 257, 272, 419, 475, 257, 257, 257, 277, 257, 257, 335, 257",
      /*  40 */ "257, 257, 282, 257, 257, 365, 257, 257, 257, 257, 257, 288, 257, 457, 257, 257, 294, 257, 257, 257",
      /*  60 */ "257, 257, 299, 257, 304, 257, 463, 257, 257, 257, 309, 257, 257, 257, 257, 257, 314, 319, 257, 257",
      /*  80 */ "257, 324, 310, 257, 329, 417, 471, 341, 257, 257, 257, 257, 333, 419, 257, 278, 339, 345, 363, 257",
      /* 100 */ "257, 371, 257, 257, 257, 377, 257, 369, 383, 257, 257, 257, 375, 257, 257, 389, 257, 257, 257, 381",
      /* 120 */ "257, 257, 395, 257, 257, 257, 387, 257, 257, 401, 257, 257, 257, 393, 257, 257, 407, 257, 257, 257",
      /* 140 */ "257, 257, 305, 257, 257, 399, 257, 257, 257, 263, 451, 257, 257, 257, 257, 257, 429, 257, 258, 405",
      /* 160 */ "449, 411, 417, 471, 315, 415, 469, 473, 423, 417, 471, 257, 268, 339, 345, 427, 257, 257, 257, 257",
      /* 180 */ "257, 435, 441, 257, 369, 257, 257, 257, 290, 257, 257, 257, 433, 257, 257, 439, 257, 257, 320, 257",
      /* 200 */ "273, 445, 449, 257, 325, 257, 481, 257, 257, 257, 455, 257, 257, 487, 257, 257, 257, 461, 257, 257",
      /* 220 */ "348, 257, 257, 257, 257, 257, 257, 467, 257, 257, 506, 257, 479, 295, 351, 257, 257, 257, 485, 257",
      /* 240 */ "257, 354, 257, 257, 257, 257, 491, 447, 257, 257, 495, 257, 504, 257, 257, 257, 510, 511, 511, 511",
      /* 260 */ "511, 518, 587, 511, 511, 511, 572, 530, 511, 511, 511, 582, 538, 511, 511, 511, 597, 554, 511, 511",
      /* 280 */ "511, 601, 511, 630, 511, 511, 534, 511, 511, 700, 511, 511, 540, 511, 562, 511, 511, 511, 609, 804",
      /* 300 */ "511, 511, 511, 611, 671, 511, 511, 511, 670, 566, 511, 511, 511, 685, 579, 511, 511, 511, 690, 676",
      /* 320 */ "511, 511, 511, 715, 570, 511, 511, 511, 743, 511, 690, 734, 576, 595, 511, 511, 511, 558, 511, 605",
      /* 340 */ "511, 511, 511, 591, 511, 511, 609, 615, 511, 511, 777, 511, 511, 781, 511, 511, 789, 511, 516, 511",
      /* 360 */ "511, 512, 511, 620, 511, 511, 511, 616, 511, 511, 685, 511, 511, 624, 511, 638, 511, 511, 511, 628",
      /* 380 */ "511, 646, 511, 511, 511, 634, 511, 654, 511, 511, 511, 642, 511, 662, 511, 511, 511, 650, 511, 511",
      /* 400 */ "675, 511, 511, 658, 511, 689, 511, 511, 511, 666, 511, 511, 690, 767, 576, 798, 739, 586, 586, 511",
      /* 420 */ "511, 544, 511, 511, 690, 798, 744, 694, 511, 511, 511, 684, 511, 511, 710, 511, 511, 698, 511, 511",
      /* 440 */ "774, 511, 511, 704, 511, 722, 727, 731, 738, 511, 609, 511, 511, 680, 511, 752, 511, 511, 511, 706",
      /* 460 */ "511, 760, 511, 511, 511, 718, 511, 764, 511, 511, 511, 723, 511, 711, 511, 511, 511, 550, 511, 511",
      /* 480 */ "546, 511, 511, 748, 511, 785, 511, 511, 511, 756, 511, 511, 597, 722, 793, 797, 511, 802, 511, 526",
      /* 500 */ "511, 511, 522, 511, 808, 511, 511, 511, 771, 511, 6, 0, 0, 0, 0, 12497, 0, 4113, 0, 0, 0, 217097, 0",
      /* 523 */ "8225, 0, 12497, 20529, 24585, 28737, 32777, 0, 40972, 0, 12497, 0, 303217, 49233, 53465, 0, 57353, 0",
      /* 541 */ "0, 0, 241673, 0, 62065, 0, 0, 0, 282633, 0, 90124, 0, 12497, 0, 81932, 0, 12497, 0, 73740, 0, 12497",
      /* 562 */ "0, 315401, 0, 12497, 0, 94217, 0, 12497, 327689, 332225, 0, 0, 0, 369697, 131076, 131076, 131076, 0",
      /* 580 */ "0, 74417, 0, 0, 160513, 311300, 155652, 0, 0, 0, 16785, 0, 123377, 0, 12497, 0, 126985, 0, 0, 0",
      /* 600 */ "434180, 0, 156385, 160513, 311300, 303108, 164617, 360452, 360452, 0, 353073, 0, 0, 28737, 32777",
      /* 615 */ "167945, 0, 0, 0, 53465, 0, 180233, 0, 12497, 0, 184329, 0, 12497, 0, 332289, 0, 0, 28769, 32777, 0",
      /* 635 */ "213012, 0, 12497, 0, 189009, 0, 12497, 0, 172052, 0, 12497, 0, 192521, 0, 12497, 0, 188436, 0, 12497",
      /* 654 */ "0, 196617, 0, 12497, 0, 196628, 0, 12497, 0, 201457, 0, 12497, 0, 279325, 0, 12497, 160529, 0, 0, 0",
      /* 674 */ "86689, 376844, 0, 0, 0, 90417, 0, 208905, 0, 12497, 213001, 0, 0, 0, 98841, 221193, 0, 0, 0, 106505",
      /* 694 */ "0, 234585, 0, 12497, 0, 237577, 0, 0, 65545, 0, 389129, 332225, 0, 0, 70265, 0, 241673, 0, 0, 0",
      /* 714 */ "111305, 0, 0, 402417, 0, 0, 319497, 90417, 405513, 0, 0, 0, 155652, 0, 246889, 417796, 249865, 0",
      /* 732 */ "450564, 409604, 0, 0, 340529, 344073, 409609, 0, 0, 0, 163868, 393244, 0, 0, 0, 180252, 0, 516132, 0",
      /* 751 */ "12497, 0, 431225, 0, 12497, 0, 434185, 0, 12497, 0, 147500, 0, 12497, 0, 0, 447761, 0, 0, 340577",
      /* 770 */ "344073, 0, 0, 451929, 0, 0, 398209, 0, 0, 270345, 90417, 0, 287905, 0, 12497, 0, 290825, 0, 12497, 0",
      /* 790 */ "454665, 0, 12497, 0, 296105, 417796, 249865, 475140, 0, 0, 0, 204804, 442404, 458756, 0, 0, 74377, 0",
      /* 808 */ "0, 431289, 0, 12497"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 812; ++i) {GOTO[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] REDUCTION = new int[130];
  static
  {
    final String s1[] =
    {
      /*   0 */ "37, 0, 0, -1, 1, -1, 2, -1, 3, -1, 4, -1, 5, -1, 38, 1, 6, 2, 7, 5, 7, 4, 7, 3, 7, 6, 8, -1, 39, 7, 9",
      /*  31 */ "-1, 10, -1, 40, 8, 11, 9, 11, -1, 12, 11, 12, 10, 13, 13, 13, 12, 14, 14, 15, -1, 16, 15, 42, 17, 41",
      /*  57 */ "16, 17, -1, 18, -1, 19, -1, 44, 19, 43, 18, 20, -1, 21, 20, 21, -1, 22, 22, 22, 21, 46, 24, 45, 23",
      /*  82 */ "23, -1, 47, 25, 24, 26, 25, -1, 48, 27, 26, -1, 49, 28, 27, -1, 28, -1, 29, 32, 29, 31, 29, 30, 29",
      /* 107 */ "29, 30, -1, 31, 33, 32, 34, 50, 35, 33, -1, 34, 36, 34, -1, 52, 38, 51, 37, 35, -1, 36, 39"
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
    "IMPLICIT-51",
    "IMPLICIT-52"
  };

                                                            // line 922 "ixml.ebnf"
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
                                                              private Rule rule;
                                                              private java.util.Stack<Alts> alts = new java.util.Stack<>();
                                                              private StringBuilder stringBuilder = new StringBuilder();
                                                              private StringBuilder nameBuilder = new StringBuilder();

                                                              public Grammar grammar() {
                                                                new PostProcess(grammar).visit(grammar);
                                                                return grammar;
                                                              }
                                                            }
                                                            // line 1728 "Ixml.java"
// End
