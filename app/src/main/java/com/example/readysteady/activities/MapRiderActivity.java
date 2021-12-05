package com.example.readysteady.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import android.widget.Button;
import android.widget.Toast;

import com.example.readysteady.R;
import com.example.readysteady.databinding.ActivityMapRiderBinding;

import com.example.readysteady.models.LoginModel;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
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
    String riderID;
    String driverId = "driver";
    String destination;
    LatLng destinationLatLng;


    private Boolean requestBol = false;
    private Marker pickupMarker;
    private static int AUTOCOMPLETE_REQUEST_CODE = 1;
    String apiKey;

    // Set the fields to specify which types of place data to
    // return after the user has made a selection.
    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapRiderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        logout = findViewById(R.id.logout);
        requestForRide = findViewById(R.id.request);
        apiKey = getString(R.string.api_key);
        loginModel = (LoginModel) getIntent().getSerializableExtra("loginUser");
        riderID = loginModel.getUsername().split("@")[0];
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setHint("Search Destination");

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
            if (destination == null || destination.isEmpty()){
                Toast.makeText(getApplicationContext(), "Please select destination.", Toast.LENGTH_SHORT).show();
                return;
            }
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
                geoFire.removeLocation(riderID);

                if(pickupMarker != null){
                    pickupMarker.remove();
                }
                requestForRide.setText("Call Transport");


            }else{
                requestBol = true;
                geoFire.setLocation(riderID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                pickup = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                pickupMarker = mMap.addMarker(new MarkerOptions().position(pickup).title("Pick-up Location").icon(bitmapDescriptorFromVector(getApplicationContext(),R.mipmap.pickup)));
                requestForRide.setText("Finding Driver..");
                getClosestDriver();

            }
        });



        // Initialize the AutocompleteSupportFragment.

        Places.initialize(getApplicationContext(), apiKey);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);
        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // TODO: Get info about the selected place.
                destination = place.getName();
                destinationLatLng = place.getLatLng();
            }


            @Override
            public void onError(@NonNull Status status) {
                // TODO: Handle the error.
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
                    driverFoundID = driverId;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverFoundID).child("customerRequest");
                    HashMap map = new HashMap();
                    map.put("customerRideid", riderID);
                    map.put("destination", destination);
                    map.put("destinationLatLn", destinationLatLng);

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
        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance().getReference("AvailableDrivers").child(driverFoundID).child("l");
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
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
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
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Current Driver").icon(bitmapDescriptorFromVector(getApplicationContext(),R.mipmap.taxi)));

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

    public static BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

}