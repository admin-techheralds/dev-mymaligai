package com.techheralds.mymaligai.customer.ui.my_profile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.techheralds.mymaligai.customer.R;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class MyProfileFragment extends Fragment {
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    CircleImageView dp;
    TextView name, phoneNumber, deliveryAddress;
    Button editBtn, addAddressBtn;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_my_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        dp = root.findViewById(R.id.profileUserDp);
        name = root.findViewById(R.id.profileUserName);
        phoneNumber = root.findViewById(R.id.profileUserPhoneNumber);
        deliveryAddress = root.findViewById(R.id.profileUserAddress);
        editBtn = root.findViewById(R.id.profileEditBtn);
        addAddressBtn = root.findViewById(R.id.addAddressBtn);

        name.setText(firebaseAuth.getCurrentUser().getDisplayName());

        phoneNumber.setText(firebaseAuth.getCurrentUser().getPhoneNumber());

        Uri photoUrl = firebaseAuth.getCurrentUser().getPhotoUrl();

        if (photoUrl != null) {
            Picasso.with(getContext()).load(photoUrl).into(dp);
        } else {
            dp.setImageResource(R.drawable.nouser);
        }

        //Get delivery address
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("local", Context.MODE_PRIVATE);
        final String address = sharedPreferences.getString("address", "");

        if (address.equals("")) {
            deliveryAddress.setText("No delivery address added");
            addAddressBtn.setVisibility(View.VISIBLE);
        } else {
            deliveryAddress.setText(address);
        }

        addAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manageAddress("Add Address", "Add Address");
            }
        });

        //Edit Profile Dialog
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Edit Profile");
                builder.setItems(new CharSequence[]
                                {"Edit Profile Picture", "Edit Name", "Edit Delivery Address"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        CropImage.activity().setAspectRatio(1, 1).setOutputCompressQuality(50).setRequestedSize(400, 400)
                                                .setGuidelines(CropImageView.Guidelines.ON)
                                                .start(getContext(), MyProfileFragment.this);
                                        break;
                                    case 1:
                                        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
                                        bottomSheetDialog.setContentView(R.layout.edit_name_sheet);

                                        bottomSheetDialog.show();

                                        Button updateBtn = bottomSheetDialog.findViewById(R.id.updateNameBtn);
                                        final EditText updateInput = bottomSheetDialog.findViewById(R.id.updateNameInput);
                                        updateInput.setText(firebaseAuth.getCurrentUser().getDisplayName());
                                        updateBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                if (!updateInput.getText().toString().equals("")) {
                                                    final ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, "Updating Name...");
                                                    firebaseDatabase.getReference().child("customers/" + firebaseAuth.getCurrentUser().getUid() + "/name").setValue(updateInput.getText().toString().trim()).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            //Update firebase user profile

                                                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                                    .setDisplayName(updateInput.getText().toString().trim())
                                                                    .build();

                                                            firebaseAuth.getCurrentUser().updateProfile(profileUpdates);
                                                            progressDialog.dismiss();
                                                            name.setText(updateInput.getText().toString().trim());
                                                            bottomSheetDialog.dismiss();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(getContext(), "Error occurred.Try again!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                } else {
                                                    Toast.makeText(getContext(), "Enter Name", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                        break;

                                    case 2:
                                        manageAddress("Edit Address", "Update Address");
                                        break;
                                }
                            }
                        });
                builder.create().show();
            }
        });


        return root;
    }

    public void manageAddress(String headerText, String btnText) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("local", Context.MODE_PRIVATE);
        final String address = sharedPreferences.getString("address", "");

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.edit_address_sheet);

        TextView header = bottomSheetDialog.findViewById(R.id.header);
        header.setText(headerText);

        Button btn = bottomSheetDialog.findViewById(R.id.addressBtn);
        btn.setText(btnText);

        final EditText addressIp = bottomSheetDialog.findViewById(R.id.addressIp);
        addressIp.setText(address);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!addressIp.getText().toString().trim().equals("")) {
                    final ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, "Please wait...");
                    firebaseDatabase.getReference().child("customers/" + firebaseAuth.getCurrentUser().getUid() + "/address").setValue(addressIp.getText().toString().trim())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //Save delivery address locally
                                    SharedPreferences sharedPreferences = getContext().getSharedPreferences("local", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("address", addressIp.getText().toString().trim());
                                    editor.apply();
                                    deliveryAddress.setText(addressIp.getText().toString().trim());
                                    progressDialog.dismiss();
                                    bottomSheetDialog.dismiss();
                                    addAddressBtn.setVisibility(View.GONE);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Please try again", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "Enter Delivery Address", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bottomSheetDialog.show();
    }

    @Override

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                final FirebaseUser user = firebaseAuth.getCurrentUser();

                final ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, "Uploading Profile Picture...");

                firebaseStorage.getReference().child("Dp/" + user.getUid()).putFile(result.getUri()).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        firebaseStorage.getReference().child("Dp/" + user.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri url) {
                                //Update firebase user profile

                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        //        .setDisplayName(name)
                                        .setPhotoUri(Uri.parse(url.toString()))
                                        .build();

                                user.updateProfile(profileUpdates);
                                Picasso.with(getContext()).load(url).into(dp);
                                firebaseDatabase.getReference().child("customers/").child(user.getUid()).child("dp").setValue(url.toString()).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressDialog.dismiss();

                                    }
                                });

                            }
                        });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

}