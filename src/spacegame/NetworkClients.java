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

		// TODO print when sending packets
		
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
		case PACKET_NEW_CLIENT:
			dOut.writeInt(id);
			dOut.writeInt(Ship.ships[id].ShipType);
			break;
		case PACKET_DEATH:{
			dOut.writeInt(id);
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
		case PACKET_RESPAWN:
			ship.respawn();
			Network.server.respawnShip(ship.id);
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
		case PACKET_RESPAWN:
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
		case PACKET_DEATH:{
			int id = dIn.readInt();
			if (Ship.ships[id].Health > 0) // don't wanna duplicate the explode animation
				Ship.ships[id].takeDamage(Ship.ships[id].MaxHealth + Ship.ships[id].MaxShield);
			System.out.println("Client: " + id + " died!");
			break;
		}
		case PACKET_RESPAWN:{
			int id = dIn.readInt();
			Ship.ships[id].respawn();
			System.out.println("Client: " + id + " respawned!");
			break;
		}
		case PACKET_BODY_ADD:{
			// TODO read data about body
			int id = dIn.readInt();
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