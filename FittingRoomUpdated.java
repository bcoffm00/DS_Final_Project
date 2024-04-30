import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;


import java.util.Queue;
import java.util.LinkedList;

public class FittingRoomUpdated {
    private static final int PORT = 32005;
    private static final int MAX_FITTING_ROOMS = 4;
    private static final int MAX_WAITING_ROOM = MAX_FITTING_ROOMS * 2;
    private static boolean[] rooms = new boolean[MAX_FITTING_ROOMS + 1]; // Indexing from 1 for simplicity
    private static Queue<Socket> waitingQueue = new LinkedList<>();
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10); 

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        	//Connects to Central Server
        	Socket s = new Socket("192.168.0.0",PORT);
			
			PrintWriter out = new PrintWriter(s.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			
			//Sends message to Central saying its a Server
			out.println("Server");
			out.flush();
			
            System.out.println("Server started. Listening on Port " + PORT);
            String line = "";
            while (true) {
                Socket socket = serverSocket.accept();
                
                PrintWriter output = new PrintWriter(socket.getOutputStream());
    			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                line = input.readLine();
                
                //Checking for Hearbeat Connection
    			if(line.contentEquals("Heartbeat")) {
                 	output.println("Heartbeat");
                 	output.flush();
                 	socket.close();
                     }else if(line.contentEquals("FITQUERY")){
                    	 System.out.println("New client connected");
                    	 executor.execute(new ClientHandler(socket));
                     }
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (PrintWriter output = new PrintWriter(clientSocket.getOutputStream());
            		BufferedReader input =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                boolean running = true;
                while (running) {
                	
                	String clientMessage = input.readLine();
                	System.out.println(clientMessage);
                	//Checks to see if client ready for message
                		if(clientMessage.equalsIgnoreCase("ready")) {
                		
                    	output.println("Connected to Fitting Room Server. Type 'ENTER' to enter a room or 'EXIT' to leave.");
                        output.flush();
                        System.out.println("86");
                        
                        clientMessage = input.readLine();
                        
                        System.out.println(clientMessage);
                        if (clientMessage.equalsIgnoreCase("EXIT")) {
                            running = false;
                        } else if(clientMessage.equalsIgnoreCase("ENTER")){
                        	System.out.println("93");
                        	handleClientRequest(clientMessage,input, output);
                        }
                       
                    
                		}
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

      

		private void handleClientRequest(String request, BufferedReader input, PrintWriter output) throws IOException {
            if (request.equalsIgnoreCase("ENTER")) {
                lock.lock();
                try {
                    int roomNumber = findFreeRoom();
                   
                    if (roomNumber != -1) {
                    	
                    
                    		System.out.println("123");
                            output.println("You have entered room " + roomNumber);
                            output.flush();
                        	
                    	
                    } else if (waitingQueue.size() < MAX_WAITING_ROOM) {
                        waitingQueue.add(clientSocket);
                       
                        
                        	output.println("All rooms are occupied. You have been added to the waiting queue.");
                            output.flush();
                        	
                        
                    } else {
                    	
                    	
                    		output.println("Both fitting rooms and waiting room are full. Please try again later.");
                            output.flush();
                        	
                        
                    }
                } finally {
                    lock.unlock();
                }
            } else if (request.equalsIgnoreCase("EXIT")) {
                boolean success = leaveRoom();
                if (success) {
                    output.println("You have exited the room.");
                    tryAssignRoom();
                } else {
                    output.println("Error: You were not in a room.");
                }
            } else {
                output.println("Invalid command.");
            }
        }

        private int findFreeRoom() {
            for (int i = 1; i <= MAX_FITTING_ROOMS; i++) {
                if (!rooms[i]) {
                    rooms[i] = true;
                    return i;
                }
            }
            return -1; 
        }

        private boolean leaveRoom() {
            for (int i = 1; i <= MAX_FITTING_ROOMS; i++) {
                if (rooms[i]) {
                    rooms[i] = false;
                    return true;
                }
            }
            return false; 
        }

        private void tryAssignRoom() {
            if (!waitingQueue.isEmpty()) {
                int roomNumber = findFreeRoom();
                if (roomNumber != -1) {
                    Socket client = waitingQueue.poll();
                    try (DataOutputStream output = new DataOutputStream(client.getOutputStream())) {
                        output.writeUTF("A room is now free. You have entered room " + roomNumber);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}