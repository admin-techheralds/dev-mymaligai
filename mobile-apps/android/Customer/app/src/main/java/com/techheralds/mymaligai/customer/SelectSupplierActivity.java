package com.techheralds.mymaligai.customer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class SelectSupplierActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseUser firebaseUser;
    TextView emptyText;
    ArrayList<Supplier> suppliers;
    ListView listView;
    inviteAdapterList adapterList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_supplier);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Select Supplier");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        suppliers = new ArrayList<>();

        listView = findViewById(R.id.listView);
        emptyText = findViewById(R.id.noSupplierText);

        final ProgressDialog progressDialog = ProgressDialog.show(SelectSupplierActivity.this, null, "Loading Suppliers.Please wait...");
        firebaseDatabase.getReference().child("customers-suppliers/" + firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressDialog.dismiss();
                if (dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        progressDialog.dismiss();
                        firebaseDatabase.getReference().child("suppliers/" + ds.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                Supplier data = userSnap.getValue(Supplier.class);
                                suppliers.add(data);
                                adapterList = new inviteAdapterList(SelectSupplierActivity.this, suppliers);
                                listView.setAdapter(adapterList);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                } else {
                    emptyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialog.dismiss();
                Toast.makeText(SelectSupplierActivity.this, "Can't Load.Please try again", Toast.LENGTH_SHORT).show();
            }
        });
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
            view = LayoutInflater.from(context).inflate(R.layout.suppliers_list, parent, false);
            TextView name = view.findViewById(R.id.name);
            CircleImageView dp = view.findViewById(R.id.avatar);
            TextView number = view.findViewById(R.id.phoneNumber);
            Button acceptBtn = view.findViewById(R.id.acceptBtn);
            Button rejectBtn = view.findViewById(R.id.rejectBtn);

            name.setText(suppliers.get(position).getName());
            number.setText(suppliers.get(position).getPhoneNumber());

            if (suppliers.get(position).getPhoto().equals("")) {
                dp.setImageResource(R.drawable.nouser);
            } else {
                Picasso.with(SelectSupplierActivity.this).load(suppliers.get(position).getPhoto()).into(dp);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(SelectSupplierActivity.this, PlaceOrderActivity.class);
                    intent.putExtra("uid", suppliers.get(position).getUid());
                    intent.putExtra("name", suppliers.get(position).getName());
                    intent.putExtra("dp", suppliers.get(position).getPhoto());
                    intent.putExtra("phoneNumber", suppliers.get(position).getPhoneNumber());
                    startActivity(intent);
                }
            });
            return view;
        }
    }
}
