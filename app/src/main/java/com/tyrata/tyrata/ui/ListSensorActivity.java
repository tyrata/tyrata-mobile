package com.tyrata.tyrata.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;
import com.tyrata.tyrata.data.remote.BleScanner;
import com.tyrata.tyrata.data.remote.ScanResultsConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Displays a list of scanned sensors and handles click events
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ListSensorActivity extends AppCompatActivity implements ScanResultsConsumer {
    private final static String TAG = "BLE Device List";
    private boolean ble_scanning = false;
    private Handler handler = new Handler();
    private ListAdapter ble_device_list_adapter;
    private InactiveListAdapter inactive_device_list_adapter;
    private BleScanner ble_scanner;
    private static final long SCAN_TIMEOUT = 5000;
    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private boolean permissions_granted = false;
    private int device_count = 0;
    private Toast toast;
    public FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static class ViewHolder {
        public TextView text;
        public TextView bdaddr;
    }

    public static class InactiveDevice {
        public String name;
        public String address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_list);
        ble_scanner = new BleScanner(this.getApplicationContext());
        ble_device_list_adapter = new ListAdapter();

        ListView listView = (ListView) this.findViewById(R.id.active_devices);
        listView.setAdapter(ble_device_list_adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (ble_scanning) {
                    setScanState(false);
                    ble_scanner.stopScanning();
                }
                BluetoothDevice device = ble_device_list_adapter.getDevice(position);
                if (toast != null) {
                    toast.cancel();
                }
                Intent intent;
                intent = new Intent(ListSensorActivity.this, SensorDataActivity.class);
                intent.putExtra(Constants.SENSOR_NAME, device.getName());
                intent.putExtra(Constants.SENSOR_MAC, device.getAddress());
                intent.putExtra(Constants.SENSOR_DATA, ble_device_list_adapter.getData(position));
                startActivity(intent);
            }
        });
        inactive_device_list_adapter = new InactiveListAdapter();
        ListView inactiveListView = (ListView) this.findViewById(R.id.inactive_devices);
        inactiveListView.setAdapter(inactive_device_list_adapter);
        inactiveListView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (ble_scanning) {
                    setScanState(false);
                    ble_scanner.stopScanning();
                }
                InactiveDevice device = inactive_device_list_adapter.getDevice(position);
                if (toast != null) {
                    toast.cancel();
                }
                Intent intent;
                intent = new Intent(ListSensorActivity.this, SensorDataActivity.class);
                intent.putExtra(Constants.SENSOR_NAME, device.name);
                intent.putExtra(Constants.SENSOR_MAC, device.address);
                startActivity(intent);
            }
        });
        // Setup the toolbar and back button
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new         Intent(getApplicationContext(), MainActivity.class));
            }
        });

        db.collection("sensors")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    inactive_device_list_adapter.clear();
                                    inactive_device_list_adapter.notifyDataSetChanged();
                                }
                            });
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                inactive_device_list_adapter.add(document.get("Name").toString(), document.get("MAC").toString());
                                Log.d(TAG, document.getId() + " => " + document.get("Name").toString());
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
//        // Testing section. Brings in whether or not we are in development mode and then sets a
//        // listener on a button to take us to Beta Testing when there are no sensors available
//        // TODO: Remove when other testing method is established
//        String type = "";
//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            type = extras.getString("MODE");
//            //The key argument here must match that used in the other activity
//        }
//        this.type = type;
//        Button devBtn = findViewById(R.id.devBeta_btn);
//        devBtn.setText("  " + type + "  ");
//        devBtn.setOnClickListener(view -> switchToTesting(null, this.type));
//        // End of Testing Hack

