package com.tyrata.tyrata.ui.settings;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;
import com.tyrata.tyrata.data.remote.BleAdapterService;
import com.tyrata.tyrata.ui.SensorDataActivity;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FrequencySettingsActivity extends AppCompatActivity {
    private final static String TAG = "Sensor Info";
    private final static String BLETIMEOUT_TOAST_TEXT = "Setting BLETIMEOUT to 2 hours...";
    private final static String RESET_SENSOR_TOAST_TEXT = "Resetting sensor...";

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

    private ArrayList<TextInputEditText> inputValues;

    private String device_address;
    private String device_name;
    private BleAdapterService bluetooth_le_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequency_settings);

        initialize();
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
        bluetooth_le_adapter.factory();
        bluetooth_le_adapter.setSensor("1");
        Toast.makeText(FrequencySettingsActivity.this,
                RESET_SENSOR_TOAST_TEXT, Toast.LENGTH_LONG).show();
    }

    private void setValues() {
        for(TextInputEditText input : inputValues) {

            String text = input.getText().toString();

            if(text == null || text.isEmpty()) {
                continue;
            }

            int id = input.getId();

            switch(id) {
                case R.id.peaks:
                    bluetooth_le_adapter.setPeaks(text);
                    bluetooth_le_adapter.requestPeaks();
                    break;
                case R.id.freq_start:
                    bluetooth_le_adapter.setFreqStart(text);
                    bluetooth_le_adapter.requestFreqStart();
                    break;
                case R.id.freq_end:
                    bluetooth_le_adapter.setFreqEnd(text);
                    bluetooth_le_adapter.requestFreqEnd();
                    break;
                case R.id.first_pass:
                    bluetooth_le_adapter.setFreq1Inc(text);
                    bluetooth_le_adapter.requestFreq1Inc();
                    break;
                case R.id.second_pass:
                    bluetooth_le_adapter.setFreq2Inc(text);
                    bluetooth_le_adapter.requestFreq2Inc();
                    break;
                case R.id.third_pass:
                    //bluetooth_le_adapter.setFreq3Inc(text);
                    //bluetooth_le_adapter.requestFreq3Inc();
                    break;
                case R.id.fourth_pass:
                    //bluetooth_le_adapter.setFreq4Inc(text);
                    //bluetooth_le_adapter.requestFreq4Inc();
                    break;
                case R.id.freq2offset:
                    bluetooth_le_adapter.setFreq2Offset(text);
                    bluetooth_le_adapter.requestFreq2Offset();
                    break;
                case R.id.freq3offset:
                   //bluetooth_le_adapter.setFreq3Offset(text);
                   // bluetooth_le_adapter.requestFreq3Offset();
                    break;
                case R.id.freq4offset:
                    //bluetooth_le_adapter.setFreq4Offset(text);
                    //bluetooth_le_adapter.requestFreq4Offset();
                    break;
            }
            input.setText("");
        }
    }

    private void updateValues() {
        bluetooth_le_adapter.requestPeaks();
        bluetooth_le_adapter.requestFreqStart();
        bluetooth_le_adapter.requestFreqEnd();
        bluetooth_le_adapter.requestFreq1Inc();
        bluetooth_le_adapter.requestFreq2Inc();
        bluetooth_le_adapter.requestFreq2Offset();
        bluetooth_le_adapter.requestVoltage();

        /*
        // OLD Commands, no longer used
        bluetooth_le_adapter.requestFreq3Inc();
        bluetooth_le_adapter.requestFreq4Inc();
        bluetooth_le_adapter.requestFreq3Offset();
        bluetooth_le_adapter.requestFreq4Offset();
         */
    }

    /**
    Initialization
     */
    private void initialize() {
        setDeviceData();
        setToolbar();
        setButtons();
        setInputs();
        setLabels();
    }

    private void setDeviceData() {
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(Constants.SENSOR_NAME);
        device_address = intent.getStringExtra(Constants.SENSOR_MAC);
        ((TextView) this.findViewById(R.id.device_address)).setText(device_address);
        ((TextView) this.findViewById(R.id.device_name)).setText(device_name);
        bluetooth_le_adapter = SensorDataActivity.bluetooth_le_adapter;
    }
    private void setToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setButtons(){
        Button voltageBtn = findViewById(R.id.get_voltage_btn);
        voltageBtn.setOnClickListener(view -> bluetooth_le_adapter.requestVoltage());

        Button updateBtn = findViewById(R.id.update_values_btn);
            updateBtn.setOnClickListener(view -> {
            updateValues();
        });

        Button setBtn = findViewById(R.id.set_values_btn);
        setBtn.setOnClickListener(view -> setValues());

        Button bleBtn = findViewById(R.id.ble_timeout_btn);
        bleBtn.setOnClickListener(view -> {
            bluetooth_le_adapter.setBleTimeout("120");
            Toast.makeText(FrequencySettingsActivity.this,
                    BLETIMEOUT_TOAST_TEXT, Toast.LENGTH_SHORT).show();
            Log.d("", "BLETIMEOUT set to 2 hours.");
        });
    }

    private void setInputs(){
        TextInputEditText peaks = findViewById(R.id.peaks);
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
        inputValues.add(peaks);
        inputValues.add(startFreq);
        inputValues.add(endFreq);
        inputValues.add(firstPass);
        inputValues.add(secondPass);
        inputValues.add(thirdPass);
        inputValues.add(fourthPass);
        inputValues.add(offset2);
        inputValues.add(offset3);
        inputValues.add(offset4);
    }

    private void setLabels(){
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
    //
}
