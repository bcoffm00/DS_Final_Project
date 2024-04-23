import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class FittingRoom {
    private static final int PORT = 32005;
    private static final int MAX_FITTING_ROOMS = 4;
    private static boolean[] rooms = new boolean[MAX_FITTING_ROOMS]; // true if occupied, false if free
    private static ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(10); // Pool for handling multiple clients

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

                output.writeUTF("Connected to Fitting Room Server. Type 'ENTER' to enter a room or 'EXIT' to leave.");

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
                int roomNumber = findFreeRoom();
                if (roomNumber != -1) {
                    output.writeUTF("You have entered room " + roomNumber);
                } else {
                    output.writeUTF("No free rooms available, please wait.");
                }
            } else if (request.equalsIgnoreCase("EXIT")) {
                boolean success = leaveRoom();
                if (success) {
                    output.writeUTF("You have exited the room.");
                } else {
                    output.writeUTF("Error: You were not in a room.");
                }
            } else {
                output.writeUTF("Invalid command.");
            }
        }

        private int findFreeRoom() {
            lock.lock();
            try {
                for (int i = 0; i < MAX_FITTING_ROOMS; i++) {
                    if (!rooms[i]) {
                        rooms[i] = true;
                        return i;
                    }
                }
                return -1; // no free room found
            } finally {
                lock.unlock();
            }
        }

        private boolean leaveRoom() {
            lock.lock();
            try {
                for (int i = 0; i < MAX_FITTING_ROOMS; i++) {
                    if (rooms[i]) {
                        rooms[i] = false;
                        return true;
                    }
                }
                return false; // no room was occupied
            } finally {
                lock.unlock();
            }
        }
    }
}
