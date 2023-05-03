package de.bottlecaps.markupblitz;

public class Insertion extends Literal {

  public Insertion(String value, boolean isHex) {
    super(false, value, isHex);
  }

  @Override
  public String toString() {
    return "+" + super.toString();
  }
}