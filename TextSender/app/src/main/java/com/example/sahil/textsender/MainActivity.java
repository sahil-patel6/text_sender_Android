package com.example.sahil.textsender;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    EditText sendText;
    TextView getText;
    String textFromClipboard;
    Button sendButton;
    Button getButton;
    Button signIn;
    FirebaseAuth mref;
    DatabaseReference mDatabase;

    ValueEventListener postListener;

    ConstraintLayout constraintLayout;

    final int RC_SIGN_IN = 121;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendText = (EditText) findViewById(R.id.send_text);
        sendText.setMovementMethod(new ScrollingMovementMethod());
        getText = (TextView) findViewById(R.id.get_text);
        getText.setMovementMethod(new ScrollingMovementMethod());
        sendButton = (Button) findViewById(R.id.send_button);
        getButton = (Button) findViewById(R.id.get_button);

        constraintLayout = (ConstraintLayout) findViewById(R.id.main);

        ClipboardManager cp = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cp.hasPrimaryClip()) {
            sendText.setText(cp.getText().toString());
            textFromClipboard = cp.getText().toString();
        }

        signIn = (Button) findViewById(R.id.signIn);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        final List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build());

        mref = FirebaseAuth.getInstance();
        mref.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {

                    sendButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Snackbar.make(constraintLayout,"Please Sign In First",Snackbar.LENGTH_LONG).show();
                        }
                    });

                    getButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Snackbar.make(constraintLayout,"Please Sign In First",Snackbar.LENGTH_LONG).show();
                        }
                    });

                    signIn.setText("Sign In/ Sign Up");
                    signIn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            startActivityForResult(AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .setIsSmartLockEnabled(false)
                                    .build(), RC_SIGN_IN);
                        }
                    });

                }else if (firebaseAuth.getCurrentUser() != null) {
                    signIn.setText("Sign Out from "+ firebaseAuth.getCurrentUser().getDisplayName());

                    sendButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mDatabase.child("users").child(firebaseAuth.getUid()).setValue(sendText.getText().toString(), new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                    if (databaseError == null){
                                        Snackbar.make(constraintLayout,"Text Sent Successfully",Snackbar.LENGTH_LONG).show();
                                    }else if (databaseError != null){
                                        Snackbar.make(constraintLayout,"An error occured while sending",Snackbar.LENGTH_LONG).show();
                                        Log.e("Error While Sending",databaseError.toString());
                                    }
                                }
                            });
                        }
                    });

                    getButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.child("users").child(mref.getUid()).getValue()!=null) {
                                        String value = dataSnapshot.child("users").child(mref.getUid()).getValue().toString();
                                        getText.setText(value);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Snackbar.make(constraintLayout,"An error occured while getting",Snackbar.LENGTH_LONG).show();
                                }
                            });
                        }
                    });

                    postListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                            if (dataSnapshot.child("users").child(mref.getUid()).getValue() != null){
                                String value = dataSnapshot.child("users").child(mref.getUid()).getValue().toString();
                                if (!sendText.getText().toString().equals(value) && value != null) {
                                    getText.setText(value);
                                }
                            } else if (dataSnapshot.child("users").child(mref.getUid()).getValue() == null){
                                getText.setText("Post Something to load Text");
                                Snackbar.make(constraintLayout,"Post Something to load Text",Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e("Error from database",databaseError.toString());
                            Snackbar.make(constraintLayout,"An Error Occured",Snackbar.LENGTH_LONG).show();
                        }
                    };

                    signIn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AuthUI.getInstance()
                                    .signOut(MainActivity.this)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            mDatabase.removeEventListener(postListener);
                                            Snackbar.make(constraintLayout,"Signed out successfully",Snackbar.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    });

                    mDatabase.addValueEventListener(postListener);

                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();


                signIn.setText("Sign out from " + user.getDisplayName());
                Snackbar.make(constraintLayout, "Sign In Successful", Snackbar.LENGTH_LONG).show();

                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Log.e("error while signing", String.valueOf(response.getError().getErrorCode()));
                Snackbar.make(constraintLayout, "An error ouccured while signing you", Snackbar.LENGTH_LONG).show();
            }
        }
    }

}
