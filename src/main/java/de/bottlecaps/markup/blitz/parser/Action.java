package de.bottlecaps.markup.blitz.parser;

public class Action {
  public static enum Type {
    SHIFT,
    SHIFT_REDUCE,
    REDUCE,
    FORK;

    public int code(int arg) {
      return arg << 2 + ordinal();
    }

    @Override
    public String toString() {
      return name().toLowerCase().replace('_', '-');
    }
  }

  /** The action type. */
  private final Type type;
  /** The action code, interpreted depending on the action:<ul>
   * <li> - for SHIFT: the target state</li>
   * <li> - for SHIFT_REDUCE and REDUCE: the reduction id</li>
   * <li> - for FORK: the fork id</li></ul> */
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
    return type.code(argument);
  }

  public static int code(Type type, int argument) {
    return type.code(argument);
  }

  @Override
  public String toString() {
    return type.toString() + " " + argument;
  }
}
