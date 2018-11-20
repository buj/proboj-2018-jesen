package server.communication;

import java.io.*;
import java.net.*;
import java.util.*;


/** Can send and receive messages from the otherside. Also, we can set
 * and retrieve its player ID, which is done in the lobby. This
 * parameter is later used in the game. */
public class Client {
  public int id;
  
  protected Socket socket;
  protected Scanner sc;
  protected PrintStream ps;
  
  /** Creates a Client that listens and sends to <socket0>. Starting
   * id is -1, indicating 'undefined' (such a Client will do nothing
   * once the game starts). */
  public Client (Socket socket0) throws IOException {
    socket = socket0;
    id = -1;
    sc = new Scanner(socket.getInputStream());
    ps = new PrintStream(socket.getOutputStream());
  }
  
  /** Blocking. Waits for and returns the next line of input. Throws
   * an IOException if there is no further input. */
  public String receive () throws IOException {
    try {
      return sc.nextLine();
    }
    catch (NoSuchElementException exc) {
      throw new IOException("Error while receiving: scanner threw NoSuchElementException", exc);
    }
  }
  
  /** Sends the provided message to the otherside. Ignores all errors. */
  public void send (String msg) {
    ps.println(msg);
  }
  
  /** Closes the underlying socket. */
  public void die () {
    try {
      socket.close();
    }
    catch (IOException exc) {
      System.err.println(String.format("%d: IOException while closing socket, during client death [%s]", id, exc.getMessage()));
    }
  }
}