//        // Get the shared instance of the FirebaseAuth object
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        FirebaseUser mUser = mAuth.getCurrentUser(); // Get Current User
//        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
//
//        // Get reference to the "sensors" node in the JSON tree (See Firebase console for more info)
//        mSensorMetaRef = mDatabase.getReference("users")
//                .child(mUser.getUid()).child("sensors");
//
//        addDynamicSensorListener();
//        ble_device_list_adapter = new ListAdapter();
//        mActiveDevicesList = findViewById(R.id.active_devices);
//        mActiveDevicesList.setAdapter();
//        // Start BetaTesterActivity for the item (sensor) clicked
//        mActiveDevicesList.setOnItemClickListener((adapterView, view, i, l) -> {
//            String macAddress = (String) mActiveDevicesList.getItemAtPosition(i);
//            switchToTesting(macAddress, this.type);
//        });
//        mInactiveDevicesList.setOnItemClickListener((adapterView, view, i, l) -> {
//            String macAddress = (String) mInactiveDevicesList.getItemAtPosition(i);
//            switchToTesting(macAddress, this.type);
//        });
//
//        // Start or bind to the BLE background service
//        Intent gattServiceIntent = new Intent(ListSensorActivity.this,
//                BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ble_scanner = null;
    }

    @Override
    public void candidateBleDevice(final BluetoothDevice device, final byte[] scan_record, int rssi) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ble_device_list_adapter.addDevice(device, scan_record);
                ble_device_list_adapter.notifyDataSetChanged();
                device_count++;
            }
        });
    }

    private void setScanState(boolean value) {
        ble_scanning = value;
        ((Button) this.findViewById(R.id.devBeta_btn)).setText(value ? Constants.STOP_SCANNING : Constants.FIND);
    }

    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    @Override
    public void scanningStopped() {
        if (toast != null) {
            toast.cancel();
        }
        setScanState(false);
    }

    public void onScan(View view) {
        if (!ble_scanner.isScanning()) {
            device_count=0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissions_granted = false;
                    requestLocationPermission();
                } else {
                    Log.i(Constants.TAG, "Location permission has already been granted. Starting scanning.");
                    permissions_granted = true;
                }
            } else {
// the ACCESS_COARSE_LOCATION permission did not exist before M so....
                permissions_granted = true;
            }
            startScanning();
        } else {
            ble_scanner.stopScanning();
        }
    }

    private void requestLocationPermission() {
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
            Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG, "Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(ListSensorActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            Log.i(Constants.TAG, "Received response for location permission request.");
// Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
// Location permission has been granted
                Log.i(Constants.TAG, "Location permission has now been granted. Scanning.....");
                permissions_granted = true;
                if (ble_scanner.isScanning()) {
                    startScanning();
                }
            }else{
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void startScanning() {
        if (permissions_granted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ble_device_list_adapter.clear();
                    ble_device_list_adapter.notifyDataSetChanged();
                }
            });
            simpleToast(Constants.SCANNING,2000);
            ble_scanner.startScanning(this, SCAN_TIMEOUT);
        } else {
            Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
        }
    }
