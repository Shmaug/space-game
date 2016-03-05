package spacegame;

import java.awt.*;

/**
 * A simple text button.
 * @author Trevor
 */
public class Button {
	String text;
	Vector2 size;
	/**
	 * The position in terms of ScreenWidth/ScreenHeight
	 */
	Vector2 scalePos;
	/**
	 * The position in terms of pixels offset
	 */
	Vector2 offsetPos;
	Color textColor;
	Color hovColor;
	Font font;
	boolean hover = false;
	
	public boolean Visible = true;
	
	Image sprite;
	
	public Button(String txt, Font font, Vector2 posScale, Vector2 posOffset, Vector2 size, Color color1, Color color2){
		text = txt;
		scalePos = posScale;
		offsetPos = posOffset;
		textColor = color1;
		hovColor = color2;
		this.font = font;
		this.size = size;
	}
	
	public Button(Image img, Vector2 posScale, Vector2 posOffset, Vector2 size){
		sprite = img;
		scalePos = posScale;
		offsetPos = posOffset;
		this.size = size;
	}
	
	void onClick(){
		
	}
	
	void update(float delta){
		if (Visible){
			Vector2 realPos = new Vector2(
					(scalePos.x * Main.ScreenWidth + offsetPos.x),
					(scalePos.y * Main.ScreenHeight + offsetPos.y));
			
			if (sprite != null){
				hover = Input.MousePosition.x > realPos.x && Input.MousePosition.x < realPos.x + size.x &&
						Input.MousePosition.y > realPos.y && Input.MousePosition.y < realPos.y + size.y;
					
			}else{
				hover = Input.MousePosition.x > realPos.x && Input.MousePosition.x < realPos.x + size.x &&
						Input.MousePosition.y > realPos.y - size.y && Input.MousePosition.y < realPos.y;
			}
			if (hover && Input.lastMouseButtons[0] && !Input.MouseButtons[0])
				onClick();
		}
	}
	
	void draw(Graphics2D g2d){
		if (Visible){
			Vector2 realPos = new Vector2(
					(scalePos.x * Main.ScreenWidth + offsetPos.x),
					(scalePos.y * Main.ScreenHeight + offsetPos.y));
			if (sprite != null){
				g2d.drawImage(sprite, (int)realPos.x, (int)realPos.y, (int)size.x, (int)size.y, null);
			}else{
				g2d.setFont(font);
				
				g2d.setColor(Color.darkGray);
				g2d.drawString(text, 
						(int)realPos.x,
						(int)realPos.y + 3);
				g2d.setColor(textColor);
				g2d.drawString(text, 
						(int)realPos.x,
						(int)realPos.y + (hover ? 1 : -1));
			}
		}
	}
}
