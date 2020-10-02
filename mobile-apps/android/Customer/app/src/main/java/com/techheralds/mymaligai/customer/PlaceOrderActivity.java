package com.techheralds.mymaligai.customer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.NumberFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;

public class PlaceOrderActivity extends AppCompatActivity {
    String supplierUid, supplierName, supplierPhoneNumber, supplierDp;
    TextView nameText;
    CircleImageView dpImageView;
    EditText deleveryTImeBtn;
    Spinner spinner;
    ArrayList<Map<String, Object>> tags;
    ArrayList<String> tempTags;//for spinner
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseUser firebaseUser;
    String currTag = "";
    ArrayList<inventory> inventoryItems, tempArray;
    inventoryAdapterList inventoryAdapterList;
    ListView inventoryItemListView;
    String currTime;
    int mYear;
    int mMonth;
    int mDay;
    int mHour;
    int mMinute;
    String startDate, endDate;
    Calendar c;
    Context ctx = this;
    ArrayList<String> unitsArr[];
    ArrayList<Double> multiplyFactorArr[];
    ArrayList<Map<String, Object>> demandsArr, tempDemandsArr;
    ArrayList<Integer> spinnerPoitions;
    TextView textCartItemCount;
    int mCartItemCount = 0;
    long hoursDiff = 0;
    double totalAmount = 0.0;
    itemsAdapterList itemsAdapterList;
    SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Place Order");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        spinnerPoitions = new ArrayList<>();
        unitsArr = new ArrayList[5];
        multiplyFactorArr = new ArrayList[5];
        // initializing
        for (int i = 0; i < 5; i++) {
            unitsArr[i] = new ArrayList<>();
        }
        for (int i = 0; i < 5; i++) {
            multiplyFactorArr[i] = new ArrayList<>();
        }

        // Big in grams
        unitsArr[0].add("250g");
        unitsArr[0].add("500g");
        unitsArr[0].add("1kg");
        unitsArr[0].add("2kg");
        unitsArr[0].add("5kg");
        unitsArr[0].add("10kg");
        // Low in grams
        unitsArr[1].add("5g");
        unitsArr[1].add("10g");
        unitsArr[1].add("25g");
        unitsArr[1].add("50g");
        unitsArr[1].add("100g");
        unitsArr[1].add("150g");
        // Big in liters
        unitsArr[2].add("200ml");
        unitsArr[2].add("250ml");
        unitsArr[2].add("500ml");
        unitsArr[2].add("1L");
        unitsArr[2].add("2L");
        // Low in liters
        unitsArr[3].add("10ml");
        unitsArr[3].add("25ml");
        unitsArr[3].add("50ml");
        unitsArr[3].add("100ml");
        unitsArr[3].add("150ml");
        //Counts
        unitsArr[4].add("1pc");
        unitsArr[4].add("2pcs");
        unitsArr[4].add("5pcs");
        unitsArr[4].add("10pcs");
        unitsArr[4].add("20pcs");

        // multuply factor grams
        multiplyFactorArr[0].add(0.25);
        multiplyFactorArr[0].add(0.5);
        multiplyFactorArr[0].add(1.0);
        multiplyFactorArr[0].add(2.0);
        multiplyFactorArr[0].add(5.0);
        multiplyFactorArr[0].add(10.0);
        //multiply factor small grams
        multiplyFactorArr[1].add(0.05);
        multiplyFactorArr[1].add(0.1);
        multiplyFactorArr[1].add(0.25);
        multiplyFactorArr[1].add(0.5);
        multiplyFactorArr[1].add(1.0);
        multiplyFactorArr[1].add(1.5);
        // multiply factor liters
        multiplyFactorArr[2].add(0.2);
        multiplyFactorArr[2].add(0.25);
        multiplyFactorArr[2].add(0.5);
        multiplyFactorArr[2].add(1.0);
        multiplyFactorArr[2].add(2.0);
        // multiply factor low liters
        multiplyFactorArr[3].add(0.1);
        multiplyFactorArr[3].add(0.25);
        multiplyFactorArr[3].add(0.5);
        multiplyFactorArr[3].add(1.0);
        multiplyFactorArr[3].add(1.5);
        //multiplyfactor counts
        multiplyFactorArr[4].add(1.0);
        multiplyFactorArr[4].add(2.0);
        multiplyFactorArr[4].add(5.0);
        multiplyFactorArr[4].add(10.0);
        multiplyFactorArr[4].add(20.0);

        tags = new ArrayList<>();
        inventoryItems = new ArrayList<>();
        tempArray = new ArrayList<>();

