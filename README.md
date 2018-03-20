# ForeSight Navigateur

The navigation app for the ForeSight project. For the embedded software used on the anklet, see [here](https://example.com).

## Setup
First, you must create the gitignored file ```app/src/debug/res/values/google_maps_api.xml```, which is ignored in ```/app/.gitignore```. After you do this, copy the following text into it:
 ```xml
 <resources>
    <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">YOUR_MAPS_API_KEY</string>
    <string name="directions_key" templateMergeStrategy="preserve" translatable="false">YOUR_DIRECTIONS_API_KEY</string>
</resources>

  ```
  Then, replace ```YOUR_MAPS_API_KEY``` and ```YOUR_DIRECTIONS_API_KEY``` with your actual keys obtained from [https://console.developers.google.com/](https://console.developers.google.com/). Remember to enable the correct API components for each key, and to set the proper key restrictions.

## Building
* Android SDK (Obviously)
	* Need Google Play Services SDK Component (to allow use of the Google Location Services API) - more info about this can be found [here](https://developers.google.com/android/guides/setup)
	* Just install all the things it tells you to when IntelliJ/Android Studio yells at you...

