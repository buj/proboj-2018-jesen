package server.game;


/** Represents position on the map. */
public class Position {
  public final int r, c;
  
  public Position (int r0, int c0) {
    r = r0;
    c = c0;
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
  public int hashCode () {
    return String.format("%d %d", r, c).hashCode();
  }
}
