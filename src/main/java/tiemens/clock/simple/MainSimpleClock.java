/*========================================================================
 * MainSimpleClock.java
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
package tiemens.clock.simple;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.DecimalFormat;
import java.util.Calendar;

import javax.swing.JFrame;

/**
 * A simple Clock
 */
public class MainSimpleClock 
extends javax.swing.JComponent 
{
    // ==================================================
    // class static data
    // ==================================================

    /**
     * generated serialUID
     */
    private static final long serialVersionUID = -902418120271381675L;


    // ==================================================
    // class static methods
    // ==================================================

    public static void main(String[] args) 
    {
        JFrame window = new JFrame("Clock In Java");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MainSimpleClock clock = new MainSimpleClock();
        clock.setFont(new Font("sansserif", Font.PLAIN, 48));

        window.setContentPane(clock);

        clock.start();

        window.pack();
        window.setVisible(true);

    }

    // ==================================================
    // instance data
    // ==================================================

    private SimpleTimeFormatGenerator timeGenerator;
    private volatile boolean done = false;
    private Thread thread;

    // ==================================================
    // factories
    // ==================================================

    // ==================================================
    // constructors
    // ==================================================

    public MainSimpleClock() 
    {
        timeGenerator = new SimpleTimeFormatGenerator();
    }



    // ==================================================
    // public methods
    // ==================================================

    public void start() 
    {
        if (thread != null) 
        {
            stop();
            thread = null;
        }

        thread = new Thread(new Runnable() 
        {
            public void run() 
            {
                while (!done) 
                {
                    MainSimpleClock.this.repaint();
                    try 
                    {
                        Thread.sleep(700);
                    }
                    catch (InterruptedException e) 
                    {
                        done = true;
                    }
                }
            }
        });

        thread.start();
    }

    public void stop() 
    {
        done = true;
    }

    /**
     *  Get current time and draw centered time string. 
     */
    @Override
    public void paint(Graphics g) 
    {
        String  s = timeGenerator.getTimeString(new java.util.Date().getTime());

        FontMetrics fm = getFontMetrics(getFont());
        int x = (getSize().width - fm.stringWidth(s)) / 2;
        // System.out.println("Size is " + getSize());
        final int ascent = fm.getAscent();
        final int descent = fm.getDescent();
        final int top = 0;
        final int bottom = getSize().height;

        int y = (top + ((bottom + 1 - top)) / 2) - 
                ((ascent + descent) / 2) +
                ascent;

        g.drawString(s, x, y);
    }

    @Override
    public Dimension getPreferredSize() 
    {
        return new Dimension(300, 100);
    }

    @Override
    public Dimension getMinimumSize() 
    {
        return new Dimension(50, 10);
    }

    // ==================================================
    // non public methods
    // ==================================================

    public static class SimpleTimeFormatGenerator
    {
        protected DecimalFormat tf;
        protected DecimalFormat tflz;

        public SimpleTimeFormatGenerator()
        {
            tf = new DecimalFormat("#0");
            tflz = new DecimalFormat("00");
        }

        public String getTimeString(final long millis)
        {
            Calendar myCal = Calendar.getInstance();
            myCal.setTimeInMillis(millis);
            int hour = myCal.get(Calendar.HOUR);
            if (hour == 0) 
            {
                hour = 12;
            }

            StringBuffer sb = new StringBuffer();
            sb.append(tf.format(hour));
            sb.append(':');
            sb.append(tflz.format(myCal.get(Calendar.MINUTE)));
            sb.append(':');
            sb.append(tflz.format(myCal.get(Calendar.SECOND)));
            return sb.toString();
        }

        public String getBiggestString() 
        {
            return "00:00:00";
        }
    } // class
}
