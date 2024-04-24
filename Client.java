import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    private static DataInputStream in = null;
    private static BufferedReader input = null;
    private static DataOutputStream out = null;

	
	public static void main(String[] args) {
        //Controller IP
		String host = "192.168.0.0" ;

		int port = 32005;
		try {
			
			//Connects to Sever
			Socket s = new Socket(host, port);
			//Message From User
			input = new BufferedReader(new InputStreamReader(System.in));
			//Message sent to Server.
			out = new DataOutputStream(s.getOutputStream());
			out.writeUTF("client");
			
			// Message From Server
			in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
			
		

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
