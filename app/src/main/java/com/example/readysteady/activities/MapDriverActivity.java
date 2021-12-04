package com.example.readysteady.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.example.readysteady.databinding.ActivityMapDriverBinding;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.readysteady.models.LoginModel;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapDriverActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private ActivityMapDriverBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest locationRequest;
    LoginModel loginModel;
    GeoFire geoFire;
    Button logout;

    private String customerID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapDriverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        logout = findViewById(R.id.logout);
        loginModel = (LoginModel) getIntent().getSerializableExtra("loginUser");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("AvailableDrivers");
        geoFire = new GeoFire(databaseReference);
        mapFragment.getMapAsync(this);
        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class );
            startActivity(mainActivity);
            finish();
        });

        getAssignedCustomer();

    }

    private void getAssignedCustomer(){
        String driverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference customerAssignedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("customerRideId");
        customerAssignedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                        customerID = snapshot.getValue().toString();
                        getCustomerAssignedPickupLocation();
                }else{
                    customerID = "";
                    if(pickupMarker!= null){
                        pickupMarker.remove();
                    }
                    if(customerAssignedPickupLocationRefListener != null){
                        customerAssignedPickupLocationRef.removeEventListener(customerAssignedPickupLocationRefListener);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    Marker pickupMarker;
    private DatabaseReference customerAssignedPickupLocationRef;
    private ValueEventListener customerAssignedPickupLocationRefListener;
    private void getCustomerAssignedPickupLocation(){
        customerAssignedPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("riderRequest").child(customerID).child("l");
        customerAssignedPickupLocationRefListener = customerAssignedPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && !customerID.equals("")){
                   List<Object> map = (List<Object>) snapshot.getValue();
                   double locationLat = 0;
                   double locationLng = 0;
                   if(map.get(0) != null){
                       locationLat = Double.parseDouble(map.get(0).toString());
                   }
                    if(map.get(0) != null){
                        locationLng = Double.parseDouble(map.get(0).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Current Pickup Location"));
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



        if(getApplication()!=null) {

            lastLocation = location;
            System.out.println("got location" + location);
            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            // save values
            String username = loginModel.getUsername();
            DatabaseReference databaseRefAvailable = FirebaseDatabase.getInstance().getReference("AvailableDrivers");
            DatabaseReference databaseRefWorking = FirebaseDatabase.getInstance().getReference("WorkingDrivers");
            GeoFire geoFireAvailable = new GeoFire(databaseRefAvailable);
            GeoFire geoFireWorking = new GeoFire(databaseRefWorking);

            switch (customerID){
                case "":
                    geoFireWorking.removeLocation(username);
                    geoFireAvailable.setLocation(username.split("@")[0], new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

                default:
                    geoFireAvailable.removeLocation(username);
                    geoFireWorking.setLocation(username.split("@")[0], new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
            }
        }







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

    @Override
    protected void onStop() {
        super.onStop();
        String username = loginModel.getUsername();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("AvailableDrivers");

        GeoFire geoFire = new GeoFire(databaseReference);
        geoFire.removeLocation(username.split("@")[0]);
    }
}