package com.techheralds.mymaligai.supplier.ui.demands_report;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.techheralds.mymaligai.supplier.R;
import com.techheralds.mymaligai.supplier.demand;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DemandsReportFragment extends Fragment {
    EditText selectDateIp;
    int mYear;
    int mMonth;
    int mDay;
    ArrayList<demand> allDemands;
    ArrayList<demand> filteredDemands;
    FirebaseUser firebaseUser;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    ListView listView;
    Boolean isToasted = false;
    ArrayList<Map<String, Object>> reportArr;
    reportAdapterList adapterList;
    TableLayout tableLayout;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_demands_report, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        tableLayout = root.findViewById(R.id.table);
        reportArr = new ArrayList<>();

        selectDateIp = root.findViewById(R.id.dateSelectIp);
        allDemands = new ArrayList<>();
        filteredDemands = new ArrayList<>();
        listView = root.findViewById(R.id.demandsList);

        mYear = Calendar.getInstance().get(Calendar.YEAR);
        mMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        mDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        selectDateIp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_Datepicker();
            }
        });
        return root;
    }


    private void show_Datepicker() {
        Calendar c = Calendar.getInstance();
        int mYearParam = mYear;
        int mMonthParam = mMonth - 1;
        int mDayParam = mDay;

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mMonth = monthOfYear + 1;
                        mYear = year;
                        mDay = dayOfMonth;
                        final String date = mDay + "/" + mMonth + "/" + mYear;
                        selectDateIp.setText("Date: " + date);
                        final ProgressDialog progressDialog = ProgressDialog.show(getContext(), null, "Please wait...");
                        isToasted = false;

                        firebaseDatabase.getReference().child("demands/").orderByChild("supplier").equalTo(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                progressDialog.dismiss();
                                if (dataSnapshot.getChildrenCount() > 0) {
                                    reportArr.clear();
                                    adapterList = new reportAdapterList(getContext(), reportArr);
                                    listView.setAdapter(adapterList);
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        demand demandData = ds.getValue(demand.class);
                                        ArrayList<Map<String, Object>> demandListData = demandData.getDemandList();

                                        String deliveryDate = demandData.getDeliveryTime().split(",")[0];
                                        Date date1, date2;
                                        try {
                                            date1 = new SimpleDateFormat("dd/MM/yyyy").parse(deliveryDate);
                                            date2 = new SimpleDateFormat("dd/MM/yyyy").parse(date);

                                            if (date2.compareTo(date1) > -1) {
                                                if (demandData.getStatus().toLowerCase().equals("placed")) {

                                                    for (Map<String, Object> demandListDatum : demandListData) {
                                                        String name = demandListDatum.get("name").toString();
                                                        String quantity = demandListDatum.get("quantity").toString();

                                                        int index = findIndexOf(name);

                                                        if (index > -1) {
                                                            //item aready exits
                                                            String reportQuantity = reportArr.get(index).get("quantity").toString();
                                                            if (reportQuantity.endsWith("kg")) {
                                                                //its a kg value
                                                                String subsStringed = reportQuantity.substring(0, reportQuantity.length() - 2);
                                                                if (quantity.endsWith("kg")) {
                                                                    //kilogram
                                                                    String subsStringed1 = quantity.substring(0, quantity.length() - 2);
                                                                    float total = Float.valueOf(subsStringed) + Float.valueOf(subsStringed1);
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", total + "kg");

                                                                    reportArr.set(index, data);
                                                                } else {
                                                                    //gram

                                                                    String subsStringed1 = quantity.substring(0, quantity.length() - 1);

                                                                    float total = Float.valueOf(subsStringed) + Float.valueOf(String.format("%2.02f", (float) Integer.valueOf(subsStringed1) / 1000));
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", total + "kg");

                                                                    reportArr.set(index, data);
                                                                }
                                                            } else if (reportQuantity.endsWith("L")) {

                                                                //its a Litre value
                                                                String subsStringed = reportQuantity.substring(0, reportQuantity.length() - 1);
                                                                if (quantity.endsWith("ml")) {
                                                                    //milli litre
                                                                    String subsStringed1 = quantity.substring(0, quantity.length() - 2);
                                                                    float total = Float.valueOf(subsStringed) + Float.valueOf(String.format("%2.02f", (float) Integer.valueOf(subsStringed1) / 1000));

                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", total + "L");
                                                                    reportArr.set(index, data);
                                                                } else {
                                                                    //Litre
                                                                    String subsStringed1 = quantity.substring(0, quantity.length() - 1);
                                                                    float total = Float.valueOf(subsStringed) + Float.valueOf(subsStringed1);
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", total + "L");
                                                                    reportArr.set(index, data);
                                                                }
                                                            } else {
                                                                String subsStringed;
                                                                if (reportQuantity.endsWith("pc")) {
                                                                    subsStringed = reportQuantity.substring(0, reportQuantity.length() - 2);
                                                                } else {
                                                                    subsStringed = reportQuantity.substring(0, reportQuantity.length() - 3);
                                                                }
                                                                //unit in count
                                                                if (quantity.endsWith("pc")) {
                                                                    String subsStringed1 = quantity.substring(0, quantity.length() - 2);
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", Integer.valueOf(subsStringed) + Integer.valueOf(subsStringed1) + "pcs");
                                                                    reportArr.set(index, data);
                                                                } else {
                                                                    String subsStringed1 = quantity.substring(0, quantity.length() - 3);
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", Integer.valueOf(subsStringed) + Integer.valueOf(subsStringed1) + "pcs");
                                                                    reportArr.set(index, data);
                                                                }
                                                            }
                                                        } else {
                                                            //new item
                                                            if (quantity.endsWith("g")) {
                                                                //gram/kilogram values
                                                                if (quantity.endsWith("kg")) {
                                                                    //kilogram

                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", quantity);

                                                                    reportArr.add(data);
                                                                } else {
                                                                    //gram

                                                                    String subsStringed = quantity.substring(0, quantity.length() - 1);
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", String.format("%2.02f", (float) Integer.valueOf(subsStringed) / 1000) + "kg");

                                                                    reportArr.add(data);
                                                                }
                                                            } else if (quantity.toLowerCase().endsWith("l")) {
                                                                //litre/milli litre values
                                                                if (quantity.endsWith("ml")) {
                                                                    //milli litre
                                                                    String subsStringed = quantity.substring(0, quantity.length() - 2);
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", String.format("%2.02f", (float) Integer.valueOf(subsStringed) / 1000) + "L");
                                                                    reportArr.add(data);
                                                                } else {
                                                                    //Litre
                                                                    Map<String, Object> data = new HashMap<>();
                                                                    data.put("name", name);
                                                                    data.put("quantity", quantity);
                                                                    reportArr.add(data);
                                                                }
                                                            } else {
                                                                //unit in count
                                                                Map<String, Object> data = new HashMap<>();
                                                                data.put("name", name);
                                                                data.put("quantity", quantity);
                                                                reportArr.add(data);

                                                            }
                                                        }
                                                        if (reportArr.size() == 0) {
                                                            Toast.makeText(getContext(), "You have no demands", Toast.LENGTH_SHORT).show();
                                                        }
                                                        adapterList = new reportAdapterList(getContext(), reportArr);
                                                        listView.setAdapter(adapterList);
                                                    }

                                                }
                                            }
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }

                                        if (reportArr.size() == 0) {
                                            if (!isToasted) {
                                                isToasted = true;
                                                Toast.makeText(getContext(), "You have no demands", Toast.LENGTH_SHORT).show();
                                                tableLayout.setVisibility(View.GONE);
                                            }
                                        } else {
                                            tableLayout.setVisibility(View.VISIBLE);
                                        }
                                    }
                                } else {
                                    Toast.makeText(getContext(), "You have no demands", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Can't load", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, mYearParam, mMonthParam, mDayParam);
        datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
        datePickerDialog.show();
    }


    private String capitalize(String capString) {
        StringBuffer capBuffer = new StringBuffer();
        Matcher capMatcher = Pattern.compile("([a-z])([a-z]*)", Pattern.CASE_INSENSITIVE).matcher(capString);
        while (capMatcher.find()) {
            capMatcher.appendReplacement(capBuffer, capMatcher.group(1).toUpperCase() + capMatcher.group(2).toLowerCase());
        }

        return capMatcher.appendTail(capBuffer).toString();
    }

    public int findIndexOf(String itemName) {
        int index = -1;
        for (int i = 0; i < reportArr.size(); i++) {
            if (reportArr.get(i).get("name").toString().toLowerCase().trim().equals(itemName.toLowerCase())) {
                index = i;
            }
        }

        return index;
    }

    public class reportAdapterList extends BaseAdapter {

        Context context;
        ArrayList<Map<String, Object>> reportsArr;

        public reportAdapterList(Context context, ArrayList<Map<String, Object>> reportsArr) {
            this.context = context;
            this.reportsArr = reportsArr;
        }

        @Override
        public int getCount() {
            return reportsArr.size();
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
        public View getView(int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.report_list, parent, false);
            if (reportsArr.size() > 0) {
                TextView itemName = view.findViewById(R.id.itemName);
                TextView itemQuantity = view.findViewById(R.id.itemQuantity);
                itemName.setText(reportsArr.get(position).get("name").toString());
                itemQuantity.setText(reportsArr.get(position).get("quantity").toString());
            }
            return view;
        }
    }

}