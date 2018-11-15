package server.game.logic;

import server.game.map.Position;
import server.game.units.Unit;


/** An auxiliary structure: a record about a unit's state and its position.
 * Note that when the game state is updated, only the 'unit' part is updated.
 * The position is left the same. Thus, any change in the game state
 * invalidates this. */
class PosUnit {
  Position pos;
  Unit unit;
  
  PosUnit (Position pos0, Unit unit0) {
    pos = pos0;
    unit = unit0;
  }
  
  @Override
  public String toString () {
    return String.format("%s %s", pos.toString(), unit.toString());
  }
}