//    /**
//     * Listens for change in sensors list in FireDB (cloud)
//     */
//    private void addDynamicSensorListener() {
//        sensorValueListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                activeSensorNameList = new ArrayList<>();
//                activeSensorAddressList = new ArrayList<>();
//                inactiveSensorNameList = new ArrayList<>();
//                inactiveSensorAddressList = new ArrayList<>();
//
//                // Get devices (from Firebase) and populate active and inactive device lists
//                for (DataSnapshot uniqueKeySnapshot : dataSnapshot.getChildren()) {
//                    String currentMac = uniqueKeySnapshot.child("macAddress").getValue(String.class);
//                    if (CommonUtil.activeDevices.containsKey(currentMac)) {
//                        activeSensorNameList
//                                .add(uniqueKeySnapshot.child("name").getValue(String.class));
//                        activeSensorAddressList
//                                .add(currentMac);
//                    } else {
//                        inactiveSensorNameList
//                                .add(uniqueKeySnapshot.child("name").getValue(String.class));
//                        inactiveSensorAddressList
//                                .add(uniqueKeySnapshot.child("macAddress").getValue(String.class));
//                    }
//                }
//                showDeviceList();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//            }
//        };
//        mSensorMetaRef.addValueEventListener(sensorValueListener);
//    }

    private class ListAdapter extends BaseAdapter {
        private HashMap<BluetoothDevice, byte[]> ble_devices;

        public ListAdapter() {
            super();
            ble_devices = new HashMap<>();
        }

        public void addDevice(BluetoothDevice device, byte[] data) {
            if(device.getName() == null) {
                return;
            }
            final String regex = "TYRATA_([0-9])";
            final String regex2 = "Nordic_([A-Za-z0-9])";
            final String string = device.getName();

            final Pattern pattern = Pattern.compile(regex);
            final Pattern pattern2 = Pattern.compile(regex2);

            final Matcher matcher = pattern.matcher(string);
            final Matcher matcher2 = pattern2.matcher(string);
            if ((matcher.find() || matcher2.find()) && !ble_devices.containsKey(device)) {
                if(inactive_device_list_adapter.contains(device.getAddress())) {
                    inactive_device_list_adapter.remove(device.getAddress());
                    inactive_device_list_adapter.notifyDataSetChanged();
                }
                ble_devices.put(device, data);
            }
        }

        public boolean contains(BluetoothDevice device) {
            return ble_devices.containsKey(device);
        }

        public BluetoothDevice getDevice(int position) {
            Object[] devices =  ble_devices.keySet().toArray();
            return (BluetoothDevice) devices[position];
        }

        public byte[] getData(int position) {
            BluetoothDevice device = getDevice(position);
            return ble_devices.get(device);
        }

        public void clear() {
            ble_devices.clear();
        }

        @Override
        public int getCount() {
            return ble_devices.size();
        }

        @Override
        public Object getItem(int i) {
            return ble_devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = ListSensorActivity.this.getLayoutInflater().inflate(R.layout.list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.textView);
                viewHolder.bdaddr = (TextView) view.findViewById(R.id.bdaddr);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = getDevice(i);
            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.text.setText(deviceName);
            } else {
                viewHolder.text.setText("unknown device");
            }
            viewHolder.bdaddr.setText(device.getAddress());
            return view;
        }
    }

    private class InactiveListAdapter extends BaseAdapter {
        private List<InactiveDevice> inactive_devices;

        public InactiveListAdapter() {
            super();
            inactive_devices = new ArrayList<>();
        }

        public void add(String name, String address) {
            InactiveDevice device = new InactiveDevice();
            device.address = address;
            device.name = name;
            inactive_devices.add(device);
        }

        public boolean contains(InactiveDevice device) {
            return inactive_devices.contains(device);
        }

        public boolean contains(String address) {
            for(InactiveDevice device : inactive_devices) {
                if(device.address.equalsIgnoreCase(address)) {
                    return true;
                }
            }
            return false;
        }

        public void remove(String address) {
            InactiveDevice deviceToRemove = null;
            for(InactiveDevice device : inactive_devices) {
                if(device.address.equalsIgnoreCase(address)) {
                    deviceToRemove = device;
                }
            }
            if(deviceToRemove != null) {
                inactive_devices.remove(deviceToRemove);
            }
        }
        public InactiveDevice getDevice(int position) {
            return (InactiveDevice) inactive_devices.get(position);
        }

        public void clear() {
            inactive_devices.clear();
        }

        @Override
        public int getCount() {
            return inactive_devices.size();
        }

        @Override
        public Object getItem(int i) {
            return inactive_devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = ListSensorActivity.this.getLayoutInflater().inflate(R.layout.list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.textView);
                viewHolder.bdaddr = (TextView) view.findViewById(R.id.bdaddr);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            InactiveDevice device = getDevice(i);
            String deviceName = device.name;
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.text.setText(deviceName);
            } else {
                viewHolder.text.setText("unknown device");
            }
            viewHolder.bdaddr.setText(device.address);
            return view;
        }
    }
}
