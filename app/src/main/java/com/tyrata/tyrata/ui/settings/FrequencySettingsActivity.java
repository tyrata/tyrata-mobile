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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;
import com.tyrata.tyrata.data.remote.BleAdapterService;
import com.tyrata.tyrata.ui.ListSensorActivity;
import com.tyrata.tyrata.ui.SensorDataActivity;

import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FrequencySettingsActivity extends AppCompatActivity {
    private final static String TAG = "Sensor Info";
    private final static String BLETIMEOUT_TOAST_TEXT = "Setting BLETIMEOUT to 2 hours...";
    private final static String RESET_SENSOR_TOAST_TEXT = "Resetting sensor...";
    private final static String A_OPT = "A";
    private final static String B_OPT = "B";
    private final static String FRANKLIN_OPT = "Franklin";
    private final static String CUSTOM = "Custom";

    public static TextView peaks;
    public static TextView freq_start;
    public static TextView freq_end;
    public static TextView freq_1_inc;
    public static TextView freq_2_inc;
    public static TextView freq_2_offset;
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
                case R.id.freq2offset:
                    bluetooth_le_adapter.setFreq2Offset(text);
                    bluetooth_le_adapter.requestFreq2Offset();
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
        setSpinner();
    }

    private void setSpinner() {
        Spinner spinner = findViewById(R.id.settings_selector);
        ArrayList<String> options = new ArrayList<>();
        options.add(CUSTOM);
        options.add(A_OPT);
        options.add(B_OPT);
        options.add(FRANKLIN_OPT);
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
                        break;
                    case 1:
                        sendA();
                        break;
                    case 2:
                        sendB();
                        break;
                    case 3:
                        sendFranklin();
                        break;
                    default:
                        // Do Nothing
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
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
        //TODO: Test
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setButtons(){
        Button voltageBtn = findViewById(R.id.get_voltage_btn);
        voltageBtn.setOnClickListener(view -> {
            bluetooth_le_adapter.requestVoltage();
            SensorDataActivity.isAfterReading = false;
        });

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
        TextInputEditText offset2 = findViewById(R.id.freq2offset);
        inputValues = new ArrayList<>();
        inputValues.add(peaks);
        inputValues.add(startFreq);
        inputValues.add(endFreq);
        inputValues.add(firstPass);
        inputValues.add(secondPass);
        inputValues.add(offset2);
    }

    private void setLabels(){
        peaks = findViewById(R.id.peaks_text);
        freq_start = findViewById(R.id.freq_start_text);
        freq_end = findViewById(R.id.freq_end_text);
        freq_1_inc = findViewById(R.id.first_pass_text);
        freq_2_inc = findViewById(R.id.second_pass_text);
        freq_2_offset = findViewById(R.id.offset2_text);
        voltageVal = findViewById(R.id.voltage_value_text);
    }
    //
    private void sendA() {
        bluetooth_le_adapter.setPeaks("1");
        bluetooth_le_adapter.requestPeaks();
        bluetooth_le_adapter.setFreqStart("5000000");
        bluetooth_le_adapter.requestFreqStart();
        bluetooth_le_adapter.setFreqEnd("25000000");
        bluetooth_le_adapter.requestFreqEnd();
        bluetooth_le_adapter.setFreq1Inc("100000");
        bluetooth_le_adapter.requestFreq1Inc();
        bluetooth_le_adapter.setFreq2Offset("100000");
        bluetooth_le_adapter.requestFreq2Offset();
        bluetooth_le_adapter.setFreq2Inc("1000");
        bluetooth_le_adapter.requestFreq2Inc();
    }

    private void sendB() {
        bluetooth_le_adapter.setPeaks("3");
        bluetooth_le_adapter.requestPeaks();
        bluetooth_le_adapter.setFreqStart("2000000");
        bluetooth_le_adapter.requestFreqStart();
        bluetooth_le_adapter.setFreqEnd("20000000");
        bluetooth_le_adapter.requestFreqEnd();
        bluetooth_le_adapter.setFreq1Inc("5000");
        bluetooth_le_adapter.requestFreq1Inc();
        bluetooth_le_adapter.setFreq2Offset("200000");
        bluetooth_le_adapter.requestFreq2Offset();
        bluetooth_le_adapter.setFreq2Inc("100");
        bluetooth_le_adapter.requestFreq2Inc();
    }

    private void sendFranklin() {
        bluetooth_le_adapter.setPeaks("1");
        bluetooth_le_adapter.requestPeaks();
        bluetooth_le_adapter.setFreq1Inc("5000");
        bluetooth_le_adapter.requestFreq1Inc();
        bluetooth_le_adapter.setFreq2Offset("5000");
        bluetooth_le_adapter.requestFreq2Offset();
        bluetooth_le_adapter.setFreq2Inc("500");
        bluetooth_le_adapter.requestFreq2Inc();
    }

}
