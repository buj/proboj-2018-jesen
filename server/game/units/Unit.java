package server.game.units;


/** A unit controlled by a player. Can be either a warrior (who is tough
 * but can only attack on melee range) or an archer (who has ranged
 * attack but is flimsy). */
public class Unit {
  public enum Type {
    WARRIOR(100, 100, 8, 8),
    ARCHER(100, 100, 7, 0);
    
    public final int maxHealth, maxStamina, baseAttack, baseDefense;
    
    /** Creates a unit type with the given stats. */
    Type (int h0, int s0, int a0, int d0) {
      maxHealth = h0;
      maxStamina = s0;
      baseAttack = a0;
      baseDefense = d0;
    }
  }
  
  public final int id, owner;
  public final Type type;
  protected int health, stamina;
  
  protected static int free_id = 0;
  
  /** Creates a new unit of the given type, with full health and stamina. */
  public Unit (int owner0, Type type0) {
    owner = owner0;
    type = type0;
    health = type.maxHealth;
    stamina = type.maxStamina;
    id = free_id;
    free_id += 1;
  }
  
  /** Causes the unit to change its health by +amount, which can be
   * negative (causing the unit to lose health). Will keep the unit's
   * health within bounds [0, maxHealth]. */
  public void changeHealth (int amount) {
    health += amount;
    health = Math.min(type.maxHealth, Math.max(0, health));
  }
  public int getHealth () {
    return health;
  }
  public double getHealthPercentage () {
    return (double)health / type.maxHealth;
  }
  public boolean isDead () {
    return health == 0;
  }
  
  /** Causes the unit to change its stamina by +amount. Will keep the
   * stamina within bounds [0, maxStamina]. */
  public void changeStamina (int amount) {
    stamina += amount;
    stamina = Math.min(type.maxStamina, Math.max(0, stamina));
  }
  public int getStamina () {
    return stamina;
  }
  public double getStaminaPercentage () {
    return (double)stamina / type.maxStamina;
  }
  /** Exhaustion is equal to the amount of stamina we are missing. */
  public int getExhaustion () {
    return type.maxStamina - stamina;
  }
  
  /** Returns the attack strength of this unit, taking into account
   * current health and stamina. */
  public double getAttack () {
    double hp = getHealthPercentage();
    double sp = getStaminaPercentage();
    return (double)type.baseAttack * hp * (1.0 + sp) / 2.0;
  }
  
  /** Returns the defensive strength of this unit, taking into account
   * current health and stamina. */
  public double getDefense () {
    double hp = getHealthPercentage();
    double sp = getStaminaPercentage();
    return (double)type.baseDefense * hp * (1.0 + sp) / 2.0;
  }
  
  @Override
  public String toString () {
    return String.format("%d %d %d %d %d", id, owner, type.ordinal(), health, stamina);
  }
}
