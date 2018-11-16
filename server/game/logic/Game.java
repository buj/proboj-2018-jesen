package server.game.logic;

import java.util.*;
import server.game.map.*;
import server.game.units.*;
import server.game.Constants;


/** Simulates game logic: moves, attacks, deaths, ... taking into account
 * information about visibility and terrain. */
public class Game {
  protected Random rng;
  
  protected Terrain terrain;
  protected Visibility visibility;
  
  protected Map<Position, Unit> unitMap;
  protected int score; // number of attackers that have successfully passed through
  protected int turn; // turn number
  protected Stepper stepper; // contains almost all of game logic
  protected boolean gameOver;
  
  /** Constructs a Game from the given terrain and list of initialUnits. */
  public Game (long seed, Terrain terrain0, List<InitialUnit> initial) {
    rng = new Random(seed);
    terrain = terrain0;
    visibility = new LinearVisibility(terrain, 3);
    unitMap = new HashMap<Position, Unit>();
    score = 0;
    turn = 1;
    stepper = new Stepper();
    gameOver = false;
    
    // populate the unit list from <initial>
    for (InitialUnit data : initial) {
      Unit unit = new Unit(data.owner, data.type);
      unitMap.put(data.pos, unit);
    }
  }
  
  /** How much damage should be dealt? We have strength <a> and opponent
   * has strength <b>. */
  static int blow (double a, double b) {
    double dmg = (double)Constants.baseDamage * a / (a+b);
    return (int)dmg;
  }
  
  /** An auxiliary structure. Waits for player commands and then
   * executes them when requested. Should be used only once---for
   * each time step, create a new Stepper and use that one. */
  class Stepper {
    // track units that have already received a command
    Set<Position> exhausted;
    
    // structures for attacks and shots
    Map<Position, List<Position> > atkMap;
    Map<Position, List<UnitEvent> > combatResults;
    
    // structures for movement
    Map<Position, List<Position> > moveMap;
    Map<Position, Position> moveChains;
    
    Stepper () {
      exhausted = new HashSet<Position>();
      atkMap = new HashMap<Position, List<Position> >();
      combatResults = new HashMap<Position, List<UnitEvent> >();
      moveMap = new HashMap<Position, List<Position> >();
      moveChains = new HashMap<Position, Position>();
    }
    
    
    /** Creates a UnitEvent that says the following: "Unit at position
     * <pos> will have its health changed by <mod>." Does not check
     * bounds. */
    void healthChange (Position pos, int mod) {
      combatResults.putIfAbsent(pos, new ArrayList<UnitEvent>());
      combatResults.get(pos).add(UnitEvent.health(mod));
    }
    /** Creates a UnitEvent that says the following: "Unit at position
     * <pos> will have its stamina changed by <mod>." Does not check
     * bounds. */
    void staminaChange (Position pos, int mod) {
      combatResults.putIfAbsent(pos, new ArrayList<UnitEvent>());
      combatResults.get(pos).add(UnitEvent.stamina(mod));
    }
    
    /** Returns the stamina cost of moving from position <pos> to position <tgt>. */
    int staminaCost (Position pos, Position tgt) {
      int h0 = terrain.heightAt(pos);
      int h1 = terrain.heightAt(tgt);
      Terrain.Type tt = terrain.terrainAt(tgt);
      if (h0 < h1 || tt == Terrain.Type.FOREST) {
        return Constants.roughTerrainCost;
      }
      return Constants.normalTerrainCost;
    }
    
