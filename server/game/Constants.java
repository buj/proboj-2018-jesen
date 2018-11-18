package server.game;


/** All the game's constants. */
public class Constants {
  public static final int attacker = 1;
  public static final int defender = 0;
  public static final int observer = -1;
  
  public static final int baseDamage = 50;
  public static final int healthRegen = 1;
  public static final int boostedHealthRegen = 10;
  
  public static final int normalTerrainCost = 10;
  public static final int roughTerrainCost = 50;
  public static final int staminaRegen = 50;
  public static final int boostedStaminaRegen = 100;
  
  public static final int maxTurns = 1000;
  public static final int turnMillis = 1000;
}