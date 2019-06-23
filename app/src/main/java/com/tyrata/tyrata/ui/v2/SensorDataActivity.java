package com.tyrata.tyrata.ui.v2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;
import com.tyrata.tyrata.data.model.Graph;
import com.tyrata.tyrata.data.remote.v2.BleAdapterService;
import com.tyrata.tyrata.data.remote.v2.BleScanner;
import com.tyrata.tyrata.data.remote.v2.ScanResultsConsumer;
import com.tyrata.tyrata.ui.ListSensorActivity;
import com.tyrata.tyrata.util.CommonUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.PI;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SensorDataActivity extends Activity implements ScanResultsConsumer {

        private final static String TAG = "Sensor Info";
    //private static final String CAPACITANCE_LABEL= "Capacitance (pF)";
    private static final String CAPACITANCE_LABEL= "Measurement";
    private static final String FREQUENCY_LABEL= "Frequency (Hz)";
    private static final String CAPACITANCE_DATA_LABEL = "COLLECT CAPACITANCE DATA";
    private static final String FREQUENCY_DATA_LABEL = "COLLECT FREQUENCY DATA";
    private static final int REQUEST_LOCATION = 0;
        //  private BluetoothLeService mBluetoothLeService
    private Graph mGraph;
    private boolean ble_scanning = false;
    private Toast toast;
    private static final long SCAN_TIMEOUT = 5000;
    private boolean permissions_granted = false;
    private BleScanner ble_scanner;
        private long mReqTime;
        private Handler mHandler;
    private String device_name;
    private String device_address;
    private byte[] device_data;
    private boolean back_requested = false;
    private HashMap<String, String> lastReading;
    public static BleAdapterService bluetooth_le_adapter;
    private ListAdapter readings_list_adapter;
    private String reading = "";
    public static String sensor_mode;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private DocumentSnapshot active_sensor;
    private boolean isReadings;
    private boolean isAD7747;
    private boolean isAfterReading;

    String coll;

    public static class ViewHolder {
        public TextView time;
        public TextView temp;
        public TextView capacitance;
        public TextView frequency;
        public TextView voltage;
    }
    private final ServiceConnection service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(message_handler);
            bluetooth_le_adapter.connect(device_address);
            onConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler message_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            byte[] b = null;
// message handling logic
            switch (msg.what) {
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                    showMsg(text);
                    break;
                case BleAdapterService.GATT_CONNECTED:
// we're connected
                    bluetooth_le_adapter.discoverServices();
                    showMsg("CONNECTED");
                    ((Button) SensorDataActivity.this.findViewById(R.id.req_btn)).setEnabled(true);
                    ((TextView) findViewById(R.id.is_connected)).setText("CONNECTED");
                    View editName = findViewById(R.id.ic_settings);
                    editName.setEnabled(true);
                    findViewById(R.id.more_settings).setEnabled(true);
                    try {
                        Thread.sleep(500);
                        bluetooth_le_adapter.sendMessage("SENSOR=" + (isAD7747
                                ? "0" : "1"));
                        Thread.sleep(500);
                        bluetooth_le_adapter.sendMessage("SENSOR?");
                        Thread.sleep(1000);
                        if(isAfterReading) {
                            bluetooth_le_adapter.sendMessage("HZ");
                            showMsg("Just sent HZ");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case BleAdapterService.GATT_DISCONNECT:
// we're disconnected
                    showMsg("DISCONNECTED");
                    if (back_requested) {
                        SensorDataActivity.this.finish();
                    }
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
// validate services and if ok....
                    List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                    boolean uart_present = false;

                    for (BluetoothGattService svc : slist) {
                        Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + " INSTANCE=" + svc.getInstanceId());
                        if(svc.getUuid().toString().equalsIgnoreCase(BleAdapterService.NORDIC_UART_UUID)) {
                            uart_present = true;
                            continue;
                        }
                    }
                    if(uart_present) {
                        showMsg("UART Baby");
                        bluetooth_le_adapter.readCharacteristic(
                                BleAdapterService.LINK_LOSS_SERVICE_UUID,
                                BleAdapterService.RX_UUID);
                    } else {
                        showMsg("Device does not have expected GATT services");
                    }
                    break;

                case BleAdapterService.GATT_CHARACTERISTIC_READ:
                    bundle = msg.getData();
                    Log.d(Constants.TAG, "Service=" + bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString().toUpperCase() + " Characteristic=" + bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString().toUpperCase());
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.RX_UUID)
                            && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.NORDIC_UART_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {

                        }
                    }
                    break;

                case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    bundle = msg.getData();
                    if (bundle.get(BleAdapterService.PARCEL_CHARACTERISTIC_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.TX_UUID)
                            && bundle.get(BleAdapterService.PARCEL_SERVICE_UUID).toString()
                            .toUpperCase().equals(BleAdapterService.NORDIC_UART_UUID)) {
                        b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                        if (b.length > 0) {
                            showMsg(Arrays.toString(b));
                        }
                    }
                    break;
                case BleAdapterService.NOTIFICATION_OR_INDICATION_RECEIVED:
                    bundle = msg.getData();
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                    System.out.println("SEND ME THIS LINE MICHAEL (Response): " + new String(b));
                    getDataFromReading(b);
                    handleIncoming(new String(b));
                    Log.d(Constants.TAG, Arrays.toString(b));
            }
        }
    };

    private void getDataFromReading(byte[] b) {
        String s = new String(b);
        reading = reading + s;
        if(!validateReading() || !isAD7747){
            return;
        }
        int index = reading.indexOf("<");
        reading = reading.substring(index);
        System.out.println("This should be the entire reading: " + reading);
        String d = getScan("pF");
        // String d = getScan("pF");
        String time = getScan("Time");
        String t = getScan("C");
        String v = getScan("V");
        this.lastReading = new HashMap<>();
        lastReading.put(isAD7747 ? "Capacitance" : "Frequency" , d);
        lastReading.put("Time", time);
        lastReading.put("Temperature", t);
        lastReading.put("Voltage", v);
        addLastReadingToTable();
        reading = "";
    }

    private void addLastReadingToTable() {
        if(lastReading == null) {
            showMsg("Somewhere along the way you tried adding to the table when you shouldn't have, can't add anything");
            return;
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        lastReading.put("Date", dateFormat.format(date));
        active_sensor.getReference().update("readings", FieldValue.arrayUnion(lastReading));
        readings_list_adapter.addMeasurement(lastReading);
        readings_list_adapter.notifyDataSetChanged();
        showGraph();
    }

    private boolean validateReading() {
        return reading.contains("</V>");
    }

    private String getScan(String s) {
        String[] sArr = reading.split("<"+ s + ">");
        String dataStr = sArr[1];
        String[] dataArr = dataStr.split("<");
        String data = dataArr[0];
        if(data.contains("AD7747")) {
            data = "Out of Range";
        }
        System.out.println("MICHAEL: " + s + " is what you're getting.... Value is " + data);
        return data;
    }

    @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_sensor_data);
            isAD7747 = sensor_mode.equalsIgnoreCase(Constants.AD7747_MODE);
        coll = isAD7747 ? "ad7747" : "rf";
        setActionBar(new Toolbar(getApplicationContext()));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
        //mGraph.addSecondaryAxis("Temperature (C)");
        View editName = findViewById(R.id.ic_settings);
        editName.setEnabled(false);
        findViewById(R.id.more_settings).setEnabled(false);
        ((TextView) findViewById(R.id.is_connected)).setText("NOT CONNECTED");
