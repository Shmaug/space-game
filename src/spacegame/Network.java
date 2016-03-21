package spacegame;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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

class ServerContainer{
	String host;
	String name;
	int players;
	int totalPlayers;
	int ping;
	
	ServerContainer(String ip, String sname, int p, int t, int pg){
		host = ip;
		name = sname;
		players = p;
		totalPlayers = t;
		ping = pg;
	}
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
	
	private static boolean Scanning = false;
	private static Thread scanThread;
	public static ArrayList<ServerContainer> foundServers = new ArrayList<ServerContainer>();
	
	public static void update(float delta){
    	// network update
    	if (server == null && client != null && client.running)
			client.sendPositionUpdate();
    	if (server != null && server.running)
    		server.sendPositionUpdate();
	}
	
	public static void stopScanning(){
		Scanning = false;
		if (scanThread != null)
			scanThread.interrupt();
	}
	
	public static void scanForLocalServers(){
		if (Scanning)
			stopScanning();
		foundServers.clear();
		
		Scanning = true;
		scanThread = new Thread(){
			public void run(){
				String localip = "";
		    	try { localip = InetAddress.getLocalHost().getHostAddress(); } catch (Exception e ) { }
				String[] numbers = localip.split("\\.");
				localip = "";
				for (int i = 0; i < numbers.length - 1; i++)
					localip += numbers[i] + ".";
		    	
				for (int i = 2; i <= 255; i++){
					if (!Scanning)
						break;
			    	
					String ip = localip + i;
					try{
    					Socket s = new Socket();
    					s.setTcpNoDelay(true);
    					s.connect(new InetSocketAddress(ip, 7777), 10);
    					DataOutputStream dOut = new DataOutputStream(s.getOutputStream());
    					DataInputStream dIn = new DataInputStream(s.getInputStream());
    					
    					long time = System.currentTimeMillis();
    					dOut.writeInt(-1);
    					dOut.flush();
    					
    					String name = dIn.readUTF();
    					int pc = dIn.readInt();
    					int t = dIn.readInt();
    					s.close();
    					foundServers.add(new ServerContainer(ip, name, pc, t, (int)(System.currentTimeMillis() - time)));
						System.out.println("Found server at " + ip + " (" + pc + "/" + t + ")");
					}catch(IOException e){ }
				}
			}
		};
		scanThread.start();
	}
}

/**
 * A server
 * @author Trevor
 */
class NetworkServer{
	public ServerSocket socket;
	boolean running = false;
	
	String serverName;
	
	Thread connectThread;
	
	long lastUpdateSent;
	
	public NetworkServer(int port, String name) throws IOException{
		serverName = name;
		socket = new ServerSocket(port);
	}
	
	/*
	 * Notify all clients that a ship respawned
	 */
	public void sendShipRespawn(int id){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != id)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_RESPAWN, new int[] { id });
					} catch (IOException e) { }
			}
		}
	}
	/*
	 * Notify all clients that a ship died
	 */
	public void sendDeath(int id, int killer){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != id)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_DEATH, new int[] { id, killer });
					} catch (IOException e) { }
			}
		}
	}
	/*
	 * Notify all clients that a ship changed
	 */
	public void sendShipChange(int id){
		for (int i = 0; i < Ship.ships.length; i++){
			if (Ship.ships[i] != null && Ship.ships[i].client != null && Ship.ships[i].client.running){
				if (i != id)
					try {
						Ship.ships[i].client.sendPacket(PacketType.PACKET_SHIP_CHANGE, new int[] { id });
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
						Ship.ships[i].client.sendPacket(PacketType.PACKET_NEW_CLIENT, new int[] { id });
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
						Ship.ships[i].client.sendPacket(PacketType.PACKET_BODY_REMOVE, new int[] { id });
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
						Ship.ships[i].client.sendPacket(PacketType.PACKET_BODY_ADD, new int[] { id });
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
							if (stype == -1){
								// client is just pinging, send back some data, close the connection
								client.dataOut.writeUTF(serverName);
								int c = 0;
								for (int i = 0; i < Ship.ships.length; i++)
									if (Ship.ships[i] != null)
										c++;
								client.dataOut.writeInt(c);
								client.dataOut.writeInt(Ship.ships.length);
								client.dataOut.flush();
								sock.close();
							}else{
								String name = client.dataIn.readUTF();
								
								// Client connected, find a slot in Ship.ships and assign it
								Ship ship = null;
								for (int i = 0; i < Ship.ships.length; i++){
									if (Ship.ships[i] == null){
										ship = new Ship(stype);
										ship.client = client;
										ship.id = i;
										ship.clientName = name;
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
												dOut.writeInt(Ship.ships[i].shipType);
												dOut.writeUTF(Ship.ships[i].clientName);
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
												if (b instanceof Asteroid)
													dOut.writeInt(((Asteroid)b).texId);
												dOut.writeFloat(b.position.x);
												dOut.writeFloat(b.position.y);
												dOut.writeFloat(b.velocity.x);
												dOut.writeFloat(b.velocity.y);
												dOut.writeFloat(b.rotation);
												dOut.writeFloat(b.angularVelocity);
												dOut.writeFloat(b.mass);
												dOut.writeFloat(b.radius);
												dOut.writeBoolean(b.anchored);
												dOut.writeBoolean(b.collidable);
												dOut.writeBoolean(b.gravity);
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
										
										System.out.println("Server: Connection " + ship.id + " established (" + name+ ")");
										
										int ack = client.dataIn.readInt();
										if (ack == 42){
											client.start();
											sendConnection(ship.id);
											
											KillFeed.log(client.ship.clientName + " CONNECTED");
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
							}
						} catch (IOException e){
							System.out.println("Server: Failed to receive a connection");
							((SpaceGame)Main.gameWindow.gamePanel.game).changeState(GameState.CONNECTION_FAILED);
							stopServer(); // server can't receive a connection, stop trying
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
	public void stopServer(){
		running = false;
		if (connectThread != null)
			connectThread.interrupt();
	}
	
	public boolean isRunning(){
		return running;
	}
}
