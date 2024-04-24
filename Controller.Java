import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.io.DataOutputStream;

public class Controller {
public static ArrayList<Socket> Servers = new ArrayList<Socket>();
	
public static ArrayList<Socket> getServers(){
	return Servers;
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
	String ClientIP = null;
	String ServerIP = null;
	int port;
	boolean client = false;

	
	Socket connection;
	private DataInputStream input = null;
	private DataOutputStream out = null;
	
	int waitingrooms;
	int fittingrooms;
	
	private DataInputStream ControllerIn = null;
	private DataOutputStream ControllerOut = null;
	
	public Connection(Socket connection, int port) {
		this.connection = connection;
		this.port = port;
	}
	
	// Message Method
	public void message(String request, DataInputStream Cinput, DataOutputStream Cout,  DataInputStream Sinput, DataOutputStream Sout) {
		
	}

	@Override
	public void run() {
	try {
		input = new DataInputStream(connection.getInputStream());
		out = new DataOutputStream(connection.getOutputStream());
		String line  =	input.readUTF();
		
		//Keeps Track Of Connections
		if(line.contentEquals("Server")) {
			Controller.getServers().add(connection);
			System.out.println("Connected to fitting room at:" + connection.getInetAddress().getHostAddress());
			
		
			out.writeUTF("ready");
			fittingrooms = Integer.parseInt(input.readUTF());
			System.out.println("Server at: " + connection.getInetAddress().getHostAddress() +" has " + fittingrooms +" fitting rooms");
			out.writeUTF("ready");
			waitingrooms = Integer.parseInt(input.readUTF());
			
			ServerIP = connection.getInetAddress().getHostAddress();
			System.out.println("Server at: " + connection.getInetAddress().getHostAddress() +" has " + waitingrooms +" waiting rooms");
			
			
		}else if(line.contentEquals("client")) {
			ClientIP = connection.getInetAddress().getHostAddress();
			client = true;
			System.out.println("Connected to Client at: " + connection.getInetAddress().getHostAddress());
			
			// Controller can now push messages
			Socket s = Controller.getServers().get(0);
			
			ControllerIn = new DataInputStream(s.getInputStream());
			ControllerOut = new DataOutputStream(s.getOutputStream());
			
			ControllerOut.writeUTF("ENTER");
		}
		
		
			while(connection.isConnected()) {
		
			
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
