import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataOutputStream;

public class Controller {


	public static void main(String[] args) throws IOException {
		int port = 32005;
		
		

		ServerSocket controller = new ServerSocket(port);
	
		
		System.out.println("Server started");

		while (true) {
			try {

				Socket clientSocket = controller.accept();

				ClientHandler x = new ClientHandler(clientSocket,port);
				x.start();

			} catch (Exception e) {
				controller.close();
				e.printStackTrace();
			}
		}

	}

}

class ClientHandler extends Thread {
    //Server IP
	String host = "192.168.0.214";
	int port;
	
	Socket clientSocket;
	private DataInputStream in = null;
	private DataOutputStream out = null;
	
	public ClientHandler(Socket clientSocket, int port) {
		this.clientSocket = clientSocket;
		this.port = port;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Connected to Client at " + clientSocket.getInetAddress().getHostAddress());
			out = new DataOutputStream(clientSocket.getOutputStream());
			Socket fittingRoom = new Socket(host,port);
			
			String message = "Please Select from the following menu or write OVER to end Session:\nIP\nTIME\nMATH\n=========================================\n";

			// Message Pushed to Client
			out.writeUTF(message);
			
			//Get Input from Client
			in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
			String line = "";
			while(!line.equalsIgnoreCase("OVER")) {
				line = in.readUTF();
				System.out.println(clientSocket.getInetAddress().getHostAddress()+ ": " + line + "\n");
			
			}	
			
	    System.out.println("Disconnected to Client at " + clientSocket.getInetAddress().getHostAddress());	
		in.close();
		out.close();
		clientSocket.close();
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}
