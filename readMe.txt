-------------
BOXPOD README
-------------

1. print out the marker (marker/marker.png)
2. assemble midi interface board with accelerometer and potentiometer, install Edirol UM-1 driver and read configuration instructions
   (http://www.roland.com/products/en/_support/dld.cfm?PRODUCT=UM-1X)
3. hook up a webcam
4. import project in eclipse (http://www.eclipse.org/downloads/, IDE java)
5. add libraries in src/libs to build path
6. compile and run as java applet

note: if you do not have the midi interfaceboard, you can also control the program using key Z for enter and the mouse as knob replacement.

keys:
- d: debug panel on/off
- z: enter, like shaking with the board
- m: mouse input on/off
- < or >: to switch between sensor input (x, y, z, pod)
 
error:
if you get this error while compiling:
jp.nyatla.nyartoolkit.NyARException: java.lang.NullPointerException
add the data folder from assets/ to the bin/ folder