import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    private static DataInputStream in = null;
    private static BufferedReader input = null;
    private static DataOutputStream out = null;

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		String host = "localhost";

		int port = 32005;
        System.out.println("Hi");
		try {
			
			//Connects to Sever
			Socket s = new Socket(host, port);
			
			// Message From Server
			in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			//Message From User
			input = new BufferedReader(new InputStreamReader(System.in));
			
			//Message sent to Server.
			out = new DataOutputStream(s.getOutputStream());

			String message = in.readUTF();
			System.out.println(message);
			
			String line = "";
			
			while(true) {
				line = input.readLine();
				out.writeUTF(line);
				if(line.equalsIgnoreCase("OVER")) {
					break;
				}
				message = in.readUTF();
				System.out.println(message);
			}

			
			input.close();
			in.close();
			s.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
