package spacegame;

import java.awt.*;

/**
 * An asteroid. Has health, explodes into particles when destroyed
 * @author Trevor
 *
 */
public class Asteroid extends Body {
	public int texId;
	public float Health;
	
	public Asteroid(int type){
    	sprite = ContentLoader.asteroidTextures[type];
    	radius = sprite.getWidth() * .45f;
    	Health = radius * 4;
    	mass = radius * radius;
    	texId = type;
    	gravity = true;
	}
	
	public void TakeDamage(float dmg){
		if (Health > 0){
			Health -= dmg;
			
			if (Health <= 0)
				removalFlag = true;
		}
	}
	
	@Override
	public void OnRemove(){
		// little particles
		for (int i = 0; i < 50; i++){
			Particle p = new Particle(2, new Color(139, 69, 19));
			p.AlphaDecay = -.5f;
			p.sprite = ContentLoader.asteroidGibTexture;
			p.rotation = (float)Math.random() * 3f;
			p.angularVelocity = (float)Math.random() * 3f - 1.5f;
			p.radius = 1;
			p.SizeDecay = -.5f;
			p.position = position;
			p.velocity = velocity.add(new Vector2((float)Math.random() - .5f, (float)Math.random() - .5f).mul((float)Math.random() * 200f + 200f));
			Particle.particles.add(p);
		}
	}
}
