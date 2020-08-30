package com.techheralds.mymaligai.customer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {
    CircleImageView userDp;
    String userType;
    EditText tagsEditText, userName;
    Button selectTagBtn;
    LinearLayout supplierLayout;
    Button doneBtn;
    Uri profileUri;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    LinearLayout consumerLayout;
    tagsAdapterList adapterList;
    ArrayList<String> tags, tagsSelected;
    ListView listView;
    TextView selectedTagsText;
    Boolean isTagsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Setup Profile");
        }

        tags = new ArrayList<>();
        tagsSelected = new ArrayList<>();

        userName = findViewById(R.id.userName);
        userDp = findViewById(R.id.userDp);
        doneBtn = findViewById(R.id.doneBtn);
        selectTagBtn = findViewById(R.id.selectTagBtn);
        selectedTagsText = findViewById(R.id.selectedTags);

        consumerLayout = findViewById(R.id.consumerTagsLayout);

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userName.getText().toString().equals("")) {
                    uploadData();
                } else {
                    Toast.makeText(SetupActivity.this, "Enter Name", Toast.LENGTH_SHORT).show();
                }
            }
        });


        selectTagBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(SetupActivity.this);
                bottomSheetDialog.setContentView(R.layout.tags_select_sheet);
                bottomSheetDialog.setCancelable(false);
                if (isTagsLoaded) {
                    bottomSheetDialog.show();
                } else {
                    final ProgressDialog progressDialog = ProgressDialog.show(SetupActivity.this, null, "Please wait...");

                    firebaseDatabase.getReference().child("supplierTags").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            progressDialog.dismiss();
                            isTagsLoaded = true;
                            for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                tags.add(ds.getKey());
                            }
                            bottomSheetDialog.show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                ImageButton doneBtn;
                adapterList = new tagsAdapterList(SetupActivity.this, tags);
                listView = bottomSheetDialog.findViewById(R.id.tagsList);

                listView.setAdapter(adapterList);

                doneBtn = bottomSheetDialog.findViewById(R.id.doneBtn);

                doneBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (tagsSelected.size() > 0) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (String s : tagsSelected) {
                                stringBuilder.append(capitalize(s) + ", ");
                            }
                            selectedTagsText.setText(stringBuilder);
                            bottomSheetDialog.dismiss();
                        } else {
                            bottomSheetDialog.show();
                            Toast.makeText(SetupActivity.this, "Select atleast one category", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        userDp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().setAspectRatio(1, 1).setOutputCompressQuality(50).setRequestedSize(400, 400)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(SetupActivity.this);
            }
        });

    }

    private void uploadData() {
        final ProgressDialog progressDialog = ProgressDialog.show(SetupActivity.this, "", "Setting up Profile.Please wait...");
        final FirebaseUser user = firebaseAuth.getCurrentUser();
        final String name = userName.getText().toString();


        if (profileUri != null) {
            firebaseStorage.getReference().child("Dp/" + user.getUid()).putFile(profileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    firebaseStorage.getReference().child("Dp/" + user.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            ArrayList<String> tempTags = new ArrayList<String>();

                            tempTags = tagsSelected;

                            //Update firebase user profile

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .setPhotoUri(Uri.parse(uri.toString()))
                                    .build();

                            user.updateProfile(profileUpdates);

                            Customer newUser = new Customer(name, uri.toString(), user.getUid(), user.getPhoneNumber(),"");
                            firebaseDatabase.getReference().child("customers/" + user.getUid()).setValue(newUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(SetupActivity.this, "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            }).

                    addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(SetupActivity.this, "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            ArrayList<String> tempTags = new ArrayList<String>();


            //Update firebase user profile
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .setPhotoUri(null)
                    .build();

            user.updateProfile(profileUpdates);


            Customer newUser = new Customer(name, "", user.getUid(), user.getPhoneNumber(),"");
            firebaseDatabase.getReference().child("customers/" + user.getUid()).setValue(newUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(SetupActivity.this, "Error occurred.Please try again", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                profileUri = result.getUri();

                userDp.setImageURI(profileUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public class tagsAdapterList extends BaseAdapter {
        Context context;
        ArrayList<String> tags;

        public tagsAdapterList(Context context, ArrayList<String> tags) {
            this.context = context;
            this.tags = tags;
        }

        @Override
        public int getCount() {
            return tags.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.tags_select_list, parent, false);
            CheckBox checkBox = view.findViewById(R.id.tagCheckBox);
            if (tags.size() > 0) {
                checkBox.setText(capitalize(tags.get(position)));
                if (tagsSelected.indexOf(tags.get(position)) > -1) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }

                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (tagsSelected.indexOf(tags.get(position)) > -1) {
                            tagsSelected.remove(tags.get(position));
                        } else {
                            tagsSelected.add(tags.get(position));
                        }
                    }
                });
            }

            return view;
        }
    }

    private String capitalize(String capString) {
        StringBuffer capBuffer = new StringBuffer();
        Matcher capMatcher = Pattern.compile("([a-z])([a-z]*)", Pattern.CASE_INSENSITIVE).matcher(capString);
        while (capMatcher.find()) {
            capMatcher.appendReplacement(capBuffer, capMatcher.group(1).toUpperCase() + capMatcher.group(2).toLowerCase());
        }

        return capMatcher.appendTail(capBuffer).toString();
    }

}
