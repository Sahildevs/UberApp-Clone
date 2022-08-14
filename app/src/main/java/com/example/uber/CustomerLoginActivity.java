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

public class CustomerLoginActivity extends AppCompatActivity {

    private EditText cemail, cpassword;
    private Button clogin;
    private TextView cReg;

    //to authenticate the customer we use
    private FirebaseAuth firebaseAuth;

    //while authenticating the user we show progress bar
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        //initializing the FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //initializing the Progress dialog
        progressDialog = new ProgressDialog(this);

        //checking if user has already loged in to our app
        FirebaseUser user = firebaseAuth.getCurrentUser();

        //if user already logedin, case is not null
        if(user != null){
            finish();  //destroyes the activity
            startActivity(new Intent(CustomerLoginActivity.this, CustomerMapActivity.class));
        }

        definUi();

        buttonAction();

    }

    //assigning variables to layout id
    private void definUi(){
        cemail = findViewById(R.id.etCustmEmail);
        cpassword = findViewById(R.id.etCustmPassword);
        clogin = findViewById(R.id.btnCustmLogin);
        cReg = findViewById(R.id.tvCustmSignup);
    }

    //defining the button actions
    private void buttonAction(){

        cReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CustomerLoginActivity.this, CustomerRegActivity.class));
            }
        });

        clogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //we are getting the data from the email & pass fields & storing in our new variables
                String c_email = cemail.getText().toString();
                String c_password = cpassword.getText().toString();

                //once we get the above data we will log the customer in
                customerlogin(c_email, c_password);
            }
        });

    }

    private void customerlogin(String c_email, String c_password) {

        //to check if user has entered his email in the emali field
        if (TextUtils.isEmpty(c_email)) {
            Toast.makeText(CustomerLoginActivity.this, "Please enter an email", Toast.LENGTH_SHORT).show();
        }

        //to check if user has entered his password in the password field
        if (TextUtils.isEmpty(c_password)) {
            Toast.makeText(CustomerLoginActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
        }

        //once all the fields are filled then we will login the customer
        else {

            //while the authentication process is in progress we show a Progress bar with
            progressDialog.setTitle("Customer Login");
            progressDialog.setMessage("Please wait till we verify you");
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(c_email, c_password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){
                                Toast.makeText(CustomerLoginActivity.this, "Login Sucessful", Toast.LENGTH_SHORT).show();
                                //once the above task is sucessfull well dismiss the progress bar
                                progressDialog.dismiss();

                                //once the above task is sucessful direct the customer to the google maps activity
                                startActivity(new Intent(CustomerLoginActivity.this, CustomerMapActivity.class));
                            }

                            else {
                                Toast.makeText(CustomerLoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }
                    });

        }

    }
}