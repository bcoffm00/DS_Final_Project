import java.io.*;
import java.net.Socket;
import static java.lang.System.exit;

/**
 * Main class for the entire file, contains all needed classes and code for the customer
 */
public class Client {
	/**
	 * Main method, generates N number of threads and starts them followed by a 0 -> 1 second delay
	 * @param args contains the IP, PORT, and N number of threads
	 */
	public static void main(String[] args) {

		for (int i = 0; i < Integer.parseInt(args[2]); i++) {
			//Creates N number of threads as specified by the third program argument
			System.out.println("Client thread "  + i + " created");
			CustomerThread newThread = new CustomerThread(args, i);
			newThread.start();
			try {
				Thread.sleep((long)(Math.random() * 1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
				exit(1);
			}
		}
	}
	
	/**
	 * This is the thread class ripped in the main method, it contains the run method and constructor
	 * as well as any needed code for the start method in the thread class
	 */
	public static class CustomerThread extends Thread {
		/**
		 * Contains the ip, args[0], and port, args[1], for the central server socket
		 */
		private String[] args;
		
		/**
		 * Contains the thread number (essentially the name) for this specific thread
		 */
		private int threadNum;
		
		/**
		 * Contains a reference to the central server socket which all communication is passed through
		 */
		private Socket centralServer;
		
		/**
		 * Print writer for the central server socket
		 */
		private PrintWriter pw = null;
		
		/**
		 * Buffer reader for the central server socket
		 */
		private BufferedReader br = null;
		
		/**
		 * This is the constructor for the "CustomerThread" class, It takes in the program arguments as well as an integer representing the
		 * thread name and uses them to set the appropriate class variables. It will then attempt to bind the socket for the central server
		 * as well as create the print writer and buffer reader objects
		 * @param args contains the port and ip of the central server
		 * @param a contains the thread number that is used as the name
		 */
		public CustomerThread (String[] args, int a) {
			this.args = args;
			this.threadNum = a;
			//Set basic class variables for use later
			System.out.println("\tAttempting to connect client " + this.threadNum + " to central server");
			//Debugging
			try {
				this.centralServer = new Socket(args[0], Integer.parseInt(args[1]));
				this.pw = new PrintWriter(this.centralServer.getOutputStream());
				this.br = new BufferedReader(new InputStreamReader(this.centralServer.getInputStream()));
				//Binds a socket for the central server and sets up an input and output stream reader/writer for it
			} catch (Exception e) {
				e.printStackTrace();
				exit(1);
			}
			System.out.println("\tClient " + this.threadNum + " is connected to central server");
			//Debugging
		}
		
		/**
		 * This is an overloaded run method which will be called when the thread is ripped
		 * It consists mostly of a single run-time loop which contains a switch-case allowing for the disconnect 
		 * message to be checked during each iteration over the loop, if this is tripped then task (switch variable)
		 * will be reset to 1 (first case in the switch-case) allowing for the linear handshake path to restart using the 
		 * central server which automatically will try to reconnect to a new fitting room and send a message based on success
		 * which will then be handled according to the PDF by the client thread. After this point a message should be sent to the 
		 * fitting room which contains "ENTER" which will attempt to enter the thread into the fitting room or resend according to the answer
		 * response. following this sections success the thread will wait from 0 -> 1 second and then send a exit message and begin closing connections
		 */
		public void run() {
			String cliName = "Client " + this.threadNum;
			try {
				
				int task = 1;
				String message = "";
				while (task < 9) {
					if (task > 1) {
						message = br.readLine();
						System.out.println(message);
						//Attempts to read in a message if on any case other than 1
						//Prints message for debugging purposes
					}
					if ((message).equalsIgnoreCase("disconnect")) {
						task = 1;
						//Resets the case to 1 if a message is received that the fitting room went down
						//allowing the code to re-walk the cases
					} else {
						switch (task) {
							case 1:
								this.pw.println("client");this.pw.flush();
								System.out.println("\t\t" + cliName + " sending \"client\" message to central server");
								task++;
								break;
								//Declares that we are a client to the central server
							case 2: 
								if (message.contains("Connected to Fitting Room Server")) {
									System.out.println("\t\t\t" + cliName + " has connected to the fitting room");
									task++;
									pw.println("ENTER");pw.flush();
									break;
								} else if (message.contains("No fitting room server")) {
									System.out.println("\t\t\t" + cliName + ", no fitting rooms available, killing client");
									task = 10000;
									this.pw.println("RECEIVED");
									break;
								} else {
									System.out.println("\t\t\t" + cliName + ", Unknown input, reverting");
									task--;
									break;
								}
								//Handles the expected responses for all options of the fitting room status handshake
							case 3: 
								if (message.contains("Client has entered room")) {
									System.out.println("\t\t\t\t" + cliName + " has entered a changing room");
									task++;
									//Client is in changing room, move on to sleep for x amount
									break;
								} else if (message.contains("All rooms are occupied")){
									System.out.println("\t\t\t" + cliName + " has entered a waiting room, reverting");
									task--;
									//Client is in waiting room, re-send enter message until in changing room message received
									break;
								} else if (message.contains("Both fitting rooms and waiting room are full")){
									task = 10000;
									System.out.println("\t\t\t" + cliName + " all fitting and waiting options are full, disconecting");
									//All fitting room and waiting rooms are full disconnecting
									break;
								} else {
									task--;
									//Unknown or null input, just retry
									break;
								}
								//Handles the options for the changing room - waiting room handshake
							case 4:
								//System.out.println("here");
								Thread.sleep((long)(Math.random() * 1000));
								//System.out.println("there");
								pw.println("EXIT");pw.flush();
								//Sends the leave message to the central server
								task = 10000;
								//Ensures the code will not loop again
								System.out.println("\t\t\t\t" + cliName + " has finished executing, closing connections");
								break;
						}
					}
				}								
			} catch (Exception ex) {
				ex.printStackTrace();
				exit(1);
			}
			try {
				if (this.pw != null) {
					this.pw.close();
				} if (this.br != null) {
					this.br.close();
				} if (this.centralServer != null) {
					this.centralServer.close();
				}
				//Ensures that all connections are closed
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			
		}


	}
}
