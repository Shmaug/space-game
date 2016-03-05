package spacegame;


import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.net.InetAddress;
import java.util.*;

@SuppressWarnings("serial")
/**
 * The main game panel, resides in a JFrame (Main)
 * This is just for painting things
 * @author Trevor
 *
 */
class GamePanel extends JPanel{
	
	public IGame game;
	public Main mainWindow;
	
	public GamePanel(Main window){
		mainWindow = window;
    	setOpaque(false);
    	
    	setSize(mainWindow.getSize());
    	
    	game = new SpaceGame();
	}
	
	long lastUpdate = System.nanoTime();
    public void update(){
        Input.keysDown = Arrays.copyOf(Input.keys, Input.keys.length);

        game.update((System.nanoTime() - lastUpdate) * 10e-10f);
    	lastUpdate = System.nanoTime();

        Input.lastMousePosition = new Vector2(Input.MousePosition.x, Input.MousePosition.y);
        Input.lastMouseButtons = Arrays.copyOf(Input.MouseButtons, Input.MouseButtons.length);
        Input.lastKeys = Arrays.copyOf(Input.keysDown, Input.keysDown.length);
    }
    
    long lastDraw = System.nanoTime();
    public int fps = 0; // frames per second
    int fc = 0; // frame count
    float ft = 0; // frame timer
    
    float timer = 0;
    
    public void paintComponent(Graphics g){
    	Main.ScreenWidth = getWidth();
    	Main.ScreenHeight = getHeight();
    	
    	float delta = (System.nanoTime() - lastDraw) * 10e-10f;
    	
    	fc++;
    	ft += delta; // add delta to frame timer
    	if (ft > 1){ // we over 1 second, reset timer
    		ft = 0;
    		fps = fc;
    		fc = 0;
    	}
    	
        Graphics2D g2d = (Graphics2D)g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        game.draw(g2d);

    	g2d.setTransform(new AffineTransform());

    	g2d.setFont(ContentLoader.Avenir.deriveFont(16f));
    	g2d.setColor(Color.green);
    	g2d.drawString(fps + " fps" , 10, Main.ScreenHeight - 10);
    	String localip = null;
    	try { localip = InetAddress.getLocalHost().getHostAddress(); } catch (Exception e ) { }
    	if (Network.server != null)
			g2d.drawString("HOSTING AT " + localip , 175, Main.ScreenHeight - 10);
		else if (Network.client != null)
        	g2d.drawString(Network.client.ping + "ms" , 175, Main.ScreenHeight - 10);
    	
    	g2d.dispose();
    	
    	lastDraw = System.nanoTime();
    }
}

@SuppressWarnings("serial")
/**
 * The main JFrame. Creates a thread to update and draw
 * a GamePanel inside it.
 * @author Trevor
 *
 */
public class Main extends JFrame implements KeyListener, MouseListener, MouseMotionListener  {
    public static int ScreenWidth, ScreenHeight;

    public boolean isRunning = false;
    public GamePanel gamePanel;
    
    
    public static Main gameWindow;

    /**
     * Creates a JFrame, maximized, then calls
     * Content.LoadContent(), init(), and starts updating
     */
    public Main(){
    	setSize(new Dimension(900, 690));
    	
    	//setUndecorated(true); // borderless (fullscreen) window
    	setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH); // maximize window
    	
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Game");
        setLocationRelativeTo(null); // center on screen

    	ContentLoader.LoadContent();
        init();
        start();
    }

    public void keyPressed( KeyEvent e ) {
        char c = e.getKeyChar();
        if ( c != KeyEvent.CHAR_UNDEFINED )
            Input.keys[e.getKeyCode()] = true;
        
    	if (Input.Typing){
	    	int i = (int)c;
	    	if (i == 10)
    			gameWindow.gamePanel.game.EnterPressed();
	    	else if (i >= 32 && i < 127)
	    		Input.Typed = Input.Typed + c;
	    	else if (i == 8)
	    		if (Input.Typed.length() > 0)
	    			Input.Typed = Input.Typed.substring(0, Input.Typed.length() - 1);
			
	    }
    }

    public void keyReleased( KeyEvent e ) {
        char c = e.getKeyChar();
        if ( c != KeyEvent.CHAR_UNDEFINED )
            Input.keys[e.getKeyCode()] = false;
    }

    public void keyTyped( KeyEvent e ) { }

    public void mouseEntered( MouseEvent e ) { }

    public void mouseExited( MouseEvent e ) { }

    public void mousePressed( MouseEvent e ) {
        if (e.getButton() == MouseEvent.BUTTON1)
            Input.MouseButtons[0] = true;
        if (e.getButton() == MouseEvent.BUTTON2)
            Input.MouseButtons[1] = true;
        if (e.getButton() == MouseEvent.BUTTON3)
            Input.MouseButtons[2] = true;
    }

    public void mouseReleased( MouseEvent e ) {
        if (e.getButton() == MouseEvent.BUTTON1)
            Input.MouseButtons[0] = false;
        if (e.getButton() == MouseEvent.BUTTON2)
            Input.MouseButtons[1] = false;
        if (e.getButton() == MouseEvent.BUTTON3)
            Input.MouseButtons[2] = false;
    }

    public void mouseMoved( MouseEvent e ) {
        Input.MousePosition.x = e.getX();
        Input.MousePosition.y = e.getY();
    }

    public void mouseDragged( MouseEvent e ) {
        Input.MousePosition.x = e.getX();
        Input.MousePosition.y = e.getY();
    }

    public void mouseClicked( MouseEvent e ) { }

    /**
     * Creates and initializes the GamePanel
     */
    public void init(){
    	gamePanel = new GamePanel(this);
        getContentPane().add(gamePanel, BorderLayout.CENTER);
        
        gamePanel.addMouseListener(this);
        gamePanel.addMouseMotionListener(this);
        
    	gamePanel.game.init();
    }

    /**
     * Starts the update thread. Tries to call Update at 30hz,
     * and Draw at 60fps
     */
    public void start(){
        isRunning = true;

        // This is where the magic happens
        Thread loop = new Thread(){
			public void run(){
		        final double targetHertz = 30; // target updates per second
		        final double updateTime = 1e9 / targetHertz; // target time between updates
		        final int maxUpdates = 5; // max updates before a render is forced
		        
		        final double targetFps = 60; // target frames per second (fps)
		        final double renderTime = 1e9 / targetFps; // target time between renders
		        
		        double lastUpdate = System.nanoTime();
		        double lastRender = System.nanoTime();

		        while (isRunning){
		        	double now = System.nanoTime();
		        	
		        	int updates = 0;
		        	while (now - lastUpdate > updateTime && updates < maxUpdates){ // Update the game as much as possible before drawing
		        		gamePanel.update();
		        		lastUpdate += updateTime;
		        		updates++;
		        	}
		        	
		        	if (now - lastUpdate > updateTime){ // Compensate for really long updates
		        		lastUpdate = now - updateTime;
		        	}
		        	
		        	// Draw the game
		        	gamePanel.repaint();
		        	lastRender = now;
		        	
		        	// kill some time until next draw
		        	
		        	while (now - lastRender < renderTime && now - lastUpdate < updateTime){
		        		Thread.yield();
		        		
		        		try { Thread.sleep(1);} catch (Exception e) { }
		        		
		        		now = System.nanoTime();
		        	}
		        }
			}
		};
		loop.start();
    }

    public static void main(String[] args) {
        gameWindow = new Main();
        gameWindow.addKeyListener(gameWindow);
        gameWindow.setVisible(true);
    }
}
