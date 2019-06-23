package com.tyrata.tyrata.data.remote;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
import com.tyrata.tyrata.data.model.Sensor;
import com.tyrata.tyrata.data.model.v2.Reading;
import com.tyrata.tyrata.util.CommonUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device/sensor.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    // @Todo Hardcoded MANUFACTURER_ID. Change to match Tyrata sensor's ManufacturerID
    private static final int MANUFACTURER_ID = 0x0059;

    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String ACTION_DEVICE_FOUND = "ACTION_DEVICE_FOUND";
    public final static String ACTION_INACTIVITY_FOUND = "ACTION_INACTIVITY_FOUND";

    // Variable to remove devices idle for more than 10s
    private static final long INACTIVITY_THRESHOLD = 10;
    private static final String DEVICE_DOES_NOT_SUPPORT_UART = "Don't support it";
    private static final String UUID_SPS_SERVER_RX = "K";
    // Variable to check for inactive devices every minute (5 sec)
    private static long INACTIVITY_SCAN_INTERVAL = 5000;

    public static String mMessage = "-1";
    public static String mConnectToMac = "";

    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBluetoothAdapter;

    // Nordic's UUIDs
    /** Nordic UART Service UUID */
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    /** RX characteristic UUID */
    private final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    /** TX characteristic UUID */
    private final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private BluetoothGattCharacteristic mRXCharacteristic, mTXCharacteristic;

    private boolean mUseLongWrite = true;

    private String mMacAddress;
    private Handler handler = new Handler();

    // Firebase variables
//    private FirebaseAuth mAuth;
//    private FirebaseUser mUser;
//    private DatabaseReference mSensorMetaRef;

    // Creates a binder object; used by other activities to bind to this service
    private final IBinder mBinder = new LocalBinder();

    // Return binder object to the activity that creates/binds to this service
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // Unbind this service from activity (usually when activity is destroyed)
    @Override
    public boolean onUnbind(Intent intent) { return super.onUnbind(intent); }

    /**
     * Broadcast new data to the listener classes (VehicleInfo and TireInfo)
     * ACTION_DEVICE_FOUND: New BLE Device found
     * ACTION_DATA_AVAILABLE: Data available from sensor
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        if (action.equals(ACTION_DATA_AVAILABLE)) intent.putExtra(ACTION_DATA_AVAILABLE, mMessage);
        sendBroadcast(intent);
    }

    /**
     * Check if the devices are active or inactive
     */
    private void findActiveDevices() {
       // Log.d(TAG, "Checking for Active devices!");
        boolean inactiveDevicesFound = false;
        for (Iterator<Map.Entry<String, Pair<Long, com.tyrata.tyrata.data.model.v2.Reading>>> it =
             CommonUtil.activeDevices.entrySet().iterator(); it.hasNext(); ) {
            // Get the sensor info entry in the map
            Map.Entry<String, Pair<Long, Reading>> entry = it.next();
            // Calculate inactivity time (in seconds) using sensor's latest reading timestamp
            long timeElapsed = (System.currentTimeMillis() - entry.getValue().first) / 1000;

            Log.d(TAG, "Time elapsed for device: " + timeElapsed);
            // Check if the inactivity time is long enough
            if (timeElapsed >= INACTIVITY_THRESHOLD) {
                inactiveDevicesFound = true;
                Log.d(TAG, "Removing device " + entry.getKey());
        //        mSensorMetaRef.child(mMacAddress).child("isActive").setValue(false);
                CommonUtil.inactiveDevices.add(entry.getKey()); // Add sensor to inactive devices list
                it.remove(); // Remove sensor from active devices list
            }
        }
        if (inactiveDevicesFound) broadcastUpdate(ACTION_INACTIVITY_FOUND);
    }

    /**
     * @Todo Implement this function to connect to the device with mMacAddress
     * and set/read characteristics/descriptors
     */
    public static void connectToSensor(String mMacAddress) {
        mConnectToMac = mMacAddress;
    }

    /**
     * Start scanning for BLE devices
     */
    public void startScan() {
        mScanCallback = new BleScanCallback();

        mBluetoothLeScanner = mBluetoothAdapter != null ? mBluetoothAdapter.getBluetoothLeScanner() : null;

        // @Todo Not filtering by UUID, but by Device address. Maybe filter by UUID later?
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UART_SERVICE_UUID))
                .build();
        filters.add(scanFilter);

        // Low power scanning (to save battery)
        ScanSettings settings = new ScanSettings.Builder()
