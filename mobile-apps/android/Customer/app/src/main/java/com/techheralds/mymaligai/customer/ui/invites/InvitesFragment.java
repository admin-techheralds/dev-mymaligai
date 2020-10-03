package com.techheralds.mymaligai.customer.ui.invites;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.techheralds.mymaligai.customer.R;
import com.techheralds.mymaligai.customer.Supplier;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class InvitesFragment extends Fragment {
    FirebaseUser firebaseUser;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth firebaseAuth;
    ArrayList<Supplier> suppliers;
    ListView listView;
    inviteAdapterList adapterList;
    TextView emptyText;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_invites, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        suppliers = new ArrayList<>();
        listView = root.findViewById(R.id.listView);
        emptyText = root.findViewById(R.id.emptyText);

        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, "Please wait...");
        firebaseDatabase.getReference().child("cInvites/" + firebaseUser.getPhoneNumber()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        progressDialog.dismiss();
                        firebaseDatabase.getReference().child("suppliers/" + ds.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                Supplier data = userSnap.getValue(Supplier.class);
                                suppliers.add(data);
                                adapterList = new inviteAdapterList(getContext(), suppliers);
                                listView.setAdapter(adapterList);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                } else {
                    progressDialog.dismiss();
                    emptyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Try again", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }


    public class inviteAdapterList extends BaseAdapter {
        ArrayList<Supplier> suppliers;
        Context context;

        public inviteAdapterList(Context context, ArrayList<Supplier> suppliers) {
            this.suppliers = suppliers;
            this.context = context;
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
            view = LayoutInflater.from(context).inflate(R.layout.invite_list, parent, false);
            TextView name = view.findViewById(R.id.name);
            CircleImageView dp = view.findViewById(R.id.avatar);
            TextView number = view.findViewById(R.id.phoneNumber);
            final TextView date = view.findViewById(R.id.date);
            Button acceptBtn = view.findViewById(R.id.acceptBtn);
            Button rejectBtn = view.findViewById(R.id.rejectBtn);

            name.setText(suppliers.get(position).getName());
            number.setText(suppliers.get(position).getPhoneNumber());
            firebaseDatabase.getReference().child("cInvites/" + firebaseUser.getPhoneNumber() + "/" + suppliers.get(position).getUid() + "/invited_date").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    date.setText("Invited on: "+dataSnapshot.getValue().toString());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            if (suppliers.get(position).getPhoto().equals("")) {
                dp.setImageResource(R.drawable.nouser);
            } else {
                Picasso.with(getContext()).load(suppliers.get(position).getPhoto()).into(dp);
            }

            final View finalView = view;
            acceptBtn.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onClick(View v) {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime now = LocalDateTime.now();
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "Accepted");
                    data.put("accepted_date",dtf.format(now));
                    firebaseDatabase.getReference().child("sInvites/" + suppliers.get(position).getUid() + "/" + firebaseUser.getPhoneNumber()).updateChildren(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            firebaseDatabase.getReference().child("cInvites/" + firebaseUser.getPhoneNumber() + "/" + suppliers.get(position).getUid()).removeValue();
                            firebaseDatabase.getReference().child("customers-suppliers/" + firebaseUser.getUid() + "/" + suppliers.get(position).getUid()).setValue(true);

                            suppliers.remove(suppliers.get(position));
                            ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();
                            if (suppliers.size() == 0) {
                                emptyText.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            });
            return view;
        }
    }

}