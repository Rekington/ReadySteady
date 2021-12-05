package com.example.readysteady.activities;


import android.app.ProgressDialog;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.example.readysteady.R;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.example.readysteady.databinding.ActivityMapDriverBinding;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.readysteady.models.LoginModel;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapDriverActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, RoutingListener {

    private GoogleMap mMap;
    private ActivityMapDriverBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LatLng pickUpLocation;
    LatLng destinationLatlng;
    FusedLocationProviderClient mFusedLocationClient;
    LocationRequest locationRequest;
    LoginModel loginModel;
    GeoFire geoFire;
    Button logout;
    String driverID;

    LinearLayout mcustomerInfo;
    TextView name, phone, userLocation;
    ImageView profileImage;

    private int status = 0;

    private String customerID ="", destination ;
    private LatLng destinationLatLng;

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

        mRideStatus.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (status) {
                case 1:
                    status =2;
                    removePolylines();
                    if(destinationLatLng.latitude!=0.0 && destinationLatLng.longitude!=0.0) {
                        getRouteToMarker(destinationLatLng);
                    }
                    mRideStatus.setText("Drive Complete!");
                    break;
                case 2:
                    endRide();
                    break;
            }
        }

        });

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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void getCustomerDetails(DataSnapshot dataSnapshot){
        mcustomerInfo.setVisibility(View.VISIBLE);
        name.setText("Test User");
        phone.setText("07742700000");
        if(dataSnapshot.child("destination").exists() ){
            DataSnapshot dest = dataSnapshot.child("destination");
            userLocation.setText(dataSnapshot.child("destination").getValue().toString());
            destinationLatlng = new LatLng(Double.parseDouble(dataSnapshot.child("destinationLatLn").child("latitude").getValue().toString()),Double.parseDouble(dataSnapshot.child("destinationLatLn").child("longitude").getValue().toString()));
            Log.println(Log.INFO,"LOG:","Destination LatLng "+ destinationLatlng);
        }

    }

    private void getAssignedCustomer(){
        DatabaseReference customerAssignedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("customerRequest");
        customerAssignedRef.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    status = 1;
                        customerID = "rider";
                        getCustomerAssignedPickupLocation();
                        getCustomerDetails(snapshot);
                }else{
                endRide();


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
                    getRouteToMarker(pickUpLocation);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getCustomerAssignedDestination(){
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("customerRequests");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        mCustomerAssignedDestination.setText("Destination: " + destination);
                    } else {
                        mCustomerAssignedDestination.setText("Destination: No Selection");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if (map.get("destinationLat") != null) {
                        destinationLat = Double.valueOf(map.get("destinationLat").toString());
                    }
                    if (map.get("destinationLng") != null) {
                        destinationLng = Double.valueOf(map.get("destinationLng").toString());
                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getRouteToMarker(LatLng pickUpLocation) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), pickUpLocation)
                .build();
        routing.execute();
    }

    private void endRide(){
        mRideStatus.setText("Picked up Customer");
        removePolylines();

        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child("customerRequests");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerID);
        customerID="";

        if (pickupMarker != null){
            pickupMarker.remove();
        }
        if(customerAssignedPickupLocationRefListener != null){
            customerAssignedPickupLocationRef.removeEventListener(customerAssignedPickupLocationRefListener);
        }
        mcustomerInfo.setVisibility(View.GONE);
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
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if(polylines.size()>0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i <route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {

    }

    private void removePolylines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }
}