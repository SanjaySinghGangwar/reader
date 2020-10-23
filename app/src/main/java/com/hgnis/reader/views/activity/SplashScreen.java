package com.hgnis.reader.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hgnis.reader.R;

public class SplashScreen extends AppCompatActivity {
    FirebaseAuth auth;
    Handler handle;
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        handle = new Handler();
        FirebaseApp.initializeApp(getApplicationContext());
        auth = FirebaseAuth.getInstance();

    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        Toast.makeText(this, "" + user, Toast.LENGTH_SHORT).show();
       /* if (auth == null) {
            Toast.makeText(this, "null", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Not Null", Toast.LENGTH_SHORT).show();
        }*/

    }
}