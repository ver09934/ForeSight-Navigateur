# ForeSight Navigateur

The navigation app for the ForeSight project. For the embedded software used on the anklet, [click here](https://google.com).

## Build Requirements
* Android SDK (Obviously)
	* Need Google Play Services SDK Component (to allow use of the Google Location Services API) - more info about this can be found [here](https://developers.google.com/android/guides/setup)
* Your own Maps API key (to be placed in app/src/debug/res/values/google_maps_api.xml)
* Your own Directions API Key (to be placed in app/src/debug/res/values/google_maps_api.xml)
	* You might be able to use the same key, but it wasn't working for me
* Everything else should be covered by the build.gradle files

It was helpful when using the Java Client for Google Maps Services to clone [the repository](https://github.com/googlemaps/google-maps-services-java) and generate the javadoc for it.

## Notes

If time runs short, this [Google-Directions-Android](https://github.com/jd-alexander/Google-Directions-Android) project's library could be used to simplify obtaining a route to display.
(It will have to be calculated what level of curvature of the polyline should be considered a 'turn'). 
