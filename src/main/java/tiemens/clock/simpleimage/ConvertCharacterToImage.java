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
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertCharacterToImage
{
	private final Map<Character, Image> char2image;
    private final Color preferBackgroundColor;
    
    public ConvertCharacterToImage(final Map<Character, Image> in,
    		                       final Color inPreferBackgroundColor)
    {
        char2image = new HashMap<Character, Image>();
        char2image.putAll(in);
        
        preferBackgroundColor = inPreferBackgroundColor;
    }
    
    public Color getPreferBackgroundColor()
    {
    	return preferBackgroundColor;
    }
    
    public Image convert(final Character c)
    {
        return char2image.get(c);
    }
    
    public List<Image> convert(final String s)
    {
        List<Image> ret = new ArrayList<Image>(s.length());
        for (int i = 0, n = s.length(); i < n; i++)
        {
            ret.add(char2image.get(s.charAt(i)));
        }
        return ret;
    }
}
