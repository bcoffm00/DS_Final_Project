
/********************************************************
 * Name:   		Brody Coffman, Tony Aldana and Yash Patel
 * Problem Set:	Final Group project
 * Due Date :	5/2/24
 *******************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * SemaphoreFR is a fitting room management system that utilizes Semaphores for
 * concurrency control. It includes a main server component to handle fitting
 * room operations and client connections. The system consists of fitting rooms
 * where clients can enter, wait, and exit as needed.
 */
public class FittingRoom{

    private static final Logger LOGGER = Logger.getLogger(FittingRoom.class.getName());
    static {
        setupLogger();
    }

    /**
     * Configures the server's logging system. Logs are written to "FittingRoom.log"
     * file.
     */

    private static void setupLogger() {
        try {
            LogManager.getLogManager().reset();
            Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
            FileHandler fh = new FileHandler("FittingRoom.txt", true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting up logger", e);
        }
    }

    /** IP address of the central server */
    final static String CENTRAL_IP = "192.168.0.0";

    /** Port number of the central server */
    final static int CENTRAL_PORT = 32005;

    /**
     * The main method initializes the fitting room system, connects to the central
     * server, and starts the server socket to listen for incoming client
     * connections.
     *
     * @param args Command-line arguments specifying the number of fitting rooms
     */
    public static void main(String[] args) {
        // Sends the message "SERVER" to the central server to indicate this is a
        // fitting room
        initConnect();
        ServerSocket serverSock;
        try {
            // Started the server socket
            serverSock = new ServerSocket(CENTRAL_PORT);
            FitRoom FittingRoom = new FitRoom(Integer.parseInt(args[0]));
            // Main loop to rip threads
            while (true) {
                Socket newCon = serverSock.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(newCon.getInputStream()));
                String line = br.readLine();

                // If its a heartbeat socket
                if (line.equalsIgnoreCase("HEARTBEAT")) {
                    // CHECK THE TWO SEMA's AND PASS BACK DATA
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

            // END OF TRY
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred", ex);
        }

        // END OF MAIN
    }

    /**
     * Sends the current state of the fitting rooms to the requesting client.
     *
     * @param a The FitRoom instance
     * @return A string representing the state of the fitting rooms
     */
    private static String bloodTest(FitRoom a) {
        return a.bloodTest();
    }

    /**
     * Sends a message to the central server indicating that this is a fitting room.
     */
    private static void initConnect() {
        try {
            Socket centralServer = new Socket(CENTRAL_IP, CENTRAL_PORT);
            PrintWriter toConnect = new PrintWriter(centralServer.getOutputStream());

            toConnect.println("SERVER");
            toConnect.flush();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error connecting to central server", ex);
        }
    }

    /**
     * Represents a client connection thread to handle communication with the
     * central server.
     */
    @SuppressWarnings("unused")
    private static class fitConnection extends Thread {
        private FitRoom par = null;
        private Socket connection = null;
        private PrintWriter toConnect = null;
        private BufferedReader fromConnect = null;

        /**
         * Constructs a fitConnection object with the given socket and FitRoom instance.
         *
         * @param newSock The client socket
         * @param b       The FitRoom instance
         */
        public fitConnection(Socket newSock, FitRoom b) {
            this.par = b;

            try {
                this.connection = newSock;

                this.fromConnect = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
                this.toConnect = new PrintWriter(this.connection.getOutputStream());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error initializing connection", ex);
            }
        }

        /**
         * Sends a message to the central server.
         *
         * @param message The message to send
         * @return True if the message is successfully sent, otherwise false
         */
        public boolean sendMessage(String message) {
            // Messaging handler for modularization
            try {
                LOGGER.info("Attempting to send message, [" + message + "] to central server");
                // Attempts to send a message via the print writer

                this.toConnect.println(message.toUpperCase());
                this.toConnect.flush();

                LOGGER.info("Successfully sent message, [" + message + "] to central server");
                // If message sends successfully then return true
                return true;
            } catch (Exception e) {
                // If message fails to send then return false
                LOGGER.info("Failed to send message, [" + message + "] to central server");
                return false;
            }
        }

        /**
         * Reads a message from the central server.
         *
         * @return The message read from the server
         */
        public String readMessage() {
            String message = "";
            // Message receiver handler for modularization
            try {
                LOGGER.info("Reading message from central server");
                // attempts to read in message via bufferedReader

                message = this.fromConnect.readLine();

                // If message is read successfully then return message
                LOGGER.info("Received message from central server: " + message);
                return message;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error reading message from central server", ex);
                // If message fails to be read then return empty string
                return message;
            }
        }

        /**
         * Starts the thread's execution.
         */
        public void run() {
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
                            while (!this.par.compare(this)) {
                            }
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
                                }
                                if (this.fromConnect != null) {
                                    try {
                                        this.fromConnect.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (this.connection != null) {
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
                LOGGER.log(Level.SEVERE, "Exception occurred", ex);
            }
        }

    }

    /**
     * Represents a semaphore-based change room.
     */
    private static class ChangeRoom {
        private int numRooms;
        private Semaphore fitRooms = null;

        /**
         * Constructs a ChangeRoom object with the given number of rooms.
         *
         * @param a The number of rooms
         */

        public ChangeRoom(int a) {
            this.numRooms = a;
            this.fitRooms = new Semaphore(a);
        }

        public boolean tryAcquire() {
            return this.fitRooms.tryAcquire();
        }

        public void release() {
            this.fitRooms.release();
        }

        public int stateCheck() {
            return this.fitRooms.availablePermits();
        }
    }

    /**
     * Represents a waiting room queue for clients waiting to enter the fitting
     * rooms.
     */
    private static class WaitRoom {
        private int numRooms;
        private Semaphore access = new Semaphore(1);
        private LinkedList<fitConnection> waitRooms = new LinkedList<>();

        /**
         * Constructs a WaitRoom object with the given number of rooms.
         *
         * @param a The number of rooms
         */
        public WaitRoom(int a) {
            this.numRooms = a;
        }

        public boolean contains(fitConnection a) {
            while (!access.tryAcquire()) {}
            boolean contains = this.waitRooms.contains(a);
            access.release();
            return contains;
        }

        public boolean enQueue(fitConnection a) {
            while (!access.tryAcquire()) {}
            if (this.waitRooms.size() < this.numRooms) {
                this.waitRooms.add(a);
                access.release();
                return true;
            }
            access.release();
            return false;
        }

        public fitConnection peek() {
            while (!access.tryAcquire()) {}
            fitConnection a = this.waitRooms.peek();
            access.release();
            return a;
        }

        public fitConnection deQueue() {
            while (!access.tryAcquire()) {}
            if (this.waitRooms.isEmpty()) {
                access.release();
                return null;
            } else {
                fitConnection a = this.waitRooms.remove();
                access.release();
                return a;
            }
        }

        public int stateCheck() {
            while (!access.tryAcquire()) {}
            int a = this.waitRooms.size();
            access.release();
            return a;
        }

    }

    /**
     * Represents a fitting room containing a waiting room and a change room.
     */
    private static class FitRoom {
        private WaitRoom WaitingRooms = null;
        private ChangeRoom ChangingRooms = null;

        /**
         * Constructs a FitRoom object with the given number of fitting rooms.
         *
         * @param a The number of fitting rooms
         */
        public FitRoom(int a) {
            this.WaitingRooms = new WaitRoom(a * 2);
            this.ChangingRooms = new ChangeRoom(a);
        }

        public String bloodTest() {
            return this.WaitingRooms.stateCheck() + "," + this.ChangingRooms.stateCheck();
        }

        public String enter(fitConnection a) {
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

        public boolean compare(fitConnection a) {
            if (this.WaitingRooms.peek().equals(a)) {
                return true;
            } else {
                return false;
            }
        }

        public String exit() {
            this.ChangingRooms.release();
            return "RECEIVED";
        }
    }
}
