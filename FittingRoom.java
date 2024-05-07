
/********************************************************
 * Name:   		Brody Coffman, Tony Aldana and Yash Patel
 * Problem Set:	Final Group project
 * Due Date :	5/2/24
 *******************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * SemaphoreFR is a fitting room management system that utilizes Semaphores for
 * concurrency control. It includes a main server component to handle fitting
 * room operations and client connections. The system consists of fitting rooms
 * where clients can enter, wait, and exit as needed.
 */
public class FittingRoom {

	private static final Logger LOGGER = Logger.getLogger(FittingRoom.class.getName());
	static {
		setupLogger();
	}

	/**
	 * Configures the server's logging system. Logs are written to "FittingRoom.log"
	 * file.
	 */

	private static void setupLogger() {
		try {
			LogManager.getLogManager().reset();
			Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
			FileHandler fh = new FileHandler("FittingRoom.txt", true);
			fh.setFormatter(new SimpleFormatter());
			logger.addHandler(fh);
			logger.setLevel(Level.INFO);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error setting up logger", e);
		}
	}

	/** IP address of the central server */
	final static String CENTRAL_IP = "10.183.244.42";

	/** Port number of the central server */
	final static int CENTRAL_PORT = 32005;

	public static boolean isConnectionAlive(String hostname, int port) {
		boolean alive = false;
		SocketAddress address = new InetSocketAddress(hostname, port);
		Socket socket = new Socket();
		int timeout = 5000;
		PrintWriter out;
		BufferedReader input;
		try {
			socket.connect(address, timeout);
			out = new PrintWriter(socket.getOutputStream());
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.println("Heartbeat");
			out.flush();
			String line = input.readLine();
			while (!line.equalsIgnoreCase("HEARTBEAT")) {
				line = input.readLine();
			}

			alive = true;
			return alive;

		} catch (SocketTimeoutException e) {
			return alive;
		} catch (IOException e) {
			return alive;
		}
	}

	/**
	 * The main method initializes the fitting room system, connects to the central
	 * server, and starts the server socket to listen for incoming client
	 * connections.
	 *
	 * @param args Command-line arguments specifying the number of fitting rooms
	 */
	public static void main(String[] args) {
		// Sends the message "SERVER" to the central server to indicate this is a
		// fitting room
		Socket centralServer = null;
		PrintWriter toConnect = null;
		try {
			centralServer = new Socket(CENTRAL_IP, CENTRAL_PORT);
			toConnect = new PrintWriter(centralServer.getOutputStream());

			toConnect.println("SERVER," + args[0]);
			toConnect.flush();

		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Error connecting to central server", ex);
		}
		ServerSocket serverSock;
		try {
			// Started the server socket
			serverSock = new ServerSocket(CENTRAL_PORT);
			FitRoom FittingRoom = new FitRoom(Integer.parseInt(args[0]));

			boolean alive = isConnectionAlive(CENTRAL_IP, CENTRAL_PORT);
			// Main loop to rip threads
			int a = 1;
			while (alive) {
				alive = isConnectionAlive(CENTRAL_IP, CENTRAL_PORT);
				Socket newCon = serverSock.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(newCon.getInputStream()));
				String line = br.readLine();

				// If its a heartbeat socket
				if (line.equalsIgnoreCase("HEARTBEAT")) {
					// CHECK THE TWO SEMA's AND PASS BACK DATA
					PrintWriter pr = new PrintWriter(newCon.getOutputStream());
					pr.println(bloodTest(FittingRoom));
					pr.flush();

					try {
						br.close();
						pr.close();
						newCon.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else if (line.equalsIgnoreCase("fitquery")) {
					fitConnection curConnection = new fitConnection(newCon, FittingRoom, a);
					a++;
					curConnection.start();
				}

			}

			// END OF TRY
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Exception occurred", ex);
		}

		// END OF MAIN
	}

	/**
	 * Sends the current state of the fitting rooms to the requesting client.
	 *
	 * @param a The FitRoom instance
	 * @return A string representing the state of the fitting rooms
	 */
	private static String bloodTest(FitRoom a) {
		return a.bloodTest();
	}

	/**
	 * Sends a message to the central server indicating that this is a fitting room.
	 */
	private static void initConnect(Socket a) {
		try {
			a = new Socket(CENTRAL_IP, CENTRAL_PORT);
			PrintWriter toConnect = new PrintWriter(a.getOutputStream());

			toConnect.println("SERVER");
			toConnect.flush();

		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "Error connecting to central server", ex);
		}
	}

	/**
	 * Represents a client connection thread to handle communication with the
	 * central server.
	 */
	@SuppressWarnings("unused")
	private static class fitConnection extends Thread {
		private int threadNum;
		private FitRoom par = null;
		private Socket connection = null;
		private PrintWriter toConnect = null;
		private BufferedReader fromConnect = null;

