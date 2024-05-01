import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;

public class ClientUpdated {
	public static void main(String[] args) {

		for (int i = 0; i < Integer.parseInt(args[2]); i++) {
			//Creates N number of threads as specified by the third program argument
			CustomerThread newThread = new CustomerThread(args, i);
			System.out.println("Client thread "  + i + " created");
			newThread.start();
		}
	}
	
	
	static class CustomerThread extends Thread {
		private String[] args;
		private int threadNum;
		private Socket centralServer;
		private Socket fittingRoom;
		private PrintWriter pw = null;
		private BufferedReader br = null;
		public CustomerThread (String[] args, int a) {
			this.args = args;
			this.threadNum = a;
			System.out.println("\tAttempting to connect client " + this.threadNum + " to central server");
			try {
				this.centralServer = new Socket(args[0], Integer.parseInt(args[1]));
				this.pw = new PrintWriter(this.centralServer.getOutputStream());
				this.br = new BufferedReader(new InputStreamReader(this.centralServer.getInputStream()));
				this.pw.println("client");this.pw.flush();
				//Binds a socket for the central server and sets up an input and output stream reader/writer for it
			} catch (Exception e) {
				e.printStackTrace();
				exit(1);
			}
			System.out.println("\tClient " + this.threadNum + " is connected to central server");

		}

		public void run() {
			String cliName = "Client " + this.threadNum;
			try {
				
				int task = 1;
				String message = "";
				while (task < 9) {
					if (task > 1) {
						message = br.readLine();
						System.out.println(message);
					}
					if ((message).equalsIgnoreCase("disconnect")) {
						task = 1;
					} else {
						switch (task) {
							case 1:
								message = br.readLine();
								pw.println("ENTER");pw.flush();
								System.out.println("\t\t" + cliName + " sending \"Enter\" message to fitting room");
								task++;
								break;
								//Sends ENTER message to server
							case 2: 
								if (message.contains("You have entered room")) {
									task++;
									//Client is in changing room, move on to sleep for x amount
								} else if (message.contains("All rooms are occupied")){
									task--;
									//Client is in waiting room, re-send enter message until in changing room message received
								} else if (message.contains("Both fitting rooms and waiting room are full")){
									task = 10000;
									System.out.println("\t\t" + cliName + " all fitting and waiting options are full, disconecting");
									//All fitting room and waiting rooms are full disconnecting
								} else {
									task--;
									//Unknown or null input, just retry
								}
								break;
							case 3: 
								//Client is beginning to change for 0 - 1000 miliSeconds
								this.sleep((long)(Math.random() * 1000));
								task++;
								break;
							case 4:
								pw.println("EXIT");pw.flush();
								//Sends the leave message to the central server
								break;
							case 5:
								task = 10000;
								break;
						}
					}
				}								
			} catch (Exception ex) {
				ex.printStackTrace();
				exit(1);
			}
		}


	}
}