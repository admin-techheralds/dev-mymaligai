package com.techheralds.mymaligai.admin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddSupplierActivity extends AppCompatActivity {
    EditText name, phoneNumber, address, location;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    String verificationId = null;
    Button photoBtn;
    CircleImageView photo;
    Uri photoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_supplier);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Supplier");
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        name = findViewById(R.id.name);
        phoneNumber = findViewById(R.id.phoneNumber);
        address = findViewById(R.id.address);
        location = findViewById(R.id.location);
        photoBtn = findViewById(R.id.photoBtn);
        photo = findViewById(R.id.photo);

        photoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().setAspectRatio(1, 1).setOutputCompressQuality(50).setRequestedSize(400, 400)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(AddSupplierActivity.this);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                photoUri = result.getUri();
                photo.setImageURI(photoUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.addBtn:
                String nameValue = name.getText().toString();
                String phoneValue = phoneNumber.getText().toString();
                String addressValue = address.getText().toString();
                String locationValue = location.getText().toString();

                if (!nameValue.equals("")) {
                    if (!phoneValue.equals("")) {
                        if (!addressValue.equals("")) {
                            if (!locationValue.equals("")) {
                                sendOTP();
                            } else {
                                location.requestFocus();
                                Toast.makeText(this, "Enter Supplier Location", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            address.requestFocus();
                            Toast.makeText(this, "Enter Supplier Address", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        phoneNumber.requestFocus();
                        Toast.makeText(this, "Enter Supplier Phone Number", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    name.requestFocus();
                    Toast.makeText(this, "Enter Supplier Name", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_supplier, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void sendOTP() {
        final String defaultNumber = phoneNumber.getText().toString();
        final ProgressDialog progressDialog = ProgressDialog.show(AddSupplierActivity.this, null, "Sending OTP.Please wait...");
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(AddSupplierActivity.this);
        bottomSheetDialog.setContentView(R.layout.otp_sheet);
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + defaultNumber,
                60,
                TimeUnit.SECONDS,
                this,
                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential, null, bottomSheetDialog);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(AddSupplierActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        verificationId = s;
                        progressDialog.dismiss();

                        Button button;
                        final EditText otp = bottomSheetDialog.findViewById(R.id.otp);
                        TextView otpText = bottomSheetDialog.findViewById(R.id.otpText);
                        otpText.setText("Enter the 6 digit OTP sent to +91" + defaultNumber);

                        button = bottomSheetDialog.findViewById(R.id.otpBtn);

                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                signInWithPhoneAuthCredential(null, otp.getText().toString(), bottomSheetDialog);
                            }
                        });

                        bottomSheetDialog.show();
                    }
                });
    }

    public void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential, String otp, final BottomSheetDialog bottomSheetDialog) {
        PhoneAuthCredential credential;

        if (!otp.equals("")) {
            credential = PhoneAuthProvider.getCredential(verificationId, otp);
            Toast.makeText(this, "Verifying OTP.Please wait...", Toast.LENGTH_SHORT).show();
        } else {
            credential = phoneAuthCredential;
            Toast.makeText(this, "Auto verifying OTP.Please wait...", Toast.LENGTH_SHORT).show();
        }
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            setData();
                            bottomSheetDialog.dismiss();
                        } else {
                            Toast.makeText(AddSupplierActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void setData() {

        final String nameValue = name.getText().toString();
        final String addressValue = address.getText().toString();
        final String locationValue = location.getText().toString();

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final ProgressDialog progressDialog = ProgressDialog.show(AddSupplierActivity.this, null, "Please wait...");
        final String smsTemplate = "Greetings from " + nameValue + ".We deliver your necessary product at your door step";

        if (photoUri != null) {
            firebaseDatabase.getReference().child("suppliers/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getChildrenCount() > 0) {
                        progressDialog.dismiss();
                        Toast.makeText(AddSupplierActivity.this, "Phone Number already registered", Toast.LENGTH_SHORT).show();
                    } else {


                        firebaseStorage.getReference().child("Dp/" + user.getUid()).putFile(photoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                firebaseStorage.getReference().child("Dp/" + user.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(final Uri uri) {

                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setPhotoUri(Uri.parse(uri.toString()))
                                                .build();
                                        user.updateProfile(profileUpdates);

                                        Supplier supplier = new Supplier(nameValue, user.getUid(), user.getPhoneNumber(), addressValue, locationValue, true, user.getMetadata().getCreationTimestamp(), uri.toString(), smsTemplate, null);
                                        firebaseDatabase.getReference().child("suppliers/" + user.getUid()).setValue(supplier).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(AddSupplierActivity.this, "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Intent intent = new Intent(AddSupplierActivity.this, MainActivity.class);
                                                startActivity(intent);
                                            }
                                        });
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(AddSupplierActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } else {
            firebaseDatabase.getReference().child("suppliers/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getChildrenCount() > 0) {
                        progressDialog.dismiss();
                        Toast.makeText(AddSupplierActivity.this, "Phone Number already registered", Toast.LENGTH_SHORT).show();
                    } else {
                        Supplier supplier = new Supplier(nameValue, user.getUid(), user.getPhoneNumber(), addressValue, locationValue, true, user.getMetadata().getCreationTimestamp(), "", smsTemplate, null);
                        firebaseDatabase.getReference().child("suppliers/" + user.getUid()).setValue(supplier).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(AddSupplierActivity.this, "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                progressDialog.dismiss();
                                Intent intent = new Intent(AddSupplierActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }
}