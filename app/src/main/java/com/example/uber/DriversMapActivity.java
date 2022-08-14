package com.example.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ktx.Firebase;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int ACCESS_LOCATION_REQUEST_CODE = 1001;

    //starting location updates
    LocationRequest locationRequest;

    Marker pickUpMarker;//userLocationMarker;

    private FirebaseAuth mAuth;

    private DatabaseReference assignedCustomerRef, assignedCustomerPickUpRef;

    private String driverID, customerID = "";

    private ValueEventListener assignedCustomerPickUpRefListner;

    private CircleImageView account;

    private TextView riderName, riderPhoneNo;
    private CircleImageView riderImage;
    private RelativeLayout relativeLayout;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        riderName = findViewById(R.id.riderName);
        riderPhoneNo = findViewById(R.id.riderPhone);
        riderImage = findViewById(R.id.profileImageRider);
        relativeLayout = findViewById(R.id.rel2);

        account = findViewById(R.id.account);

        account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DriversMapActivity.this, ProfileActivity.class);

                //to know from which activity is the user comming from, either customer or driver
                intent.putExtra("type", "Drivers");

                startActivity(intent);
            }
        });


        //stupid mistake, if not initialized the firebaseAuth
        mAuth = FirebaseAuth.getInstance();

        driverID = mAuth.getCurrentUser().getUid();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        getAssignedCustomerRequest();
    }


    //getting customer request id to assign to a driver
    private void getAssignedCustomerRequest() {

        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverID).child("CustomerRideId");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //this will check if the user has requested a cab or not
                //if user request is true
                if(snapshot.exists()) {

                    //this statement retrives the customer request id
                    customerID = snapshot.getValue().toString();

                    //once we get the customer request id we will get the location of that customer
                    getAssignedCustomerPickUpLocation();

                    relativeLayout.setVisibility(View.VISIBLE);
                    getAssignedCustomerInfo();

                }else {
                    //if user request is false

                    customerID = "";
                    if(pickUpMarker != null) {
                        pickUpMarker.remove();
                    }

                    if(assignedCustomerPickUpRefListner != null) {

                        assignedCustomerPickUpRef.removeEventListener(assignedCustomerPickUpRefListner);
                    }

                    relativeLayout.setVisibility(View.GONE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerPickUpLocation() {

        assignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Customer Request").child(customerID).child("l");

        assignedCustomerPickUpRefListner = assignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    List<Object> customerLocationMap = (List<Object>) snapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    //when customer is found we get the lat & lang from the db and convert it into double datatype and store in our var
                    if(customerLocationMap.get(0) != null) {
                        LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if(customerLocationMap.get(1) != null) {
                        LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    //once we get the customer position we will add a marker on the driver map showing the customer location
                    LatLng customerLatLang = new LatLng(LocationLat, LocationLng);
                    mMap.addMarker(new MarkerOptions().position(customerLatLang).title("Rider PickUp"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
    public void onMapReady(@NotNull GoogleMap googleMap) {

        mMap = googleMap;

        checkForpermissions();


    }

    //1.here we check for user permissions
    private void checkForpermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //if permission is granted we will enable user location
            getCurrentLocation();
            //zoomToUserLocation();
        } else {

            //we will show user why the permission is necessary
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                //then we ask for the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
            } else {

                //we directly ask for permission without showing the dialog box
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
            }
        }
    }

    //2.here we will enable user location
    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mMap.setMyLocationEnabled(true);

    }

    //here we directly zoom to the user location whenever the map activity is opened
    private void zoomToUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));

            }
        });
    }

    //creating location callback
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if(mMap != null){
                setUserLocationMarker(locationResult.getLastLocation());
            }
        }
    };


    //this methods will be called in the onStart method
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());


    }

    //this method will be called in the onStop method
    private void stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    //this will set the markere on the updated location of the user
    private void setUserLocationMarker(Location location){

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if(pickUpMarker == null){
            //create a new marker
            MarkerOptions markerOptions = new MarkerOptions();
            /*markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.redcar)); //set the custom marker
            markerOptions.rotation(location.getBearing());
            markerOptions.anchor((float) 0.5, (float) 0.5);
            //userLocationMarker = mMap.addMarker(markerOptions);*/
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }
        else {
            //use the previously created marker
            //userLocationMarker.setPosition(latLng);
            //userLocationMarker.setRotation(location.getBearing());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }

        //*******************************************************
        //here we get the driver id who is online
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //now creating reference to our database/ driver available node
        DatabaseReference driversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");

        //here we are geting the updated location of the driver and saving it to the database
        GeoFire geoFireAvailability = new GeoFire(driversAvailableRef);

        //now creating reference to our database/ driver working node
        DatabaseReference driversWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        GeoFire geoFireWorking = new GeoFire(driversWorkingRef);


        switch (customerID) {

            //first case says, if there is no customer id it means the driver is free
            case "":
                geoFireWorking.removeLocation(userId);
                geoFireAvailability.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                break;

            default:
                geoFireAvailability.removeLocation(userId);
                geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                break;
        }

    }

    //when our maps activity will be started the location updates will be started
    @Override
    protected void onStart() {
        super.onStart();

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates();

        }
        else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);

        }

    }

    //when our maps activity will be stoped the location updates will be stoped
    @Override
    protected void onStop() {
        super.onStop();

        stopLocationUpdates();

        //removing driver from available drivers in the database when they close the app
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFire = new GeoFire(databaseReference);
        geoFire.removeLocation(userId);



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
                //zoomToUserLocation();
            }
        }
    }

    private void getAssignedCustomerInfo() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerID);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String uName = snapshot.child("name").getValue().toString();
                    String uPhone = snapshot.child("phone").getValue().toString();
                    String profilePic = snapshot.child("image").getValue().toString();

                    riderName.setText(uName);
                    riderPhoneNo.setText(uPhone);
                    Picasso.get().load(profilePic).into(riderImage);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}