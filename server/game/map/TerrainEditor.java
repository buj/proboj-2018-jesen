package server.game.map;

import server.game.Constants;


/** Used to edit the wrapped terrain. Used by map editor. */
public class TerrainEditor {
  protected Terrain t;
  protected Visibility v;
  
  /** Loads the given terrain in the editor. */
  public TerrainEditor (Terrain t0) {
    t = t0;
    v = new LinearVisibility(t, Constants.sight);
  }
  /** Creates a new terrain with the given dimensions, with elevation 0
   * and plains everywhere. */
  public TerrainEditor (int r, int c) {
    this(new Terrain(r, c));
  }
  
  /** Sets the terrain at position <pos> to type <color>. */
  public void paint (Position pos, Terrain.Type color) {
    if (t.outOfBounds(pos)) {
      return;
    }
    t.terrain[pos.r][pos.c] = color;
    v.recalculate(pos);
  }
  
  /** Sets the height at position <pos> to <h>. */
  public void setHeight (Position pos, int h) {
    if (t.outOfBounds(pos)) {
      return;
    }
    t.elevation[pos.r][pos.c] = h;
    v.recalculate(pos);
  }
  
  /** Returns the underlying terrain. */
  public Terrain getTerrain () {
    return t;
  }
  
  /** Returns the underlying visibility graph. */
  public Visibility getVisibility () {
    return v;
  }
  
  /** Resizes the map to dimensions <r> rows, <c> columns. Not
   * necessarily a fast operation, but one that should not be called
   * too often. */
  public void resize (int r, int c) {
    Terrain newt = new Terrain(r, c);
    int lim_r = Math.min(r, t.r);
    int lim_c = Math.min(c, t.c);
    for (int i = 0; i < lim_r; i++) {
      for (int j = 0; j < lim_c; j++) {
        newt.terrain[i][j] = t.terrain[i][j];
        newt.elevation[i][j] = t.elevation[i][j];
      }
    }
    t = newt;
    v = new LinearVisibility(t, Constants.sight);
  }
}
