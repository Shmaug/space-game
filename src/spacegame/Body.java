package spacegame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * The base class of every physics object
 * @author Trevor
 *
 */
public class Body {
	public static Body[] bodies = new Body[1024];
	
	public float rotation;
	public float angularVelocity;
	public Vector2 velocity;
	public Vector2 position;
	public float mass;
	public float radius;
	/**
	 * Whether or not this object is movable
	 */
	public boolean anchored = false;
	
	/**
	 * Color to fill the circle (ignored if sprite != null)
	 */
	public Color fillColor;
	public BufferedImage sprite;
	
	/**
	 * Whether or not objects gravitate towards this (will still gravitate towards others!)
	 */
	public boolean gravity = true;
	
	public boolean removeOnHit = false;
	public boolean collidable = true;
	/**
	 * Bodies to ignore hit detection
	 */
	public Body[] noHit;
	
	/**
	 * Set this to true to have this removed from Bodies
	 */
	public boolean removalFlag = false;

	public Body(){
		rotation = 0;
		position = Vector2.Zero();
		velocity = Vector2.Zero();
		fillColor = Color.white;
		radius = 1f;
		mass = 1f;
	}
	public Body(float mass, float radius){
		rotation = 0;
		position = Vector2.Zero();
		velocity = Vector2.Zero();
		fillColor = Color.white;
		this.radius = radius;
		this.mass = mass;
	}
	
	public static void addBody(Body b){
		for (int i = 0; i < Body.bodies.length; i++){
			if (Body.bodies[i] == null){
				Body.bodies[i] = b;
				if (Network.server != null)
					Network.server.sendBodyAdd(i);
				break;
			}
		}
	}
	public static void removeBody(Body b){
		for (int i = 0; i < Body.bodies.length; i++){
			if (Body.bodies[i] == b){
				Body.bodies[i] = null;
				if (Network.server != null)
					Network.server.sendBodyRemove(i);
				break;
			}
		}
	}
	
	private void hitDetect(Body b){
		float d2 = Vector2.DistanceSquared(position, b.position);
		float r2 = (b.radius + radius) * (b.radius + radius);
		// HIT DETECTION
		if (removeOnHit && b.collidable){
			boolean check = true;
			if (noHit != null){
				for (int i = 0; i < noHit.length; i++){
					if (noHit[i] == b){
						check = false;
						break;
					}
				}
			}
			if (check){
				if (d2 <= r2){
					removalFlag = true;
					
					if (this instanceof Projectile)
						if (b instanceof Ship)
							((Ship)b).takeDamage(((Projectile)this).Damage, ((Projectile)this).owner);
						else if (b instanceof Asteroid)
							((Asteroid)b).TakeDamage(((Projectile)this).Damage);
				}
			}
		}
		
		// ELASTIC COLLISION
		if (!anchored && !removalFlag && collidable && b.collidable && d2 < r2){
			// get some data
			Vector2 dir = position.sub(b.position).normalized();
			float nv = b.velocity.sub(velocity).dot(dir);
			
			float deltaV = velocity.sub(b.velocity).length();
			
			if (nv > 0){
				float im1 = 1 / mass;
				float im2 = 1 / b.mass;
				
				float i = -(1 + PhysicsConstants.Restitution) * nv;
				i /= im1 + im2;
				
				Vector2 impulse = dir.mul(i);
				if (!b.anchored){
					velocity = velocity.sub(impulse.mul(im1));
					b.velocity = b.velocity.add(impulse.mul(im2));
				}else{
					velocity = velocity.sub(impulse.mul(im1 + im2));
				}
				Vector2 pos = dir.mul((radius + b.radius - (float)Math.sqrt(d2)) / (im1 + im2));
				pos.mul(-1);
				if (!b.anchored){
					position = position.sub(pos.mul(im1));
					b.position = b.position.add(pos.mul(im2));
				}else{
					position = position.sub(pos.mul(im1 + im2));
				}
				
				if (deltaV > 40){
					float dmg = deltaV / 6;
					if (this instanceof Ship)
						((Ship)this).takeDamage(dmg * (1 - (mass / (mass + b.mass))), b instanceof Ship ? (Ship)b : null);

					if (b instanceof Ship)
						((Ship)b).takeDamage(dmg * (1 - (b.mass / (mass + b.mass))), this instanceof Ship ? (Ship)this : null);
				}
			}
		}
	}
	
