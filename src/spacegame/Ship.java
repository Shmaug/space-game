package spacegame;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

class Gun{
	public float fireRate;
	public float damage;
	public float laserSpeed;
	public Rectangle particleSrc;
	public Vector2 position;
	public float energyConsumption;
	public float charge;
	
	public Gun(Vector2 pos, Rectangle parSrc, float frate, float dmg, float spd, float ec){
		position = pos;
		particleSrc = parSrc;
		fireRate = frate;
		damage = dmg;
		laserSpeed = spd;
		energyConsumption = ec;
	}
}

public class Ship extends Body {
	public static Ship[] ships = new Ship[32];
	public ArrayList<Particle> thrustParticles;
	
	public int id;

	public int shipType;
	
	public float health;
	public float maxHealth;
	public float shield;
	public float maxShield;
	
	public float damage; // just for stats screen
	public float fireRate; // shots/sec, just for the stats screen
	public float thrust;
	public float maxSpeed;
	public float turnSpeed;
	
	public float energy, maxEnergy;
	public float specialEnergy, maxSpecialEnergy;
	public Gun[] guns;
	public Gun[] specialGuns;
	public Vector2[] thrustPositions;
	public Rectangle thrustSpriteSrc;
	public Rectangle srcRect;
	public Vector2 origin;
	
	public boolean thrusting = false;
	public boolean firing = false;
	public boolean specialFire = false;
	
	public float timeAlive = 0;
	
	public Vector2 targetDirection = Vector2.Zero();
	
	public Rectangle turretSrc;
	public Vector2 turretOrigin;
	public Vector2 turretMount;
	public float turretRotation;
	public Gun[] turretGuns;
	
	private float shieldRechargeCooldown;
	private float specialEnergyRechargeCooldown;
	private float energyRechargeCooldown;
	private int cGun = 0;
	private int cTGun = 0;
	/**
	 * Used only by NetworkServer
	 */
	ServerClient client;
	String clientName = "";
	
	public Ship(int type){
		super(1, 1);
		
		gravity = false;
		collidable = true;
		
		setShipType(type);
		
		position = new Vector2((float)Math.cos(Math.random() * Math.PI * 2), (float)Math.sin(Math.random() * Math.PI * 2)).mul((float)Math.random() * 1000 + 500);
		thrustParticles = new ArrayList<Particle>();
	}

