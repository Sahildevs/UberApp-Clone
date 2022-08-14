package com.example.uber;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerRegActivity extends AppCompatActivity {

    private EditText cEmail, cPassword;
    private Button cReg;

    //to authenticate the customer we use
    private FirebaseAuth firebaseAuth;

    //while authenticating the user we show progress bar
    private ProgressDialog progressDialog;

    //to store UID of the customer in the firebase database
    private DatabaseReference customerDatabaseRef;

    //to get the customer UID
    private String onlineCustomerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_reg);

        //initializing the FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //initializing the ProgressDialog
        progressDialog = new ProgressDialog(this);

        defineUi();

        buttonAction();

    }

    //assigning variables to layout id
    private void defineUi(){
        cEmail = findViewById(R.id.etCRegEmail);
        cPassword = findViewById(R.id.etCRegPassword);
        cReg = findViewById(R.id.btnCustmReg);
    }

    //defining the button actions
    private void buttonAction(){

        cReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //we are getting the data from the email & pass fields & storing in our new variables
                String c_Email = cEmail.getText().toString();
                String c_Password = cPassword.getText().toString();

                //once we get the above data we will register the driver
                customerRegistration(c_Email, c_Password);
            }
        });
    }

    //defining the regesteration process
    private void customerRegistration(String c_Email, String c_Password){

        //to check if user has entered his email in the emali field
        if(TextUtils.isEmpty(c_Email)){
            Toast.makeText(CustomerRegActivity.this, "Please enter an email", Toast.LENGTH_SHORT).show();
        }

        //to check if user has entered his password in the password field
        if(TextUtils.isEmpty(c_Password)){
            Toast.makeText(CustomerRegActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
        }

        //once all the fields are filled then we will register the customer
        else{

            //while the authentication process is in progress we show a Progress bar with
            progressDialog.setTitle("Customer Regestiration");
            progressDialog.setMessage("Just a moment");
            progressDialog.show();

            //here we create the user in Firebase
            firebaseAuth.createUserWithEmailAndPassword(c_Email, c_Password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {

                                //here we will get the UID of the customer who is going to register in our variable
                                onlineCustomerId = firebaseAuth.getCurrentUser().getUid();

                                //initializing the Database Reference and create node to save customerId
                                customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(onlineCustomerId);

                                customerDatabaseRef.setValue(true);

                                Toast.makeText(CustomerRegActivity.this, "SignUp Sucessfull", Toast.LENGTH_SHORT).show();
                                //once the above task is sucessfull well dismiss the progress bar
                                progressDialog.dismiss();

                            }
                            else{
                                Toast.makeText(CustomerRegActivity.this, "SignUp Failed", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }
                    });

        }
    }
}