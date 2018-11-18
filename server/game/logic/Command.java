package server.game.logic;

import server.game.map.Position;
import java.util.*;


/** Represents a command to a unit. */
public class Command {
  public enum Type {
    MOVE, ATTACK
  }
  public final Type type;
  public final Position pos, tgt;
  
  /** A command of type <type0>, addressed to unit at position <pos0>,
   * targeting the unit/cell at position <tgt0>. */
  public Command (Type type0, Position pos0, Position tgt0) {
    type = type0;
    pos = pos0;
    tgt = tgt0;
  }
  
  /** Loads the command from the provided scanner, and returns it. */
  public static Command loadFrom (Scanner sc) {
    Position pos = Position.loadFrom(sc);
    Type type = Type.values()[sc.nextInt()];
    Position tgt = Position.loadFrom(sc);
    return new Command(type, pos, tgt);
  }
  public static Command loadFrom (String str) {
    return loadFrom(new Scanner(str));
  }
}
