import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	public static void main (String[] args) {
		final String CENTRAL_IP = "10.183.240.31";
		final int CENTRAL_PORT = 32005;
		final int NUM_OF_CLIENTS = 3;
		
		for (int i = 1; i <= NUM_OF_CLIENTS; i++) {
			//Creates N number of threads as specified by the third program argument
			try {
				
				
				System.out.println("\tClient thread "  + i + " created");
				//Debugging print
				customerThread newThread = new customerThread(CENTRAL_IP, CENTRAL_PORT, i);
				//Creates Thread
				newThread.start();
				//Starts Thread
				Thread.sleep((long)(Math.random() * 1000));
				//Sleeps for 0 -> 1 seconds before creating new thread
				
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//end of loop
		}
		//end of main
	}
	
	
	
	private static class customerThread extends Thread {
		private int port;
		private String ip;
		
		private int threadNum;
		//The value of i when the thread is created
		// i, 1,2,3,4,5....
		private String tName;
		//The printable version of threadNum
		// "Thread i"
		
		private Socket centralServer = null;
		private PrintWriter toCentral = null;
		private BufferedReader fromCentral = null;
		
		private int getThreadNum() {
			return threadNum;
		}
		private void setThreadNum(int threadNum) {
			this.threadNum = threadNum;
		}
		
		private String getTName() {
			return tName;
		}
		private void setTName() {
			this.tName = ("Client " + this.getThreadNum());
		}
		
		public customerThread (String IP, int port, int threadNum) {
			this.ip = IP;
			this.port = port;
			
			this.setThreadNum(threadNum);
			this.setTName();
			
			System.out.println(this.getTName() + ", Created");
		}
		
		//@OVERRIDES THREAD.RUN()
		//Contains body of code for the thread
		public void run () {
			
			//TASK 1 HERE
			//Set up socket for central server as well as both input and output streams
			//After that send identifying "CLIENT" message to central server
			try {
				this.initConnection();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			////////Main Run-Time Loop/////////////////////
			int task = 2;
			String message = "";
			
			int errTask = -1;			
			while (task <= 9) {
				//Main switch case of tasks to loop through
				switch (task) {
					case 0:
						//Task 0, THIS IS AN ERROR TASK, ALL UNKNOWN SEQUENCING RELATED ERRORS SHOULD BE REDIRECTED HERE
						//Prints error message followed by setting task to whichever task closes connections
						System.out.println("Error in sequencing at " + errTask);
						task = 9;
						break;
						
						
					case 1:
						//Task 1, THIS IS AN ERROR TASK, ALL INPUT RELATED ERRORS SHOULD BE REDIRECTED HERE
						//THE ONLY POSSIBLE SOURCES OF INPUT RELATED ERRORS SHOULD BE TASK 2, OR, TASK 4
						//Prints error message followed by setting the task to whichever task closes connections
						System.out.println("Error in processing input at " + errTask);
						task = 9;
						break;
						
						
////////////////////////////////////////////////////////ERROR HANDLING CASES ^^^^^^^^^^^^/////////////////////////////////////////////////////////////////////////
					case 2:
						//Task 2, Read in a message from the central server which should either be {"CONNECTED,IP ADDRESS HERE", or, "NOT CONNECTED"}
						message = this.readMessage(task);
						task++;
						break;
						
						
					case 3:
						//Task 3, Processes the message received in task 2 and redirect accordingly
						if (message.equalsIgnoreCase("")) {
							//ERROR HANDLING, MESSAGE FAILED TO BE READ IN TASK 2///
							errTask = 2;
							//redirect to input error task
							task = 1;
							break;
							////////////////////////////////////////////////////////
						} 
						
						///ANYTHING PAST THIS POINT IN TASK 3 ASSUMES MESSAGING AND READING WAS SUCCESSFULL IN PRIOR TASKS
						if (message.split(",")[0].equalsIgnoreCase("CONNECTED")) {
							//MESSAGE HANDLING, MESSAGE READ IN SUCCESSFULLY DURING TASK 2
							System.out.println("\t\t\t" + this.getTName() + ", Connected to fitting room server located at <" + message.split(",")[1] + ">");
							//message successful move onto task 4
							task++;
							break;
						} else if (message.equalsIgnoreCase("NOTCONNECTED")) {
							//MESSAGE HANDLING, MESSAGE READ IN SUCCESSFULLY DURING TASK 2
							System.out.println("\t\t\t" + this.getTName() + ", No space in fitting rooms or no fitting rooms available");
							//message successful move onto task 8
							task = 9;
							break;
						} else {
							//UNKNOWN ERROR, ISSUE IN SEQUENCING MOST LIKELY OR SPELLING
							errTask = task;
							task = 0;
							break;
						}
						
						
					case 4:
						//Task 4, Send the enter message to the central server which will be redirected to the fitting room
						this.sendMessage("enter", task);
						//"ENTER" message should be sent to the fitting room which will attempt to place me into a changing room
						task++;
						//Moves onto task 5
						break;
						
						
					case 5:
						//Task 5, Read in a message from the central server which should be either {"ENTERED,IP ADDRESS HERE", or, "WAITING, IP ADDRESS HERE"}
						message = this.readMessage(task);
						//Moves onto task 6
						task++;
						break;
					
						
					case 6:
						//Task 6, Processes the message received in task 5 and redirects accordingly
						if (message.equalsIgnoreCase("")) {
							//ERROR HANDLING, MESSAGE FAILED TO BE READ IN TASK 5///
							errTask = 5;
							//redirect to input error task
							task = 1;
							break;
							////////////////////////////////////////////////////////
						} else if (message.equalsIgnoreCase("disconnect")) {
							//ERROR HANDLING, FITTING ROOM CRASHED
							System.out.println("\t\t\t\t" + this.getTName() + ", Fitting Room Crashed, Redirecting");
							//Redirects back to task 2 which will read a message and processes connected or not connected
							task = 2;
							break;
						}
						
						///ANYTHING PAST THIS POINT IN TASK 6 ASSUMES MESSAGING AND READING WAS SUCCESSFULL IN PRIOR TASKS
						if (message.split(",")[0].equalsIgnoreCase("ENTERED")) {
							//MESSAGE HANDLING, MESSAGE READ IN SUCCESSFULLY DURING TASK 5
							System.out.println("\t\t\t\t" + this.getTName() + ", Entered changing room inside of Fitting room server located at " + message.split(",")[1]);
							//message successful move onto task 7
							task++;
							break;
						} else if (message.split(",")[0].equalsIgnoreCase("WAITING")) {
							//MESSAGE HANDLING, MESSAGE READ IN SUCCESSFULLY DURING TASK 5
							System.out.println("\t\t\t\t" + this.getTName() + ", Entered waiting room inside of Fitting room server located at " + message.split(",")[1]);
							//Revert back to task 5 and wait for Entered message
							task--;
							break;
						} else {
							//UNKNOWN ERROR, ISSUE IN SEQUENCING MOST LIKELY OR SPELLING
							errTask = task;
							task = 0;
							break;
						}
						
						
					case 7:
						//Task 7, Sleep for 0 -> 1 seconds
						try {
							Thread.sleep((long)(Math.random() * 1000));
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						//Move onto task 8
						task++;						
						break;
						
						
					case 8:
						//Task 8, Send the final "EXIT" message
						this.sendMessage("exit", task);
						//Move onto task 9
						task++;
						break;
						
						
					case 9:
						//Task 9, close all connections
						if (this.toCentral != null) {
							this.toCentral.close();
						} if (this.fromCentral != null) {
							try {
								this.fromCentral.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						} if (this.centralServer != null) {
							try {
								this.centralServer.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						//All connections closed, move onto unreachable task which will end thread execution
						task++;
						break;
				}
			}
			
			
			
			
			
		}
		
		
		public void initConnection () throws IOException {
			System.out.println("\t\t" + this.getTName() + ", Attempting connection to central server located at <" + this.ip + ">");
			
			try {
				this.centralServer = new Socket(this.ip, this.port);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			System.out.println("\t\t" + this.getTName() + ", Connection to central server established");
			
			//Setting up input and output flow
			this.fromCentral = new BufferedReader(new InputStreamReader(this.centralServer.getInputStream()));
			this.toCentral = new PrintWriter(this.centralServer.getOutputStream());
			//input and output streams set up
			
			//Sending message to central server to identify this socket as a client
			this.sendMessage("client", 0);
			//ignores output from method and assumes that it was sent successfully
		}
		
		
		public boolean sendMessage (String message, int a) {
			a++;
			//Messaging handler for modularization
			try {
				System.out.println("Attempting to send message, [" + message + "] to central server at task: " + a);
				//Attempts to send a message via the print writer
				
				this.toCentral.println(message.toUpperCase());
				this.toCentral.flush();
				
				System.out.println("Successfully sent message, [" + message + "] to central server at task: " + a);
				//If message sends successfully then return true
				return true;
			} catch (Exception e) {
				//If message fails to send then return false
				System.out.println("Failed to send message, [" + message + "] to central server at task: " + a);
				return false;
			}
		}
		
		public String readMessage (int a) {
			String message = "";
			a++;
			//Message receiver handler for modularization
			try {
				System.out.println("Attempting to read in message from central server at task: " + a);
				//attempts to read in message via bufferedReader
				
				message = this.fromCentral.readLine();
				
				//If message is read successfully then return message
				System.out.println("Successfully read in message, [" + message + "] from central server at task: " + a);
				return message;
			} catch (Exception ex) {
				System.out.println("Failed to read in message from central server at task: " + a);
				//If message fails to be read then return empty string
				return message;
			}
		}
		
		
		
	}
}
