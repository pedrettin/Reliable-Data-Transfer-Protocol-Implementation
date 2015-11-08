/** Test reliable data transport protocol.
 *  usage: TestRdt myIp myPort wSize timeout [ debug ]
 *  		[ discProb delta runLength ] [ peerIp peerPort  ]
 *  
 *  A pair of TestRdt processes can be used to test the Rdt class.
 *  One is used as a server, the other as a client. The server should
 *  be started first. Both may send packets.
 *  
 *  myIp	is the IP address to be bound to this program's socket
 *  myPort	is the port number to be bound to this program's socket;
 *  		when starting a client, this can be set to zero
 *  wSize	is the window size to be used by the protocol (in packets);
 *	        should be the same at both ends
 *  timeout	is the time that the protocol waits before re-sending a packet
 *		(expressed as a floating point value in seconds)
 *  debug	if the debug argument is present and equal to the string
 *  		"debug", the program prints every packet sent or received
 *  discProb	is the probability that a generated packet gets discarded,
 *  		allowing us to exercise the protocol's ability to recover;
 *  		default value is 0
 *  delta	is the time to wait between sending packets at the source
 *  		(expressed as a floating point value in seconds);
 *  		if zero, no packets are sent; default is 0
 *  runLength	is the duration of the run (expressed as a floating point value
 *  		n seconds); if it is zero, the process runs until it is killed;
 *  		default is zero
 *  peerIp	is the IP address of the peer host - only required for client
 *  peerPort	is the IP address of the peer host - only required for client
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TestRdt {
	public static void main(String[] args) throws Exception {
		// process command line arguments
		if (args.length < 5)  {
			System.out.println("usage: TestRdt myIp myPort " +
				"wSize timeout [ debug ] " +
				"[ discProb delta runLength ] " +
				"[ peerIp peerPort ]");
			System.exit(1);
		}
		InetAddress myIp = InetAddress.getByName(args[0]);
		int myPort = Integer.parseInt(args[1]);
		int wSize = Integer.parseInt(args[2]);
		double timeout = Double.parseDouble(args[3]);

		boolean debug = false; int nextArg = 4;
		if (args.length > nextArg && args[nextArg].equals("debug")) {
			debug = true; nextArg++;
		}
		double discProb = 0;
		if (args.length > nextArg) 
			discProb = Double.parseDouble(args[nextArg++]);
		double delta = 0;
		if (args.length > nextArg) 
			delta = Double.parseDouble(args[nextArg++]);
		double runLength = 0;
		if (args.length > nextArg) {
			runLength = Double.parseDouble(args[nextArg++]);
		}
		InetSocketAddress peerAdr = null;
		if (args.length > nextArg+1) 
			peerAdr = new InetSocketAddress(args[nextArg],
				    	Integer.parseInt(args[nextArg+1]));

		try {
			// instantiate components and start their threads
			Substrate sub = new Substrate(myIp,myPort,peerAdr,
						      discProb,debug);
			sub.start();
			Rdt rdt = new Rdt(wSize,timeout,sub);
			rdt.start();
			// delay sending of packets by server, to give client
			// a chance to send the first packet
			if (peerAdr == null) Thread.sleep(2000);
			SrcSnk ss = new SrcSnk(delta,runLength,rdt);
			ss.start();
			// wait for substrate to quit, then stop others
			sub.join(); rdt.stop(); ss.stop();
		} catch(Exception e) {
			System.out.println("TestRdt: exception " + e);
			System.exit(1);
		}

	}
}