		/**
		 * Constructs a fitConnection object with the given socket and FitRoom instance.
		 *
		 * @param newSock The client socket
		 * @param b       The FitRoom instance
		 */
		public fitConnection(Socket newSock, FitRoom b, int a) {
			this.threadNum = a;
			this.par = b;

			try {
				this.connection = newSock;

				this.fromConnect = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
				this.toConnect = new PrintWriter(this.connection.getOutputStream());
			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Error initializing connection", ex);
			}
		}

		/**
		 * Sends a message to the central server.
		 *
		 * @param message The message to send
		 * @return True if the message is successfully sent, otherwise false
		 */
		public boolean sendMessage(String message) {
			// Messaging handler for modularization
			try {
				LOGGER.info("Attempting to send message, [" + message + "] to central server");
				System.out.println("Attempting to send message, [" + message + "] to central server");
				// Attempts to send a message via the print writer

				this.toConnect.println(message.toUpperCase());
				this.toConnect.flush();

				LOGGER.info("Successfully sent message, [" + message + "] to central server");
				System.out.println("Successfully sent message, [" + message + "] to central server");
				// If message sends successfully then return true
				return true;
			} catch (Exception e) {
				// If message fails to send then return false
				LOGGER.info("Failed to send message, [" + message + "] to central server");
				System.out.println("Failed to send message, [" + message + "] to central server");
				return false;
			}
		}

		/**
		 * Reads a message from the central server.
		 *
		 * @return The message read from the server
		 */
		public String readMessage() {
			String message = "";
			// Message receiver handler for modularization
			try {
				LOGGER.info("Reading message from central server");
				System.out.println("Reading message from central server");
				// attempts to read in message via bufferedReader

				message = this.fromConnect.readLine();

				// If message is read successfully then return message
				LOGGER.info("Received message from central server: " + message);
				System.out.println("Received message from central server: " + message);
				return message;
			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Error reading message from central server", ex);
				System.out.println("Reading message from central server");
				// If message fails to be read then return empty string
				return message;
			}
		}

