package com.tyrata.tyrata.ui.v2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;
import com.tyrata.tyrata.data.model.Graph;
import com.tyrata.tyrata.data.remote.BluetoothLeService;
import com.tyrata.tyrata.data.remote.v2.BleAdapterService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FrequencySettingsActivity extends AppCompatActivity {
    private final static String TAG = "Sensor Info";
    private final static int NUM_SCANS_ID = 0;
    private final static int FREQ_START_ID = 1;
    private final static int FREQ_END_ID = 2;
    private final static int FIRST_PASS_ID = 3;
    private final static int SECOND_PASS_ID = 4;
    private final static int THIRD_PASS_ID = 5;
    private final static int FOURTH_PASS_ID = 6;
    private final static int OFFSET_2_ID = 7;
    private final static int OFFSET_3_ID = 8;
    private final static int OFFSET_4_ID = 9;

    public static TextView peaks;
    public static TextView freq_start;
    public static TextView freq_end;
    public static TextView freq_1_inc;
    public static TextView freq_2_inc;
    public static TextView freq_3_inc;
    public static TextView freq_4_inc;
    public static TextView freq_2_offset;
    public static TextView freq_3_offset;
    public static TextView freq_4_offset;
    public static TextView voltageVal;
    private BluetoothLeService mBluetoothLeService;

    private Graph mGraph;
    private ListView mSensorInfoView;
    private ArrayList<String> mTimeStampList;
    private ArrayList<String> mThicknessList;
    private ArrayList<String> mDifferenceList;
    private ArrayList<TextInputEditText> inputValues;
    private ArrayList<TextView> textValues;
    private String type;
    private String device_address;
    private String device_name;
    private long mReqTime;
    private Handler mHandler;
    private BleAdapterService bluetooth_le_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency_settings);
        Bundle extras = getIntent().getExtras();
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(Constants.SENSOR_NAME);
        device_address = intent.getStringExtra(Constants.SENSOR_MAC);
        ((TextView) this.findViewById(R.id.device_address)).setText(device_address);
        ((TextView) this.findViewById(R.id.device_name)).setText(device_name);
        bluetooth_le_adapter = SensorDataActivity.bluetooth_le_adapter;
        // Setup the toolbar and back button
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), SensorDataActivity.class);
                startActivity(i);
            }
        });

        mHandler = new Handler();

        Button voltageBtn = findViewById(R.id.get_voltage_btn);
        voltageBtn.setOnClickListener(view -> getVoltage());

        Button updateBtn = findViewById(R.id.update_values_btn);
        updateBtn.setOnClickListener(view -> {
            try {
                updateValues();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Button setBtn = findViewById(R.id.set_values_btn);
        setBtn.setOnClickListener(view -> setValues());

        Button bleBtn = findViewById(R.id.ble_timeout_btn);
        bleBtn.setOnClickListener(view -> {
            bluetooth_le_adapter.sendMessage("BLETIMEOUT=120");
            Toast.makeText(FrequencySettingsActivity.this,
                    "Setting BLETIMEOUT to 2 hours...", Toast.LENGTH_SHORT).show();
            Log.d("", "BLETIMEOUT set to 2 hours.");
        });
        TextInputEditText numScans = findViewById(R.id.num_scans);
        TextInputEditText startFreq = findViewById(R.id.freq_start);
        TextInputEditText endFreq = findViewById(R.id.freq_end);
        TextInputEditText firstPass = findViewById(R.id.first_pass);
        TextInputEditText secondPass =findViewById(R.id.second_pass);
        TextInputEditText thirdPass = findViewById(R.id.third_pass);
        TextInputEditText fourthPass = findViewById(R.id.fourth_pass);
        TextInputEditText offset2 = findViewById(R.id.freq2offset);
        TextInputEditText offset3 = findViewById(R.id.freq3offset);
        TextInputEditText offset4 = findViewById(R.id.freq4offset);
        inputValues = new ArrayList<>();
        inputValues.add(numScans);
        inputValues.add(startFreq);
        inputValues.add(endFreq);
        inputValues.add(firstPass);
        inputValues.add(secondPass);
        inputValues.add(thirdPass);
        inputValues.add(fourthPass);
        inputValues.add(offset2);
        inputValues.add(offset3);
        inputValues.add(offset4);

        peaks = findViewById(R.id.peaks_text);
        freq_start = findViewById(R.id.freq_start_text);
        freq_end = findViewById(R.id.freq_end_text);
        freq_1_inc = findViewById(R.id.first_pass_text);
        freq_2_inc = findViewById(R.id.second_pass_text);
        freq_3_inc = findViewById(R.id.third_pass_text);
        freq_4_inc = findViewById(R.id.fourth_pass_text);
        freq_2_offset = findViewById(R.id.offset2_text);
        freq_3_offset = findViewById(R.id.offset3_text);
        freq_4_offset = findViewById(R.id.offset4_text);
        voltageVal = findViewById(R.id.voltage_value_text);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Reset sensor readings
     */
    private void resetSensor() {
        // @Todo Display edit text here to get user specified value
        // Send RESET
        bluetooth_le_adapter.sendMessage("FACTORY");
        bluetooth_le_adapter.sendMessage("SENSOR=1");
        Toast.makeText(com.tyrata.tyrata.ui.v2.FrequencySettingsActivity.this,
                "Resetting sensor...", Toast.LENGTH_LONG).show();
    }

    private void setValues() {
        for(TextInputEditText input : inputValues) {
            String text = input.getText().toString();
            if(text == null || text.isEmpty()) {
                continue;
            }
            int id = input.getId();
            System.out.println("***Here is the ID: " + id);
            switch(id) {
                case R.id.num_scans:
                    bluetooth_le_adapter.sendMessage("PEAKS="+text);
                    bluetooth_le_adapter.sendMessage("PEAKS?");
                    break;
                case R.id.freq_start:
                    bluetooth_le_adapter.sendMessage("FREQSTART="+text);
                    bluetooth_le_adapter.sendMessage("FREQSTART?");
                    break;
                case R.id.freq_end:
                    bluetooth_le_adapter.sendMessage("FREQEND="+text);
                    bluetooth_le_adapter.sendMessage("FREQEND?");
                    break;
                case R.id.first_pass:
                    bluetooth_le_adapter.sendMessage("FREQ1INC="+text);
                    bluetooth_le_adapter.sendMessage("FREQ1INC?");
                    break;
                case R.id.second_pass:
                    bluetooth_le_adapter.sendMessage("FREQ2INC="+text);
                    bluetooth_le_adapter.sendMessage("FREQ2INC?");
                    break;
                case R.id.third_pass:
                    bluetooth_le_adapter.sendMessage("FREQ3INC="+text);
                    bluetooth_le_adapter.sendMessage("FREQ3INC?");
                    break;
                case R.id.fourth_pass:
                    bluetooth_le_adapter.sendMessage("FREQ4INC="+text);
                    bluetooth_le_adapter.sendMessage("FREQ4INC?");
                    break;
                case R.id.freq2offset:
                    bluetooth_le_adapter.sendMessage("FREQ2OFFSET="+text);
                    bluetooth_le_adapter.sendMessage("FREQ2OFFSET?");
                    break;
                case R.id.freq3offset:
                    bluetooth_le_adapter.sendMessage("FREQ3OFFSET="+text);
                    bluetooth_le_adapter.sendMessage("FREQ3OFFSET?");
                    break;
                case R.id.freq4offset:
                    bluetooth_le_adapter.sendMessage("FREQ4OFFSET="+text);
                    bluetooth_le_adapter.sendMessage("FREQ4OFFSET?");
                    break;
            }
            input.setText("");
        }

    }

    private void updateValues() throws InterruptedException {
        bluetooth_le_adapter.sendMessage("PEAKS?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQSTART?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQEND?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ1INC?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ2INC?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ3INC?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ4INC?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ2OFFSET?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ3OFFSET?");
        Thread.sleep(100);
        bluetooth_le_adapter.sendMessage("FREQ4OFFSET?");
        Thread.sleep(100);
        getVoltage();
        Thread.sleep(100);
    }

    private void getVoltage() {
        bluetooth_le_adapter.sendMessage("BATTERY");
    }
}
