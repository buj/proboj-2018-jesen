package server;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import server.communication.*;


/** Prior to the game's start, all clients are located in the lobby.
 * There are a limited number of player seats (id = 0, 1), but unlimited
 * number of do-nothing seats (id = -1). */
public class Lobby {
  protected static Logger logger = Logger.getLogger("Server");
  
  protected boolean[] occupied;
  
  /** Initial empty lobby. */
  public Lobby () {
    occupied = new boolean[2];
  }
  
  /** Returns whether seat number <i> is exclusive (only 1 client can
   * have it) or if any number of clients may have it. */
  static boolean is_exclusive (int i) {
    return (i == 0 || i == 1);
  }
  
  /** Tell the lobby that seat <i> has become occupied. Returns true
   * if the takeover was successful, false otherwise. */
  boolean take (int i) {
    if (is_exclusive(i)) {
      synchronized (this) {
        if (!occupied[i]) {
          occupied[i] = true;
          notifyAll();
          return true;
        }
      }
    }
    return false;
  }
  
  /** Tells the lobby that seat <i> has become free. */
  void free (int i) {
    if (is_exclusive(i)) {
      synchronized (this) {
        occupied[i] = false;
        notifyAll();
      }
    }
  }
  
  /** Returns the number of occupied seats. */
  int num_occupied () {
    int res = 0;
    synchronized (this) {
      for (int i = 0; i < 2; i++) {
        res += (occupied[i] ? 1 : 0);
      }
    }
    return res;
  }
  
  /** Starts a conversation with the provided client regarding his seat.
   * Throws a NoSuchElementException when the client shuts down. */
  public void communicateWith (Client client) throws IOException {
    while (true) {
      Scanner sc = new Scanner(client.receive());
      String cmd;
      try {
        cmd = sc.next();
      }
      catch (NoSuchElementException exc) {
        logger.info(String.format("got empty message from client %d (id = %d)", client.hashCode(), client.id));
        continue;
      }
      if (cmd.equals("take")) {
        int i;
        try {
          i = sc.nextInt();
        }
        catch (NoSuchElementException exc) {
          logger.info(String.format("got 'take' from %d, but what follows is not an int", client.hashCode()));
          continue;
        }
        if (take(i)) {
          free(client.id);
          client.id = i;
          client.send("ok");
        }
        else {
          client.send("denied");
        }
      }
      else
      if (cmd.equals("free")) {
        free(client.id);
        client.id = -1;
        client.send("ok");
      }
      else
      if (cmd.equals("finish")) {
        break;
      }
    }
  }
}
