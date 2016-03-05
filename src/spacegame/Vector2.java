package spacegame;

public class Vector2 {
	public float x;
	public float y;
	
	public Vector2(){
		x = 0;
		y = 0;
	}
	public Vector2(float x, float y){
		this.x = x;
		this.y = y;
	}
	
	// I really shouldn't have to explain these
	public Vector2 add(Vector2 other){
		return new Vector2(x + other.x, y + other.y);
	}
	public Vector2 sub(Vector2 other){
		return new Vector2(x - other.x, y - other.y);
	}
	public Vector2 mul(Vector2 other){
		return new Vector2(x * other.x, y * other.y);
	}
	public Vector2 div(Vector2 other){
		return new Vector2(x / other.x, y / other.y);
	}
	public Vector2 mul(float other){
		return new Vector2(x * other, y * other);
	}
	public Vector2 div(float other){
		return new Vector2(x / other, y / other);
	}
	
	public float length(){
		return (float)Math.sqrt(x*x + y*y);
	}
	public Vector2 normalized(){
		return this.div(length());
	}
	
	/**
	 * Calculates dot product
	 */
	public float dot(Vector2 other){
		return x*other.x + y*other.y;
	}
	
	public boolean equals( Vector2 other ) {
	    if( x == other.x && y == other.y ) {
	        return true;
	    }
	    return false;
	}
	
	public static Vector2 Add(Vector2 a, Vector2 b){
		return new Vector2(a.x + b.x, a.y + b.y);
	}
	public static Vector2 Subtract(Vector2 a, Vector2 b){
		return new Vector2(a.x - b.x, a.y - b.y);
	}
	public static Vector2 Multiply(Vector2 a, Vector2 b){
		return new Vector2(a.x * b.x, a.y * b.y);
	}
	public static Vector2 Divide(Vector2 a, Vector2 b){
		return new Vector2(a.x / b.x, a.y / b.y);
	}
	public static Vector2 Multiply(Vector2 a, float b){
		return new Vector2(a.x * b, a.y * b);
	}
	public static Vector2 Divide(Vector2 a, float b){
		return new Vector2(a.x / b, a.y / b);
	}
	
	public static float Distance(Vector2 a, Vector2 b){
	    float d1 = (a.x - b.x);
	    float d2 = (a.y - b.y);
	    return (float)Math.sqrt(d1*d1 + d2*d2);
	}
	public static float DistanceSquared(Vector2 a, Vector2 b){
	    float d1 = (a.x - b.x);
	    float d2 = (a.y - b.y);
	    return d1*d1 + d2*d2;
	}

	public static Vector2 Zero() { return new Vector2(0, 0); }
	public static Vector2 UnitX() { return new Vector2(1, 0); }
	public static Vector2 UnitY() { return new Vector2(0, 1); }
	public static Vector2 One() { return new Vector2(1, 1); }
	
	public String toString() {
	    return x + ", " + y;
	}
}
