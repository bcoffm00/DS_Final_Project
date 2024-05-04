import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Central{
	private static Semaphore access = new Semaphore(1);
	private static ArrayList<Connection> ServerList = new ArrayList<Connection>();

	public static Semaphore getAccess() {
		return access;
	}

	public static ArrayList<Connection> getServerList() {
		return ServerList;
	}
	
	public static boolean checkList(Connection a) {
		while(access.tryAcquire() != true) {
			
		}
		boolean contained = ServerList.contains(a);
		access.release();
		return contained;
	}

	public static void main(String args[]) {
		int port = 32005;
		int n = 0;
		
		try {
		//Start Server
		ServerSocket server = new ServerSocket(port);
		System.out.println("Server started");
		while(true) {

			Socket connection = server.accept();
			
			Connection x = new Connection(connection);
			
			x.start();
			
			
			}
		}catch (IOException e) {
		
			e.printStackTrace();
		}

		
		
		
	}
	
	
	
	
	/**
	 * Manage Connections for the Central Server
	 * @author tonya
	 *
	 */
	// Connection Class
	static class Connection extends Thread{
		String type;
		String ip;
		int ServerID = 0;
		int ClientID = 0;
		String name;
		Socket connection;
		
		BufferedReader connectionIn;
		PrintWriter connectionOut;
		int wrooms;
		int frooms;
		
		/**
		 * Constructor for Connection class
		 * Passes in the socket of the machine
		 * @param connection
		 */
		public Connection(Socket connection) {
			this.connection = connection;
			ip = connection.getInetAddress().getHostAddress();
			
		}
		
		/**
		 * Adds Server to the List of servers
		 * If it is disconnected it removes from
		 * the list of servers
		 */
		//If type is server this is called in the run method
		public void Server() {
			System.out.println("Connected to Server " + ServerID + " At " + ip);
			name = type+"-"+ServerID+"-"+ip;
			
			while(Central.getAccess().tryAcquire() != true) {
				
			}
			
			Central.getServerList().add(this);
			Central.getAccess().release();
			
			boolean running = isConnectionAlive(ip,32005);


			while(running) {
				try {
					Thread.sleep(2000);
					running = isConnectionAlive(ip,32005);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			while(!Central.getAccess().tryAcquire()) {

			}
			Central.getServerList().remove(this);
			Central.getAccess().release();

			System.out.println("Server at: " + ip+" has disconnected");
			
		}
		
		/**
		 * Manages the messages between a client and server
		 * Reroutes clients if Server goes down
		 * @throws IOException 
		 * 
		 */
		//If type is client this is called in the run method
		public void Client(){
			BufferedReader ServerIn = null;
			PrintWriter ServerOut = null;
			System.out.println("Connected to Client " + ClientID + " At " + ip);
			name = type+"-"+ClientID+"-"+ip;
					int task = 0;
					boolean clientTurn = false;
					boolean serverTurn = false;
					String line = "";
					Connection Server = null;
					FIRST:
					while(task < 9) {
					try {
						if(clientTurn) {
							line = connectionIn.readLine();	
							
						}else if(serverTurn) {
							line = ServerIn.readLine();
						}
						
						switch(task){
							// Tries to connect to FittingRoom
						case 0:
							Server = getServerConnection();
							
							if(Server == null) {
								System.out.println("SENDING NOTCONNECTED TO: " + name);
								connectionOut.println("NOTCONNECTED");
								connectionOut.flush();
								
								return;
								
							}else {
								connectionOut.println("CONNECTED," + Server.ip);
								connectionOut.flush();
								
									clientTurn = true;
									serverTurn = false;
									
									task = 1;
									line = "";
									Socket s = new Socket(Server.ip,32005);
									
									ServerOut = new PrintWriter(s.getOutputStream());
									ServerIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
									
									System.out.println("Sending [fitquery] TO: " + Server.name);
									ServerOut.println("fitquery");
									ServerOut.flush();
									break;
							}
						case 1:
							// Message Sent to Server from Client
							System.out.println(name + " Sending: " + line + " to " + Server.name + "\n");
							
									ServerOut.println(line);
									ServerOut.flush();
									serverTurn = true;
									clientTurn = false;
									task = 2;
									break;
								
						case 2:// Message sent to client from Server
							System.out.println(Server.name + " Sending: " + line + " to " + name + "\n");
							if(line.equalsIgnoreCase("ENTERED")) {
								connectionOut.println(line + "," + Server.ip);
								connectionOut.flush();
								clientTurn = true;
								serverTurn = false;
								task = 1;
								break;
							}
							if(line.equalsIgnoreCase("WAITING")) {
								connectionOut.println(line + "," + Server.ip);
								connectionOut.flush();
								clientTurn = false;
								serverTurn = true;
								task = 2;
								break;
								}
							if(line.equalsIgnoreCase("RECEIVED")) {
								connectionOut.println(line + "," + Server.ip);
								connectionOut.flush();
								clientTurn = false;
								serverTurn = false;
								return;
							}
							
							}
						}catch(Exception e) {
							System.out.println("An error Occured");
								task = 0;
								break FIRST;
							} 
						}
				
					}
		
		/**
		 * Checks if the string x can be parsed as an Integer
		 * @param x String
		 * @return true if x can be parsed false otherwise
		 */
		//Checks if a number is an int
		public boolean isInteger(String x) {
			try {
				Integer.parseInt(x);
				return true;
			}catch (NumberFormatException i) {
				return false;
			}
		}
		
		//Checks if connection is alive
		/**
		 * Checks if a machine of a particular socket is alive by
		 * periodically sending messages
		 * This also sets the amount of waiting rooms and fitting rooms
		 * are available at the moment
		 * @param hostname ip address of the machine
		 * @param port the port the machine is listening to
		 * @return true if it is still alive false otherwise
		 */
		public boolean isConnectionAlive(String hostname, int port) {
			boolean alive = false;
			SocketAddress address = new  InetSocketAddress(hostname,port);
			Socket socket = new Socket();
			int timeout = 5000;
			PrintWriter out;
			BufferedReader input;
			try {
				socket.connect(address,timeout);
				out =  new PrintWriter(socket.getOutputStream());
				input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out.println("Heartbeat");
				out.flush();
				String line = input.readLine();

				String [] rooms = line.split(",");
				if(isInteger(rooms[0]) && isInteger(rooms[1])) {

					wrooms = Integer.parseInt(rooms[0]);
					frooms= Integer.parseInt(rooms[1]);
					socket.close();
					alive = true;
				}

			}catch(SocketTimeoutException e) {
				return alive;
			} catch (IOException e) {
				return alive;
			}
			return alive;
		}
		
		/**
		 * This goes through the list of servers and return a Connection with
		 * the first available waiting room or fitting room spot.
		 * @return Returns a connection
		 */
		// Gets server by using round robin
		public Connection getServerConnection() {
			
				while(Central.getAccess().tryAcquire() != true) {

				}

				ArrayList<Connection> server = Central.getServerList();
				Connection fittingroom = null;

				if(server.size() == 0 ) {
					Central.getAccess().release();
					return null;
				}else {
				for(int i = 0; i < server.size(); i ++) {
					fittingroom = server.get(i);
					if(fittingroom.frooms != 0) {
						Central.getAccess().release();
						return fittingroom;
					}else if (fittingroom.wrooms != 0) {
						Central.getAccess().release();
						return fittingroom;
					}else {
						fittingroom = null;
					}
				}


				Central.getAccess().release();

				return fittingroom;
				}
			}
		
		@Override
		public void run() {
			
			try {
				connectionIn = new BufferedReader (new InputStreamReader(connection.getInputStream()));
				connectionOut = new PrintWriter(connection.getOutputStream());
				String line = connectionIn.readLine();
				if(line.equalsIgnoreCase("SERVER")) {
					ServerID++;
					type = "SERVER";
					Server();
				}if(line.equalsIgnoreCase("CLIENT")){
					ClientID++;
					type = "Client";
					Client();
				}else {
					connection.close();
				}
				
				System.out.println("Closing Connection at " + name);
				connection.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		
		
		
		
		
		
		
		
	}
	
}