//
//        TextView x_label = findViewById(R.id.x_label);
//        x_label.setText("Readings");
//        TextView y_label = findViewById(R.id.y_label);
//        y_label.setText((sensor_mode.equals(Constants.AD7747_MODE)  ? "Capacitance (pF)" : "Frequency (Hz)"));
        GraphView graphView = findViewById(R.id.graph_beta);
        mGraph = new Graph(graphView, "Readings",(isAD7747  ? "Capacitance (pF)" : "Frequency (Hz)"), "");

        // Change units if clicked on X or Y axes (labels)
//        x_label.setOnClickListener(view -> {
//            mGraph.removeAllSeries();
//            if(isReadings) {
//                x_label.setText("Time");
//            } else {
//                x_label.setText("Readings");
//            }
//            isReadings = !isReadings;
//            showGraph();
//        });
/**
 * Set of buttons in second row.
 */
        // Read Register
        Button settingsBtn = findViewById(R.id.more_settings);
        settingsBtn.setOnClickListener(view -> settingsListener());
        readings_list_adapter = new SensorDataActivity.ListAdapter();
        ListView listView = (ListView) this.findViewById(R.id.other_info);
        listView.setAdapter(readings_list_adapter);
        isReadings = true;
            ble_scanner = new BleScanner(this.getApplicationContext());
            mHandler = new Handler();
            createIntervalSpinner();
            createAxisSpinner();
