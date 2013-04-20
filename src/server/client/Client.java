package server.client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.StringTokenizer;

import server.Server;
import server.world.World;

import common.Vector3;
import common.Vector3f;

public class Client {

	private Socket sock;
	private BufferedReader in;
	private PrintWriter out;
	private Queue<String> writeQueue;
	
	// Threads
	private Thread readThread;
	private Thread writeThread;
	private volatile boolean running = true;
	
	// Client manager
	private ClientManager clientManager;
	
	// World
	private World world;
	
	// Server
	private Server server;
	
	// Player
	private volatile int id = -1;
	private volatile Vector3f coordinates;
	private volatile Vector3f rotation;
	
	public Client(Socket clientSock, ClientManager clientMan, World w, Server s) throws Exception {
		this.sock = clientSock;
		this.clientManager = clientMan;
		this.world = w;
		this.server = s;
		
		in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		out = new PrintWriter(sock.getOutputStream(), true);
		
		// TODO: find out if LinkedList is the best to use here
		writeQueue = new LinkedList<String>();
	}
	
	public void begin() {
		// To use in threads
		final Client thisClient = this;
		
		// Start the read thread
		readThread = new Thread() {
			@Override
			public void run() {
				while(running) {
					try {
						// Read a line
						String line = in.readLine();
						
						// Process the line
						StringTokenizer tokenizer = new StringTokenizer(line);
						String command = tokenizer.nextToken();
						
						if(command.equals("CUBE")) {
							Vector3 pos = new Vector3(Integer.valueOf(tokenizer.nextToken()),
									Integer.valueOf(tokenizer.nextToken()),
									Integer.valueOf(tokenizer.nextToken()));
							
							int iType = Integer.valueOf(tokenizer.nextToken());
							char type = (char) iType;
							
							// Set the cube type
							world.setCube(pos, type);
						} else if(command.equals("SERVCMD")) {
							// Get the substring
							String cmd = line.substring(line.indexOf(' ') + 1, line.length());
							String response = server.command(cmd);
							
							if(response != null)
								writeLine("CONSOLEMSG " + response);
						} else if(command.equals("POS")) {
							Vector3f pos = new Vector3f(Float.valueOf(tokenizer.nextToken()),
									Float.valueOf(tokenizer.nextToken()),
									Float.valueOf(tokenizer.nextToken()));
							
							Vector3f rot = new Vector3f(Float.valueOf(tokenizer.nextToken()),
									Float.valueOf(tokenizer.nextToken()),
									Float.valueOf(tokenizer.nextToken()));
							
							coordinates = pos;
							rotation = rot;
							
							clientManager.broadcastToAllExcept(getPos(), thisClient);
						}
						
					} catch (Exception e) {
						System.out.println("Client exception occured, closing connection");
						clientManager.remove(id);
						stop();
						
						clientManager.broadcastToAllExcept("REM " + thisClient.id, thisClient);
					}
				}
			}
		};
		
		readThread.start();
		
		// Start the write thread
		writeThread = new Thread() {
			@Override
			public void run() {
				while(running) {
					synchronized(writeQueue) {
						if(writeQueue.size() > 0) {
							String line = writeQueue.remove();
							out.println(line);
						}
					}
				}
			}
		};
		
		writeThread.start();
		
		// Send client id
		writeLine("ID " + id);
		
		// Send world data
		writeLine(world.getWorldString());
		
		// Get player position data
		for(Entry<Integer, Client> e : clientManager.getMap().entrySet()) {
			Client c = e.getValue();
			
			if(c != this)
				writeLine(c.getPos());
		}
		
		// Send camera position
		coordinates = new Vector3f(75, 34, 23);
		rotation = new Vector3f(-15, -160, 0);
		clientManager.broadcast(getPos());
	}
	
	public void stop() {
		running = false;
		
		// Force stop the threads
		readThread.stop();
		writeThread.stop();
		
		// Close the socket
		try {
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Socket getSock() {
		return sock;
	}
	
	public void writeLine(String line) {
		synchronized(writeQueue) {
			writeQueue.add(line);
		}
	}
	
	public Vector3f getCoordinates() {
		return coordinates;
	}
	
	public Vector3f getRotation() {
		return rotation;
	}
	
	public void setID(int id) {
		this.id = id;
	}
	
	public String getPos() {
		return "POS " + id + " " + coordinates.x + " " + coordinates.y + " " + coordinates.z + " " + rotation.x + " " + rotation.y + " " + rotation.z;
	}
}
