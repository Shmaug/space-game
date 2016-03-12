package spacegame;


import java.awt.Font;
import java.awt.image.*;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * Loads images and stuff from the project/.jar
 * @author Trevor
 */
public class ContentLoader {
	public static BufferedImage[] shipTextures = new BufferedImage[2];
	public static BufferedImage shieldTexture;
	public static BufferedImage laserTexture;
	public static BufferedImage spaceBGTexture;
	
	// Body textures
	public static BufferedImage[] planetTextures = new BufferedImage[5];
	public static BufferedImage[] asteroidTextures = new BufferedImage[12];
	public static BufferedImage[] starTextures = new BufferedImage[2];
	
	public static BufferedImage asteroidGibTexture;
	
	public static BufferedImage explodeTexture0;
	public static BufferedImage explodeTexture1;
	
	public static BufferedImage muzzleFlashTexture;
	
	public static Font Avenir;
	public static Font AvenirBold;
	public static Font AvenirDemi;
	
	/**
	 * Load stuff
	 */
	public static void LoadContent(){
		for (int i = 0; i < shipTextures.length; i++)
			shipTextures[i] = 		loadImage("/resources/ships/ship" + i + ".png");
		for (int i = 0; i < planetTextures.length; i++)
			planetTextures[i] = 	loadImage("/resources/body/planets/planet" + i + ".png");
		for (int i = 0; i < asteroidTextures.length; i++)
			asteroidTextures[i] = 	loadImage("/resources/body/asteroids/asteroid" + i + ".png");
		for (int i = 0; i < starTextures.length; i++)
			starTextures[i] = 		loadImage("/resources/body/stars/star" + i + ".png");

		shieldTexture = 	 	loadImage("/resources/fx/shield.png");
		spaceBGTexture =	 	loadImage("/resources/fx/space.png");
		
		asteroidGibTexture =	loadImage("/resources/particle/asteroidGib.png");
		explodeTexture0 = 		loadImage("/resources/particle/explode0.png");
		explodeTexture1 = 		loadImage("/resources/particle/explode1.png");
		laserTexture = 			loadImage("/resources/particle/laser.png");
		
		Avenir = 				loadFont("/resources/font/Avenir.otf");
		AvenirBold =			loadFont("/resources/font/Avenir-Bold.otf");
		AvenirDemi =			loadFont("/resources/font/Avenir-Demi.otf");
	}
	
	/**
	 * loads an image
	 * @param rsrcPath the path from the executable directory
	 */
	public static BufferedImage loadImage(String rsrcPath){
		BufferedImage img = null;

		URL path = Main.class.getResource(rsrcPath);
		try {
			img = ImageIO.read(path);
		} catch (IOException e) {
			if (path != null)
				System.out.println("could not find " + path.getPath());
		}
		
		return img;
	}

	/**
	 * loads a font
	 * @param rsrcPath the path from the executable directory
	 */
	public static Font loadFont(String rsrcPath){
		Font f = null;
		
		URL path = Main.class.getResource(rsrcPath);
		try {
			f = Font.createFont(Font.TRUETYPE_FONT, path.openStream());
		} catch (Exception e) {
			if (path != null)
				System.out.println("could not find " + path.getPath());
		}
		return f;
	}
}
