import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;

public class Client {
	public static void main(String[] args) {
		ArrayList<boolean[]> flagList = new ArrayList<>();

		for (int i = 0; i < Integer.parseInt(args[2]); i++) {
			//Creates N number of threads as specified by the third program argument
			CustomerThread newThread = new CustomerThread(args, i);
			System.out.println("Client thread "  + i + " created");
			newThread.start();
		}

		/*String response = "";
		Scanner console = new Scanner(System.in);
		while (!(response = console.nextLine()).equals("END")) {
			System.out.println("Current list of clients in a fitting room:");
			for (int i = 0; i < flagList.size(); i++) {
				if (!flagList.get(i)[0]) {
					System.out.println("\tThread " + i + ":\t In fitting room changing");
				}
			}
			//response formatting: "thread number" ie.. "1" this would correspond to the i'th thread, starts from 0 inclusively
			boolean[] kill = {false, true};
			//{if its in a waiting room, leave flag}
			flagList.set(Integer.parseInt(response), kill);
		}*/


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
				this.pw.write("client");this.pw.flush();
				//Binds a socket for the central server and sets up an input and output stream reader/writer for it
			} catch (Exception e) {
				e.printStackTrace();
				exit(1);
			}
			System.out.println("\tClient " + this.threadNum + " is connected to central server");
			this.Enter();

		}
		private void Enter () {
			//This is the method for connecting to a fitting room
			try {

				pw.write("FittingRoomQuery");pw.flush();
				System.out.println("\tClient " + this.threadNum + " fitting room IP qeuried");
				//Message is sent to the central server asking for the fitting room ip that is assigned to this thread
			} catch (Exception ex) {
				ex.printStackTrace();
				exit(1);
			}
			//At this point the central server should send a message to this socket that contains the ip of the fitting room
			try {
				String response = null;
				while ((response  = br.readLine()) == null) {
					this.sleep(500);
					//TODO have the central server send back a fitting room ip
					//GETS STUCK HERE FOR NOW
				}

				//This should wait for the central server to send the fitting room ip ^^^
				this.fittingRoom = new Socket(response, Integer.parseInt(args[1]));
			} catch (Exception ex) {
				System.out.println("Error in the fitting room ip response");
				ex.printStackTrace();
				exit(1);
			}
		}

		public void run() {
			/////////////
			//this.Enter();
			//THIS NEEDS TO BE ALTERED SOMEHOW
			//////////////////
			//After successfully being connected with the central server and sending the "fittingroomquery" message then I should be assigned to a fitting room
			//and the IP of said fitting room returned. I should be placed into the waiting room and when the fitting room is ready for me to be in the changing room then
			//A "ready" message should be sent via the central server to me

			//waiting for the message that states that im not in the waiting room and there is space for me in a changing room
			pw.write("ENTER");pw.flush();
			System.out.println("\t\t" + "Client " + this.threadNum + " sending \"Enter\" message to fitting room");
			String waitingAnswer = "";
			int kill = 0;
			try {
				while (!(waitingAnswer = br.readLine()).equalsIgnoreCase("changingRoom") && kill == 0) {
					if (waitingAnswer.equalsIgnoreCase("waitingRoom")) {
						System.out.println("Client " + this.threadNum + " is waiting on a changing room");
					} else if (waitingAnswer.equalsIgnoreCase("full")) {
						kill++;
					}
					pw.write("ENTER");pw.flush();
					this.sleep(250);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			if (kill == 0) {
				//system should wait a random amount of time from 0 -> 1 before leaving the changing room
				try {
					this.sleep((long)(Math.random() * 1000));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				pw.write("EXIT");pw.flush();
				//Sends the leave message to the central server

			}

		}


	}
}