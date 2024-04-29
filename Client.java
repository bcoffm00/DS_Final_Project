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
			CustomerThread newThread = new CustomerThread(args, i, flagList);
			flagList.add(new boolean[]{false, false});
			newThread.start();
		}

		String response = "";
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
		}


    }
	static class CustomerThread extends Thread {
		private String[] args;
		private int threadNum;
		public ArrayList<boolean[]> flags;
		private Socket centralServer;
		private Socket fittingRoom;
		private PrintWriter pw = null;
		private BufferedReader br = null;
		public CustomerThread (String[] args, int a, ArrayList<boolean[]> b) {
			this.args = args;
			this.threadNum = a;
			this.flags = b;
			System.out.println("Attempting to connect Thread " + this.threadNum + " to central server");
			try {
				this.centralServer = new Socket(args[0], Integer.parseInt(args[1]));
				this.pw = new PrintWriter(this.centralServer.getOutputStream());
				this.br = new BufferedReader(new InputStreamReader(this.centralServer.getInputStream()));
				//Binds a socket for the central server and sets up an input and output stream reader/writer for it
			} catch (Exception e) {
				e.printStackTrace();
				exit(1);
			}
			System.out.println("Thread " + this.threadNum + " is connected to central server");
			this.Enter();

		}
		private void Enter () {
			//This is the method for connecting to a fitting room
			try {
				pw.write("FittingRoomQuery");pw.flush();
				//Message is sent to the central server asking for the fitting room ip that is assigned to this thread
			} catch (Exception ex) {
				ex.printStackTrace();
				exit(1);
			}
			//At this point the central server should send a message to this socket that contains the ip of the fitting room
			try {
				String response;
				while ((response  = br.readLine()) == null) {
					this.sleep(500);
					//TODO have the central server send back a fitting room ip
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
			pw.write("Enter");pw.flush();
			System.out.println("\t\tSending \"Enter\" message to central server");
			//Writes and then flushes a message to the central server containing "Enter" which should then somehow
			//add the customer thread into the appropriate fitting room and send that message to the fitting room server

			//waiting for the message that im in the fitting room

			while (!flags.get(this.threadNum)[1]) {
				if (!this.fittingRoom.isConnected()) {
					this.Enter();
					pw.write("Enter");pw.flush();
					//Re-connects to a fitting room server and then sends a enter message to the central server telling it to go back into the fitting room
				}
                try {
                    this.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //Waits until this the kill flag is changed
			}

			pw.write("leave");pw.flush();
			//Sends the leave message to the central server






        }


	}
}
