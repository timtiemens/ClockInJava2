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

import java.awt.Color;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import tiemens.clock.simple.MainSimpleClock;

/**
 * @author tim
 *
 */
public class SimpleImageClockMain
    extends JFrame
{
	private static final long serialVersionUID = -2146496413203402920L;

	/**
     * @param args
     */
    public static void main(String[] args)
    {
        ConvertCharacterToImage c2img = 
            ConvertCharacterToImageFactory.getDefault();
        
        MainSimpleClock.SimpleTimeFormatGenerator timeGenerator =
                new MainSimpleClock.SimpleTimeFormatGenerator();
        
        String disp = timeGenerator.getBiggestString();
        DisplayPanel dp = new DisplayPanel(disp.length(),
                                           disp,
                                           null, 
                                           c2img, 
                                           250, 90);
        dp.setInfo(timeGenerator.getTimeString(new java.util.Date().getTime()));
        
        new SimpleImageClockMain(dp);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new UpdateTimeTimerTask(dp, timeGenerator), 
                                  0L, 
                                  1000L);
        timer.scheduleAtFixedRate(new ChangeConvertCharacterTimerTask(dp),
                                  0L,
                                  5000L);
    }

    public SimpleImageClockMain(DisplayPanel dp)
    {
        
        JFrame f = this;
        
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.add(dp);
        f.pack();
        //f.setSize(300, 100);
        f.setVisible(true);
    }
    
    public static class UpdateTimeTimerTask
        extends TimerTask
    {
        final private DisplayPanel displayPanel;
        final private MainSimpleClock.SimpleTimeFormatGenerator timeGenerator;

        public UpdateTimeTimerTask(final DisplayPanel inDisplayPanel,
                                   final MainSimpleClock.SimpleTimeFormatGenerator 
                                                      inTimeGenerator)
        {
            displayPanel = inDisplayPanel;
            timeGenerator = inTimeGenerator;
        }
        
        @Override
        public void run() 
        {
            displayPanel.setInfo(timeGenerator
                                 .getTimeString(new java.util.Date().getTime()));
        }
    } // timer

    public static class ChangeConvertCharacterTimerTask
        extends TimerTask
    {
        final private DisplayPanel displayPanel;
        private Iterator<ConvertCharacterToImage> iter = null;
        private final Color originalBackground;
        public ChangeConvertCharacterTimerTask(final DisplayPanel inDisplayPanel)
        {
            displayPanel = inDisplayPanel;
            iter = null;
            originalBackground = displayPanel.getBackground();
        }
        
        @Override
        public void run() 
        {
            ConvertCharacterToImage next = getNext();
            
            final Color nextbg = next.getPreferBackgroundColor();
            if (nextbg != null)
            {
                displayPanel.setBackground(nextbg);
            }
            else
            {
                displayPanel.setBackground(originalBackground);
            }
            
            displayPanel.setDigitsImages(next);
        }
        
        private ConvertCharacterToImage getNext()
        {
            if ((iter == null) ||
                (! iter.hasNext()))
            {
                iter = ConvertCharacterToImageFactory.iterateAll();
            }
            return iter.next();
        }
    } // timer
}
