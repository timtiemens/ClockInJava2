/*========================================================================
 * Clock.java
 * May 16, 2011 11:05:47 PM | ttiemens
 * Copyright (c) 2011 Tim Tiememsn
 *========================================================================
 * This file is part of ClockInJava.
 *
 *    ClockInJava is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    ClockInJava is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with ClockInJava.  If not, see <http://www.gnu.org/licenses/>.
 */
package tiemens.clock.simpleimage;


import java.awt.Graphics;
import java.awt.Image;
import java.awt.Panel;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/**
 * 
 * @author Tim Tiemens
 */
public class DisplayPanel
    extends Panel 
{
	private static final long serialVersionUID = -2829153743268504589L;

	private static Logger logger = Logger.getLogger("DisplayPanel");
    
    /**
     * background image, can be null.
     */
    private Image img_bg = null;
    
    /**
     * digit images, can not be null.
     */
    private ConvertCharacterToImage dig = null;
    
    /**
     * images to display in order.
     */
    private Image[] display_images;
    
    /**
     * current string to display
     */
    private String current_info;
    
    /**
     * number of display digits
     */
    private int num_digits;
    
    /**
     * total display width
     */
    private int w;
    
    /**
     * total display height
     */
    private int h;
    
    /**
     * digits width
     */
    private int dw;
    
    /**
     * digits height
     */
    private int dh;
    
    /**
     * digits X start coordinate
     */
    private int dx;
    
    /**
     * digits Y start coordinate
     */
    private int dy;
    
    /**
     * background image width
     */
    private int bw;
    
    /**
     * background image height
     */
    private int bh;
    
    /**
     * background image X start coordinate
     */
    private int bx;
    
    /**
     * background image Y start coordinate
     */
    private int by;
    
    /**
     * Build a void panel.
     */
    public DisplayPanel(ConvertCharacterToImage dimg) 
    {
        this(1, "", null, dimg, 1, 1);
    }
    
    /**
     * Build a display panel.
     * @param numdig number of display digits
     * @param info string to display
     * @param backgroundimage background image
     * @param dimg array of digits images
     * @param aw applet width
     * @param ah applet height
     */
    public DisplayPanel(int numdig, 
                        String info, 
                        Image backgroundimage,
                        ConvertCharacterToImage dimg,
                        int aw, 
                        int ah) 
    {
        num_digits = numdig;
        current_info = info;
        img_bg = backgroundimage; //background image
        dig = dimg; 
        w = aw;
        h = ah;
        
        setLayout(null);
        resize();
    }
    
	/**
	 * overwrite update method for double buffering
	 * @param g graphics
	 */
	public void update(Graphics g) 
	{
	    paint(g);
	}
	
	/**
	 * Set string to display
	 * @param info string to display
	 */
	public void setInfo(String info) 
	{
		current_info = info;
		runRepaintLater();
	}
	private void runRepaintLater()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
		    public void run()
		    {
		        repaint();
		    }
		});
	}
	
	/**
	 * Set the display background
	 * @param image background image
	 */
	public void setBackgroundImage(Image image) 
	{
		img_bg = image; //background image
        runRepaintLater();
	}
	
	/**
	 * Set number of display digits
	 * @param numdig number of digits on display
	 */
	public void setNumDigits(int numdig) 
	{
		num_digits = numdig;
		runRepaintLater();
	}
	
	/**
	 * Set the display digits
	 * @param c2image converter
	 */
	public void setDigitsImages(ConvertCharacterToImage c2image) 
	{
		dig = c2image;
        runRepaintLater();
	}
	
	/**
	 * Creates the Panel's peer.
	 * The peer allows you to modify the appearance of the panel without changing its functionality.
	 */
	public synchronized void addNotify() 
	{
		resize();
		super.addNotify();
	}
	
	/**
	 * Resize the display
	 */
	public void resize() 
	{
	    bw = 0;
		bh = 0;
		bx = 0;
		by = 0;
		dw = 0;
		dh = 0;
		dx = 0;
		dy = 0; //reset variables
		
		//consider background image size
		if (img_bg != null) 
		{
			bw = (int) img_bg.getWidth(this);
			bh = (int) img_bg.getHeight(this);
		}
		
		//calculate string size
		if (current_info != null) 
		{
			if (current_info.length() > num_digits) 
			{
			    // resize string to max num of digits
				current_info = current_info.substring(0, num_digits); 
			}
			
			List<Image> current_info_images = dig.convert(current_info);
			

			for (Image theimage : current_info_images)
			{
				if (theimage != null) 
				{
					dw += theimage.getWidth(this); //sum width
					dh = Math.max(dh, theimage.getHeight(this)); //calc highest digit
				}
			}
			
			display_images = current_info_images.toArray(new Image[0]);
			logger.fine("number of images = " + display_images.length);
	         //display_images = new Image[current_info_images.size()];
		}
		
		//display size (background + digits)
		//h = Math.max(dh, bh);
		//w = Math.max(dw, bw);
		
		//calc coordinates (center objects)
		dx = (int) ( (w - dw) / 2);
		dy = (int) ( (h - dh) / 2);
		bx = (int) ( (w - bw) / 2);
		by = (int) ( (h - bh) / 2);
		
		logger.fine("DisplayPanel w=" + w + " h=" + h);
		setSize(w, h);
	}
	
	/**
	 * Paint image at specified position
	 * @param gbuffer graphic context
	 * @param img image to paint
	 * @param x X coordinate
	 * @param y Y coordinate
	 */
	protected void paintImage(Graphics gbuffer, Image img, int x, int y) 
	{
		if (img != null) 
		{
			//Graphics g = getGraphics();
			if (gbuffer == null) 
			{
				return;
			}
			gbuffer.drawImage(img, x, y, this);
		}
	}
	
	/**
	 * draw button elements (border, image and label) at calculated positions
	 *  @param g the graphic area when diplay button elements
	 */
	public synchronized void paint(Graphics g) 
	{
		resize(); //calculate size and get images to display
		
		//DOUBLE BUFFERING:
		// Create an off screen image to draw on
		Image offscreen = createImage(w,h);
		Graphics bufferGraphics = offscreen.getGraphics();
		
		bufferGraphics.clearRect(0, 0, w, h); //clean digits area
		
		//paint background image
		if (img_bg != null) 
		{
			paintImage(bufferGraphics, img_bg, bx, by);
		}
		
		//paint display digits
		int posx = dx; //current digit X position
		for (Image theimage : display_images)
		{
		    //paint current digit image
			paintImage(bufferGraphics, theimage, posx, dy);
			posx += theimage.getWidth(this);
		}
		g.drawImage(offscreen,0,0,this);
	}
	
}