        inventoryItemListView = findViewById(R.id.inventoryItemsList);

       /* searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onQueryTextChange(final String newText) {
                inventoryAdapterList = new inventoryAdapterList(PlaceOrderActivity.this, tempArray);
                inventoryItemListView.setAdapter(inventoryAdapterList);
                inventoryAdapterList.getFilter().filter(newText);

                return false;
            }
        });*/

        tempTags = new ArrayList<>();

        demandsArr = new ArrayList<>();
        tempDemandsArr = new ArrayList<>();

        mYear = Calendar.getInstance().get(Calendar.YEAR);
        mMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        mDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        mHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        mMinute = Calendar.getInstance().get(Calendar.MINUTE);

        String AM_PM;
        if (mHour < 12) {
            AM_PM = "AM";
        } else {
            if ((mHour - 12) > 0) {
                mHour = mHour - 12;
            }
            AM_PM = "PM";
        }

        if (mMinute < 10) {
            currTime = mDay + "/" + mMonth + "/" + mYear + ", " + mHour + ":0" + mMinute + " " + AM_PM;
        } else {
            currTime = mDay + "/" + mMonth + "/" + mYear + ", " + mHour + ":" + mMinute + " " + AM_PM;
        }


        startDate = mYear + "-" + mMonth + "-" + mDay + " @ " + mHour + ":" + mMinute + " " + AM_PM;

        supplierUid = getIntent().getExtras().getString("uid");
        supplierName = getIntent().getExtras().getString("name");
        supplierPhoneNumber = getIntent().getExtras().getString("phoneNumber");
        supplierDp = getIntent().getExtras().getString("dp");

        dpImageView = findViewById(R.id.supplierDp);
        spinner = findViewById(R.id.spinner);
        nameText = findViewById(R.id.supplierName);

        deleveryTImeBtn = findViewById(R.id.deleveryTImeBtn);

