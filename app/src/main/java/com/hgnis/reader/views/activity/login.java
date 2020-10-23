package com.hgnis.reader.views.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.hgnis.reader.R;
import com.hgnis.reader.utility.AppSharePreference;

import org.json.JSONException;

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
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(getApplicationContext());
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);
        initializeALL();

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
                    (object, response) -> {
                        // Application code
                        try {
                            Log.i("Response", response.toString());

                            String firstName = response.getJSONObject().getString("first_name");
                            Log.i("Login" + "FirstName", firstName);
                            appSharePreference.setName(firstName);
                            handleFacebookAccessToken(accessToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
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
        mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            Toast.makeText(this, "yes", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "NO", Toast.LENGTH_SHORT).show();
        }
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
        googleLoginIntegration();
        facebookLoginIntegration();
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
            firebaseAuthWithGoogle(task.getResult().getIdToken());
            appSharePreference.setName(task.getResult().getDisplayName());
        } else if (task.isCanceled()) {
            Toast.makeText(this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
        } else if (task.isComplete()) {
            Toast.makeText(this, "Relax we don't store anything, EveryThing is stored on your phone only ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String user = mAuth.getCurrentUser().getUid();
                            appSharePreference.setUID(user);
                            sendToHomePage();
                        } else {
                            Toast.makeText(login.this, "" + task.getException(), Toast.LENGTH_SHORT).show();

                        }

                        // ...
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("TAG", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String user = mAuth.getCurrentUser().getUid();
                            appSharePreference.setUID(user);
                            sendToHomePage();

                        } else {
                            Toast.makeText(login.this, "" + task.getException(), Toast.LENGTH_SHORT).show();
                        }

                    }
                });
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