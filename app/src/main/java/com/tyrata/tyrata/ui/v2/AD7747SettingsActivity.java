package com.tyrata.tyrata.ui.v2;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;
import com.tyrata.tyrata.data.remote.v2.BleAdapterService;
import com.tyrata.tyrata.data.remote.v2.BleScanner;

import java.util.HashMap;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AD7747SettingsActivity extends Activity {
    private final static String TAG = "Sensor Info";

    public static TextView regReadVal;
    public static TextView regSetVal;
    public static TextView zeroCapVal;
    public static TextView voltageVal;
    public static TextView tempVal;
    public static TextView capDacAVal;
    public static int capDacMSB;

    private String type;
    private TextView mDataField;
    private String mMacAddress;
    private Handler mHandler;
    private Toast toast;
    private static final long SCAN_TIMEOUT = 5000;
    private boolean permissions_granted = false;
    private BleScanner ble_scanner;
    private long mReqTime;
    private String mRegisterString;
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_DATA = "data";
    private String device_name;
    private String device_address;
    private byte[] device_data;
    private boolean back_requested = false;
    private HashMap<String, String> lastReading;
    private BleAdapterService bluetooth_le_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad7747_settings);
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(EXTRA_NAME);
        device_address = intent.getStringExtra(EXTRA_ID);
        ((TextView) this.findViewById(R.id.device_address)).setText(device_address);
        ((TextView) this.findViewById(R.id.device_name)).setText(device_name);

        setActionBar(new Toolbar(getApplicationContext()));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        bluetooth_le_adapter = SensorDataActivity.bluetooth_le_adapter;
        // Read Register
        Button readBtn = findViewById(R.id.read_register_btn);
        readBtn.setOnClickListener(view -> readRegisterPopUp());

        // Set Register
        Button setBtn = findViewById(R.id.set_register_btn);
        setBtn.setOnClickListener(view -> setRegisterPopUp());

        // Zero Capacitance
        Button zeroBtn = findViewById(R.id.zero_cap_btn);
        zeroBtn.setOnClickListener(view -> showAlertNotification());

        // Get Time
        Button voltageBtn = findViewById(R.id.get_voltage_btn);
        voltageBtn.setOnClickListener(view -> updateVoltage());

        // Get Temp
        Button tempBtn = findViewById(R.id.get_temperature_btn);
        tempBtn.setOnClickListener(view -> updateTemp());

        // Sleep
        Switch sleepSwitch = findViewById(R.id.sleep_toggle);
        sleepSwitch.setOnCheckedChangeListener((view, state) -> updateSleep(state));

        // Read CapDacA
        Button readCapDacABtn = findViewById(R.id.read_capDacA_btn);
        readCapDacABtn.setOnClickListener(view -> readCapDac());

        // Read CapDacA
        Button setCapDacABtn = findViewById(R.id.set_capDacA_btn);
        setCapDacABtn.setOnClickListener(view -> setCapDac());

        // Update
        Button updateBtn = findViewById(R.id.update_values_btn);
        updateBtn.setOnClickListener(view -> updateValue());

        // Labels
        regReadVal = findViewById(R.id.register_value);

        regSetVal = findViewById(R.id.set_register_value);

        zeroCapVal = findViewById(R.id.zero_cap_value);

        voltageVal = findViewById(R.id.voltage_value);

        tempVal = findViewById(R.id.temp_value);

        capDacAVal = findViewById(R.id.capdacA_value);

}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void onResume() {
        super.onResume();
    }

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO: Add a functionality to display messages...
            }
        });
    }
    public void onBackPressed() {
//        Log.d(Constants.TAG, "onBackPressed");
        AD7747SettingsActivity.this.finish();
//        if (bluetooth_le_adapter.isConnected()) {
//            try {
//                bluetooth_le_adapter.disconnect();
//            } catch (Exception e) {
//            }
//        } else {
//            finish();
//        }
    }
    /**
     * Reset sensor readings
     */
    private void resetSensor() {
        // @Todo Display edit text here to get user specified value
        bluetooth_le_adapter.sendMessage("FACTORY");
        bluetooth_le_adapter.sendMessage("SENSOR=0");
        Toast.makeText(com.tyrata.tyrata.ui.v2.AD7747SettingsActivity.this,
                "Resetting sensor...", Toast.LENGTH_LONG).show();
    }

    /**
     * Alert notification
     */
    private void showAlertNotification() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(com.tyrata.tyrata.ui.v2.AD7747SettingsActivity.this);
        alert.setTitle("Are you sure?");
        alert.setMessage("This will zero the capacitance");
        alert.setPositiveButton("YES", (dialog, whichButton) -> {
                bluetooth_le_adapter.sendMessage("AD7747ZEROIZE");
                zeroCapVal.setText("OK");
        });
        alert.setNegativeButton("NO", (dialog, whichButton) -> {
        });
        alert.show();
    }

    /**
     * Click on Read Register
     */
    private void readRegisterPopUp() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(com.tyrata.tyrata.ui.v2.AD7747SettingsActivity.this);
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alert.setView(input);
        alert.setTitle("Read Register");
        alert.setMessage("What register would you like to read? (In Hex)");

        // Set up the buttons
        alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /**
                 * x = input.getText().toString();
                 *
                 * Request = AD7747REGx?
                 *
                 * Response should be AD7747REGx=y if x exists
                 * where y = value of register.
                 */
                String x = input.getText().toString();
                bluetooth_le_adapter.sendMessage("AD7747REG" + x + "?");
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

    /**
     * Click on Set Register
     */
    private void setRegisterPopUp() {
        LinearLayout layout = new LinearLayout(AD7747SettingsActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText titleBox = new EditText(AD7747SettingsActivity.this);
        titleBox.setHint("Register Number");
        layout.addView(titleBox);

        final EditText descriptionBox = new EditText(AD7747SettingsActivity.this);
        descriptionBox.setHint("Register Value");
        layout.addView(descriptionBox);

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Set Register Value (In Hex)");
        alert.setView(layout).setPositiveButton("Set Hex Values",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        /**
                         * Send the request to set a register with
                         * x = input1.getText().toString();
                         * y = input2.getText().toString();
                         *
                         * Request = AD7747REGx=y
                         *
                         */
                        String x = titleBox.getText().toString();
                        String y = descriptionBox.getText().toString();
                        bluetooth_le_adapter.sendMessage("AD7747REG"+x+"="+y);
                        regReadVal.setText(x);
                        regSetVal.setText(y);
                        dialog.cancel();
                    }
                }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alert.show();
    }

    // On temp button clicked
    private void updateVoltage() {
        bluetooth_le_adapter.sendMessage("BATTERY");
    }

    private void updateTemp() {
        bluetooth_le_adapter.sendMessage("TEMP");
    }

    // On update button clicked
    private void updateValue() {
        this.updateTemp();
        this.updateSleep(false);
        this.updateVoltage();
        bluetooth_le_adapter.sendMessage("AD7747ZEROIZE");
        zeroCapVal.setText("OK");
        //TODO: Still do this????
        bluetooth_le_adapter.sendMessage("AD7747REG0B?");
    }

    private void updateSleep(boolean state) {
        if(state) {
            // send SLEEPON
            bluetooth_le_adapter.sendMessage("SLEEPON");
        } else {
            // send SLEEPOFF
            bluetooth_le_adapter.sendMessage("SLEEPOFF");
        }
        return;
    }

    public void readCapDac() {
        bluetooth_le_adapter.sendMessage("AD7747REG0B?");
    }

    public void setCapDac() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(com.tyrata.tyrata.ui.v2.AD7747SettingsActivity.this);
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        final AlertDialog dialog = new AlertDialog.Builder(com.tyrata.tyrata.ui.v2.AD7747SettingsActivity.this)
                .setView(input)
                .setTitle("Set CapDacA")
                .setMessage("Value to set 6 LSBs of Cap Dac A (In Hex).. Max Value is 3F")
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button b = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (input.getText().length() <= 0) {
                            return;
                        }
                        try {

                        } catch (Exception e) {

                        }
                        int value = Integer.parseInt(input.getText().toString(), 16);
                        if (value <= 63 && value >= 0) {
                            String x = input.getText().toString();
                            int capDacVal = capDacMSB | Integer.parseInt(x, 16);
                            bluetooth_le_adapter.sendMessage("AD7747REG0B=" + Integer.toHexString(capDacVal).toUpperCase());
                            readCapDac();
                            dialog.dismiss();
                        } else {
                            AlertDialog.Builder alert2 = new AlertDialog.Builder(com.tyrata.tyrata.ui.v2.AD7747SettingsActivity.this);
                            alert2.setTitle("Warning");
                            alert2.setMessage("Value must be between 0 and 3F");
                            alert2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog2, int which) {
                                    dialog2.cancel();
                                }
                            });
                            alert2.show();
                        }
                    }
                });
            }
        });

        dialog.show();
    }
 }
