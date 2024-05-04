import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FittingRoom {
	final static String CENTRAL_IP = "10.183.240.31";
	final static int CENTRAL_PORT = 32005;
	
	public static void main (String[] args) {
		//Sends the message "SERVER" to the central server to indicate this is a fitting room
		initConnect();
		ServerSocket serverSock;
		try {
			//Started the server socket
			serverSock = new ServerSocket(CENTRAL_PORT);
			
			//Main loop to rip threads
			while (true) {
				Socket newCon = serverSock.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(newCon.getInputStream()));
				String line = br.readLine();
				
				//If its a heartbeat socket
				if (line.equalsIgnoreCase("HEARTBEAT")) {
					//CHECK THE TWO SEMA's AND PASS BACK DATA
					PrintWriter pr = new PrintWriter(newCon.getOutputStream());
					pr.println(bloodTest());
					pr.flush();
					
					try {
						br.close();
						pr.close();
						newCon.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else if (line.equalsIgnoreCase("fitquery")) {
					/////////////////////////////////////////////////////////////////////////////////////////////////
					//thoughts; Figure out if I want to try to add a connection to a pre existing class that will control the status of the fitting room and waiting room
					// the order of messages should be fitquery -> enter -> entered,waiting -> exit -> received
				}
				
			}
			
			
			//END OF TRY
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//END OF MAIN
	}
	
	
	private static String bloodTest () {
		String message = "A,B";
		
		return message;
	}
	private static void initConnect () {
		try {
			Socket centralServer = new Socket(CENTRAL_IP, CENTRAL_PORT);
			PrintWriter toConnect = new PrintWriter(centralServer.getOutputStream());
			
			toConnect.println("SERVER");toConnect.flush();
			centralServer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	
	@SuppressWarnings("unused")
	private static class fitConnection {
		private Socket connection = null;
		private PrintWriter toConnect = null;
		private BufferedReader fromConnect = null;
		
		
		
	}

}
