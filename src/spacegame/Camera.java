package spacegame;

import java.awt.geom.AffineTransform;

/**
 * A camera class to translate, rotate, and scale the world
 * @author Trevor
 *
 */
public class Camera {
	public Vector2 Position;
	public float Scale;
	public float Rotation;
	
	public Camera(Vector2 position, float scale){
		Position = position;
		Scale = scale;
		Rotation = 0;
	}
	
	/**
	 * Gets the transform of the camera (for translating from world -> screen space)
	 * @return
	 */
	public AffineTransform getTransform(){
		AffineTransform af = new AffineTransform();
		
		af.translate(Main.ScreenWidth / 2, Main.ScreenHeight / 2);
		af.rotate(Rotation);
		af.scale(Scale, Scale);
		af.translate(-Position.x, -Position.y);
		
		return af;
	}

	/**
	 * Gets the inverted transform of the camera (for translating from screen -> world space)
	 * @return
	 */
	public AffineTransform getInvTransform(){
		AffineTransform af = new AffineTransform();
		
		af.translate(Position.x, Position.y);
		af.scale(1/Scale, 1/Scale);
		af.rotate(-Rotation);
		af.translate(-Main.ScreenWidth / 2, -Main.ScreenHeight / 2);
		
		return af;
	}
}
