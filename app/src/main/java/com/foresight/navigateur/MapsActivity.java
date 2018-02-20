package com.foresight.navigateur;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View; //Might take out

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

//--------------------------------------------------------

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;

    private static final LatLng rohan = new LatLng(42.780970, -73.841671);

    public static final CameraPosition rohan_in =
            new CameraPosition.Builder().target(rohan)
                    .zoom(16)
                    .build();

    public static final CameraPosition rohan_out =
            new CameraPosition.Builder().target(rohan)
                    .zoom(10)
                    .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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

        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        */

        mMap.addMarker(new MarkerOptions().position(rohan).title("Marker at Rohan's House"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(rohan));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(rohan_out));
    }

    public void zoomToRohan(View view) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(rohan_in), 3000, null);
    }

    public void zoomFromRohan(View view) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(rohan_out), 3000, null);
    }

}
