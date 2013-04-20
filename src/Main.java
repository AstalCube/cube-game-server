import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import server.Server;

public class Main {
	
	private static final int PORT = 6000;

	public static void main(String[] args) {
		// Start a server
		Server serv = new Server(PORT);
		
		try {
			serv.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Read command line input
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			
			while(serv.running) {
				String line = in.readLine();
				String response = serv.command(line);
				
				if(response != null)
					System.out.println(response);
			}
			
			in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}
