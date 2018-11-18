package server;

import java.io.*;
import server.communication.Client;
import server.game.GameServer;


/** Encapsulates subprotocols, such as communication with lobby and
 * communication with game server, into a single protocol. Should be
 * carried out asynchronously by a new thread. */
public class ClientProtocol implements Runnable {
  protected Client client;
  protected Lobby lobby;
  protected GameServer gserver;
  
  /** Creates a ClientProtocol that communicates with client <client0>,
   * lobby <lobby0> and game server <gserver0>. */
  public ClientProtocol (Client client0, Lobby lobby0, GameServer gserver0) {
    client = client0;
    lobby = lobby0;
    gserver = gserver0;
  }
  
  @Override
  public void run () {
    try {
      lobby.communicateWith(client);
      gserver.communicateWith(client);
    }
    catch (IOException exc) {
      System.err.format("ClientProtocol: IOException while communicating with client %d (id = %d) [%s], ending conversation\n", client.hashCode(), client.id, exc.getMessage());
    }
    lobby.free(client.id);
  }
}
