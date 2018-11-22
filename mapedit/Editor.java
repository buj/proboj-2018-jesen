package mapedit;

import java.util.*;
import java.util.logging.*;
import server.game.map.*;
import server.game.units.*;


/** Editor for everything: terrain types, elevation, and initial
 * unit placement. Knows a particular painter that draws the map;
 * whenever a change occurs, the editor tells the painter to redraw
 * a part of tehe canvas. */
public class Editor {
  static Logger logger = Logger.getLogger("main");
  
  protected TerrainEditor terra;
  protected UnitEditor units;
  protected Painter painter;
  
  /** Editor that loads up a new map of size <r> times <c>. */
  public Editor (int r, int c) {
    terra = new TerrainEditor(new Terrain(r, c));
    units = new UnitEditor();
  }
  
  /** Loads the terrain and unit information from the provided scanner.
   * If an error occurs during loading, no change occurs. */
  public void loadFrom (Scanner sc) {
    TerrainEditor old_terra = terra;
    UnitEditor old_units = units;
    try {
      terra = new TerrainEditor(new Terrain(sc));
      units = new UnitEditor(sc);
      painter.redrawAll();
    }
    catch (NoSuchElementException exc) {
      logger.info("Error while loading from file, aborting");
      terra = old_terra;
      units = old_units;
    }
  }
  
  /** Sets the painter. */
  public void setPainter (Painter painter0) {
    painter = painter0;
    painter.redrawAll();
  }
  
  /** Convenience methods for retrieving information about the map. */
  public Terrain.Type terrainAt (Position pos) {
    return terra.getTerrain().terrainAt(pos);
  }
  public int heightAt (Position pos) {
    return terra.getTerrain().heightAt(pos);
  }
  public InitialUnit unitAt (Position pos) {
    return units.get(pos);
  }
  public int numRows () {
    return terra.getTerrain().r;
  }
  public int numCols () {
    return terra.getTerrain().c;
  }
  public boolean outOfBounds (Position pos) {
    return terra.getTerrain().outOfBounds(pos);
  }
  
  /** Methods for changing the map. Afterwards, the painter is told
   * to redraw a part of the screen. */
  public void setTerrain (Position pos, Terrain.Type type) {
    if (outOfBounds(pos)) {
      return;
    }
    terra.paint(pos, type);
    painter.redraw(pos);
  }
  public void setHeight (Position pos, int h) {
    if (outOfBounds(pos)) {
      return;
    }
    terra.setHeight(pos, h);
    painter.redraw(pos);
  }
  public void setUnit (Position pos, int owner, Unit.Type type) {
    if (outOfBounds(pos)) {
      return;
    }
    units.paint(pos, owner, type);
    painter.redraw(pos);
  }
  public void removeUnit (Position pos) {
    if (outOfBounds(pos)) {
      return;
    }
    units.remove(pos);
    painter.redraw(pos);
  }
  
  /** Resizes the map to dimensions <r> times <c>. */
  public void resize (int r, int c) {
    terra.resize(r, c);
    units.resize(r, c);
    painter.redrawAll();
  }
  
  @Override
  public String toString () {
    StringBuilder bui = new StringBuilder();
    bui.append(terra.getTerrain().toString());
    bui.append("\n");
    bui.append(units.toString());
    return bui.toString();
  }
}


/** Editor for initial unit placement. */
class UnitEditor {
  protected Map<Position, InitialUnit> map;
  
  public UnitEditor () {
    map = new HashMap<Position, InitialUnit>();
  }
  /** Loads the list of units from the provided scanner. */
  public UnitEditor (Scanner sc) {
    this();
    int n = sc.nextInt();
    for (int i = 0; i < n; i++) {
      InitialUnit unit = InitialUnit.loadFrom(sc);
      map.put(unit.pos, unit);
    }
  }
  
  /** Puts a unit of player <owner> and of type <type> on the position <pos>.
   * If any unit was there before, it is discarded. */
  public void paint (Position pos, int owner, Unit.Type type) {
    map.put(pos, new InitialUnit(owner, type, pos));
  }
  
  /** Removes the unit at position <pos>, if there is one. */
  public void remove (Position pos) {
    map.remove(pos);
  }
  
  /** Returns the InitialUnit that is at position pos. If there is no
   * unit, return null. */
  public InitialUnit get (Position pos) {
    return map.get(pos);
  }
  
  /** When the map is resized, some of the unis may end up out of bounds.
   * Remove them. */
  public void resize (int r, int c) {
    Iterator<Position> it = map.keySet().iterator();
    while (it.hasNext()) {
      Position pos = it.next();
      if (pos.r >= r || pos.c >= c) {
        it.remove();
      }
    }
  }
  
  @Override
  public String toString () {
    StringBuilder bui = new StringBuilder();
    bui.append(map.size());
    bui.append("\n");
    for (Map.Entry<Position, InitialUnit> entry : map.entrySet()) {
      bui.append(entry.getValue().toString());
      bui.append("\n");
    }
    return bui.toString();
  }
}
