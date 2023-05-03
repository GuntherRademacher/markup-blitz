package de.bottlecaps.markupblitz;

class ClassMember extends Member {
  private final String value;

  public ClassMember(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public void accept(Visitor v) {
  }
}