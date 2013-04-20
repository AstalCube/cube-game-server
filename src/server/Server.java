package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import server.client.Client;
import server.client.ClientManager;
import server.world.World;

import common.Vector3;


public class Server implements Runnable {
	
	// Maximum size of the queue of sockets trying to connect
	private static final int BACKLOG = 5;

	// Server socket which accepts incoming connections
	private ServerSocket serverSock;
	private int port;
	
	// Main thread
	private Thread mainThread;
	public volatile boolean running = true;
	
	// Keeps track of all clients
	private ClientManager clientManager;
	
	// The game world
	private World world;
	
	public Server(int port) {
		this.port = port;
		
		// Create the client manager
		clientManager = new ClientManager();
		
		// Generate the world
		System.out.print("Generating terrain... ");
		world = new World(null, clientManager);
		world.generateTerrain(new Vector3(256, 50, 256), 0, 40, 128.0f, 2);
		System.out.println(" DONE!");
	}
	
	public void start() throws Exception {
		// Start the server socket
		serverSock = new ServerSocket(port, BACKLOG);
		System.out.println("Server socket listening on port " + port);
		
		// Start the main thread
		mainThread = new Thread(this);
		mainThread.start();
		System.out.println("Started main thread");
	}
	
	public void stop() {
		running = false;
		
		// Force stop the main thread
		mainThread.stop();
		System.out.println("Stopped main thread");
		
		// Close the socket
		try {
			serverSock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Stop all connected clients
		clientManager.stopAll();
		System.out.println("Stopped client threads");
	}

	@Override
	public void run() {
		while(running) {
			// Accept new connections
			try {
				// Retrieve the socket and create the client object
				Socket clientSock = serverSock.accept();
				Client client = new Client(clientSock, clientManager, world, this);

				// Add the client to the client manager
				clientManager.add(client);
				
				// Start the client thread
				client.begin();
				
				// Print to console
				System.out.println("Accepted new client: " + client.getSock().getInetAddress().toString());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String command(String command) {
		try {
			StringTokenizer tokenizer = new StringTokenizer(command);
			String cmd = tokenizer.nextToken();
			
			if(cmd.equals("stop")) {
				stop();
			} else if(cmd.equals("list")) {
				String list = clientManager.getMap().size() + " connected clients: ";
				int count = 0;
				
				for(Entry<Integer, Client> e : clientManager.getMap().entrySet()) {
					list += e.getValue().getSock().getInetAddress().toString();
					
					if(count != clientManager.getMap().size() - 1)
						list += ' ';
					
					count++;
				}
				
				return list;
			} else if(cmd.equals("worldgen")) {
				// World info
				Vector3 size = new Vector3(Integer.valueOf(tokenizer.nextToken()),
										Integer.valueOf(tokenizer.nextToken()),
										Integer.valueOf(tokenizer.nextToken()));
				
				int minHeight = Integer.valueOf(tokenizer.nextToken());
				int maxHeight = Integer.valueOf(tokenizer.nextToken());
				
				float resolution = Float.valueOf(tokenizer.nextToken());
				
				long seed = Long.valueOf(tokenizer.nextToken());
				
				// Generate world
				long startTime = System.currentTimeMillis();
				world.generateTerrain(size, minHeight, maxHeight, resolution, seed);
				long endTime = System.currentTimeMillis();
				return "Generated terrain in " + (endTime - startTime) + " ms";
			} else if(cmd.equals("worldsave")) {
				String filename = tokenizer.nextToken();
				long startTime = System.currentTimeMillis();
				world.save(filename);
				long endTime = System.currentTimeMillis();
				return "World saved as: " + filename + " in " + (endTime - startTime) + " ms";
			} else if(cmd.equals("worldload")) {
				String filename = tokenizer.nextToken();
				long startTime = System.currentTimeMillis();
				world.load(filename);
				long endTime = System.currentTimeMillis();
				return "Loaded world: " + filename + " in " + (endTime - startTime) + " ms"; 
			}
		} catch(Exception e) {
			return e.toString();
		}
		
		return null;
	}
	
}
