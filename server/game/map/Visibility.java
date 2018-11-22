package server.game.map;

import java.util.*;


/** A visibility graph. For each location, we remember the list of
 * locations that can be seen from the former. And also the other way
 * around: for each location, we remember the list of locations that
 * can see the former. */
public interface Visibility {
  /** Returns the list of all positions visible from <pos>. If <pos> is
   * out of bounds of the map, returns null. */
  Set<Position> visibleFrom (Position pos) ;
  
  default Set<Position> visibleFrom (int pos_r, int pos_c) {
    return visibleFrom(new Position(pos_r, pos_c));
  }
  
  /** Returns the list of all positions that can see <pos>. If <pos> is
   * out of bounds of the map, returns null. */
  Set<Position> canSee (Position pos) ;
  
  default Set<Position> canSee (int pos_r, int pos_c) {
    return canSee(new Position(pos_r, pos_c));
  }
  
  /** Recalculates visibility at the given position. */
  void recalculate (Position pos) ;
  
  default void recalculate (int pos_r, int pos_c) {
    recalculate(new Position(pos_r, pos_c));
  }
  
  /** Returns a list of all positions that are in the sight radius
   * of the given position. */
  static List<Position> getSight (Position pos, int range) {
    List<Position> res = new ArrayList<Position>();
    for (int i = -range; i <= range; i++) {
      int lim = range - Math.abs(i);
      for (int j = -lim; j <= lim; j++) {
        res.add(new Position(pos.r + i, pos.c + j));
      }
    }
    return res;
  }
}


/** Contains all the more common stuff between 'SimpleVisibility' and
 * 'LinearVisibility'. */
abstract class AbstractVisibility implements Visibility {
  protected Map<Position, Set<Position> > from, to;
  protected Terrain map;
  protected int range;
  
  /** Constructs a visibility graph for the given Terrain and range. */
  public AbstractVisibility (Terrain map0, int range0) {
    from = new HashMap<>();
    to = new HashMap<>();
    map = map0;
    range = range0;
    
    // firstly, create lists for all positions
    for (int i = 0; i < map.r; i++) {
      for (int j = 0; j < map.c; j++) {
        Position pos = new Position(i, j);
        from.put(pos, new HashSet<Position>());
        to.put(pos, new HashSet<Position>());
      }
    }
    // for each position, calculate its visibility
    for (int i = 0; i < map.r; i++) {
      for (int j = 0; j < map.c; j++) {
        recalculate(i, j);
      }
    }
  }
  
  /** Returns true if A can see B, false otherwise. For internal use only. */
  abstract protected boolean _canSee (Position A, Position B) ;
  
  /** A convenience method for automatically updating 'from' and 'to'. */
  private void calcVis (Position A, Position B) {
    if (_canSee(A, B)) {
      from.get(A).add(B);
      to.get(B).add(A);
    }
  }
  
  @Override
  public void recalculate (Position A) {
    // first, clear previous info
    from.get(A).clear();
    to.get(A).clear();
    for (Position B : Visibility.getSight(A, range)) {
      if (map.outOfBounds(B)) {
        continue;
      }
      from.get(B).remove(A);
      to.get(B).remove(A);
    }
    // now, calculate new info
    for (Position B : Visibility.getSight(A, range)) {
      calcVis(A, B);
      calcVis(B, A);
    }
  }
  
  @Override
  public Set<Position> visibleFrom (Position pos) {
    return from.get(pos);
  }
  
  @Override
  public Set<Position> canSee (Position pos) {
    return to.get(pos);
  }
  
  @Override
  public String toString () {
    StringBuilder bui = new StringBuilder();
    for (int i = 0; i < map.r; i++) {
      for (int j = 0; j < map.c; j++) {
        Set<Position> set = visibleFrom(i, j);
        bui.append(set.size());
        for (Position pos : set) {
          bui.append(" ");
          bui.append(pos.toString());
        }
        bui.append("\n");
      }
    }
    return bui.toString();
  }
}



/** Simple (but unrealistic) visibility graph. We see everything in range that
 * has lower or equal elevation except for forests; forests are
 * unobscured only when we are adjacent to them and they are not too high.
 * We see adjacent cells that are 1 elevation higher than we are. */
class SimpleVisibility extends AbstractVisibility {
  public SimpleVisibility (Terrain map0, int range0) {
    super(map0, range0);
  }
  
  @Override
  protected boolean _canSee (Position A, Position B) {
    int hA = map.heightAt(A);
    int hB = map.heightAt(B);
    Terrain.Type t = map.terrainAt(B);
    int dist = A.distTo(B);
    
    if (map.outOfBounds(A) || map.outOfBounds(B)) {
      return false;
    }
    if (t == Terrain.Type.FOREST && dist > 1) { // can't see distant forests
      return false;
    }
    if (hB > hA) {
      if (dist > 1 || hB > hA + 1) { // can't see too much higher
        return false;
      }
    }
    return true;
  }
}
