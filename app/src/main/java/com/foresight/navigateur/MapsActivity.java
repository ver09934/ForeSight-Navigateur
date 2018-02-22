package com.foresight.navigateur;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
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
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

//--------------------------------------------------------

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;

    private FusedLocationProviderClient mFusedLocationClient;

    private LocationRequest mLocationRequest;

    private LocationCallback mLocationCallback;

    boolean paused = false;

    Toast t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
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

                    addMarkerAndZoomTo(location); // Update UI with location data

                    if (!paused) {
                        t = Toast.makeText(getApplicationContext(), "Current Coordinates:\n" + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG);
                        t.show();
                        //Toast.makeText(getApplicationContext(), "Current location:\n" + location, Toast.LENGTH_LONG).show();
                    }

                }
            }
        };

        startLocationUpdates(); //Must be called AFTER mLocationCallback is instantiated or it will throw a null pointer exception!

        mMap.setOnMapLongClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        t.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        // startLocationUpdates();
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

   //------------------------Map Long-Clicking---------------------------

    @Override
    public void onMapLongClick(LatLng point) {
        //mTapTextView.setText("long pressed, point=" + point);
        markerArrayList.add(addSimpleMarker(point));
    }

    private ArrayList<Marker> markerArrayList = new ArrayList<>();

    public void clearManualMarkers() {
        for (Marker marker: markerArrayList) {
            marker.remove();
        }
    }

    //------------------------User Functions------------------------------


    Marker testMarker = null;
    // Call in onMapReady with functionOne(getCurrentFocus());
    // UPDATE CURRENT LOCATION MARKER
    public void functionOne(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {

                        // If there is already a current location marker
                        if (testMarker != null)
                            testMarker.remove();

                        testMarker = addMarkerAndZoomTo(location);
                    }
                }
            });

        }
    }

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

    private Marker addSimpleMarker(Location inputLocation) {
        // Add marker
        Double lat = inputLocation.getLatitude();
        Double lon = inputLocation.getLongitude();
        LatLng myLatLng = new LatLng(lat, lon);
        return mMap.addMarker(new MarkerOptions().position(myLatLng).title(lat + ", " + lon)); // Label location with coordinates
    }

    // Let's overload this thing!
    private Marker addSimpleMarker(LatLng inputLatLng) {
        return mMap.addMarker(new MarkerOptions().position(inputLatLng).title(inputLatLng.latitude + ", " + inputLatLng.longitude)); // Label location with coordinates
    }

    public void functionTwo(View view) {
        clearManualMarkers();
    }

    public void functionThree(View view) {}

    public void functionFour(View view) {}

}
