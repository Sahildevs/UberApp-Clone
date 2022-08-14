package com.example.uber;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread thread = new Thread(){
            @Override
            public void run() {

                try {
                    //we want to display the splash screen for 7 seconds
                    sleep(7000);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                finally {
                    //after 7 seconds on splash screen go to welcome screen
                    Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                    startActivity(intent);

                }
            }
        };

        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        finish();
    }
}