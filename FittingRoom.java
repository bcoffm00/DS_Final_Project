/**
 * The {@code FittingRoom} class manages a server that simulates a system where clients can
 * request to enter a fitting room, wait if the rooms are occupied, and leave the room. 
 * It uses a fixed thread pool for handling multiple client requests concurrently and logs 
 * all activities to a file.
 * 
 * <p>Key functionalities include handling client connections, managing room availability,
 * and queuing clients when no rooms are immediately available. The server listens on a 
 * predefined port and supports basic commands from clients to enter or exit rooms.</p>
 * 
 * <p>This class utilizes a static initialization block to set up logging configurations
 * and a {@code main} method to instantiate the server and handle incoming connections.</p>
 * 
 * <ul>
 * <li>PORT: The network port the server listens on.</li>
 * <li>MAX_FITTING_ROOMS: Maximum number of fitting rooms available.</li>
 * <li>MAX_WAITING_ROOM: Maximum number of clients that can wait for a fitting room.</li>
 * </ul>
 */

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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
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
    private static final Logger LOGGER = Logger.getLogger(FittingRoom.class.getName());

    static {
        setupLogger();
    }
    
    /**
     * Sets up logging for the application. Configures a {@code FileHandler} to write logs to
     * "FittingRoom.log" with a {@code SimpleFormatter}. Resets previous log configurations.
     */
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
    
    /**
     * Main method to start the server. Initializes a fixed thread pool and listens for
     * client connections, delegating each connection to a {@code ClientHandler} to manage.
     * Logs all significant server actions and errors.
     * 
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server started. Listening on Port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();

                executor.execute(new ClientHandler(socket));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server socket error", e);
        }
    }
    
    /**
     * The {@code ClientHandler} class manages individual client connections, processing
     * requests to enter or exit fitting rooms and maintaining communication until the client
     * disconnects. It handles client input and output via network streams.
     * 
     * <p>Key responsibilities include interpreting client commands, checking room availability,
     * and updating client status in response to their requests.</p>
     */
    private static class ClientHandler implements Runnable {

        private Socket clientSocket;
        private boolean exitedProperly = false;
        
        /**
         * Constructs a new ClientHandler for the given socket.
         * @param socket The client socket.
         */
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        /**
         * Main execution method for the client handler. Manages client requests and responses
         * throughout the lifetime of the connection. Ensures proper resource cleanup after
         * connection termination.
         */
        public void run() {

            try (DataInputStream input = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                 DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

                output.writeUTF("Connected to Fitting Room Server. Type 'ENTER' to enter a room, 'EXIT' to leave a room or 'OVER' to disconnect.");

                boolean running = true;
                while (running) {
                    try {
                        String clientMessage = input.readUTF();
                        if (clientMessage.equalsIgnoreCase("OVER")) {
                            running = false;
                            exitedProperly = true;
                        } else {
                            handleClientRequest(clientMessage, output);
                        }
                    } catch (SocketException e) {
                        LOGGER.log(Level.SEVERE, "Socket was closed unexpectedly", e);
                        running = false;
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "I/O error", e);
            } finally {

                releaseResources();
                if(exitedProperly) {
                
                	try {

                		clientSocket.close();

                	} catch (IOException e) {

                		LOGGER.log(Level.SEVERE, "Error closing socket", e);
                	}
                }
            }
        }
        
        /**
         * Releases resources allocated to a client, such as occupied rooms, and removes the
         * client from the waiting queue. Logs the resource release event.
         */
        private void releaseResources() {

            lock.lock();
            try {

                Integer roomNumber = socketToRoomMap.remove(clientSocket);
                if (roomNumber != null && roomNumber > 0) {
                    rooms[roomNumber] = false;
                }
                waitingQueue.remove(clientSocket);
                LOGGER.info("Resources released for client: " + clientSocket);
            } finally {

                lock.unlock();
            }
        }
        
        /**
         * Handles specific client requests such as entering or exiting rooms. Responds to the
         * client based on the current state of the fitting rooms and waiting queue.
         * 
         * @param request The client's request command.
         * @param output The data output stream to respond to the client.
         * @throws IOException if an I/O error occurs while sending the response.
         */
        private void handleClientRequest(String request, DataOutputStream output) throws IOException {
            lock.lock();
            try {
                if (request.equalsIgnoreCase("ENTER")) {
                    int roomNumber = findFreeRoom();
                    if (roomNumber != -1) {
                        output.writeUTF("Client has entered room " + roomNumber);
                    } else if (waitingQueue.size() < MAX_WAITING_ROOM) {
                        waitingQueue.add(clientSocket);
                        output.writeUTF("All rooms are occupied. You have been added to the waiting queue.");
                    } else {
                        output.writeUTF("Both fitting rooms and waiting room are full. Please try again later.");
                    }
                } else if (request.equalsIgnoreCase("EXIT")) {
                    boolean success = leaveRoom();
                    if (success) {
                        output.writeUTF("You have exited the room.");
                        exitedProperly = true;
                        tryAssignRoom();
                    } else {
                        output.writeUTF("Error: You were not in a room.");
                    }
                } else {
                    output.writeUTF("Invalid command.");
                }
            } finally {
                lock.unlock();
            }
        }

        
        /**
         * Attempts to find a free fitting room. If a room is available, it assigns the room to
         * the client and marks it as occupied.
         * 
         * @return The room number that was assigned, or -1 if no rooms are available.
         */
        private int findFreeRoom() {
            for (int i = 1; i <= MAX_FITTING_ROOMS; i++) {
                if (!rooms[i]) {
                    rooms[i] = true;
                    socketToRoomMap.put(clientSocket, i);
                    return i;
                }
            }
            return -1;
        }
        
        /**
         * Attempts to release a room that a client is exiting. Updates internal tracking to
         * reflect the room's new available status.
         * 
         * @return {@code true} if the room was successfully released, {@code false} otherwise.
         */
        private boolean leaveRoom() {
            Integer roomNumber = socketToRoomMap.get(clientSocket);
            if (roomNumber != null && rooms[roomNumber]) {
                rooms[roomNumber] = false;
                socketToRoomMap.remove(clientSocket);
                return true;
            }
            return false;
        }
        
        /**
         * Tries to assign a room to the next client in the waiting queue if a room becomes
         * available. Logs the event of moving a client from the queue to a room.
         */
        private void tryAssignRoom() {
            if (!waitingQueue.isEmpty()) {
                int roomNumber = findFreeRoom();
                if (roomNumber != -1) {
                    Socket client = waitingQueue.poll();
                    socketToRoomMap.put(client, roomNumber);
                    try (DataOutputStream output = new DataOutputStream(client.getOutputStream())) {
                        output.writeUTF("A room is now free. You have entered room " + roomNumber);
                        LOGGER.info("Client moved from waiting queue to room number " + roomNumber);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error moving client from queue", e);
                    }
                }
            }
        }
    }
}
