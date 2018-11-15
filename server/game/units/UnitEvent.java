package server.game.units;


/** Represents a change in a unit's state: either health has changed,
 * or stamina has changed. */
public class UnitEvent {
  public enum Type {
    HEALTH, STAMINA
  }
  protected Type type;
  protected int mod;
  
  /** Constructs a unit event of the given type that changes
   * health/stamina by <mod0>. */
  public UnitEvent (Type type0, int mod0) {
    type = type0;
    mod = mod0;
  }
  
  /** Convenience method: returns a health change event. */
  public static UnitEvent health (int mod) {
    return new UnitEvent(Type.HEALTH, mod);
  }
  /** Convenience method: returns a stamina change event. */
  public static UnitEvent stamina (int mod) {
    return new UnitEvent(Type.STAMINA, mod);
  }
  
  /** Applies the unit event to the given unit. */
  public void apply (Unit unit) {
    if (type == Type.HEALTH) {
      unit.changeHealth(mod);
    }
    else
    if (type == Type.STAMINA) {
      unit.changeStamina(mod);
    }
  }
}
