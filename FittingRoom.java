/***********************************************************
 * Name:   		   Brody Coffman, Tony Aldana and Yash Patel
 * Problem Set:	    Final Group project
 * Due Date :	     5/2/24
 ***********************************************************/

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.LogManager;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.Queue;
import java.util.LinkedList;


public class FittingRoom {

    private static final int PORT = 32005;
    private static final int MAX_FITTING_ROOMS = 1;
    private static final int MAX_WAITING_ROOM = MAX_FITTING_ROOMS * 1;
    //private static boolean[] rooms = new boolean[MAX_FITTING_ROOMS + 1];
    //private static Queue<Socket> waitingQueue = new LinkedList<>();
    //private static Map<Socket, Integer> socketToRoomMap = new HashMap<>();
    //private static ReentrantLock lock = new ReentrantLock();
    private static final Logger LOGGER = Logger.getLogger(FittingRoom.class.getName());
    public static Semaphore froom;
	public static Semaphore wroom;

    static {
        setupLogger();
    }

    private static void setupLogger() {
        try {
            LogManager.getLogManager().reset();
            Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            FileHandler fh = new FileHandler("FittingRoom.log", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting up logger", e);
        }
    }
    public static void main(String[] args) {
    	froom = new Semaphore(MAX_FITTING_ROOMS );
    	wroom = new Semaphore(MAX_WAITING_ROOM );

    	ExecutorService executor = Executors.newFixedThreadPool(10);
    	int i = 0;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        	//Connects to Central Server
        	Socket s = new Socket("192.168.0.0",PORT);

			PrintWriter out = new PrintWriter(s.getOutputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

			//Sends message to Central saying its a Server
			out.println("Server");
			out.flush();

			LOGGER.info("Server started. Listening on Port " + PORT);
            String line = "";
            while (true) {
                Socket socket = serverSocket.accept();

                PrintWriter output = new PrintWriter(socket.getOutputStream());
    			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                line = input.readLine();

                //Checking for Hearbeat Connection
    			if(line.contentEquals("Heartbeat")) {
                 	output.println(wroom.availablePermits() + "," + froom.availablePermits());
                 	output.flush();
                 	socket.close();
                     }else if(line.contentEquals("FITQUERY")){
                    	 System.out.println("New client connected");
                    	 executor.execute(new ClientHandler(socket,i));
                    	 i++;
                     }

            }
        } catch (IOException e) {
        	 LOGGER.log(Level.SEVERE, "Server socket error", e);

        }
    }

    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private int id;
        public ClientHandler(Socket socket, int id) {
            this.clientSocket = socket;
            this.id = id;

        }

        public void run() {

            try ( PrintWriter output = new PrintWriter(clientSocket.getOutputStream());
    			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            	System.out.println(94);
                output.println("Connected to Fitting Room Server. Type 'ENTER' to enter a room, 'EXIT' to leave a room or 'OVER' to disconnect.");
                output.flush();

                boolean running = true;
                while (running) {
                    try {

                        String clientMessage = input.readLine();
                        if (clientMessage != null) {
                        	handleClientRequest(clientMessage, output,input,running);

                        }


                    } catch (SocketException e) {
                    	System.out.println("Socket Exception At Thread-" + id + ": ");
                    	e.printStackTrace();

                    	 LOGGER.log(Level.SEVERE, "Socket was closed unexpectedly", e);

                    }
                }
            	}
            catch (IOException e) {
            	e.printStackTrace();
            	LOGGER.log(Level.SEVERE, "I/O error", e);


            } finally {

               // releaseResources();

                try {

                    clientSocket.close();

                } catch (IOException e) {

                	LOGGER.log(Level.SEVERE, "Error closing socket", e);
                }
            }
        }

        private void releaseResources() {

          lock.lock();
            try {
                Integer roomNumber = socketToRoomMap.remove(clientSocket);
                if (roomNumber != null && roomNumber > 0) {
                    rooms[roomNumber] = false;
                }
                waitingQueue.remove(clientSocket);
                LOGGER.info("Resources released for client " + id + ": " + clientSocket);
            } finally {

                lock.unlock();
            }
            System.out.println("Resources released for client " + id + ": " + clientSocket);
        }

        private void handleClientRequest(String request, PrintWriter output,BufferedReader input, Boolean running) throws IOException {
        	System.out.println("Thread " + id + ": " + request);
            if (request.equalsIgnoreCase("ENTER")) {
                lock.lock();
                try {
                    int roomNumber = findFreeRoom();
                    if (roomNumber != -1) {

                        output.println("Client has entered room " + roomNumber);
                        output.flush();
                        System.out.println(154);
                    } else if (waitingQueue.size() < MAX_WAITING_ROOM) {
                        waitingQueue.add(clientSocket);
                        output.println("All rooms are occupied. You have been added to the waiting queue.");
                        output.flush();
                    } else {
                        output.println("Both fitting rooms and waiting room are full. Please try again later.");
                        output.flush();

                        running = false;
                    }
                } finally {
                    lock.unlock();
                }
            	while(true) {

					if(froom.tryAcquire()) {

						output.println("\t\t Client has entered room ");
						output.flush();
						System.out.println("\t\t Client has entered room ");

						return;

					}else if(wroom.tryAcquire()) {

						System.out.println("All rooms are occupied. You have been added to the waiting queue.");
						while(true) {
							if(froom.tryAcquire()) {
								wroom.release();
								output.println("A room is now free. Client has entered room");
								output.flush();
								System.out.println("A room is now free. Client has entered room");

								return;
							}
						}

					}else {
						output.flush();
						running = false;
						return;

					}

				}


            } else if (request.equalsIgnoreCase("EXIT")) {

               // boolean success = leaveRoom();
                try {
                	froom.release();

                	output.println("You have exited the room.");
                    output.flush();
                    running = false;
                    return;
                }catch(Exception e) {
                	e.printStackTrace();
                	running = false;
                	return;
                }
            }
        }

       /* private int findFreeRoom() {
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
                    try (PrintWriter output = new PrintWriter(client.getOutputStream())) {
                        output.println("A room is now free. Client has entered room " + roomNumber);
                        output.flush();
                        LOGGER.info("Client moved from waiting queue to room number " + roomNumber);
                    } catch (IOException e) {
                    	LOGGER.log(Level.SEVERE, "Error moving client from queue", e);
                    }
                }
            }
        } 
    }
}
