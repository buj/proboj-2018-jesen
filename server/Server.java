package server;

import java.io.*;
import java.net.*;
import java.util.*;
import server.communication.*;
import server.game.logic.Game;
import server.game.map.*;
import server.game.units.*;
import server.game.*;


/** Runs it all. */
public class Server implements Runnable {
  protected Random rng;
  protected Listener listener;
  protected GameServer gserver;
  
  /** Creates a test game. */
  public Server () throws IOException {
    Receptionist receptionist = new Receptionist(new InetSocketAddress("127.0.0.1", 4247));
    Lobby lobby = new Lobby();
    
    // creates the game
    rng = new Random(1023456789);
    Terrain terra = Terrain.mildRandom(rng, 10, 10);
    List<InitialUnit> initial = InitialUnit.dummyStartingPositions(terra);
    Game game = new Game(rng, terra, initial);
    gserver = new GameServer(game);
    
    // creates the listener
    listener = new Listener(receptionist, lobby, gserver);
  }
  
  @Override
  public void run () {
    // starts the thing that listens for clients
    Thread lobby_worker = new Thread(listener);
    lobby_worker.setDaemon(true);
    lobby_worker.start();
    
    // start the game
    Thread game_worker = new Thread(gserver);
    game_worker.start();
    
    try { // wait for game worker to finish
      game_worker.join();
    }
    catch (InterruptedException exc) {
      System.err.format("Server: Interrupt while waiting for game server to die. Gonna die non-gracefully [%s]\n", exc.getMessage());
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
        System.err.format("Server: cannot create observation file number %d [%s]\n", id, exc.getMessage());
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
      System.err.format("Server main: error while creating server, receptionist IOException, aborting. [%s]\n", exc.getMessage());
      return;
    }
    server.run();
  }
}


/** Listens for connections. */
class Listener implements Runnable {
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
      System.err.format("Listener: IOException while running, ending. [%s]\n", exc.getMessage());
    }
  }
}
