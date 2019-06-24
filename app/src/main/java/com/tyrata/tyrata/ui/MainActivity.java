package com.tyrata.tyrata.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import com.tyrata.tyrata.R;
import com.tyrata.tyrata.data.Constants;

import static com.tyrata.tyrata.util.CommonUtil.REQUEST_ENABLE_BT;

/**
 * Main Activity - First screen displayed after logging in
 * Starts BLE Service and has buttons to enter MEASUREMENT and DEMO modes
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Handler mHandler;

  //  private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        mAuth = FirebaseAuth.getInstance();
//        FirebaseUser user = mAuth.getCurrentUser();
      //  Log.d(TAG, "Logging in the USER + " + user);

        // Used for UI thread updates
        mHandler = new Handler();
        // Button to enter MEASUREMENT (beta-testing) mode
        Button freqResMeasBtn = findViewById(R.id.resonant_frequency_measurement);
        freqResMeasBtn.setOnClickListener(view -> switchToList(Constants.RF_MODE));
        // Button to enter DEVELOPMENT (no-sensor) mode
        Button adMeasBtn = findViewById(R.id.ad7747_measurement);
        adMeasBtn.setOnClickListener(view -> switchToList(Constants.AD7747_MODE));
        // Get Device's Bluetooth Adapter. Error if device doesn't support Bluetooth
        // @Todo Close app if Bluetooth is not supported
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;

        Button logoutBtn = findViewById(R.id.btn_logout);
//        logoutBtn.setOnClickListener(view -> {
//            if(mAuth.getCurrentUser()!=null) {
//                mAuth.signOut();
//                mAuth.addAuthStateListener(firebaseAuth -> {
//                    if (mAuth.getCurrentUser() == null) {
//                        Toast.makeText(
//                                getApplicationContext(),
//                                "You are now logged out!",
//                                Toast.LENGTH_SHORT).show();
//                        startActivity(new Intent(MainActivity.this, SignInActivity.class));
//                    }
//                });
//            } else {
//                Toast.makeText(
//                        getApplicationContext(),
//                        "You are now logged out!",
//                        Toast.LENGTH_SHORT).show();
//                startActivity(new Intent(MainActivity.this, SignInActivity.class));
//            }
//
//        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // @Todo Delete function after Demo
    @Override
    public void onBackPressed() { /* Do nothing */ }

    /**
     * Handles output from Bluetooth's ACTION_REQUEST_ENABLE
     * That is, called when "Turn on your bluetooth" dialog is accepted or cancelled
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) return;

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Start Vehicle Info Activity (VehicleInfoDisplayActivity.java)
     */
    public void switchToDemoMode(boolean isv2) {
        EditText input = new EditText(this);

        AlertDialog alert =
                new AlertDialog.Builder(this)
                        .setMessage("Enter passcode:")
                        .setView(input)
                        .setPositiveButton("OK", null)
                        .setNegativeButton("CANCEL", null)
                        .create();

        alert.setOnShowListener(dialogInterface -> {
            Button okBtn = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            okBtn.setOnClickListener(view1 -> {
                String passCode = input.getText().toString();
//                FirebaseDatabase.getInstance().getReference()
//                        .addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                if (dataSnapshot.child("passcode").getValue().equals(passCode)) {
//                                    // Authentication success, redirect to new activity
//                                    Intent intent;
//                                    if (!isv2) intent = new Intent(MainActivity.this,
//                                            VehicleInfoDisplayActivity.class);
//                                    else intent = new Intent(MainActivity.this,
//                                            Demov2.class);
//                                    intent.putExtra("VIN", "");
//                                    intent.putExtra("TIRE_COLOR",
//                                            CommonUtil.decodeMessage(BluetoothLeService.mMessage));
//                                    // Minimum one active device implies connection, so no alert
//                                    intent.putExtra("ALERT",
//                                            CommonUtil.activeDevices.size() < 1);
//                                    startActivity(intent);
//                                } else // passcode authentication failure
//                                    input.setError("Wrong passcode!");
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError databaseError) {
//                            }
//                        });
            });
        });
        alert.show();
    }

    public void switchToList(String mode) {
        SensorDataActivity.sensor_mode = mode;
        Intent i = new Intent(MainActivity.this, ListSensorActivity.class);
        startActivity(i);
    }
}