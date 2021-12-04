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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

import java.util.List;
import java.util.Objects;

public class MapDriverActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private ActivityMapDriverBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LatLng pickUpLocation;
    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest locationRequest;
    LoginModel loginModel;
    GeoFire geoFire;
    Button logout;
    String driverID;

    LinearLayout mcustomerInfo;
    TextView name, phone, userLocation;
    ImageView profileImage;

    private String customerID ="" ;

    Marker pickupMarker;
    private DatabaseReference customerAssignedPickupLocationRef;
    private ValueEventListener customerAssignedPickupLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapDriverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        logout = findViewById(R.id.logout);
        mcustomerInfo = findViewById(R.id.customerInfo);
        name = findViewById(R.id.userName);
        phone = findViewById(R.id.userPhone);
        userLocation = findViewById(R.id.userLocation);
        profileImage = findViewById(R.id.profileImage);

        loginModel = (LoginModel) getIntent().getSerializableExtra("loginUser");
        driverID = loginModel.getUsername().split("@")[0];
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

    private void getCustomerDetails(DataSnapshot dataSnapshot){
        mcustomerInfo.setVisibility(View.VISIBLE);
        name.setText("Test User");
        phone.setText("07742700000");
        userLocation.setText(Objects.requireNonNull(dataSnapshot.getValue()).toString());
    }

    private void getAssignedCustomer(){
        DatabaseReference customerAssignedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("customerRequest");
        customerAssignedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                        customerID = "rider";
                        getCustomerAssignedPickupLocation();
                        getCustomerDetails(snapshot);
                }else{
                    customerID = "";
                    if(pickupMarker!= null){
                        pickupMarker.remove();
                    }
                    if(customerAssignedPickupLocationRefListener != null){
                        customerAssignedPickupLocationRef.removeEventListener(customerAssignedPickupLocationRefListener);
                    }
                    mcustomerInfo.setVisibility(View.GONE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
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
                    if(map.get(1) != null){
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickUpLocation = new LatLng(locationLat, locationLng);
                    mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Current Pickup Location").icon(MapRiderActivity.bitmapDescriptorFromVector(getApplicationContext(), R.mipmap.pickup)));
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
    public void onLocationChanged(@NonNull Location location){

        if(getApplication()!=null) {

            lastLocation = location;
            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
            // save values
            DatabaseReference databaseRefAvailable = FirebaseDatabase.getInstance().getReference("AvailableDrivers");
            DatabaseReference databaseRefWorking = FirebaseDatabase.getInstance().getReference("WorkingDrivers");
            GeoFire geoFireAvailable = new GeoFire(databaseRefAvailable);
            GeoFire geoFireWorking = new GeoFire(databaseRefWorking);

            switch (customerID){
                case "rider":
                    if (pickUpLocation!= null){
                        geoFireAvailable.removeLocation(driverID);
                        geoFireWorking.setLocation(driverID, new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude));
                        mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Current Pickup Location").icon(MapRiderActivity.bitmapDescriptorFromVector(getApplicationContext(),R.mipmap.pickup)));
                    }
                    break;
                default:
                    geoFireWorking.removeLocation(driverID);
                    geoFireAvailable.setLocation(driverID, new GeoLocation(location.getLatitude(), location.getLongitude()));
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