// read intent data
            final Intent intent = getIntent();
            device_name = intent.getStringExtra(Constants.SENSOR_NAME);
            device_address = intent.getStringExtra(Constants.SENSOR_MAC);
            device_data = intent.getByteArrayExtra(Constants.SENSOR_DATA);
// show the device name
            ((TextView) this.findViewById(R.id.device_address)).setText(device_address);
            ((TextView) this.findViewById(R.id.device_name)).setText(device_name);
// connect to the Bluetooth adapter service
            Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
            bindService(gattServiceIntent, service_connection, BIND_AUTO_CREATE);

            Button clr_btn = findViewById(R.id.clear_data);
            clr_btn.setOnClickListener(view -> clearData());
        // Redraw graph
        ImageView resetGraphBtn = findViewById(R.id.reset_graph);
        resetGraphBtn.setOnClickListener(view -> showGraph());

        Button share_btn = findViewById(R.id.share_btn);
        share_btn.setOnClickListener(view -> onExportData());

        Button reset_btn = findViewById(R.id.reset_sensor);
        reset_btn.setOnClickListener(view -> resetSensor());

        TextView freqTxt = findViewById(R.id.freq_text);
        freqTxt.setText(isAD7747 ? "" : FREQUENCY_LABEL);
        TextView dataTxt = findViewById(R.id.collected_data_text);
        dataTxt.setText(isAD7747 ? CAPACITANCE_DATA_LABEL : FREQUENCY_DATA_LABEL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView) this.findViewById(R.id.device_name)).setText(device_name);
        ((Button) SensorDataActivity.this.findViewById(R.id.req_btn)).setEnabled(bluetooth_le_adapter != null && bluetooth_le_adapter.isConnected());
    }
    private void clearData() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(SensorDataActivity.this);
        alert.setTitle("Are you sure?");
        alert.setMessage("This will clear all of the data collected.");
        alert.setPositiveButton("YES", (dialog, whichButton) -> {
            AlertDialog.Builder alert2 = new AlertDialog.Builder(SensorDataActivity.this);
            alert2.setTitle("Actually??");
            alert2.setPositiveButton("YES!", (dialog2, whichButton2) ->  {
                this.readings_list_adapter.clear();
                active_sensor.getReference().update("readings", new ArrayList<HashMap<String, String>>());
                readings_list_adapter.notifyDataSetChanged();
                showGraph();
            });
            alert2.setNegativeButton("CANCEL", (dialog2, whichButton2) -> {

            });
            alert2.show();
        });
        alert.setNegativeButton("NO", (dialog, whichButton) -> {
        });
        alert.show();
    }

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CommonUtil.sensorValues[2] = msg;
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(service_connection);
        bluetooth_le_adapter = null;
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        back_requested = true;
        if (bluetooth_le_adapter.isConnected()) {
            try {
                bluetooth_le_adapter.disconnect();
            } catch (Exception e) {
            }
        } else {
            finish();
        }
    }

        /**
         * Create a dropdown menu with options for setting data fetching time-interval
         * Eg. Selecting '10s' will get reading from sensor every 10sec
         */
        private void createIntervalSpinner() {
            Spinner spinner = findViewById(R.id.epoch_selector);
            ArrayList<String> options = new ArrayList<>();
            options.add("Take reading on button-click");
            options.add("Take reading every 1s");
            options.add("Take reading every 10s");
            options.add("Take reading every 30s");
            options.add("Take reading every 60s");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    options);
            spinner.setAdapter(adapter);

            // Add reading to DB on button click
            Button reqBtn = findViewById(R.id.req_btn);
            reqBtn.setOnClickListener(view1 -> getReading());

            // Map total_ticks to the selected item in spinner
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // Make REQUEST button invisible for other options
                    reqBtn.setVisibility(View.INVISIBLE);

                    switch (i) {
                        case 0:
                            // Show REQUEST_DATA button only if requested_point mode is selected
                            mReqTime = -1;
                            reqBtn.setVisibility(View.VISIBLE);
                            break;
                        case 1:
                            mReqTime = 1000; // 1s
                            break;
                        case 2:
                            mReqTime = 10000; // 10s
                            break;
                        case 3:
                            mReqTime = 30000; // 30s
                            break;
                        case 4:
                            mReqTime = 60000; // 60s
                            break;
                        default:
                            // Get data only when requested (execution should never be here)
                            mReqTime = -1;
                    }

                    getDataFromSensor(mReqTime);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });
        }

    /**
     * Create a dropdown menu with options for setting data fetching time-interval
     * Eg. Selecting '10s' will get reading from sensor every 10sec
     */
    private void createAxisSpinner() {
        Spinner spinner = findViewById(R.id.x_axis_selector);
        ArrayList<String> options = new ArrayList<>();
        options.add("Readings");
        options.add("Time");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                options);
        spinner.setAdapter(adapter);

        // Map total_ticks to the selected item in spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                switch (i) {
                    case 0:
                        isReadings = true;

                        break;
                    case 1:
                        isReadings = false;

                        break;
                }
                showGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }
