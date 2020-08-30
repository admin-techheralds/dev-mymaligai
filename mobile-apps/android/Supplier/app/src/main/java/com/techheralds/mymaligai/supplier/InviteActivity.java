package com.techheralds.mymaligai.supplier;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class InviteActivity extends AppCompatActivity {
    public static final int REQUEST_READ_CONTACTS = 100;
    public static final int REQUEST_READ_PHONE_STATE = 101;
    public static final int REQUEST_SEND_SMS = 102;
    public static final int REQUEST_RECEIVE_SMS = 103;
    FirebaseUser user;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    ArrayList<Map<String, Object>> mobileArray, tempArray;
    ListView listView;
    contactAdapterList adapterList;
    ProgressDialog pd;
    SearchView searchView;
    ArrayList<String> selectedNumbers;
    ArrayList<Map<String, Object>> selectedContacts;
    FloatingActionButton sendInviteBtn;
    selectedContactAdapterList selectedContactAdapterList;
    int smsSentCount = 0;
    ArrayList<String> invitedCustomers = new ArrayList<>();

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        user = firebaseAuth.getCurrentUser();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Invite Customers");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        firebaseDatabase.getReference().child("sInvites/" + user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        invitedCustomers.add(ds.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        sendInviteBtn = findViewById(R.id.inviteBtn);

        sendInviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedNumbers.size() > 0) {

                    final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(InviteActivity.this);
                    bottomSheetDialog.setContentView(R.layout.selected_contacts_sheet);
                    final ListView listView = bottomSheetDialog.findViewById(R.id.listView);

                    final Boolean[] isSent = {false};

                    FloatingActionButton button = bottomSheetDialog.findViewById(R.id.inviteBtn);
                    selectedContactAdapterList = new selectedContactAdapterList(InviteActivity.this, selectedContacts);
                    listView.setAdapter(selectedContactAdapterList);

                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ActivityCompat.checkSelfPermission(InviteActivity.this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(InviteActivity.this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(InviteActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                                bottomSheetDialog.dismiss();
                                final ProgressDialog progressDialog = new ProgressDialog(InviteActivity.this);
                                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                progressDialog.setTitle("Sending SMS");
                                progressDialog.setMax(selectedNumbers.size());
                                progressDialog.show();
                                for (final String number : selectedNumbers) {
                                    new Handler().postDelayed(new Runnable() {
                                        @RequiresApi(api = Build.VERSION_CODES.O)
                                        public void run() {
                                            String phNo = number.replaceAll("\\D+", "");

                                            smsSentCount++;
                                            progressDialog.setProgress(smsSentCount);
                                            progressDialog.show();

                                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, YYYY, HH:mm a");
                                            LocalDateTime now = LocalDateTime.now();
                                            sendSMS(phNo.substring(phNo.length() - 10), number, dtf.format(now));

                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                        @Override
                                                        public void onDismiss(DialogInterface dialog) {
                                                            //   searchView.setQuery("", true);
                                                            // ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
                                                            Toast.makeText(InviteActivity.this, "SMS Sent", Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        }
                                                    });
                                                    progressDialog.dismiss();
                                                }
                                            }, 1000);
                                        }
                                    }, 2000);
                                }

                            } else {
                                requestSMSPermissions();
                            }
                        }
                    });
                    bottomSheetDialog.show();
                } else {
                    Toast.makeText(InviteActivity.this, "Select atleast one contact", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mobileArray = new ArrayList();
        tempArray = new ArrayList<>();
        selectedNumbers = new ArrayList<>();
        selectedContacts = new ArrayList<>();

        listView = findViewById(R.id.listView);
        listView.setTextFilterEnabled(true);
        searchView = findViewById(R.id.searchBar);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onQueryTextChange(final String newText) {
                adapterList = new contactAdapterList(InviteActivity.this, tempArray);
                listView.setAdapter(adapterList);
                adapterList.getFilter().filter(newText);

                return false;
            }
        });


        if (ActivityCompat.checkSelfPermission(InviteActivity.this, android.Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected void onPreExecute() {
                    pd = ProgressDialog.show(InviteActivity.this,
                            null, "Loading Contacts.Please wait...", true, false);
                }// End of onPreExecute method

                @Override
                protected Void doInBackground(Void... params) {
                    mobileArray = getAllContacts();
                    tempArray = getAllContacts();
                    return null;
                }// End of doInBackground method

                @Override
                protected void onPostExecute(Void result) {
                    pd.dismiss();
                    adapterList = new contactAdapterList(InviteActivity.this, mobileArray);
                    listView.setAdapter(adapterList);
                }//End of onPostExecute method
            }.execute((Void[]) null);

        } else {
            requestContactsPermission();
        }
    }

    private void sendSMS(final String phoneNumber, final String fullNumber, final String date) {

        SharedPreferences sharedPreferences = getSharedPreferences("local", Context.MODE_PRIVATE);
        String smsTemplte = sharedPreferences.getString("smsTemplate", "");
        String smsURL = sharedPreferences.getString("smsURL", "");
        String message = smsTemplte + "\nInstall Customer App here: " + smsURL;

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(InviteActivity.this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(InviteActivity.this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been accept---
        registerReceiver(new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Map<String, Object> data = new HashMap<>();
                        int i = selectedNumbers.indexOf(fullNumber);

                        data.put("name", selectedContacts.get(i).get("name").toString());
                        data.put("number", "+91" + phoneNumber);
                        data.put("invited_date", date);
                        data.put("accepted_date", "");
                        data.put("status", "Pending");
                        firebaseDatabase.getReference().child("sInvites/" + user.getUid() + "/+91" + phoneNumber).setValue(data);
                        firebaseDatabase.getReference().child("cInvites/+91" + phoneNumber + "/" + user.getUid()+"/invited_date").setValue(date);

                        //smsSentCount++;
                        //Toast.makeText(getBaseContext(), "SMS accept", Toast.LENGTH_SHORT).show();
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

    private void requestContactsPermission() {
        ActivityCompat.requestPermissions(InviteActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected void onPreExecute() {
                            pd = ProgressDialog.show(InviteActivity.this,
                                    null, "Loading Contacts.Please wait...", true, false);
                        }// End of onPreExecute method

                        @Override
                        protected Void doInBackground(Void... params) {
                            mobileArray = getAllContacts();
                            tempArray = getAllContacts();

                            return null;
                        }// End of doInBackground method

                        @Override
                        protected void onPostExecute(Void result) {
                            pd.dismiss();
                            adapterList = new contactAdapterList(InviteActivity.this, mobileArray);
                            listView.setAdapter(adapterList);
                        }//End of onPostExecute method
                    }.execute((Void[]) null);
                } else {
                    Toast.makeText(InviteActivity.this, "Give Permission.So that you can invite your customers", Toast.LENGTH_LONG).show();
                    // permission denied,Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<Map<String, Object>> getAllContacts() {
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                Map<String, Object> data = new HashMap<>();
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                data.put("name", name);
                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    ArrayList<String> numbers = new ArrayList<>();
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        //to remove white spaces
                        phoneNo = phoneNo.replaceAll("\\s+", "");

                        if (numbers.indexOf(phoneNo) == -1) {
                            numbers.add(phoneNo);
                        }
                        data.put("phoneNumber", numbers);
                        if (list.indexOf(data) == -1) {
                            list.add(data);
                        }
                    }
                    pCur.close();
                }
            }
        }

        if (cur != null) {
            cur.close();
        }
        return list;
    }

    public class contactAdapterList extends BaseAdapter implements Filterable {
        ArrayList<Map<String, Object>> contacts;
        Context context;

        public contactAdapterList(Context context, ArrayList<Map<String, Object>> contacts) {
            this.contacts = contacts;
            this.context = context;
        }

        @Override
        public int getCount() {
            return contacts.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            if (contacts.size() > 0) {
                view = LayoutInflater.from(context).inflate(R.layout.contact_list, parent, false);
                TextView name = view.findViewById(R.id.name);
                CircleImageView avatar = view.findViewById(R.id.avatar);
                LinearLayout numbersLayout = view.findViewById(R.id.numbersLayout);

                name.setText(contacts.get(position).get("name").toString());
                if (contacts.get(position).get("phoneNumber") != null) {
                    ArrayList<String> numbers = (ArrayList<String>) contacts.get(position).get("phoneNumber");

                    for (final String number : numbers) {
                        CheckBox checkBox = new CheckBox(InviteActivity.this);
                        String phNo = number.replaceAll("\\D+", "");
                        if (phNo.length() >= 10) {
                            String fullNumber = "+91" + phNo.substring(phNo.length() - 10);
                            checkBox.setText(fullNumber);

                            if (invitedCustomers.indexOf(fullNumber) > -1) {
                                checkBox.setEnabled(false);
                                checkBox.setChecked(true);
                                checkBox.setText("Already Invited!");
                            }
                        } else {
                            checkBox.setText(number);
                            checkBox.setEnabled(false);
                        }

                        if (selectedNumbers.indexOf(number) != -1) {
                            checkBox.setChecked(true);
                        }

                        numbersLayout.addView(checkBox);

                        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("name", contacts.get(position).get("name"));
                                    data.put("number", number);
                                    selectedContacts.add(data);

                                    selectedNumbers.add(number);

                                } else {
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("name", contacts.get(position).get("name"));
                                    data.put("number", number);
                                    selectedContacts.remove(data);

                                    selectedNumbers.remove(number);
                                }
                            }
                        });
                    }
                }
                if (contacts.get(position).get("name").toString() != null) {
                    Picasso.with(context).load("https://ui-avatars.com/api/?name=" + contacts.get(position).get("name").toString()).into(avatar);
                }
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
                        for (Map<String, Object> item : contacts) {
                            String firstName = item.get("name").toString().toLowerCase(); // if we ignore case
                            //String lastName = item.get("pho").toLowerCase(); // if we ignore case
                            if (firstName.contains(constraint.toString())) {
                                filteredList.add(item); // added item witch contains our text in EditText
                            }
                        }

                        results.count = filteredList.size(); // set count of filtered list
                        results.values = filteredList; // set filtered list
                    }
                    return results; // return our filtered list
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {

                    contacts = (ArrayList<Map<String, Object>>) results.values; // replace list to filtered list
                    notifyDataSetChanged(); // refresh adapter
                }
            };
            return filter;
        }
    }

    public class selectedContactAdapterList extends BaseAdapter {
        ArrayList<Map<String, Object>> contacts;
        Context context;

        public selectedContactAdapterList(Context context, ArrayList<Map<String, Object>> contacts) {
            this.contacts = contacts;
            this.context = context;
        }

        @Override
        public int getCount() {
            return contacts.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
        @Override
        public View getView(final int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.selected_contacts_list, parent, false);
            TextView name = view.findViewById(R.id.name);
            CircleImageView avatar = view.findViewById(R.id.avatar);
            TextView number = view.findViewById(R.id.phoneNumber);

            name.setText(contacts.get(position).get("name").toString());
            String phNo = contacts.get(position).get("number").toString().replaceAll("\\D+", "");
            if (phNo.length() >= 10) {
                String fullNumber = "+91" + phNo.substring(phNo.length() - 10);
                number.setText(fullNumber);
            }

            Picasso.with(context).load("https://ui-avatars.com/api/?name=" + contacts.get(position).get("name").toString()).into(avatar);
            return view;
        }

    }

}
