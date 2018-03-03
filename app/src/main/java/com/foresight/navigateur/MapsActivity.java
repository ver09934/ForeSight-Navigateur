package com.foresight.navigateur;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMapLongClickListener {

    // General
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;

    // Assorted
    boolean paused = false;
    private TextView mMapInstructionsView;

    // Using play services location
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    // Locations obtained using play services location
    Location currentLocation = null; // Set to null to start
    LatLng currentLatLng = null; // Set to null to start

    //Previous location
    Location previousLocation = null;
    LatLng previousLatLng = null;

    // Bearing between previous and current location
    Double currentBearing = null;

    // Desired Location
    private Marker selectedPointMarker = null;
    LatLng selectedLatLng = null;

    // For original zoom to location
    private boolean isFirstTime = true;

    private boolean navigationIsActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mMapInstructionsView = findViewById(R.id.map_instructions_text);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Show blue location dot and thingy to bring you there
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        setupLocationRequest();
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {

                    // Set current location to previous location - do this before we update currentLocation
                    // TODO: Create a buffer-like ArrayList to store the past 10 or 20 locations - makes it easy to do running averages, etc.
                    if (currentLocation != null) {
                        previousLocation = currentLocation;
                        previousLatLng = currentLatLng;
                    }

                    // addMarkerAndZoomTo(location); // Update UI with location data

                    currentLocation = location;
                    currentLatLng = getLatLngFromLocation(location);

                    //System.out.println("BEARING BOI: " + location.getBearing());
                    Toast.makeText(getApplicationContext(),
                            "Bearing from 1 Location: " + location.getBearing() + "\nBearing from 2 Locations: " + ((currentBearing != null) ? currentBearing: "null"),
                            Toast.LENGTH_SHORT).show();

                    zoomFirstTime();

                    if (!paused) {
                        // Toast.makeText(getApplicationContext(), "Current Coordinates:\n" + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG).show();
                        // Toast.makeText(getApplicationContext(), "Current location:\n" + location, Toast.LENGTH_LONG).show();
                    }

                }
            }
        };
        startLocationUpdates(); //Must be called AFTER mLocationCallback is instantiated or it will throw a null pointer exception!

        mMap.setOnMapLongClickListener(this);
    }

    protected void setupLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, getMainLooper()); //Also works fine if looper arg is set to null
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        // startLocationUpdates();
    }

    //-------------------Working with Routes and Coordinates, and Random Utils-----------------------------

    private void updateCurrentBearing() {
        if (currentLocation != null && previousLocation != null) {
            currentBearing = (double) previousLocation.bearingTo(currentLocation);
        }
    }

    private LatLng getLatLngFromLocation(Location inputLocation) {
        return new LatLng(inputLocation.getLatitude(), inputLocation.getLongitude());
    }

    //------------------------Directions-------------------------------------------------------

    private GeoApiContext getGeoContext() {
        return new GeoApiContext.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .queryRateLimit(3)
                .apiKey(getString(R.string.directions_key))
                .build();
    }

    // Future: origin and destination can also be exact address strings - get then using Places API
    private void getNewDirectionsResult(LatLng origin, LatLng destination) {
        if (currentLatLng != null && selectedLatLng != null) {
            // Weird solution for a weird problem
            com.google.maps.model.LatLng convertedOrigin = new com.google.maps.model.LatLng(origin.latitude, origin.longitude);
            com.google.maps.model.LatLng convertedDestination = new com.google.maps.model.LatLng(destination.latitude, destination.longitude);

            DateTime now = new DateTime();
            try {
                /*
                DirectionsResult results = new DirectionsApiRequest(getGeoContext())
                        //.newRequest(getGeoContext())
                        .mode(TravelMode.WALKING)
                        .origin(convertedOrigin)
                        .destination(convertedDestination)
                        .departureTime(now)
                        .await();
                */
                DirectionsResult results = DirectionsApi
                        .newRequest(getGeoContext())
                        .mode(TravelMode.WALKING)
                        .origin(convertedOrigin)
                        .destination(convertedDestination)
                        .departureTime(now)
                        .await();

                Toast.makeText(getApplicationContext(), "Directions obtained", Toast.LENGTH_SHORT).show();

                addMarkersToMap(results);
                addPolyline(results);

                navigationIsActive = true;

            } catch (com.google.maps.errors.ApiException e) {
                e.printStackTrace();
            } catch (java.lang.InterruptedException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Current or destination LatLng are null", Toast.LENGTH_LONG).show();
        }
    }

    private void addMarkersToMap(DirectionsResult results) {
        removeSelectedPointMarker();
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(results.routes[0].legs[0].startLocation.lat, results.routes[0].legs[0].startLocation.lng))
                .title(results.routes[0].legs[0].startAddress));
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(results.routes[0].legs[0].endLocation.lat, results.routes[0].legs[0].endLocation.lng))
                .title(results.routes[0].legs[0].endAddress)
                .snippet(getEndLocationTitle(results)));
    }

    private String getEndLocationTitle(DirectionsResult results){
        return  "Time: "+ results.routes[0].legs[0].duration.humanReadable + "   Distance: " + results.routes[0].legs[0].distance.humanReadable;
    }

    private void addPolyline(DirectionsResult results) {
        List<LatLng> decodedPath = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());
        mMap.addPolyline(new PolylineOptions().addAll(decodedPath));
    }

    public void stopNavigation() {
        if (navigationIsActive) {
            mMap.clear();
            navigationIsActive = false;
            addSelectedPointMarker();
        }
    }

   //------------------------Map Long-Clicking to Select Destination---------------------------

    @Override
    public void onMapLongClick(LatLng point) {
        if (selectedPointMarker != null && !navigationIsActive)
            selectedPointMarker.remove();
        if (!navigationIsActive) {
            mMapInstructionsView.setText(getString((R.string.map_instructions_point_pressed), point.latitude, point.longitude));
            selectedPointMarker = addMarkerFromLatLng(point);
            selectedLatLng = point;
        }
    }

    // Reset end location and stop navigation
    private void resetPointSelection() {
        if (!navigationIsActive) {
            if (selectedPointMarker != null)
                selectedPointMarker.remove();
            selectedPointMarker = null;
            selectedLatLng = null;
            mMapInstructionsView.setText(getString(R.string.map_instructions));
        }
    }

    private void removeSelectedPointMarker() {
        if (selectedPointMarker != null) {
            selectedPointMarker.remove();
            selectedPointMarker = null;
        }
    }

    private void addSelectedPointMarker() {
        if (selectedLatLng != null)
            selectedPointMarker = addMarkerFromLatLng(selectedLatLng);
    }

    //------------------------Camera Functions----------------------------

    private void zoomTo(Location inputLocation, float zoomLevel) {
        LatLng myLatLng = getLatLngFromLocation(inputLocation);
        CameraPosition currentLocationCameraPosition = new CameraPosition.Builder().target(myLatLng).zoom(zoomLevel).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentLocationCameraPosition));
    }

    private Marker addMarkerFromLocation(Location inputLocation, String label) {
        LatLng myLatLng = getLatLngFromLocation(inputLocation);
        Marker markerOut = mMap.addMarker(new MarkerOptions().position(myLatLng).title(label));
        return markerOut;
    }

    // Label location with coordinates
    private Marker addMarkerFromLoaction(Location inputLocation) {
        return addMarkerFromLocation(inputLocation, inputLocation.getLatitude() + ", " + inputLocation.getLongitude());
    }

    private Marker addMarkerFromLatLng(LatLng inputLatLng, String label) {
        return mMap.addMarker(new MarkerOptions().position(inputLatLng).title(label));
    }

    // Label location with coordinates
    private Marker addMarkerFromLatLng(LatLng inputLatLng) {
        return addMarkerFromLatLng(inputLatLng, inputLatLng.latitude + ", " + inputLatLng.longitude);
    }

    public void zoomFirstTime() {
        if (isFirstTime) {
            zoomTo(currentLocation, 13);
            isFirstTime = false;
        }
    }

    //------------------------User Functions------------------------------

    // Reset end point selection
    public void functionOne(View view) {
        resetPointSelection();
    }

    // Request directions
    public void functionTwo(View view) {
        getNewDirectionsResult(currentLatLng, selectedLatLng);
    }

    // Cancel navigation
    public void functionThree(View view) {
        stopNavigation();
    }

    public void functionFour(View view) {}

}
