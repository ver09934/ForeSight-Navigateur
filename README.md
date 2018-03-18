# ForeSight Navigateur

The navigation app for the ForeSight project. For the embedded software used on the anklet, [click here](https://example.com).

## Build Requirements
* Android SDK (Obviously)
	* Need Google Play Services SDK Component (to allow use of the Google Location Services API) - more info about this can be found [here](https://developers.google.com/android/guides/setup)
	* Just install all the things it tells you to when IntelliJ/Anroid Studio yells at you
* Your own Maps API key and Directions API key (to be placed in app/src/debug/res/values/google_maps_api.xml)
	* You could also just use mine that I accidentally pushed to this repo (if this was a "real project", no, I wouldn't have done that)
	* You might be able to use the same key for both, but it wasn't working for me

## Bluetooth Stuff
* Our HC-06 MAC Address: 00:14:03:05:FF:E6 
* Useful tutorial for the HC-06 with Android: [Control an Arduino with Bluetooth](https://www.allaboutcircuits.com/projects/control-an-arduino-using-your-phone/)

## TODO
* Look into storing API Keys in environment variables [ideas link](https://github.com/mapbox/mapbox-gl-native/issues/713)
* Replace potentially compromised API Keys with new ones, and deactivate the old ones
* (If have time) work on improving user input (input address, locations API, autocomplete, screen-reading, etc.)
* Think about calculating what level of curvature of the polyline should be considered a 'turn' (see algorithm.txt)
* Bluetooth planning
	* Handle pairing and connectivity through settings
	* Use app to read and write from connection
	* Will still need to determine which device to read/write to

## Notes
* If time runs short, this [Google-Directions-Android](https://github.com/jd-alexander/Google-Directions-Android) project's library could be used to simplify obtaining a route to display.
