package com.example.readysteady.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import com.example.readysteady.R;
import com.example.readysteady.databinding.ActivityMapDriverBinding;
import com.example.readysteady.databinding.ActivityMapRiderBinding;
import com.example.readysteady.models.LoginModel;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class MapRiderActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private ActivityMapRiderBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest locationRequest;
    LoginModel loginModel;
    GeoFire geoFire;
    Button logout;
    Button requestForRide;
    LatLng pickup;

    private Boolean requestBol = false;
    private Marker pickupMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapRiderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        logout = findViewById(R.id.logout);
        requestForRide = findViewById(R.id.request);

        loginModel = (LoginModel) getIntent().getSerializableExtra("loginUser");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("riderRequest");
        geoFire = new GeoFire(databaseReference);
        mapFragment.getMapAsync(this);
        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class );
            startActivity(mainActivity);
            finish();
        });

        requestForRide.setOnClickListener(v -> {
            if (requestBol){
                requestBol = false;
                geoQuery.removeAllListeners();
                driverLocationRef.removeEventListener(driverLocationRefListener);

                if (driverFoundID != null){
                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                driverRef.setValue(true);
                driverFoundID = null;
                }
                driverFound = false;
                radius = 1;
                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                geoFire.removeLocation(loginModel.getUsername().split("@")[0]);

                if(pickupMarker != null){
                    pickupMarker.remove();
                }
                requestForRide.setText("Call Transport");


            }else{

                requestBol = true;

                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

                geoFire.setLocation(loginModel.getUsername().split("@")[0], new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                pickup = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                pickupMarker = mMap.addMarker(new MarkerOptions().position(pickup).title("Pick-up Location"));
                requestForRide.setText("Finding Driver..");

                getClosestDriver();

            }
        });
    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    GeoQuery geoQuery;
    private void getClosestDriver(){
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("AvailableDrivers");

        GeoFire geoFire = new GeoFire(driverLocation);

        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickup.latitude, pickup.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    String customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("customerRideid", customerID);
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    requestForRide.setText("Currently Looking for Driver...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private void getDriverLocation(){
        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance().getReference().child("workingDrivers").child(driverFoundID).child("l");
        driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && requestBol){
                    List<Object> map = (List<Object>) snapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    requestForRide.setText("We have found a Driver!");
                    if(map.get(0) != null){
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if(map.get(0) != null){
                        locationLng = Double.parseDouble(map.get(0).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if(mDriverMarker != null){
                        mDriverMarker.remove();
                    }
                    Location location1 = new Location("");
                    location1.setLatitude(pickup.latitude);
                    location1.setLongitude(pickup.longitude);

                    Location location2 = new Location("");
                    location2.setLatitude(driverLatLng.latitude);
                    location2.setLongitude(driverLatLng.longitude);

                    float distance = location1.distanceTo(location2);

                    if(distance<100){
                        requestForRide.setText("Your driver has arrived!");
                    }else {

                        requestForRide.setText("Driver Located: " + String.valueOf(distance));
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Current Driver"));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildApiClient();
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;
        System.out.println("got location" + location);
        LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
        // save values
        String username = loginModel.getUsername();
        geoFire.setLocation(username.split("@")[0], new GeoLocation(location.getLatitude(), location.getLongitude()));
    }
    protected synchronized void buildApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}