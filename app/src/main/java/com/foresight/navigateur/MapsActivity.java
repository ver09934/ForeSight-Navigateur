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
import android.text.method.ScrollingMovementMethod;
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

        setupArrays();

        mMapInstructionsView = findViewById(R.id.map_instructions_text);
        mMapInstructionsView.setMovementMethod(new ScrollingMovementMethod());

        bluetoothTextView = findViewById(R.id.bluetooth_status_info);
        bluetoothTextView.setMovementMethod(new ScrollingMovementMethod());

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

    private final int FASTEST_UPDATE_SPEED = 1000; //5000
    private final int UPDATE_SPEED = 2000; //10000

    private double[] magneticCompassHeadingArray = new double[20];

    // Locations obtained using play services location
    private Location[] currentLocationArray = new Location[20];
    private Location currentLocation = null;
    private Location previousLocation = null;
    private LatLng currentLatLng = null;
    private LatLng previousLatLng = null;

    // Bearing between previous and current location
    private double currentBearingFromTwoLocations = -1;

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

    // Desired Location
    private Marker selectedPointMarker = null;
    LatLng selectedLatLng = null;

    // For original zoom to location
    private boolean isFirstTime = true;

    // Navigation and route
    private boolean haveRoute = false;
    private List<LatLng> routePoints = null;

    private LatLng currentNavPoint = null;
    int currentNavPointIndex;
    private boolean navigationIsActive = false;

    //public double currentAverageHeading = 0;

    //----------------------Setup Location and Heading Arrays-------------------------------

    public void setupArrays() {
        for (int i = 0; i < magneticCompassHeadingArray.length; i++) {
            magneticCompassHeadingArray[i] = -1;
        }
        for (int i = 0; i < currentLocationArray.length; i++) {
            currentLocationArray[i] = null;
        }
    }

    public void updateCurrentLocationArray(Location location) {

        for (int i = 0; i < currentLocationArray.length - 1; i++) {
            currentLocationArray[i] = currentLocationArray[i + 1];
        }
        currentLocationArray[currentLocationArray.length - 1] = location;

        currentLocation = currentLocationArray[currentLocationArray.length - 1];
        previousLocation = currentLocationArray[currentLocationArray.length - 2];

        if (currentLocation != null)
            currentLatLng = getLatLngFromLocation(currentLocation);
        if (previousLocation != null)
        previousLatLng = getLatLngFromLocation(previousLocation);
    }

    //----------------------------Main Maps Functions----------------------
    public void masterMapMethod() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    // Manipulates the map once available
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

                    updateCurrentLocationArray(location);

                    // zoomTo(location, 15); // Zoom to current location on map
                    updateCurrentBearingFromTwoLocations(); // Update current l

                    sendData("z"); // Request bearing


                    // TODO: Navigation
                    if (navigationIsActive) {

                        if (getDistanceToCurrentNavPoint() <= 5)
                            advanceCurrentNavPoint();

                        mMapInstructionsView.append("\nDistance to next point: " + getDistanceToCurrentNavPoint());
                        mMapInstructionsView.append("\nBearing to next point: " + getBearingToSend());
                        mMapInstructionsView.append("\nCurrent NavPoint index: " + Integer.toString(currentNavPointIndex) + "/" + Integer.toString(routePoints.size()));

                        // after assigning new raw headings
                        //getCurrentAverageHeading();

                        String toSend = getLetterFromAngle(getBearingToSend());
                        sendData(toSend);
                    }


                    if (!paused) {
                        Toast.makeText(getApplicationContext(),
                                "Bearing from 1 Location: " + location.getBearing() + "\nBearing from 2 Locations: " + ((currentBearingFromTwoLocations != -1) ? currentBearingFromTwoLocations : "null"),
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
        mLocationRequest.setInterval(UPDATE_SPEED);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_SPEED);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, getMainLooper()); //Also works fine if looper arg is set to null
    }

    //---------------------------App Pausing and Restarting-----------------------------------------------

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

    //-------------------Bearing Logic, Routes, Coordinates, and Random Utils---------------------------------------------------

    public double getDistanceToCurrentNavPoint() {
        if (currentNavPoint != null) {
            return currentLocation.distanceTo(getLocationFromLatLng(currentNavPoint));
        }
        return 0;
    }

    public void advanceCurrentNavPoint() {
        if (currentNavPointIndex < routePoints.size() - 1) {
            currentNavPointIndex++;
            currentNavPoint = routePoints.get(currentNavPointIndex);
        }
        else {
            Toast.makeText(getApplicationContext(), "Navigation completed!", Toast.LENGTH_LONG).show();
            navigationIsActive = false;
        }
    }

    public void setFirstNavPoint() {
        if (routePoints != null) {
            currentNavPointIndex = 0;
            currentNavPoint = routePoints.get(currentNavPointIndex);
        }
    }

    public double getBearingToCurrentNavPoint() {
        if (currentNavPoint != null) {
            return currentLocation.bearingTo(getLocationFromLatLng(currentNavPoint));
        }
        return 0;
    }

    // TODO: Issue: Angle is still sometimes negative... suspect angle ends up less than -360?
    public double getBearingToSend() {
        double intendedBearing = getBearingToCurrentNavPoint();
        double realBearing = getCurrentAverageHeading();
        double diffBearing = intendedBearing - realBearing;
        diffBearing = (diffBearing >= 0) ? diffBearing : diffBearing + 360;
        return diffBearing;
    }

    public double getCurrentAverageHeading() {
        // TODO: Insure these are all 0 <= theta < 360
        if (currentLocation != null && currentBearingFromTwoLocations != -1 && avgThreeMagHeadings() != 0) {
            double heading1 = currentBearingFromTwoLocations;
            double heading2 = currentLocation.getBearing();
            double heading3 = avgThreeMagHeadings();
            //currentAverageHeading = (heading1 + heading2 + heading3) / 3;
            return (heading1 + heading2 + heading3) / 3;
        }
        else if (currentBearingFromTwoLocations != -1)
            //currentAverageHeading = currentBearingFromTwoLocations;
            return currentBearingFromTwoLocations;
        else if (currentLocation != null)
            //currentAverageHeading = currentLocation.getBearing();
            return currentLocation.getBearing();
        else
            //currentAverageHeading = 0;
            return 0;
    }

    public void toggleNavigationIsActive() {
        // navigationIsActive = !navigationIsActive;
        if (navigationIsActive) {
            navigationIsActive = false;
            Toast.makeText(getApplicationContext(), "Navigation terminated", Toast.LENGTH_LONG).show();
        }
        else {
            navigationIsActive = true;
            Toast.makeText(getApplicationContext(), "Navigation started", Toast.LENGTH_LONG).show();
        }
    }

    /*
    Takes in angle 0-360 (or 0-359.99?)
    Could convert ascii to int and map from 0-360 using floors and stuff, but this is just easier...
    */
    public String getLetterFromAngle(Double angle) {
        if (0 <= angle && angle < 45)
            return "a";
        if (45 <= angle && angle < 90)
            return "b";
        if (135 <= angle && angle < 180)
            return "c";
        if (180 <= angle && angle < 225)
            return "d";
        if (225 <= angle && angle < 270)
            return "e";
        if (270 <= angle && angle < 315)
            return "f";
        if (315 <= angle && angle < 360)
            return "g";
        return "a"; // In case something weird happens
    }


    // Not used at this point, I don't think...
    // returns the middle angle formed between all three angles, in that order
    // Gives Minor Angle!
    public double getFormedAngle(Location inputOne, Location inputTwo, Location inputThree) {
        double thetaOne = inputTwo.bearingTo(inputOne);
        double thetaTwo = inputTwo.bearingTo(inputThree);
        double angle = Math.abs(thetaOne - thetaTwo);
        return (angle > 180) ? angle - 180: angle;
    }

    private void updateCurrentBearingFromTwoLocations() {
        if (currentLocation != null && previousLocation != null) {
            double bearing = (double) previousLocation.bearingTo(currentLocation);
            currentBearingFromTwoLocations = (bearing >= 0) ? bearing : bearing + 360;
        }
    }

    private LatLng getLatLngFromLocation(Location inputLocation) {
        return new LatLng(inputLocation.getLatitude(), inputLocation.getLongitude());
    }

    private Location getLocationFromLatLng(LatLng inputLatLng) {
        Location outputLocation = new Location("");
        outputLocation.setLatitude(inputLatLng.latitude);
        outputLocation.setLongitude(inputLatLng.longitude);
        return outputLocation;
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
    private void getNewDirectionsResult() {

        if (currentLatLng != null && selectedLatLng != null) {

            // Weird solution for a weird problem
            com.google.maps.model.LatLng convertedOrigin = new com.google.maps.model.LatLng(currentLatLng.latitude, currentLatLng.longitude);
            com.google.maps.model.LatLng convertedDestination = new com.google.maps.model.LatLng(selectedLatLng.latitude, selectedLatLng.longitude);

            DateTime now = new DateTime();
            try {
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

                // toggleNavigationIsActive(); // Start navigation once we have a directions result

                // See all points
                /*
                for (LatLng test: routePoints)
                    addMarkerFromLatLng(test);
                */

                haveRoute = true;

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

        routePoints = decodedPath; //Make set of route points accessible by other methods
    }

    public void clearRoute() {
        if (haveRoute) {
            mMap.clear();
            haveRoute = false;
            addSelectedPointMarker();
        }
    }

   //------------------------Map Long-Clicking to Select Destination---------------------------

    @Override
    public void onMapLongClick(LatLng point) {
        if (selectedPointMarker != null && !haveRoute)
            selectedPointMarker.remove();
        if (!haveRoute) {
            mMapInstructionsView.setText(getString((R.string.map_instructions_point_pressed), point.latitude, point.longitude));
            selectedPointMarker = addMarkerFromLatLng(point);
            selectedLatLng = point;
        }
    }

    // Reset end location and stop navigation
    private void resetPointSelection() {
        if (!haveRoute) {
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
    private Marker addMarkerFromLocation(Location inputLocation) {
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
        getNewDirectionsResult();
    }

    // Cancel navigation
    public void mapsFunctionThree(View view) {
        clearRoute();
    }

    public void mapsFunctionFour(View view) {
        masterBluetoothMethod();
    }

    public void mapsFunctionFive(View view) {
        sendData("b");
    }

    public void mapsFunctionSix(View view) {
        bluetoothTextView.append("\nDOOT!");
    }

    public void mapsFunctionSeven(View view) {
        toggleNavigationIsActive();
    }

    //==============================================================================================================
    //=============================================== BLUETOOTH ====================================================
    //==============================================================================================================

    /*
    TextView: use .setText(String) and .append(String)
     */

    public void updateMagneticCompassHeadingArray(String inputString) {
        try {
            double heading = getRealBearing(inputString);
            for (int i = 0; i < magneticCompassHeadingArray.length - 1; i++) {
                magneticCompassHeadingArray[i] = magneticCompassHeadingArray[i + 1];
            }
            magneticCompassHeadingArray[magneticCompassHeadingArray.length - 1] = heading;
        }
        catch (java.lang.NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // TODO: Make sure this maps negative angles to 0 <= theta < 360
    public double getRealBearing(String inputString) {
        double angle = Double.parseDouble(inputString);
        return (angle >= 0) ? angle : angle + 360;
    }

    private double avgThreeMagHeadings() {
        int sum = 0;
        int denom = 0;
        if (magneticCompassHeadingArray[magneticCompassHeadingArray.length - 1] != -1) {
            sum += magneticCompassHeadingArray[magneticCompassHeadingArray.length - 1];
            denom++;
        }
        if (magneticCompassHeadingArray[magneticCompassHeadingArray.length - 2] != -1) {
            sum += magneticCompassHeadingArray[magneticCompassHeadingArray.length - 2];
            denom++;
        }
        if (magneticCompassHeadingArray[magneticCompassHeadingArray.length - 3] != -1) {
            sum += magneticCompassHeadingArray[magneticCompassHeadingArray.length - 3];
            denom++;
        }

        if (denom != 0)
            return sum/denom;
        else
            return 0;
    }

    //----------------

    //MASTER BLUETOOTH METHOD
    public void masterBluetoothMethod() {

        if(BTinit()) {
            if(BTconnect()) {
                deviceConnected = true;

                beginListenForData(); //Commented out for testing purposes

                mMapInstructionsView.append(getString(R.string.bluetooth_listening));
            }
        }

    }

    public void sendData(String inputString) {
        if (deviceConnected) {
            inputString.concat("\n");
            try {
                outputStream.write(inputString.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            bluetoothTextView.append(getString(R.string.bluetooth_sent_text, inputString));
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

        return connected;
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        int byteCount = inputStream.available();
                        if (byteCount > 1)
                        // TODO: Greater than 1? Get Rohan to pad output with leading zeroes
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string = new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    bluetoothTextView.append("\n" + string);
                                    updateMagneticCompassHeadingArray(string);
                                }
                            });

                        }
                    }
                    catch (IOException ex) {
                        stopThread = true;
                    }
                }
            }
        });
        thread.start();
    }
}
