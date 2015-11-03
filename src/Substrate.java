import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Substrate {
	private DatagramSocket sock;
	private InetSocketAddress peerAdr;
	private double discProb;
	private boolean debug;

	private Sender sndr;
	private Receiver rcvr;
	
	/** Initialize a new Substrate object.
	 *  @param myIp is the IP address to bind to the socket
	 *  @param port is the port number to bind to the socket (may be 0)
	 *  @param peerAdr is the IP address/port pair for the peer host
	 *  (may be null, if this object is being used in a server)
	 *  @param discProb is a discard probability used to randomly discard
	 *  packets received from the Rdt object
	 *  @param debug is a flag; if it is 1, each packet sent and received
	 *  is printed out
	 */
	Substrate(InetAddress myIp, int port, InetSocketAddress peerAdr,
		  double discProb, boolean debug) {
		// initialize instance variables
		this.peerAdr = peerAdr;
		this.discProb = discProb;
		this.debug = debug;

		// open and configure socket with timeout
		sock = null;
		try {
			sock = new DatagramSocket(port,myIp);
			sock.setSoTimeout(100);
			sock.setReceiveBufferSize(1000000);
		} catch(Exception e) {
			System.out.println("unable to create socket");
			System.exit(1);
		}

		sndr = new Sender(sock,peerAdr,discProb,debug);
		rcvr = new Receiver(sock,peerAdr,sndr,debug);
	}

	/** Start Substrate running. */
	public void start() { sndr.start(); rcvr.start(); }

	/** Wait for Substrate to stop. */
	public void join() throws Exception { sndr.join(); rcvr.join(); }

	/** Send a packet.
	 *  @param p is a packet to be sent
	 */
	public void send(Packet p) { sndr.send(p); }
		
	/** Test if substrate is ready to send more packets.
	 *  @return true if substrate is ready
	 */
	public boolean ready() { return sndr.ready(); }

	/** Retrieve the next packet from the substrate.
	 *  @return the next incoming packet
	 */
	public Packet receive() { return rcvr.receive(); }
	
	/** Test for the presence of incoming packets.
	 *  @return true if there are packets available to be received.
	 */
	public boolean incoming() { return rcvr.incoming(); }
}