	public void setShipType(int type){
		shipType = type;
		sprite = ContentLoader.shipTextures[type];
		Rectangle laserSrc;
		switch (type){
		case 0:
			mass = 5;
			radius = 35;
			maxSpeed = 600;
			turnSpeed = 1.75f;
			thrust = 1700;
			health = maxHealth = 50;
			shield = maxShield = 100;
			energy = maxEnergy = 100;
			specialEnergy = maxSpecialEnergy = 100;
			damage = 10;
			fireRate = 5;
			laserSrc = new Rectangle(48, 50, 13, 3);
			guns = new Gun[]{
				new Gun(new Vector2(46, 4.5f), laserSrc, fireRate, damage, 1250, 1),
				new Gun(new Vector2(46, 47.5f), laserSrc, fireRate, damage, 1250, 1)
			};
			thrustPositions = new Vector2[]{
				new Vector2(3, 26.5f)
			};
			srcRect = new Rectangle(0, 0, 45, 53);
			thrustSpriteSrc = new Rectangle(46, 0, 7, 15);
			break;
		case 1:
			mass = 7;
			radius = 40;
			maxSpeed = 500;
			turnSpeed = 1.5f;
			thrust = 1750;
			health = maxHealth = 100;
			shield = maxShield = 120;
			energy = maxEnergy = 100;
			specialEnergy = maxSpecialEnergy = 100;
			damage = 7;
			fireRate = 6;
			laserSrc = new Rectangle(60, 40, 20, 8);
			guns = new Gun[]{
				new Gun(new Vector2(33, 6.5f), laserSrc, fireRate, damage, 1500, 1),
				new Gun(new Vector2(33, 40.5f), laserSrc, fireRate, damage, 1500, 1)
			};
			thrustPositions = new Vector2[]{
					new Vector2(0, 5f),
					new Vector2(0, 41f)
			};
			srcRect = new Rectangle(0, 0, 39, 48);
			thrustSpriteSrc = new Rectangle(45, 0, 35, 14);
			break;
		case 2:
			mass = 13;
			radius = 65;
			maxSpeed = 500;
			turnSpeed = .9f;
			thrust = 2100;
			health = maxHealth = 130;
			shield = maxShield = 120;
			energy = maxEnergy = 100;
			specialEnergy = maxSpecialEnergy = 100;
			damage = 17;
			fireRate = 3;
			laserSrc = new Rectangle(102, 0, 23, 6);
			guns = new Gun[]{
					new Gun(new Vector2(69, 7.5f), laserSrc, fireRate, damage, 1500, 1),
					new Gun(new Vector2(69, 96.5f), laserSrc, fireRate, damage, 1500, 1)
				};
			thrustPositions = new Vector2[]{
				new Vector2(-5, 53f)
			};
			srcRect = new Rectangle(0, 0, 98, 104);
			thrustSpriteSrc = new Rectangle(97, 79, 34, 25);
			turretSrc = new Rectangle(98, 32, 31, 26);
			turretOrigin = new Vector2(8, 13);
			turretMount = new Vector2(29, 52);
			turretGuns = new Gun[]{
				new Gun(new Vector2(31, 3f), laserSrc, fireRate, damage, 1500, 1),
				new Gun(new Vector2(31, 24f), laserSrc, fireRate, damage, 1500, 1)
			};
			break;
		case 3:
			mass = 12;
			radius = 50;
			maxSpeed = 550;
			turnSpeed = .8f;
			thrust = 2000;
			health = maxHealth = 100;
			shield = maxShield = 100;
			energy = maxEnergy = 100;
			specialEnergy = maxSpecialEnergy = 100;
			damage = 2;
			fireRate = 15;
			laserSrc = new Rectangle(96, 78, 20, 8);
			guns = new Gun[]{
				new Gun(new Vector2(54, 22.5f), laserSrc, 2, 5, 1600, 2),
				new Gun(new Vector2(54, 63.5f), laserSrc, 2, 5, 1600, 2)
			};
			thrustPositions = new Vector2[]{
				new Vector2(-3, 41f)
			};
			srcRect = new Rectangle(0, 0, 85, 86);
			thrustSpriteSrc = new Rectangle(100, 3, 69, 36);
			turretSrc = new Rectangle(126, 61, 49, 25);
			turretOrigin = new Vector2(24, 13);
			turretMount = new Vector2(16, 44);
			turretGuns = new Gun[]{
				new Gun(new Vector2(47, 13.5f), laserSrc, 15, 5, 2000, 2),
			};
			break;
		case 4:
			mass = 25;
			radius = 120;
			maxSpeed = 400;
			turnSpeed = 1f;
			thrust = 3000;
			health = maxHealth = 200;
			shield = maxShield = 120;
			energy = maxEnergy = 100;
			specialEnergy = maxSpecialEnergy = 100;
			damage = 7;
			fireRate = 4;
			laserSrc = new Rectangle(200, 120, 20, 8);
			guns = new Gun[]{
				new Gun(new Vector2(95, 27), laserSrc, fireRate, damage, 1200, 5),
				new Gun(new Vector2(94, 100), laserSrc, fireRate, damage, 1200, 5),
				new Gun(new Vector2(104, 38), laserSrc, fireRate, damage, 1200, 5),
				new Gun(new Vector2(104, 89), laserSrc, fireRate, damage, 1200, 5)
			};
			specialEnergy = maxSpecialEnergy = 100;
			specialGuns = new Gun[]{
					new Gun(new Vector2(116, 14), laserSrc, 15, 5, 1300, 5),
					new Gun(new Vector2(116, 116), laserSrc, 15, 5, 1300, 5)
			};
			thrustPositions = new Vector2[]{
					new Vector2(23, 97),
					new Vector2(27, 44),
					new Vector2(23, 28),
					new Vector2(27, 82)
			};
			srcRect = new Rectangle(0, 0, 176, 127);
			thrustSpriteSrc = new Rectangle(229, 15, 64, 20);
			break;
		case 5:
			mass = 35;
			radius = 140;
			maxSpeed = 400;
			turnSpeed = 1f;
			thrust = 3300;
			health = maxHealth = 200;
			shield = maxShield = 130;
			energy = maxEnergy = 50;
			specialEnergy = maxSpecialEnergy = 100;
			damage = 7;
			fireRate = 6;
			laserSrc = new Rectangle(202, 171, 20, 8);
			guns = new Gun[]{
				new Gun(new Vector2(79, 29), laserSrc, fireRate, damage, 1200, 5),
				new Gun(new Vector2(71, 3), laserSrc, fireRate, damage, 1200, 5),
				new Gun(new Vector2(79, 148), laserSrc, fireRate, damage, 1200, 5),
				new Gun(new Vector2(71, 173), laserSrc, fireRate, damage, 1200, 5)
			};
			specialEnergy = maxSpecialEnergy = 500;
			specialGuns = new Gun[]{
					new Gun(new Vector2(105, 89), new Rectangle(185, 87, 50, 4), 0, 5, 1000, 500) // firerate of 0 denotes beam weapon
			};
			thrustPositions = new Vector2[]{
					new Vector2(14, 88)
			};
			srcRect = new Rectangle(0, 0, 165, 179);
			thrustSpriteSrc = new Rectangle(199, 9, 40, 27);
			break;
		}

		origin = new Vector2(srcRect.width * .5f, srcRect.height * .5f);
		// center guns/thrusters
		for (int i = 0; i < guns.length; i++)
			guns[i].position = guns[i].position.sub(origin);
		if (specialGuns != null)
			for (int i = 0; i < specialGuns.length; i++)
				specialGuns[i].position = specialGuns[i].position.sub(origin);
		if (turretGuns != null){
			turretMount = turretMount.sub(origin);
			for (int i = 0; i < turretGuns.length; i++)
				turretGuns[i].position = turretGuns[i].position.sub(turretOrigin);
		}
		
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
		energy = maxEnergy;
		specialEnergy = maxSpecialEnergy;
		rotation = turretRotation = 0;
		position = new Vector2((float)Math.cos(Math.random() * Math.PI * 2), (float)Math.sin(Math.random() * Math.PI * 2)).mul((float)Math.random() * 1000);
		velocity = Vector2.Zero();
		collidable = true;
	}
	/**
	 * This method is created just to implement polymorphism
	 * @param dmg Damage to take
	 */
	public void takeDamage(float dmg){
		takeDamage(dmg, null);
	}
	/**
	 * Applies damage to Shield, then to Health
	 * @param dmg Damage to take
	 */
	public void takeDamage(float dmg, Ship other){
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
					Network.server.sendDeath(id, other != null ? other.id : -1);
				
				if (other != null)
					KillFeed.log(other.clientName + " KILLED " + clientName);
				else
					KillFeed.log(clientName + " DIED");
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
    	
		// fire lasers at gun positions
		for (int i = 0; i < guns.length; i++){
			if (firing){
				if (i == cGun){
					guns[i].charge = Math.min(1f / guns[i].fireRate, guns[i].charge + delta);
					if ((guns[i].fireRate == 0 && energy > 0) || (guns[i].charge >= 1f / guns[i].fireRate && energy >= guns[i].energyConsumption)){
						cGun = (cGun + 1) % guns.length;
						
						guns[i].charge = 0;
						
			    		Projectile laser = new Projectile(2, guns[i].damage, sprite);
			    		laser.srcRect = guns[i].particleSrc;
			    		laser.rotation = rotation;
			    		laser.AlphaDecay = 0;
			    		laser.SizeDecay = 0;
			    		laser.radius = 1f;
			    		laser.mass = .25f;
			    		laser.velocity = velocity.add(rot.mul(guns[i].laserSpeed));
			    		laser.gravity = false;
			    		laser.removeOnHit = true;
			    		laser.noHit = new Body[] { this };
			    		laser.owner = this;
			    		laser.collidable = false;
						laser.position = position.add(new Vector2(
							guns[i].position.x * rot.x - guns[i].position.y * rot.y, // x*cos(a) - y*sin(a)
							guns[i].position.x * rot.y + guns[i].position.y * rot.x  // x*sin(a) - y*cos(a)
						));
						Body.addBody(laser);
						
						if (guns[i].fireRate == 0)
							energy -= guns[i].energyConsumption * delta;
						else
							energy -= guns[i].energyConsumption;
					}
				}
			}else
				guns[i].charge = Math.max(0, guns[i].charge - delta);
		}
		
		// fire special guns at gun positions
		if (specialGuns != null){
			for (int i = 0; i < specialGuns.length; i++){
				if (specialFire){
					specialGuns[i].charge = Math.min(1f / specialGuns[i].fireRate, specialGuns[i].charge + delta);
					if ((specialGuns[i].fireRate == 0 && specialEnergy > 0) || (specialGuns[i].charge >= 1f / specialGuns[i].fireRate && specialEnergy >= specialGuns[i].energyConsumption)){
						specialGuns[i].charge = 0;
						
			    		Projectile laser = new Projectile(2, specialGuns[i].damage, sprite);
			    		laser.srcRect = specialGuns[i].particleSrc;
			    		laser.rotation = rotation;
			    		laser.AlphaDecay = 0;
			    		laser.SizeDecay = 0;
			    		laser.radius = 1f;
			    		laser.mass = .25f;
			    		laser.velocity = velocity.add(rot.mul(specialGuns[i].laserSpeed));
			    		laser.gravity = false;
			    		laser.removeOnHit = true;
			    		laser.noHit = new Body[] { this };
			    		laser.owner = this;
			    		laser.collidable = false;
						laser.position = position.add(new Vector2(
							specialGuns[i].position.x * rot.x - specialGuns[i].position.y * rot.y, // x*cos(a) - y*sin(a)
							specialGuns[i].position.x * rot.y + specialGuns[i].position.y * rot.x  // x*sin(a) - y*cos(a)
						));
						Body.addBody(laser);
						
						if (specialGuns[i].fireRate == 0)
							specialEnergy -= specialGuns[i].energyConsumption * delta;
						else
							specialEnergy -= specialGuns[i].energyConsumption;
					}
				}else
					specialGuns[i].charge = Math.max(0, specialGuns[i].charge - delta);
			}
		}
		
		if (turretGuns != null){
	    	Vector2 trot = new Vector2((float)Math.cos(turretRotation), (float)Math.sin(turretRotation));
	    	Vector2 p = position.add(new Vector2(
					turretMount.x * rot.x - turretMount.y * rot.y, // x*cos(a) - y*sin(a)
					turretMount.x * rot.y + turretMount.y * rot.x  // x*sin(a) - y*cos(a)
				));
	    	
			for (int i = 0; i < turretGuns.length; i++){
				if (firing){
					if (i == cTGun){
						turretGuns[i].charge = Math.min(1f / turretGuns[i].fireRate, turretGuns[i].charge + delta);
						if ((turretGuns[i].fireRate == 0 && energy > 0) || (turretGuns[i].charge >= 1f / turretGuns[i].fireRate && energy >= turretGuns[i].energyConsumption)){
							cTGun = (cTGun + 1) % turretGuns.length;
							
							turretGuns[i].charge = 0;
							
			        		Projectile laser = new Projectile(2, turretGuns[i].damage, sprite);
			        		laser.srcRect = turretGuns[i].particleSrc;
			        		laser.rotation = turretRotation;
			        		laser.AlphaDecay = 0;
			        		laser.SizeDecay = 0;
			        		laser.radius = 1f;
			        		laser.mass = .25f;
			        		laser.velocity = velocity.add(trot.mul(turretGuns[i].laserSpeed));
			        		laser.gravity = false;
			        		laser.removeOnHit = true;
			        		laser.noHit = new Body[] { this };
			        		laser.owner = this;
			        		laser.collidable = false;
			    			laser.position = p.add(new Vector2(
								turretGuns[i].position.x * trot.x - turretGuns[i].position.y * trot.y, // x*cos(a) - y*sin(a)
								turretGuns[i].position.x * trot.y + turretGuns[i].position.y * trot.x  // x*sin(a) - y*cos(a)
							));
			    			Body.addBody(laser);
			    			
			    			if (turretGuns[i].fireRate == 0)
			    				energy -= turretGuns[i].energyConsumption * delta;
			    			else
			    				energy -= turretGuns[i].energyConsumption;
						}
					}
				} else 
					turretGuns[i].charge = Math.max(0, turretGuns[i].charge - delta);
			}
		}
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
			if (energyRechargeCooldown > 0)
				energyRechargeCooldown -= delta;
			else
				energy = Math.min(energy + delta * 50, maxEnergy);
			if (specialEnergyRechargeCooldown > 0)
				specialEnergyRechargeCooldown -= delta;
			else
				specialEnergy = Math.min(specialEnergy + delta * 50, maxSpecialEnergy);
			
	    	tryThrust(delta);
			
			float eb4 = energy;
			float seb4 = specialEnergy;
	    	tryShoot(delta);
	    	if (eb4 != energy)
	    		energyRechargeCooldown = 2;
	    	if (seb4 != specialEnergy)
	    		specialEnergyRechargeCooldown = 2;
	    	
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
    		
    		turretRotation = (float)Math.atan2(targetDirection.y, targetDirection.x);
			
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
			
			g2d.translate(position.x, position.y);
			
			// draw name
			Font f = ContentLoader.AvenirBold.deriveFont(Font.BOLD, 24);
			g2d.setFont(f);
			g2d.setColor(Color.orange);
			g2d.drawString(clientName, -g2d.getFontMetrics(f).stringWidth(clientName) / 2, radius + 24);
			
			g2d.rotate(rotation);
			g2d.translate(-origin.x, -origin.y);
			
			// draw thrust particles
			for (int i = 0; i < thrustParticles.size(); i++)
				thrustParticles.get(i).draw(g2d);

			// draw ship
			g2d.drawImage(sprite, 0, 0, srcRect.width, srcRect.height, 0, 0, srcRect.width, srcRect.height, null);
			
			// draw turret
			if (turretMount != null){
				AffineTransform _b4 = g2d.getTransform();

				g2d.translate(turretMount.x + origin.x, turretMount.y + origin.y);
				g2d.rotate(-rotation + turretRotation);
				g2d.translate(-turretOrigin.x, -turretOrigin.y);
				
				g2d.drawImage(sprite, 0, 0, turretSrc.width, turretSrc.height, turretSrc.x, turretSrc.y, (int)turretSrc.getMaxX(), (int)turretSrc.getMaxY(), null);

				g2d.setTransform(_b4);
			}
			
			g2d.translate(origin.x, origin.y);
			
			// draw little charge things
			g2d.setColor(new Color(1, .2f, .2f));
			for (int i = 0; i < guns.length; i++){
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25f * (float) Math.pow(Math.min(Math.max(guns[i].charge * guns[i].fireRate, 0), 1), .9f)));
				g2d.fillOval((int)guns[i].position.x - 5, (int)guns[i].position.y - 6, 14, 12);
			}

			if (specialGuns != null){
				for (int i = 0; i < specialGuns.length; i++){
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25f * (float) Math.pow(Math.min(Math.max(specialGuns[i].charge * specialGuns[i].fireRate, 0), 1), .9f)));
					g2d.fillOval((int)specialGuns[i].position.x - 5, (int)specialGuns[i].position.y - 6, 14, 12);
				}
			}
			
			if (turretGuns != null){
				for (int i = 0; i < turretGuns.length; i++){
					AffineTransform _b4 = g2d.getTransform();
	
					g2d.translate(turretMount.x, turretMount.y);
					g2d.rotate(-rotation + turretRotation);

					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25f * (float) Math.pow(Math.min(Math.max(turretGuns[i].charge * turretGuns[i].fireRate, 0), 1), .9f)));
					g2d.fillOval((int)turretGuns[i].position.x - 5, (int)turretGuns[i].position.y - 6, 14, 12);
					
					g2d.setTransform(_b4);
				}
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
