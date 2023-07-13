package de.bottlecaps.markup.blitz.parser;

public class Action {
  public static enum Type {
    ERROR,
    SHIFT,
    SHIFT_REDUCE,
    REDUCE,
    FORK,
    ACCEPT;

    public static final int BITS = 3;

    @Override
    public String toString() {
      return name().toLowerCase().replace('_', '-');
    }
  }

  /** The action type. */
  private final Type type;
  /** The action code, interpreted depending on the action:<ul>
   * <li> - for SHIFT: the target state</li>
   * <li> - for REDUCE and SHIFT_REDUCE: the reduction id</li>
   * <li> - for FORK: the fork id</li></li>
   * <li> - for ERROR and ACCEPT: 0 (not needed)</ul> */
  private final int argument;

  public Action(Type type, int argument) {
    super();
    this.type = type;
    this.argument = argument;
  }

  public Type getType() {
    return type;
  }

  public int getArgument() {
    return argument;
  }

  public int code() {
    return code(type, argument);
  }

  public static int code(Type type, int argument) {
    return (argument << Type.BITS) + type.ordinal();
  }

  public static Action of(int code) {
    return new Action(Type.values()[code & ((1 << Type.BITS) - 1)], code >> Type.BITS);
  }

  @Override
  public String toString() {
    return type.toString() + " " + (type == Type.ACCEPT ? "" : argument);
  }
}
