package spacegame;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Projectile extends Body {
	public int id;
	
	public float Life;
	public float SizeDecay;
	public float AlphaDecay;
	public float Damage;
	
	public Ship owner;
	
	public boolean Animated = false;
	public int frameWidth;
	public int curFrame;
	public int numFrames;
	public float frameRate;
	float frameTimer;
	
	public Projectile(float life, float dmg, Color color){
		super(1, 1);
		
		FillColor = color;
		
		Life = life;
		Gravity = false;
		RemoveOnHit = false;
		Collidable = false;
		
		SizeDecay = 0;
		AlphaDecay = 0;
		Damage = dmg;
		
		curFrame = 0;
		
		zIndex = 1; // middle
	}
	
	@Override
	void update(float delta){
		if (Animated){
			frameTimer += delta;
			if (frameTimer > 1f / frameRate){
				frameTimer = 0;
				curFrame++;
				if (curFrame >= numFrames)
					curFrame = numFrames - 1;
			}
		}
		
		Life -= delta;
		Radius += delta * SizeDecay;
		float r = FillColor.getRed() / 255f, g = FillColor.getGreen() / 255f, b = FillColor.getBlue() / 255f, a = FillColor.getAlpha() / 255f;
		FillColor = new Color(r, g, b, Math.min(Math.max(a + delta * AlphaDecay, 0), 1));
		
		if (Life <= 0)
			RemovalFlag = true;
		
		super.update(delta);
	}
	
	@Override
	void draw(Graphics2D g2d){
		AffineTransform before = g2d.getTransform();
		if (sprite != null){
			float w2 = sprite.getWidth() / 2f;
			float h2 = sprite.getHeight() / 2f;
			
			if (Animated)
				w2 = frameWidth / 2f;
			
			g2d.translate(Position.x, Position.y);
			g2d.scale(Radius, Radius);
			g2d.rotate(Rotation);
			g2d.translate(-w2, -h2);

			Composite cbefore = g2d.getComposite();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FillColor.getAlpha() / 255f));
			
			if (Animated){
				int h = sprite.getHeight();
				g2d.drawImage(sprite, 0, 0, (int)frameWidth, h, (int)(frameWidth*curFrame), 0, (int)(frameWidth*curFrame + frameWidth), h, null);
			}else{
				g2d.drawImage(sprite, 0, 0, null);
			}
			
			g2d.setComposite(cbefore);
		}else{
			g2d.translate(Position.x, Position.y);
			g2d.rotate(Rotation);
			g2d.translate(-Radius, -Radius);
			
			g2d.setColor(FillColor);
			g2d.fillOval(0, 0, (int)Radius*2, (int)Radius*2);
		}
		
		g2d.setTransform(before);
	}
}
