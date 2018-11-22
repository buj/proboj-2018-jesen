package mapedit;

import java.util.*;
import server.game.Constants;
import server.game.map.*;
import server.game.units.*;


/** Something that modifies a single cell of the map. */
public interface Operation {
  void apply (Position pos, Editor ed) ;
}


/** Each clicked position is changed to a specific terrain. */
class TerrainPainter implements Operation {
  protected Terrain.Type target;
  
  /** Creates an operation that converts clicked cells to the
   * specified terrain <target0>. */
  public TerrainPainter (Terrain.Type target0) {
    target = target0;
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    ed.setTerrain(pos, target);
  }
}


/** Each clicked position's elevation is set to a particular value. */
class HeightSetter implements Operation {
  protected double target;
  
  public HeightSetter (double h) {
    target = Math.max(0, Math.min(Constants.maxHeight, h));
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    ed.setHeight(pos, (int)target);
  }
  
  /** Changes the target by <d>. */
  public void change (double d) {
    target += d;
    target = Math.max(0, Math.min(Constants.maxHeight, target));
  }
}


/** Each clicked position's elevation is changed by +x (where x can be negative). */
class HeightAdjuster implements Operation {
  protected double mod;
  
  public HeightAdjuster (double mod0) {
    mod = Math.max(-Constants.maxHeight, Math.min(Constants.maxHeight, mod0));
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    int orig_h = ed.heightAt(pos);
    int new_h = Math.max(0, Math.min(Constants.maxHeight, orig_h + (int)mod));
    ed.setHeight(pos, new_h);
  }
  
  /** Changes the change by <d>. */
  public void change (double d) {
    mod += d;
    mod = Math.max(-Constants.maxHeight, Math.min(Constants.maxHeight, mod));
  }
}


/** Puts a unit of specified owner and type at each clicked position. */
class UnitPutter implements Operation {
  protected int owner;
  protected Unit.Type type;
  
  public UnitPutter (int owner0, Unit.Type type0) {
    owner = owner0;
    type = type0;
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    ed.setUnit(pos, owner, type);
  }
}


/** Removes units from clicked positions. */
class UnitRemover implements Operation {
  @Override
  public void apply (Position pos, Editor ed) {
    ed.removeUnit(pos);
  }
}


/** An operation that copies an entire rectangle to 'clipboard'. To
 * be used in conjunction with operation 'Paste'. For maximum effect,
 * it should be wrapped in a 'Rectangular'. */
class Copy implements Operation {
  protected Map<Position, Terrain.Type> terrains;
  protected Map<Position, Integer> heights;
  protected Map<Position, InitialUnit> units;
  protected Position anchor;
  
  /** A copy operation. If <copyTerra>, we copy the terrain information.
   * Similar goes for <copyHeights> and heights, and <copyUnits> and
   * units. */
  public Copy (boolean copyTerra, boolean copyHeights, boolean copyUnits) {
    terrains = (copyTerra ? new HashMap<>() : null);
    heights = (copyHeights ? new HashMap<>() : null);
    units = (copyUnits ? new HashMap<>() : null);
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    if (terrains != null) {
      Terrain.Type type = ed.terrainAt(pos);
      terrains.put(pos, type);
    }
    if (heights != null) {
      int h = ed.heightAt(pos);
      heights.put(pos, h);
    }
    if (units != null) {
      InitialUnit unit = ed.unitAt(pos);
      if (unit != null) {
        units.put(pos, unit);
      }
    }
    if (anchor == null) {
      anchor = pos;
    }
  }
  
  /** Clears the clipboard. */
  public void clear () {
    if (terrains != null) {
      terrains.clear();
    }
    if (heights != null) {
      heights.clear();
    }
    if (units != null) {
      units.clear();
    }
    anchor = null;
  }
}


/** An operation that works in conjunction with 'Copy' to replicate
 * parts of the map. */
class Paste implements Operation {
  protected Copy sub;
  
  public Paste (Copy sub0) {
    sub = sub0;
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    if (sub.terrains != null) {
      for (Map.Entry<Position, Terrain.Type> entry : sub.terrains.entrySet()) {
        Position tgt = pos.plus(entry.getKey()).minus(sub.anchor);
        ed.setTerrain(tgt, entry.getValue());
      }
    }
    if (sub.heights != null) {
      for (Map.Entry<Position, Integer> entry : sub.heights.entrySet()) {
        Position tgt = pos.plus(entry.getKey()).minus(sub.anchor);
        ed.setHeight(tgt, entry.getValue());
      }
    }
    if (sub.units != null) {
      for (Map.Entry<Position, InitialUnit> entry : sub.units.entrySet()) {
        Position tgt = pos.plus(entry.getKey()).minus(sub.anchor);
        InitialUnit unit = entry.getValue();
        ed.setUnit(pos, unit.owner, unit.type);
      }
    }
  }
}
