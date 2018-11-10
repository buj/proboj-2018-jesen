package server.communication;

import java.io.*;
import java.net.*;


/** Used to spawn Clients. It waits on a ServerSocket for incoming
 * connections. */
public class Receptionist {
  protected ServerSocket server;
  
  public Receptionist (SocketAddress addr) throws IOException {
    server = new ServerSocket();
    server.bind(addr);
  }
  
  /** Waits until a connection arrives, then creates a Client instance
   * for it and returns it. */
  public Client accept () throws IOException {
    Socket incoming = server.accept();
    return new Client(incoming);
  }
}
