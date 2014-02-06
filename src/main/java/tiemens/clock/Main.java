/*========================================================================
 * Main.java
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
