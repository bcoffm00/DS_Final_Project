/********************************************************
 * Name:   		Brody Coffman, Tony Aldana and Yash Patel
 * Problem Set:	Final Group project
 * Due Date :	5/2/24
 *******************************************************/

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
import java.util.concurrent.locks.ReentrantLock;
import java.util.Queue;
import java.util.LinkedList;


public class FittingRoom {

    private static final int PORT = 32005;
    private static final int MAX_FITTING_ROOMS = 4;
    private static final int MAX_WAITING_ROOM = MAX_FITTING_ROOMS * 2;
    private static boolean[] rooms = new boolean[MAX_FITTING_ROOMS + 1];
    private static Queue<Socket> waitingQueue = new LinkedList<>();
    private static Map<Socket, Integer> socketToRoomMap = new HashMap<>();
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {

    	ExecutorService executor = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        	//Connects to Central Server
        	Socket s = new Socket("10.183.240.15",PORT);

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

            try ( PrintWriter output = new PrintWriter(clientSocket.getOutputStream());
    			BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                output.println("Connected to Fitting Room Server. Type 'ENTER' to enter a room, 'EXIT' to leave a room or 'OVER' to disconnect.");
                output.flush();

                boolean running = true;
                while (running) {
                    try {
                        String clientMessage = input.readLine();
                        if (clientMessage.equalsIgnoreCase("OVER")) {
                            running = false;
                        } else {
                            handleClientRequest(clientMessage, output);
                        }
                    } catch (SocketException e) {
                        System.out.println("Socket was closed unexpectedly: " + e.getMessage());
                        running = false;
                    }
                }
            } catch (IOException e) {

                System.out.println("I/O error: " + e.getMessage());
            } finally {

                releaseResources();

                try {

                    clientSocket.close();

                } catch (IOException e) {

                    System.out.println("Error closing socket: " + e.getMessage());
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
            } finally {

                lock.unlock();
            }
            System.out.println("Resources released for client: " + clientSocket);
        }

        private void handleClientRequest(String request, PrintWriter output) throws IOException {
            if (request.equalsIgnoreCase("ENTER")) {
                lock.lock();
                try {
                    int roomNumber = findFreeRoom();
                    if (roomNumber != -1) {
                        output.println("Client has entered room " + roomNumber);
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
                    output.flush();
                    tryAssignRoom();
                } else {
                    output.println("Error: You were not in a room.");
                    output.flush();
                }
            } else {
                output.println("Invalid command.");
                output.flush();
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
                    try (PrintWriter output = new PrintWriter(clientSocket.getOutputStream())) {
                        output.println("A room is now free. You have entered room " + roomNumber);
                        output.flush();
                        System.out.println("Client moved from waiting queue to room number " + roomNumber);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
