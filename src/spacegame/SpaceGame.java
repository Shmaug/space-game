package spacegame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;

/**
 * The game class.
 * @author Trevor
 *
 */
public class SpaceGame implements IGame {
	public static GameState gameState = GameState.SCREEN_MAIN;
	public static boolean Paused = false;
	
	public static int myShip = -1;
	
	Camera camera;

	Font menuFontSmall;
	Font menuFontNormal;
	Font menuFontBig;
	
	Button[] mainMenuButtons;
	Button[] shipSelectButtons;
	int selectedShip; // in ship selection screen
	Button[] joinScreenButtons;
	int joinScreenServerPageIndex = 0;
	Button[] hostScreenButtons;
	Button[] connectionFailedScreenButtons;
	Button[] deadButtons;
	Button[] pauseButtons;
	
	boolean choosingShipInGame = false;
	
	float deathTimer;
	
	boolean host = false;
	boolean join = false;
	String savedIP = "";
	String savedName = "";
	String savedServerName = "";
	
    public void init() {
    	////// UI CREATION //////
    	menuFontSmall = ContentLoader.Avenir.deriveFont(16f);
    	menuFontNormal = ContentLoader.AvenirBold.deriveFont(Font.BOLD, 48f);
    	menuFontBig = ContentLoader.AvenirBold.deriveFont(Font.BOLD, 72f);
    	
    	changeState(GameState.SCREEN_MAIN);
    }
    
    public void EnterPressed(){
    	if (gameState == GameState.SCREEN_JOIN){
    		joinScreenButtons[0].onClick();
    	}
    }
    
