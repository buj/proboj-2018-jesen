package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import server.communication.*;
import server.game.logic.Game;
import server.game.map.*;
import server.game.units.*;
import server.game.*;


/** Runs it all. */
public class Server implements Runnable {
  protected static Logger logger = Logger.getLogger("Server");
  
  protected Random rng;
  protected Receptionist receptionist;
  protected Lobby lobby;
  protected Listener listener;
  protected GameServer gserver;
  
  /** Creates a test game. */
  public Server (Map<String, String> argMap) throws IOException {
    // set seed if in args
    int seed = 1023456789;
    if (argMap.containsKey("seed")) {
      seed = Integer.parseInt(argMap.get("seed"));
    }
    rng = new Random(seed);
    
    // default arguments
    Terrain terra = Terrain.mildRandom(rng, 100, 100);
    List<InitialUnit> initial = InitialUnit.dummyStartingPositions(terra);
    
    // first argument is map and initial units file
    if (argMap.containsKey("map")) {
      FileInputStream fin = new FileInputStream(argMap.get("map"));
      Scanner sc = new Scanner(fin);
      terra = new Terrain(sc);
      initial = InitialUnit.getStartingPositions(sc);
    }
    
    // optional second argument: IP address and port
    String addr = "127.0.0.1";
    int port = 4247;
    if (argMap.containsKey("addr")) {
      addr = argMap.get("addr");
    }
    if (argMap.containsKey("port")) {
      port = Integer.parseInt(argMap.get("seed"));
    }
    receptionist = new Receptionist(new InetSocketAddress(addr, port));
    lobby = new Lobby();
    
    // creates the game
    Game game = new Game(rng, terra, initial);
    gserver = new GameServer(game);
    
    // creates the thing that listens for clients
    listener = new Listener(receptionist, lobby, gserver);
  }
  
  @Override
  public void run () {
    // starts the thing that listens for clients
    Thread lobby_worker = new Thread(listener);
    lobby_worker.setDaemon(true);
    lobby_worker.start();
    
    // wait for two players, then start the game
    synchronized (lobby) {
      while (lobby.num_occupied() != 2) {
        logger.info(String.format("waiting for players to join..."));
        try {
          lobby.wait();
        }
        catch (InterruptedException exc) {
          logger.info(String.format("interrupted while waiting for players to join. Gonna quit"));
          return;
        }
      }
    }
    Thread game_worker = new Thread(gserver);
    game_worker.start();
    
    try { // wait for game worker to finish
      game_worker.join();
    }
    catch (InterruptedException exc) {
      logger.info(String.format("interrupt while waiting for game server to die. Gonna die non-gracefully [%s]", exc.getMessage()));
    }
    // create observation files
    String[] names = new String[]{"observer", "defender", "attacker"};
    for (int id = -1; id <= 1; id++) {
      try {
        String filename = String.format("%s.log", names[1 + id]);
        PrintStream fout = new PrintStream(new FileOutputStream(filename));
        fout.print(gserver.getHistory(id));
        fout.close();
      }
      catch (FileNotFoundException exc) {
        logger.info(String.format("cannot create observation file number %d [%s]", id, exc.getMessage()));
      }
    }
  }
  
  /** Runs a test game. */
  public static void main (String[] args) {
    // set appropriate formatting
    System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tT:%1$tL %2$s]%n%4$s: %5$s%6$s%n");
    
    // run the server
    Server server;
    try {
      Map<String, String> argMap = new HashMap<>();
      for (String arg : args) {
        String[] splitted = arg.split("=");
        argMap.put(splitted[0], splitted[1]);
      }
      server = new Server(argMap);
    }
    catch (IOException exc) {
      logger.info(String.format("error while creating server, receptionist IOException, aborting. [%s]", exc.getMessage()));
      return;
    }
    catch (IndexOutOfBoundsException exc) {
      logger.info(String.format("error while parsing arguments: expected something of form x=y [%s]", exc.getMessage()));
      return;
    }
    server.run();
  }
}


/** Listens for connections. */
class Listener implements Runnable {
  protected static Logger logger = Logger.getLogger("Server");
  
  protected Receptionist receptionist;
  protected Lobby lobby;
  protected GameServer gserver;
  
  Listener (Receptionist receptionist0, Lobby lobby0, GameServer gserver0) {
    receptionist = receptionist0;
    lobby = lobby0;
    gserver = gserver0;
  }
  
  @Override
  public void run () {
    try { // listen for client connectionst
      while (true) {
        Client client = receptionist.accept();
        Thread worker = new Thread(new ClientProtocol(client, lobby, gserver));
        worker.setDaemon(true);
        worker.start();
      }
    }
    catch (IOException exc) {
      logger.info(String.format("IOException while running, ending. [%s]", exc.getMessage()));
    }
  }
}
