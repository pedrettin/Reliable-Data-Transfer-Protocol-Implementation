/** Datagram sender.
 *
 *  This class provides a send interface to a datagram socket.
 *  Specifically, it allows one to test to see if another packet
 *  can be sent, before attempting a potentially blocking send
 *  operation.
 */  

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Sender implements Runnable {
	private DatagramSocket sock;
	private InetSocketAddress peerAdr;
	private double discProb;
	private boolean debug;

	private ArrayBlockingQueue<Packet> sendq;
	private Thread myThread;	// thread that executes run() method

	Sender(DatagramSocket sock, InetSocketAddress peerAdr,
		    double discProb, boolean debug) {
		this.sock = sock; this.peerAdr = peerAdr;
		this.discProb = discProb; this.debug = debug;

		// initialize queue for received packets
		// stores both the packet and socket address of the sender
		sendq = new ArrayBlockingQueue<Packet>(1000,true);
	}

	/** Instantiate run() thread and start it running. */
	public void start() {
		myThread = new Thread(this); myThread.start();
	}

	/** Wait for thread to quit. */
	public void join() throws Exception { myThread.join(); }

	public void setPeerAdr(InetSocketAddress peerAdr) {
		this.peerAdr = peerAdr;
	}

	/** Send thread sends out-going packets to the network.
	 *  This method is run by a separate thread. Whenever there
	 *  is an outgoing packet to be sent, it sends it and waits
	 *  for the next.
	 */
	public void run() {
		long t0 = System.nanoTime();
		long now, eventTime, firstEventTime;
		now = eventTime = firstEventTime = 0;

		byte[] buf = new byte[1]; // dummy buffer, not really used 
                DatagramPacket dg = new DatagramPacket(buf,1);

		int sendCount, sendAck, discCount, discAck;
		sendCount = sendAck = discCount = discAck = 0;;

		// run until nothing has happened for 3 seconds
		while (eventTime == 0 || now < eventTime + 3000000000L) {
			now = System.nanoTime() - t0;
			// idle until peerAdr is set
			if (peerAdr == null) {
				try {
					Thread.sleep(100);
				} catch(Exception e) {
					System.err.println("Sender:run: "
						+ "sleep exception " + e);
					System.exit(1);
				}
				continue;
			}
			// check sendq for a packet, if none arrive in 100 ms,
			// check termination condition before trying again
			Packet p = null;
			try {
				p = sendq.poll(100,TimeUnit.MILLISECONDS);
			} catch(Exception e) {
				System.err.println("Sender:run: exception " +e);
				System.exit(1);
			}
			if (p == null) continue; // check for termination
			if (p.type == 0) sendCount++;
			else sendAck++;
			eventTime = now;
			if (firstEventTime == 0) firstEventTime = now;
			if (Math.random() < discProb) {
				if (p.type == 0) discCount++;
				else discAck++;
				if (debug) {
					System.out.println("discarding " + p);
					System.out.flush();
				}
				continue;
			}

			// pack and send the packet
			buf = p.pack();
                	if (buf == null) {
				System.err.println("Sender: packing error " +p);
				System.exit(1);
			}
			dg.setData(buf); dg.setLength(buf.length);
			dg.setSocketAddress(peerAdr);
	                if (debug) {
	                        System.out.println(sock.getLocalSocketAddress()
	                                + " sending to " 
	                                + dg.getSocketAddress() + " " + p);
	                        System.out.flush();
	                }
                	try {
				sock.send(dg);
			} catch(Exception e) {
				System.err.println("Sender: send error " + p);
				System.exit(1);
			}
		}
		System.out.println("  Sender: sent " + sendCount 
				+ " data packets, " + sendAck + " acks"); 
		System.out.println("          discarded " + discCount 
				+ " data packets, " + discAck + " acks"); 
		System.out.println("          runLength " 
			+ (((double) (eventTime - firstEventTime))/1000000000));
	}

	/** Send a packet to a specified destination.
	 *  @param p is packet to be sent
	 */
	public void send(Packet p) {
		try {
			sendq.put(p);
		} catch(Exception e) {
			System.err.println("Sender:send sendq exception " + e);
			System.exit(1);
		}
	}

	/** Return true if ready to accept another packet. */
	public boolean ready() { return sendq.remainingCapacity() > 0; }
}
