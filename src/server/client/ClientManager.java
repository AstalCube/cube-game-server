package server.client;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ClientManager {

	private Map<Integer, Client> clientMap;
	
	public ClientManager() {
		// Create the client list
		clientMap = new HashMap<Integer, Client>();
	}
	
	public void add(Client client) {
		// Find a free ID
		int freeID = 0;
		
		while(clientMap.containsKey(freeID)) {
			freeID++;
		}
		
		clientMap.put(freeID, client);
		client.setID(freeID);
	}
	
	public void remove(int id) {
		clientMap.remove(id);
	}
	
	public Map<Integer, Client> getMap() {
		return clientMap;
	}
	
	public void stopAll() {
		for(Entry<Integer, Client> e : clientMap.entrySet()) {
			e.getValue().stop();
		}
	}
	
	public void broadcast(String line) {
		for(Entry<Integer, Client> e : clientMap.entrySet()) {
			e.getValue().writeLine(line);
		}
	}
	
	public void broadcastToAllExcept(String line, Client exception) {
		for(Entry<Integer, Client> e : clientMap.entrySet()) {
			if(e.getValue() != exception)
				e.getValue().writeLine(line);
		}
	}
}
