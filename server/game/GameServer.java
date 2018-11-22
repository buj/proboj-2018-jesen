package server.game;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.time.*;
import server.communication.Client;
import server.game.logic.Game;


/** A runnable that runs the game and has methods that clients can
 * use to communicate with the game (set commands, ...). */
public class GameServer implements Runnable {
  protected static Logger logger = Logger.getLogger("Game");
  
  protected Clock clock;
  
  protected Game game;
  protected BlockingQueue<String> atkCommands, defCommands;
  
  // observer stuff
  protected final String mapInfo;
  protected List<String> atkHistory, defHistory, obsHistory;
  
  /** Constructs a game server that will run the provided game. The
   * provided game should be freshly constructed. */
  public GameServer (Game game0) {
    clock = Clock.systemDefaultZone();
    
    game = game0;
    atkCommands = new LinkedBlockingQueue<String>();
    defCommands = new LinkedBlockingQueue<String>();
    
    // initial data
    mapInfo = game.getMapString();
    atkHistory = new ArrayList<String>();
    defHistory = new ArrayList<String>();
    obsHistory = new ArrayList<String>();
    for (int id = 1; id >= -1; id--) {
      sourceOf(id).add(game.getData(id));
    }
  }
  
  @Override
  public void run () {
    logger.info(String.format("GameServer: starting turn %d", game.getTurn()));
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
        int ns = duration.getNano();
        int ms = ns / 1000000;
        ns %= 1000000;
        try {
          Thread.sleep(1000 * duration.getSeconds() + ms, ns);
        }
        catch (InterruptedException exc) {
          logger.info(String.format("Tried to interrupt game server... but it just ignores the interrupt. [%s]", exc.getMessage()));
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
      logger.info(String.format("GameServer: starting turn %d", game.getTurn()));
      for (int id = 1; id >= -1; id--) {
        List<String> source = sourceOf(id);
        synchronized (source) {
          source.add(game.getData(id));
        }
      }
      for (int id = 1; id >= -1; id--) {
        List<String> source = sourceOf(id);
        synchronized (source) {
          source.notifyAll();
        }
      }
    }
  }
  
  //////////// CLIENT METHODS //////////////////////////////////////////
  
  /** Returns the player id, terrain, visibility, and current unit
   * configuration information for player <id>. */
  public String getIntro (int id) {
    StringBuilder bui = new StringBuilder();
    bui.append(getStatic(id));
    bui.append("\n");
    bui.append(getAtTime(id, -1));
    return bui.toString();
  }
  
  /** Returns the static parts of the game: the player's id, terrain
   * and visibility. */
  public String getStatic (int id) {
    StringBuilder bui = new StringBuilder();
    bui.append(id);
    bui.append("\n");
    bui.append(mapInfo);
    return bui.toString();
  }
  
  /** Returns the queue of commands of player <id>. */
  BlockingQueue<String> commandsOf (int id) {
    switch (id) {
      case Constants.attacker: return atkCommands;
      case Constants.defender: return defCommands;
    }
    return null;
  }
  
  /** Returns the list that provides information to player <id>. */
  List<String> sourceOf (int id) {
    switch (id) {
      case Constants.attacker: return atkHistory;
      case Constants.defender: return defHistory;
    }
    return obsHistory;
  }
  
  /** Returns the game state for player <id>, whose last received state
   * information comes from time <t>. Waits until the history has size
   * at least <t+1>, and then returns the last game state.
   * 
   * Clients that did not receive any information yet should ask with t = -1. */
  public String getAtTime (int id, int t) {
    List<String> source = sourceOf(id);
    synchronized (source) {
      while (source.size() <= t) {
        try {
          source.wait();
        }
        catch (InterruptedException exc) {
          logger.info(String.format("Interrupt during 'getAtTime' of gameServer... but it is ignored [%s]", exc.getMessage()));
        }
      }
      int n = source.size();
      return source.get(n-1);
    }
  }
  
  /** Returns the entire history, from the point of view of player <id>.
   * Should only be called after the game concludes. */
  public String getHistory (int id) {
    StringBuilder bui = new StringBuilder();
    bui.append(getStatic(id));
    bui.append("\n");
    
    List<String> source = sourceOf(id);
    boolean first = true;
    for (String str : source) {
      if (!first) {
        bui.append("\n");
      }
      first = false;
      bui.append(str);
    }
    return bui.toString();
  }
  
  /** Starts a conversation with the provided client. */
  public void communicateWith (Client client) throws IOException {
    while (true) {
      try {
        Thread.sleep(5);
      }
      catch (InterruptedException exc) {}
      Scanner sc = new Scanner(client.receive());
      String cmdType;
      try {
        cmdType = sc.next();
      }
      catch (NoSuchElementException exc) {
        logger.info(String.format("GameServer: got empty message from client %d (id = %d)", client.hashCode(), client.id));
        continue;
      }
      if (cmdType.equals("commands")) {
        if (client.id != Constants.attacker && client.id != Constants.defender) { // only real players may act!
          continue;
        }
        String desc;
        try {
          desc = sc.nextLine();
        }
        catch (NoSuchElementException exc) {
          logger.info(String.format("GameServer: got 'commands' but there is nothing further to clarify what command; from client %d (id = %d)", client.hashCode(), client.id));
          continue;
        }
        commandsOf(client.id).add(desc);
      }
      else
      if (cmdType.equals("intro")) {
        client.send(getIntro(client.id));
      }
      else
      if (cmdType.equals("get")) {
        int t;
        try {
          t = sc.nextInt();
        }
        catch (NoSuchElementException exc) {
          logger.info(String.format("GameServer: got 'get' but then expected a turn number, got something else; from client %d (id = %d)", client.hashCode(), client.id));
          continue;
        }
        String data = getAtTime(client.id, t);
        client.send(data);
      }
      else
      if (cmdType.equals("finish")) {
        break;
      }
    }
  }
}
