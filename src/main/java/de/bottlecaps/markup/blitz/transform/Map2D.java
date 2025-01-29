// Copyright (c) 2023-2025 Gunther Rademacher. Provided under the Apache 2 License.

package de.bottlecaps.markup.blitz.transform;

import java.util.TreeMap;

public class Map2D extends TreeMap<Map2D.Index, Integer> {
  private static final long serialVersionUID = 1L;

  private int endX;
  private int endY;

  public Map2D(int endX, int endY) {
    this.endX = endX;
    this.endY = endY;
  }

  @Override
  public Integer put(Index key, Integer value) {
    if (key.x > getEndX() || key.y > getEndY())
      throw new IllegalArgumentException();
    return super.put(key, value);
  }

  public int getEndX() {
    return endX;
  }

  public int getEndY() {
    return endY;
  }

  public static class Index implements Comparable<Index> {
    private int x;
    private int y;

    public Index(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public int compareTo(Index o) {
      return getX() != o.getX()
           ? getX() - o.getX()
           : getY() - o.getY();
    }

    @Override
    public String toString() {
      return "[" + getX() + ", " + getY() + "]";
    }

    public int getY() {
      return y;
    }

    public int getX() {
      return x;
    }
  }
}
