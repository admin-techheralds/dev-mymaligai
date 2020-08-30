package com.techheralds.mymaligai.supplier.ui.manage_consumers;

import android.Manifest;
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
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techheralds.mymaligai.supplier.R;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ManageConsumersFragment extends Fragment {
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    TextView emptyText;
    inviteAdapterList adapterList;
    ArrayList<Map<String, Object>> invites;
    ListView listView;

    public static final int REQUEST_READ_PHONE_STATE = 101;
    public static final int REQUEST_SEND_SMS = 102;
    public static final int REQUEST_RECEIVE_SMS = 103;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_manage_consumers, container, false);

        invites = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        user = firebaseAuth.getCurrentUser();

        emptyText = root.findViewById(R.id.emptyText);
        listView = root.findViewById(R.id.listView);

        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, "Please wait...");
        firebaseDatabase.getReference().child("sInvites/" + user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                invites.clear();
                adapterList = new inviteAdapterList(getContext(), invites);
                listView.setAdapter(adapterList);
                progressDialog.dismiss();
                if (dataSnapshot.getChildrenCount() > 0) {
                    emptyText.setVisibility(View.GONE);
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        Map<String, Object> data = (Map<String, Object>) ds.getValue();
                        invites.add(data);
                        adapterList = new inviteAdapterList(getContext(), invites);
                        listView.setAdapter(adapterList);
                    }
                } else {
                    emptyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        return root;
    }

    private void sendSMS(final String phoneNumber, final String date) {

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("local", Context.MODE_PRIVATE);
        String smsTemplte = sharedPreferences.getString("smsTemplate", "");
        String smsURL = sharedPreferences.getString("smsURL", "");
        String message = smsTemplte + "\nInstall Customer App here: " + smsURL;

        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(getContext(), 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(getContext(), 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been accept---
        getActivity().registerReceiver(new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Map<String, Object> data = new HashMap<>();
                        data.put("date", date);
                        data.put("status", "Pending");
                        firebaseDatabase.getReference().child("sInvites/" + user.getUid() + "/" + phoneNumber).updateChildren(data);
                        firebaseDatabase.getReference().child("cInvites/" + phoneNumber + "/" + user.getUid()).setValue(true);
                        //smsSentCount++;
                        Toast.makeText(getContext(), "SMS sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getContext(), "Generic failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getContext(), "No service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getContext(), "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getContext(), "Radio off", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        //Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();

                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();

                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
    }

    public class inviteAdapterList extends BaseAdapter {
        ArrayList<Map<String, Object>> contacts;
        Context context;

        public inviteAdapterList(Context context, ArrayList<Map<String, Object>> contacts) {
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
            view = LayoutInflater.from(context).inflate(R.layout.invite_list, parent, false);
            TextView name = view.findViewById(R.id.name);
            CircleImageView avatar = view.findViewById(R.id.avatar);
            TextView number = view.findViewById(R.id.phoneNumber);
            TextView date = view.findViewById(R.id.date);
            TextView aDate = view.findViewById(R.id.aDate);
            ImageButton accepted = view.findViewById(R.id.accepted);
            ImageButton rejected = view.findViewById(R.id.rejected);
            ImageButton pending = view.findViewById(R.id.pending);
            ImageButton resend = view.findViewById(R.id.resend);

            name.setText(contacts.get(position).get("name").toString());
            number.setText(contacts.get(position).get("number").toString());

            if (!contacts.get(position).get("accepted_date").toString().equals("")) {
                aDate.setVisibility(View.VISIBLE);
                aDate.setText("Accepted on: " + contacts.get(position).get("accepted_date").toString());
            }

            String status = contacts.get(position).get("status").toString();

            if (status.equalsIgnoreCase("accepted")) {
                accepted.setVisibility(View.VISIBLE);
            } else if (status.equalsIgnoreCase("rejected")) {
                rejected.setVisibility(View.VISIBLE);
                resend.setVisibility(View.VISIBLE);
            } else if (status.equalsIgnoreCase("pending")) {
                pending.setVisibility(View.VISIBLE);
                resend.setVisibility(View.VISIBLE);
            }
            date.setText("Invited on: " + contacts.get(position).get("invited_date").toString());
            Picasso.with(context).load("https://ui-avatars.com/api/?name=" + contacts.get(position).get("name").toString()).into(avatar);

            accepted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Invite accepted", Toast.LENGTH_SHORT).show();
                }
            });
            rejected.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Invite rejected", Toast.LENGTH_SHORT).show();
                }
            });
            pending.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(context, "Invite pending", Toast.LENGTH_SHORT).show();
                }
            });
            resend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(getContext()).setTitle("Resend Invite")
                            .setMessage("Are you want to resend invite to " + contacts.get(position).get("name") + "(" + contacts.get(position).get("number") + ")")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Resend Invite", new DialogInterface.OnClickListener() {
                                @RequiresApi(api = Build.VERSION_CODES.O)
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                                            ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

                                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, YYYY, HH:mm a");
                                        LocalDateTime now = LocalDateTime.now();
                                        sendSMS(contacts.get(position).get("number").toString(), dtf.format(now));
                                    } else {
                                        requestSMSPermissions();
                                    }
                                }
                            }).show();
                }
            });
            return view;
        }
    }

    private void requestSMSPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }

        int permissionCheck1 = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS);

        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
        }

        int permissionCheck2 = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS);

        if (permissionCheck2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_RECEIVE_SMS);
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
                    Toast.makeText(getContext(), "Permission Required", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_SEND_SMS: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestSMSPermissions();
                    Toast.makeText(getContext(), "Click Send Button Again", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Permission Required", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_RECEIVE_SMS: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestSMSPermissions();
                } else {
                    Toast.makeText(getContext(), "Permission Required", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

}
