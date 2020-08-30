package com.techheralds.mymaligai.supplier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginActivity extends AppCompatActivity {
    Button loginBtn;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    EditText phoneNumer;
    CircleImageView logo;
    TextView title;
    String currSupplierId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Login");
        }

        loginBtn = findViewById(R.id.loginBtn);
        phoneNumer = findViewById(R.id.phoneNumber);
        logo = findViewById(R.id.logo);
        title = findViewById(R.id.title);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        user = firebaseAuth.getCurrentUser();

        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        //Read json data
        readJsonData("supplierdetails.json");

    }

    public void login() {
        final String phoneNumerIp = phoneNumer.getText().toString();
        if (!phoneNumerIp.equals("")) {
            final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, "",
                    "Checking Supplier.Please wait...", true);
            firebaseAuth.signInWithEmailAndPassword("test@user.com", "Tech321").addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    firebaseDatabase.getReference().child("suppliers").limitToFirst(1).orderByChild("phoneNumber").equalTo("+91" + phoneNumerIp).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                    Supplier data = ds.getValue(Supplier.class);
                                    if (data.getUid().equals(currSupplierId)) {
                                        dialog.dismiss();
                                        List<AuthUI.IdpConfig> providers = Arrays.asList(
                                                new AuthUI.IdpConfig.PhoneBuilder().setDefaultNumber("IN", phoneNumerIp).build());
                                        startActivityForResult(
                                                AuthUI.getInstance()
                                                        .createSignInIntentBuilder()
                                                        .setAvailableProviders(providers)
                                                        .build(),
                                                001);
                                        } else{
                                            firebaseAuth.signOut();
                                            dialog.dismiss();
                                            Toast.makeText(LoginActivity.this, "Can't login.Supplier data don't match", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                } else{
                                    firebaseAuth.signOut();
                                    dialog.dismiss();
                                    Toast.makeText(LoginActivity.this, "No Supplier registered with this phone number", Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onCancelled (@NonNull DatabaseError databaseError){

                            }
                        });
                    }
                }).

                addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure (@NonNull Exception e){
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            } else{
                Toast.makeText(this, "Enter 10 digit phone number", Toast.LENGTH_SHORT).show();
            }
        }


        public void readJsonData (String params){
            try {
                InputStream is = getAssets().open(params);

                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String mResponse = new String(buffer);
                JSONObject jsonObject = new JSONObject(mResponse);

                if (!jsonObject.get("SUPPLIERNAME").toString().equals("")) {
                    title.setText(jsonObject.get("SUPPLIERNAME").toString());
                }

                if (!jsonObject.get("SUPPLIERLOGO").toString().equals("")) {
                    logo.setBackgroundResource(R.drawable.rounded_corners);
                    Picasso.with(getApplicationContext()).load(jsonObject.get("SUPPLIERLOGO").toString()).into(logo);
                }

                if (!jsonObject.get("SUPPLIERID").toString().equals("")) {
                    currSupplierId = jsonObject.get("SUPPLIERID").toString();
                }

            } catch (IOException e) {
                // Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                // Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onActivityResult ( int requestCode, int resultCode, Intent data){
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == 001) {

                if (resultCode == RESULT_OK) {
                    // Successfully signed in
                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, "",
                            "Please wait...", true);

                    firebaseDatabase.getReference().child("suppliers/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue() != null) {
                                Supplier data = dataSnapshot.getValue(Supplier.class);

                                if (data.getStatus()) {
                                    //Save sms template locally
                                    SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("smsTemplate", data.getSmsTemplate());
                                    editor.apply();

                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(data.getName())
                                            .build();

                                    user.updateProfile(profileUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            dialog.dismiss();
                                            finish();
                                        }
                                    });
                                } else {
                                    dialog.dismiss();
                                    new AlertDialog.Builder(LoginActivity.this).setTitle("Access Denied")
                                            .setMessage("Your account is disabled.Please contact the admin to enable it!")
                                            .setPositiveButton("Ok", null).show();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(LoginActivity.this, "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                        }
                    });


                }
            }
        }

    }
