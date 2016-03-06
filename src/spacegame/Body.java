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
	
	public float Rotation;
	public float AngularVelocity;
	public Vector2 Velocity;
	public Vector2 Position;
	public float Mass;
	public float Radius;
	/**
	 * Whether or not this object is movable
	 */
	public boolean Anchored = false;
	
	/**
	 * Color to fill the circle (ignored if sprite != null)
	 */
	public Color FillColor;
	public BufferedImage sprite;
	
	/**
	 * Whether or not objects gravitate towards this (will still gravitate towards others!)
	 */
	public boolean Gravity = true;
	public int zIndex = 2;
	
	public boolean RemoveOnHit = false;
	public boolean Collidable = true;
	/**
	 * Bodies to ignore hit detection
	 */
	public Body[] noHit;
	
	/**
	 * Set this to true to have this removed from Bodies
	 */
	public boolean RemovalFlag = false;

	public Body(){
		Rotation = 0;
		Position = Vector2.Zero();
		Velocity = Vector2.Zero();
		FillColor = Color.white;
		Radius = 1f;
		Mass = 1f;
	}
	public Body(float mass, float radius){
		Rotation = 0;
		Position = Vector2.Zero();
		Velocity = Vector2.Zero();
		FillColor = Color.white;
		Radius = radius;
		Mass = mass;
	}
	
	public static void addBody(Body b){
		for (int i = 0; i < Body.bodies.length; i++){
			if (Body.bodies[i] == null){
				Body.bodies[i] = b;
				// TODO tell all clients a new body was made
				break;
			}
		}
	}
	public static void removeBody(Body b){
		for (int i = 0; i < Body.bodies.length; i++){
			if (Body.bodies[i] == b){
				Body.bodies[i] = null;
				// TODO tell all clients a body was removed
				break;
			}
		}
	}
	
	private void hitDetect(Body b){
		float d2 = Vector2.DistanceSquared(Position, b.Position);
		float r2 = (b.Radius + Radius) * (b.Radius + Radius);
		// HIT DETECTION
		if (RemoveOnHit && b.Collidable){
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
					RemovalFlag = true;
					
					if (this instanceof Projectile)
						if (b instanceof Ship)
							((Ship)b).TakeDamage(((Projectile)this).Damage, ((Projectile)this).owner);
						else if (b instanceof Asteroid)
							((Asteroid)b).TakeDamage(((Projectile)this).Damage);
				}
			}
		}
		
		// ELASTIC COLLISION
		if (!Anchored && !RemovalFlag && Collidable && b.Collidable && d2 < r2){
			// get some data
			Vector2 dir = Position.sub(b.Position).normalized();
			float nv = b.Velocity.sub(Velocity).dot(dir);
			
			float deltaV = Velocity.sub(b.Velocity).length();
			
			if (nv > 0){
				float im1 = 1 / Mass;
				float im2 = 1 / b.Mass;
				
				float i = -(1 + PhysicsConstants.Restitution) * nv;
				i /= im1 + im2;
				
				Vector2 impulse = dir.mul(i);
				if (!b.Anchored){
					Velocity = Velocity.sub(impulse.mul(im1));
					b.Velocity = b.Velocity.add(impulse.mul(im2));
				}else{
					Velocity = Velocity.sub(impulse.mul(im1 + im2));
				}
				Vector2 pos = dir.mul((Radius + b.Radius - (float)Math.sqrt(d2)) / (im1 + im2));
				pos.mul(-1);
				if (!b.Anchored){
					Position = Position.sub(pos.mul(im1));
					b.Position = b.Position.add(pos.mul(im2));
				}else{
					Position = Position.sub(pos.mul(im1 + im2));
				}
				
				if (deltaV > 40){
					float dmg = deltaV / 6;
					if (this instanceof Ship)
						((Ship)this).TakeDamage(dmg * (1 - (Mass / (Mass + b.Mass))), b instanceof Ship ? (Ship)b : null);

					if (b instanceof Ship)
						((Ship)b).TakeDamage(dmg * (1 - (b.Mass / (Mass + b.Mass))), this instanceof Ship ? (Ship)this : null);
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
		if (!Anchored){
			for (int j = 0; j < bodies.length; j++){
				Body b = bodies[j];
				if (b != null){
					if (b != this){
						// Detect collisions
						hitDetect(b);
						
						// Apply acceleration towards bodies, using Newtonian gravity equations
						float d2 = Vector2.DistanceSquared(Position, b.Position);
						if (b.Gravity && d2 > 200){
							float f = PhysicsConstants.G * Mass * b.Mass / d2; // f = G * (m1*m2 / (r^2))
							Vector2 dir = ((b.Position.sub(Position)).normalized());
							
							Velocity = Velocity.add(dir.mul(f / Mass * delta));
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
		
		Position = Position.add(Velocity.mul(delta));
		Rotation += AngularVelocity * delta;
	}
	
	public void OnRemove(){
		
	}
	
	void draw(Graphics2D g2d){
		AffineTransform before = g2d.getTransform();
		if (sprite != null){
			float w2 = sprite.getWidth() / 2f;
			float h2 = sprite.getHeight() / 2f;
			
			g2d.translate(Position.x, Position.y);
			g2d.rotate(Rotation);
			g2d.translate(-w2, -h2);

			Composite cbefore = g2d.getComposite();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FillColor.getAlpha() / 255f));
			g2d.drawImage(sprite, 0, 0, null);
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
	
	/**
	 * Draws all bodies in Body.bodies and Particle.particles
	 */
	public static void Draw(Graphics2D g2d){
		for (int z = 2; z >= 0; z--){
			// bodies
			for (int i = 0; i < bodies.length; i++)
				if (bodies[i] != null && bodies[i].zIndex == z)
					bodies[i].draw(g2d);
			// particles
			for (int i = 0; i < Particle.particles.size(); i++)
				if (Particle.particles.get(i).zIndex == z)
					Particle.particles.get(i).draw(g2d);
			// ships
			for (int i = 0; i < Ship.ships.length; i++)
				if (Ship.ships[i] != null)
					Ship.ships[i].draw(g2d);
		}
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
				if(b.RemovalFlag){
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
			if(b.RemovalFlag){
				b.OnRemove();
				Particle.particles.remove(i);
				i--;
			}
		}
	}
}