    /** Check if the movement is valid: if the two positions are adjacent
     * to one another and if terrain permits such a move.*/
    boolean canMove (Position pos, Position tgt) {
      // are the two positions adjacent?
      if (pos.distTo(tgt) != 1) {
        return false;
      }
      // is the terrain not water?
      Terrain.Type tt = terrain.terrainAt(tgt);
      if (tt == Terrain.Type.WATER) {
        return false;
      }
      // is it the finish line? if so, all is well
      if (tt == Terrain.Type.FINISH_LINE) {
        return true;
      }
      // is the height difference not too large?
      int h0 = terrain.heightAt(pos);
      int h1 = terrain.heightAt(tgt);
      if (Math.abs(h0 - h1) > 1) {
        return false;
      }
      // finish
      return true;
    }
    
    
    /** Unit at position <pos> wants to move to position <tgt>. Checks
     * if the movement is valid, and only then it is added to the list
     * of pending commands. */
    void moveCommand (Position pos, Position tgt) {
      // terrain/position related check
      if (!canMove(pos, tgt)) {
        return;
      }
      // enough stamina?
      Unit unit = unitMap.get(pos);
      if (unit.getStamina() < staminaCost(pos, tgt)) {
        return;
      }
      // finish
      moveMap.putIfAbsent(tgt, new ArrayList<Position>());
      moveMap.get(tgt).add(pos);
    }
    
    /** Unit at position <pos> wants to attack position <tgt>. 
     * Check if the attack is valid: if the enemy is in range, if
     * the unit is not trying to selfdestruct or harm its ally,
     * and if it has enough stamina. */
    void attackCommand (Position pos, Position tgt) {
      // are both cells occupied?
      if (!unitMap.containsKey(tgt)) {
        return;
      }
      // checks based on unit type
      Unit attacker = unitMap.get(pos);
      if (attacker.type == Unit.Type.WARRIOR) {
        if (!canMove(pos, tgt) || attacker.getStamina() < staminaCost(pos, tgt)) {
          return;
        }
      }
      if (attacker.type == Unit.Type.ARCHER) { // range 2, and must see the target
        if (!visibility.visibleFrom(pos).contains(tgt)) {
          return;
        }
        int dist = pos.distTo(tgt);
        if (dist > 2) {
          return;
        }
      }
      // is it really an enemy?
      Unit defender = unitMap.get(tgt);
      if (attacker.owner == defender.owner) {
        return;
      }
      // finish
      atkMap.putIfAbsent(tgt, new ArrayList<Position>());
      atkMap.get(tgt).add(pos);
      if (attacker.type == Unit.Type.WARRIOR) {
        // loses stamina
        int cost = staminaCost(pos, tgt);
        staminaChange(pos, -cost);
        // tries to move there
        moveMap.putIfAbsent(tgt, new ArrayList<Position>());
        moveMap.get(tgt).add(pos);
      }
    }
    
    /** Player <player> has given the command <cmd>. We check the
     * command for correctness, and only then do we execute it. */
    void command (int player, Command cmd) {
      // does the source cell contain this player's unit?
      Unit unit = unitMap.get(cmd.pos);
      if (unit == null || unit.owner != player) {
        return;
      }
      // did it already receive a command?
      if (exhausted.contains(cmd.pos)) {
        return;
      }
      exhausted.add(cmd.pos);
      // finish
      if (cmd.type == Command.Type.ATTACK) {
        attackCommand(cmd.pos, cmd.tgt);
      }
      else
      if (cmd.type == Command.Type.MOVE) {
        moveCommand(cmd.pos, cmd.tgt);
      }
    }
    
    
    /** Executes all queued attacks. */
    void executeAttacks () {
      for (Position tgt : atkMap.keySet()) { // for each cell that is attacked
        List<Position> attackers = atkMap.get(tgt);
        Unit defender = unitMap.get(tgt);
        double baseDef = defender.getDefense() / attackers.size();
        
        // have defender fight with each attacker
        for (Position pos : attackers) {
          Unit attacker = unitMap.get(pos);
          double atk = attacker.getAttack();
          double def = baseDef;
          // apply combat modifiers from terrain
          if (terrain.heightAt(tgt) > terrain.heightAt(pos)) {
            def *= 1.5;
          }
          if (terrain.terrainAt(tgt) == Terrain.Type.FOREST) {
            def *= 1.5;
          }
          if (attacker.type == Unit.Type.WARRIOR) {
            // fight! close combat!
            int atkDmgDealt = blow(atk, def);
            int defDmgDealt = blow(def, atk);
            healthChange(tgt, -atkDmgDealt);
            healthChange(pos, -defDmgDealt);
          }
          else
          if (attacker.type == Unit.Type.ARCHER) {
            // ranged volley of arrows
            int atkDmgDealt = blow(atk, def);
            healthChange(tgt, -atkDmgDealt);
          }
        }
      }
    }
    
