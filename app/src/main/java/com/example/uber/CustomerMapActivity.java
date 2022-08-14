package com.example.uber;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    private static final int ACCESS_LOCATION_REQUEST_CODE = 1001;

    private Button bookCab;
    private String customerId;

    Location location;

    //creating latlang variable to save our customer location in the firebase
    LatLng customerPickUpLocation;

    private int radius;
    private Boolean driverFound = false, requestType = false;
    private String driverFoundId;

    //starting location updates
    LocationRequest locationRequest;

    //declaring the user location marker
    Marker pickUpMarker;//userLocationMarker

    //declaring the driver location marker on customer map
    Marker driverMarker;

    //creating database reference to pass in geofire
    private DatabaseReference customerDatabaseRef;

    private DatabaseReference driverAvailableRef;

    private DatabaseReference driverRef;

    //to show the customer driver location
    private DatabaseReference driverLocationRef;

    GeoQuery geoQuery;

    private ValueEventListener driverLocationRefListner;

    private CircleImageView cAccount;

    private TextView driverName, driverPhoneNo, driver_CarName;
    private CircleImageView driverImage;
    private RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        driverName = findViewById(R.id.driverName);
        driverPhoneNo = findViewById(R.id.driverPhone);
        driver_CarName = findViewById(R.id.driverCarName);
        driverImage = findViewById(R.id.profileImageDriver);
        relativeLayout = findViewById(R.id.rel1);


        cAccount = findViewById(R.id.cAccount);

        cAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(CustomerMapActivity.this, ProfileActivity.class);
                //to know from which activity is the user comming from, either customer or driver
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        bookCab = findViewById(R.id.btnBookCab);

        //here we get the users who are online and save it in our variable
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Request");

        driverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");

        //whenever driver confirms a customer request his id will be put in to the driver working node
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        fusedLocationProviderClient = new FusedLocationProviderClient(this);

        //initializing location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


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

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        checkForPermissions();
    }

    //1.here we check for user permissions
    private void checkForPermissions() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //if permission is granted get user location
            getCurrentLocation();

        } else {

            //if permission is not granted ask for permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
        }
    }

    //2.here we enable users location
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

    //6.creating location callback to pass in the fusedLocationProviderClient to request location updates
    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);

            //for setting the user location marker
            if(mMap != null) {
                setUserLocationMarker(locationResult.getLastLocation());
            }
        }
    };


    //4.this method starts the location updates
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

        //we use the fusedLocationProviderClient to start location updates
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    //5.this method stops the location updates
    private void stopLocationUpdates() {

        //now to stop the location updates we use the same fusedLocationProviderClient
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    //7.overiding the onStart method of our activity
    @Override
    protected void onStart() {
        super.onStart();


        //here we call the startLocationUpdates method which starts the location updates
        startLocationUpdates();
    }

    //8.overriding the onStop method of our activity
    @Override
    protected void onStop() {
        super.onStop();

        //here we call the stopLocationUpdates method which stops the location updatse
        stopLocationUpdates();
    }

    //9.whenever we recive location results inside location callback we need to set user location marker on the map
    //we call this method only when the map is ready
    private void setUserLocationMarker(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if(pickUpMarker == null) {
            //we create a new marker

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            //userLocationMarker = mMap.addMarker(markerOptions.title("You are here"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }
        else {
            //use the previously created marker
            pickUpMarker.setPosition(latLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        }

        bookCab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                if(requestType) {

                    requestType = false;
                    geoQuery.removeAllListeners();
                    driverLocationRef.removeEventListener(driverLocationRefListner);

                    if(driverFound != null) {
                        driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                                .child(driverFoundId).child("CustomerRideId");
                        driverRef.removeValue();
                        driverFoundId = null;
                    }

                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);
                    geoFire.removeLocation(customerId);

                    if(pickUpMarker != null) {
                        pickUpMarker.remove();
                    }
                    bookCab.setText("Call a Cab");

                    relativeLayout.setVisibility(View.GONE);

                }else{

                    requestType = true;

                    GeoFire geoFire = new GeoFire(customerDatabaseRef);

                    //here we save the customer id and location to the firebase
                    geoFire.setLocation(customerId, new GeoLocation(location.getLatitude(), location.getLongitude()));

                    customerPickUpLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title("PickUp location"));

                    //once customer clicks on the book a cab button it wiil search nearby drivers and the button text will change
                    bookCab.setText("Finding cab nearby");

                    //creating method which will get the nearby cab drivers
                    getNearbyCab();

                }
            }
        });


    }

    //this method will find the cabs available near the customer
    private void getNearbyCab() {

        //here we are getting the available drivers from the firebase
        GeoFire geoFire = new GeoFire(driverAvailableRef);

        //here we search the driver in a radius around the customerPickUpLocation
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPickUpLocation.latitude, customerPickUpLocation.longitude), radius);

        geoQuery.removeAllListeners();//just to prevent from errors when it will call the method again

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //whenever the driver is available in the nearby location this method will be called

                //we have already declared a boolean varible driverFound = false, so if driver found is = true, we found a driver
                if(!driverFound && requestType) {
                    driverFound = true;

                    //once driver is found we will get the driver id usig the key from the firebase
                    driverFoundId = key;

                    driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideId", customerId);
                    driverRef.updateChildren(driverMap);

                    //this method shows the customer the driver location
                    GetDriverLocation();
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
                //this method will be called when ther is no driver available nearby

                if(!driverFound) {

                    //so if the driver is not found we will increment the search radius and again search for available drivers
                    radius = radius + 1;
                    getNearbyCab();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    //getting the drivers location on the customers map
    private void GetDriverLocation() {

        driverLocationRefListner = driverLocationRef.child(driverFoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        //if the data under the driver working node exists,which means driver found
                        if(snapshot.exists() && requestType) {
                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();

                            //creating double data type var to store lat lang data which is saved in string data type, which will be used to calculate distance later
                            double LocationLat = 0;
                            double LocationLng = 0;
                            bookCab.setText("Cab Found");

                            relativeLayout.setVisibility(View.VISIBLE);
                            getAssignedDriverInfo();

                            //when driver is found we get the lat & lang from the db and convert it into double datatype and store in our var
                            if(driverLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if(driverLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            //once we get the driver location we will add a marker on the customer map showing the driver location
                            //so here we create a marker
                            LatLng driverLatLang = new LatLng(LocationLat, LocationLng);

                            //if driver cancel the request or phone gets off we need to remove the driver location marker from the customer map
                            if(driverMarker != null) {
                                driverMarker.remove();
                            }

                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLang).title("Your Cab"));

                            //we also display the distance between the driver and the customer
                            //location1 is the customer location
                            Location location1 = new Location("");
                            location1.setLatitude(customerPickUpLocation.latitude);
                            location1.setLongitude(customerPickUpLocation.longitude);

                            //location2 is the driver location
                            Location location2 = new Location("");
                            location2.setLatitude(driverLatLang.latitude);
                            location2.setLongitude(driverLatLang.longitude);

                            //here we calculate the distance in meters between the customer and the driver
                            float Distance = location1.distanceTo(location2);

                            //to notify rider when driver arrived at pickup location
                            if(Distance < 90) {
                                bookCab.setText("Cab arrived");
                            }else {

                                //then we display the distance on the bookcab button
                                bookCab.setText("Cab Found " + String.valueOf(Distance) + " away");
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }


    //here we zoom to the user location as soon our maps activity is started
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

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //mMap.addMarker(new MarkerOptions().position(latLng).title("You are here"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            }
        });

    }

    //3.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();

            }

        }
    }

    private void getAssignedDriverInfo() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    String uName = snapshot.child("name").getValue().toString();
                    String uPhone = snapshot.child("phone").getValue().toString();
                    String profilePic = snapshot.child("image").getValue().toString();
                    String uCar = snapshot.child("car").getValue().toString();

                    driverName.setText(uName);
                    driverPhoneNo.setText(uPhone);
                    driver_CarName.setText(uCar);
                    Picasso.get().load(profilePic).into(driverImage);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}