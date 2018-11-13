package server.game;

import java.util.*;


/** The game map: each cell has its terrain type and elevation. Does not
 * include information about starting positions of players and their
 * units. */
public class Terrain {
  public enum Type {
    PLAINS, FOREST, WATER, SENTINEL
  }
  public final int r, c;
  protected Type[][] terrain;
  protected int[][] elevation;
  
  /** Constructs the Terrain from a string description. */
  public Terrain (String str) {
    Scanner sc = new Scanner(str);
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
  
  public boolean outOfBounds (int ra, int ca) {
    return (ra < 0 || ra >= r || ca < 0 || ca >= c);
  }
  
  /** Returns the Terrain.Type at location [pos_r, pos_c]. If that is out
   * of bounds of the map, returns Type.SENTINEL */
  public Type terrainAt (int pos_r, int pos_c) {
    if (outOfBounds(pos_r, pos_c)) {
      return Type.SENTINEL;
    }
    return terrain[pos_r][pos_c];
  }
  public Type terrainAt (Position pos) {
    return terrainAt(pos.r, pos.c);
  }
  
  /** Returns the height at location [pos_r, pos_c]. If that is out of
   * the map, returns 0. */
  public int heightAt (int pos_r, int pos_c) {
    if (outOfBounds(pos_r, pos_c)) {
      return 0;
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
