/*========================================================================
 * ConvertCharacterToImage.java
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
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertCharacterToImage
{
    private final String name;
    private final Map<Character, Image> char2image;
    private final Color preferBackgroundColor;

    public ConvertCharacterToImage(final String inName,
                                   final Map<Character, Image> in,
                                   final Color inPreferBackgroundColor)
    {
        name = inName;
        char2image = new HashMap<Character, Image>();
        char2image.putAll(in);

        preferBackgroundColor = inPreferBackgroundColor;
    }

    public String getName() 
    {
        return name;
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