    /** Applies all unit events. */
    void applyEvents () {
      for (Position pos : combatResults.keySet()) { // for each recipient...
        List<UnitEvent> events = combatResults.get(pos);
        Unit unit = unitMap.get(pos);
        for (UnitEvent ev : events) { // for each of his events... apply it
          ev.apply(unit);
        }
      }
    }
    
    /** Units that survived and were not given any orders will
     * regenerate health and stamina. */
    void regenerate () {
      for (Position pos : unitMap.keySet()) {
        // if unit is exhausted or dead, ignore it
        if (exhausted.contains(pos)) {
          continue;
        }
        Unit unit = unitMap.get(pos);
        if (unit.isDead()) {
          continue;
        }
        // find out if it is next to water
        boolean nextToWater = false;
        for (int dir = 0; dir < 4; dir++) {
          Position adj = pos.adj(dir);
          if (terrain.terrainAt(adj) == Terrain.Type.WATER) {
            nextToWater = true;
            break;
          }
        }
        // regenerate health and stamina
        int hpRegen, spRegen;
        if (nextToWater) {
          hpRegen = Constants.boostedHealthRegen;
          spRegen = Constants.boostedStaminaRegen;
        }
        else {
          hpRegen = Constants.healthRegen;
          spRegen = Constants.staminaRegen;
        }
        unit.changeHealth(hpRegen);
        unit.changeStamina(spRegen);
      }
    }
    
    /** Constructs 'moveChains' from 'moveMap': where multiple units
     * wanted to move, we choose randomly one of them that receives
     * priority. (But we ignore dead units.) */
    void solveCollisions () {
      for (Position tgt : moveMap.keySet()) {
        List<Position> movers = moveMap.get(tgt);
        
        // clear dead movers
        List<Position> nonDeadMovers = new ArrayList<Position>();
        for (Position pos : movers) {
          Unit unit = unitMap.get(pos);
          if (!unit.isDead()) {
            nonDeadMovers.add(pos);
          }
        }
        // randomly choose the winner
        int n = nonDeadMovers.size();
        if (n == 0) {
          continue;
        }
        int who = rng.nextInt(n);
        Position pos = nonDeadMovers.get(who);
        moveChains.put(pos, tgt);
      }
    }
    
    /** Finally moves all units in 'unitMap' to their destination. 
     * This includes clearing out any zombie units. */
    void moveIt () {
      // clear zombies
      Iterator<Position> it = unitMap.keySet().iterator();
      while (it.hasNext()) {
        Position pos = it.next();
        Unit unit = unitMap.get(pos);
        if (unit.isDead()) {
          it.remove();
        }
      }
      // move along the chains
      ArrayList<Position> temp = new ArrayList<Position>(moveChains.keySet());
      for (Position pos : temp) {
        if (!moveChains.containsKey(pos)) {
          continue;
        }
        // find the chain
        List<Position> chain = new ArrayList<Position>();
        Position curr = pos;
        while (moveChains.containsKey(curr)) {
          chain.add(curr);
          curr = moveChains.get(curr);
          if (curr.equals(pos)) { // cycle
            break;
          }
        }
        if (!unitMap.containsKey(curr) && !curr.equals(pos)) { // will not move a cycle
          chain.add(curr);
          // move units along the chain
          int n = chain.size();
          for (int i = n - 2; i >= 0; i--) {
            Position from = chain.get(i);
            Position to = chain.get(i+1);
            Unit who = unitMap.get(from);
            unitMap.remove(from);
            unitMap.put(to, who);
          }
        }
        // clear this part of moveChains
        for (Position pos2 : chain) {
          moveChains.remove(pos2);
        }
      }
    }
    
