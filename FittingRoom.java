import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class FittingRoom {
	final static String CENTRAL_IP = "192.168.0.0";
	final static int CENTRAL_PORT = 32005;
	
	public static void main (String[] args) {
		//Sends the message "SERVER" to the central server to indicate this is a fitting room
		initConnect();
		ServerSocket serverSock;
		try {
			//Started the server socket
			serverSock = new ServerSocket(CENTRAL_PORT);
			FitRoom FittingRoom = new FitRoom(Integer.parseInt(args[0]));
			//Main loop to rip threads
			while (true) {
				Socket newCon = serverSock.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(newCon.getInputStream()));
				String line = br.readLine();
				
				//If its a heartbeat socket
				if (line.equalsIgnoreCase("HEARTBEAT")) {
					//CHECK THE TWO SEMA's AND PASS BACK DATA
					PrintWriter pr = new PrintWriter(newCon.getOutputStream());
					pr.println(bloodTest(FittingRoom));
					pr.flush();
					
					try {
						br.close();
						pr.close();
						newCon.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else if (line.equalsIgnoreCase("fitquery")) {
					fitConnection curConnection = new fitConnection(newCon, FittingRoom);
					curConnection.start();
				}
				
			}
			
			
			//END OF TRY
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//END OF MAIN
	}
	
	
	private static String bloodTest (FitRoom a) {
		return a.bloodTest();
	}
	private static void initConnect () {
		try {
			Socket centralServer = new Socket(CENTRAL_IP, CENTRAL_PORT);
			PrintWriter toConnect = new PrintWriter(centralServer.getOutputStream());
			
			toConnect.println("SERVER");toConnect.flush();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	
	@SuppressWarnings("unused")
	private static class fitConnection extends Thread{
		private FitRoom par = null;
		private Socket connection = null;
		private PrintWriter toConnect = null;
		private BufferedReader fromConnect = null;
		
		public fitConnection (Socket newSock, FitRoom b) {
			this.par = b;
			
			try {
				this.connection = newSock;
			
				this.fromConnect = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
				this.toConnect = new PrintWriter(this.connection.getOutputStream());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		public boolean sendMessage (String message) {
			//Messaging handler for modularization
			try {
				System.out.println("Attempting to send message, [" + message + "] to central server");
				//Attempts to send a message via the print writer
				
				this.toConnect.println(message.toUpperCase());
				this.toConnect.flush();
				
				System.out.println("Successfully sent message, [" + message + "] to central server");
				//If message sends successfully then return true
				return true;
			} catch (Exception e) {
				//If message fails to send then return false
				System.out.println("Failed to send message, [" + message + "] to central server");
				return false;
			}
		}
		
		public String readMessage () {
			String message = "";
			//Message receiver handler for modularization
			try {
				System.out.println("Attempting to read in message from central server");
				//attempts to read in message via bufferedReader
				
				message = this.fromConnect.readLine();
				
				//If message is read successfully then return message
				System.out.println("Successfully read in message, [" + message + "] from central server");
				return message;
			} catch (Exception ex) {
				System.out.println("Failed to read in message from central server");
				//If message fails to be read then return empty string
				return message;
			}
		}
		
		public void run () {
			try {
				String message = "";
				String response = "";
				int task = 1;
				while (task < 9) {
					switch (task) {
						case 0:
							System.out.println("Sequencing error");
							task = 1000;
							break;
						case 1:
							message = this.readMessage();
							task++;
							break;
							
						case 2:
							if (message.equalsIgnoreCase("enter")) {
								response = this.par.enter(this);
								task++;
								break;
							} else {
								task = 0;
								break;
							}
							
						case 3:
							this.sendMessage(response);
							task++;
							break;
						
						case 4:
							if (response.equalsIgnoreCase("waiting")) {
								task = 5;
								break;
							} else {
								task = 6;
								break;
							}
						
						case 5:
							while (!this.par.compare(this)) {}
							while (response.equalsIgnoreCase("waiting")) {
								response = this.par.enter(this);
							}
							this.sendMessage(response);
							task++;
							break;
							
						case 6:
							message = this.readMessage();
							task++;
							break;
						
						case 7:
							this.sendMessage(this.par.exit());
							task++;
							break;
						
						case 8:
							try {
								if (this.toConnect != null) {
									this.toConnect.close();
								} if (this.fromConnect != null) {
									try {
										this.fromConnect.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								} if (this.connection != null) {
									try {
										this.connection.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
					}
				}
				
				if (message.equalsIgnoreCase("ENTER")) {
					message = this.par.enter(this);
				}
				
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	private static class ChangeRoom {
		private int numRooms;
		private Semaphore fitRooms = null;
		
		public ChangeRoom (int a) {
			this.numRooms = a;
			this.fitRooms = new Semaphore(a);
		}
		
		public boolean tryAcquire () {
			return this.fitRooms.tryAcquire();
		}
		
		public void release () {
			this.fitRooms.release();
		}
		
		public int stateCheck () {
			return this.fitRooms.availablePermits();
		}
	}
	
	private static class WaitRoom {
		private int numRooms;
		private LinkedList<fitConnection> waitRooms = new LinkedList<>();
		
		public WaitRoom (int a) {
			this.numRooms = a;
		}
		
		public boolean contains (fitConnection a) {
			return this.waitRooms.contains(a);
		}
		public boolean enQueue (fitConnection a) {
			if (this.waitRooms.size() < this.numRooms) {
				this.waitRooms.add(a);
				return true;
			}
			
			return false;
		}
		
		public fitConnection peek () {
			return this.waitRooms.peek();
		}
		
		public fitConnection deQueue () {
			if(this.waitRooms.isEmpty()) {
				return null;
			}else {
			return this.waitRooms.remove();
			}
		}
		
		public int stateCheck () {
			return this.waitRooms.size();
		}
		
		 
	}
	
	private static class FitRoom{
		private WaitRoom WaitingRooms = null;
		private ChangeRoom ChangingRooms = null;
		
		public FitRoom (int a) {
			this.WaitingRooms = new WaitRoom(a*2);
			this.ChangingRooms = new ChangeRoom(a);
		}
		
		public String bloodTest () {
			return this.WaitingRooms.stateCheck() + "," + this.ChangingRooms.stateCheck();
		}
		
		public String enter (fitConnection a) {
			if (this.ChangingRooms.tryAcquire()) {
				
				this.WaitingRooms.deQueue();
				return "ENTERED";
			} else {
				if (!this.WaitingRooms.contains(a)) {
					this.WaitingRooms.enQueue(a);
				}
				return "WAITING";
			}
		}
		
		public boolean compare (fitConnection a) {
			if (this.WaitingRooms.peek().equals(a)) {
				return true;
			} else {
				return false;
			}
		}
		
		public String exit () {
			this.ChangingRooms.release();
			return "RECEIVED";
		}
		
		
		
	}

}
