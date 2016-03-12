package spacegame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public class Ship extends Body {
	public static Ship[] ships = new Ship[32];
	public ArrayList<Particle> thrustParticles;
	
	public int id;

	public int shipType;
	
	public float health;
	public float maxHealth;
	public float shield;
	public float maxShield;
	
	public float damage;
	public float laserSpeed;
	public float fireRate; // shots/sec
	public float thrust;
	public float maxSpeed;
	public float turnSpeed;
	
	public Vector2[] gunPositions;
	public Vector2[] thrustPositions;
	public Rectangle thrustSpriteSrc;
	public Rectangle laserSpriteSrc;
	public Rectangle srcRect;
	public Vector2 origin;
	
	public boolean thrusting = false;
	public boolean firing = false;
	
	public float timeAlive = 0;
	
	public Vector2 targetDirection = Vector2.Zero();
	
	private float shieldRechargeCooldown;
	private float gunCharge;
	
	/**
	 * Used only by NetworkServer
	 */
	ServerClient client;
	String clientName = "";
	
	public Ship(int type){
		super(1, 1);
		zIndex = 0; // very front
		
		gravity = false;
		collidable = true;
		
		setShipType(type);
		
		position = new Vector2((float)Math.cos(Math.random() * Math.PI * 2), (float)Math.sin(Math.random() * Math.PI * 2)).mul((float)Math.random() * 1000 + 500);
		thrustParticles = new ArrayList<Particle>();
	}

	public void setShipType(int type){
		shipType = type;
		sprite = ContentLoader.shipTextures[type];
		switch (type){
		case 0:
			mass = 5;
			radius = 35;
			maxSpeed = 600;
			laserSpeed = 1250;
			turnSpeed = 1.75f;
			thrust = 1700;
			health = maxHealth = 50;
			shield = maxShield = 100;
			damage = 10;
			fireRate = 5;
			gunPositions = new Vector2[]{
				new Vector2(46, 4.5f),
				new Vector2(46, 47.5f)
			};
			thrustPositions = new Vector2[]{
				new Vector2(3, 26.5f)
			};
			srcRect = new Rectangle(0, 0, 45, 53);
			thrustSpriteSrc = new Rectangle(46, 0, 7, 15);
			laserSpriteSrc = new Rectangle(48, 50, 13, 3);
			break;
		case 1:
			mass = 7;
			radius = 40;
			maxSpeed = 500;
			laserSpeed = 1500;
			turnSpeed = 1.5f;
			thrust = 1750;
			health = maxHealth = 100;
			shield = maxShield = 120;
			damage = 9;
			fireRate = 8;
			gunPositions = new Vector2[]{
				new Vector2(33, 6.5f),
				new Vector2(33, 40.5f)
			};
			thrustPositions = new Vector2[]{
					new Vector2(0, 5f),
					new Vector2(0, 41f)
			};
			srcRect = new Rectangle(0, 0, 39, 48);
			thrustSpriteSrc = new Rectangle(45, 0, 35, 14);
			laserSpriteSrc = new Rectangle(60, 40, 20, 8);
			break;
		}

		origin = new Vector2(srcRect.width * .5f, srcRect.height * .5f);
		// center guns/thrusters
		for (int i = 0; i < gunPositions.length; i++)
			gunPositions[i] = gunPositions[i].sub(origin);
		for (int i = 0; i < thrustPositions.length; i++)
			thrustPositions[i] = thrustPositions[i].sub(origin);
		
		respawn();
	}
	
	@Deprecated
	public void setShipTypeOld(int type){
		/*
		ShipType = type;
		sprite = ContentLoader.shipTextures[type];
		switch (type){
		case 0:
			Mass = 5;
			Radius = 75;
			MaxSpeed = 600;
			LaserSpeed = 1250;
			TurnSpeed = 1.75f;
			Thrust = 1700;
			Health = MaxHealth = 100;
			Shield = MaxShield = 100;
			Damage = 10;
			FireRate = 5;
			ThrustParticleRadius = 10;
			GunPositions = new Vector2[]{
				new Vector2(50, 9.5f),
				new Vector2(50, 74.5f)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(10, 42)
			};
			ThrustParticleColor = new Color(.25f, .25f, 1f, 0.5f);
			break;
		case 1:
			Mass = 4;
			Radius = 45;
			MaxSpeed = 800;
			TurnSpeed = 3;
			LaserSpeed = 1500;
			Thrust = 1600;
			Health = MaxHealth = 75;
			Shield = MaxShield = 75;
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
			ThrustParticleColor = new Color(.25f, .25f, 1f, 0.5f);
			break;
		case 2:
			Mass = 20;
			Radius = 140;
			MaxSpeed = 500;
			TurnSpeed = 1.25f;
			Thrust = 4000;
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
			ThrustParticleColor = new Color(.25f, .25f, 1f, 0.5f);
			break;
		case 3:
			Mass = 15f;
			Radius = 55;
			MaxSpeed = 700;
			LaserSpeed = 1500;
			TurnSpeed = 1.35f;
			Thrust = 3000;
			Health = MaxHealth = 75;
			Shield = MaxShield = 175;
			Damage = 15;
			FireRate = 1;
			ThrustParticleRadius = 10;
			GunPositions = new Vector2[]{
				new Vector2(86, 17),
				new Vector2(86, 62)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(2, 40)
			};
			ThrustParticleColor = new Color(.25f, .25f, 1f, 0.5f);
			break;
		case 4:
			Mass = 3f;
			Radius = 50;
			MaxSpeed = 875;
			LaserSpeed = 2000;
			TurnSpeed = 3;
			Thrust = 1000;
			Health = MaxHealth = 50;
			Shield = MaxShield = 50;
			Damage = 30;
			FireRate = .75f;
			ThrustParticleRadius = 5;
			GunPositions = new Vector2[]{
				new Vector2(51, 19),
				new Vector2(51, 37)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(-5, 28)
			};
			ThrustParticleColor = new Color(.25f, .25f, 1f, 0.5f);
			break;
		case 5:
			Mass = 1.25f;
			Radius = 80;
			MaxSpeed = 700;
			LaserSpeed = 2000;
			TurnSpeed = 1.75f;
			Thrust = 500;
			Health = MaxHealth = 125;
			Shield = MaxShield = 125;
			Damage = 10;
			FireRate = 3;
			ThrustParticleRadius = 5;
			GunPositions = new Vector2[]{
				new Vector2(51, 55),
				new Vector2(51, 91)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(4, 68),
				new Vector2(4, 80)
			};
			ThrustParticleColor = new Color(.25f, .25f, 1f, 0.5f);
			break;
		case 6:
			Mass = 5f;
			Radius = 50;
			MaxSpeed = 600;
			LaserSpeed = 2000;
			TurnSpeed = 3;
			Thrust = 1800;
			Health = MaxHealth = 75;
			Shield = MaxShield = 100;
			Damage = 30;
			FireRate = 1f;
			ThrustParticleRadius = 3;
			GunPositions = new Vector2[]{
				new Vector2(65, 7),
				new Vector2(65, 55)
			};
			ThrustPositions = new Vector2[]{
				new Vector2(14, 27),
				new Vector2(14, 36)
			};
			ThrustParticleColor = new Color(1f, .25f, .25f, 0.5f);
			break;
		}
		
		// center guns/thrusters
		for (int i = 0; i < GunPositions.length; i++)
			GunPositions[i] = GunPositions[i].sub(new Vector2(sprite.getWidth() / 2, sprite.getHeight() / 2));
		for (int i = 0; i < ThrustPositions.length; i++)
			ThrustPositions[i] = ThrustPositions[i].sub(new Vector2(sprite.getWidth() / 2, sprite.getHeight() / 2));
		
		respawn();*/
	}
	
	/**
	 * Respawns this ships
	 */
	public void respawn(){
		health = maxHealth;
		shield = maxShield;
		position = new Vector2((float)Math.cos(Math.random() * Math.PI * 2), (float)Math.sin(Math.random() * Math.PI * 2)).mul((float)Math.random() * 1000);
		velocity = Vector2.Zero();
		collidable = true;
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
		if (health > 0){
			if (shield > 0){
				shield -= dmg;
				if (shield < 0){
					health += shield;
					shield = 0;
				}
			}else{
				health -= dmg;
			}
			
			shieldRechargeCooldown = 2;
			
			if (health <= 0){
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
		timeAlive = 0;
		health = 0;
		collidable = false;
		
		// big explosion
		Particle b = new Particle(1f, Color.white);
		b.radius = 5 + radius / 100;
		b.position = position;
		b.velocity = Vector2.Zero();
		b.frameWidth = 64;
		b.numFrames = 16;
		b.frameRate = b.numFrames / b.Life;
		b.Animated = true;
		b.sprite = ContentLoader.explodeTexture0;
		Particle.particles.add(b);
		
		// little particles
		for (int i = 0; i < radius / 2; i++){
			Particle p = new Particle(1, Color.white);
			if (Math.random() > .5){
				p.radius = 1;
				p.frameWidth = 64;
				p.numFrames = 16;
				p.sprite = ContentLoader.explodeTexture0;
			}else{
				p.radius = .75f;
				p.frameWidth = 196;
				p.numFrames = 13;
				p.sprite = ContentLoader.explodeTexture1;
			}
			p.Animated = true;
			p.frameRate = b.numFrames / b.Life;
			p.position = position;
			
			Vector2 dir = new Vector2((float)Math.random() - .5f, (float)Math.random() - .5f);
			if (dir.length() > .9f) dir = dir.normalized().mul(.9f);
			p.velocity = dir.mul((float)Math.random() * radius + 225f);
			Particle.particles.add(p);
		}
	}
	
	/**
	 * Tries to shoot, if gunCooldown <= 0 and Firing
	 * Should be called every frame
	 * @param delta Delta time since last frame
	 */
	void tryShoot(float delta){
    	Vector2 rot = new Vector2((float)Math.cos(rotation), (float)Math.sin(rotation));
    	
		if (firing){
			gunCharge += delta;
			if (gunCharge >= 1f / fireRate){
				gunCharge = 0;
				// fire lasers at gun positions
	    		for (int i = 0; i < gunPositions.length; i++){
	        		//Projectile laser = new Projectile(2, damage, new Color(1f, .25f, .25f, 0.75f));
	        		Projectile laser = new Projectile(2, damage, sprite);
	        		laser.srcRect = laserSpriteSrc;
	        		laser.rotation = rotation;
	        		laser.AlphaDecay = 0;
	        		laser.SizeDecay = 0;
	        		laser.radius = 1f;
	        		laser.mass = .25f;
	        		laser.velocity = velocity.add(rot.mul(laserSpeed));
	        		laser.gravity = false;
	        		laser.removeOnHit = true;
	        		laser.noHit = new Body[] { this };
	        		laser.owner = this;
	        		laser.collidable = false;
	    			laser.position = position.add(new Vector2(
						gunPositions[i].x * rot.x - gunPositions[i].y * rot.y, // x*cos(a) - y*sin(a)
						gunPositions[i].x * rot.y + gunPositions[i].y * rot.x  // x*sin(a) - y*cos(a)
					));
	    			Body.addBody(laser);
	    		}
			}
		}else
			gunCharge = 0;
	}
	
	/**
	 * Tries to create thrust particles and propel the ship forward if Thrusting
	 * Should be called every frame
	 * @param delta
	 */
	void tryThrust(float delta){
    	Vector2 rot = new Vector2((float)Math.cos(rotation), (float)Math.sin(rotation));
    	
		if (thrusting){
			Vector2 a = rot.mul((thrust / mass) * delta);
			velocity = velocity.add(a);
			
			// Add thrust particles at thrust positions
			// Don't do this with the new ships
			
    		for (int i = 0; i < thrustPositions.length; i++){
    			Particle t = new Particle(.75f, sprite);
    			t.srcRect = thrustSpriteSrc;
        		t.AlphaDecay = -1 / t.Life;
        		t.SizeDecay = -1;
        		t.mass = .25f;
        		t.velocity = new Vector2(-100, ((float)Math.random() * 15 - 7));
        		t.gravity = false;
        		t.removeOnHit = true;
        		t.noHit = new Body[] { this };
        		t.collidable = false;
    			t.position = thrustPositions[i].add(origin);
    			thrustParticles.add(t);
    		}
		}
	}
	
	@Override
	void update(float delta){
		if (health > 0){
			timeAlive += delta;
			if (shieldRechargeCooldown > 0)
				shieldRechargeCooldown -= delta;
			else
				shield = Math.min(shield + delta * 50, maxShield);
			
	    	tryThrust(delta);
			tryShoot(delta);

	    	Vector2 dir = new Vector2((float)Math.cos(rotation), (float)Math.sin(rotation)); // current forward direction
	    	
	    	float targav = 0;
	    	float dot = dir.dot(targetDirection);
	    	if (dot < .99f){
	    		// sick one-liner below
		    	if (new Vector2((float)Math.cos(rotation + turnSpeed), (float)Math.sin(rotation + turnSpeed)).dot(targetDirection) >
		    	new Vector2((float)Math.cos(rotation - turnSpeed), (float)Math.sin(rotation - turnSpeed)).dot(targetDirection))
		    		targav = turnSpeed;
		    	else
		    		targav = -turnSpeed;
	    	}
	    	
    		angularVelocity = angularVelocity + (targav - angularVelocity) * .25f; // lerp
			
			super.update(delta);
			
			if (velocity.length() > maxSpeed)
				velocity = velocity.normalized().mul(maxSpeed);
		}else{
			timeAlive -= delta;
			thrusting = firing = false;
			velocity = Vector2.Zero();
		}
		
		for (int i = 0; i < thrustParticles.size(); i++){
			thrustParticles.get(i).update(delta);
			if(thrustParticles.get(i).removalFlag){
				thrustParticles.get(i).OnRemove();
				thrustParticles.remove(i);
				i--;
			}
		}
	}
	
	@Override
	void draw(Graphics2D g2d){
		if (sprite != null && health > 0){
			AffineTransform before = g2d.getTransform();
			Composite cbefore = g2d.getComposite();
			
			// Draw ship
			
			g2d.translate(position.x, position.y);
			
			// draw name
			Font f = ContentLoader.AvenirBold.deriveFont(Font.BOLD, 24);
			g2d.setFont(f);
			g2d.setColor(Color.orange);
			g2d.drawString(clientName, -g2d.getFontMetrics(f).stringWidth(clientName) / 2, radius + 24);
			
			g2d.rotate(rotation);
			g2d.translate(-origin.x, -origin.y);

			g2d.drawImage(sprite, 0, 0, srcRect.width, srcRect.height, 0, 0, srcRect.width, srcRect.height, null);

			// draw thrust particles
			for (int i = 0; i < thrustParticles.size(); i++)
				thrustParticles.get(i).draw(g2d);
			
			g2d.translate(origin.x, origin.y);
			
			if (firing){
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25f * (float) Math.pow(Math.min(Math.max(gunCharge * fireRate, 0), 1), .9f)));
				g2d.setColor(new Color(1, .2f, .2f));
				// draw little charge things
				for (int i = 0; i < gunPositions.length; i++)
					g2d.fillOval(//ContentLoader.muzzleFlashTexture,
							(int)gunPositions[i].x - 5, (int)gunPositions[i].y - 6, 14, 12);
							//0, 0, 317, 155, null);
			}
			
			// Draw shield
			
			float sw2 = ContentLoader.shieldTexture.getWidth() / 2f;
			float sh2 = ContentLoader.shieldTexture.getHeight() / 2f;
			
			final float ssc = radius / (ContentLoader.shieldTexture.getWidth() / 2f);
			
			g2d.scale(ssc, ssc);
			g2d.translate(-sw2, -sh2);

			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f * (shield / maxShield)));
			
			g2d.drawImage(ContentLoader.shieldTexture, 0, 0, null);
			
			g2d.setComposite(cbefore);
			g2d.setTransform(before);
		}
	}
}
