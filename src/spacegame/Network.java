package spacegame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

enum PacketType{
	PACKET_CONNECT,				// server->client server data
	PACKET_SERVER_FULL,			// server->client server full
	PACKET_CLIENT_UPDATE,		// client->server ship position/rotation/etc
	PACKET_SERVER_UPDATE,		// server->client ship positions/rotations/etc
	PACKET_SHIP_CHANGE,			// (both ways)    ship change
	PACKET_DAMAGE,			// (both ways)    ship take damage
}

/**
 * The class that the rest of the code uses to do networking
 * @author Trevor
 *
 */
public class Network{
	public static NetworkServer server;
	public static LocalClient client;
	
	static Thread clientUpdateThread;
	static Thread serverUpdateThread;
	
	public static void update(float delta){
    	// network update
    	if (server == null && client != null && client.running && System.currentTimeMillis() - client.lastUpdateSent > 10)
			client.sendPositionUpdate();
    	if (server != null && server.running && System.currentTimeMillis() - server.lastUpdateSent > 10)
    		server.sendPositionUpdate();
	}
}

/**
 * A server
 * @author Trevor
 */
class NetworkServer{
	public ServerSocket socket;
	boolean running = false;
	
	Thread connectThread;
	
	long lastUpdateSent;
	
	public NetworkServer(int port) throws IOException{
		socket = new ServerSocket(port);
	}
	
	/*
	 * Sends updates out to each client
	 */
	public void sendPositionUpdate(){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null){
				if (i != SpaceGame.myShip) // Don't send updates to ourself
					Ship.ships[i].client.sendPositionUpdate();
			}
		}
		lastUpdateSent = System.currentTimeMillis();
	}
	
	/**
	 * Starts the server, creating listenThread
	 */
	public void start(){
		if (!running){
			running = true;
			// Start a listen loop on listenThread
			connectThread = new Thread(){
				public void run(){
					while (running){
						ServerClient client = null;
						try {
							// Try to receive a client
							Socket sock = socket.accept();
							System.out.println("Server: Accepted connection");
							client = new ServerClient();
							client.socket = sock;
							client.dataIn = new DataInputStream(sock.getInputStream());
							client.dataOut = new DataOutputStream(sock.getOutputStream());
							
							int stype = client.dataIn.readInt();
							
							// Client connected, find a slot in Ship.ships and assign it
							Ship ship = null;
							for (int i = 0; i < Ship.ships.length; i++){
								if (Ship.ships[i] == null){
									ship = new Ship(stype);
									ship.client = client;
									ship.id = i;
									break;
								}
							}
							if (ship == null){
								// Server full
								try{
									client.dataOut.writeInt(PacketType.PACKET_SERVER_FULL.ordinal());
									sock.close();
								}catch(IOException e){
									System.out.println("Server: Failed to refuse client (full) " + e);
								}
							} else {
								try{
									client.dataOut.writeInt(PacketType.PACKET_CONNECT.ordinal());
									// Send the player their ship id
									client.dataOut.writeInt(ship.id);
									for (int i = 0; i < Ship.ships.length; i++)
										if (Ship.ships[i] != null){
											client.dataOut.writeInt(i);
											client.dataOut.writeInt(Ship.ships[i].ShipType);
										}
									// Send the player other ship information
									client.dataOut.writeInt(-1);

									client.running = true;
									client.ship = ship;
									Ship.ships[ship.id] = ship;
									
									System.out.println("Server: Connection " + ship.id + " established (" + client.dataIn.available() + ")");
									
									// send data about all the bodies
									for (int i = 0; i < Body.bodies.length; i++){
										Body b = Body.bodies[i];
										if (b != null){
											client.dataOut.writeInt(i);

											client.dataOut.writeBoolean(b instanceof Asteroid);
											client.dataOut.writeFloat(b.Position.x);
											client.dataOut.writeFloat(b.Position.y);
											client.dataOut.writeFloat(b.Velocity.x);
											client.dataOut.writeFloat(b.Velocity.y);
											client.dataOut.writeFloat(b.Rotation);
											client.dataOut.writeFloat(b.AngularVelocity);
											client.dataOut.writeFloat(b.Mass);
											client.dataOut.writeFloat(b.Radius);
											client.dataOut.writeBoolean(b.Anchored);
											client.dataOut.writeBoolean(b.Collidable);
											client.dataOut.writeBoolean(b.Gravity);
											client.dataOut.writeInt(b.zIndex);
										}
									}
									client.dataOut.writeInt(-1);
									
									client.start();
								} catch (IOException e){
									System.out.println("Server: Failed to finalize connection " + e);
									try{
										sock.close();
									} catch (IOException e2){
										System.out.println("Server: Failed to close client socket");
									}
								}
							}
						} catch (IOException e){
							System.out.println("Server: Failed to receive a connection");
							((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.CONNECTION_FAILED);
							stopClient(); // server can't receive a connection, stop trying
						}
					}
				}
			};
			connectThread.start();
		}
	}
	
	/**
	 * Stops the server and interrupts listenThread
	 */
	public void stopClient(){
		running = false;
		if (connectThread != null)
			connectThread.interrupt();
	}
	
	public boolean isRunning(){
		return running;
	}
}

