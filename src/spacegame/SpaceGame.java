package spacegame;

import java.awt.*;
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
	
	public static int myShip = -1;
	
	Camera camera;

	Font menuFontSmall;
	Font menuFontNormal;
	Font menuFontBig;
	
	Button[] mainMenuButtons;
	Button[] shipSelectButtons;
	int selectedShip; // in ship selection screen
	Button[] joinScreenButtons;
	Button[] hostScreenButtons;
	Button[] connectionFailedScreenButtons;
	Button respawnButton;
	
	float deathTimer;
	
	boolean host = false;
	boolean join = false;
	
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
		Input.Typing = false;
    	switch (state){
    	case INGAME:
    		// IN GAME //
    		loadGame();
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
    			new Button("<", menuFontNormal, new Vector2(.5f, .5f), new Vector2(-340, 0), new Vector2(35, 40), Color.white, Color.orange){
    				public void onClick(){
    					selectedShip--;
    					shipSelectButtons[1].Visible = true;
    					if (selectedShip < 0){
    						selectedShip = 0;
    						shipSelectButtons[0].Visible = false;
    					}
    				}
    			},
    			new Button(">", menuFontNormal, new Vector2(.5f, .5f), new Vector2(300, 0), new Vector2(35, 40), Color.white, Color.orange){
    				public void onClick(){
    					selectedShip++;
    					shipSelectButtons[0].Visible = true;
    					if (selectedShip >= ContentLoader.shipTextures.length){
    						selectedShip = ContentLoader.shipTextures.length - 1;
    						shipSelectButtons[1].Visible = false;
    					}
    				}
    			},
    			new Button("START", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-150/2, 0), new Vector2(150, 40), Color.white, Color.orange){
    				public void onClick(){
    					if (host || join){
							if (Network.server != null){
								Network.server.stopClient();
								Network.server = null;
							}
							if (Network.client != null){
								Network.client.stop();
								Network.client = null;
							}
								
							if (host){
								try {
									Network.server = new NetworkServer(7777);
									Network.server.start();

									Network.client = new LocalClient();
									Network.client.connect("127.0.0.1", 7777, selectedShip);
									
		    						changeState(GameState.LOADING);
								} catch (IOException e) {
									System.out.println("Server failed to initialize: " + e);
	        						changeState(GameState.CONNECTION_FAILED);
								}
							}
							if (join){
					    		// Connect to the server
								Network.client = new LocalClient();
								Network.client.connect(Input.Typed, 7777, selectedShip);
								
	    						changeState(GameState.LOADING);
							}
    					} else
    						changeState(GameState.INGAME);
    				}
    			},
    			new Button("BACK", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-135/2, 80), new Vector2(135, 40), Color.white, Color.orange){
    				public void onClick(){
    					if (join)
        					changeState(GameState.SCREEN_JOIN);
    					else if (host)
        					changeState(GameState.SCREEN_HOST);
    					else
    						changeState(GameState.SCREEN_MAIN);
    				}
    			},
        	};
    		shipSelectButtons[0].Visible = false;
    		break;
    	case SCREEN_JOIN:
    		Input.Typing = true;
    		Input.Typed = "";
    		joinScreenButtons = new Button[]{
    			new Button("NEXT", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-130/2, 0), new Vector2(130, 40), Color.white, Color.orange){
    				public void onClick(){
    					join = true;
    					changeState(GameState.SCREEN_SHIPSEL);
    				}
    			},
    			new Button("BACK", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-135/2, 80), new Vector2(135, 40), Color.white, Color.orange){
    				public void onClick(){
    					join = false;
    					changeState(GameState.SCREEN_MAIN);
    				}
    			},
    		};
    		
    		break;
    	case SCREEN_HOST:
    		hostScreenButtons = new Button[]{
    			new Button("NEXT", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-130/2, 0), new Vector2(130, 40), Color.white, Color.orange){
    				public void onClick(){
    					host = true;
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
    		
    		break;
    	case CONNECTION_FAILED:
    		connectionFailedScreenButtons = new Button[]{
        			new Button("MAIN MENU", menuFontNormal, new Vector2(0.5f, .8f), new Vector2(-300/2, 80), new Vector2(300, 40), Color.white, Color.orange){
        				public void onClick(){
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
    	if (host || (!host && !join)){ // if hosting or singleplayer
        	Body.bodies = new Body[Body.bodies.length];
        	
			Ship s = new Ship(selectedShip);
			Ship.ships[0] = s;
			myShip = 0;
			
    		// we are the server
	    	Body earth = new Body();
	    	earth.Position = new Vector2(0, 500);
	    	earth.Mass = 1e5f;
	    	earth.sprite = ContentLoader.planetTexture;
	    	earth.Radius = earth.sprite.getWidth() / 2 - 20;
	    	earth.Anchored = true;
	    	earth.Collidable = true;
	    	Body.addBody(earth);
	    	
	    	Asteroid asteroid = new Asteroid(1e3f, ContentLoader.asteroidTexture.getWidth() / 2 + 10);
	    	asteroid.Position = new Vector2(0, -100);
	    	asteroid.Velocity = new Vector2(150, 0);
	    	asteroid.AngularVelocity = 1;
	    	Body.addBody(asteroid);
    	}
    	
    	camera = new Camera(Vector2.Zero(), 1);
    	
    	respawnButton = new Button("RESPAWN", ContentLoader.AvenirBold.deriveFont(48f), new Vector2(.5f, .75f), new Vector2(-120, 0), new Vector2(240, 40), Color.orange, new Color(192, 57, 43)){
			public void onClick(){
				Ship p = Ship.ships[myShip];
				p.Health = p.MaxHealth;
				p.Shield = p.MaxShield;
				p.Position = Vector2.Zero();
				p.Velocity = Vector2.Zero();
				p.Collidable = true;
			}
		};
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
	    			respawnButton.update(delta);
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
	    			Ship s = new Ship((int)(Math.random()*2));
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
		        	Asteroid asteroid = new Asteroid(1e3f, ContentLoader.asteroidTexture.getWidth() / 2 + 10);
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
    		
    		break;
    	case SCREEN_HOST:
    		for (int i = 0; i < hostScreenButtons.length; i++)
    			hostScreenButtons[i].update(delta);
    		
    		break;
    	case SCREEN_JOIN:
    		for (int i = 0; i < joinScreenButtons.length; i++)
    			joinScreenButtons[i].update(delta);
    		
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
	    			
	    			respawnButton.draw(g2d);
	    		}
			}
	        break;
    	case SCREEN_MAIN:
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
    		
    		break;
    	case SCREEN_SHIPSEL:
			g2d.setFont(menuFontBig);
			
			g2d.setColor(Color.white);
			g2d.drawString("SELECT SHIP", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("SELECT SHIP") / 2 - 15, 200);
			
			g2d.drawImage(ContentLoader.shipTextures[selectedShip], Main.ScreenWidth / 2 - ContentLoader.shipTextures[selectedShip].getWidth() / 2, Main.ScreenHeight / 2 - ContentLoader.shipTextures[selectedShip].getHeight() / 2, null);
    		
    		for (int i = 0; i < shipSelectButtons.length; i++)
    			shipSelectButtons[i].draw(g2d);
    		
    		break;
    	case SCREEN_HOST:
			g2d.setFont(menuFontBig);
			g2d.setColor(Color.white);
			g2d.drawString("HOST GAME", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("HOST GAME") / 2 - 15, 200);
			
    		for (int i = 0; i < hostScreenButtons.length; i++)
    			hostScreenButtons[i].draw(g2d);
    		
    		break;
    	case SCREEN_JOIN:
			g2d.setFont(menuFontBig);
			g2d.setColor(Color.white);
			g2d.drawString("JOIN GAME", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontBig).stringWidth("JOIN GAME") / 2, 200);

			g2d.setFont(menuFontNormal);
			g2d.drawString("ENTER IP:", Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontNormal).stringWidth("ENTER IP:") / 2, 290);
			
			g2d.setColor(Color.white);
			g2d.drawRoundRect(Main.ScreenWidth / 2 - 275, 375 - 45, 550, 55, 4, 4);
			g2d.setFont(menuFontNormal);
			g2d.setColor(Color.orange);
			if (Input.Typed.length() > 15)
				Input.Typed = Input.Typed.substring(0, 15);
			g2d.drawString(Input.Typed + (System.currentTimeMillis() / 300 % 2 == 0 ? "" : "_"), Main.ScreenWidth / 2 - g2d.getFontMetrics(menuFontNormal).stringWidth(Input.Typed) / 2, 375);
			
    		for (int i = 0; i < joinScreenButtons.length; i++)
    			joinScreenButtons[i].draw(g2d);
    		
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
}