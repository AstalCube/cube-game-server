package server.world;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import server.client.ClientManager;

import common.Vector3;
import common.Vector3f;

public class World {
	
	// World dimensions
	private Vector3 size;
	private Vector3f cubeSize;
	
	// Array containing integers which defines each cube
	private volatile char[][][] cubes;
	
	// Client manager
	private ClientManager clientManager;

	public World(Vector3f cubeSize, ClientManager clientManager) {
		this.cubeSize = cubeSize;
		this.clientManager = clientManager;
	}
	
	public void generateTerrain(Vector3 size, int minHeight, int maxHeight, float resolution, long seed) {
		this.size = size;
		
		// Specify the seed to use
		SimplexNoise.genGrad(seed);
		
		// Generate a heightmap
		int heightMap[][] = new int[size.x][size.z];
		
		for(int z = 0; z < size.z; z++) {
			for(int x = 0; x < size.x; x++) {
				heightMap[x][z] = (int) (((SimplexNoise.noise(x / resolution, z / resolution) + 1.0f) / 2.0f) * (maxHeight - minHeight) + minHeight);
			}
		}
		
		// Create the cube array
		cubes = new char[size.x][size.y][size.z];
		
		// Fill the array
		for(int z = 0; z < size.z; z++) {
			for(int x = 0; x < size.x; x++) {
				// Create cubes up to heightMap[x][z]
				for(int y = 0; y < heightMap[x][z]; y++) {
					if(y == 0) {
						cubes[x][y][z] = CubeType.DIRT;
					} else if(y < 5) {
						cubes[x][y][z] = CubeType.STONE;
					} else if(y < 10) {
						cubes[x][y][z] = CubeType.WATER;
					} else {
						cubes[x][y][z] = CubeType.GRASS;
					}
				}
				
				// Set the rest to empty cubes
				for(int y = heightMap[x][z]; y < size.y; y++) {
					cubes[x][y][z] = CubeType.EMPTY;
				}
			}
		}
		
		// Create an even water level
		for(int z = 0; z < size.z; z++) {
			for(int x = 0; x < size.x; x++) {
				for(int y = 0; y < 5; y++) {
					if(cubes[x][y][z] == CubeType.EMPTY)
						cubes[x][y][z] = CubeType.WATER;
				}
				
				for(int y = 5; y < 10; y++) {
					cubes[x][y][z] = CubeType.WATER;
				}
			}
		}
		
		// Broadcast to connected clients if there is any
		clientManager.broadcast(getWorldString());
	}
	
	public void load(String filename) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename))));
			
			// Read the world size
			StringTokenizer tokenizer = new StringTokenizer(in.readLine());
			size = new Vector3(Integer.valueOf(tokenizer.nextToken()), 
							Integer.valueOf(tokenizer.nextToken()), 
							Integer.valueOf(tokenizer.nextToken()));
			cubes = new char[size.x][size.y][size.z];
			
			// Save the world
			for(int x = 0; x < size.x; x++) {
				for(int y = 0; y < size.y; y++) {
					for(int z = 0; z < size.z; z++) {
						int iType = in.read();
						char type = (char) iType;
						cubes[x][y][z] = type;
					}
				}
			}
			
			in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		// Broadcast to connected clients if there is any
		clientManager.broadcast(getWorldString());
	}
	
	public void save(String filename) {
		try {
			PrintWriter out = new PrintWriter(new FileOutputStream(new File(filename)));
			
			// Save the world size as a string with a \n at the end
			out.println(size.x + " " + size.y + " " + size.z);
			
			// Save the world
			for(int x = 0; x < size.x; x++) {
				for(int y = 0; y < size.y; y++) {
					for(int z = 0; z < size.z; z++) {
						out.write(cubes[x][y][z]);
					}
				}
			}
			
			out.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setCube(Vector3 position, char type) {
		if(position.x >= 0 && position.x < size.x &&
		   position.y >= 0 && position.y < size.y &&
		   position.z >= 0 && position.z < size.z) {
			// Set the cube type
			cubes[position.x][position.y][position.z] = type;
		
			// Broadcast
			int iType = type;
			clientManager.broadcast("CUBE " + position.x + " " + position.y + " " + position.z + " " + iType);
		}
	}
	
	public String getWorldString() {
		// Add the WORLD word and the size
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("WORLD " + size.x + " " + size.y + " " + size.z + " ");
		
		// Add data for each cube
		for(int x = 0; x < size.x ; x++) {
			for(int y = 0; y < size.y; y++) {
				for(int z = 0; z < size.z; z++) {
					int iType = cubes[x][y][z];
					strBuilder.append(iType + " ");
				}
			}
		}
		
		return strBuilder.toString();
	}
}
