package spacegame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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
    	if (server == null && client != null && client.running)
			client.sendPositionUpdate();
    	if (server != null && server.running)
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
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
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
							sock.setTcpNoDelay(true);
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
									client.dataOut.flush();
									sock.close();
								}catch(IOException e){
									System.out.println("Server: Failed to refuse client (full) " + e);
								}
							} else {
								try{
									ByteArrayOutputStream bOut = new ByteArrayOutputStream();
									DataOutputStream dOut = new DataOutputStream(bOut);
									dOut.writeInt(PacketType.PACKET_CONNECT.ordinal());
									// Send the player their ship id
									dOut.writeInt(ship.id);
									for (int i = 0; i < Ship.ships.length; i++)
										if (Ship.ships[i] != null){
											dOut.writeInt(i);
											dOut.writeInt(Ship.ships[i].ShipType);
										}
									// Send the player other ship information
									dOut.writeInt(-1);
									client.dataOut.flush();

									// send data about all the bodies
									for (int i = 0; i < Body.bodies.length; i++){
										Body b = Body.bodies[i];
										if (b != null){
											dOut.writeInt(i);

											dOut.writeBoolean(b instanceof Asteroid);
											dOut.writeFloat(b.Position.x);
											dOut.writeFloat(b.Position.y);
											dOut.writeFloat(b.Velocity.x);
											dOut.writeFloat(b.Velocity.y);
											dOut.writeFloat(b.Rotation);
											dOut.writeFloat(b.AngularVelocity);
											dOut.writeFloat(b.Mass);
											dOut.writeFloat(b.Radius);
											dOut.writeBoolean(b.Anchored);
											dOut.writeBoolean(b.Collidable);
											dOut.writeBoolean(b.Gravity);
											dOut.writeInt(b.zIndex);
										}
									}
									dOut.writeInt(-1);
									client.dataOut.flush();
									
									byte[] buf = bOut.toByteArray();
									bOut.close();
									System.out.println("Server: sending " + buf.length + " data");
									client.dataOut.writeInt(buf.length);
									client.dataOut.write(buf);
									client.dataOut.flush();
									
									client.ship = ship;
									Ship.ships[ship.id] = ship;
									
									System.out.println("Server: Connection " + ship.id + " established (" + client.dataIn.available() + ")");
									
									int ack = client.dataIn.readInt();
									if (ack == 42){
										client.start();
										// TODO notify all Ship.ships that a new guy connected
									}else
										System.out.println("??? " + ack);
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
	public long TimeItTakesForAPacketToGetToTheServerFromTheClient;
	
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
			
			dOut.writeLong(System.currentTimeMillis()); // timestamp
			dOut.writeLong(TimeItTakesForAPacketToGetToTheServerFromTheClient); // latency
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
		byte[] buf = new byte[dataIn.readInt()];
		dataIn.read(buf, 0, buf.length);
		
		try{
		DataInputStream dIn = new DataInputStream(new ByteArrayInputStream(buf));
		
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
			
			TimeItTakesForAPacketToGetToTheServerFromTheClient = System.currentTimeMillis() - dIn.readLong();
			break;
		}
		}
		} catch (EOFException e){
			System.out.println("Server: Got malformed packet (" + buf.length + " bytes) at " + System.currentTimeMillis());
			e.printStackTrace();
			if (buf.length == 0)
				throw new IOException("Malformed packet");
		}
		
	}
	
	public void start(){
		// Start the client's update thread (receive data from client)
		receiveThread = new Thread(){
			public void run(){
				running = true;
				while (running && !socket.isClosed()){
					try {
						receivePacket();
					} catch (IOException e) {
						System.out.println("Server: Failed to communicate with client (" + ship.id + ")");
						e.printStackTrace();
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
		if (ship != null)
			Ship.ships[ship.id] = null;
	}
}

/**
 * A local client (exists on each client, 1 per client)
 * @author Trevor
 */
class LocalClient extends NetworkClient {
	boolean receiving = false;
	
	long TimeItTakesForAPacketToGetToTheClientFromTheServer;
	
	public void sendPositionUpdate(){
		try {
			sendPacket(PacketType.PACKET_CLIENT_UPDATE);
		} catch (IOException e) {
			stop();
		}
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
			
			dOut.writeLong(System.currentTimeMillis()); // timestamp
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
		byte[] buf = new byte[dataIn.readInt()];
		if (buf.length == 0)
			while (buf.length == 0)
				buf = new byte[dataIn.readInt()];
		dataIn.read(buf, 0, buf.length);
		
		try{
		DataInputStream dIn = new DataInputStream(new ByteArrayInputStream(buf));
		
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
			TimeItTakesForAPacketToGetToTheClientFromTheServer = System.currentTimeMillis() - dIn.readLong();
			long TimeItTakesForAPacketToGetToTheServerFromTheClient = dIn.readLong();
			ping = TimeItTakesForAPacketToGetToTheClientFromTheServer + TimeItTakesForAPacketToGetToTheServerFromTheClient;
			break;
		}
		}
		} catch (EOFException e){
			System.out.println("Client: Got malformed packet (" + buf.length + " bytes) at " + System.currentTimeMillis());
			e.printStackTrace();
			if (buf.length == 0) // server fucked up real good
				throw new IOException("Malformed packet");
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
						socket.setTcpNoDelay(true);
						System.out.println("Client: connected to " + host);
						dataIn = new DataInputStream(socket.getInputStream());
						dataOut = new DataOutputStream(socket.getOutputStream());
						
						dataOut.writeInt(selectedShip);
						dataOut.flush();
						
						byte[] buf = new byte[dataIn.readInt()];
						dataIn.read(buf, 0, buf.length);
						DataInputStream dIn = new DataInputStream(new ByteArrayInputStream(buf));
						// Receive connection data
						if (dIn.readInt() == 0){
							// Receive player data
							int id = dIn.readInt();

							Ship.ships = new Ship[Ship.ships.length]; // reset ships
							int i = 0;
							while ((i = dIn.readInt()) != -1)
								Ship.ships[i] = new Ship(dIn.readInt());
							
							Ship.ships[id] = new Ship(selectedShip);
							ship = Ship.ships[id];
							ship.id = id;
							SpaceGame.myShip = id;
							System.out.println("Client: I am " + id);
							
							// Receive body data
							i = 0;
							Body.bodies = new Body[Body.bodies.length];
							while ((i = dIn.readInt()) != -1){
								Body b = new Body();
								if (dIn.readBoolean())
									b = new Asteroid(0, 0);
								else
									b.sprite = ContentLoader.planetTexture;
								b.Position = new Vector2(dIn.readFloat(), dIn.readFloat());
								b.Velocity = new Vector2(dIn.readFloat(), dIn.readFloat());
								b.Rotation = dIn.readFloat();
								b.AngularVelocity = dIn.readFloat();
								b.Mass = dIn.readFloat();
								b.Radius = dIn.readFloat();
								b.Anchored = dIn.readBoolean();
								b.Collidable = dIn.readBoolean();
								b.Gravity = dIn.readBoolean();
								b.zIndex = dIn.readInt();
								Body.bodies[i] = b;
							}
						}
						dIn.close();
						
						((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.INGAME);
						
						System.out.println("Client: Established connection");
						
						dataOut.writeInt(42); // Ready to receive data
						dataOut.flush();

						// MAIN CLIENT LOOP
						running = true;
						while (!socket.isClosed() && running) {
							try {
								receivePacket();
							} catch (IOException e) {
								System.out.println("Client: Error: " + e.toString());
								e.printStackTrace();
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
						e.printStackTrace();
						((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.CONNECTION_FAILED);
					}
				}
			};
			receiveThread.start();
	}
}