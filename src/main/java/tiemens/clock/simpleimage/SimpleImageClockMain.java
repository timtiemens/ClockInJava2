/*========================================================================
 * SimpleImageClockMain.java
 * June 6, 2013 ttiemens
 *========================================================================
 * This file is part of ClockInJava2.
 *
 * Copyright (c) 2013, Tim Tiemens
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * You should have received a copy of the BSD-2-Clause license
 * along with this program.  If not, see <http://opensource.org/licenses/BSD-2-Clause>.
 *   
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
            
            System.out.println("Switching to image set " + next.getName());
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
