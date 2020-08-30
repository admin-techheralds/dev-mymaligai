package com.techheralds.mymaligai.admin;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_READ_PHONE_STATE = 101;
    public static final int REQUEST_SEND_SMS = 102;
    public static final int REQUEST_RECEIVE_SMS = 103;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    FirebaseRemoteConfig firebaseRemoteConfig;
    FirebaseStorage firebaseStorage;
    private FirebaseDatabase firebaseDatabase1;
    FloatingActionButton addSupplierBtn;
    ListView listView;
    supplierAdapterList adapterList;
    ArrayList<Supplier> suppliers;
    OkHttpClient client = new OkHttpClient();
    String currSupplierId = null;
    String buildingSts = "";
    String currURL = null;
    String bitly_api_key = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Suppliers");
        }

        firebaseDatabase1 = firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(1000)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("bitly_api_key", "");
        firebaseRemoteConfig.setDefaultsAsync(defaults);

        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful()) {
                    bitly_api_key = firebaseRemoteConfig.getString("bitly_api_key");
                    //  Toast.makeText(MainActivity.this, bitly_api_key, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        addSupplierBtn = findViewById(R.id.newSupplierFAB);
        listView = findViewById(R.id.listView);

        suppliers = new ArrayList<>();

        addSupplierBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddSupplierActivity.class);
                startActivity(intent);
            }
        });

        final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, null, "Please wait...");
        firebaseDatabase.getReference().child("suppliers/").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                suppliers.clear();
                progressDialog.dismiss();
                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Supplier data = ds.getValue(Supplier.class);
                        suppliers.add(data);
                        adapterList = new supplierAdapterList(MainActivity.this, suppliers);
                        listView.setAdapter(adapterList);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No supplier registered", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "Can't Load", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public class supplierAdapterList extends BaseAdapter {
        Context context;
        ArrayList<Supplier> suppliers;

        public supplierAdapterList(Context context, ArrayList<Supplier> suppliers) {
            this.context = context;
            this.suppliers = suppliers;
        }

        @Override
        public int getCount() {
            return suppliers.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }


        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.supplier_list, parent, false);
            TextView name = view.findViewById(R.id.name);
            final TextView status = view.findViewById(R.id.status);
            CircleImageView photo = view.findViewById(R.id.photo);

            name.setText(suppliers.get(position).getName());

            final Boolean supplierStatus = suppliers.get(position).getStatus();

            if (supplierStatus) {
                status.setText("Enabled");
            } else {
                status.setText("Disabled");
            }

            String supplierPhoto = suppliers.get(position).getPhoto();

            if (supplierPhoto.equals("")) {
                photo.setImageResource(R.drawable.nouser);
            } else {
                Picasso.with(MainActivity.this).load(supplierPhoto).into(photo);
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currSupplierId = suppliers.get(position).getUid();
                    final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
                    bottomSheetDialog.setContentView(R.layout.edit_supplier_sheet);
                    final TextView buildStatus, buildingStatus, buildingStatusTxt;
                    Spinner spinner;
                    final Button updateBtn, buildBtn, smsBtn, stopBtn;
                    final Boolean[] selectedStatus = new Boolean[1];
                    spinner = bottomSheetDialog.findViewById(R.id.spinner);
                    updateBtn = bottomSheetDialog.findViewById(R.id.btn);
                    buildBtn = bottomSheetDialog.findViewById(R.id.buildBtn);
                    smsBtn = bottomSheetDialog.findViewById(R.id.sendSmsbtn);
                    stopBtn = bottomSheetDialog.findViewById(R.id.stopBtn);
                    buildStatus = bottomSheetDialog.findViewById(R.id.buildStatus);
                    buildingStatusTxt = bottomSheetDialog.findViewById(R.id.buildingStatusTxt);
                    buildingStatus = bottomSheetDialog.findViewById(R.id.buildingStatus);

                    ArrayList<String> spinnerArr = new ArrayList<>();
                    spinnerArr.add("Enable");
                    spinnerArr.add("Disable");

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.support_simple_spinner_dropdown_item, spinnerArr);
                    spinner.setAdapter(adapter);

                    if (suppliers.get(position).getStatus()) {
                        spinner.setSelection(0);
                    } else {
                        spinner.setSelection(1);
                    }

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int sPosition, long id) {
                            if (sPosition == 0) {
                                selectedStatus[0] = true;
                            } else {
                                selectedStatus[0] = false;
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    if (suppliers.get(position).getBuild_details() != null) {
                        String status = suppliers.get(position).getBuild_details().get("status").toString();
                        buildStatus.setText(capitalize(status));

                        if (status.equalsIgnoreCase("completed")) {
                            smsBtn.setVisibility(View.VISIBLE);
                        }
                    } else {
                        buildStatus.setText("No Build");
                        buildBtn.setVisibility(View.VISIBLE);
                    }

                    //Set Building Status
                    final int seconds = 1000;
                    final Handler handler = new Handler();

                    handler.postDelayed(new Runnable() {
                        @SuppressLint("RestrictedApi")
                        @Override
                        public void run() {
                            buildingStatus.setText(buildingSts);
                            handler.postDelayed(this, seconds);

                            if (buildingSts.equalsIgnoreCase("APK build successful.You can share link via SMS")) {
                                buildStatus.setText("Completed");
                                smsBtn.setVisibility(View.VISIBLE);
                                stopBtn.setVisibility(View.GONE);
                                bottomSheetDialog.setCancelable(true);
                                handler.removeMessages(0);
                            } else if (buildingSts.equalsIgnoreCase("build stopped")) {
                                bottomSheetDialog.setCancelable(true);
                                handler.removeMessages(0);
                            }
                        }

                    }, seconds);

                    buildBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            bottomSheetDialog.setCancelable(false);
                            buildingStatusTxt.setVisibility(View.VISIBLE);
                            buildingSts = "Checking Environment...";
                            isEnvBuildRunning();

                            buildBtn.setVisibility(View.GONE);
                            stopBtn.setVisibility(View.VISIBLE);
                        }
                    });

                    stopBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            buildingSts = "Build stopped";
                            bottomSheetDialog.setCancelable(true);
                            buildBtn.setVisibility(View.VISIBLE);
                            stopBtn.setVisibility(View.GONE);
                            stopBuildEnv();
                        }
                    });

                    smsBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                shortURL(suppliers.get(position).getBuild_details().get("apk_file").toString(), suppliers.get(position).getPhoneNumber());
                            } else {
                                requestSMSPermissions();
                            }
                        }
                    });

                    updateBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final ProgressDialog progressDialog = ProgressDialog.show(MainActivity.this, null, "Please wait...");
                            Map<String, Object> data = new HashMap<>();
                            data.put("status", selectedStatus[0]);

                            firebaseDatabase.getReference().child("suppliers/" + suppliers.get(position).getUid()).updateChildren(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    progressDialog.dismiss();
                                    bottomSheetDialog.dismiss();
                                    suppliers.get(position).setStatus(selectedStatus[0]);
                                    if (selectedStatus[0]) {
                                        status.setText("Enabled");
                                    } else {
                                        status.setText("Disabled");
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Error occurred.Pleas try again", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });

                    bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            buildingSts = "";
                        }
                    });

                    bottomSheetDialog.show();
                }
            });

            return view;
        }
    }

    public void isEnvBuildRunning() {
        String url = "https://us-central1-supplier-customer.cloudfunctions.net/app/isBuildEnvironmentRunning";
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                stopBuildEnv();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject = new JSONObject(myResponse);
                                String status = jsonObject.get("status").toString();

                                if (status.equalsIgnoreCase("false")) {
                                    buildingSts = "No current build.Starting build environment...";
                                    startBuildEnv();
                                } else {
                                    buildingSts = "Currently building an APK.Try after sometime";
                                }
                            } catch (JSONException err) {
                                Log.d("Error", err.toString());
                            }
                        }
                    });
                }
            }
        });
    }

    public void startBuildEnv() {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("status", "SUCCESS")
                .build();

        Request request = new Request.Builder()
                .url("https://us-central1-supplier-customer.cloudfunctions.net/app/startBuildEnvironment")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                stopBuildEnv();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buildingSts = "Build environment started.Setting up...";
                            waitBuildEnv();
                        }
                    });
                }
            }
        });
    }

    public void waitBuildEnv() {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("status", "35.193.223.0")
                .build();

        Request request = new Request.Builder()
                .url("https://us-central1-supplier-customer.cloudfunctions.net/app/waitBuildEnvironment")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Looper.prepare();
                stopBuildEnv();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            buildingSts = "Triggering APK build...";
                            triggerSupplierBuild();
                        }
                    });
                }
            }
        });
    }

    public void triggerSupplierBuild() {
        final int seconds = 20000;
        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @SuppressLint("RestrictedApi")
            @Override
            public void run() {
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody body = RequestBody.create(mediaType, "{\r\n    \"supplier_id\" : \"" + currSupplierId + "\"\r\n}\r\n");
                Request request = new Request.Builder()
                        .url("https://us-central1-supplier-customer.cloudfunctions.net/app/triggerSupplierBuild")
                        .method("POST", body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            final String myResponse = response.body().string();
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSONObject jsonObject = new JSONObject(myResponse);
                                        String status = jsonObject.get("status").toString();

                                        if (status.equalsIgnoreCase("success")) {
                                            buildingSts = "APK build started.Creating an APK...";
                                        } else if (status.equalsIgnoreCase("inprogress")) {
                                            buildingSts = "APK building in progress...";
                                        } else if (status.equalsIgnoreCase("completed")) {
                                            buildingSts = "APK build successful.You can share link via SMS";
                                            stopBuildEnv();
                                            handler.removeMessages(0);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                });
                handler.postDelayed(this, seconds);
            }

        }, seconds);

    }

    public void stopBuildEnv() {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("status", "SUCCESS")
                .build();

        Request request = new Request.Builder()
                .url("https://us-central1-supplier-customer.cloudfunctions.net/app/stopBuildEnvironment")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Looper.prepare();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Build environment stopped", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private String capitalize(String capString) {
        StringBuffer capBuffer = new StringBuffer();
        Matcher capMatcher = Pattern.compile("([a-z])([a-z]*)", Pattern.CASE_INSENSITIVE).matcher(capString);
        while (capMatcher.find()) {
            capMatcher.appendReplacement(capBuffer, capMatcher.group(1).toUpperCase() + capMatcher.group(2).toLowerCase());
        }

        return capMatcher.appendTail(capBuffer).toString();
    }

    public void sendOTP(String phoneNumber) {
        String defaultNumber = phoneNumber;
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().setDefaultNumber("IN", defaultNumber).build());


        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 001) {

            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "verified!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "can't complete verification process.Please try again", Toast.LENGTH_LONG).show();
            }
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

        }
        return super.onOptionsItemSelected(item);
    }

    private void requestSMSPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }

        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);

        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
        }

        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);

        if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_RECEIVE_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestSMSPermissions();
                } else {
                    Toast.makeText(this, "Permission Required", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SEND_SMS: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestSMSPermissions();
                    Toast.makeText(this, "Click Send Button Again", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permission Required", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_RECEIVE_SMS: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestSMSPermissions();
                } else {
                    Toast.makeText(this, "Permission Required", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void sendSMS(final String phoneNumber, final String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been accept---
        registerReceiver(new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS Sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        //Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();

                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    public void shortURL(String url, final String phoneNumber) {
        Toast.makeText(this, "Shortening URL...", Toast.LENGTH_SHORT).show();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"long_url\":\"" + url + "\", \"domain\":\"bit.ly\", \"group_guid\":\"Bk876DZoNu7\"}");
        Request request = new Request.Builder()
                .url("https://api-ssl.bitly.com/v4/shorten")
                .method("POST", body)
                .addHeader("Authorization", "Bearer " + bitly_api_key)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Looper.prepare();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject = new JSONObject(myResponse);
                                sendSMS(phoneNumber, "Install MyMaligai Supplier app here:\n" + jsonObject.get("link").toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

}