        if (supplierDp.equals("")) {
            dpImageView.setImageResource(R.drawable.nouser);
        } else {
            Picasso.with(getApplicationContext()).load(supplierDp).into(dpImageView);
        }
        //get deleviry time
        deleveryTImeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_Datepicker();
            }
        });
        nameText.setText(supplierName);

        final ProgressDialog progressDialog = ProgressDialog.show(PlaceOrderActivity.this, null, "Please Wait...");
        tags.clear();

        //Load preferred category
        firebaseDatabase.getReference().child("suppliers/" + supplierUid + "/tags").addListenerForSingleValueEvent(new ValueEventListener() {
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
                        spinner.setAdapter(adapter);

                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                loadItemsWithTag(tags.get(position).get("id").toString(), tags.get(position).get("category").toString());
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        progressDialog.dismiss();
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(PlaceOrderActivity.this, "This supplier haven't added any categories", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PlaceOrderActivity.this, "Can't Load Data", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.place_order, menu);
        final MenuItem menuItem = menu.findItem(R.id.action_cart);

        View actionView = menuItem.getActionView();
        textCartItemCount = (TextView) actionView.findViewById(R.id.cart_badge);

        setupBadge();

        actionView.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(menuItem);
            }
        });

        return true;
    }

    public void loadItemsWithTag(String id, String category) {
        final ProgressDialog progressDialog = ProgressDialog.show(PlaceOrderActivity.this, null, "Loading items with the category '" + capitalize(category) + "'.Please wait...");

        firebaseDatabase.getReference().child("inventory/" + supplierUid + "/" + id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                inventoryItems.clear();
                inventoryAdapterList = new inventoryAdapterList(PlaceOrderActivity.this, inventoryItems);
                inventoryItemListView.setAdapter(inventoryAdapterList);
                if (dataSnapshot.getChildrenCount() > 0) {
                    for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                        spinnerPoitions.add(0);
                    }
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        progressDialog.dismiss();
                        inventory data = ds.getValue(inventory.class);
                        tempArray.add(data);
                        inventoryItems.add(data);
                        inventoryAdapterList = new inventoryAdapterList(PlaceOrderActivity.this, inventoryItems);
                        inventoryItemListView.setAdapter(inventoryAdapterList);
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(PlaceOrderActivity.this, "No inventory items in this category", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
                Toast.makeText(PlaceOrderActivity.this, "Try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.placeDemandBtn:
                final String dTime = deleveryTImeBtn.getText().toString();

                if (demandsArr.size() > 0) {
                    if (dTime.length() > 0) {
                        if (hoursDiff >= 6) {
                            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                            double price = totalAmount;
                            String moneyString = formatter.format(price);

                            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(PlaceOrderActivity.this);
                            bottomSheetDialog.setContentView(R.layout.place_order_sheet);

                            final Button btn, addBtn;
                            btn = bottomSheetDialog.findViewById(R.id.placeOrderBtn);
                            addBtn = bottomSheetDialog.findViewById(R.id.addBtn);

                            final TextView totalItems, totalMrp, address;
                            totalItems = bottomSheetDialog.findViewById(R.id.totalItems);
                            totalMrp = bottomSheetDialog.findViewById(R.id.totalMrp);
                            address = bottomSheetDialog.findViewById(R.id.address);

                            addBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //Add New Delivery Address

                                    final BottomSheetDialog bottomSheetDialog1 = new BottomSheetDialog(PlaceOrderActivity.this);
                                    bottomSheetDialog1.setContentView(R.layout.edit_address_sheet);

                                    TextView header = bottomSheetDialog1.findViewById(R.id.header);
                                    header.setText("Add Address");

                                    Button btn1 = bottomSheetDialog1.findViewById(R.id.addressBtn);
                                    btn1.setText("Add Address");

                                    final EditText addressIp = bottomSheetDialog1.findViewById(R.id.addressIp);

                                    btn1.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if (!addressIp.getText().toString().trim().equals("")) {
                                                final ProgressDialog progressDialog = ProgressDialog.show(PlaceOrderActivity.this, null, "Please wait...");
                                                firebaseDatabase.getReference().child("customers/" + firebaseAuth.getCurrentUser().getUid() + "/address").setValue(addressIp.getText().toString().trim())
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                //Save delivery address locally
                                                                SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                                editor.putString("address", addressIp.getText().toString().trim());
                                                                editor.apply();
                                                                address.setText(addressIp.getText().toString().trim());
                                                                progressDialog.dismiss();
                                                                bottomSheetDialog1.dismiss();
                                                                addBtn.setVisibility(View.GONE);
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(PlaceOrderActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                Toast.makeText(PlaceOrderActivity.this, "Enter Delivery Address", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                                    bottomSheetDialog1.show();
                                }
                            });


                            totalItems.setText(demandsArr.size() == 1 ? demandsArr.size() + " Item" : demandsArr.size() + " Items");
                            totalMrp.setText(moneyString);

                            //Get delivery address
                            SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                            final String deliveryAddress = sharedPreferences.getString("address", "");

                            if (deliveryAddress.equals("")) {
                                address.setText("No delivery address added");
                                addBtn.setVisibility(View.VISIBLE);
                            } else {
                                address.setText(deliveryAddress);
                            }

                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //Get delivery address
                                    SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
                                    final String deliveryAddress = sharedPreferences.getString("address", "");

                                    if (!deliveryAddress.equals("")) {
                                        final ProgressDialog progressDialog = ProgressDialog.show(PlaceOrderActivity.this, null, "Placing Order.Please Wait...");
                                        final String key = firebaseDatabase.getReference().child("demands").push().getKey();
                                        demand newDemand = new demand(firebaseUser.getUid(), supplierUid, dTime, "placed", currTime, demandsArr, key,deliveryAddress, totalAmount,"cod");
                                        firebaseDatabase.getReference().child("demands/" + key).setValue(newDemand).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Intent intent = new Intent(PlaceOrderActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(ctx, "Error occurred.Try again", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        Toast.makeText(PlaceOrderActivity.this, "Add Delivery Address", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            bottomSheetDialog.show();

                        } else {
                            Toast.makeText(ctx, "Delivery Time should be 6 hours from now", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ctx, "Select delivery time", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(PlaceOrderActivity.this, "Add atleast one item you need", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.action_cart:
                if (mCartItemCount > 0) {
                    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(PlaceOrderActivity.this);
                    bottomSheetDialog.setContentView(R.layout.demand_item_sheet);
                    TextView priceText = bottomSheetDialog.findViewById(R.id.totalPrice);
                    TextView header = bottomSheetDialog.findViewById(R.id.header);
                    header.setText("Items in Cart");

                    NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                    double price = totalAmount;
                    String moneyString = formatter.format(price);
                    priceText.setText("Total MRP: " + moneyString);
                    ListView listView1 = bottomSheetDialog.findViewById(R.id.itemsListView);
                    itemsAdapterList = new itemsAdapterList(PlaceOrderActivity.this, demandsArr);
                    listView1.setAdapter(itemsAdapterList);
                    bottomSheetDialog.show();

                } else {
                    Toast.makeText(ctx, "No items added to cart", Toast.LENGTH_SHORT).show();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupBadge() {

        if (textCartItemCount != null) {
            if (mCartItemCount == 0) {
                if (textCartItemCount.getVisibility() != View.GONE) {
                    textCartItemCount.setVisibility(View.GONE);
                }
            } else {
                textCartItemCount.setText(String.valueOf(Math.min(mCartItemCount, 99)));
                if (textCartItemCount.getVisibility() != View.VISIBLE) {
                    textCartItemCount.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public static Date parseDate(String strDate) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Date date1 = null;
        try {
            date1 = dateFormat.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date1;
    }


    private void show_Datepicker() {
        c = Calendar.getInstance();
        int mYearParam = mYear;
        int mMonthParam = mMonth - 1;
        int mDayParam = mDay;

        DatePickerDialog datePickerDialog = new DatePickerDialog(ctx,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mMonth = monthOfYear + 1;
                        mYear = year;
                        mDay = dayOfMonth;
                        show_Timepicker();

                    }
                }, mYearParam, mMonthParam, mDayParam);
        datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
        datePickerDialog.show();
    }

    private void show_Timepicker() {

        TimePickerDialog timePickerDialog = new TimePickerDialog(ctx,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int pHour,
                                          int pMinute) {

                        mHour = pHour;
                        mMinute = pMinute;
                        String AM_PM;
                        if (mHour < 12) {
                            AM_PM = "AM";
                        } else {
                            if ((mHour - 12) > 0) {
                                mHour = mHour - 12;
                            }
                            AM_PM = "PM";
                        }
                        if (mMinute < 10) {
                            deleveryTImeBtn.setText(mDay + "/" + mMonth + "/" + mYear + ", " + mHour + ":0" + mMinute + " " + AM_PM);
                        } else {
                            deleveryTImeBtn.setText(mDay + "/" + mMonth + "/" + mYear + ", " + mHour + ":" + mMinute + " " + AM_PM);
                        }
                        String dateFormat = "yyyy-MM-dd @ hh:mm a";
                        endDate = mYear + "-" + mMonth + "-" + mDay + " @ " + mHour + ":" + mMinute + " " + AM_PM;
                        try {
                            hoursDiff = hoursDifference(new SimpleDateFormat(dateFormat).parse(startDate), new SimpleDateFormat(dateFormat).parse(endDate));
                        } catch (ParseException e) {
                            Toast.makeText(ctx, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                        }


                    }
                }, mHour, mMinute, false);

        timePickerDialog.show();
    }

    private long hoursDifference(Date date1, Date date2) {
        long secs = (date2.getTime() - date1.getTime()) / 1000;
        long hours = secs / 3600;
        return hours;
    }


    public class inventoryAdapterList extends BaseAdapter implements Filterable {
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
            view = LayoutInflater.from(context).inflate(R.layout.place_demand_list, parent, false);

            if (inventories.size() > 0) {
                ImageView itemImg = view.findViewById(R.id.itemImg);
                TextView itemName = view.findViewById(R.id.itemName);
                final TextView itemPrice = view.findViewById(R.id.itemPrice);
                final Button itemBtn = view.findViewById(R.id.itemBtn);
                final Spinner qunatitySpinner = view.findViewById(R.id.itemQunatitySpinner);
                final String[] currSpinnerValue = {unitsArr[inventories.get(position).getQuantity_type()].get(0)};
                final double[] currItemPrice = {0};

                if(inventories.get(position).getImg() != null){
                    if (!inventories.get(position).getImg().equals("")) {
                        Picasso.with(PlaceOrderActivity.this).load(inventories.get(position).getImg()).into(itemImg);
                    }
                }

                itemName.setText(capitalize(inventories.get(position).getName()));

                ArrayAdapter<String> adapter = new ArrayAdapter<>(PlaceOrderActivity.this, R.layout.support_simple_spinner_dropdown_item, unitsArr[inventories.get(position).getQuantity_type()]);
                qunatitySpinner.setAdapter(adapter);

                qunatitySpinner.setSelection(spinnerPoitions.get(position));


                Map<String, Object> data = new HashMap<>();
                data.put("name", inventories.get(position).getName());
                data.put("quantity", qunatitySpinner.getSelectedItem().toString());
                data.put("img", inventories.get(position).getImg());
                data.put("sku",inventories.get(position).getSku());
                data.put("price", multiplyFactorArr[inventories.get(position).getQuantity_type()].get(qunatitySpinner.getSelectedItemPosition()) * inventories.get(position).getPrice());
                if (demandsArr.indexOf(data) > -1) {
                    itemBtn.setBackgroundColor(Color.parseColor("#f04141"));
                    itemBtn.setText("Remove from Cart");
                }

                qunatitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int sPosition, long id) {
                        spinnerPoitions.set(position, qunatitySpinner.getSelectedItemPosition());
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                        double price = multiplyFactorArr[inventories.get(position).getQuantity_type()].get(sPosition) * inventories.get(position).getPrice();
                        String moneyString = formatter.format(price);
                        itemPrice.setText("MRP: " + moneyString);

                        Map<String, Object> data = new HashMap<>();
                        data.put("name", inventories.get(position).getName());
                        data.put("quantity", currSpinnerValue[0]);
                        data.put("price", currItemPrice[0]);
                        data.put("sku",inventories.get(position).getSku());
                        data.put("img", inventories.get(position).getImg());

                        if (!currSpinnerValue[0].equals(qunatitySpinner.getSelectedItem())) {
                            if (demandsArr.indexOf(data) > -1) {
                                totalAmount = totalAmount - currItemPrice[0];
                                demandsArr.remove(data);
                                mCartItemCount = demandsArr.size();
                                setupBadge();
                                itemBtn.setBackgroundColor(Color.parseColor("#008577"));
                                itemBtn.setText("Add to Cart");
                            }
                        } else {
                            if (demandsArr.indexOf(data) > -1) {
                                itemBtn.setBackgroundColor(Color.parseColor("#f04141"));
                                itemBtn.setText("Remove from Cart");
                            }
                        }
                        currSpinnerValue[0] = (String) qunatitySpinner.getSelectedItem();
                        currItemPrice[0] = price;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                itemBtn.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("ResourceAsColor")
                    @Override
                    public void onClick(View v) {

                        Map<String, Object> data = new HashMap<>();
                        data.put("name", inventories.get(position).getName());
                        data.put("quantity", qunatitySpinner.getSelectedItem().toString());
                        data.put("img", inventories.get(position).getImg());
                        data.put("price", currItemPrice[0]);
                        data.put("sku",inventories.get(position).getSku());
                        if (demandsArr.indexOf(data) == -1) {
                            totalAmount = totalAmount + currItemPrice[0];
                            demandsArr.add(data);
                            itemBtn.setBackgroundColor(Color.parseColor("#f04141"));
                            itemBtn.setText("Remove from Cart");
                        } else {
                            totalAmount = totalAmount - currItemPrice[0];
                            demandsArr.remove(data);
                            itemBtn.setBackgroundColor(Color.parseColor("#008577"));
                            itemBtn.setText("Add to Cart");
                        }

                        mCartItemCount = demandsArr.size();
                        setupBadge();

                    }
                });
            }

            return view;
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();

                    if (constraint == null || constraint.length() == 0) {
                        // if your editText field is empty, return full list of FriendItem
                        results.count = tempArray.size();
                        results.values = tempArray;
                    } else {
                        ArrayList<Map<String, Object>> filteredList = new ArrayList<>();
                        constraint = constraint.toString().toLowerCase(); // if we ignore case
                        for (inventory item : tempArray) {
                            String firstName = item.getName().toLowerCase(); // if we ignore case
                            //String lastName = item.get("pho").toLowerCase(); // if we ignore case
                            if (firstName.contains(constraint.toString())) {
                                filteredList.add((Map<String, Object>) item); // added item witch contains our text in EditText
                            }
                        }

                        results.count = filteredList.size(); // set count of filtered list
                        results.values = filteredList; // set filtered list
                    }
                    return results; // return our filtered list
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    inventoryItems = (ArrayList<inventory>) results.values; // replace list to filtered list
                    notifyDataSetChanged(); // refresh adapter
                }
            };
            return filter;
        }
    }

    public class itemsAdapterList extends BaseAdapter {
        Context context;
        ArrayList<Map<String, Object>> items;

        public itemsAdapterList(Context context, ArrayList<Map<String, Object>> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.demand_item_list, parent, false);
            TextView itemName = view.findViewById(R.id.itemName);
            TextView itemQuantity = view.findViewById(R.id.itemQuantity);
            ImageView itemImg = view.findViewById(R.id.itemImg);

            if (items.get(position).get("img") != null) {
                if (!items.get(position).get("img").equals("")) {
                    Picasso.with(PlaceOrderActivity.this).load(items.get(position).get("img").toString()).into(itemImg);
                }
            }
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            double price = ((double) items.get(position).get("price"));
            String moneyString = formatter.format(price);
            itemName.setText(capitalize(items.get(position).get("name").toString()));
            itemQuantity.setText(items.get(position).get("quantity").toString() + " - MRP: " + moneyString);

            return view;
        }
    }


}
