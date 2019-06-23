package com.tyrata.tyrata.ui.settings;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.remote.BleAdapterService;
import com.tyrata.tyrata.ui.SensorDataActivity;

import static com.tyrata.tyrata.data.Constants.CANCEL;
import static com.tyrata.tyrata.data.Constants.NO;
import static com.tyrata.tyrata.data.Constants.OK;
import static com.tyrata.tyrata.data.Constants.SEND;
import static com.tyrata.tyrata.data.Constants.SENSOR_MAC;
import static com.tyrata.tyrata.data.Constants.SENSOR_NAME;
import static com.tyrata.tyrata.data.Constants.YES;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AD7747SettingsActivity extends Activity {
    private final static String TAG = "Sensor Info";

    private final static String ZEROIZE_TITLE = "Are you sure?";
    private final static String ZEROIZE_BODY = "This will zero the capacitance";

    private final static String READREG_TITLE = "Read Register";
    private final static String READREG_BODY = "What register would you like to read? (In Hex)";

    private final static String SETREG_TITLE = "Set Register Value (In Hex)";
    private final static String SETREG_POS_BTN = "Set Hex Values";
    private final static String SETREG_HINT1 = "Register Number";
    private final static String SETREG_HINT2 = "Register Value";

    private final static String SETCAPDACA_TITLE = "Set CapDacA";
    private final static String SETCAPDACA_BODY = "Value to set 6 LSBs of Cap Dac A (In Hex).. Max Value is 3F";
    private final static String SETCAPDACA_TITLE2 = "Warning";
    private final static String SETCAPDACA_BODY2 = "Value must be between 0 and 3F";

    public static TextView regReadVal;
    public static TextView regSetVal;
    public static TextView zeroCapVal;
    public static TextView voltageVal;
    public static TextView tempVal;
    public static TextView capDacAVal;
    public static int capDacMSB;
    private String device_name;
    private String device_address;
    private BleAdapterService bluetooth_le_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad7747_settings);

        initialize();

}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onBackPressed() {
        AD7747SettingsActivity.this.finish();
    }

    /**
     * Alert notification
     */
    private void zeroizePopUp() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(AD7747SettingsActivity.this);
        alert.setTitle(ZEROIZE_TITLE);
        alert.setMessage(ZEROIZE_BODY);
        alert.setPositiveButton(YES, (dialog, whichButton) -> {
                bluetooth_le_adapter.zeroize();
                zeroCapVal.setText(OK);
        });
        alert.setNegativeButton(NO, (dialog, whichButton) -> {
        });
        alert.show();
    }

    /**
     * Click on Read Register
     */
    private void readRegisterPopUp() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(AD7747SettingsActivity.this);
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alert.setView(input);
        alert.setTitle(READREG_TITLE);
        alert.setMessage(READREG_BODY);

        // Set up the buttons
        alert.setPositiveButton(SEND, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String x = input.getText().toString();
                bluetooth_le_adapter.requestReg(x);
            }
        });
        alert.setNegativeButton(CANCEL, new DialogInterface.OnClickListener() {
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
        titleBox.setHint(SETREG_HINT1);
        layout.addView(titleBox);

        final EditText descriptionBox = new EditText(AD7747SettingsActivity.this);
        descriptionBox.setHint(SETREG_HINT2);
        layout.addView(descriptionBox);

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(SETREG_TITLE);
        alert.setView(layout).setPositiveButton(SETREG_POS_BTN,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        String x = titleBox.getText().toString();
                        String y = descriptionBox.getText().toString();
                        bluetooth_le_adapter.setReg(x, y);
                        regReadVal.setText(x);
                        regSetVal.setText(y);
                        dialog.cancel();
                    }
                }).setNegativeButton(CANCEL,
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
        bluetooth_le_adapter.requestVoltage();
    }

    private void updateTemp() {
        bluetooth_le_adapter.requestTemp();
    }

    // On update button clicked
    private void updateValue() {
        this.updateTemp();
        this.updateSleep(false);
        this.updateVoltage();
        bluetooth_le_adapter.zeroize();
        zeroCapVal.setText(OK);
        bluetooth_le_adapter.requestCapDacA();
    }

    private void updateSleep(boolean state) {
        if(state) {
            // send SLEEPON
            bluetooth_le_adapter.sleepOn();
        } else {
            // send SLEEPOFF
            bluetooth_le_adapter.sleepOff();
        }
        return;
    }

    public void readCapDac() {
        bluetooth_le_adapter.requestCapDacA();
    }

    public void setCapDac() {
        // Show alert box that asks to confirm deletion
        AlertDialog.Builder alert = new AlertDialog.Builder(AD7747SettingsActivity.this);
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        final AlertDialog dialog = new AlertDialog.Builder(AD7747SettingsActivity.this)
                .setView(input)
                .setTitle(SETCAPDACA_TITLE)
                .setMessage(SETCAPDACA_BODY)
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
                            bluetooth_le_adapter.setCapDacA(Integer.toHexString(capDacVal).toUpperCase());
                            readCapDac();
                            dialog.dismiss();
                        } else {
                            AlertDialog.Builder alert2 = new AlertDialog.Builder(AD7747SettingsActivity.this);
                            alert2.setTitle(SETCAPDACA_TITLE2);
                            alert2.setMessage(SETCAPDACA_BODY2);
                            alert2.setPositiveButton(OK, new DialogInterface.OnClickListener() {
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

    private void initialize() {
        setDeviceData();
        setToolbar();
        setButtons();
        setLabels();
    }

    private void setDeviceData() {
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(SENSOR_NAME);
        device_address = intent.getStringExtra(SENSOR_MAC);
        ((TextView) this.findViewById(R.id.device_address)).setText(device_address);
        ((TextView) this.findViewById(R.id.device_name)).setText(device_name);

        bluetooth_le_adapter = SensorDataActivity.bluetooth_le_adapter;
    }

    private void setToolbar() {
        setActionBar(new Toolbar(getApplicationContext()));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setButtons() {
        // Read Register
        Button readBtn = findViewById(R.id.read_register_btn);
        readBtn.setOnClickListener(view -> readRegisterPopUp());

        // Set Register
        Button setBtn = findViewById(R.id.set_register_btn);
        setBtn.setOnClickListener(view -> setRegisterPopUp());

        // Zero Capacitance
        Button zeroBtn = findViewById(R.id.zero_cap_btn);
        zeroBtn.setOnClickListener(view -> zeroizePopUp());

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
    }

    private void setLabels() {
        // Labels
        regReadVal = findViewById(R.id.register_value);

        regSetVal = findViewById(R.id.set_register_value);

        zeroCapVal = findViewById(R.id.zero_cap_value);

        voltageVal = findViewById(R.id.voltage_value);

        tempVal = findViewById(R.id.temp_value);

        capDacAVal = findViewById(R.id.capdacA_value);
    }
 }
