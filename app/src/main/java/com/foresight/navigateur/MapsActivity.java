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
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import org.joda.time.DateTime;

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

    // Marker at long-clicked location
    private boolean pointSelectionIsLocked = false;
    private Marker selectedPointMarker = null;

    // Obtained by long-click
    LatLng desiredLatLng = null;


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

                    // addMarkerAndZoomTo(location); // Update UI with location data

                    currentLocation = location;
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

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

    //------------------------Directions-------------------------------------------------------


    private GeoApiContext getGeoContext() {
        return new GeoApiContext.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .queryRateLimit(3)
                .apiKey(getString(R.string.google_maps_key))
                .build();
    }

    // Future: origin and destination can also be exact address strings - get then using Places API
    private void getNewDirectionsResult(LatLng origin, LatLng destination) {
        // Weird solution for a weird problem
        com.google.maps.model.LatLng convertedOrigin = new com.google.maps.model.LatLng(origin.latitude, origin.longitude);
        com.google.maps.model.LatLng convertedDestination = new com.google.maps.model.LatLng(destination.latitude, destination.longitude);

        DateTime now = new DateTime();
        try {
            DirectionsResult results = DirectionsApi
                    .newRequest(getGeoContext())
                    .mode(TravelMode.WALKING)
                    .origin(convertedOrigin)
                    .destination(convertedDestination)
                    .departureTime(now)
                    .await();
            Toast.makeText(getApplicationContext(), "The doot worked", Toast.LENGTH_SHORT).show();
            //return results;
        }
        catch (com.google.maps.errors.ApiException e) {
            Toast.makeText(getApplicationContext(), "The doot failed 1", Toast.LENGTH_SHORT).show();
        }
        catch (java.lang.InterruptedException e) {
            Toast.makeText(getApplicationContext(), "The doot failed 2", Toast.LENGTH_SHORT).show();
        }
        catch (java.io.IOException e) {
            Toast.makeText(getApplicationContext(), "The doot failed 3", Toast.LENGTH_SHORT).show();
        }
        // Should probably do an actual something if an exception is caught

        // This is a BAD SOLUTION! DO NOT TRY AT HOME!
        // return getNewDirectionsResult(origin, destination);
    }

   //------------------------Map Long-Clicking to Select Destination---------------------------

    @Override
    public void onMapLongClick(LatLng point) {
        if (!pointSelectionIsLocked) {
            mMapInstructionsView.setText(getString((R.string.map_instructions_point_pressed), point.latitude, point.longitude));
            selectedPointMarker = addMarkerFromLatLng(point);
            desiredLatLng = point;
        }
        pointSelectionIsLocked = true;
    }

    // Reset end location and stop navigation
    public void resetPointSelection() {
        if (selectedPointMarker != null)
            selectedPointMarker.remove();
        desiredLatLng = null;
        mMapInstructionsView.setText(getString(R.string.map_instructions));
        pointSelectionIsLocked = false;
    }

    //------------------------Camera Functions----------------------------

    private Marker addMarkerAndZoomTo(Location inputLocation) {
        // Add marker
        Double lat = inputLocation.getLatitude();
        Double lon = inputLocation.getLongitude();
        LatLng myLatLng = new LatLng(lat, lon);
        Marker markerOut = mMap.addMarker(new MarkerOptions().position(myLatLng).title(lat + ", " + lon)); // Label location with coordinates
        //Create Camera position and go to it
        CameraPosition currentLocationCameraPosition = new CameraPosition.Builder().target(myLatLng).zoom(17).build(); // Zoom 17 is pretty close, see maps api documentation
        //mMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentLocationCameraPosition), 3000, null);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentLocationCameraPosition));
        // Return marker
        return markerOut;
    }

    private Marker addMarkerFromLatLng(LatLng inputLatLng) {
        return mMap.addMarker(new MarkerOptions().position(inputLatLng).title(inputLatLng.latitude + ", " + inputLatLng.longitude)); // Label location with coordinates
    }

    //------------------------User Functions------------------------------

    public void functionOne(View view) {
        resetPointSelection();
    }

    public void functionTwo(View view) {
        if (currentLatLng != null && desiredLatLng != null)
            getNewDirectionsResult(currentLatLng, desiredLatLng);
        else
            Toast.makeText(getApplicationContext(), "Current or destination LatLng are null", Toast.LENGTH_LONG).show();
    }

    public void functionThree(View view) {}

    public void functionFour(View view) {}

}
