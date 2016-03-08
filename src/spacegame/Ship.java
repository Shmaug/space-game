package spacegame;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class Ship extends Body {
	public static Ship[] ships = new Ship[32];
	
	public int id;

	public int ShipType;
	
	public float Health;
	public float MaxHealth;
	public float Shield;
	public float MaxShield;
	
	public float Damage;
	public float LaserSpeed;
	public float FireRate; // shots/sec
	public float Thrust;
	public float MaxSpeed;
	public float TurnSpeed;
	
	public Vector2[] GunPositions;
	public Vector2[] ThrustPositions;
	public float ThrustParticleRadius;
	
	public boolean Thrusting = false;
	public boolean Firing = false;
	
	public float TimeAlive = 0;
	
	public Vector2 targetDirection = Vector2.Zero();
	
	float gunCooldown;
	float shieldRechargeCooldown;
	
	/**
	 * Used only by NetworkServer
	 */
	ServerClient client;
	
	public Ship(int type){
		super(1, 1);
		zIndex = 0; // very front
		
		Gravity = false;
		Collidable = true;
		ShipType = type;
		sprite = ContentLoader.shipTextures[type];
		switch (type){
		case 0:
			Mass = 1;
			Radius = 75;
			MaxSpeed = 600;
			LaserSpeed = 1250;
			TurnSpeed = 1.75f;
			Thrust = 500;
			Health = MaxHealth = 100;
			Shield = MaxShield = 100;
			Damage = 10;
			FireRate = 5;
			ThrustParticleRadius = 10;
			GunPositions = new Vector2[]{
				new Vector2(50, 9),
				new Vector2(50, 73)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(10, 42)
			};
			break;
		case 1:
			Mass = .8f;
			Radius = 45;
			MaxSpeed = 800;
			TurnSpeed = 3;
			LaserSpeed = 1500;
			Thrust = 500;
			Health = MaxHealth = 75;
			Shield = MaxShield = 50;
			Damage = 5;
			FireRate = 7;
			ThrustParticleRadius = 6;
			GunPositions = new Vector2[]{
				new Vector2(61, 3),
				new Vector2(61, 58)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(-1, 16),
				new Vector2(-1, 45)
			};
			break;
		case 2:
			Mass = 2f;
			Radius = 140;
			MaxSpeed = 400;
			TurnSpeed = 1.25f;
			Thrust = 250;
			Health = MaxHealth = 200;
			Shield = MaxShield = 200;
			LaserSpeed = 1000;
			Damage = 40;
			FireRate = 1;
			ThrustParticleRadius = 20;
			GunPositions = new Vector2[]{
				new Vector2(162, 11),
				new Vector2(156, 51),
				new Vector2(156, 153),
				new Vector2(162, 193)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(8, 102)
			};
			break;
		}
		
		// center guns/thrusters
		for (int i = 0; i < GunPositions.length; i++)
			GunPositions[i] = GunPositions[i].sub(new Vector2(sprite.getWidth() / 2, sprite.getHeight() / 2));
		for (int i = 0; i < ThrustPositions.length; i++)
			ThrustPositions[i] = ThrustPositions[i].sub(new Vector2(sprite.getWidth() / 2, sprite.getHeight() / 2));
		
		Position = new Vector2((float)Math.cos(Math.random() * Math.PI * 2), (float)Math.sin(Math.random() * Math.PI * 2)).mul((float)Math.random() * 1000);
	}
	
	/**
	 * Respawns this ships
	 */
	public void respawn(){
		Health = MaxHealth;
		Shield = MaxShield;
		Position = new Vector2((float)Math.cos(Math.random() * Math.PI * 2), (float)Math.sin(Math.random() * Math.PI * 2)).mul((float)Math.random() * 1000);
		Velocity = Vector2.Zero();
		Collidable = true;
	}
	/**
	 * This method is created just to implement polymorphism
	 * @param dmg Damage to take
	 */
	public void takeDamage(float dmg){
		TakeDamage(dmg, null);
	}
	/**
	 * Applies damage to Shield, then to Health
	 * @param dmg Damage to take
	 */
	public void TakeDamage(float dmg, Ship other){
		if (Health > 0){
			if (Shield > 0){
				Shield -= dmg;
				if (Shield < 0){
					Health += Shield;
					Shield = 0;
				}
			}else{
				Health -= dmg;
			}
			
			shieldRechargeCooldown = 2;
			
			if (Health <= 0){
				Explode();
				
				if (Network.server != null)
					Network.server.sendDeath(id);
			}
		}
	}
	
	/**
	 * Creates an explosion, sets health to 0, sets TimeAlive to 0
	 */
	public void Explode(){
		TimeAlive = 0;
		Health = 0;
		Collidable = false;
		
		// big explosion
		Particle b = new Particle(1f, Color.white);
		b.Radius = 5 + Radius / 100;
		b.Position = Position;
		b.Velocity = Vector2.Zero();
		b.frameWidth = 64;
		b.numFrames = 16;
		b.frameRate = b.numFrames / b.Life;
		b.Animated = true;
		b.sprite = ContentLoader.explodeTexture0;
		Particle.particles.add(b);
		
		// little particles
		for (int i = 0; i < Radius / 2; i++){
			Particle p = new Particle(1, Color.white);
			if (Math.random() > .5){
				p.Radius = 1;
				p.frameWidth = 64;
				p.numFrames = 16;
				p.sprite = ContentLoader.explodeTexture0;
			}else{
				p.Radius = .75f;
				p.frameWidth = 196;
				p.numFrames = 13;
				p.sprite = ContentLoader.explodeTexture1;
			}
			p.Animated = true;
			p.frameRate = b.numFrames / b.Life;
			p.Position = Position;
			
			Vector2 dir = new Vector2((float)Math.random() - .5f, (float)Math.random() - .5f);
			if (dir.length() > .9f) dir = dir.normalized().mul(.9f);
			p.Velocity = dir.mul((float)Math.random() * Radius + 225f);
			Particle.particles.add(p);
		}
	}
	
	/**
	 * Tries to shoot, if gunCooldown <= 0 and Firing
	 * Should be called every frame
	 * @param delta Delta time since last frame
	 */
	void tryShoot(float delta){
    	Vector2 rot = new Vector2((float)Math.cos(Rotation), (float)Math.sin(Rotation));
    	
		if (gunCooldown > 0)
			gunCooldown -= delta;
		if (Firing && gunCooldown <= 0){
			gunCooldown = 1f / FireRate;
			// fire lasers at gun positions
    		for (int i = 0; i < GunPositions.length; i++){
        		Projectile laser = new Projectile(2, Damage, new Color(1f, .25f, .25f, 0.75f));
        		laser.sprite = ContentLoader.laserTexture;
        		laser.Rotation = Rotation;
        		laser.AlphaDecay = 0;
        		laser.SizeDecay = 0;
        		laser.Radius = 1f;
        		laser.Mass = .25f;
        		laser.Velocity = Velocity.add(rot.mul(LaserSpeed));
        		laser.Gravity = false;
        		laser.RemoveOnHit = true;
        		laser.noHit = new Body[] { this };
        		laser.owner = this;
        		laser.Collidable = false;
    			laser.Position = Position.add(new Vector2(
					GunPositions[i].x * rot.x - GunPositions[i].y * rot.y, // x*cos(a) - y*sin(a)
					GunPositions[i].x * rot.y + GunPositions[i].y * rot.x  // x*sin(a) - y*cos(a)
				));
    			Body.addBody(laser);
    		}
		}
	}
	
	/**
	 * Tries to create thrust particles and propel the ship forward if Thrusting
	 * Should be called every frame
	 * @param delta
	 */
	void tryThrust(float delta){
    	Vector2 rot = new Vector2((float)Math.cos(Rotation), (float)Math.sin(Rotation));
    	
		if (Thrusting){
			Vector2 a = rot.mul((Thrust / Mass) * delta);
			//Vector2 b = Velocity;
			//if (Velocity.length() > MaxSpeed && a.dot(b) > 0)
			//	a = a.sub( b.mul( a.dot(b) / b.dot(b)) ); // Vector rejection
			Velocity = Velocity.add(a);
			if (Velocity.length() > MaxSpeed)
				Velocity = Velocity.normalized().mul(MaxSpeed);
			
			// Add thrust particles at thrust positions
    		for (int i = 0; i < ThrustPositions.length; i++){
    			Particle t = new Particle(2, new Color(.25f, .25f, 1f, 0.5f));
        		t.AlphaDecay = -.5f;
        		t.SizeDecay = -2;
        		t.Radius = ThrustParticleRadius;
        		t.Mass = .25f;
        		t.Velocity = Velocity.sub(rot.mul(100)).add(new Vector2((float)Math.random()-.5f,(float)Math.random()-.5f).mul(50));
        		t.Gravity = false;
        		t.RemoveOnHit = true;
        		t.noHit = new Body[] { this };
        		t.Collidable = false;
    			t.Position = Position.add(new Vector2(
					ThrustPositions[i].x * rot.x - ThrustPositions[i].y * rot.y, // x*cos(a) - y*sin(a)
					ThrustPositions[i].x * rot.y + ThrustPositions[i].y * rot.x  // x*sin(a) - y*cos(a)
				));
    			Particle.particles.add(t);
    		}
		}
	}
	
	@Override
	void update(float delta){
		if (Health > 0){
			TimeAlive += delta;
			if (shieldRechargeCooldown > 0)
				shieldRechargeCooldown -= delta;
			else
				Shield = Math.min(Shield + delta * 50, MaxShield);
			
	    	tryThrust(delta);
			tryShoot(delta);
			

	    	Vector2 dir = new Vector2((float)Math.cos(Rotation), (float)Math.sin(Rotation)); // current forward direction
	    	
	    	float targav = 0;
	    	float dot = dir.dot(targetDirection);
	    	if (dot < .99f){
	    		// sick one-liner below
		    	if (new Vector2((float)Math.cos(Rotation + TurnSpeed), (float)Math.sin(Rotation + TurnSpeed)).dot(targetDirection) >
		    	new Vector2((float)Math.cos(Rotation - TurnSpeed), (float)Math.sin(Rotation - TurnSpeed)).dot(targetDirection))
		    		targav = TurnSpeed;
		    	else
		    		targav = -TurnSpeed;
	    	}
	    	
    		AngularVelocity = AngularVelocity + (targav - AngularVelocity) * .25f; // lerp
			
			super.update(delta);
		}else{
			TimeAlive -= delta;
			Thrusting = Firing = false;
			Velocity = Vector2.Zero();
		}
	}
	
	@Override
	void draw(Graphics2D g2d){
		if (sprite != null && Health > 0){
			AffineTransform before = g2d.getTransform();
			
			// Draw ship
			float w2 = sprite.getWidth() / 2f;
			float h2 = sprite.getHeight() / 2f;
			
			g2d.translate(Position.x, Position.y);
			g2d.rotate(Rotation);
			g2d.translate(-w2, -h2);
			
			g2d.drawImage(sprite, 0, 0, null);

			g2d.translate(w2, h2);
			
			// Draw shield
			
			float sw2 = ContentLoader.shieldTexture.getWidth() / 2f;
			float sh2 = ContentLoader.shieldTexture.getHeight() / 2f;
			
			final float ssc = Radius / (ContentLoader.shieldTexture.getWidth() / 2f);
			
			g2d.scale(ssc, ssc);
			g2d.translate(-sw2, -sh2);

			Composite cbefore = g2d.getComposite();
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Shield / MaxShield));
			
			g2d.drawImage(ContentLoader.shieldTexture, 0, 0, null);
			
			g2d.setComposite(cbefore);
			g2d.setTransform(before);
		}
	}
}
