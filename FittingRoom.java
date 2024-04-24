import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
        try {
			Socket s = new Socket("192.168.0.0",PORT);
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			DataInputStream input = new DataInputStream(s.getInputStream());
			out.writeUTF("Server");
			String line = input.readUTF();

			//sends fitting rooms
			if(line.contentEquals("ready")) {
				out.writeUTF(Integer.toString(MAX_FITTING_ROOMS));
			}
			line = input.readUTF();
			if(line.contentEquals("ready")) {
				out.writeUTF(Integer.toString(2 * MAX_FITTING_ROOMS));
			}
            input.close();
            out.close();
		} catch (UnknownHostException e1) {
			
			e1.printStackTrace();
		} catch (IOException e1) {
		
			e1.printStackTrace();
		}
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
                        //Sending Input instead
                        handleClientRequest(clientMessage,input, output);
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

        private void handleClientRequest(String request,DataInputStream input, DataOutputStream output) throws IOException {
            if (request.equalsIgnoreCase("ENTER")) {
                int roomNumber = findFreeRoom();
                if (roomNumber != -1) {
                    //ADDED CODE FOR CONTROLLER CHECK
                    output.writeUTF("Entered");
                    request = input.readUTF();
                    while(!request.equalsIgnoreCase("READY")){

                    }
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
