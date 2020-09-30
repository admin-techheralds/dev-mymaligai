package com.techheralds.mymaligai.supplier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Handler;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.squareup.picasso.Picasso;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.log4j.chainsaw.Main;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseCrashlytics crashlytics;
    FirebaseUser user;
    FirebaseRemoteConfig firebaseRemoteConfig;

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        crashlytics = FirebaseCrashlytics.getInstance();

        crashlytics.setCrashlyticsCollectionEnabled(true);


        firebaseDatabase.getReference().child("suppliers/" + user.getUid() + "/status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (((Boolean) dataSnapshot.getValue()) == false) {
                    new android.app.AlertDialog.Builder(MainActivity.this).setTitle("Access Denied")
                            .setMessage("Your account is disabled.Please contact the admin to enable it!").setCancelable(false)
                            .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    firebaseAuth.signOut();
                                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(1000)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("customer_app_link", "");
        firebaseRemoteConfig.setDefaultsAsync(defaults);

        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful()) {
                    String url = firebaseRemoteConfig.getString("customer_app_link");
                    //  Toast.makeText(MainActivity.this, url, Toast.LENGTH_SHORT).show();
                    //Save sms template locally
                    SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("smsURL", url);
                    editor.apply();
                } else {
                    Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        final FloatingActionButton addItemsFab = findViewById(R.id.fab_add_items);
        final FloatingActionButton inviteFab = findViewById(R.id.fab_invite);

        inviteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InviteActivity.class);
                startActivity(intent);
            }
        });

        addItemsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddItemsActivity.class);
                startActivity(intent);
            }
        });
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_demands_list, R.id.nav_demands_report, R.id.nav_manage_consumers,
                R.id.nav_my_profile)
                .setDrawerLayout(drawer)
                .build();
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        final View header = navigationView.getHeaderView(0);

        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                navController.navigate(R.id.nav_my_profile);
                drawer.closeDrawers();
            }
        });
        final int minutes = 1000;
        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @SuppressLint("RestrictedApi")
            @Override
            public void run() {
                if (firebaseAuth.getCurrentUser() != null) {
                    TextView userName = header.findViewById(R.id.nav_header_userName);
                    userName.setText(firebaseAuth.getCurrentUser().getDisplayName());

                    TextView userPhone = header.findViewById(R.id.nav_header_userPhone);
                    userPhone.setText(firebaseAuth.getCurrentUser().getPhoneNumber());

                    CircleImageView userDp = header.findViewById(R.id.nav_header_userDp);
                    Uri photoUrl = firebaseAuth.getCurrentUser().getPhotoUrl();

                    if (photoUrl != null) {
                        Picasso.with(getApplicationContext()).load(photoUrl).into(userDp);
                    } else {
                        userDp.setImageResource(R.drawable.nouser);
                    }

                    if (navController.getCurrentDestination().getLabel().toString().equalsIgnoreCase("demands list")) {
                        inviteFab.setVisibility(View.GONE);
                        addItemsFab.setVisibility(View.VISIBLE);
                    } else if (navController.getCurrentDestination().getLabel().toString().equalsIgnoreCase("demands report")) {
                        inviteFab.setVisibility(View.GONE);
                        addItemsFab.setVisibility(View.GONE);
                    } else if (navController.getCurrentDestination().getLabel().toString().equalsIgnoreCase("manage customers")) {
                        inviteFab.setVisibility(View.VISIBLE);
                        addItemsFab.setVisibility(View.GONE);
                    } else if (navController.getCurrentDestination().getLabel().toString().equalsIgnoreCase("my profile")) {
                        inviteFab.setVisibility(View.GONE);
                        addItemsFab.setVisibility(View.GONE);
                    }

                    //set json data
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("title", user.getDisplayName());
                        jsonObject.put("logo", user.getPhotoUrl());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                  //  createAndSaveFile("appDetails", jsonObject.toString());

                    handler.postDelayed(this, minutes);
                }
            }
        }, minutes);

    }

    public void createAndSaveFile(String params, String mJsonResponse) {
        try {
            FileWriter file = new FileWriter("/data/data/" + getApplicationContext().getPackageName() + "/" + params);
            file.write(mJsonResponse);
            file.flush();
            file.close();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout from this account?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                firebaseAuth.signOut();
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
                break;

            case R.id.action_see_invited:
                Intent intent = new Intent(MainActivity.this, InviteActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}
