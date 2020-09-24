package com.techheralds.mymaligai.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    EditText email, pwd;
    Button loginBtn;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Login");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        email = findViewById(R.id.email);
        pwd = findViewById(R.id.pwd);
        loginBtn = findViewById(R.id.btn);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String emailValue = email.getText().toString();
                final String pwdValue = pwd.getText().toString();

                if (!emailValue.equals("")) {
                    if (!pwdValue.equals("")) {
                        final ProgressDialog progressDialog = ProgressDialog.show(LoginActivity.this, null, "Please wait...");

                        firebaseAuth.signInWithEmailAndPassword(emailValue, pwdValue).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                progressDialog.dismiss();
                                SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("email", emailValue.trim());
                                editor.putString("pwd", pwdValue.trim());
                                editor.apply();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(LoginActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        pwd.requestFocus();
                        Toast.makeText(LoginActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    email.requestFocus();
                    Toast.makeText(LoginActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
