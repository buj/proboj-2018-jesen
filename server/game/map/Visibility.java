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
}


/** Simple (but unrealistic) visibility graph. We see everything in range that
 * has lower or equal elevation except for forests; forests are
 * unobscured only when we are adjacent to them and they are not too high.
 * We see adjacent cells that are 1 elevation higher than we are. */
class SimpleVisibility implements Visibility {
  protected Map<Position, Set<Position> > from, to;
  
  /** Constructs a visibility graph for the given Terrain and range. */
  public SimpleVisibility (Terrain map, int range) {
    from = new HashMap<>();
    to = new HashMap<>();
    
    // firstly, create lists for all positions
    for (int i = 0; i < map.r; i++) {
      for (int j = 0; j < map.c; j++) {
        Position pos = new Position(i, j);
        from.put(pos, new HashSet<Position>());
        to.put(pos, new HashSet<Position>());
      }
    }
    
    // for each position
    for (int i = 0; i < map.r; i++) {
      for (int j = 0; j < map.c; j++) {
        Position A = new Position(i, j);
        int hA = map.heightAt(A);
        
        // try all positions in range
        for (int di = -range; di <= range; di++) {
          int lim = range - Math.abs(di);
          for (int dj = -lim; dj <= lim; dj++) {
            Position B = new Position(i + di, j + dj);
            int hB = map.heightAt(B);
            int dist = Math.abs(di) + Math.abs(dj);
            
            // test: can we see? if so, extend the lists
            if (map.terrainAt(B) == Terrain.Type.FOREST) {
              if (dist > 1) {
                continue;
              }
            }
            if (hB > hA) {
              if (dist > 1 || hB > hA + 1) {
                continue;
              }
            }
            from.get(A).add(B);
            to.get(B).add(A);
          }
        }
      }
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
}
