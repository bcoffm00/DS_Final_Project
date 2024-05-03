import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;

public class Client {
	public static void main(String[] args) {

		for (int i = 0; i < Integer.parseInt(args[2]); i++) {
			//Creates N number of threads as specified by the third program argument
			System.out.println("Client thread "  + i + " created");
			CustomerThread newThread = new CustomerThread(args, i);
			newThread.start();
			try {
				Thread.sleep((long)(Math.random() * 1000));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	static class CustomerThread extends Thread {
		private String[] args;
		private int threadNum;
		private Socket centralServer;
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
						if (!message.contains("Client has entered room") || !message.contains("A room is now free")) {
							System.out.println("listening");
							message = br.readLine();
							System.out.println("heard");
						}
						System.out.println(message + " to: " + cliName);
						
					}
					if ((message).equalsIgnoreCase("disconnect")) {
						task = 1;
					} else {
						switch (task) {
							case 1:
								pw.println("client");pw.flush();
								System.out.println("\t\t" + cliName + " sending \"client\" message to central server");
								task++;
								break;
								//Sends ENTER message to server
							case 2:
								if (message.contains("Connected to Fitting Room Server")) {
									System.out.println("\t\t\t" + cliName + " has connected to the fitting room");
									task++;
									pw.println("ENTER");pw.flush();


								} else if (message.contains("No fitting room server")) {
									System.out.println("\t\t\t" + cliName + ", no fitting rooms available, killing client");
									task = 10000;
									this.pw.println("RECEIVED");this.pw.flush();

								} else {
									System.out.println("\t\t\t" + cliName + ", Unknown input, reverting");
									task--;

								}
								break;
							case 3:
								if (message.contains("Client has entered room ")) {
									System.out.println("\t\t\t\t" + cliName + " has entered a changing room");
									task = 4;
									//Client is in changing room, move on to sleep for x amount
									
								} else if (message.contains("All rooms are occupied")){
									System.out.println("\t\t\t" + cliName + " has entered a waiting room, reverting");
									task = 5;
									//Client is in waiting room, check for messages until the client gets a 

								} else if (message.contains("Both fitting rooms and waiting room are full")){
									task = 2;
									System.out.println("\t\t" + cliName + " all fitting and waiting options are full, disconecting");
									
									//All fitting room and waiting rooms are full disconnecting

								} else if(message.contains("You have exited the room.")) {
									task = 1000;
									System.out.println("\t\t\t" + cliName + " has exited the fitting room.");
								}
								break;
							case 4:
								System.out.println("here");
								Thread.sleep((long)(Math.random() * 1000));
								System.out.println("there");
								pw.println("EXIT");pw.flush();
								//Sends the leave message to the central server
								task = 10000;
								break;
							case 5:
								if (message.contains("A room is now free")) {
									System.out.println("\t\t" + cliName + " has entered a changing room from a waiting room");
									task = 4;
								}
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
			} catch (Exception ex) {
				ex.printStackTrace();
			}


		}


	}
}
