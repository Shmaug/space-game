package spacegame;

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
	PACKET_DEATH,				// (both ways)    a client died
	PACKET_RESPAWN,				// (both ways)    a client respawned
	PACKET_NEW_CLIENT,			// (both ways)    a client connected
	PACKET_CLIENT_DISCONNECT,	// (both ways)    a client disconnected
	PACKET_BODY_REMOVE,
 	PACKET_BODY_ADD,
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
	 * Notify all clients that a ship respawned
	 */
	public void respawnShip(int id){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != SpaceGame.myShip)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_RESPAWN, id);
					} catch (IOException e) { }
			}
		}
	}
	/*
	 * Notify all clients that a ship died
	 */
	public void sendDeath(int id){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != SpaceGame.myShip)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_DEATH, id);
					} catch (IOException e) { }
			}
		}
	}
	/*
	 * Notify all clients that a ship connected
	 */
	public void sendConnection(int id){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != id)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_NEW_CLIENT, id);
					} catch (IOException e) { }
			}
		}
	}
	
	/**
	 * 
	 * @param i index of the body
	 */
	public void sendBodyRemove(int id) {
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != SpaceGame.myShip)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_BODY_REMOVE, id);
					} catch (IOException e) { }
			}
		}
	}
	/**
	 * 
	 * @param i index of the body
	 */
	public void sendBodyAdd(int id) {
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != SpaceGame.myShip)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_BODY_ADD, id);
					} catch (IOException e) { }
			}
		}
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
									client.dataOut.writeInt(buf.length);
									client.dataOut.write(buf);
									client.dataOut.flush();
									
									client.ship = ship;
									Ship.ships[ship.id] = ship;
									
									System.out.println("Server: Connection " + ship.id + " established (" + client.dataIn.available() + ")");
									
									int ack = client.dataIn.readInt();
									if (ack == 42){
										client.start();
										sendConnection(ship.id);
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