	/**
	 * Performs elastic collision with all Body.bodies and Ship.ships,
	 * then moves/rotates according to Velocity/AngularVelocity
	 * @param delta
	 */
	void update(float delta){
		if (!anchored){
			for (int j = 0; j < bodies.length; j++){
				Body b = bodies[j];
				if (b != null){
					if (b != this){
						// Detect collisions
						hitDetect(b);
						
						// Apply acceleration towards bodies, using Newtonian gravity equations
						float d2 = Vector2.DistanceSquared(position, b.position);
						if (b.gravity && d2 > 200){
							float f = PhysicsConstants.G * mass * b.mass / d2; // f = G * (m1*m2 / (r^2))
							Vector2 dir = ((b.position.sub(position)).normalized());
							
							velocity = velocity.add(dir.mul(f / mass * delta));
						}
					}
				}
			}
			
			// Hit detect against ships
			for (int j = 0; j < Ship.ships.length; j++){
				if (Ship.ships[j] != null && Ship.ships[j] != this)
					hitDetect(Ship.ships[j]);
			}
		}
		
		position = position.add(velocity.mul(delta));
		rotation += angularVelocity * delta;

		if (position.x > 1024 * 3)
			position.x = -1024 * 3;
		if (position.x < -1024 * 3)
			position.x = 1024 * 3;
		
		if (position.y > 1024 * 3)
			position.y = -1024 * 3;
		if (position.y < -1024 * 3)
			position.y = 1024 * 3;
	}
	
	public void OnRemove(){
		
	}
	
	void draw(Graphics2D g2d){
		AffineTransform before = g2d.getTransform();
		if (sprite != null){
			float w2 = sprite.getWidth() / 2f;
			float h2 = sprite.getHeight() / 2f;
			
			g2d.translate(position.x, position.y);
			g2d.rotate(rotation);
			g2d.translate(-w2, -h2);

			Composite cbefore = g2d.getComposite();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fillColor.getAlpha() / 255f));
			g2d.drawImage(sprite, 0, 0, null);
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
	
	/**
	 * Draws all bodies in Body.bodies and Particle.particles
	 */
	public static void Draw(Graphics2D g2d){
		// ships
		for (int i = 0; i < Ship.ships.length; i++)
			if (Ship.ships[i] != null)
				Ship.ships[i].draw(g2d);
		// bodies
		for (int i = 0; i < bodies.length; i++)
			if (bodies[i] != null)
				bodies[i].draw(g2d);
		// particles
		for (int i = 0; i < Particle.particles.size(); i++)
			if (Particle.particles.get(i) != null)
				Particle.particles.get(i).draw(g2d);
	}
	
	/**
	 * Updates all bodies in Body.bodies and Particle.particles and Ship.ships
	 */
	public static void Update(float delta){
		// bodies
		for (int i = 0; i < bodies.length; i++){
			Body b = bodies[i];
			if (b != null){
				b.update(delta);
				if(b.removalFlag){
					b.OnRemove();
					bodies[i] = null;
				}
			}
		}
		
		// ships
		for (int i = 0; i < Ship.ships.length; i++)
			if (Ship.ships[i] != null)
				Ship.ships[i].update(delta);
		
		// particles
		for (int i = 0; i < Particle.particles.size(); i++){
			Body b = Particle.particles.get(i);
			b.update(delta);
			if(b.removalFlag){
				b.OnRemove();
				Particle.particles.remove(i);
				i--;
			}
		}
	}
}
