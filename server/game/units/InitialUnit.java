package server.game.units;

import java.util.*;
import server.game.map.*;
import server.game.Constants;


/** Specifies a unit's starting position, owner/faction, and type. */
public class InitialUnit {
  public final int owner;
  public final Unit.Type type;
  public final Position pos;
  
  /** Constructs an InitialUnit of the given player, with the given
   * type and at the given position. */
  public InitialUnit (int owner0, Unit.Type type0, Position pos0) {
    owner = owner0;
    type = type0;
    pos = pos0;
  }
  
  @Override
  public String toString () {
    return String.format("%d %d %s", owner, type.ordinal(), pos.toString());
  }
  
  /** Loads and returns an InitialUnit from the provided Scanner. */
  public static InitialUnit loadFrom (Scanner sc) {
    int o = sc.nextInt();
    Unit.Type t = Unit.Type.values()[sc.nextInt()];
    Position pos = Position.loadFrom(sc);
    return new InitialUnit(o, t, pos);
  }
  
  /** Returns a list of InitialUnits that determines the starting position
   * of the game. The list is loaded from the given scanner. */
  public static List<InitialUnit> getStartingPositions (Scanner sc) {
    List<InitialUnit> res = new ArrayList<InitialUnit>();
    int n = sc.nextInt();
    for (int i = 0; i < n; i++) {
      res.add(InitialUnit.loadFrom(sc));
    }
    return res;
  }
  
  /** Returns a default list of InitialUnits: the bottom row are defender
   * units, and the topmost row are attacker units, except for water
   * cells. */
  public static List<InitialUnit> dummyStartingPositions (Terrain terra) {
    List<InitialUnit> res = new ArrayList<InitialUnit>();
    for (int i = 0; i < terra.c; i++) {
      for (int j = 0; j < 2; j++) {
        int row = (1 - j) * (terra.r - 1);
        if (terra.terrainAt(row, i) != Terrain.Type.WATER) {
          Unit.Type x = (i%2 == 1 ? Unit.Type.WARRIOR : Unit.Type.ARCHER);
          res.add(new InitialUnit(j, x, new Position(row, i)));
        }
      }
    }
    return res;
  }
}
