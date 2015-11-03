/** Source/Sink class
 *  This class generates packet payloads and checks incoming
 *  payloads, sent by a peer host. Each outgoing payload takes
 *  the form of the string "testing 123" where the number is a
 *  sequence number that starts at 1 and is incremented for every
 *  payload sent. When payloads are received, we check the sequence
 *  number of the arriving payloads to make sure that we are getting
 *  the payloads in the proper sequence.
 *
 *  When a new payload is generated, it is passed to an Rdt object,
 *  using its send method. Similarly, packets are received from the
 *  Rdt object using its receive method.
 *
 *  The thread is started using the start method (which calls the
 *  run method in a new thread of control). It can be stopped
 *  using the stop method; this causes the run method to terminate
 *  its main loop and print a short status report, then return.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SrcSnk implements Runnable {
	private Thread myThread;	// thread that executes run() method

	private long delta;		// time between packets in ns
	private long runLength;		// amount of time to run in ns
	private Rdt rdt;		// reference to Rdt object

	private int inCount = 0;	// count of received packets
	private int outCount = 0;	// count of sent packets
	private boolean quit;		// stop thread when true

	/** Initialize a new SrcSnk object
	 *  @param delta is a float, representing the amount of time to wait
	 *  between sending packets (in seconds)
	 *  @param runLength is a float, representing the the length of the time
	 *  interval during which packets should be sent
	 *  @param rdt is a reference to a Rdt object
	 */
	SrcSnk(double delta, double runLength, Rdt rdt) {
		this.delta = (long) (delta * 1000000000); // convert to ns
		this.runLength = (long) (runLength * 1000000000);
		this.rdt = rdt; this.quit = false;
	}

	/** Instantiate and start a thread to execute run(). */
	public void start() {
		myThread = new Thread(this); myThread.start();
	}

	/** Signal run method to halt. */
	public void stop() { quit = true; }

	/** wait for thread to terminate. */
	public void join() throws Exception { myThread.join(); }

	/** Run the SrcSnk thread.
	 *  This method executes a loop that generates new outgoing
	 *  payloads and receives incoming payloads. It sends packets
	 *  for a specified period of time and terminates after the stop
	 *  method is called.
	 */
	public void run() {
		long t0 = System.nanoTime();
		long now = 0;
		long next = 1000000000;
		long stopTime = next + runLength;

		int sleeptime; // time to sleep when nothing to do
		if (delta > 0 && delta < 1000000) sleeptime = (int) delta;
		else sleeptime = 999999;

		String msg; inCount = outCount = 0;
		int idleCount = 0;
		while (!quit) {
			now = System.nanoTime() - t0;
			if (rdt.incoming()) {
				msg = rdt.receive();
				if (!msg.equals("testing " + inCount)) {
                                        System.out.println("got: " + msg
						+ "when expecting "
                                                + "testing " + inCount);
                                        System.exit(1);
				}
                                inCount++;
				idleCount = 0;
			} else if (now > next && now < stopTime &&
			     	   rdt.ready() && delta > 0) {
				// send an outgoing payload
				msg = "testing " + outCount;
				rdt.send(msg);
				outCount++; next += delta;
				idleCount = 0;
			} else {
				idleCount++;
			}
			if (idleCount >= 10) {
				try {
					Thread.sleep(0L,sleeptime);
				} catch(Exception e) {
					System.err.println("SrcSnk:run: "
						+ "sleep exception " + e);
					System.exit(1);
				}
				idleCount = 0;
			}
		}
		System.out.println("  SrcSnk: sent " + outCount
					+ ", received " + inCount);
		System.out.println("          runLength "
					+ (((double) runLength)/1000000000));
	}
}

