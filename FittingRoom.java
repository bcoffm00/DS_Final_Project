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

    private static class ClientHandler implements Runnable {

        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

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
                LOGGER.info("Resources released for client: " + clientSocket);
            } finally {

                lock.unlock();
            }
        }

        private void handleClientRequest(String request, DataOutputStream output) throws IOException {
            if (request.equalsIgnoreCase("ENTER")) {
                lock.lock();
                try {
                    int roomNumber = findFreeRoom();
                    if (roomNumber != -1) {
                        output.writeUTF("Client has entered room " + roomNumber);
                    } else if (waitingQueue.size() < MAX_WAITING_ROOM) {
                        waitingQueue.add(clientSocket);
                        output.writeUTF("All rooms are occupied. You have been added to the waiting queue.");
                    } else {
                        output.writeUTF("Both fitting rooms and waiting room are full. Please try again later.");
                    }
                } finally {
                    lock.unlock();
                }
            } else if (request.equalsIgnoreCase("EXIT")) {
                boolean success = leaveRoom();
                if (success) {
                    output.writeUTF("You have exited the room.");
                    tryAssignRoom();
                } else {
                    output.writeUTF("Error: You were not in a room.");
                }
            } else {
                output.writeUTF("Invalid command.");
            }
        }

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

        private boolean leaveRoom() {
            Integer roomNumber = socketToRoomMap.get(clientSocket);
            if (roomNumber != null && rooms[roomNumber]) {
                rooms[roomNumber] = false;
                socketToRoomMap.remove(clientSocket);
                return true;
            }
            return false;
        }

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
