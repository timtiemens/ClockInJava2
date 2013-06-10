ClockInJava2
============

Simple, easy, clock implementation(s), v2

2013 Jun created

####Starting Command Line
$ gradle build
$ java -cp build/classes tiemens.clock.Main
$ java -cp build/classes tiemens.clock.Main simple
$ java -cp build/classes tiemens.clock.Main image

"simple" - is the simplest, text-based display

"image" - uses icon glyphs for 0-9 digits, and cycles through all available sizes and colors
   The available sizes are 6x9, 9x15, 12x21, 14x23, 26x31.
   The available colors are black, blue, green, red, white and yellow.
   Not all combinations are available.


####Updates (from a mere 2 years ago)
The following items have been changed from ClockInJava:
 * gradle
 * standard java directory layout (src/main, src/test, etc.)
 * remove onejar
 * remove instancer/antlr [instancer is in its own project now]



