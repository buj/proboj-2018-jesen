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
  public Server () throws IOException {
    receptionist = new Receptionist(new InetSocketAddress("127.0.0.1", 4247));
    lobby = new Lobby();
    
    // creates the game
    rng = new Random(1023456789);
    Terrain terra = Terrain.mildRandom(rng, 100, 100);
    List<InitialUnit> initial = InitialUnit.dummyStartingPositions(terra);
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
        logger.info(String.format("Server: waiting for players to join...\n"));
        try {
          lobby.wait();
        }
        catch (InterruptedException exc) {
          logger.info(String.format("Server: interrupted while waiting for players to join. Gonna quit"));
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
      logger.info(String.format("Server: Interrupt while waiting for game server to die. Gonna die non-gracefully [%s]", exc.getMessage()));
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
        logger.info(String.format("Server: cannot create observation file number %d [%s]", id, exc.getMessage()));
      }
    }
  }
  
  /** Runs a test game. */
  public static void main (String[] args) {
    Server server;
    try {
      server = new Server();
    }
    catch (IOException exc) {
      logger.info(String.format("Server main: error while creating server, receptionist IOException, aborting. [%s]", exc.getMessage()));
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
      logger.info(String.format("Listener: IOException while running, ending. [%s]", exc.getMessage()));
    }
  }
}
