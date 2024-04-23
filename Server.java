import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

	public static void main(String[] args) throws IOException {

		int port = 32005;

		ServerSocket server = new ServerSocket(port);

		System.out.println("Server started");

		while (true) {
			try {

				Socket clientSocket = server.accept();

				ClientHandler x = new ClientHandler(clientSocket);
				x.incrementClients();

				x.start();

			} catch (Exception e) {
				server.close();
				e.printStackTrace();
			}
		}

	}

}

class ClientHandler extends Thread {
	static int ClientCount = 0;
	Socket clientSocket;
	public int id;

	public ClientHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
		id = ClientCount;
	}

	public void incrementClients(){
	ClientCount ++;
	}
	public void decrementClients(){
	ClientCount --;
	}

	@Override
	public void run() {
		try {
			System.out.println("New connection accepted from " + clientSocket.getInetAddress().getHostAddress());

			System.out.println("Hello and Welcome, client " + id);
			System.out.println(this.getName());


			decrementClients();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
