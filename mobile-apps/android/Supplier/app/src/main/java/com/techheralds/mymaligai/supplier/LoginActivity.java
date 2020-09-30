package com.techheralds.mymaligai.supplier;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    Button loginBtn;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    EditText phoneNumer;
    CircleImageView logo;
    TextView title;
    String currSupplierId;

    @RequiresApi(api = Build.VERSION_CODES.O)
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
            if (!user.getPhoneNumber().equals("")) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    login();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        //Read json data
        readJsonData("supplierdetails.json");

        SharedPreferences sharedPreferences = getSharedPreferences("local", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Boolean isChange = sharedPreferences.getBoolean("isChange", false);

        if (isChange) {
            String url = sharedPreferences.getString("url", null);
            logo.setBackgroundResource(R.drawable.rounded_corners);
            Picasso.with(getApplicationContext()).load(url).into(logo);

            editor.putBoolean("isChange", false);
            editor.apply();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void login() throws UnsupportedEncodingException {
        final String phoneNumerIp =  phoneNumer.getText().toString();
        if (!phoneNumerIp.equals("")) {
            String pNo = "+91"+phoneNumerIp;
            String e = Base64.getEncoder().encodeToString(pNo.getBytes("utf-8"));
            checkSupplier(e);
        } else {
            Toast.makeText(this, "Enter 10 digit phone number", Toast.LENGTH_SHORT).show();
        }
    }


    public void readJsonData(String params) {
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
            is.close();
        } catch (IOException e) {
            // Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            // Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                                editor.putLong("supplier_id", data.getSupplier_id());
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

    public void checkSupplier(String encodedString) {
        final ProgressDialog dialog = ProgressDialog.show(LoginActivity.this, "",
                "Checking Supplier.Please wait...", true);
        String url = "https://us-central1-supplier-customer.cloudfunctions.net/app/checkSupplierExists?phonenumber=" + encodedString;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(LoginActivity.this, "fail", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    LoginActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject = new JSONObject(myResponse);
                                String uid = jsonObject.get("id").toString();

                                if (uid != null) {
                                    if (uid.equals(currSupplierId)) {
                                        dialog.dismiss();
                                        final String phoneNumerIp =  phoneNumer.getText().toString();
                                        List<AuthUI.IdpConfig> providers = Arrays.asList(
                                                new AuthUI.IdpConfig.PhoneBuilder().setDefaultNumber("IN", phoneNumerIp).build());
                                        startActivityForResult(
                                                AuthUI.getInstance()
                                                        .createSignInIntentBuilder()
                                                        .setAvailableProviders(providers)
                                                        .build(),
                                                001);
                                    } else {
                                        dialog.dismiss();
                                        Toast.makeText(LoginActivity.this, "Can't login.Supplier data don't match", Toast.LENGTH_LONG).show();
                                    }
                                }
                            } catch (JSONException err) {
                                Log.d("Error", err.toString());
                            }
                        }
                    });
                } else {
                    final String myResponse = response.body().string();
                    LoginActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(LoginActivity.this, "No Supplier registered with this phone number", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        });
    }

}
