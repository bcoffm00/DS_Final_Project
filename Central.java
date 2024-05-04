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


	public static void main(String args[]) {
		int port = 32005;
		int n = 0;
		
		//Start Server
		while(true) {
		try {
			ServerSocket server = new ServerSocket(port);
			System.out.println("Server started");
			
			Socket connection = server.accept();
			
			Connection x = new Connection(connection);
		
			
			
		} catch (IOException e) {
		
			e.printStackTrace();
		}

		}
		
		
	}
	
	
	
	
	/**
	 * Manage Connections for the Central Server
	 * @author tonya
	 *
	 */
	// Connection Class
	static class Connection implements Runnable{
		String type;
		String ip;
		int id = 0;
		String name;
		Socket connection;
		
		BufferedReader connectionIn;
		PrintWriter connectionOut;
		int wrooms;
		int frooms;
		
		public Connection(Socket connection) {
			this.connection = connection;
			ip = connection.getInetAddress().getHostAddress();
			id++;
		}
		
		
		//If type is server
		public void Server() {
			System.out.println("Connected to Server " + id + " At " + ip);
			name = type+"-"+id+"-"+ip;
			
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
		
		//If type is client
		public void Client() {
			BufferedReader ServerIn = null;
			PrintWriter ServerOut = null;
			name = type+"-"+id+"-"+ip;
			
			Connection Server = getServerConnection();
			
			if(Server == null) {
				connectionOut.println("NOT CONNECTED");
				try {
					connection.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}else {
				connectionOut.println("CONNECTED," + Server.ip);
			
				 try {
					boolean clientTurn = true;
					boolean serverTurn = false;
					
					int task = 1;
					String line = "";
					ServerOut = new PrintWriter(Server.connection.getOutputStream());
					ServerIn = new BufferedReader(new InputStreamReader(Server.connection.getInputStream()));
					while(task < 9) {
						if(!Central.getServerList().contains(Server)) {
							connectionOut.println("DISCONNECT");
							Server = getServerConnection();
							
							if(Server == null) {
								connectionOut.println("NOT CONNECTED");
								try {
									connection.close();
									return;
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							
							}else {
								ServerOut = new PrintWriter(Server.connection.getOutputStream());
								ServerIn = new BufferedReader(new InputStreamReader(Server.connection.getInputStream()));
								task = 1;
							}
							
							
							
						}else if(clientTurn) {
							line = connectionIn.readLine();	
							
						}else if(serverTurn) {
							line = ServerIn.readLine();
						}
						
						switch(task){
						
						case 1:
							// Message Sent to Server from Client
							System.out.println(name + " Sending: " + line + " to " + Server.name + "\n");
							
									ServerOut.println(line);
									serverTurn = true;
									clientTurn = false;
									task = 2;
									break;
								
						case 2:// Message sent to client from Server
							System.out.println(Server.name + " Sending: " + line + " to " + name + "\n");
							if(line.equalsIgnoreCase("ENTERED")) {
								connectionOut.println(line + "," + Server.ip);
								clientTurn = true;
								serverTurn = false;
								task = 1;
							}
							if(line.equalsIgnoreCase("WAITING")) {
								connectionOut.println(line + "," + Server.ip);
								clientTurn = false;
								serverTurn = true;
								task = 2;
							}
						}
					}
				} catch (SocketException e) {
					try {
						connection.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			
		}
		
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
		public boolean isConnectionAlive(String hostname, int port) {
			boolean alive = false;
			SocketAddress address = new  InetSocketAddress(hostname,port);
			Socket socket = new Socket();
			int timeout = 2000;
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
		
		public Connection getServerConnection() {
			System.out.println(153);
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
					type = "SERVER";
					Server();
				}if(line.equalsIgnoreCase("CLIENT")){
					type = "Client";
					Client();
				}else {
					connection.close();
				}
				
				connection.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		
		
		
		
		
		
		
		
	}
	
}
