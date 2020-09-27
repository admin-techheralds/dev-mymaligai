package com.techheralds.mymaligai.supplier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.internal.Util;


public class AddItemsActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseStorage firebaseStorage;
    FirebaseUser firebaseUser;
    ListView listView;
    inventoryAdapterList adapterList;
    ArrayList<inventory> inventoryItems;
    Spinner selectTagSpinner;
    ArrayList<Map<String, Object>> tags;
    ArrayList<String> tempTags; //for spinner
    ArrayList<String> quantityTypeArray;
    String currTag = null;
    String currId = null;
    Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_items);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Inventory");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        quantityTypeArray = new ArrayList<>();
        quantityTypeArray.add("250g, 500g, 1kg, 2kg, 5kg, 10kg");
        quantityTypeArray.add("5g, 10g, 25g, 50g, 100g, 150g");
        quantityTypeArray.add("200ml, 250ml, 500ml, 1L, 2L");
        quantityTypeArray.add("10ml, 25ml, 50ml, 100ml, 150ml");
        quantityTypeArray.add("1pc, 2pcs, 5pcs,10pcs, 20pcs");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        inventoryItems = new ArrayList<>();
        tags = new ArrayList<>();
        tempTags = new ArrayList<>();
        selectTagSpinner = findViewById(R.id.selectTagBtn);

        listView = findViewById(R.id.inventoryListView);
        final ProgressDialog progressDialog = ProgressDialog.show(AddItemsActivity.this, null, "Please Wait...");
        tags.clear();
        //Load preferred tags
        firebaseDatabase.getReference().child("suppliers/" + firebaseUser.getUid() + "/tags").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("category", ds.getValue());
                        data.put("id", ds.getKey());
                        tags.add(data);
                        tempTags.add(capitalize(ds.getValue().toString()));

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(), R.layout.support_simple_spinner_dropdown_item, tempTags);
                        selectTagSpinner.setAdapter(adapter);

                        selectTagSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                loadItemsWithTag(tags.get(position).get("id").toString(), tags.get(position).get("category").toString());
                                currTag = tags.get(position).get("category").toString();
                                currId = tags.get(position).get("id").toString();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        progressDialog.dismiss();
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(AddItemsActivity.this, "You haven't added any categories", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddItemsActivity.this, "Can't Load Data", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void loadItemsWithTag(String id, String category) {
        final ProgressDialog progressDialog = ProgressDialog.show(AddItemsActivity.this, null, "Loading items with the category '" + capitalize(category) + "'.Please wait...");

        firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                inventoryItems.clear();
                adapterList = new inventoryAdapterList(AddItemsActivity.this, inventoryItems);
                listView.setAdapter(adapterList);
                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        progressDialog.dismiss();
                        inventory data = ds.getValue(inventory.class);
                        inventoryItems.add(data);
                        adapterList = new inventoryAdapterList(AddItemsActivity.this, inventoryItems);
                        listView.setAdapter(adapterList);
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(AddItemsActivity.this, "No inventory items in this category", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
                Toast.makeText(AddItemsActivity.this, "Try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_items, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.addItemsBtn:
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(AddItemsActivity.this);
                bottomSheetDialog.setContentView(R.layout.add_new_item_sheet);

                bottomSheetDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        imageUri = null;
                    }
                });

                Spinner quantityTypeSpinner;
                Button qunatityTypeBtn, addBtn;
                final int[] currQuantityType = new int[1];
                final EditText itemName, itemPrice, attr1, attr2, attr3;
                Button addImageBtn;
                TextView header;
                final String itemImg = "";
                final ImageView imageView = bottomSheetDialog.findViewById(R.id.previewImg);
                itemName = bottomSheetDialog.findViewById(R.id.itemNameIp);
                addImageBtn = bottomSheetDialog.findViewById(R.id.addImageBtn);
                attr1 = bottomSheetDialog.findViewById(R.id.attr1);
                attr2 = bottomSheetDialog.findViewById(R.id.attr2);
                attr3 = bottomSheetDialog.findViewById(R.id.attr3);

                //Upload image
                addImageBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CropImage.activity().setAspectRatio(4, 3).setRequestedSize(400, 200)
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .start(AddItemsActivity.this);
                    }
                });
                final int minutes = 1000;
                final Handler handler = new Handler();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageURI(imageUri);
                        handler.postDelayed(this, minutes);

                    }
                }, minutes);

                itemPrice = bottomSheetDialog.findViewById(R.id.itemPriceIp);
                itemPrice.setHint("Item Price for 1kg");
                header = bottomSheetDialog.findViewById(R.id.header);
                header.setText("Add Items To " + capitalize(currTag));

                addBtn = bottomSheetDialog.findViewById(R.id.addBtn);


                addBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!itemName.getText().toString().equals("")) {
                            if (imageUri != null) {
                                if (!itemPrice.getText().toString().equals("")) {
                                    if (findIndexOf(itemName.getText().toString()) == -1) {

                                        final String iName = itemName.getText().toString().trim();
                                        final int iQuantityType = currQuantityType[0];
                                        final int active = 1;
                                        String tempName;
                                        if (iName.length() < 3) {
                                            tempName = String.format("%0" + (3 - iName.length()) + "d%s", 0, iName);
                                        } else {
                                            tempName = iName.substring(0, 3);
                                        }

                                        String iA1 = attr1.getText().toString().trim();
                                        String iA2 = attr2.getText().toString().trim();
                                        String iA3 = attr3.getText().toString().trim();

                                        final String bulk_import_id = "";
                                        final long created_at = System.currentTimeMillis();
                                        final String created_mode = "APP";
                                        final int in_stock = 1;
                                        String sA1, sA2, sA3;

                                        if (iA1.length() < 3) {
                                            sA1 = String.format("%0" + (3 - iA1.length()) + "d%s", 0, iA1);
                                        } else {
                                            sA1 = iA1.substring(0, 3);
                                        }
                                        if (iA2.length() < 3) {
                                            sA2 = String.format("%0" + (3 - iA2.length()) + "d%s", 0, iA2);
                                        } else {
                                            sA2 = iA2.substring(0, 3);
                                        }
                                        if (iA3.length() < 3) {
                                            sA3 = String.format("%0" + (3 - iA3.length()) + "d%s", 0, iA3);
                                        } else {
                                            sA3 = iA3.substring(0, 3);
                                        }

                                        SecureRandom random = new SecureRandom();
                                        int num = random.nextInt(1000000);
                                        String randomNum = String.format("%06d", num);

                                        SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                                        long supplier_id = sharedPreferences.getLong("supplier_id", 0);

                                        final String sku = "sku-" + randomNum + "-" + supplier_id + "-" + currTag.substring(0, 3) + "-" + tempName + "-" + sA1 + "-" + sA2 + "-" + sA3;
                                        final String finalIA = iA1;
                                        final String finalIA1 = iA2;
                                        final String finalIA2 = iA3;
                                        final Float iPrice = Float.valueOf(itemPrice.getText().toString().trim());

                                        final ProgressDialog progressDialog = ProgressDialog.show(AddItemsActivity.this, null, "Uploading Image...");
                                        firebaseStorage.getReference().child("images/" + firebaseUser.getUid() + "/" + currId + "/" + iName).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                firebaseStorage.getReference().child("images/" + firebaseUser.getUid() + "/" + currId + "/" + iName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                    @Override
                                                    public void onSuccess(Uri uri) {
                                                        final inventory data = new inventory(iName, iQuantityType, uri.toString(), iPrice, active, finalIA, finalIA1, finalIA2, bulk_import_id, created_at, created_mode, in_stock, sku);
                                                        firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + currId + "/" + sku).setValue(data).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(AddItemsActivity.this, "Can't add.Please try again", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                progressDialog.dismiss();
                                                                if (findIndexOf(itemName.getText().toString().trim()) > -1) {
                                                                    inventoryItems.set(findIndexOf(itemName.getText().toString().trim()), data);
                                                                } else {
                                                                    inventoryItems.add(data);
                                                                }
                                                                itemName.setText("");
                                                                imageUri = null;
                                                                itemPrice.setText("");
                                                                attr1.setText("");
                                                                attr2.setText("");
                                                                attr3.setText("");
                                                                adapterList = new inventoryAdapterList(AddItemsActivity.this, inventoryItems);
                                                                listView.setAdapter(adapterList);
                                                                Toast.makeText(AddItemsActivity.this, "Item added", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(AddItemsActivity.this, "Can't upload image.Please try again", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        Toast.makeText(AddItemsActivity.this, "Item already present", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(AddItemsActivity.this, "Enter Item Price", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(AddItemsActivity.this, "Upload Item Image", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(AddItemsActivity.this, "Enter Item Name", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddItemsActivity.this, R.layout.support_simple_spinner_dropdown_item, quantityTypeArray);
                quantityTypeSpinner = bottomSheetDialog.findViewById(R.id.spinner);
                quantityTypeSpinner.setAdapter(adapter);

                quantityTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        currQuantityType[0] = position;
                        if (position == 0) {
                            itemPrice.setHint("Item Price for 1kg");
                        }
                        if (position == 1) {
                            itemPrice.setHint("Item Price for 100g");
                        }
                        if (position == 2) {
                            itemPrice.setHint("Item Price for 1L");
                        }
                        if (position == 3) {
                            itemPrice.setHint("Item Price for 100ml");
                        }
                        if (position == 4) {
                            itemPrice.setHint("Item Price for 1pc");
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });


                bottomSheetDialog.show();
                break;

        }
        return super.

                onOptionsItemSelected(item);
    }

    public int findIndexOf(String itemName) {
        int index = -1;
        for (int i = 0; i < inventoryItems.size(); i++) {
            if (inventoryItems.get(i).getName().trim().toLowerCase().equals(itemName.toLowerCase())) {
                index = i;
            }
        }

        return index;
    }


    public class inventoryAdapterList extends BaseAdapter {
        Context context;
        ArrayList<inventory> inventories;

        public inventoryAdapterList(Context context, ArrayList<inventory> inventories) {
            this.context = context;
            this.inventories = inventories;
        }

        @Override
        public int getCount() {
            return inventories.size();
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
            view = LayoutInflater.from(context).inflate(R.layout.demand_item_list, parent, false);

            if (inventories.size() > 0) {
                TextView title = view.findViewById(R.id.itemName);
                TextView subTitle = view.findViewById(R.id.itemQuantity);
                ImageView image = view.findViewById(R.id.itemImg);

                title.setText(capitalize(inventories.get(position).getName()));

                int quantityType = inventories.get(position).getQuantity_type();
                String itemUnitType = "";
                if (quantityType == 0) {
                    itemUnitType = "1kg";
                }
                if (quantityType == 1) {
                    itemUnitType = "100g";
                }
                if (quantityType == 2) {
                    itemUnitType = "1L";
                }
                if (quantityType == 3) {
                    itemUnitType = "100ml";
                }
                if (quantityType == 4) {
                    itemUnitType = "1pc";
                }
                subTitle.setText("Rs." + inventories.get(position).getPrice() + "(" + itemUnitType + ")");

                if (inventories.get(position).getImg() != null) {
                    if (!inventories.get(position).getImg().equals("")) {
                        Picasso.with(AddItemsActivity.this).load(inventories.get(position).getImg()).into(image);
                    }
                }

                final String finalItemUnitType = itemUnitType;
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(AddItemsActivity.this).setTitle(inventories.get(position).getName())
                                .setMessage("Rs." + inventories.get(position).getPrice() + "(" + finalItemUnitType + ")")
                                .setNegativeButton("Edit Item", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(AddItemsActivity.this);
                                        bottomSheetDialog.setContentView(R.layout.add_new_item_sheet);

                                        bottomSheetDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                imageUri = null;
                                            }
                                        });

                                        Spinner quantityTypeSpinner;
                                        Button qunatityTypeBtn, editBtn;
                                        final EditText itemName, itemPrice, attr1, attr2, attr3;
                                        ;
                                        TextView header;
                                        final int[] currQuantityType = new int[1];
                                        Button addImageBtn;
                                        final String itemImg = "";
                                        attr1 = bottomSheetDialog.findViewById(R.id.attr1);
                                        attr2 = bottomSheetDialog.findViewById(R.id.attr2);
                                        attr3 = bottomSheetDialog.findViewById(R.id.attr3);
                                        header = bottomSheetDialog.findViewById(R.id.header);
                                        itemName = bottomSheetDialog.findViewById(R.id.itemNameIp);
                                        addImageBtn = bottomSheetDialog.findViewById(R.id.addImageBtn);
                                        final ImageView imageView = bottomSheetDialog.findViewById(R.id.previewImg);

                                        //Upload image
                                        addImageBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CropImage.activity().setAspectRatio(4, 3).setRequestedSize(400, 200)
                                                        .setGuidelines(CropImageView.Guidelines.ON)
                                                        .start(AddItemsActivity.this);
                                            }
                                        });
                                        final int minutes = 1000;
                                        final Handler handler = new Handler();

                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (inventories.size() > 0) {
                                                    if (imageUri != null) {
                                                        imageView.setImageURI(imageUri);
                                                    } else {
                                                        if (inventories.get(position) != null) {
                                                            Picasso.with(AddItemsActivity.this).load(inventoryItems.get(position).getImg()).into(imageView);
                                                        }
                                                    }
                                                    handler.postDelayed(this, minutes);
                                                }
                                            }
                                        }, minutes);
                                        itemPrice = bottomSheetDialog.findViewById(R.id.itemPriceIp);
                                        itemPrice.setHint("Item Price for 1kg");
                                        editBtn = bottomSheetDialog.findViewById(R.id.addBtn);


                                        editBtn.setText("Update Item");
                                        editBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                final String iName = itemName.getText().toString().trim();
                                                final Float iPrice = Float.valueOf(itemPrice.getText().toString().trim());
                                                final int iQuantityType = currQuantityType[0];

                                                if (!itemName.getText().toString().equals("")) {
                                                    if (imageUri != null) {
                                                        if (!itemPrice.getText().toString().equals("")) {
                                                            if (!inventories.get(position).getName().toLowerCase().equals(itemName.getText().toString().toLowerCase())) {
                                                                if (findIndexOf(itemName.getText().toString()) != -1) {
                                                                    Toast.makeText(AddItemsActivity.this, "Item already present", Toast.LENGTH_SHORT).show();
                                                                    return;
                                                                }
                                                                firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + currId + "/" + inventories.get(position).getSku()).removeValue();
                                                            }

                                                            final ProgressDialog progressDialog = ProgressDialog.show(AddItemsActivity.this, null, "Uploading Image...");
                                                            firebaseStorage.getReference().child("images/" + firebaseUser.getUid() + "/" + currId + "/" + iName).putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                @Override
                                                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                                    firebaseStorage.getReference().child("images/" + firebaseUser.getUid() + "/" + currId + "/" + iName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                        @Override
                                                                        public void onSuccess(final Uri uri) {

                                                                            Map<String, Object> data = new HashMap<>();
                                                                            data.put("quantity_type", iQuantityType);
                                                                            data.put("price", iPrice);
                                                                            data.put("img", uri.toString());
                                                                            firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + currId + "/" + inventoryItems.get(position).getSku()).updateChildren(data).addOnFailureListener(new OnFailureListener() {
                                                                                @Override
                                                                                public void onFailure(@NonNull Exception e) {
                                                                                    Toast.makeText(AddItemsActivity.this, "Can't add.Please try again", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {
                                                                                    progressDialog.dismiss();
                                                                                    inventories.get(position).setName(itemName.getText().toString().trim());
                                                                                    inventories.get(position).setQuantity_type(currQuantityType[0]);
                                                                                    inventories.get(position).setImg(uri.toString());
                                                                                    inventories.get(position).setPrice(Float.valueOf(itemPrice.getText().toString()));

                                                                                    itemName.setText("");
                                                                                    imageUri = null;
                                                                                    itemPrice.setText("");
                                                                                    ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
                                                                                    Toast.makeText(AddItemsActivity.this, "Item Updated", Toast.LENGTH_SHORT).show();
                                                                                    bottomSheetDialog.dismiss();
                                                                                }
                                                                            });
                                                                        }
                                                                    });
                                                                }
                                                            }).addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull Exception e) {
                                                                    progressDialog.dismiss();
                                                                    Toast.makeText(AddItemsActivity.this, "Can't upload image.Please try again", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        } else {
                                                            Toast.makeText(AddItemsActivity.this, "Enter Item Price", Toast.LENGTH_SHORT).show();
                                                        }
                                                    } else {
                                                        if (!inventories.get(position).getName().toLowerCase().equals(itemName.getText().toString().toLowerCase())) {
                                                            if (findIndexOf(itemName.getText().toString()) != -1) {
                                                                Toast.makeText(AddItemsActivity.this, "Item already present", Toast.LENGTH_SHORT).show();
                                                                return;
                                                            }
                                                            firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + currId + "/" + inventories.get(position).getName().toLowerCase()).removeValue();
                                                        }

                                                        Map<String, Object> data = new HashMap<>();
                                                        data.put("quantity_type", iQuantityType);
                                                        data.put("price", iPrice);

                                                        firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + currId + "/" + inventoryItems.get(position).getSku()).updateChildren(data).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(AddItemsActivity.this, "Can't add.Please try again", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                inventories.get(position).setName(itemName.getText().toString().trim());
                                                                inventories.get(position).setQuantity_type(currQuantityType[0]);
                                                                inventories.get(position).setImg(inventoryItems.get(position).getImg());
                                                                inventories.get(position).setPrice(Float.valueOf(itemPrice.getText().toString()));

                                                                itemName.setText("");
                                                                imageUri = null;
                                                                itemPrice.setText("");
                                                                ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
                                                                Toast.makeText(AddItemsActivity.this, "Item Updated", Toast.LENGTH_SHORT).show();
                                                                bottomSheetDialog.dismiss();
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    Toast.makeText(AddItemsActivity.this, "Enter Item Name", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });


                                        itemName.setText(inventories.get(position).getName());
                                        itemName.setEnabled(false);
                                        //itemImg.setText(inventories.get(position).getImg());
                                        itemPrice.setText(String.valueOf(inventories.get(position).getPrice()));
                                        header.setText("Edit Item");
                                        attr1.setText(inventories.get(position).getAttr1());
                                        attr1.setEnabled(false);
                                        attr2.setText(inventories.get(position).getAttr2());
                                        attr2.setEnabled(false);
                                        attr3.setText(inventories.get(position).getAttr3());
                                        attr3.setEnabled(false);

                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(AddItemsActivity.this, R.layout.support_simple_spinner_dropdown_item, quantityTypeArray);
                                        quantityTypeSpinner = bottomSheetDialog.findViewById(R.id.spinner);
                                        quantityTypeSpinner.setAdapter(adapter);

                                        quantityTypeSpinner.setSelection(inventories.get(position).getQuantity_type());

                                        quantityTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                            @Override
                                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                currQuantityType[0] = position;
                                                if (position == 0) {
                                                    itemPrice.setHint("Item Price for 1kg");
                                                }
                                                if (position == 1) {
                                                    itemPrice.setHint("Item Price for 100g");
                                                }
                                                if (position == 2) {
                                                    itemPrice.setHint("Item Price for 1L");
                                                }
                                                if (position == 3) {
                                                    itemPrice.setHint("Item Price for 100ml");
                                                }
                                                if (position == 4) {
                                                    itemPrice.setHint("Item Price for 1pc");
                                                }
                                            }

                                            @Override
                                            public void onNothingSelected(AdapterView<?> parent) {

                                            }
                                        });


                                        bottomSheetDialog.show();
                                    }
                                }).setPositiveButton("Delete Item", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new AlertDialog.Builder(AddItemsActivity.this).setTitle("Delete Item")
                                        .setMessage("Are you sure?").setNegativeButton("Cancel", null).
                                        setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                firebaseDatabase.getReference().child("inventory/" + firebaseUser.getUid() + "/" + currId + "/" + inventories.get(position).getSku()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Toast.makeText(AddItemsActivity.this, "Item deleted", Toast.LENGTH_SHORT).show();
                                                        inventoryItems.remove(inventories.get(position));
                                                        ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(AddItemsActivity.this, "Can't delete.Try again", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }).show();
                            }
                        }).show();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imageUri = result.getUri();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
