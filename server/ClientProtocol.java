package server;

import java.io.*;
import java.util.logging.*;
import server.communication.Client;
import server.game.GameServer;


/** Encapsulates subprotocols, such as communication with lobby and
 * communication with game server, into a single protocol. Should be
 * carried out asynchronously by a new thread. */
public class ClientProtocol implements Runnable {
  protected static Logger logger = Logger.getLogger("Server");
  
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
      logger.info(String.format("ClientProtocol: IOException while communicating with client %d (id = %d) [%s], ending conversation", client.hashCode(), client.id, exc.getMessage()));
    }
    lobby.free(client.id);
  }
}
