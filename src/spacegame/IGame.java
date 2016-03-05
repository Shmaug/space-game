package spacegame;

import java.awt.*;

enum GameState{
	INGAME,
	SCREEN_MAIN,
	SCREEN_SHIPSEL,
	SCREEN_JOIN,
	SCREEN_HOST,
	CONNECTION_FAILED,
	LOADING,
}

/**
 * This is pretty much just to implement interfaces
 * @author Trevor
 *
 */
public interface IGame {
    /**
     * Initializes the game
     * @param g2d
     */
    public void init();

    /**
     * Update is called as much as possible. Updates the game (physics n stuff)
     * @param g2d
     */
    public void update(float delta);

    /**
     * Draw is called at a target framerate (60fps). Draws the game.
     * @param g2d
     */
    public void draw(Graphics2D g2d);

    /**
     * Called when the enter key is pressed.
     * @param g2d
     */
    public void EnterPressed();
}