//                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
                .build();
//        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mBluetoothLeScanner.startScan(mScanCallback);

        // Check for active and inactive sensors every INACTIVITY_SCAN_INTERVAL ms
        handler.post(new Runnable() {
            @Override
            public void run() {
                findActiveDevices();
                handler.postDelayed(this, INACTIVITY_SCAN_INTERVAL);
            }
        });
    }

//    /**
//     * Find the service that has the SPS UUID
//     */
    private static BluetoothGattService findService(List<BluetoothGattService> serviceList) {
        for (BluetoothGattService service : serviceList) {
            String serviceIdString = service.getUuid()
                    .toString();
            Log.d(TAG, "SERVICE FOUND" + serviceIdString);
            if (matchesServiceUuidString(serviceIdString)) {
                return service;
            }
        }
        return null;
    }

//    public void destroyService() {
//        // Set all users devices to inactive
//        mSensorMetaRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for (DataSnapshot snap : dataSnapshot.getChildren()) {
//                    snap.child("isActive").getRef().setValue(false);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//            }
//        });
//
//        handler.removeCallbacksAndMessages(null);
//        CommonUtil.activeDevices.clear();
//        CommonUtil.inactiveDevices.clear();
//        mBluetoothLeScanner.stopScan(mScanCallback);
//        mScanCallback = null;
//        mAuth = null;
//        mUser = null;
//
//        stopSelf();
//    }

    /**
     * Returns current service instance for listeners to use (using the mBinder variable)
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    /**
     * Check if the Service UUID of sensor matches
     */
    private static boolean matchesServiceUuidString(String serviceIdString) {
        return uuidMatches(serviceIdString, UART_SERVICE_UUID.toString());
    }

    /**
     * Check if the Receiver characteristic UUID of sensor matches
     */
    private static boolean characteristicMatches(BluetoothGattCharacteristic characteristic, UUID uuidString) {
        if (characteristic == null) {
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return uuidMatches(uuid.toString(), uuidString.toString());
    }

    private static boolean uuidMatches(String uuidString, String match) {
        return uuidString.equalsIgnoreCase(match);
    }

    /**
     * Initializes Firebase variables
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
//    public boolean initialize() {
//        mAuth = FirebaseAuth.getInstance(); // Get the shared instance of the FirebaseAuth object
//        mUser = mAuth.getCurrentUser(); // Get Current User
//        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
//        if(mUser == null) {
//            return false;
//        }
//        // Get reference to the "sensors" node in the JSON tree (See Firebase console for more info)
//        mSensorMetaRef = mDatabase.getReference("users").child(mUser.getUid()).child("sensors");
//        Log.d(TAG, "BLE Service Current User: " + mUser);
//
//        BluetoothManager mBluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        if (mBluetoothManager == null) {
//            Log.e(TAG, "Unable to initialize BluetoothManager.");
//            return false;
//        }
//
//        mBluetoothAdapter = mBluetoothManager.getAdapter();
//        if (mBluetoothAdapter == null) {
//            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
//            return false;
//        }
//
//        return mBluetoothAdapter.isEnabled();
//    }

//    /**
//     * Find characteristic associated with the command, that is, with a RX UUID
//     */
    private BluetoothGattCharacteristic findCharacteristic(BluetoothGatt gatt) {
        List<BluetoothGattService> serviceList = gatt.getServices();
        BluetoothGattService service = findService(serviceList);
        if (service == null) {
            Log.d(TAG, "NO SERVICES FOUND");
            return null;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList) {
            if (characteristicMatches(characteristic, UART_RX_CHARACTERISTIC_UUID)) {
                return characteristic;
            }
        }
        return null;
    }
//    /**
//     * Add device to the paired (or new) device list and to the local DB
//     */
//    private boolean addScanResult(BluetoothDevice device,
//                                  int reading,
//                                  int differenceFromInitialValue,
//                                  long elapsedTimeSincePowerOn,
//                                  int batteryPowerRemaining) {
//        String macAddress = device.getAddress();
//        String deviceName = device.getName();
//        boolean isNewReading = true;
//        long currentTime = System.currentTimeMillis();
//
//        // Check if the found device is already stored
//        if (!CommonUtil.activeDevices.containsKey(macAddress)) { // Device is now active
//            if (CommonUtil.inactiveDevices.contains(macAddress))
//                CommonUtil.inactiveDevices.remove(macAddress);
//
//            CommonUtil.activeDevices.put(macAddress, new Pair<>(currentTime,
//                    new Reading(reading,
//                            differenceFromInitialValue,
//                            elapsedTimeSincePowerOn,
//                            batteryPowerRemaining)));
//
//            Log.d(TAG, "Found device " + macAddress);
//            broadcastUpdate(ACTION_DEVICE_FOUND);
//        } // Update current time that indicates latest data received
//        else {
//            Reading currentReading = CommonUtil.activeDevices.get(macAddress).second;
//            if (currentReading.value == reading) // If data is same as previous, its not new
//                isNewReading = false;
//
//            // Make current data latest (along with the newest timestamp)
//            currentReading.value = reading;
//            currentReading.difference = differenceFromInitialValue;
//            currentReading.elapsedTime = elapsedTimeSincePowerOn;
//            currentReading.battery = batteryPowerRemaining;
//            CommonUtil.activeDevices.put(macAddress,
//                    new Pair<>(currentTime, currentReading));
//        }
//
//        // Add device to the remote database (Firebase)
//        mSensorMetaRef.child(macAddress).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (!snapshot.exists()) {
//                    // New device, add to DB with default name
//                    mSensorMetaRef
//                            .child(macAddress)
//                            .setValue(new Sensor(
//                                    macAddress,
//                                    deviceName,
//                                    reading + "",
//                                    true));
//                } else if (CommonUtil.activeDevices.containsKey(macAddress)) {
//                    // Device is active, so set active to true in DB (Firebase)
//                    mSensorMetaRef
//                            .child(macAddress)
//                            .child("isActive")
//                            .setValue(true);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//            }
//        });
//
//        return isNewReading;
//    }
    /**
     * Add device to the paired (or new) device list and to the local DB
     */
    private boolean addScanResultV2(BluetoothDevice device, long time, int voltage, int temp, long value, int type) {
        String macAddress = device.getAddress();
        String deviceName = device.getName();
        boolean isNewReading = true;
        long currentTime = System.currentTimeMillis();

        // Check if the found device is already stored
        if (!CommonUtil.activeDevices.containsKey(macAddress)) { // Device is now active
            if (CommonUtil.inactiveDevices.contains(macAddress))
                CommonUtil.inactiveDevices.remove(macAddress);
            com.tyrata.tyrata.data.model.v2.Reading reading = new com.tyrata.tyrata.data.model.v2.Reading(time, voltage, temp, value, type);
            CommonUtil.activeDevices.put(macAddress, new Pair<>(currentTime, reading));

            Log.d(TAG, "Found device " + macAddress);
            broadcastUpdate(ACTION_DEVICE_FOUND);
        } // Update current time that indicates latest data received
        else {
            com.tyrata.tyrata.data.model.v2.Reading currentReading = CommonUtil.activeDevices.get(macAddress).second;
            if (currentReading.value == value) // If data is same as previous, its not new
                isNewReading = false;

            // Make current data latest (along with the newest timestamp)
            currentReading.value = value;
            currentReading.voltage = voltage;
            currentReading.temp = temp;
            currentReading.time = time;
            CommonUtil.activeDevices.put(macAddress,
                    new Pair<>(currentTime, currentReading));
        }

        // Add device to the remote database (Firebase)
//        mSensorMetaRef.child(macAddress).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (!snapshot.exists()) {
//                    // New device, add to DB with default name
//                    mSensorMetaRef
//                            .child(macAddress)
//                            .setValue(new Sensor(
//                                    macAddress,
//                                    deviceName,
//                                    value + "",
//                                    true));
//                } else if (CommonUtil.activeDevices.containsKey(macAddress)) {
//                    // Device is active, so set active to true in DB (Firebase)
//                    mSensorMetaRef
//                            .child(macAddress)
//                            .child("isActive")
//                            .setValue(true);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//            }
//        });

        return isNewReading;
    }
    /**
     * Parse data-packet for data and MAC Address
     */
    private void parseDataPacket(BluetoothDevice device, byte[] data) {
        // Data packet format:
        // 2 bytes reading, 2 bytes difference, 4 bytes elapsed time, 1 byte battery

//        StringBuilder sb = new StringBuilder();
//        for (byte b : data) {
//            sb.append(Integer.toBinaryString(b & 255 | 256).substring(1));
//        }
//        Log.d(TAG, "BITS : " + data.length + " " + sb.toString());
//        long elapsedTimeSincePowerOn = ((data[3] & 0xff) << 24) |
//                ((data[2] & 0xff) << 16) |
//                ((data[1] & 0xff) << 8) |
//                (data[0] & 0xff);
//        int differenceFromInitialValue = ((data[5] & 0xff) << 8) | (data[4] & 0xff);
//        int reading = ((data[7] & 0xff) << 8) | (data[6] & 0xff);
//        int batteryPowerRemaining = (data[8] & 0xff);
//        Log.d(TAG, "reading : " + reading);
//        Log.d(TAG, "differenceFromInitialValue : " + differenceFromInitialValue);
//        Log.d(TAG, "elapsedTimeSincePowerOn : " + elapsedTimeSincePowerOn);
//        Log.d(TAG, "batteryPowerRemaining : " + batteryPowerRemaining);
//        boolean isNewReading = addScanResult(device,
//                reading,
//                differenceFromInitialValue,
//                elapsedTimeSincePowerOn,
//                batteryPowerRemaining);
        /**
         * New data packet format::
         * bits 0-3: Capacitance = 0 or Frequency = 1
         *
         * bits 4-27:
         *                Capacitance (pF) - data * 8.192/(2^24)
         *                      5F32AB = 6238891 * 8.192pF/(2^24) = 3.0463pF
         *                Frequency (kHz)
         *                      00909C = 37020 kHz
         * bits 28-39: Temperature in C (2's Complement)
         *              Check MSB, if 1, it's negative, so flip bits and add one to get value.
         *
         *              E70h since msb is 1 it is negative, take 2s compliment+1 to get temperature
         *                      E70 2s compliment+1 = 18F + 1 = 190 = 400 then times .0625C = 25C, but since negative it is -25C
         *
         * bits 40-47: Battery voltage in deciVolts
         *              i.e. 15 = 21 deciVolts (2.1 Volts)
         *
         * bits 48-71: Time since power on in seconds
         *              i.e. 00012C = 300 sec. (5 min.)
         */
        long time = ((data[2] & 0xff) << 16) |
                ((data[1] & 0xff) << 8) |
                (data[0] & 0xff);
        int voltage = ((data[3] & 0xff));
        int temperature = ((data[4] & 0xff) << 8) | ((data[5] & 0x0f));
        long value = ((data[5] & 0xf0) << 16) | ((data[6] & 0xff) << 12) | ((data[7] & 0xff) << 4) | (data[8] & 0x0f);
        int type = (data[8] & 0xf0);

        Log.d(TAG, "value : " + value);
        Log.d(TAG, "voltage : " + voltage);
        Log.d(TAG, "time : " + time);
        Log.d(TAG, "temp : " + temperature);
        Log.d(TAG, "type : " + type);

        mMessage = value + "";

        boolean isNewReading = addScanResultV2(device, time, voltage, temperature, value, type);

//
//        // @Todo Set multiple sensors for different tires
//        // @Todo Multiple sensors (with same ID) will cause problems in Demo mode
//        // Notify the listeners that new data is available
        if (isNewReading) {
            broadcastUpdate(ACTION_DATA_AVAILABLE);
        }
    }

    /**
     * Callback to handle BLE scan results
     */
    private class BleScanCallback extends ScanCallback {

        /**
         * Callback when a BLE advertisement has been found.
         * Parses the advertisement and adds device to the found-devices list
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null)
                Log.d(TAG, "Device scanned : " + result.getDevice().getName() + ":" + result.getScanRecord().getManufacturerSpecificData());
            SparseArray<byte[]> data =
                    Objects.requireNonNull(result.getScanRecord()).getManufacturerSpecificData();

            if (result.getDevice().getName() != null &&
                    result.getDevice().getName().startsWith("TYRATA") &&
                    data != null) {
                int key = data.keyAt(0);
                byte[] dataPacket = data.get(key);
//                Log.d(TAG, "MANF DATA "+ data);
                // IMPORTANT: The BLE device's address MUST BE PUBLIC
                mMacAddress = result.getDevice().getAddress();

                // Check if RESET button is clicked
                if (!mConnectToMac.equals("") && mConnectToMac.equals(mMacAddress)) {
                    // Stop inactivity-device-searching-handler first and start new scan
                    handler.removeCallbacksAndMessages(null);

                    // Stop parsing more data packets by setting this string to empty
                    mConnectToMac = "";


//                    GattClientCallback gattClientCallback = new GattClientCallback();
//                    mGatt = result.getDevice().connectGatt(BluetoothLeService.this,
//                            false, gattClientCallback);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }

//                mBluetoothLeScanner.stopScan(mScanCallback);
//                GattClientCallback gattClientCallback = new GattClientCallback();
//                mGatt = result.getDevice().connectGatt(BluetoothLeService.this,
//                        false, gattClientCallback);
                parseDataPacket(result.getDevice(), dataPacket);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }

    }

//    /**
//     * Call back to handle GATT server state
//     */
//    private class GattClientCallback extends BluetoothGattCallback {
//
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
//
//            if (status != BluetoothGatt.GATT_SUCCESS) {
//                // handle anything not SUCCESS as failure
//                Log.d(TAG, "GATT failure status " + status);
//                disconnectGattServer();
//                return;
//            }
//
//            if (newState == BluetoothProfile.STATE_CONNECTED) mTXCharacteristic.discoverServices();
//            else if (newState == BluetoothProfile.STATE_DISCONNECTED) disconnectGattServer();
//        }
//
//        //
//        @Override
//        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
//
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d(TAG, "Device service discovery successful, status " + status);
//                enableTXNotification();
////                return;
//            }
//
//            Log.d(TAG, "Device service discovery successful, status " + status);
//            // @Todo Handle sending multiple commands in the future
//            BluetoothGattCharacteristic characteristic = findCharacteristic(mRXCharacteristic);
//            if (characteristic != null) { // Found the characteristic to write to
//                Log.d(TAG, "Characteristic found! " + characteristic);
//                characteristic.setValue("RESET".getBytes()); // Change to required command here
//                boolean success = mRXCharacteristic.writeCharacteristic(characteristic);
//                if (success) { //Characteristic written successfully
//                    Log.d(TAG, "Sent RESET COMMAND successfully");
//                } else {
//                    Log.d(TAG, "Failed to send RESET COMMAND");
//                }
//            } else Log.d(TAG, "Characteristic NOT found! ");
//
//            disconnectGattServer();
//        }
//
//        /**
//         * Enable Notification on TX characteristic
//         *
//         * @return
//         */
//        public void enableTXNotification() {
//
//    	if (mBluetoothGatt == null) {
//    		showMessage("mBluetoothGatt null" + mBluetoothGatt);
//    		broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
//    		return;
//    	}
//
//            BluetoothGattService RxService = mGatt.getService(UART_SERVICE_UUID);
//            if (RxService == null) {
//                showMessage("Rx service not found!");
//                broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
//                return;
//            }
//            BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(UART_TX_CHARACTERISTIC_UUID);
//            if (TxChar == null) {
//                showMessage("Tx charateristic not found!");
//                broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
//                return;
//            }
//            mGatt.setCharacteristicNotification(TxChar, true);
//
//            BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//            mGatt.writeDescriptor(descriptor);
//
//        }
//
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic characteristic,
//                                         int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                if (UART_TX_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
//                    Log.d(TAG, String.format("Received TX: %d", characteristic.getValue()));
////                    intent.putExtra(EXTRA_DATA, characteristic.getValue());
//                }
//            }
//        }
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt,
//                                            BluetoothGattCharacteristic characteristic) {
//            if (UART_TX_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
//                String text = new String(characteristic.getValue());
//                Log.d(TAG, "Received TX: " + text);
//                intent.putExtra(EXTRA_DATA, characteristic.getValue());
//            }
//        }
//        /**
//         * Disconnect from GATT server
//         */
//        private void disconnectGattServer() {
//            if (mGatt != null) {
//                mGatt.disconnect();
//                mGatt.close();
//            }
////            startScan();
//        }
//
//    }
}