abstract class NetworkClient {
	/**
	 * Ping, in nanoseconds
	 */
	public long ping;
	public long lastPacket;
	public Socket socket;
	public DataInputStream dataIn;
	public DataOutputStream dataOut;

	Thread receiveThread;
	
	Ship ship;
	
	boolean running = false;
	
	abstract void sendPacket(PacketType type) throws IOException;
	abstract void receivePacket() throws IOException;
	
	abstract void stop();
	
	abstract void sendPositionUpdate();
}

/**
 * A server client (exists on the server, lets server talk and listen to each client)
 * @author Trevor
 */
class ServerClient extends NetworkClient {
	public void sendPositionUpdate(){
		try {
			sendPacket(PacketType.PACKET_SERVER_UPDATE);
		} catch (IOException e) {
			stop();
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	void sendPacket(PacketType type) throws IOException{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DataOutputStream dOut = new DataOutputStream(bOut);
		
		dOut.writeInt(type.ordinal());
		switch (type){
		case PACKET_SERVER_UPDATE:{
			for (int i = 0; i < Ship.ships.length; i++){
				if (Ship.ships[i] != null){
					dOut.writeInt(i);
					dOut.writeFloat(Ship.ships[i].Position.x);
					dOut.writeFloat(Ship.ships[i].Position.y);
					dOut.writeFloat(Ship.ships[i].Velocity.x);
					dOut.writeFloat(Ship.ships[i].Velocity.y);
					dOut.writeFloat(Ship.ships[i].Rotation);
					dOut.writeFloat(Ship.ships[i].AngularVelocity);
					byte tf = 0;
					if (Ship.ships[i].Thrusting) tf |= 1;
					if (Ship.ships[i].Firing) tf |= 2;
					dOut.writeByte(tf);
					dOut.writeFloat(Ship.ships[i].Health);
					dOut.writeFloat(Ship.ships[i].Shield);
				}
			}
			dOut.writeInt(-1);
			
			for (int i = 0; i < Body.bodies.length; i++){
				Body b = Body.bodies[i];
				if (b != null){
					dOut.writeInt(i);
					dOut.writeFloat(b.Position.x);
					dOut.writeFloat(b.Position.y);
					dOut.writeFloat(b.Velocity.x);
					dOut.writeFloat(b.Velocity.y);
					dOut.writeFloat(b.Rotation);
					dOut.writeFloat(b.AngularVelocity);
				}
			}
			dOut.writeInt(-1);
			break;
		}
		}
		
		byte[] buf = bOut.toByteArray();
		dOut.close();
		
		dataOut.writeInt(buf.length);
		dataOut.write(buf);

		dataOut.flush();
	}

	@SuppressWarnings("incomplete-switch")
	void receivePacket() throws IOException {
		while (dataIn.available() == 0){
			Thread.yield();
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) { if (!running) break;}
		}
		if (!running) return;
		
		int len = dataIn.readInt();
		byte[] data = new byte[len];
		dataIn.read(data, 0, len);
		
		DataInputStream dIn = new DataInputStream(new ByteArrayInputStream(data));
		
		PacketType type = PacketType.values()[dIn.readInt()];
		switch (type){
		case PACKET_CLIENT_UPDATE:{
			Vector2 pos = new Vector2(dIn.readFloat(), dIn.readFloat());
			Vector2 vel = new Vector2(dIn.readFloat(), dIn.readFloat());
			float rot = dIn.readFloat();
			float angvel = dIn.readFloat();
			byte thrustFire = dIn.readByte();
			float health = dIn.readFloat();
			float shield = dIn.readFloat();
			
			if (ship != null && ship.id != SpaceGame.myShip){ // because the server still has a client with the same Ship.ships
				ship.Position = pos;
				ship.Velocity = vel;
				ship.Rotation = rot;
				ship.AngularVelocity = angvel;
				ship.Thrusting = (thrustFire & 1) > 0;
				ship.Firing = (thrustFire & 2) > 0;
				ship.Health = health;
				ship.Shield = shield;
			}
			break;
		}
		}
	}
	
	public void start(){
		// Start the client's update thread (receive data from client)
		receiveThread = new Thread(){
			public void run(){
				while (running && !socket.isClosed()){
					try {
						receivePacket();
					} catch (IOException e) {
						System.out.println("Server: Failed to communicate with client (" + ship.id + ")");
						Ship.ships[ship.id] = null;
						running = false;
						try {
							socket.close();
						} catch (IOException e1) { }
					}
				}
			}
		};
		receiveThread.start();
	}

	public void stop(){
		running = false;
		if (receiveThread != null)
			receiveThread.interrupt();
		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Client: Failed to close socket");
				e.printStackTrace();
			}
	}
}

