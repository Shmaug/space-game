package spacegame;

public class Input {
	/**
	 * Do not read or write to this array.
	 */
	public static boolean[] keys = new boolean[255];

	/**
	 * Values for all KeyEvent keycodes that are pressed in the current frame
	 * <pre>
	 * <b>USAGE</b><pre>
	 * if (keysDown[KeyEvent.VK_W])
	 *  // W is pressed
	 * </pre></pre>
	 */
	public static boolean[] keysDown = new boolean[255];
	/**
	 * <pre>
	 * Values for all KeyEvent keycodes that were pressed 1 frame ago.
	 * Useful for detecting exactly when a key was pressed or released
	 * <pre><b>USAGE</b><pre>
	 * if (keysDown[KeyEvent.VK_W])
	 *  // W is pressed
	 * </pre></pre></pre>
	 */
	public static boolean[] lastKeys = new boolean[255];
	/**<pre>
	 * 0: button 1
	 * 1: button 2
	 * 3: middle button</pre>
	 */
	public static boolean[] MouseButtons = new boolean[3];
	/**<pre>
	 * 0: button 1
	 * 1: button 2
	 * 3: middle button</pre>
	 */
	public static boolean[] lastMouseButtons = new boolean[3];
	public static Vector2 MousePosition = Vector2.Zero();
	public static Vector2 lastMousePosition = Vector2.Zero();
	/**
	 * Set to true to record key presses to "Typed"
	 */
    public static boolean Typing = false;
	/**
	 * Stores keys typed. Set to "" to reset
	 */
    public static String Typed = "";
}
