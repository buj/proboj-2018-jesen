package server.game.map;

import java.util.*;


/** Represents position on the map. */
public class Position {
  public final int r, c;
  
  public Position (int r0, int c0) {
    r = r0;
    c = c0;
  }
  
  /** Loads a position from the given scanner and returns it. */
  public static Position loadFrom (Scanner sc) {
    int r = sc.nextInt();
    int c = sc.nextInt();
    return new Position(r, c);
  }
  
  // The four cardinal directions: up (0), right (1), down (2), left (3)
  public static int dr[] = new int[]{-1, 0, 1, 0};
  public static int dc[] = new int[]{0, 1, 0, -1};
  
  /** Returns the position that is adjacent to this one in
   * direction <i>. */
  public Position adj (int i) {
    return new Position(r + dr[i], c + dc[i]);
  }
  
  /** Returns the manhattan distance from this position to <tgt>. */
  public int distTo (Position tgt) {
    return Math.abs(tgt.r - r) + Math.abs(tgt.c - c);
  }
  
  @Override
  public boolean equals (Object other) {
    if (other instanceof Position) {
      Position pos = (Position)other;
      return r == pos.r && c == pos.c;
    }
    return false;
  }
  
  @Override
  public String toString () {
    return String.format("%d %d", r, c);
  }
  
  @Override
  public int hashCode () {
    return toString().hashCode();
  }
}
