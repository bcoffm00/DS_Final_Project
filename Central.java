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

public static int Id = 0;



//List of fitting room Servers
public static ArrayList<Connection> Servers = new ArrayList<Connection>();

public static ArrayList<Connection> getServers(){
	return Servers;
}

public static Semaphore getSL() {
	return SL;
}

public static int getId() {
	return Id;
}

public static void setId(int x) {
	Id = x;
}

	public static void main(String[] args) throws IOException {
		int port = 32005;



		ServerSocket controller = new ServerSocket(port);


		System.out.println("Server started");

	//Listens For Connections

		while (true) {
			try {

				Socket connection = controller.accept();

				Connection x = new Connection(connection,port,Id);
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
	int id;
	String name;
	int wrooms;
	int frooms;

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

	public Connection(Socket connection, int port, int Id) {
		this.connection = connection;
		this.port = port;
		this.id = Id;

		Central.setId(Central.getId() + 1);
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

	//Gets Fitting Room Server
	public Connection getServerConnection() {
	System.out.println(153);
		while(Central.SL.tryAcquire() != true) {

		}

		ArrayList<Connection> server = Central.getServers();
		Connection fittingroom = null;

		if(server.size() == 0 ) {
			Central.SL.release();
			return null;
		}else {
		for(int i = 0; i < server.size(); i ++) {
			fittingroom = server.get(index);
			if(fittingroom.frooms != 0) {
				Central.SL.release();
				return fittingroom;
			}else if (fittingroom.wrooms != 0) {
				Central.SL.release();
				return fittingroom;
			}
		}


		Central.SL.release();

		return fittingroom;
		}
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

			name = "Server " + id + " at "+ ServerIP;
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

			System.out.println(name +" has disconnected");

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
				while(!line.equalsIgnoreCase("RECEIVED")){
					line = input.readLine();
				}
				System.out.println(name + " At 223 " + line);
				connection.close();
				System.out.println("Client at: " + ClientIP + " has Disconnected.");
			}else {
			String hostaddress = FittingRoom.ServerIP;
			Socket s = new Socket(hostaddress, 32005);

			String ServerName = FittingRoom.name;
			name = "Client " + id + " at "+ ClientIP;;

			//Get input/output for fitting room server
			ControllerOut= new PrintWriter(s.getOutputStream());
			ControllerIn = new BufferedReader(new InputStreamReader(s.getInputStream()));


			ControllerOut.println("FITQUERY");
			ControllerOut.flush();
			int task = 1;

			System.out.println("234");
			while(task < 6) {
				if(!Central.getServers().contains(FittingRoom)) {
				System.out.println("237");



				}else {
					switch(task) {
					case 1:
						//Reads from Server
						System.out.println(name + " At case 1");
						line = ControllerIn.readLine();
						while(line == null) {
							line = ControllerIn.readLine();
						}
						System.out.println("From: " + ServerName + ":" + line + "\n");
						task++;
						break;

					case 2:
						//Sends the client message from Server
						System.out.println(name + " At case 2");

						System.out.println("From: " + ServerName + ":" + line + "To: " + name + "\n");
						if(line.contains("Connected to Fitting Room Server. Type 'ENTER' to enter a room, 'EXIT' to leave a room or 'OVER' to disconnect.")) {
							out.println(line);
							out.flush();
							task = 3;
						} else if(line.contains("has entered room")) {

							out.println(line);
							out.flush();
							task  = 3;
							break;
						}else if(line.equalsIgnoreCase("Both fitting rooms and waiting room are full. Please try again later.")){
							out.println(line);
							out.flush();
							while(Central.getSL().tryAcquire() != true) {

							}
							index++;
							Central.getSL().release();

							task = 5;
							break;
						}else if(line.equalsIgnoreCase("You have exited the room.")){
							out.println(line);
							out.flush();
							task = 6;
							break;
						}else {
						out.println(line);
						out.flush();
						task = 1;
						break;
						}

					case 3:
						//Reads from Client
						System.out.println(name + " At case 3\n");
						line = input.readLine();

						task = 4;
						break;

					case 4:
						//Sends Message to Server From Client
						System.out.println(name + " At case 4");
						System.out.println("From: " + name + ":" + line + " To: " + ServerName + "\n");

						ControllerOut.println(line);
						ControllerOut.flush();
						task = 1;
						break;


					case 5:
						//Fitting Room Server is full
						System.out.println(name + " At case 5\n");

						s.close();
						FittingRoom =  getServerConnection();
						if(FittingRoom == null) {
							out.println("No fitting room servers at this time.");
							out.flush();
							line = input.readLine();
							while(!line.equalsIgnoreCase("RECIEVED")){
								line = input.readLine();
							}

							task = 6;
							break;
					}else {
						hostaddress = FittingRoom.ServerIP;
						s = new Socket(hostaddress, 32005);
						ServerName = FittingRoom.name;

						ControllerOut= new PrintWriter(s.getOutputStream());
						ControllerIn = new BufferedReader(new InputStreamReader(s.getInputStream()));


						ControllerOut.println("FITQUERY");
						ControllerOut.flush();

						task = 1;
					}

				}

				}
			}

			ControllerOut.close();
			ControllerIn.close();
			s.close();

			input.close();
			out.close();
			connection.close();
			System.out.println(name + " has Disconnected.");
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

					System.out.println("Client at: " + name + " has Disconnected.");

			}
		}

	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
}
