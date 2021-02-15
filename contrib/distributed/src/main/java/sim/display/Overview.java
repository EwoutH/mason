package sim.display;
import sim.field.partitioning.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import sim.util.*;

public class Overview extends JComponent
	{
	private static final long serialVersionUID = 1L;

	public static final double SIZE_SCALING = 0.125;
	IntRect2D[] bounds = new IntRect2D[0];
	int outerX;
	int outerY;
	int outerWidth = 1;
	int outerHeight = 1;
	int current = -1;
	SimStateProxy proxy;
	
	public Overview(SimStateProxy proxy)
		{
		this.proxy = proxy;
		addMouseListener(new MouseAdapter()
			{
			public void mouseClicked(MouseEvent e)
				{
		int width = getBounds().width;
		int height = getBounds().height;
				for(int i = 0; i < bounds.length; i++)
					{
					double x = (bounds[i].ul().x - outerX) / (double)(outerWidth) * width;
					double y = (bounds[i].ul().y - outerY) / (double)(outerHeight) * height;
					double w = (bounds[i].br().x - bounds[i].ul().x) / (double)(outerWidth) * width;
					double h = (bounds[i].br().y - bounds[i].ul().y) / (double)(outerHeight) * height;
					int ex = e.getX();
					int ey = e.getY();
					if (ex >= x && ex < x + w &&
						ey >= y && ey < y + h) // found it
						{
						changeCurrentProcessor(i); 
						break;
						}
					}
				}
			});
		}
		
	public void changeCurrentProcessor(int val)
		{
		setCurrent(val);
		proxy.setCurrentProcessor(val);
		repaint();
		}
	
	public void setCurrent(int current) { this.current = current; }
		
	public void update(ArrayList<IntRect2D> b) 	// , int aoi)
		{
		IntRect2D[] bounds = b.toArray(new IntRect2D[0]);
		
		// strip off aoi
		//int[] extra = new int[] { -aoi, -aoi, -aoi, -aoi };
		for(int i = 0; i < bounds.length; i++)
			{
			// bounds[i] = bounds[i].resize(extra);
			if (i == 0 || bounds[i].ul().x < outerX) outerX = bounds[i].ul().x;
			if (i == 0 || bounds[i].ul().y < outerY) outerY = bounds[i].ul().y;
			if (i == 0 || bounds[i].br().x > outerWidth) outerWidth = bounds[i].br().x; 
			if (i == 0 || bounds[i].br().y > outerHeight) outerHeight = bounds[i].br().y;
			}
		outerWidth-=outerX;
		outerHeight-=outerY;
		// so we don't divide by zero
		if (outerWidth <= 0) outerWidth = 1;
		if (outerHeight <= 0) outerHeight = 1;
		this.bounds = bounds;
		}
	
	
	public void paintComponent(Graphics graphics)
		{
		Graphics2D g = (Graphics2D)graphics;
		int width = getBounds().width;
		int height = getBounds().height;
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);

		int size = (int)((1.0 / Math.sqrt(bounds.length)) * Math.min(width, height) * SIZE_SCALING) + 1;
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
		g.setFont(font);
		g.setColor(Color.WHITE);
		FontMetrics fm = g.getFontMetrics(font);
		int fmHeight = fm.getAscent();			// we only need ascent since we're doing numbers
		
		for(int i = 0; i < bounds.length; i++)
			{
			String str = "" + i;
					double x = (bounds[i].ul().x - outerX) / (double)(outerWidth) * width;
					double y = (bounds[i].ul().y - outerY) / (double)(outerHeight) * height;
					double w = (bounds[i].br().x - bounds[i].ul().x) / (double)(outerWidth) * width;
					double h = (bounds[i].br().y - bounds[i].ul().y) / (double)(outerHeight) * height;
			int fmWidth = fm.stringWidth(str);
			
			if (current == i)
				{
				g.fillRect((int)x, (int)y, (int)w, (int)h);
				g.setColor(Color.BLACK);
				g.drawString(str, (float)(x + w * 0.5 - fmWidth * 0.5), (float)(y + h * 0.5 + fmHeight * 0.5));
				g.setColor(Color.WHITE);
				}
			else
				{
				g.drawRect((int)x, (int)y, (int)w, (int)h);
				g.drawString(str, (float)(x + w * 0.5 - fmWidth * 0.5), (float)(y + h * 0.5 + fmHeight * 0.5));
				}
			}
		}
	}