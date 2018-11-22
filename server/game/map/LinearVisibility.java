package server.game.map;

import java.util.*;


/** A linear visibility graph. Very few simplifications are made: we
 * calculate line of sight just like one would expect it to work in
 * the real world. This, however, leads to quite hard to understand
 * code and mechanics. */
public class LinearVisibility extends AbstractVisibility {
  public LinearVisibility (Terrain map0, int range0) {
    super(map0, range0);
  }
  
  @Override
  protected boolean _canSee (Position A, Position B) {
    if (map.outOfBounds(A) || map.outOfBounds(B)) {
      return false;
    }
    if (!lineOfSight(between(A, B), map)) {
      return false;
    }
    return true;
  }
  
  /** Compares values num1/denom1 and num2/denom2. Returns 1 if the
   * former is greater, 0 if equal, and -1 if the latter is greater. */
  static int compare (int num1, int denom1, int num2, int denom2) {
    int modifier = (denom1 < 0 ? -1 : 1) * (denom2 < 0 ? -1 : 1);
    int left = num1 * denom2;
    int right = num2 * denom1;
    return modifier * Integer.signum(left - right);
  }
  
  /** Returns a list of positions that are 'intersected' by a line from
   * <first> to <second>. Is used for determining line of sight. */
  static List<Position> between (Position first, Position second) {
    List<Position> res = new ArrayList<>();
    
    // original difference between positions
    int orig_diff_r = first.r - second.r;
    int orig_diff_c = first.c - second.c;
    
    // current difference
    int diff_r = orig_diff_r;
    int diff_c = orig_diff_c;
    
    // in which direction to we move?
    int dr = -Integer.signum(diff_r);
    int dc = -Integer.signum(diff_c);
    
    res.add(new Position(first.r, first.c));
    while (diff_r != 0 || diff_c != 0) {
      int cmp = compare(2*diff_r + dr, 2*orig_diff_r, 2*diff_c + dc, 2*orig_diff_c);
      if (cmp >= 0) {
        diff_r += dr;
      }
      if (cmp <= 0) {
        diff_c += dc;
      }
      res.add(new Position(second.r + diff_r, second.c + diff_c));
    }
    return res;
  }
  
  /** Returns true if it is possible to see from start of <path> to
   * the end, based on information in the Terrain <map>. */
  static boolean lineOfSight (List<Position> path, Terrain map) {
    int n = path.size();
    if (n > 2) { // non-adjacent forests are obscured
      if (map.terrainAt(path.get(n - 1)) == Terrain.Type.FOREST) {
        return false;
      }
    }
    int h_start = map.heightAt(path.get(0));
    int h_target = map.heightAt(path.get(n-1));
    for (int i = 1; i < n - 1; i++) {
      int hi = map.heightAt(path.get(i));
      if (map.terrainAt(path.get(i)) == Terrain.Type.FOREST) { // forests artificially increase height by 1
        hi++;
      }
      double max_h = h_start + (h_target - h_start) * (double)i / (n - 1);
      if (hi > max_h) {
        return false;
      }
    }
    return true;
  }
}
