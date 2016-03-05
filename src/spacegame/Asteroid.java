package spacegame;

import java.awt.*;

/**
 * An asteroid. Has health, explodes into particles when destroyed
 * @author Trevor
 *
 */
public class Asteroid extends Body {
	public int id;
	
	public float Health;
	
	public Asteroid(float mass, float radius){
		super(mass, radius);
    	sprite = ContentLoader.asteroidTexture;
    	Health = 500;
    	Gravity = true;
	}
	
	public void TakeDamage(float dmg){
		if (Health > 0){
			Health -= dmg;
			
			if (Health <= 0)
				RemovalFlag = true;
		}
	}
	
	@Override
	public void OnRemove(){
		// little particles
		for (int i = 0; i < 50; i++){
			Particle p = new Particle(2, new Color(139, 69, 19));
			p.AlphaDecay = -.5f;
			p.sprite = ContentLoader.asteroidGibTexture;
			p.Rotation = (float)Math.random() * 3f;
			p.AngularVelocity = (float)Math.random() * 3f - 1.5f;
			p.Radius = 1;
			p.SizeDecay = -.5f;
			p.Position = Position;
			p.Velocity = Velocity.add(new Vector2((float)Math.random() - .5f, (float)Math.random() - .5f).mul((float)Math.random() * 200f + 200f));
			Particle.particles.add(p);
		}
	}
}