    void changeState(GameState state){
    	Paused = false;
		Input.Typing = false;
    	switch (state){
    	case INGAME:
    		// IN GAME //
    		loadGame();
    		pauseButtons = new Button[]{
    			new Button("RESUME", menuFontNormal, new Vector2(.5f, .5f), new Vector2(-200/2, 0), new Vector2(200, 40), Color.white, Color.orange){
    				public void onClick(){
    					Paused = false;
    				}
    			},
    			new Button("QUIT", menuFontNormal, new Vector2(.5f, .5f), new Vector2(-125/2, 60), new Vector2(125, 40), Color.white, Color.orange){
    				public void onClick(){
    					if (Network.server != null)
    						Network.server.stopServer();
    					if (Network.client != null)
    						Network.client.stop();
    					Network.server = null;
    					Network.client = null;
    					changeState(GameState.SCREEN_MAIN);
    				}
    			}
    		};
        	
    		deadButtons = new Button[]{
    			new Button("CHANGE SHIP", menuFontNormal, new Vector2(.5f, .75f), new Vector2(-330/2, 50), new Vector2(330, 40), Color.white, Color.orange){
    				public void onClick(){
    					choosingShipInGame = true;
    				}
    			},
    			new Button("RESPAWN", ContentLoader.AvenirBold.deriveFont(48f), new Vector2(.5f, .75f), new Vector2(-240/2, 0), new Vector2(240, 40), Color.white, new Color(192, 57, 43)){
    				public void onClick(){
    					Ship.ships[myShip].respawn();
    					if (Network.server != null)
    						Network.server.sendShipRespawn(myShip);
    					else if (Network.client != null)
    						try{
    							Network.client.sendPacket(PacketType.PACKET_RESPAWN);
    						} catch (IOException e) { }
    				}
    			}
        	};
    		break;
    	case SCREEN_MAIN:
        	// MAIN MENU //
        	mainMenuButtons = new Button[] {
        			new Button("SINGLEPLAYER", menuFontNormal, new Vector2(0.5f, .55f), new Vector2(-355/2, 0), new Vector2(355, 40), Color.white, Color.orange){
        				public void onClick(){
        					changeState(GameState.SCREEN_SHIPSEL);
        				}
        			},
        			new Button("JOIN GAME", menuFontNormal, new Vector2(0.5f, .55f), new Vector2(-280/2, 160), new Vector2(280, 40), Color.white, Color.orange){
        				public void onClick(){
        					changeState(GameState.SCREEN_JOIN);
        				}
        			},
        			new Button("HOST GAME", menuFontNormal, new Vector2(0.5f, .55f), new Vector2(-290/2, 80), new Vector2(290, 40), Color.white, Color.orange){
        				public void onClick(){
        					changeState(GameState.SCREEN_HOST);
        				}
        			},
        			new Button("QUIT GAME", menuFontNormal, new Vector2(0.5f, .55f), new Vector2(-280/2, 240), new Vector2(280, 40), Color.white, Color.orange){
    					public void onClick(){
        					System.exit(0);
        				}
        			}
        	};
        	camera = new Camera(Vector2.Zero(), 1);
        	Body.bodies = new Body[Body.bodies.length];
    		break;
    	case SCREEN_SHIPSEL:
        	// SHIP SELECT //
        	shipSelectButtons = new Button[] {
    			new Button("<", menuFontNormal, new Vector2(.2f, .5f), new Vector2(-160 - 40, 30/2), new Vector2(35, 30), Color.orange, Color.orange){
    				public void onClick(){
    					selectedShip--;
    					if (selectedShip < 0)
    						selectedShip = ContentLoader.shipTextures.length - 1;
    				}
    			},
    			new Button(">", menuFontNormal, new Vector2(.2f, .5f), new Vector2(160, 30/2), new Vector2(35, 30), Color.orange, Color.orange){
    				public void onClick(){
    					selectedShip++;
    					if (selectedShip >= ContentLoader.shipTextures.length)
    						selectedShip = 0;
    				}
    			},
    			new Button("START", menuFontNormal, new Vector2(1f, 1f), new Vector2(-250, -75), new Vector2(130, 40), Color.white, Color.orange){
    				public void onClick(){
    					if (choosingShipInGame){
    						Ship.ships[myShip].setShipType(selectedShip);
    						if (Network.server != null)
        						Network.server.sendShipChange(myShip);
        					else if (Network.client != null)
        						try{
        							Network.client.sendPacket(PacketType.PACKET_SHIP_CHANGE);
        						} catch (IOException e) { }
    						choosingShipInGame = false;
    						return;
    					}
    					savedName = Input.Typed.substring(0);
    					Input.Typed = "";
    					if (host || join){
							if (Network.server != null){
								Network.server.stopServer();
								Network.server = null;
							}
							if (Network.client != null){
								Network.client.stop();
								Network.client = null;
							}
								
							if (host){
								try {
									Network.server = new NetworkServer(7777, savedServerName);
									Network.server.start();

						        	Body.bodies = new Body[Body.bodies.length];
						        	
									Network.client = new LocalClient();
									Network.client.connect("127.0.0.1", 7777, selectedShip, savedName);

									
		    						changeState(GameState.LOADING);
								} catch (IOException e) {
									System.out.println("Server failed to initialize: " + e);
	        						changeState(GameState.CONNECTION_FAILED);
								}
							}
							if (join){
					    		// Connect to the server
								Network.client = new LocalClient();
								Network.client.connect(savedIP, 7777, selectedShip, savedName);
								
	    						changeState(GameState.LOADING);
							}
    					} else
    						changeState(GameState.INGAME);
    				}
    			},
    			new Button("BACK", menuFontNormal, new Vector2(0, 1f), new Vector2(50, -75), new Vector2(135, 40), Color.white, Color.orange){
    				public void onClick(){
    					if (choosingShipInGame){
    						// we're in a game, just paused. Go back
        					choosingShipInGame = false;
    					}else{
	    					if (join)
	        					changeState(GameState.SCREEN_JOIN);
	    					else if (host)
	        					changeState(GameState.SCREEN_HOST);
	    					else
	    						changeState(GameState.SCREEN_MAIN);
    					}
    				}
    			},
        	};
        	shipSelectButtons[2].Enabled = true;
        	Input.Typed = "";
        	Input.Typing = true;
    		break;
    	case SCREEN_JOIN:
    		joinScreenButtons = new Button[]{
    			new Button("NEXT", menuFontNormal, new Vector2(1f, 1f), new Vector2(-250, -75), new Vector2(130, 40), Color.white, Color.orange){
    				public void onClick(){
    					join = true;
    					savedIP = Input.Typed.substring(0);
    					Input.Typed = "";
    					changeState(GameState.SCREEN_SHIPSEL);
    				}
    			},
    			new Button("BACK", menuFontNormal, new Vector2(0, 1f), new Vector2(50, -75), new Vector2(135, 40), Color.white, Color.orange){
    				public void onClick(){
    					join = false;
    		    		Network.stopScanning();
    					changeState(GameState.SCREEN_MAIN);
    				}
    			},
    			new Button("<", menuFontNormal, new Vector2(.5f, 0), new Vector2(-200 - 40, 600), new Vector2(35, 30), Color.white, Color.orange){
    				public void onClick(){
    					joinScreenServerPageIndex--;
    					if (joinScreenServerPageIndex < 0)
    						joinScreenServerPageIndex = 0;
    				}
    			},
    			new Button(">", menuFontNormal, new Vector2(.5f, 0), new Vector2(200, 600), new Vector2(35, 30), Color.white, Color.orange){
    				public void onClick(){
    					joinScreenServerPageIndex++;
    					if (joinScreenServerPageIndex >= Network.foundServers.size() / 5)
    						joinScreenServerPageIndex = Network.foundServers.size() / 5;
    				}
    			},
    		};
    		joinScreenButtons[0].Enabled = true;

    		Input.Typing = true;
    		Input.Typed = "";
    		
    		Network.scanForLocalServers();
    		break;
    	case SCREEN_HOST:
    		hostScreenButtons = new Button[]{
    			new Button("NEXT", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-130/2, 0), new Vector2(130, 40), Color.white, Color.orange){
    				public void onClick(){
    					host = true;
    					savedServerName = Input.Typed.substring(0);
    					Input.Typed = "";
    					changeState(GameState.SCREEN_SHIPSEL);
    				}
    			},
    			new Button("BACK", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-135/2, 80), new Vector2(135, 40), Color.white, Color.orange){
    				public void onClick(){
    					host = false;
    					changeState(GameState.SCREEN_MAIN);
    				}
    			},
    		};
    		
