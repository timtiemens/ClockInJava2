/*========================================================================
 * Main.java
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
package tiemens.clock;

import tiemens.clock.simple.MainSimpleClock;
import tiemens.clock.simpleimage.SimpleImageClockMain;


/**
 * @author ttiemens
 * 
 */
public class Main
{
    // ==================================================
    // class static data
    // ==================================================

    // ==================================================
    // class static methods
    // ==================================================

	public static void main(String[] args)
	{
	    if ((args.length == 0) ||
	         "MainSimpleClock".equalsIgnoreCase(args[0]) ||
	         "simple".equalsIgnoreCase(args[0]))
	    {
	        MainSimpleClock.main(args);
	    }
	    else if ("SimpleImageClock".equals(args[0]) ||
	             "image".equalsIgnoreCase(args[0]))
	    {
	        SimpleImageClockMain.main(args);
	    }
	    else
	    {
	        MainSimpleClock.main(args);
	    }
	}
	
    // ==================================================
    // instance data
    // ==================================================

    // ==================================================
    // factories
    // ==================================================

    // ==================================================
    // constructors
    // ==================================================

    // ==================================================
    // public methods
    // ==================================================

    // ==================================================
    // non public methods
    // ==================================================

}
