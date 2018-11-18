package server.game.map;


/** Used to edit the wrapped terrain. Used by map editor. */
public class TerrainEditor {
  protected Terrain t;
  
  /** Loads the given terrain in the editor. */
  public TerrainEditor (Terrain t0) {
    t = t0;
  }
  /** Creates a new terrain with the given dimensions, with elevation 0
   * and plains everywhere. */
  public TerrainEditor (int r, int c) {
    t = new Terrain(r, c);
  }
  
  /** Sets the terrain at position <pos> to type <color>. */
  public void paint (Position pos, Terrain.Type color) {
    if (t.outOfBounds(pos)) {
      return;
    }
    t.terrain[pos.r][pos.c] = color;
  }
  
  /** Sets the height at position <pos> to <h>. */
  public void setHeight (Position pos, int h) {
    if (t.outOfBounds(pos)) {
      return;
    }
    t.elevation[pos.r][pos.c] = h;
  }
  
  /** Returns the underlying terrain. */
  public Terrain getTerrain () {
    return t;
  }
}
