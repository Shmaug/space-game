package spacegame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Particles do not interact with other bodies,
 * nor do they get sent across clients
 * They are soley for visuals
 * @author Trevor
 *
 */
public class Particle extends Body {
	public static ArrayList<Particle> particles = new ArrayList<Particle>();
	
	public int id;
	
	public float Life;
	public float SizeDecay;
	public float AlphaDecay;
	
	public boolean Animated = false;
	public int frameWidth;
	public int curFrame;
	public int numFrames;
	public float frameRate;
	float frameTimer;
	public Rectangle srcRect;
	
	public Particle(float life, Color color){
		super(1, 1);
		
		fillColor = color;
		
		Life = life;
		gravity = false;
		removeOnHit = false;
		collidable = false;
		
		SizeDecay = 0;
		AlphaDecay = 0;
		
		curFrame = 0;
		
		zIndex = 1; // middle
	}
	
	public Particle(float life, BufferedImage img){
		super(1,1);
		
		sprite = img;
		fillColor = Color.white;
		
		Life = life;
		gravity = false;
		removeOnHit = false;
		collidable = false;
		
		SizeDecay = 0;
		AlphaDecay = 0;
		
		curFrame = 0;
		
		zIndex = 1; // middle
	}
	
	@Override
	void update(float delta){
		if (Animated){
			// Animates through the frames of it's sprite
			
			frameTimer += delta;
			if (frameTimer > 1f / frameRate){
				frameTimer = 0;
				curFrame++;
				if (curFrame >= numFrames)
					curFrame = numFrames - 1;
			}
		}
		
		Life -= delta;
		radius += delta * SizeDecay;
		float r = fillColor.getRed() / 255f, g = fillColor.getGreen() / 255f, b = fillColor.getBlue() / 255f, a = fillColor.getAlpha() / 255f;
		fillColor = new Color(r, g, b, Math.min(Math.max(a + delta * AlphaDecay, 0), 1));
		
		if (Life <= 0)
			removalFlag = true;
		
		position = position.add(velocity.mul(delta));
		rotation += angularVelocity * delta;
	}
	
	@Override
	void draw(Graphics2D g2d){
		AffineTransform before = g2d.getTransform();
		if (sprite != null){
			float w2 = sprite.getWidth() / 2f;
			float h2 = sprite.getHeight() / 2f;
			if (srcRect != null){
				w2 = srcRect.width / 2f;
				h2 = srcRect.height / 2f;
			}
			
			if (Animated)
				w2 = frameWidth / 2f;
			
			g2d.translate(position.x, position.y);
			g2d.scale(radius, radius);
			g2d.rotate(rotation);
			g2d.translate(-w2, -h2);

			Composite cbefore = g2d.getComposite();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillColor.getAlpha() / 255f));
			
			if (Animated){
				int h = sprite.getHeight();
				g2d.drawImage(sprite, 0, 0, (int)frameWidth, h, (int)(frameWidth*curFrame), 0, (int)(frameWidth*curFrame + frameWidth), h, null);
			}else{
				if (srcRect != null)
					g2d.drawImage(sprite, 0, 0, srcRect.width, srcRect.height, srcRect.x, srcRect.y, (int)srcRect.getMaxX(), (int)srcRect.getMaxY(), null);
				else
					g2d.drawImage(sprite, 0, 0, null);
			}
			
			g2d.setComposite(cbefore);
		}else{
			g2d.translate(position.x, position.y);
			g2d.rotate(rotation);
			g2d.translate(-radius, -radius);
			
			g2d.setColor(fillColor);
			g2d.fillOval(0, 0, (int)radius*2, (int)radius*2);
		}
		
		g2d.setTransform(before);
	}
}