/**
 * A local client (exists on each client, 1 per client)
 * @author Trevor
 */
class LocalClient extends NetworkClient {
	boolean receiving = false;
	
	long lastUpdateSent = 0;
	public void sendPositionUpdate(){
		try {
			sendPacket(PacketType.PACKET_CLIENT_UPDATE);
		} catch (IOException e) {
			stop();
		}
		lastUpdateSent = System.currentTimeMillis();
	}
	
	@SuppressWarnings("incomplete-switch")
	public void sendPacket(PacketType type) throws IOException{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DataOutputStream dOut = new DataOutputStream(bOut);
		
		dOut.writeInt(type.ordinal());
		
		switch (type){
		case PACKET_CLIENT_UPDATE:
			dOut.writeFloat(ship.Position.x);
			dOut.writeFloat(ship.Position.y);
			dOut.writeFloat(ship.Velocity.x);
			dOut.writeFloat(ship.Velocity.y);
			dOut.writeFloat(ship.Rotation);
			dOut.writeFloat(ship.AngularVelocity);
			byte tf = 0;
			if (ship.Thrusting) tf |= 1;
			if (ship.Firing) tf |= 2;
			dOut.writeByte(tf);
			dOut.writeFloat(ship.Health);
			dOut.writeFloat(ship.Shield);
			break;
		}

		byte[] buf = bOut.toByteArray();
		dOut.close();
		bOut.close();
		
		dataOut.writeInt(buf.length);
		dataOut.write(buf);
		dataOut.flush();
	}
	
	@SuppressWarnings("incomplete-switch")
	public void receivePacket() throws IOException{
		while (dataIn.available() == 0){
			Thread.yield();
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) { if (!running) break;}
		}
		if (!running) return;
		
		int len = dataIn.readInt();
		byte[] data = new byte[len];
		dataIn.read(data, 0, len);
		
		DataInputStream dIn = new DataInputStream(new ByteArrayInputStream(data));
		
