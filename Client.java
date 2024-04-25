import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

public class Client {
	public static void main(String[] args) {

		for (int i = 0; i < Integer.parseInt(args[2]); i++) {
			//Creates N number of threads as specified by the third program argument
			CustomerThread newThread = new CustomerThread(args, i);
			newThread.start();
		}

    }
	static class CustomerThread extends Thread {
		private String[] args;
		private int threadNum;
		public Boolean[] killFlag = new Boolean[1];
		private Socket fitRoom;

		public CustomerThread (String[] args, int a) {
			this.args = args;
			this.threadNum = a;
		}

		public void run() {
			Socket Server = null;
			PrintWriter pw = null;
			BufferedReader br = null;
			//Creates reference points for the variables
			System.out.println("Attempting to connect to Central Server");
			try {
				Server = new Socket(args[0], Integer.parseInt(args[1]));
				pw = new PrintWriter(Server.getOutputStream());
				br = new BufferedReader(new InputStreamReader(Server.getInputStream()));
				//Binds a socket for the central server and sets up an input and output stream reader/writer for it
			} catch (Exception e) {
				e.printStackTrace();
				exit(1);
			}

			pw.write("FITQUERY");pw.flush();
			System.out.println("Connected to Central Server, Sending Fitting Room Query");
			//Writes and then flushes a message to the central server containing a request for a fitting room ip
			String responseIP = null;
			System.out.println("Waiting for Query Response");
            try {
				while (responseIP == null) {
					responseIP = br.readLine();
					//waits until central server sends a message to the client containing a properly formatted ip address
				}

				//Closes the connection with the central server as no more communication will be needed unless the fitting room goes down
				Server.close();
				pw.close();
				br.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

			if (responseIP.equalsIgnoreCase("EXIT")) {exit(0);}
			//If the fitting rooms are full as well as the waiting rooms then the server will send back an exit command to kill the thread
			//TODO
			//This line may kill the entire program not just the current thread which could be an issue

			//beginning of fitting room communications
			System.out.println("Fitting Room IP received from Central Server, Attempting to Connect to Fitting Room");
			try {
				this.fitRoom = new Socket(responseIP, Integer.parseInt(this.args[1]));
				pw = new PrintWriter(this.fitRoom.getOutputStream());
				br = new BufferedReader(new InputStreamReader(this.fitRoom.getInputStream()));
				//Connects to the received fitting room IP using the same port as previously used, also sets up the input and output reader/writer at the same time
			} catch (Exception ex) {
				ex.printStackTrace();
				exit(1);
			}

			pw.write("ENTER");pw.flush();
			//TALK TO YASH ABOUT HOW HE WANTS ME TO SAY THAT THE CLIENT IS HERE OR IF HE NEEDS A MESSAGE AT ALL
			//FOR NOW JUST ASSUMING THAT CONNECTION IS GOOD TO GO AND MOVING ONTO WAITING FOR A CONSOLE MESSAGE TO END THREAD

			try {
				System.out.println(br.readLine());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			while (!killFlag[0]) {
				if (!this.fitRoom.isConnected()) {
					Socket tempServer = null;
					PrintWriter temppw = null;
					BufferedReader tempbr = null;
					//Creates reference points for the variables
					System.out.println("Attempting to connect to Central Server");
					try {
						tempServer = new Socket(args[0], Integer.parseInt(args[1]));
						temppw = new PrintWriter(tempServer.getOutputStream());
						tempbr = new BufferedReader(new InputStreamReader(tempServer.getInputStream()));
						//Binds a socket for the central server and sets up an input and output stream reader/writer for it
					} catch (Exception e) {
						e.printStackTrace();
						exit(1);
					}

					temppw.write("FITQUERY");temppw.flush();
					System.out.println("Connected to Central Server, Sending Fitting Room Query");
					//Writes and then flushes a message to the central server containing a request for a fitting room ip
					String tempresponseIP = null;
					System.out.println("Waiting for Query Response");
					try {
						while (tempresponseIP == null) {
							tempresponseIP = tempbr.readLine();
							//waits until central server sends a message to the client containing a properly formatted ip address
						}

						//Closes the connection with the central server as no more communication will be needed unless the fitting room goes down
						tempServer.close();
						temppw.close();
						tempbr.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					temppw.write("ENTER");temppw.flush();

					if (tempresponseIP.equalsIgnoreCase("EXIT")) {exit(0);}
					//If the fitting rooms are full as well as the waiting rooms then the server will send back an exit command to kill the thread
					//TODO
					//This line may kill the entire program not just the current thread which could be an issue
					System.out.println("Fitting Room IP received from Central Server, Attempting to Connect to Fitting Room");
					try {
						this.fitRoom = new Socket(tempresponseIP, Integer.parseInt(this.args[1]));
						pw = new PrintWriter(this.fitRoom.getOutputStream());
						br = new BufferedReader(new InputStreamReader(this.fitRoom.getInputStream()));
						//Connects to the received fitting room IP using the same port as previously used, also sets up the input and output reader/writer at the same time
					} catch (Exception ex) {
						ex.printStackTrace();
						exit(1);
					}
				}
				//Fault tolerance testing of this is needed to be safe
                try {
                    this.wait(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //Kill flag is changed in the main method as this will work off of console commands based on my understanding
				//I don't believe much needs to be done here as the client will simply wait for the console command to say when its finished changing
				//TODO check with quevas about the validity of this statement
			}






        }
	}
}