    		Input.Typing = true;
    		Input.Typed = "";
    		
    		break;
    	case CONNECTION_FAILED:
    		connectionFailedScreenButtons = new Button[]{
        			new Button("MAIN MENU", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-300/2, 80), new Vector2(300, 40), Color.white, Color.orange){
        				public void onClick(){
        					if (Network.client != null){
	        					Network.client.stop();
	        					Network.client = null;
        					}
        					if (Network.server != null){
        						Network.server.stopServer();
        						Network.server = null;
        					}
        					changeState(GameState.SCREEN_MAIN);
        				}
        			},
        		};
    		break;
    	case LOADING:
    		break;
    	}
    	
    	gameState = state;
    }
    
    void loadGame(){
    	Particle.particles.clear();
    	if (!host && !join){ // if singleplayer
        	Body.bodies = new Body[Body.bodies.length];
        	
			Ship s = new Ship(selectedShip);
			Ship.ships[0] = s;
			s.ClientName = savedName;
			myShip = 0;
			
    		// we are the server
	    	Body earth = new Body();
	    	earth.Position = new Vector2(0, 500);
	    	earth.Mass = 1e5f;
	    	earth.sprite = ContentLoader.planetTextures[(int)(Math.random() * ContentLoader.planetTextures.length)];
	    	earth.Radius = earth.sprite.getWidth() * .4f;
	    	earth.Anchored = true;
	    	earth.Collidable = true;
	    	Body.addBody(earth);
	    	
	    	Asteroid asteroid = new Asteroid((int)(Math.random() * ContentLoader.asteroidTextures.length));
	    	asteroid.Position = new Vector2(0, -100);
	    	asteroid.Velocity = new Vector2(150, 0);
	    	asteroid.AngularVelocity = 1;
	    	Body.addBody(asteroid);
    	}
    	
    	camera = new Camera(Vector2.Zero(), 1);
    }

    public void update(float delta) {
    	switch (gameState){
    	case INGAME:
	    	Point2D msw = new Point2D.Float();
	    	camera.getInvTransform().transform(new Point2D.Float(Input.MousePosition.x, Input.MousePosition.y), msw);
	    	Vector2 mouseWorld = new Vector2((float)msw.getX(), (float)msw.getY());
	    	
	    	// control our ship
	    	if (myShip != -1 && Ship.ships[myShip] != null){
	    		Ship me = Ship.ships[myShip];
	    		if (me.Health > 0){
	    			deathTimer = 0;
			    	// Aim ship
			    	me.targetDirection = mouseWorld.sub(me.Position).normalized();
			    	
			    	// Move ship
			    	me.Thrusting = Input.MouseButtons[2];
			    	me.Firing = Input.MouseButtons[0];
	    		}else{
	    			deathTimer += delta;
	    			for (int i = 0; i < deadButtons.length; i++)
	    				deadButtons[i].update(delta);
	    		}
	    	}
	    	
	    	Body.Update(delta);

	    	// move camera
	    	if (myShip != -1 && Ship.ships[myShip] != null){
		    	if (Ship.ships[myShip] != null && Ship.ships[myShip].Health > 0){
			    	camera.Position = Ship.ships[myShip].Position;
		    	}
	    	}
	    	
	    	Network.update(delta);
	    	
			if (Paused)
				for (int i = 0; i < pauseButtons.length; i++)
					pauseButtons[i].update(delta);
    		
    		if (choosingShipInGame){
	    		for (int i = 0; i < shipSelectButtons.length; i++)
	    			shipSelectButtons[i].update(delta);
	    		
				shipSelectButtons[2].Enabled = true;
    		}else
    	    	if (Input.keysDown[KeyEvent.VK_ESCAPE] && !Input.lastKeys[KeyEvent.VK_ESCAPE])
    	    		Paused = !Paused;
	    	
	    	break;
    	case SCREEN_MAIN:
    		for (int i = 0; i < mainMenuButtons.length; i++)
    			mainMenuButtons[i].update(delta);
    		
    		if (Main.gameWindow.gamePanel.fps < 10)
    			Body.bodies = new Body[Body.bodies.length];
    		else
    		{
	    		int shipc = 0;
		    	for (int i = 0; i < Body.bodies.length; i++){
		    		Body b = Body.bodies[i];
		    		if (b instanceof Ship){
		    			Ship s = (Ship)b;
		    			if (s.Health <= 0)
		    				s.RemovalFlag = true;
		    			else{
		    				if (Math.abs(s.Position.x) > Main.ScreenWidth / 2 + s.Radius && Math.signum(s.Velocity.x) == Math.signum(s.Position.x))
		    					s.RemovalFlag = true;
		    					
		    				shipc++;
		    				s.targetDirection = s.Velocity.normalized();
		    				
		    				s.Firing = Main.gameWindow.gamePanel.fps > 40;
	    				}
		    		}
		    	}
	    		// spawn random ships
	    		if (shipc < 10 && Main.gameWindow.gamePanel.fps > 25 && Math.random() < .05){
	    			Ship s = new Ship((int)(Math.random()*ContentLoader.shipTextures.length));
	    			s.Firing = true;
	    			s.Thrusting = true;
	    			s.Position = new Vector2(-Main.ScreenWidth * .5f - 100, ((float)Math.random()-.5f) * Main.ScreenHeight);
	    			s.Rotation = (float)((Math.random() - .5f) * Math.PI * .25f * (.2f / Math.abs(s.Position.y - (Main.ScreenHeight / 2f))));
	    			if (Math.random() > .5){
	    				s.Position.x *= -1;
	    				s.Rotation += (float)Math.PI;
	    			}
	    			
	    			s.AngularVelocity = 0;
	    			s.Velocity = new Vector2((float)Math.cos(s.Rotation), (float)Math.sin(s.Rotation)).mul(s.MaxSpeed);
	    			
	    			Body.addBody(s);
	    		}
	    		// spawn random asteroids
	    		if (Main.gameWindow.gamePanel.fps > 25 && Math.random() < .01){
		        	Asteroid asteroid = new Asteroid((int)(Math.random() * ContentLoader.asteroidTextures.length));
		        	asteroid.Position = new Vector2(-Main.ScreenWidth * .5f - 100, ((float)Math.random()-.5f) * Main.ScreenHeight);
	    			asteroid.Rotation = (float)((Math.random() - .5f) * Math.PI * .25f * (.2f / Math.abs(asteroid.Position.y - (Main.ScreenHeight / 2f))));
	    			if (Math.random() > .5){
	    				asteroid.Position.x *= -1;
	    				asteroid.Rotation += (float)Math.PI;
	    			}
	    			
	    			asteroid.AngularVelocity = 0;
	    			asteroid.Velocity = new Vector2((float)Math.cos(asteroid.Rotation), (float)Math.sin(asteroid.Rotation)).mul(600 + (float)Math.random() * 200);
		        	asteroid.AngularVelocity = 3 * (float)Math.random();
		        	Body.addBody(asteroid);
	    		}
	    		
		    	Body.Update(delta);
    		}
    		break;
    	case SCREEN_SHIPSEL:
    		for (int i = 0; i < shipSelectButtons.length; i++)
    			shipSelectButtons[i].update(delta);
    		
			shipSelectButtons[2].Enabled = Input.Typed.length() > 0;
    		break;
    	case SCREEN_HOST:
    		for (int i = 0; i < hostScreenButtons.length; i++)
    			hostScreenButtons[i].update(delta);
    		hostScreenButtons[0].Enabled = Input.Typed.length() > 0;
    		break;
    	case SCREEN_JOIN:
    		for (int i = 0; i < joinScreenButtons.length; i++)
    			joinScreenButtons[i].update(delta);

    		joinScreenButtons[0].Enabled = Input.Typed.length() > 0;
    		break;
    	case CONNECTION_FAILED:
    		for (int i = 0; i < connectionFailedScreenButtons.length; i++)
    			connectionFailedScreenButtons[i].update(delta);
    		break;
    	case LOADING:
    		break;
    	}
    }

    public void draw(Graphics2D g2d) {
    	g2d.setColor(Color.black);
    	g2d.fillRect(0, 0, Main.ScreenWidth, Main.ScreenHeight);

    	switch (gameState){
    	case INGAME:
    		drawGame(g2d);
	        break;
    	case SCREEN_MAIN:
    		drawMainMenu(g2d);
    		break;
    	case SCREEN_SHIPSEL:
    		drawShipSel(g2d);
    		break;
    	case SCREEN_HOST:
    		drawHostScreen(g2d);
    		break;
    	case SCREEN_JOIN:
    		drawJoinScreen(g2d);
    		break;
    	case CONNECTION_FAILED:
			g2d.setFont(menuFontBig);
			g2d.setColor(Color.white);
			int w = g2d.getFontMetrics(menuFontBig).stringWidth("CONNECTION FAILED");
			g2d.drawString("CONNECTION", Main.ScreenWidth / 2 - w / 2, 200);
			g2d.setColor(Color.red);
			g2d.drawString("FAILED", Main.ScreenWidth / 2 - w / 2 + g2d.getFontMetrics(menuFontBig).stringWidth("CONNECTION "), 200);
			
			for (int i = 0; i < connectionFailedScreenButtons.length; i++)
				connectionFailedScreenButtons[i].draw(g2d);
			break;
    	case LOADING:
			g2d.setFont(menuFontBig);
			g2d.setColor(Color.white);
			g2d.drawString("LOADING", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("LOADING") / 2, 300);
			
			break;
    	}
    }
    
    public void drawJoinScreen(Graphics2D g2d){
		g2d.setFont(menuFontBig);
		g2d.setColor(Color.white);
		g2d.drawString("JOIN GAME", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("JOIN GAME") / 2, 100);

		// ip text box
		g2d.setColor(Color.white);
		g2d.drawRoundRect(Main.ScreenWidth / 2 - 250, 200, 500, 55, 4, 4);
		g2d.setFont(menuFontNormal);
		g2d.setColor(Color.orange);
		if (Input.Typed.length() > 15)
			Input.Typed = Input.Typed.substring(0, 15);
		g2d.drawString(Input.Typed + (System.currentTimeMillis() / 300 % 2 == 0 ? "" : "_"), Main.ScreenWidth / 2 - 240, 200 + 45);
		
		// server list
		for (int i = 0; i < 5; i++){
			int si = joinScreenServerPageIndex * 5 + i;
			if (si < Network.foundServers.size() - 1){
				g2d.setColor(Color.white);
				if (Input.MousePosition.x > Main.ScreenWidth * .5f - 300 && Input.MousePosition.x < Main.ScreenWidth * .5f + 350 &&
					Input.MousePosition.y > 350 + 45 * i - 40 && Input.MousePosition.y < 350 + 45 * i){
					g2d.setColor(Color.orange);
					if (Input.MouseButtons[0])
						Input.Typed = Network.foundServers.get(si).host;
				}
				
				g2d.drawString(Network.foundServers.get(si).name, Main.ScreenWidth * .5f - 300, 350 + 45 * i);
				String pc = Network.foundServers.get(si).players + "/" + Network.foundServers.get(si).totalPlayers;
				g2d.drawString(pc, Main.ScreenWidth * .5f + 220 - g2d.getFontMetrics(menuFontNormal).stringWidth(pc), 350 + 45 * i);
				g2d.drawString(Network.foundServers.get(si).ping + "ms", Main.ScreenWidth * .5f + 250, 350 + 45 * i);
			}
		}
		g2d.setColor(Color.white);
		g2d.drawString("page " + (joinScreenServerPageIndex + 1) + " of " + (Network.foundServers.size() / 5 + 1), Main.ScreenWidth * .5f - 125, 600);
		for (int i = 0; i < joinScreenButtons.length; i++)
			joinScreenButtons[i].draw(g2d);
    }
    
    public void drawHostScreen(Graphics2D g2d){
		g2d.setFont(menuFontBig);
		g2d.setColor(Color.white);
		g2d.drawString("HOST GAME", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("HOST GAME") / 2 - 15, 200);
		

		g2d.setFont(menuFontNormal);
		g2d.drawString("ENTER SERVER NAME:", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontNormal).stringWidth("ENTER SERVER NAME:") / 2, 290);
		
		g2d.setColor(Color.white);
		g2d.drawRoundRect(Main.ScreenWidth / 2 - 275, 375 - 45, 550, 55, 4, 4);
		g2d.setFont(menuFontNormal);
		g2d.setColor(Color.orange);
		if (Input.Typed.length() > 15)
			Input.Typed = Input.Typed.substring(0, 15);
		g2d.drawString(Input.Typed + (System.currentTimeMillis() / 300 % 2 == 0 ? "" : "_"), Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontNormal).stringWidth(Input.Typed) / 2, 375);
		
		for (int i = 0; i < hostScreenButtons.length; i++)
			hostScreenButtons[i].draw(g2d);
    }
    
    public void drawMainMenu(Graphics2D g2d){
    	// tile background
        for (int x = 0; x <= Main.ScreenWidth; x += ContentLoader.spaceBGTexture.getWidth()){
            for (int y = 0; y <= Main.ScreenHeight; y += ContentLoader.spaceBGTexture.getHeight()){
            	g2d.drawImage(ContentLoader.spaceBGTexture, x, y, null);
            }
        }
        
		g2d.setFont(menuFontBig);
		
		g2d.setColor(Color.darkGray);
		g2d.drawString("SPACE", Main.ScreenWidth / 2 - 160, 202);
		g2d.setColor(Color.orange);
		g2d.drawString("SPACE", Main.ScreenWidth / 2 - 160, 199);
		g2d.setColor(Color.white);
		g2d.drawString("BIG", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("BIG SPACE GAME") / 2, 200);
		g2d.drawString("GAME", Main.ScreenWidth / 2 + 90, 200);
		
		for (int i = 0; i < mainMenuButtons.length; i++)
			mainMenuButtons[i].draw(g2d);
		
		g2d.setTransform(camera.getTransform());
		
		Body.Draw(g2d);
    }
    
    public void drawGame(Graphics2D g2d){
    	// tile background
        int sx = (int)-camera.Position.x % ContentLoader.spaceBGTexture.getWidth() - ContentLoader.spaceBGTexture.getWidth();
        int sy = (int)-camera.Position.y % ContentLoader.spaceBGTexture.getHeight() - ContentLoader.spaceBGTexture.getHeight();
        for (int x = sx; x <= Main.ScreenWidth; x += ContentLoader.spaceBGTexture.getWidth()){
            for (int y = sy; y <= Main.ScreenHeight; y += ContentLoader.spaceBGTexture.getHeight()){
            	g2d.drawImage(ContentLoader.spaceBGTexture, x, y, null);
            }
        }
        
        // draw world
		g2d.setTransform(camera.getTransform());
		
        Body.Draw(g2d);
        
		if (choosingShipInGame){
	        g2d.setTransform(new AffineTransform());
			g2d.setColor(new Color(0, 0, 0, .85f));
			g2d.fillRect(0, 0, Main.ScreenWidth, Main.ScreenHeight);
			drawShipSel(g2d);
			return;
		}
        
        // SHIP/ASTEROID TRACKING
		if (myShip != -1 && Ship.ships[myShip] != null){
			Ship me = Ship.ships[myShip];
			if (me.Health > 0){
				Vector2 pos = me.Position;
				g2d.setFont(menuFontNormal);
				for (int i = 0; i < Body.bodies.length; i++){
					Body b = Body.bodies[i];
					
					if (b != me){
						if (b instanceof Asteroid && Vector2.DistanceSquared(b.Position, pos) < 1000 * 1000){
							g2d.setColor(new Color(.6f, .25f, .25f));
							
							Vector2 dir = b.Position.sub(me.Position).normalized();
							g2d.setTransform(camera.getTransform());
							g2d.translate(pos.x + dir.x * 200, pos.y + dir.y * 200);
							
							g2d.drawString("!", -20, -20);
							
							g2d.translate(0, -20);
							g2d.rotate(Math.atan2(dir.y, dir.x));

							g2d.drawString(">", 10, 20);
						}
					}
				}
				for (int i = 0; i < Ship.ships.length; i++){
					if (Ship.ships[i] != null && i != myShip && Ship.ships[i].Health > 0){

						g2d.setTransform(camera.getTransform());
						g2d.setColor(Color.red);
						
						Vector2 dir = Ship.ships[i].Position.sub(me.Position).normalized();
						g2d.translate(pos.x + dir.x * 200, pos.y + dir.y * 200);
						g2d.translate(0, -20);
						g2d.rotate(Math.atan2(dir.y, dir.x));

						g2d.drawString(">", 30, 20);
					}
				}
				// draw HUD
	        	g2d.setTransform(new AffineTransform());
    			// HEALTH/SHIELD BARS
    			float h = me.Health / me.MaxHealth;
    			float s = me.Shield / me.MaxShield;
    			
    			if (h < .25)
    				g2d.setColor(new Color(.6f, .25f, .25f, .85f));
    			else
    				g2d.setColor(new Color(.25f, .6f, .25f, .65f));
    			g2d.fillRect(Main.ScreenWidth / 2 - 200, (int)(Main.ScreenHeight * .75f), (int)(400 * h), 10);
    			
				g2d.setColor(new Color(.25f, .25f, .6f, .85f));
    			g2d.fillRect(Main.ScreenWidth / 2 - 200, (int)(Main.ScreenHeight * .75f) - 12, (int)(400 * s), 10);
    			
    			// SPEED HUD
    			g2d.setFont(menuFontNormal);
    			FontMetrics metrics = g2d.getFontMetrics(menuFontNormal);
    			String spd = (int)Math.ceil(me.Velocity.length()) + " M/S";
    			int w = metrics.stringWidth(spd);			
    			g2d.setColor(Color.white);
    			g2d.drawString(spd, (Main.ScreenWidth - w) / 2, (int)(Main.ScreenHeight * .75f) + 50);
			}else{
	        	g2d.setTransform(new AffineTransform());
	        	
    			FontMetrics metrics = g2d.getFontMetrics(menuFontBig);
    			g2d.setFont(menuFontBig);
    			int x = Main.ScreenWidth / 2 - (metrics.stringWidth("YOU HAVE DIED") / 2);
    			int y = 150;
    			
    			// outline
    			g2d.setColor(Color.white);
    			g2d.drawString("YOU HAVE", x, y);
    			g2d.setColor(Color.red);
    			g2d.drawString("DIED", x + 400, y);

    			for (int i = 0; i < deadButtons.length; i++)
    				deadButtons[i].draw(g2d);
    		}
		}
		if (Paused){
			g2d.setColor(new Color(0, 0, 0, .85f));
			g2d.fillRect(0, 0, Main.ScreenWidth, Main.ScreenHeight);
			
			g2d.setFont(menuFontBig);
			g2d.setColor(Color.white);
			g2d.drawString("PAUSED", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("PAUSED") / 2, 100);
			
			for (int i = 0; i < pauseButtons.length; i++)
				pauseButtons[i].draw(g2d);
		}
    }
    
    public void drawShipSel(Graphics2D g2d){
		g2d.setFont(menuFontBig);
		
		g2d.setColor(Color.white);
		g2d.drawString("SELECT SHIP", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("SELECT SHIP") / 2 - 15, 100);

		if (!choosingShipInGame){ // we in game yo
			g2d.setFont(menuFontNormal);
			g2d.drawString("ENTER NAME:", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontNormal).stringWidth("ENTER NAME:") / 2, Math.max(Main.ScreenHeight - 150, 600) - 60);
			
			g2d.setColor(Color.white);
			g2d.drawRoundRect(Main.ScreenWidth / 2 - 275, Math.max(Main.ScreenHeight - 150, 600) - 45, 550, 55, 4, 4);
			g2d.setColor(Color.orange);
			if (Input.Typed.length() > 15)
				Input.Typed = Input.Typed.substring(0, 15);
			g2d.drawString(Input.Typed + (System.currentTimeMillis() / 300 % 2 == 0 ? "" : "_"), Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontNormal).stringWidth(Input.Typed) / 2, Math.max(Main.ScreenHeight - 150, 600));
		}
		// draw circle around ship
		g2d.setColor(Color.darkGray);
		g2d.fillOval((int)(Main.ScreenWidth * .2f) - 150, (int)(Main.ScreenHeight * .5f) - 150, 300, 300);
		// draw ship
		g2d.drawImage(ContentLoader.shipTextures[selectedShip], (int)(Main.ScreenWidth * .2f) - ContentLoader.shipTextures[selectedShip].getWidth() / 2, Main.ScreenHeight / 2 - ContentLoader.shipTextures[selectedShip].getHeight() / 2, null);
		
		// get max ship stats
		float maxHealth, maxShield, maxSpeed, maxThrust, maxFireRate, maxDamage, maxMass;
		maxHealth = maxShield = maxSpeed = maxThrust = maxFireRate = maxDamage = maxMass = 0;
		for (int i = 0; i < ContentLoader.shipTextures.length; i++){
			Ship s = new Ship(i);
			maxHealth = Math.max(s.MaxHealth, maxHealth);
			maxShield = Math.max(s.MaxShield, maxHealth);
			maxSpeed = Math.max(s.MaxSpeed, maxSpeed);
			maxThrust = Math.max(s.Thrust, maxThrust);
			maxFireRate = Math.max(s.FireRate, maxFireRate);
			maxDamage = Math.max(s.Damage, maxDamage);
			maxMass = Math.max(s.Mass, maxMass);
		}
		Ship s = new Ship(selectedShip);
		
		// draw ship stats
		
		int h2 = Math.max(Main.ScreenHeight / 2, 320);
		int bx = (int)(Main.ScreenWidth * .4f + 400);
		int by = h2 - 250;
		int bw = Math.min(Main.ScreenWidth - bx - 20, 400);
		int bh = 25;
		
		g2d.setColor(Color.white);
		g2d.setFont(menuFontNormal);
		g2d.drawString("HEALTH", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.MaxHealth + "", bx + bw + 20, by + 30);
		g2d.setColor(new Color(1f, .25f, .25f));
		g2d.fillRect(bx, by, (int)(bw * (s.MaxHealth / maxHealth)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		g2d.drawString("SHIELD", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.MaxShield + "", bx + bw + 20, by + 30);
		g2d.setColor(new Color(.25f, .25f, 1f));
		g2d.fillRect(bx, by, (int)(bw * (s.MaxShield / maxShield)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		g2d.drawString("MAX SPEED", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.MaxSpeed + "m/s", bx + bw + 20, by + 30);
		g2d.setColor(new Color(.25f, 1f, .25f));
		g2d.fillRect(bx, by, (int)(bw * (s.MaxSpeed / maxSpeed)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		g2d.drawString("THRUST", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.Thrust/1000 + "kN", bx + bw + 20, by + 30);
		g2d.setColor(Color.orange);
		g2d.fillRect(bx, by, (int)(bw * (s.Thrust / maxThrust)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		g2d.drawString("FIRE RATE", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.FireRate + "/s", bx + bw + 20, by + 30);
		g2d.setColor(new Color(1f, .15f, .15f));
		g2d.fillRect(bx, by, (int)(bw * (s.FireRate / maxFireRate)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		g2d.drawString("DAMAGE", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.Damage + "", bx + bw + 20, by + 30);
		g2d.setColor(new Color(1f, .15f, .15f));
		g2d.fillRect(bx, by, (int)(bw * (s.Damage / maxDamage)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		g2d.drawString("MASS", Main.ScreenWidth * .4f, by + 30);
		g2d.drawString(s.Mass*1000 + "kg", bx + bw + 20, by + 30);
		g2d.setColor(new Color(.15f, .15f, .15f));
		g2d.fillRect(bx, by, (int)(bw * (s.Mass / maxMass)), bh);
		g2d.setColor(Color.white);
		g2d.drawRect(bx, by, bw, bh);
		by+=50;
		
		for (int i = 0; i < shipSelectButtons.length; i++)
			shipSelectButtons[i].draw(g2d);
    }
}
			