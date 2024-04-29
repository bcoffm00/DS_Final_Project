import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Queue;
import java.util.LinkedList;

public class FittingRoom {
    private static final int PORT = 32005;
    private static final int MAX_FITTING_ROOMS = 4;
    private static final int MAX_WAITING_ROOM = MAX_FITTING_ROOMS * 2;
    private static boolean[] rooms = new boolean[MAX_FITTING_ROOMS + 1]; // Indexing from 1 for simplicity
    private static Queue<Socket> waitingQueue = new LinkedList<>();
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10); 

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started. Listening on Port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                executor.execute(new ClientHandler(socket));
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
            try (DataInputStream input = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                 DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

                output.writeUTF("Connected to Fitting Room Server. Type 'ENTER' to enter a room, 'EXIT' to leave a room or 'OVER' to disconnect.");

                boolean running = true;
                while (running) {
                    String clientMessage = input.readUTF();
                    if (clientMessage.equalsIgnoreCase("OVER")) {
                        running = false;
                    } else {
                        handleClientRequest(clientMessage, output);
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

        private void handleClientRequest(String request, DataOutputStream output) throws IOException {
            if (request.equalsIgnoreCase("ENTER")) {
                lock.lock();
                try {
                    int roomNumber = findFreeRoom();
                    if (roomNumber != -1) {
                        output.writeUTF("You have entered room " + roomNumber);
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
