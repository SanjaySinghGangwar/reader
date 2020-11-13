package com.hgnis.reader.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.hgnis.reader.R;

public class SplashScreen extends AppCompatActivity {

    Intent i;
    Handler hand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        hand = new Handler();


    }

    @Override
    protected void onStart() {
        super.onStart();
        hand.postDelayed(new Runnable() {
            @Override
            public void run() {
                i = new Intent(SplashScreen.this, MainActivity.class);
                startActivity(i);
                finish();
            }
        }, 1500);
    }
}