package com.example.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.net.URI;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private EditText name, phone, driverCarName;
    private ImageView close, save;
    private TextView changeImage;

    //to get the string value either from customer or driver
    private String getType;

    //to check if user wants to change the profile image
    private String checker;

    //to display the chosen profile image in form of uri
    private Uri profileImageUri;
    private String myUri = "";
    private StorageTask uploadTask;
    private StorageReference storageProfilePicRef;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        getType = getIntent().getStringExtra("type");
        //to see if it is driver or customer
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfilePicRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");


        close = findViewById(R.id.ivCloseButton);
        save = findViewById(R.id.ivSaveButton);

        profileImage = findViewById(R.id.profile_image);
        changeImage = findViewById(R.id.tvChangePicture);

        name = findViewById(R.id.etName);
        phone = findViewById(R.id.etPhone);
        driverCarName = findViewById(R.id.etDriverCar);
        if(getType.equals("Drivers")) {
            driverCarName.setVisibility(View.VISIBLE);
        }

        buttonActions();

    }

    private void buttonActions() {

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(getType.equals("Drivers")) {
                    startActivity(new Intent(ProfileActivity.this, DriversMapActivity.class));
                }
                else {
                    startActivity(new Intent(ProfileActivity.this, CustomerMapActivity.class));
                }
            }
        });

        changeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //if the yser clicks on the change image textView it means he wants to change imgae and the checker = clicked
                checker = "clicked";

                //then we will send the user to the gallery to choose an image and crop it
                CropImage.activity().setAspectRatio(1, 1).start(ProfileActivity.this);

            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checker.equals("clicked")) {
                    //user also wants to upload the profile image with the other info

                    validateControllers();

                }
                else {

                }

            }
        });

        //this mehod will set the user info in the profile activity
        getUserInfo();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data!=null) {

            //this will get the image from the gallery
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            profileImageUri = result.getUri();
            //then will display the image in our image view
            profileImage.setImageURI(profileImageUri);
        }
        else {

            if(getType.equals("Drivers")) {
                startActivity(new Intent(ProfileActivity.this, DriversMapActivity.class));
            }
            else {
                startActivity(new Intent(ProfileActivity.this, CustomerMapActivity.class));
            }

            Toast.makeText(this, "Error! try again", Toast.LENGTH_SHORT).show();
        }
    }

    //this method will check if the text fields are empty or not
    private void validateControllers() {

        if(TextUtils.isEmpty(name.getText().toString())) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(phone.getText().toString())) {
            Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
        }
        else if(getType.equals("Drivers") && TextUtils.isEmpty(driverCarName.getText().toString())) {
            Toast.makeText(this, "Please enter your car name", Toast.LENGTH_SHORT).show();
        }
        else if(checker.equals("clicked")) {
            uploadProfilePic();
        }

    }

    private void uploadProfilePic() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Account Info");
        progressDialog.setMessage("Please wait while we update your info");
        progressDialog.show();

        if(profileImageUri != null) {
            final StorageReference fileRef = storageProfilePicRef.child(mAuth.getCurrentUser().getUid() + ".jpg");

            uploadTask = fileRef.putFile(profileImageUri); //store the image in profile picture folder in firestore

            //to get the link of the image to store in firebase database so can retrive and display in profile activity
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {

                    if(!task.isSuccessful()) {

                        throw task.getException();
                    }
                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    if(task.isSuccessful()) {
                        Uri downloadUri = task.getResult();

                        myUri = downloadUri.toString();

                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", mAuth.getCurrentUser().getUid());
                        userMap.put("name", name.getText().toString());
                        userMap.put("phone", phone.getText().toString());
                        userMap.put("image", myUri);

                        if(getType.equals("Drivers")) {
                            userMap.put("car", driverCarName.getText().toString());
                        }

                        //then will add all the info the the database
                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

                        progressDialog.dismiss();

                        if(getType.equals("Drivers")) {
                            startActivity(new Intent(ProfileActivity.this, DriversMapActivity.class));
                        }
                        else {
                            startActivity(new Intent(ProfileActivity.this, CustomerMapActivity.class));
                        }
                    }
                }
            });
        }
    }

    private void getUserInfo() {

        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists() && snapshot.getChildrenCount() > 0) {

                    String uName = snapshot.child("name").getValue().toString();
                    String uPhone = snapshot.child("phone").getValue().toString();
                    String profilePic = snapshot.child("image").getValue().toString();

                    name.setText(uName);
                    phone.setText(uPhone);
                    Picasso.get().load(profilePic).into(profileImage);

                    if(getType.equals("Drivers")) {
                        String uCar = snapshot.child("car").getValue().toString();
                        driverCarName.setText(uCar);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}