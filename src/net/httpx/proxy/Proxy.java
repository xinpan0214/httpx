package net.httpx.proxy;

import java.io.*;
import java.net.*;

/**
* Based on code by Julian Robichaux -- http://www.nsftools.com.
 */
public class Proxy
    extends Thread {
  public static final int DEFAULT_PORT = 8080;

  private ServerSocket server = null;
  private int thisPort = DEFAULT_PORT;
  private String fwdServer = "";
  private int fwdPort = 0;
  private int ptTimeout = ProxyThread.DEFAULT_TIMEOUT;
  private int debugLevel = 0;
  private PrintStream debugOut = System.out;

  ContentProvider contentProvider;


  /* here's a main method, in case you want to run this by itself */
  public static void main(String args[]) {
    int port = 0;
    String fwdProxyServer = "";
    int fwdProxyPort = 0;

    if (args.length == 0) {
      System.err.println(
          "USAGE: java jProxy <port number> [<fwd proxy> <fwd port>]");
      System.err.println("  <port number>   the port this service listens on");
      System.err.println(
          "  <fwd proxy>     optional proxy server to forward requests to");
      System.err.println(
          "  <fwd port>      the port that the optional proxy server is on");
      System.err.println(
          "\nHINT: if you don't want to see all the debug information flying by,");
      System.err.println(
          "you can pipe the output to a file or to 'nul' using \">\". For example:");
      System.err.println(
          "  to send output to the file prox.txt: java jProxy 8080 > prox.txt");
      System.err.println("  to make the output go away: java jProxy 8080 > nul");
      return;
    }

    // get the command-line parameters
    port = Integer.parseInt(args[0]);
    if (args.length > 2) {
      fwdProxyServer = args[1];
      fwdProxyPort = Integer.parseInt(args[2]);
    }

    // create and start the jProxy thread, using a 20 second timeout
    // value to keep the threads from piling up too much
    System.err.println("  **  Starting jProxy on port " + port +
                       ". Press CTRL-C to end.  **\n");
    Proxy jp = new Proxy(port, fwdProxyServer, fwdProxyPort, 20);
    jp.setDebug(1, System.out); // or set the debug level to 2 for tons of output
    jp.start();

    // run forever; if you were calling this class from another
    // program and you wanted to stop the jProxy thread at some
    // point, you could write a loop that waits for a certain
    // condition and then calls jProxy.closeSocket() to kill
    // the running jProxy thread
    while (true) {
      try {
        Thread.sleep(3000);
      }
      catch (Exception e) {}
    }

    // if we ever had a condition that stopped the loop above,
    // we'd want to do this to kill the running thread
    //jp.closeSocket();
    //return;
  }

  /* the proxy server just listens for connections and creates
   * a new thread for each connection attempt (the ProxyThread
   * class really does all the work)
   */
  public Proxy(int port) {
    thisPort = port;
  }

  public Proxy(int port, String proxyServer, int proxyPort) {
    thisPort = port;
    fwdServer = proxyServer;
    fwdPort = proxyPort;
  }

  public Proxy(int port, String proxyServer, int proxyPort, int timeout) {
    thisPort = port;
    fwdServer = proxyServer;
    fwdPort = proxyPort;
    ptTimeout = timeout;
  }


  public void setContentProvider(ContentProvider provider) {
    this.contentProvider = provider;
  }

  public ContentProvider getContentProvider() {
    return contentProvider;
  }

  /* allow the user to decide whether or not to send debug
   * output to the console or some other PrintStream
   */
  public void setDebug(int level, PrintStream out) {
    debugLevel = level;
    debugOut = out;
  }

  /* get the port that we're supposed to be listening on
   */
  public int getPort() {
    return thisPort;
  }

  /* return whether or not the socket is currently open
   */
  public boolean isRunning() {
    if (server == null)
      return false;
    else
      return true;
  }

  /* closeSocket will close the open ServerSocket; use this
   * to halt a running jProxy thread
   */
  public void closeSocket() {
    try {
      // close the open server socket
      server.close();
      // send it a message to make it stop waiting immediately
      // (not really necessary)
      /*Socket s = new Socket("localhost", thisPort);
          OutputStream os = s.getOutputStream();
          os.write((byte)0);
          os.close();
          s.close();*/
    }
    catch (Exception e) {
      if (debugLevel > 0)
        debugOut.println(e);
    }

    server = null;
  }

  public void run() {
    try {
      // create a server socket, and loop forever listening for
      // client connections
      server = new ServerSocket(thisPort);
      if (debugLevel > 0)
        debugOut.println("Started Proxy on port " + thisPort);

      while (true) {
        Socket client = server.accept();
        // accept only localhost connections
        if(client.getInetAddress().isAnyLocalAddress() || client.getInetAddress().isLoopbackAddress()) {
          //debugOut.println("[+] ProxyThread");
          ProxyThread t = new ProxyThread(this, client, fwdServer, fwdPort);
          t.setDebug(debugLevel, debugOut);
          t.setTimeout(ptTimeout);
          t.start();
        } else {
          debugOut.println("Proxy connection attempt from foreign host ignored");
          try {
            client.close();
          } catch(Exception e) {
          }
        }
      }
    }
    catch (Exception e) {
      if (debugLevel > 0)
        debugOut.println("ProxyThread error: " + e);
    }

    closeSocket();
  }

}

