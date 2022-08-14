package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

public class DriverRegActivity extends AppCompatActivity {

    private EditText dEmail, dPassword;
    private Button dReg;

    //to authenticate the driver we use
    private FirebaseAuth firebaseAuth;

    //while authenticating the user we show progress bar
    private ProgressDialog progressDialog;

    //to store UID of the driver in the firebase database
    private DatabaseReference driverDatabaseRef;

    //to get the drivers UID
    private String onlineDriverId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_reg);

        //initializing the FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //initializing the ProgressDialog
        progressDialog = new ProgressDialog(this);

        defineUi();

        buttonAction();



    }

    //assigning varibles with layout id
    private void defineUi(){
        dEmail = findViewById(R.id.etDRegEmail);
        dPassword = findViewById(R.id.etDRegPassword);
        dReg = findViewById(R.id.btnDriverReg);

    }

    //defining the button actions
    private void buttonAction(){

        dReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //we are getting the data from the email & pass fields & storing in our new variables
                String d_email = dEmail.getText().toString();
                String d_password = dPassword.getText().toString();

                //once we get the above data we will register the driver
                registerDriver(d_email, d_password);
            }
        });

    }

    //defining the registration process
    private void registerDriver(String d_email, String d_password){

        //to check if user has entered his email or not
        if(TextUtils.isEmpty(d_email)){
            Toast.makeText(DriverRegActivity.this,"Please enter an email", Toast.LENGTH_SHORT).show();
        }

        //to check if user has entered his password or not
        if(TextUtils.isEmpty(d_password)){
            Toast.makeText(DriverRegActivity.this,"Please enter a password", Toast.LENGTH_SHORT).show();
        }

        //once all the fields are filled then we will register the driver
        else{

            //while the authentication process is in progress we show a Progress bar with
            progressDialog.setTitle("Driver Registration");
            progressDialog.setMessage("Just a moment");
            progressDialog.show();


            //here we create the user in Firebase
            firebaseAuth.createUserWithEmailAndPassword(d_email, d_password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){

                                //here we will get the UID of the driver in our variable who is going to register
                                onlineDriverId = firebaseAuth.getCurrentUser().getUid();

                                //initializing the Database Reference and create node to save driverId
                                driverDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(onlineDriverId);

                                driverDatabaseRef.setValue(true);

                                Toast.makeText(DriverRegActivity.this, "SignUp Sucessfull", Toast.LENGTH_SHORT).show();
                                //once the above task is sucessfull well dismiss the progress bar
                                progressDialog.dismiss();
                            }
                            else{
                                Toast.makeText(DriverRegActivity.this, "SignUp Failed", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }
                    });
        }
    }
}