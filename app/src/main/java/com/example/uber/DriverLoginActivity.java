package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText demail, dpassword;
    private TextView dReg;
    private Button dlogin;

    //to authenticate the driver we use
    private FirebaseAuth firebaseAuth;

    //while authenticating the user we show progress bar
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        //initializing the FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //initializing the Progress dialog
        progressDialog = new ProgressDialog(this);

        //checking if user has already loged in to our app
        FirebaseUser user = firebaseAuth.getCurrentUser();

        //if user already logedin, case is not null
        if(user != null){
            finish();  //destroyes the activity
            startActivity(new Intent(DriverLoginActivity.this, DriversMapActivity.class));
        }

        defineUi();

        buttonAction();

    }

    //assigning variables to layout id
    private void defineUi(){
        demail = findViewById(R.id.etDriverEmail);
        dpassword = findViewById(R.id.etDriverPassword);
        dlogin = findViewById(R.id.btnDriverLogin);
        dReg = findViewById(R.id.tvDriverSignup);

    }

    //defining the button actions
    private void buttonAction(){

        dReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DriverLoginActivity.this, DriverRegActivity.class));
            }
        });

        dlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //we are getting the data from the email & pass fields & storing in our new variables
                String d_Email = demail.getText().toString();
                String d_Password = dpassword.getText().toString();

                //once we get the above data we will login the driver
                LoginDriver(d_Email, d_Password);
            }
        });

    }

    //definining the login process
    private void LoginDriver(String d_Email, String d_Password) {

        //to check if user has entered his email or not
        if(TextUtils.isEmpty(d_Email)){
            Toast.makeText(DriverLoginActivity.this, "Please enter an email", Toast.LENGTH_SHORT).show();
        }

        //to check if user has entered his password or not
        if(TextUtils.isEmpty(d_Password)){
            Toast.makeText(DriverLoginActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
        }

        //once all the fields are filled then we will register the driver
        else{

            //while the authentication process is in progress we show a Progress bar with
            progressDialog.setTitle("Driver Login");
            progressDialog.setMessage("Please wait till we verify you");
            progressDialog.show();

            //here we authenticate the credentials entered by the driver and log him in
            firebaseAuth.signInWithEmailAndPassword(d_Email, d_Password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){
                                Toast.makeText(DriverLoginActivity.this, "Login Sucessful", Toast.LENGTH_SHORT).show();
                                //once the above task is sucessfull well dismiss the progress bar
                                progressDialog.dismiss();

                                //once the above task is sucessful direct the driver to the google maps activity
                                startActivity(new Intent(DriverLoginActivity.this, DriversMapActivity.class));
                            }
                            else{
                                Toast.makeText(DriverLoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }
                    });

        }
    }
}