//
        /**
         * Set data-fetching interval and add reading to local DB when interval begins (in a loop)
         */
        private void getDataFromSensor(long interval) {
            // Do not set any interval if REQUEST BUTTON is clicked
            if(interval == -1) return;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (interval != mReqTime) { // Stop when value changed
                        Log.d(TAG, "Option changed. Restarting interval to " + mReqTime);
                        return;
                    }
                    getReading();
                    mHandler.postDelayed(this, interval); // Call this thread after interval ms
                }
            });
        }

//        public void getReading() {
//            this.setScanState(true);
//            System.out.println(Arrays.toString(device_data));
//            this.setScanState(false);
//        }

    public void getReading() {
        this.scan();
    }

        private void scan(){
            bluetooth_le_adapter.sendMessage("SCAN");
        if(!isAD7747) {
            try {
                isAfterReading = true;
                Thread.sleep(30000);
                bluetooth_le_adapter.connect(device_address);
            } catch (InterruptedException e) {
                e.printStackTrace();
                showMsg("InterruptedException");
            }
        }
        }

        /**
         * Create visual data-points using the fetched data and call Graph object to display the graph
         */
        private void showGraph() {
            mGraph.removeAllSeries();
            mGraph.setXAxisTitle(isReadings ? "Readings" : "Time");
            int dataCount = readings_list_adapter.getCount();
            // Convert lists to data-points
            DataPoint[] primaryPoints = new DataPoint[dataCount];
            DataPoint[] secondaryPoints = new DataPoint[dataCount];
            int maxArr = dataCount - 1;
            double maxX = 0;
            double minX = 0;
            try {
                maxX = (isReadings || readings_list_adapter.getCount() <=0 ) ? dataCount : new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(readings_list_adapter.getItem(0).get("Date")).getTime();
                minX = (isReadings || readings_list_adapter.getCount() <=0 ) ? 0 : new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(readings_list_adapter.getItem(maxArr).get("Date")).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            double maxYP = -1;
            double minYP = 0.0;
            double maxYS = -1;
            double minYS = 0.0;

            for (int i = 0; i < dataCount; i++) {
                int newDataPos = maxArr - i;
                String val = (isAD7747 ? readings_list_adapter.getItem(i).get("Capacitance") : readings_list_adapter.getItem(i).get("Frequency"));
                double measValue = Double.parseDouble( val == "Out of Range" ? "0.0" : val);
                // double tempValue = Double.parseDouble(readings_list_adapter.getItem(i).get("Temperature"));
                if(isReadings) {
                    primaryPoints[newDataPos] = new DataPoint(newDataPos, measValue);
                    // secondaryPoints[newDataPos] = new DataPoint(newDataPos, tempValue);
                } else {
                    Long date = null;
                    try {
                        date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(readings_list_adapter.getItem(i).get("Date")).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    primaryPoints[newDataPos] = new DataPoint(date, measValue);
                    // secondaryPoints[newDataPos] = new DataPoint(date, tempValue);
                }
                maxYP = measValue > maxYP ? measValue : maxYP;
                // maxYS = tempValue > maxYS ? tempValue : maxYS;
                minYP = measValue < minYP ? measValue : minYP;
                // minYS = tempValue < minYS ? tempValue : minYS;
            }
            // Y buffer for data readability.
            maxYP = (int) (maxYP + (Math.abs(maxYP) * .1));
            minYP = (int) (minYP - (Math.abs(minYP) * .1));
            // Y buffer for data readability.
            maxYS = (int) (maxYS + (Math.abs(maxYS) * .1));
            minYS = (int) (minYS - (Math.abs(minYS) * .1));
            // X buffer
            maxX = maxX + 1;
            maxYP = maxYP + 1;
            maxYS = maxYS + 1;
            // Create a line that connects all the points
            LineGraphSeries<DataPoint> primarySeries = new LineGraphSeries<>(primaryPoints);
            //LineGraphSeries<DataPoint> secondarySeries = new LineGraphSeries<>(secondaryPoints);

            // Show circles for each point plotted
            primarySeries.setDrawDataPoints(true);
            //secondarySeries.setDrawDataPoints(true);

            // Line colors
            primarySeries.setColor(Color.YELLOW);
            //secondarySeries.setColor(Color.RED); // Different colored line to distinguish

            // Show Toast(popup message) about the data-point touched
            primarySeries.setOnDataPointTapListener((series1, dataPoint) ->
                    Toast.makeText(
                            SensorDataActivity.this,
                            dataPoint.toString(),
                            Toast.LENGTH_SHORT).show());

            // Graph view display will have these boundaries
            mGraph.setGraphViewBounds((int) minX, (int) maxX, (int) minYP, (int) maxYP);
            //mGraph.setSecondaryGraphViewBounds((int) minYS, (int) maxYS);
            // Display graph
            if(!isReadings) {
                mGraph.switchToDate();
            }
            mGraph.plotFromData(primarySeries);
            //mGraph.plotSecondaryData(secondarySeries);
        }




        /**
         * Reset sensor readings
         */
        private void resetSensor() {
            // Show alert box that asks to confirm deletion
            AlertDialog.Builder alert = new AlertDialog.Builder(SensorDataActivity.this);
            alert.setTitle("Are you sure?");
            alert.setMessage("This will reset sensor to FACTORY settings.");
            alert.setPositiveButton("YES", (dialog, whichButton) -> {
                bluetooth_le_adapter.sendMessage("FACTORY");
                Toast.makeText(SensorDataActivity.this,
                        "Resetting sensor to FACTORY settings...", Toast.LENGTH_SHORT).show();
                Log.d("", "Sensor being reset with UART");
            });
            alert.setNegativeButton("NO", (dialog, whichButton) -> {
            });
            alert.show();
        }

        /**
         * Creates a CSV file with fields Timestamp, Reading and Difference
         * @return File path of csv (Uri)
         */
        private Uri getFormattedSensorData() {
            String columnString;
            if(isAD7747){
                columnString = "\"Timestamp\",\"Device_Name\",\"Capacitance\",\"Temperature\",\"Voltage\"";
            } else {
                columnString = "\"Timestamp\",\"Device_Name\",\"Frequency\",\"Capacitance\",\"Temperature\",\"Voltage\"";
            }

            StringBuilder resultString = new StringBuilder(columnString);
            for (int i = 0; i < readings_list_adapter.getCount(); i++) {
                HashMap<String, String> reading = readings_list_adapter.getItem(i);
                if(isAD7747) {
                    resultString.append("\n")
                            .append(reading.get("Date"))
                            .append(",")
                            .append(device_name)
                            .append(",")
                            .append(reading.get("Capacitance"))
                            .append(",")
                            .append(reading.get("Temperature"))
                            .append(",")
                            .append(reading.get("Voltage"));
                } else {
                    resultString.append("\n")
                            .append(reading.get("Date"))
                            .append(",")
                            .append(device_name)
                            .append(",")
                            .append(reading.get("Frequency"))
                            .append(",")
                            .append(convertFreqToCap(reading.get("Frequency")))
                            .append(",")
                            .append(reading.get("Temperature"))
                            .append(",")
                            .append(reading.get("Voltage"));
                }
            }


            String combinedString = resultString.toString();

            File file = null;
            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()) {
                File dir = new File(root.getAbsolutePath() + "/Tyrata");
                dir.mkdirs();
                file = new File(dir, "readings" + new Date().toString() + ".csv");
                FileOutputStream out;
                try {
                    out = new FileOutputStream(file);
                    out.write(combinedString.getBytes());
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return (file == null) ? null : Uri.fromFile(file);
        }

        /**
         * Alert notification
         */
        private void showAlertNotification(boolean doReset, boolean zeroCap) {
            // Show alert box that asks to confirm deletion
            AlertDialog.Builder alert = new AlertDialog.Builder(SensorDataActivity.this);
            alert.setTitle("Are you sure?");
            alert.setMessage(zeroCap ? "This will zero the capacitance" : "This will delete all the sensor's readings!");
            alert.setPositiveButton("YES", (dialog, whichButton) -> {
                /**
                 * If you want to zero the capacitance, then you don't want to reset the graph.
                 * Otherwise reset the graph.
                 */
                if(zeroCap) {
                    /**
                     * Request = Zero the capacitance (??)
                     */
                } else {
                    // Remove data from Firebase
                    // mReadingsRef.removeValue();
                    mGraph.removeAllSeries();
                    if (doReset) resetSensor();
                }
            });

            alert.setNegativeButton("NO", (dialog, whichButton) -> {
            });
            alert.show();
        }

    @Override
    public void candidateBleDevice(final BluetoothDevice device, final byte[] scan_record, int rssi) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(device.getAddress().equalsIgnoreCase(device_address)){
                    device_data = scan_record;
                }
            }
        });
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
    private void setScanState(boolean value) {
        ble_scanning = value;
    }

    public void onScan(View view) {
        if (!ble_scanner.isScanning()) {
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
    private void startScanning() {
        if (permissions_granted) {
            simpleToast(Constants.SCANNING,2000);
            ble_scanner.startScanning(this, SCAN_TIMEOUT);
        } else {
            Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
        }
    }

    private void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    private void requestLocationPermission() {
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
            Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG, "Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(SensorDataActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
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
            } else{
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private class ListAdapter extends BaseAdapter {
        private ArrayList<HashMap<String, String>> readings;

        public ListAdapter() {
            super();
            readings = new ArrayList<>();
        }

        public void addMeasurement(HashMap<String, String> measurement) {
//            for(HashMap map : readings) {
//                if(map.get("Capacitance") == measurement.get("Capacitance")) {
//                    return;
//                }
//            }

            readings.add(0, measurement);
        }

        public HashMap getMeasurement(int position) {
            return readings.get(position);
        }

        public void clear() {
            readings.clear();
        }

        @Override
        public int getCount() {
            return readings.size();
        }

        @Override
        public HashMap<String, String> getItem(int i) {
            return readings.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            SensorDataActivity.ViewHolder viewHolder;
            if (view == null) {
                view = SensorDataActivity.this.getLayoutInflater().inflate(R.layout.item_sensor, null);
                viewHolder = new SensorDataActivity.ViewHolder();
                viewHolder.time = (TextView) view.findViewById(R.id.data_time);
                viewHolder.temp = (TextView) view.findViewById(R.id.data_temperature);
                viewHolder.capacitance = (TextView) view.findViewById(R.id.data_capacitance);
                viewHolder.frequency = (TextView) view.findViewById(R.id.data_frequency);
                viewHolder.voltage = (TextView) view.findViewById(R.id.data_voltage);

                view.setTag(viewHolder);
            } else {
                viewHolder = (SensorDataActivity.ViewHolder) view.getTag();
            }

            viewHolder.time.setText(readings.get(i).get("Date"));
            viewHolder.temp.setText(readings.get(i).get("Temperature"));
            viewHolder.voltage.setText(readings.get(i).get("Voltage"));

            if(isAD7747) {
                viewHolder.capacitance.setText(readings.get(i).get("Capacitance"));
            } else {
                String freq = readings.get(i).get("Frequency");
                viewHolder.frequency.setText(freq);
                viewHolder.capacitance.setText(convertFreqToCap(freq));
            }

            return view;
        }
    }

    private String convertFreqToCap(String freq) {
            try{
                double freqNum = Double.parseDouble(freq);

                double capNum = Math.pow(10, 12) * ((1/(Math.pow(2 * PI * freqNum, 2)))/(16.405 * Math.pow(10, -6)));

                return capNum + "";

            } catch(Exception e) {
                System.out.println(e);
                return "";
            }
    }
    private void settingsListener() {
        Intent intent = new Intent(SensorDataActivity.this, isAD7747 ? AD7747SettingsActivity.class : FrequencySettingsActivity.class);
        intent.putExtra(Constants.SENSOR_NAME, device_name);
        intent.putExtra(Constants.SENSOR_MAC, device_address);
        startActivity(intent);
    }

    public void editName(View view) {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alert.setView(input);
        alert.setTitle("Change Name of Sensor");
        alert.setMessage("Choose what to append to Tyrata_");
        // Set up the buttons
        alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String x = input.getText().toString();
                bluetooth_le_adapter.sendMessage("ID=" + x);
                device_name = "Tyrata_0"+x;
                AlertDialog.Builder alert2 = new AlertDialog.Builder(view.getContext());
                alert2.setTitle("Reminder");
                alert2.setMessage("Change will take effect after BLE is power cycled.");
                alert2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {

                        dialog2.cancel();
                    }
                });
                alert2.show();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }
    private void handleIncoming(String s) {
        // TODO: Fix this shitty tree
        if(s.contains("7747") && !s.contains("pf")) {
            handleRegData(s);
        } else if(s.contains("TIME")) {
            handleTimeData(s);
        } else if(s.contains("TEMP =")) {
            handleTempData(s);
        } else if(s.contains("ID")){
            handleIdData(s);
        } else if (s.contains("BATTERY =")) {
            handleVoltageData(s);
        } else if (s.contains("INC")) {
            handleFreqInc(s);
        } else if (s.contains("OFFSET")) {
            handleOffset(s);
        } else if (s.contains("PEAKS")) {
            handlePeaks(s);
        } else if (s.contains("FREQSTART")) {
            handleFreqStart(s);
        }else if (s.contains("FREQEND")) {
            handleFreqEnd(s);
        } else if(s.contains("SENSOR")) {
            handleSensor(s);
        } else if(s.contains("HZ")) {
            handleHzData(s);
        }
    }

    private void handleHzData(String s) {
            String[] arr = s.split(" = ");
            String hz = arr[1];
            lastReading = new HashMap<>();
            lastReading.put("Frequency", hz);
            showMsg("Size of lastReading: " + lastReading.size());
            bluetooth_le_adapter.sendMessage("Time?");
        }

    private void handleSensor(String s) {
            System.out.println("THIS IS THE SENSOR MICHAEL: " + s);
    }

    private void handleFreqInc(String s) {
        String[] arr = s.split("=");
        if(arr[0].contains("1")) {
            FrequencySettingsActivity.freq_1_inc.setText(arr[1] + "Hz");
        } else if(arr[0].contains("2")) {
            FrequencySettingsActivity.freq_2_inc.setText(arr[1] + "Hz");
        }else if(arr[0].contains("3")) {
            FrequencySettingsActivity.freq_3_inc.setText(arr[1] + "Hz");
        }else if(arr[0].contains("4")) {
            FrequencySettingsActivity.freq_4_inc.setText(arr[1] + "Hz");
        }

    }

    private void handleOffset(String s) {
        String[] arr = s.split("=");
        if(arr[0].contains("2")) {
            FrequencySettingsActivity.freq_2_offset.setText(arr[1] + "Hz");
        }else if(arr[0].contains("3")) {
            FrequencySettingsActivity.freq_3_offset.setText(arr[1] + "Hz");
        }else if(arr[0].contains("4")) {
            FrequencySettingsActivity.freq_4_offset.setText(arr[1] + "Hz");
        }
    }

    private void handlePeaks(String s) {
        String[] arr = s.split("=");
        FrequencySettingsActivity.peaks.setText(arr[1]);
    }

    private void handleFreqStart(String s) {
        String[] arr = s.split("=");
        FrequencySettingsActivity.freq_start.setText(arr[1]);
    }

    private void handleFreqEnd(String s) {
        String[] arr = s.split("=");
        FrequencySettingsActivity.freq_end.setText(arr[1]);
    }
    private void handleIdData(String s) {
        ((TextView) this.findViewById(R.id.device_name)).setText(device_name);
    }

    private void handleTempData(String s) {
        String[] temps = s.split(" ");
        String temp = temps[2];
        int tempNum = Integer.parseInt(temp);
        double tempDoub = milliConversion(tempNum, 1);
        if(isAfterReading) {
            lastReading.put("Temperature", tempDoub + "");
            showMsg("Size of lastReading: " + lastReading.size());
            bluetooth_le_adapter.sendMessage("Battery?");
        } else {
            AD7747SettingsActivity.tempVal.setText(tempDoub + " C");
        }
    }

    private void handleTimeData(String s) {
        String[] timeArr = s.split("=");
        if(isAfterReading) {
            if(lastReading == null) {
                showMsg("Map of readings was null trying to add time, either this means you did it out of order or the frequency never got called.");
                return;
            }
            lastReading.put("Time", timeArr[1]);
            showMsg("Size of lastReading: " + lastReading.size());
            bluetooth_le_adapter.sendMessage("Temperature?");
        }
        //TODO: What are we doing with time?
        //  AD7747SettingsActivity.timeVal.setText(time);
    }

    private void handleVoltageData(String s) {
        String[] voltArr = s.split(" ");
        String volts = voltArr[2];
        int index = volts.indexOf("m");
        String voltageString = volts.substring(0, index);
        int voltage = Integer.parseInt(voltageString);
        double voltageDoub = milliConversion(voltage, 1);
        if(isAfterReading) {
            lastReading.put("Voltage", voltageDoub + "");
            showMsg("Size of lastReading: " + lastReading.size());
            showMsg("Should be adding to table");
            addLastReadingToTable();
            isAfterReading = false;
        } else {
            switch(sensor_mode) {
                case(Constants.AD7747_MODE): AD7747SettingsActivity.voltageVal.setText(voltageDoub + "");
                case(Constants.RF_MODE): FrequencySettingsActivity.voltageVal.setText(voltageDoub + "");
            }
        }
    }

    private double milliConversion(int data, int sigFigs) {
            double dataDouble = data / (1000.0 / Math.pow(10.0, sigFigs));
            return Math.round(dataDouble) / Math.pow(10.0, sigFigs);
    }

    private void handleRegData(String s) {
        String[] splitOnG = s.split("G");
        String secondHalf = splitOnG[1];
        int indexOfEquals = secondHalf.indexOf("=");
        String x = secondHalf.substring(0, indexOfEquals);
        String y = secondHalf.substring(indexOfEquals+1);
        if(x.equalsIgnoreCase("B") || x.equalsIgnoreCase("0B")){
            y = y.trim();
            int yInt = Integer.parseInt(y, 16);
            AD7747SettingsActivity.capDacMSB = yInt & 0x80;
            int regVal = (yInt & 0x3F);
            AD7747SettingsActivity.capDacAVal.setText(Integer.toHexString(regVal).toUpperCase());
        }
        AD7747SettingsActivity.regReadVal.setText(x);
        AD7747SettingsActivity.regSetVal.setText(y);
    }

    public void onExportData() {
            Uri fileUri = getFormattedSensorData();
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Reading data from + " + new Date());
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setData(Uri.parse("mailto:tyratatests@gmail.com"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } catch(Exception e)  {
            System.out.println("is exception raises during sending mail"+e);
        }
    }

    private void onConnected() {
       checkSensor();
    }

    private void checkSensor() {
        Query doc = db.collection("sensors").whereEqualTo("MAC", device_address);
        doc.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        if(!docs.isEmpty()) {
                            String docId = docs.get(0).getId();
                            Query typeQuery = db.collection(coll).whereEqualTo("SensorId", docId);
                            typeQuery.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    List<DocumentSnapshot> typeDocs = queryDocumentSnapshots.getDocuments();
                                    if(!typeDocs.isEmpty()) {
                                        active_sensor = typeDocs.get(0);
                                        ArrayList<HashMap<String, String>> maps = ((ArrayList<HashMap<String, String>>) active_sensor.getData().get("readings"));
                                        for(HashMap<String, String> map : maps) {
                                            readings_list_adapter.addMeasurement(map);
                                            readings_list_adapter.notifyDataSetChanged();
                                        }
                                        showGraph();
                                    } else {
                                        addTypeSensor(docId);
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            System.out.println(e);
                                        }
                             });

                        } else {
                            // Create a new sensors with fields
                            Map<String, Object> sensor = new HashMap<>();
                            sensor.put("Name", device_name);
                            sensor.put("MAC", device_address);

                            // Add a new document with a generated ID
                            db.collection("sensors")
                                    .add(sensor)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    String docId = documentSnapshot.getId();
                                                    addTypeSensor(docId);
                                                }
                                            });
                                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error adding document", e);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error connecting to Firebase", e);
                    }
                });
    }

    private void addTypeSensor(String docId) {
        // Create a new sensors with fields
        Map<String, Object> sensor = new HashMap<>();
        sensor.put("SensorId", docId);
        sensor.put("readings", new ArrayList<HashMap<String, String>>());

        // Add a new document with a generated ID
        db.collection(coll)
                .add(sensor)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                active_sensor = documentSnapshot;
                            }
                        });
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }
}
