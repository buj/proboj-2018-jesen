package server.game.map;

import java.util.*;


/** The game map: each cell has its terrain type and elevation. Does not
 * include information about starting positions of players and their
 * units. */
public class Terrain {
  public enum Type {
    PLAINS, FOREST, WATER, SENTINEL, FINISH_LINE
  }
  public static int SENTINEL_HEIGHT = 99;
  
  public final int r, c;
  protected Type[][] terrain;
  protected int[][] elevation;
  
  /** Constructs an empty terrain with the given dimensions (plains
   * everywhere, elevation 0). */
  public Terrain (int r0, int c0) {
    r = r0;
    c = c0;
    terrain = new Type[r][c];
    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        terrain[i][j] = Type.PLAINS;
      }
    }
    elevation = new int[r][c];
  }
  
  /** Constructs the Terrain by reading from the provided Scanner. */
  public Terrain (Scanner sc) {
    r = sc.nextInt();
    c = sc.nextInt();
    
    // load terrains
    terrain = new Type[r][c];
    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        int t = sc.nextInt();
        terrain[i][j] = Type.values()[t];
      }
    }
    // load elevations
    elevation = new int[r][c];
    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        elevation[i][j] = sc.nextInt();
      }
    }
  }
  public Terrain (String str) {
    this(new Scanner(str));
  }
  
  public boolean outOfBounds (int ra, int ca) {
    return (ra < 0 || ra >= r || ca < 0 || ca >= c);
  }
  public boolean outOfBounds (Position pos) {
    return outOfBounds(pos.r, pos.c);
  }
  
  /** Is used to check scoring condition. */
  public boolean inFinishLine (Position pos) {
    return pos.r == r;
  }
  public boolean inFinishLine (int pos_r, int pos_c) {
    return inFinishLine(new Position(pos_r, pos_c));
  }
  
  /** Returns the Terrain.Type at location [pos_r, pos_c]. If that is out
   * of bounds of the map, returns Type.SENTINEL */
  public Type terrainAt (int pos_r, int pos_c) {
    if (outOfBounds(pos_r, pos_c)) {
      if (inFinishLine(pos_r, pos_c)) {
        return Type.FINISH_LINE;
      }
      return Type.SENTINEL;
    }
    return terrain[pos_r][pos_c];
  }
  public Type terrainAt (Position pos) {
    return terrainAt(pos.r, pos.c);
  }
  
  /** Returns the height at location [pos_r, pos_c]. If that is out of
   * the map, returns SENTINEL_HEIGHT. */
  public int heightAt (int pos_r, int pos_c) {
    if (outOfBounds(pos_r, pos_c)) {
      return SENTINEL_HEIGHT;
    }
    return elevation[pos_r][pos_c];
  }
  public int heightAt (Position pos) {
    return heightAt(pos.r, pos.c);
  }
  
  /** Returns a string representation of this terrain, in the same
   * format that is used to construct the Terrain. */
  @Override
  public String toString () {
    StringBuilder bui = new StringBuilder();
    bui.append(String.format("%d %d\n\n", r, c));
    
    // terrain
    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        if (j != 0) {
          bui.append(" ");
        }
        bui.append(terrain[i][j].ordinal());
      }
      bui.append("\n");
    }
    // elevations
    bui.append("\n");
    for (int i = 0; i < r; i++) {
      for (int j = 0; j < c; j++) {
        if (j != 0) {
          bui.append(" ");
        }
        bui.append(elevation[i][j]);
      }
      bui.append("\n");
    }
    return bui.toString();
  }
}
