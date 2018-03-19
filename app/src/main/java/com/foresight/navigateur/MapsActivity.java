package com.foresight.navigateur;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
Welcome to the land of spaghetti code...
 */

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMapLongClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mMapInstructionsView = findViewById(R.id.map_instructions_text);
        bluetoothTextView = findViewById(R.id.bluetooth_status_info);

        masterMapMethod();

        //masterBluetoothMethod();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopThread = true;

        try {
            outputStream.close();
            inputStream.close();
            socket.close();
        }
        catch (java.io.IOException e) {
            e.printStackTrace();
        }
        catch (java.lang.NullPointerException e) {
            e.printStackTrace();
        }
        catch (java.lang.RuntimeException e) {
            e.printStackTrace();
        }

        deviceConnected = false;
    }

    //=====================================================================================================
    //==================================== MAPS ===========================================================
    //=====================================================================================================

    // General
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;

    // Assorted
    boolean paused = false;
    private TextView mMapInstructionsView;
    private TextView bluetoothTextView;

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

    // Navigation and route
    private boolean navigationIsActive = false;
    // TODO: private List<LatLng> routePoints = null;

    public void masterMapMethod() {
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
     * If Google Play services is not installed on the mBluetoothDevice, the user will be prompted to install
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

                    updateCurrentBearing();

                    if (!paused) {
                        Toast.makeText(getApplicationContext(),
                                "Bearing from 1 Location: " + location.getBearing() + "\nBearing from 2 Locations: " + ((currentBearing != null) ? currentBearing : "null"),
                                Toast.LENGTH_SHORT).show();
                    }

                    zoomFirstTime();
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
            double bearing = (double) previousLocation.bearingTo(currentLocation);
            currentBearing = (bearing >= 0) ? bearing : bearing +360;
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
        // TODO: routePoints = decodedPath; //Make set of route points accessible by other methods
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
    public void mapsFunctionOne(View view) {
        resetPointSelection();
    }

    // Request directions
    public void mapsFunctionTwo(View view) {
        getNewDirectionsResult(currentLatLng, selectedLatLng);
    }

    // Cancel navigation
    public void mapsFunctionThree(View view) {
        stopNavigation();
    }

    public void mapsFunctionFour(View view) {
        masterBluetoothMethod();
    }

    public void mapsFunctionFive(View view) {
        testSend();
    }

    public void mapsFunctionSix(View view) {

    }


    //==============================================================================================================
    //=============================================== BLUETOOTH ====================================================
    //==============================================================================================================

    /*
    TextView: use .setText(String) and .append(String)
     */

    private double previousMagneticCompassHeading = 0;
    private double currentMagneticCompassHeading = 0;

    // Check input --> this could bork
    public void updateMagneticCompassHeadings(String inputString) {
        previousMagneticCompassHeading = currentMagneticCompassHeading;
        currentMagneticCompassHeading = Integer.parseInt(inputString);
    }

    //----------------

    //MASTER BLUETOOTH METHOD
    public void masterBluetoothMethod() {

        if(BTinit()) {
            if(BTconnect()) {
                deviceConnected = true;
                beginListenForData();

                mMapInstructionsView.append(getString(R.string.bluetooth_listening));
            }
        }

    }

    public void testSend() {
        sendData("b");
    }

    public void sendData(String inputString) {
        if (deviceConnected) {
            inputString.concat("\n");
            try {
                outputStream.write(inputString.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            bluetoothTextView.setText(getString(R.string.bluetooth_sent_text, inputString));
        }
    }

    //--------------

    public static final String DEVICE_ADDRESS="00:14:03:05:FF:E6";
    public static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Serial Port Service ID

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    boolean deviceConnected = false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;

    public boolean BTinit() {

        boolean found = false;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            //Toast.makeText(getApplicationContext(), "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            bluetoothTextView.setText(getString(R.string.bluetooth_not_supported));
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                //Toast.makeText(getApplicationContext(), "Bluetooth supported and already enabled", Toast.LENGTH_SHORT).show();
                bluetoothTextView.setText(getString(R.string.bluetooth_enabled));
            }

            Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
            if (bondedDevices.isEmpty()) {
                //Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
                bluetoothTextView.append(getString(R.string.bluetooth_please_pair));
            } else {
                for (BluetoothDevice device : bondedDevices) {
                    if (device.getAddress().equals(DEVICE_ADDRESS)) {
                        mBluetoothDevice = device;
                        bluetoothTextView.append(getString(R.string.bluetooth_device_located));
                        found = true;
                        break;
                    }
                }
            }
        }
        return found;
    }

    public boolean BTconnect() {

        boolean connected = true;

        try {
            socket = mBluetoothDevice.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
            bluetoothTextView.append(getString(R.string.bluetooth_socket_connected));
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
            bluetoothTextView.append(getString(R.string.bluetooth_socket_failed));
        }

        if (connected) {
            try {
                outputStream=socket.getOutputStream();
                bluetoothTextView.append(getString(R.string.bluetooth_output_created));
            } catch (IOException e) {
                e.printStackTrace();
                bluetoothTextView.append(getString(R.string.bluetooth_output_failed));
            }

            try {
                inputStream=socket.getInputStream();
                bluetoothTextView.append(getString(R.string.bluetooth_input_created));
            } catch (IOException e) {
                e.printStackTrace();
                bluetoothTextView.append(getString(R.string.bluetooth_input_failed));
            }

        }

        /*
        if (connected) {
            bluetoothTextView.append("\n" + getString(R.string.bluetooth_connection_established));
        }
        else {
            bluetoothTextView.append("\n" + getString(R.string.bluetooth_connection_failed));
        }
        */

        return connected;

    }

    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    updateMagneticCompassHeadings(string);
                                }
                            });

                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }
}
