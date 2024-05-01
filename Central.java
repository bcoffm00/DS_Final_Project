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

public class Central {
	
//Control access to Server List
public static Semaphore SL = new Semaphore(1);

//List of fitting room Servers
public static ArrayList<Connection> Servers = new ArrayList<Connection>();
	
public static ArrayList<Connection> getServers(){
	return Servers;
}

public static Semaphore getSL() {
	return SL;
}

	public static void main(String[] args) throws IOException {
		int port = 32005;
		
		

		ServerSocket controller = new ServerSocket(port);
		
		
		System.out.println("Server started");

	//Listens For Connections
		while (true) {
			try {

				Socket connection = controller.accept();

				Connection x = new Connection(connection,port);
				x.start();

			} catch (Exception e) {
				controller.close();
				e.printStackTrace();
			}
		}

	}

}

class Connection extends Thread {
	static int index = 0;
	
	String ClientIP = null;
	String ServerIP = null;
	int port;


	Socket s;
	Socket connection;
	private BufferedReader input = null;
	private PrintWriter out = null;
	
	public Socket getConnection() {
		return connection;
	}

	public void setConnection(Socket connection) {
		this.connection = connection;
	}

	private BufferedReader ControllerIn = null;
	private PrintWriter ControllerOut = null;
	
	public Connection(Socket connection, int port) {
		this.connection = connection;
		this.port = port;
	}
	
	//Checks if the connection is still alive
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
			if(line.contentEquals("Heartbeat")) {
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

	//Gets Fitting Room Server
	public Connection getServerConnection() {
		while(!Central.getSL().tryAcquire()) {
			
		}
		ArrayList<Connection> server = Central.getServers();
		Connection fittingroom = null;
		while(index < server.size() && server.get(index) == null ) {
			index++;
		}
		if(server.size() == 0) {
			return fittingroom;
		}
		fittingroom = server.get(index);
		Central.getSL().release();
		return fittingroom;
	}
	
	@Override
	public void run() {
	try {
		out = new PrintWriter(connection.getOutputStream());
		input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		System.out.println("Connected");
		
		String line  =	input.readLine();
		while(line == null) {
			line  =	input.readLine();
		}
		//Keeps Track Of Connections
		if(line.equalsIgnoreCase("server")) {
			ServerIP = connection.getInetAddress().getHostAddress();
			System.out.println("Connected to fitting room at: " + ServerIP);
		
			out.println("CONNECTED");
			out.flush();
			
			while(!Central.getSL().tryAcquire()) {
				
			}
			Central.getServers().add(this);
			
			Central.getSL().release();
			
			String hostaddress = ServerIP;
			int port = 32005;
			
			boolean running = isConnectionAlive(hostaddress,port);
			
			
			while(running) { 
				try {
					Thread.sleep(2000);
					running = isConnectionAlive(hostaddress,port);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			while(!Central.getSL().tryAcquire()) {
				
			}
			Central.getServers().remove(this);
			Central.getSL().release();
			
			System.out.println("Server at: " + ServerIP +" has disconnected");
			
		}else if(line.equalsIgnoreCase("client")) {
			try {
			ClientIP = connection.getInetAddress().getHostAddress();
			
			System.out.println("Connected to Client at: " + ClientIP);
			
			// Controller can now push messages
			
			Connection FittingRoom =  getServerConnection();
			if(FittingRoom == null) {
				out.println("No fitting room servers at this time.");
				out.flush();
				line = input.readLine();
				while(!line.equalsIgnoreCase("RECIEVED")){
					line = input.readLine();
				}
				connection.close();
				System.out.println("Client at: " + ClientIP + " has Disconnected.");
			}else {
			String hostaddress = FittingRoom.ServerIP;
			Socket s = new Socket(hostaddress, 32005);
			
			
			//Get input/output for fitting room server
			ControllerOut= new PrintWriter(s.getOutputStream());
			ControllerIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			
			ControllerOut.println("FITQUERY");
			ControllerOut.flush();
			
			
			while(line != null) {
				if(Central.getServers().contains(FittingRoom)) {
				System.out.println(213);	
				
				//Sends ready message to Server
				ControllerOut.println("ready");
				ControllerOut.flush();
				
				//Makes sure user gets prompted
				line = ControllerIn.readLine();
				System.out.println(line);
				
				while(line == null) {
				line = ControllerIn.readLine();
				}
				out.println(line);
				out.flush();
				
				//Pushes message from client to Fitting Room Server
				line = input.readLine();
				System.out.println(line);
				
				ControllerOut.println(line);
				ControllerOut.flush();
				
				/*
				//Sends ready message to Server
				ControllerOut.println("ready");
				ControllerOut.flush();
				
				//Sends Fitting Room Server message to Client
				line = ControllerIn.readLine();
				out.println(line);
				*/
				}else {
					out.println("Disconnected from server");
					FittingRoom = getServerConnection();
					ControllerOut.println("FITQUERY");
					ControllerOut.flush();
				}
				
			}
			System.out.println("Client at: " + ClientIP + " has Disconnected.");
			
			ControllerOut.close();
			ControllerIn.close();
			s.close();
			}
			}catch(Exception e) {
				
					if(input != null) {
						input.close();
					}
					if(out != null) {
						out.close();
					}
					
					if(ControllerOut != null) {
						ControllerOut.close();
					}
					if(ControllerIn != null) {
						ControllerIn.close();
					}
					if(s != null) {
						s.close();
					}
					
					System.out.println("Client at: " + ClientIP + " has Disconnected.");
				
			}
		}

			input.close();
			out.close();
			connection.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
}