    /** Clears attacking units that have reached the last row, and
     * increases the attacker's score. */
    void finishLine () {
      Iterator<Position> it = unitMap.keySet().iterator();
      while (it.hasNext()) {
        Position pos = it.next();
        Unit unit = unitMap.get(pos);
        if (unit.owner != Constants.attacker) {
          continue;
        }
        if (terrain.terrainAt(pos) == Terrain.Type.FINISH_LINE) {
          it.remove();
          score += 1;
        }
      }
    }
    
    /** Checks if one side of the battle has been completely wiped out.
     * If so, the rest can be simulated (assuming infinite time). */
    void checkEnd () {
      int[] counts = new int[]{0, 0};
      for (Position pos : unitMap.keySet()) {
        Unit unit = unitMap.get(pos);
        counts[unit.owner] += 1;
      }
      if (counts[0] == 0) {
        score += counts[1];
        gameOver = true;
      }
      if (counts[1] == 0) {
        gameOver = true;
      }
    }
    
    /** Updates the game state based on the accumulated commands. */
    void update () {
      executeAttacks();
      applyEvents();
      regenerate();
      solveCollisions();
      moveIt();
      finishLine();
      checkEnd();
    }
  }
  
  /** Parses the command from player <i>, and passes it to the stepper. */
  public void command (int player, String str) {
    if (player != Constants.attacker && player != Constants.defender) {
      return;
    }
    Command cmd;
    try {
      cmd = Command.loadFrom(str);
    }
    catch (NoSuchElementException | IndexOutOfBoundsException exc) {
      System.err.println(String.format("Error while parsing command '%s': %s", str, exc.getMessage()));
      return;
    }
    stepper.command(player, cmd);
  }
  
  /** Returns true if the game is over: either time has run out, or
   * one side of the battle was completely wiped out (and the rest
   * can be simulated). */
  public boolean isGameOver () {
    return gameOver;
  }
  
  /** Advances the game by one time step. */
  public void advance () {
    if (gameOver) {
      return;
    }
    stepper.update();
    turn += 1;
    if (turn > Constants.maxTurns) {
      gameOver = true;
      stepper = null;
    }
    else {
      stepper = new Stepper();
    }
  }
  
  /** Returns the score accumulated by the attacker so far. */
  public int getScore () {
    return score;
  }
  
  /** Returns a String describing the game state: turn, score, state and
   * locations of units visible to player <i>. If <i> equals -1, returns
   * all units (observer sees it all). */
  public String getData (int player) {
    StringBuilder bui = new StringBuilder();
    bui.append(String.format("%d %d %d\n", turn, score, gameOver));
    
    // find all visible units
    Set<PosUnit> visible = new HashSet<PosUnit>();
    for (Map.Entry<Position, Unit> entry : unitMap.entrySet()) {
      Position pos = entry.getKey();
      Unit unit = entry.getValue();
      if (player != Constants.observer && unit.owner != player) {
        continue;
      }
      visible.add(new PosUnit(pos, unit));
      
      // add all units that this unit can see
      if (player != Constants.observer) {
        for (Position pos2 : visibility.visibleFrom(pos)) {
          if (!unitMap.containsKey(pos2)) {
            continue;
          }
          Unit unit2 = unitMap.get(pos2);
          visible.add(new PosUnit(pos2, unit2));
        }
      }
    }
    // put it all into stringbuilder
    bui.append(visible.size());
    bui.append("\n");
    for (PosUnit pu : visible) {
      bui.append(pu.toString());
      bui.append("\n");
    }
    return bui.toString();
  }
  
  /** Returns a String describing the map: terrain and visibility. */
  public String getMapString () {
    StringBuilder bui = new StringBuilder();
    bui.append(terrain.toString());
    bui.append("\n");
    bui.append(Visibility.toString(terrain.r, terrain.c, visibility));
    return bui.toString();
  }
}
