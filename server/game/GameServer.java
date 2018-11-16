package server.game;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import server.game.logic.Game;


/** A runnable that runs the game and has methods that clients can
 * use to communicate with the game (set commands, ...). */
class GameServer implements Runnable {
  protected Clock clock;
  
  protected Game game;
  protected BlockingQueue<String> atkCommands, defCommands;
  
  // observer stuff
  protected final String mapInfo;
  protected List<String> atkHistory, defHistory, obsHistory;
  
  /** Constructs a game server that will run the provided game. The
   * provided game should be freshly constructed. */
  public GameServer (Game game0) {
    game = game0;
    atkCommands = new LinkedBlockingQueue<String>();
    defCommands = new LinkedBlockingQueue<String>();
    
    // initial data
    mapInfo = game.getMapString();
    atkHistory = new ArrayList<String>();
    defHistory = new ArrayList<String>();
    obsHistory = new ArrayList<String>();
    for (int id = -1; id <= 1; id++) {
      sourceOf(id).add(game.getData(id));
    }
  }
  
  @Override
  public void run () {
    Duration turnTime = Duration.ofMillis(Constants.turnMillis); // duration of one turn
    Instant start = clock.instant(); // start of the turn
    
    while (!game.isGameOver()) {
      // sleep a while, can't interrupt this...
      Instant target = start.plus(turnTime);
      while (true) {
        Instant now = clock.instant();
        if (target.isBefore(now)) {
          break;
        }
        Duration duration = Duration.between(now, target);
        try {
          Thread.sleep(1000 * duration.getSeconds(), duration.getNano());
        }
        catch (InterruptedException exc) {
          System.err.format("Tried to interrupt game server... but it just ignores the interrupt. [%s]", exc.getMessage());
        }
      }
      start = clock.instant();
      
      // communicate commands to the game
      List<String> atkList = new ArrayList<String>();
      List<String> defList = new ArrayList<String>();
      atkCommands.drainTo(atkList);
      defCommands.drainTo(defList);
      for (String cmd : atkList) {
        game.command(Constants.attacker, cmd);
      }
      for (String cmd : defList) {
        game.command(Constants.defender, cmd);
      }
      
      // advance the game state, update histories
      game.advance();
      for (int id = -1; id <= 1; id++) {
        List<String> source = sourceOf(id);
        synchronized (source) {
          source.add(game.getData(id));
          source.notifyAll();
        }
      }
    }
  }
  
  //////////// CLIENT METHODS //////////////////////////////////////////
  
  /** Returns the terrain and visibility information. */
  public String getMap () {
    return mapInfo;
  }
  
  /** Returns the list that provides information to player <id>. */
  List<String> sourceOf (int id) {
    switch (id) {
      case Constants.defender: return defHistory;
      case Constants.attacker: return atkHistory;
    }
    return obsHistory;
  }
  
  /** Returns the current game information for player <id> (-1 is observer).
   * This should only be called once by the client: when he requests information for
   * the first time. Further inquiries should use 'getAtTime', to avoid
   * obtaining the same information twice. */
  public String getCurrent (int id) {
    List<String> source = sourceOf(id);
    synchronized (source) {
      int n = source.size();
      return source.get(n-1);
    }
  }
  
  /** Returns the game state for player <id> at time <t>. */
  public String getAtTime (int id, int t) {
    List<String> source = sourceOf(id);
    synchronized (source) {
      while (source.size() <= t) {
        try {
          source.wait();
        }
        catch (InterruptedException exc) {
          System.err.format("Interrupt during 'getAtTime' of gameServer... but it is ignored [%s]", exc.getMessage());
        }
      }
      return source.get(t);
    }
  }
}
