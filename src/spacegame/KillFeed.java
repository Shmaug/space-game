package spacegame;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class KillFeed {
	private static ArrayList<String> feed = new ArrayList<String>();
	
	public static void log(String text){
		feed.add(text);
		if (feed.size() > 5)
			feed.remove(0);
	}
	
	public static void draw(Graphics2D g2d, Font font){
		g2d.setColor(new Color(0,0,0,.5f));
		g2d.fillRect(Main.ScreenWidth - 300, 20, 270, 120);
		g2d.setFont(font);
		g2d.setColor(Color.white);
		int y = 30;
		for (int i = feed.size()-1; i >= 0; i--){
			g2d.drawString(feed.get(i), Main.ScreenWidth - g2d.getFontMetrics(font).stringWidth(feed.get(i)) - 50, y);
			y += 20;
		}
	}
}
