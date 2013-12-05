worldwind-vr
============

This repository houses a modification to NASA worldwind with Virtual Reality capabilities.

WorldWindVR - A Virtual Reality Implementation for NASA WorldWind

The most recent distribution can currently be downloaded from:

https://www.dropbox.com/s/ihe3ton0n0laqlu/WorldWindVR.zip

====Running====

To run WorldWindVR, simply double click RunWorldWindVR.exe or RunWorldWindVR.bat.  It has been tested so far only on Windows, under both 32-bit 
and 64-bit versions of Java (only tested with 1.6, but 1.5 may work).  Mac and Linux support are conceivable, but we are 
waiting for someone to implement a nice wrapper for the Oculus Rift API on those platforms.

====Setting Custom Locations====

If you'd like to visit a specific location, you can add a description along with it's coordinates to
the file locations.txt.  It will then be added to a list of locations which you can switch between using
the space bar.

====Controls====

Arrow Keys: Movement
Space Bar: Switches between a set of neat locations
Escape: Exits the application
Mouse: Mouse look has not yet been integrated, so you can only look around with the rift itself

Note that the first time you visit a new location, it takes a while for imagery data to load
and cache, but when you visit the same location later the imagery will usually be present from 
the get go (including if you exit the application and run WorldWindVR at a later date).

====Pre-caching imagery data====

WorldWindVR is a bit bland until the high resolution imagery loads, so if this is your first time
running, consider checking the 'Start in Imagery Pre-cache Mode' option in the launch dialog and letting
it run for 5-15 minutes.  It will automatically survey the locations in locations.txt, prompting the imagery
for those locations to load.  Once the data is cached, it will be there the next time you start WorldWindVR.

==== Acknowledgements ====

Thanks are due to the following people:
 - 38leinaD for his JRift project as well as the original LWJGL shader (which we adapted for JOGL
   and without which we would have been hopelessly lost, as we had no prior 3D graphics experience)
 - The Minecrift guys (mabrowning et. al) for updating JRift to work with the latest Rift SDK
 - NASA and its WorldWind development team
 - Palmer and everyone at Oculus VR for this amazing device and toolkit
![Screenshot](https://raw.github.com/danielr2e/worldwind-vr/master/screenshots/WorldWindVR1_GrandCanyon.jpg)
![Screenshot](https://raw.github.com/danielr2e/worldwind-vr/master/screenshots/WorldWindVR2_HalfDome.jpg)
![Screenshot](https://raw.github.com/danielr2e/worldwind-vr/master/screenshots/WorldWindVR3_Cascades.jpg)
![Screenshot](https://raw.github.com/danielr2e/worldwind-vr/master/screenshots/WorldWindVR4_Globe.jpg)