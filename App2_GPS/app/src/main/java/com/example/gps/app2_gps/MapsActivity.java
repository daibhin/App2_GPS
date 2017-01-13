package com.example.gps.app2_gps;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.drive.Permission;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.*;

import java.util.Map;


public class MapsActivity extends FragmentActivity  implements android.location.LocationListener, OnMapReadyCallback{

    private LocationManager lm;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private DatabaseReference mDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // connects to Firbase database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            String networkProvider = LocationManager.NETWORK_PROVIDER;
            String gpsProvider = LocationManager.GPS_PROVIDER;
            long interval = 1000;
            float minDistance = 1;
            // setup network (router) and GPS (device) location change listeners
            lm.requestLocationUpdates(networkProvider, interval, minDistance, this);
            lm.requestLocationUpdates(gpsProvider, interval, minDistance, this);
        }
        catch (SecurityException e) {
            Log.e("GPS", "exception occured " + e.getMessage());
        }
        catch (Exception e) {
            Log.e("GPS", "exception occured " + e.getMessage());
        }

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        }
    }


    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            setUpMap();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        final DatabaseReference ref = mDatabase.child("Locations").getRef();

        // Attach a listener to read the data at our posts reference
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // iterate through individual location objects
                for (DataSnapshot locSnapshot: dataSnapshot.getChildren()) {
                    // Parse LocationData object from object returned by Firebase
                    LocationData loc = locSnapshot.getValue(LocationData.class);
                    if (loc != null) {
                        double lat = loc.latitude;
                        double lon = loc.longitude;
                        // add marker to map
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(lat, lon))
                                .title("Lat: " + lat + " / Long:" + lon + " from Firebase"));
                    }
                }
                // Remove listener to only download locations once
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

        // Center map on Engingeering Building
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(53.283912, -9.063874));
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

    }


    public void onLocationChanged(Location location) {
        // Add current location marker to map
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lat, lon))
                        .title("Lat: " + lat + " / Long:" + lon));

        LocationData locationData = new LocationData(lat, lon);
        // Upload location to Firebase
        mDatabase.child("Locations").push().setValue(locationData);
    }

    public void onProviderDisabled(String arg0) {
        Log.e("GPS", "provider disabled " + arg0);
    }

    public void onProviderEnabled(String arg0) {
        Log.e("GPS", "provider enabled " + arg0);
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        Log.e("GPS", "status changed to " + arg0 + " [" + arg1 + "]");
    }
}
