# Magnetic-Mapper
A software used for drawing azimuthal equidistant maps that are magnetic declination -corrected.  
By corrected I mean that one could follow the compass to the north using such a map, and actually move north.

## The Research Problem
I was watching an [interesting video](https://www.youtube.com/watch?v=QSporLH2nIw) by a sailor, Herve Riboni, 
who had sailed around the world and had a theory about the magnetic declination -system being used to manipulate 
the perceived shape of our world. In this video, he explained a potential method for producing a more accurate 
map of our world. I was curious about this problem and saw it as an interesting technical challenge, so I 
decided to tackle the problem.

So the question is: Can we produce a reasonable world map, assuming that, instead of there being such a thing as 
magnetic declination, the map points would actually reside in a location that would correspond with the "false" north? 
In other words, does Herve's idea of magnetic declination being a cover-up make sense when visualized in such a manner?

If you're as curious about the answer as I am, you can jump right to the [results](#results).

## Methods
My algorithm solves the following challenges in order to produce a "magnetically corrected" map:
1. I convert an existing azimuthal equidistant map into a set of digital coordinates
2. I acquire a magnetic declination for each coordinate
3. I draw a map where each point has been shifted in a way where the original compass reading points towards the true north

### Producing a Digital Map
I performed an image analysis on 
an [azimuthal equidistant map](https://github.com/Mikkomario/Magnetic-Mapper/blob/master/data/input/maps/azimutal.jpg) 
to determine the terrain type of each pixel, based on its color. Basically the values are water (blue) and land (not blue). 

I then identified the most important pixels, i.e. the coasts by finding land pixels that have water pixels near them. 
These would be my main data points. With a low resolution map (480 x 480 pixels), there are around 6 700 of such pixels.

### Acquiring Magnetic Declination
I found an [open API for magnetic declination](https://www.ngdc.noaa.gov/geomag/calculators/magcalc.shtml#declination), 
which is awesome. If I didn't, I probably wouldn't have attempted this project in the first place. 
The main challenge with the API is that it only accepts 50 connections per second, and connections are not keep-alive. 
What that means is that using this API to calculate a huge number of declination values is very slow.

I chose to solve this challenge by using the same declination value for points that are close to each other. 
The way I chose to do this was by dividing the world into a grid and to calculate the declination for grid cell 
center-points only, where appropriate. I made the grid configurable, so that by using a more dense grid I could get 
more accurate results, if I wanted to. I also chose to save all read declinations locally in order to minimize 
requests, in case I wanted to run the application again.

Since I was working on an azimuthal equidistant map, where the north is at the center and where the world resembles 
a great circle (much like depicted in 
the [Bible](https://www.biblegateway.com/quicksearch/?quicksearch=circle+of+the+earth&version=KJV), interestingly), 
I wanted to use 
a [circular grid](https://github.com/Mikkomario/Magnetic-Mapper/blob/master/data/output/maps/grid-10-8.png) instead of 
a rectangular grid. Creating such a grid was an interesting challenge in itself.

At this point I also decided to skip declination calculations for all points below the 60th south parallel,
i.e. 60 degrees south latitude. This is because:
1. The declination values get all over the place near the south, which would likely not make the map more readable
2. Almost nobody could verify these values anyway, thanks to the Antarctic Treaty

By using this method, I was able to reduce the program run-time to just a couple of minutes. The accuracy does suffer 
slightly, but I don't think that's a problem in this kind of prototype / research project.

### Drawing a Magnetically Corrected Map
This part was pretty much putting the other puzzle pieces together. I had already drawn maps during the development 
process in order to visualize the earlier results, for testing. Now I simply had to add the declination-correction.

Interestingly, the declination correction logic, while kind of simple, is not that easy to understand - 
at least for a non-sailor like myself. Using multiple coordinate systems at once was a bit of a challenge. 
There's latitude + longitude -system where north and east are positive, and down and center are zero. 
Then there's the declination where east is also positive, but this time clockwise instead of counter-clockwise. 
On top of this I use the Utopia models, where clockwise is positive and zero is to the right. Then we have pixel 
coordinates where top-left is zero and right and down are positive. You get the idea...

I created a new coordinate system suitable for AE maps, where (0,0) is located at the north, 
unit length is the equator radius and all points are represented using vectors. This makes correcting rotations 
around the north easy to understand.

## Results
I managed to produce a number 
of [prototype maps](https://github.com/Mikkomario/Magnetic-Mapper/tree/master/data/output/maps). They are low 
resolution and kind of messy, but you can get the general idea from them. I found the corrected map most useful 
(and pleasant to look at), where I had 
[colored the points](https://github.com/Mikkomario/Magnetic-Mapper/blob/master/data/output/maps/corrected-declination-colored.png) 
according to the distance shifted during the "correction" process. I recommend you to check 
it out in the maps -output directory.

By observing the map we can see a couple of things:
- South America becomes really stretched and gets very close to Africa on the east. 
  The southern tip of South America, on the other hand takes a huge stretch to the west.
- North America gets stretched on both sides, becoming much larger
- Greenland gets relocated very close to Norway
- Africa remains pretty much the same, except for the south, as well as for Madagascar, 
  which get swept into the east ocean.
- Interestingly, Australia becomes more reasonable-looking as it gets shrunk. This compensates nicely for the 
  azimuthal equidistant map's tendency to stretch southern continents overly (?) much.

I didn't like the fact that the pixels got separated from each other. It made the maps quite hard to read. 
I could fix this issue by converting the coasts into polygons before relocating them. This would, however, take 
a lot of time to do.

As far as the research results go, I was a bit disappointed. 
I didn't like how South Africa got broken into pieces and the unequal stretching of the South America seems a little 
"unrealistic". However, the northern continents looked surprisingly reasonable, which I didn't take for granted, 
considering that I was shifting points by quite many degrees at places.

## Conclusion
It would be very interesting to hear Herve's take on my solution. Based on the findings I would say I either made 
an error in my assumptions and/or calculations, or Herve's hypothesis can't be taken as literally as I did, 
at least for the south.

It would also be nice if some people could provide alternative declination measurements. Technically all you need 
to do is to take a compass and to compare it's north heading to the location of the Polaris (i.e. the north star). 
It was very suspicious that I couldn't find more than just one data source for magnetic declination values. 
The fact (?) that these values are controlled by some singular large agency makes Herve's hypothesis more likely 
to be true, in my opinion. It's also strange how all the world is covered. I'd say it's very unlikely these are
(all) real measurements. It would be interesting to know how they were derived.

## Licensing and Used Libraries
During this project, I used the following [Utopia](https://github.com/Mikkomario/Utopia-Scala) libraries I've created:
1. [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow) in standard data structures and a lot of stuff
2. [Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch) in json processing
3. [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access), 
  [Utopia Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple) and 
  [Utopia Annex](https://github.com/Mikkomario/Utopia-Scala/tree/master/Annex) when making requests to the declination API
4. [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm) in vector mathematics, angles, colors etc.
5. [Utopia Genesis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Genesis) in image-processing

I recommend you to check them out. They're all available under MIT license (i.e. don't worry about license). 
If you do use them, it would be awesome if you could email me about it. Then I could take your use-case into 
consideration when making changes and new releases.

If you want to use code from this project, please go ahead and do so.  
I would appreciate it if you would pass a reference when/if you do.