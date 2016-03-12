package spacegame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

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

	public void sendPacket(PacketType type) throws IOException{
		sendPacket(type, -1);
	}
	
	@SuppressWarnings("incomplete-switch")
	public void sendPacket(PacketType type, int id) throws IOException{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DataOutputStream dOut = new DataOutputStream(bOut);

		dOut.writeInt(type.ordinal());
		switch (type){
		case PACKET_SERVER_UPDATE:{
			for (int i = 0; i < Ship.ships.length; i++){
				if (Ship.ships[i] != null){
					dOut.writeInt(i);
					dOut.writeFloat(Ship.ships[i].position.x);
					dOut.writeFloat(Ship.ships[i].position.y);
					dOut.writeFloat(Ship.ships[i].velocity.x);
					dOut.writeFloat(Ship.ships[i].velocity.y);
					dOut.writeFloat(Ship.ships[i].rotation);
					dOut.writeFloat(Ship.ships[i].angularVelocity);
					byte tf = 0;
					if (Ship.ships[i].thrusting) tf |= 1;
					if (Ship.ships[i].firing) tf |= 2;
					dOut.writeByte(tf);
					dOut.writeFloat(Ship.ships[i].health);
					dOut.writeFloat(Ship.ships[i].shield);
				}
			}
			dOut.writeInt(-1);
			
			dOut.writeLong(System.currentTimeMillis()); // timestamp
			dOut.writeLong(TimeItTakesForAPacketToGetToTheServerFromTheClient); // latency
			break;
		}
		case PACKET_NEW_CLIENT:
			dOut.writeInt(id);
			dOut.writeInt(Ship.ships[id].shipType);
			break;
		case PACKET_DEATH:{
			dOut.writeInt(id);
			break;
		}
		case PACKET_SHIP_CHANGE:{
			dOut.writeInt(id);
			dOut.writeInt(Ship.ships[id].shipType);
			break;
		}
		case PACKET_RESPAWN:{
			dOut.writeInt(id);
			break;
		}
		case PACKET_BODY_REMOVE:{
			dOut.writeInt(id);
			break;
		}
		case PACKET_BODY_ADD:{
			dOut.writeInt(id);
			// TODO send data about body
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
				ship.position = pos;
				ship.velocity = vel;
				ship.rotation = rot;
				ship.angularVelocity = angvel;
				ship.thrusting = (thrustFire & 1) > 0;
				ship.firing = (thrustFire & 2) > 0;
				ship.health = health;
				ship.shield = shield;
			}
			
			TimeItTakesForAPacketToGetToTheServerFromTheClient = System.currentTimeMillis() - dIn.readLong();
			break;
		}
		case PACKET_RESPAWN:
			ship.respawn();
			Network.server.sendShipRespawn(ship.id); // tell the server to tell the other clients
			break;
		case PACKET_SHIP_CHANGE:
			ship.setShipType(dIn.readInt());
			Network.server.sendShipChange(ship.id); // tell the server to tell the other clients
			break;
		case PACKET_BODY_REMOVE:
			
			break;
		case PACKET_BODY_ADD:
			
			break;
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
	
	public void sendPacket(PacketType type) throws IOException{
		sendPacket(type, -1);
	}
	
	@SuppressWarnings("incomplete-switch")
	public void sendPacket(PacketType type, int id) throws IOException{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DataOutputStream dOut = new DataOutputStream(bOut);
		
		dOut.writeInt(type.ordinal());
		
		switch (type){
		case PACKET_CLIENT_UPDATE:
			dOut.writeFloat(ship.position.x);
			dOut.writeFloat(ship.position.y);
			dOut.writeFloat(ship.velocity.x);
			dOut.writeFloat(ship.velocity.y);
			dOut.writeFloat(ship.rotation);
			dOut.writeFloat(ship.angularVelocity);
			byte tf = 0;
			if (ship.thrusting) tf |= 1;
			if (ship.firing) tf |= 2;
			dOut.writeByte(tf);
			dOut.writeFloat(ship.health);
			dOut.writeFloat(ship.shield);
			
			dOut.writeLong(System.currentTimeMillis()); // timestamp
			break;
		case PACKET_RESPAWN:
			break;
		case PACKET_SHIP_CHANGE:
			dOut.writeInt(ship.shipType);
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
					Ship.ships[i].position = pos;
					Ship.ships[i].velocity = vel;
					Ship.ships[i].rotation = rot;
					Ship.ships[i].angularVelocity = angvel;
					Ship.ships[i].thrusting = (thrustFire & 1) > 0;
					Ship.ships[i].firing = (thrustFire & 2) > 0;
					Ship.ships[i].health = health;
					Ship.ships[i].shield = shield;
				}
			}
			TimeItTakesForAPacketToGetToTheClientFromTheServer = System.currentTimeMillis() - dIn.readLong();
			long TimeItTakesForAPacketToGetToTheServerFromTheClient = dIn.readLong();
			ping = TimeItTakesForAPacketToGetToTheClientFromTheServer + TimeItTakesForAPacketToGetToTheServerFromTheClient;
			break;
		}
		case PACKET_DEATH:{
			int id = dIn.readInt();
			if (Ship.ships[id].health > 0) // don't wanna duplicate the explode animation
				Ship.ships[id].takeDamage(Ship.ships[id].maxHealth + Ship.ships[id].maxShield);
			break;
		}
		case PACKET_SHIP_CHANGE:{
			int id = dIn.readInt();
			if (id != SpaceGame.myShip)
				Ship.ships[id].setShipType(dIn.readInt());
			break;
		}
		case PACKET_RESPAWN:{
			int id = dIn.readInt();
			if (id != SpaceGame.myShip)
				Ship.ships[id].respawn();
			break;
		}
		case PACKET_BODY_ADD:{
			// TODO read data about body
			//int id = dIn.readInt();
			break;
		}
		case PACKET_BODY_REMOVE:{
			Body.bodies[dIn.readInt()] = null;
			break;
		}
		case PACKET_NEW_CLIENT:
			Ship.ships[dIn.readInt()] = new Ship(dIn.readInt());
			System.out.println("Client: New client");
			break;
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
	
	public void connect(final String host, final int port, final int selectedShip, final String name){
			receiveThread = new Thread(){
				public void run(){
					try {
						socket = new Socket(host, port);
						socket.setTcpNoDelay(true);
						System.out.println("Client: connected to " + host);
						dataIn = new DataInputStream(socket.getInputStream());
						dataOut = new DataOutputStream(socket.getOutputStream());
						
						dataOut.writeInt(selectedShip);
						dataOut.writeUTF(name);
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
							ship.clientName = name;
							System.out.println("Client: I am " + id);
							
							// Receive body data
							i = 0;
							Body.bodies = new Body[Body.bodies.length];
							while ((i = dIn.readInt()) != -1){
								Body b = new Body();
								if (dIn.readBoolean())
									b = new Asteroid(dIn.readInt());
								else
									b.sprite = ContentLoader.planetTextures[0];
								b.position = new Vector2(dIn.readFloat(), dIn.readFloat());
								b.velocity = new Vector2(dIn.readFloat(), dIn.readFloat());
								b.rotation = dIn.readFloat();
								b.angularVelocity = dIn.readFloat();
								b.mass = dIn.readFloat();
								b.radius = dIn.readFloat();
								b.anchored = dIn.readBoolean();
								b.collidable = dIn.readBoolean();
								b.gravity = dIn.readBoolean();
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