		/**
		 * Starts the thread's execution.
		 */
		public void run() {
			try {
				String message = "";
				String response = "";
				int task = 1;
				while (task < 9) {
					switch (task) {
					case 0:
						System.out.println("Sequencing error");
						task = 1000;
						break;
					case 1:
						message = this.readMessage();
						System.out.println("Thread " + this.threadNum + " has entered fitting room server");
						response = this.par.enter(this, true);
						System.out.println("Thread " + this.threadNum + " response is " + response);
						this.sendMessage(response);
						task++;
						break;

					case 2:
						if (response.equalsIgnoreCase("ENTERED")) {
							System.out.println("\t\tThread " + this.threadNum + " has entered the changing room");
							task = 4;
							break;
						} else {
							System.out.println("\tThread " + this.threadNum + " has entered the waiting room");
							task = 3;
							break;
						}


					case 3:
						while (!this.par.compare(this)) {}
						//Checking for this to be the head of the waiting room
						while (this.par.changeCheck()==0) {}
						//Checking for an open space
						while (response.equals("WAITING")) {
							response = this.par.enter(this, false);
						}
						System.out.println("\t\tThread " + this.threadNum + " has entered the changing room");
						//Now that we have successfully entered the changing room send the message
						this.sendMessage(response);
						task++;
						break;

					case 4:
						message = this.readMessage();
						//reads in the exit message
						task++;
						break;

					case 5:
						response = this.par.exit();
						System.out.println("\t\t\tThread " + this.threadNum + " has exited the changing room");
						//releases a changing room permit
						this.sendMessage(response);
						//sends received
						task++;
						break;

					case 6:
						System.out.println("\t\t\t\tClosing Thread " + this.threadNum +  " connections");
						try {
							if (this.toConnect != null) {
								this.toConnect.close();
							}
							if (this.fromConnect != null) {
								try {
									this.fromConnect.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if (this.connection != null) {
								try {
									this.connection.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						task = 1000;
						break;
					}
				}

			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Exception occurred", ex);
			}
		}

	}

	/**
	 * Represents a semaphore-based change room.
	 */
	private static class ChangeRoom {
		private int numRooms;
		private Semaphore fitRooms = null;

		/**
		 * Constructs a ChangeRoom object with the given number of rooms.
		 *
		 * @param a The number of rooms
		 */
		public ChangeRoom(int a) {
			this.numRooms = a;
			this.fitRooms = new Semaphore(a);
		}

		/**
		 * Attempts to acquire a permit from the changing room
		 *
		 * @return A boolean containing whether a permit was acquired
		 */
		public boolean tryAcquire() {
			return this.fitRooms.tryAcquire();
		}

		/**
		 * Releases a permit from the changing room
		 */
		public void release() {
			this.fitRooms.release();
		}

		/**
		 * Checks the number of permits in the changing room
		 *
		 * @return A int containing the number of open slots in the changing room
		 */
		public int stateCheck() {
			return this.fitRooms.availablePermits();
		}
	}

	/**
	 * Represents a waiting room queue for clients waiting to enter the fitting
	 * rooms.
	 */
	private static class WaitRoom {

		/**
		 * An integer representing the number of rooms
		 */
		private int numRooms;

		/**
		 * A basic lock for accessing the linked list sequentially to prevent issues
		 */
		private Semaphore access = new Semaphore(1);

		/**
		 * A linked list representing all connections in the waiting room
		 */
		private LinkedList<fitConnection> waitRooms = new LinkedList<>();

		/**
		 * Constructs a WaitRoom object with the given number of rooms.
		 *
		 * @param a The number of rooms
		 */
		public WaitRoom(int a) {
			this.numRooms = a;
		}

		/**
		 * A simple method to check if a connection is inside the linked list, does
		 * implement a lock in order to run sequentially and prevent race conditions
		 *
		 * @param a the connection to check for in the list
		 * @return contains, a boolean representing if it is in the list or not
		 */
		public boolean contains(fitConnection a) {
			while (!access.tryAcquire()) {
			}
			boolean contains = this.waitRooms.contains(a);
			access.release();
			return contains;
		}

		/**
		 * Adds a connection a to the linked list, works sequentially via a lock
		 *
		 * @param a, the connection to be added
		 * @return a boolean representing if the action succeeded or not
		 */
		public boolean enQueue(fitConnection a) {
			while (!access.tryAcquire()) {
			}
			if (this.waitRooms.size() < this.numRooms) {
				this.waitRooms.add(a);
				access.release();
				return true;
			}
			access.release();
			return false;
		}

		/**
		 * returns the connection at the top of the linked list
		 *
		 * @return a, the connection at the head of the linked list
		 */
		public fitConnection peek() {
			while (!access.tryAcquire()) {
			}
			fitConnection a = this.waitRooms.peek();
			access.release();
			return a;
		}

		/**
		 * removes and returns the head of the linked list
		 *
		 * @return the head connection
		 */
		public fitConnection deQueue() {
			while (!access.tryAcquire()) {
			}
			if (this.waitRooms.isEmpty()) {
				access.release();
				return null;
			} else {
				fitConnection a = this.waitRooms.remove();
				access.release();
				return a;
			}
		}

		/**
		 * Returns the state info the linked list
		 *
		 * @return a, the state of the semaphore
		 */
		public int stateCheck() {
			while (!access.tryAcquire()) {
			}
			int a = this.numRooms - this.waitRooms.size();
			access.release();
			return a;
		}

	}

	/**
	 * Represents a fitting room containing a waiting room and a change room.
	 */
	private static class FitRoom {
		private Semaphore lock = new Semaphore(1);
		private WaitRoom WaitingRooms = null;
		private ChangeRoom ChangingRooms = null;

		/**
		 * Constructs a FitRoom object with the given number of fitting rooms.
		 *
		 * @param a The number of fitting rooms
		 */
		public FitRoom(int a) {
			this.WaitingRooms = new WaitRoom(a * 2);
			this.ChangingRooms = new ChangeRoom(a);
		}

		public int changeCheck () {
			int a = 0;
			while (!lock.tryAcquire()) {}
			a = this.ChangingRooms.stateCheck();
			lock.release();
			return a;
		}

		public String bloodTest() {
			return this.WaitingRooms.stateCheck() + "," + this.ChangingRooms.stateCheck();
		}

		public String enter(fitConnection a, boolean b) {
			String msg = "";
			while (!lock.tryAcquire()) {}

			if (this.ChangingRooms.tryAcquire()) {
				//if this enters this if statement there was space in the changing room and a permit has been acquired
				msg = "ENTERED";
				//if !b then this enter is being called for the first time so the waiting room does not need to be deQueued
				if (!b) {
					this.WaitingRooms.deQueue();
				}
			} else {
				//if this enters this else statement there is no space in the changing room
				msg = "WAITING";
				//add this fitconnection to the waiting list if its not already there
				if (this.WaitingRooms.contains(a)) {
					this.WaitingRooms.enQueue(a);
				}

			}

			lock.release();
			return msg;
		}

		public boolean compare(fitConnection a) {
			while (!lock.tryAcquire()) {}

			if (this.WaitingRooms.peek().equals(a)) {
				lock.release();
				return true;
			} else {
				lock.release();
				return false;
			}
		}

		public String exit() {
			while (!lock.tryAcquire()) {}
			this.ChangingRooms.release();
			lock.release();
			return "RECEIVED";
		}
	}
}
