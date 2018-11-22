package mapedit;

import java.util.*;
import server.game.map.*;


/** A container for an operation. */
public class OperationBox implements Operation {
  Operation sub;
  
  public void set (Operation newSub) {
    sub = newSub;
  }
  
  public Operation get () {
    return sub;
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    if (sub != null) {
      sub.apply(pos, ed);
    }
  }
}


/** Applies the given sub-operation to an entire rectangle. */
class Rectangular implements Operation {
  protected OperationBox box;
  protected int step;
  protected Position[] corners;
  
  public Rectangular (OperationBox box0) {
    box = box0;
    step = 0;
    corners = new Position[2];
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    corners[step] = pos;
    step += 1;
    if (step == 2) {
      step = 0;
      int r1 = Math.min(corners[0].r, corners[1].r);
      int r2 = Math.max(corners[0].r, corners[1].r);
      int c1 = Math.min(corners[0].c, corners[1].c);
      int c2 = Math.max(corners[0].c, corners[1].c);
      for (int i = r1; i <= r2; i++) {
        for (int j = c1; j <= c2; j++) {
          box.get().apply(new Position(i, j), ed);
        }
      }
    }
  }
}


/** Applies the given sub-operation, but tracks on which positions it
 * was already applied. It ignores those positions. */
class OneTimer implements Operation {
  protected OperationBox box;
  protected Set<Position> visited;
  
  public OneTimer (OperationBox box0) {
    box = box0;
    visited = new HashSet<Position>();
  }
  
  @Override
  public void apply (Position pos, Editor ed) {
    if (visited.contains(pos)) {
      return;
    }
    visited.add(pos);
    box.get().apply(pos, ed);
  }
}