		PacketType type = PacketType.values()[dIn.readInt()];
		switch (type){
		case PACKET_SERVER_UPDATE:{
			int i = 0;
			while ((i = dIn.readInt()) != -1){
				Vector2 pos = new Vector2(dIn.readFloat(), dIn.readFloat());
				Vector2 vel = new Vector2(dIn.readFloat(), dIn.readFloat());
				float rot = dIn.readFloat();
				float angvel = dIn.readFloat();
				byte thrustFire = dIn.readByte();
				float health = dIn.readFloat();
				float shield = dIn.readFloat();

				if (Ship.ships[i] != ship){
					Ship.ships[i].Position = pos;
					Ship.ships[i].Velocity = vel;
					Ship.ships[i].Rotation = rot;
					Ship.ships[i].AngularVelocity = angvel;
					Ship.ships[i].Thrusting = (thrustFire & 1) > 0;
					Ship.ships[i].Firing = (thrustFire & 2) > 0;
					Ship.ships[i].Health = health;
					Ship.ships[i].Shield = shield;
				}
			}
			i = 0;
			while ((i = dIn.readInt()) != -1){
				Vector2 pos = new Vector2(dIn.readFloat(), dIn.readFloat());
				Vector2 vel = new Vector2(dIn.readFloat(), dIn.readFloat());
				float rot = dIn.readFloat();
				float angvel = dIn.readFloat();

				Body b = Body.bodies[i];
				if (b != null){
					b.Position = pos;
					b.Velocity = vel;
					b.Rotation = rot;
					b.AngularVelocity = angvel;
				}
			}
			break;
		}
		}
	}
	
	public void stop(){
		running = false;
		if (receiveThread != null)
			receiveThread.interrupt();
		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Client: Failed to close socket");
				e.printStackTrace();
			}
	}
	
	public void connect(final String host, final int port, final int selectedShip){
			receiveThread = new Thread(){
				public void run(){
					try {
						socket = new Socket(host, port);
						dataIn = new DataInputStream(socket.getInputStream());
						dataOut = new DataOutputStream(socket.getOutputStream());
						
						System.out.println("Client: connected to " + host);
						dataOut.writeInt(selectedShip);
						
						// Receive connection data
						
						// Receive connection data
						if (dataIn.readInt() == 0){
							// Receive player data
							int id = dataIn.readInt();

							Ship.ships = new Ship[Ship.ships.length]; // reset ships
							int i = 0;
							while ((i = dataIn.readInt()) != -1)
								Ship.ships[i] = new Ship(dataIn.readInt());
							
							Ship.ships[id] = new Ship(selectedShip);
							ship = Ship.ships[id];
							ship.id = id;
							SpaceGame.myShip = id;
							System.out.println("Client: I am " + id);
							
							// Receive body data
							i = 0;
							Body.bodies = new Body[Body.bodies.length];
							while ((i = dataIn.readInt()) != -1){
								Body b = new Body();
								if (dataIn.readBoolean())
									b = new Asteroid(0, 0);
								else
									b.sprite = ContentLoader.planetTexture;
								b.Position = new Vector2(dataIn.readFloat(), dataIn.readFloat());
								b.Velocity = new Vector2(dataIn.readFloat(), dataIn.readFloat());
								b.Rotation = dataIn.readFloat();
								b.AngularVelocity = dataIn.readFloat();
								b.Mass = dataIn.readFloat();
								b.Radius = dataIn.readFloat();
								b.Anchored = dataIn.readBoolean();
								b.Collidable = dataIn.readBoolean();
								b.Gravity = dataIn.readBoolean();
								b.zIndex = dataIn.readInt();
								Body.bodies[i] = b;
							}
						}
						
						((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.INGAME);
						
						System.out.println("Client: Established connection (" + dataIn.available() + ")");

						// MAIN CLIENT LOOP
						running = true;
						while (!socket.isClosed() && running) {
							try {
								receivePacket();
								ping = System.currentTimeMillis() - lastPacket;
								lastPacket = System.currentTimeMillis();
							} catch (IOException e) {
								System.out.println("Client: Error: " + e.toString());
								running = false;
								try {
									socket.close();
								} catch (IOException e1) {
									System.out.println("Client: Failed to close socket");
								}
								((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.CONNECTION_FAILED);
							}
						}
					} catch (IOException e) {
						running = false;
						System.out.println("Client: Failed to connect");
						((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.CONNECTION_FAILED);
					}
				}
			};
			receiveThread.start();
	}
}