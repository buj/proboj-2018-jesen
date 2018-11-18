package server.game;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import server.communication.Client;
import server.game.logic.Game;


/** A runnable that runs the game and has methods that clients can
 * use to communicate with the game (set commands, ...). */
public class GameServer implements Runnable {
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
    for (int id = -1; id <= 1; id++) {
      sourceOf(id).add(game.getData(id));
    }
  }
  
  @Override
  public void run () {
    Duration turnTime = Duration.ofMillis(Constants.turnMillis); // duration of one turn
    Instant start = clock.instant(); // start of the turn
    
    while (!game.isGameOver()) {
      System.err.format("GameServer: starting turn %d\n", game.getTurn());
      
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
          System.err.format("Tried to interrupt game server... but it just ignores the interrupt. [%s]\n", exc.getMessage());
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
  
  /** Returns the player id, terrain and visibility information for player <id>. */
  public String getIntro (int id) {
    StringBuilder bui = new StringBuilder();
    bui.append(id);
    bui.append("\n");
    bui.append(mapInfo);
    return bui.toString();
  }
  
  /** Returns the queue of commands of player <id>. */
  BlockingQueue<String> commandsOf (int id) {
    switch (id) {
      case Constants.defender: return defCommands;
      case Constants.attacker: return atkCommands;
    }
    return null;
  }
  
  /** Returns the list that provides information to player <id>. */
  List<String> sourceOf (int id) {
    switch (id) {
      case Constants.defender: return defHistory;
      case Constants.attacker: return atkHistory;
    }
    return obsHistory;
  }
  
  /** Returns the game state for player <id>, whose last received state
   * information comes from time <t>. Waits until the history has size
   * at least <t+2>, and then returns the last game state.
   * 
   * Clients that did not receive any information yet should ask with t = -1. */
  public String getAtTime (int id, int t) {
    List<String> source = sourceOf(id);
    synchronized (source) {
      while (source.size() <= t+1) {
        try {
          source.wait();
        }
        catch (InterruptedException exc) {
          System.err.format("Interrupt during 'getAtTime' of gameServer... but it is ignored [%s]\n", exc.getMessage());
        }
      }
      int n = source.size();
      return source.get(n-1);
    }
  }
  
  /** Starts a conversation with the provided client. */
  public void communicateWith (Client client) throws IOException {
    while (true) {
      Scanner sc = new Scanner(client.receive());
      String cmdType;
      try {
        cmdType = sc.next();
      }
      catch (NoSuchElementException exc) {
        System.err.format("GameServer: got empty message from client %d (id = %d)\n", client.hashCode(), client.id);
        continue;
      }
      if (cmdType.equals("command")) {
        if (client.id != Constants.defender && client.id != Constants.attacker) { // only real players may act!
          continue;
        }
        String cmd;
        try {
          cmd = sc.nextLine();
        }
        catch (NoSuchElementException exc) {
          System.err.format("GameServer: got 'command' but there is nothing further to clarify what command; from client %d (id = %d)\n", client.hashCode(), client.id);
          continue;
        }
        commandsOf(client.id).add(cmd);
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
          System.err.format("GameServer: got 'get' but then expected a turn number, got something else; from client %d (id = %d)\n", client.hashCode(), client.id);
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
