package com.hgnis.reader.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.hgnis.reader.R;
import com.hgnis.reader.utility.AppSharePreference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import de.hdodenhof.circleimageview.CircleImageView;

public class login extends AppCompatActivity implements View.OnClickListener {
    GoogleSignInOptions gso;
    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInAccount account;
    private static final String EMAIL = "email";
    CircleImageView signInButton;
    Intent signInIntent;
    int RC_SIGN_IN = 101;
    CircleImageView facebookLogin;
    AppSharePreference appSharePreference;
    CallbackManager callbackManager;
    LoginButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        googleLoginIntegration();
        initializeALL();
        facebookLoginIntegration();
    }

    private void facebookLoginIntegration() {
        facebookLogin = findViewById(R.id.myFaceBookButton);
        facebookLogin.setOnClickListener(this);
        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList(EMAIL));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                getProfileDetails(loginResult);
                sendToHomePage();
            }

            @Override
            public void onCancel() {
                // App code
                Toast.makeText(login.this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Toast.makeText(login.this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
            }
        });


        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        getProfileDetails(loginResult);
                        sendToHomePage();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(login.this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Toast.makeText(login.this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getProfileDetails(LoginResult loginResult) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            GraphRequest request = GraphRequest.newMeRequest(
                    loginResult.getAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            // Application code
                            try {
                                Log.i("Response", response.toString());

                                String firstName = response.getJSONObject().getString("first_name");
                                Log.i("Login" + "FirstName", firstName);
                                appSharePreference.setName(firstName);
                                sendToHomePage();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "first_name");
            request.setParameters(parameters);
            request.executeAsync();
        }
    }

    private void initializeALL() {
        appSharePreference = new AppSharePreference(this);
        appSharePreference.clearPreferences();
    }

    private void googleLoginIntegration() {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        account = GoogleSignIn.getLastSignedInAccount(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
            case R.id.myFaceBookButton:
                loginButton.performClick();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == RC_SIGN_IN) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleGoogleSignInResult(task);
            }
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        if (task.isSuccessful()) {
            appSharePreference.setName(task.getResult().getDisplayName());
            sendToHomePage();
        } else if (task.isCanceled()) {
            Toast.makeText(this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
        } else if (task.isComplete()) {
            Toast.makeText(this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendToHomePage() {

        if (appSharePreference.getName().isEmpty()) {
        } else {
            appSharePreference.setCounter("3");
            Intent i = new Intent(login.this, MainActivity.class);
            startActivity(i);
            finish();

        }

    }
}