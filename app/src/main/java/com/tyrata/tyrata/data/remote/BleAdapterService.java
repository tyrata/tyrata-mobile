package com.tyrata.tyrata.data.remote;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.tyrata.tyrata.data.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tyrata.tyrata.data.Commands.AD7747_REG;
import static com.tyrata.tyrata.data.Commands.HZ;
import static com.tyrata.tyrata.data.Commands.REQUEST;
import static com.tyrata.tyrata.data.Commands.REQUEST_BLETIMOUT;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ1INC;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ2INC;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ2OFFSET;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ3INC;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ3OFFSET;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ4INC;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQ4OFFSET;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQEND;
import static com.tyrata.tyrata.data.Commands.REQUEST_FREQSTART;
import static com.tyrata.tyrata.data.Commands.REQUEST_ID;
import static com.tyrata.tyrata.data.Commands.REQUEST_PEAKS;
import static com.tyrata.tyrata.data.Commands.REQUEST_SENSOR;
import static com.tyrata.tyrata.data.Commands.REQUEST_VOLTAGE;
import static com.tyrata.tyrata.data.Commands.SCAN;
import static com.tyrata.tyrata.data.Commands.SET;
import static com.tyrata.tyrata.data.Commands.SET_BLETIMEOUT;
import static com.tyrata.tyrata.data.Commands.SET_FACTORY;
import static com.tyrata.tyrata.data.Commands.SET_FREQ1INC;
import static com.tyrata.tyrata.data.Commands.SET_FREQ2INC;
import static com.tyrata.tyrata.data.Commands.SET_FREQ2OFFSET;
import static com.tyrata.tyrata.data.Commands.SET_FREQ3INC;
import static com.tyrata.tyrata.data.Commands.SET_FREQ3OFFSET;
import static com.tyrata.tyrata.data.Commands.SET_FREQ4INC;
import static com.tyrata.tyrata.data.Commands.SET_FREQ4OFFSET;
import static com.tyrata.tyrata.data.Commands.SET_FREQEND;
import static com.tyrata.tyrata.data.Commands.SET_FREQSTART;
import static com.tyrata.tyrata.data.Commands.SET_ID;
import static com.tyrata.tyrata.data.Commands.SET_PEAKS;
import static com.tyrata.tyrata.data.Commands.SET_SENSOR;
import static com.tyrata.tyrata.data.Commands.SLEEP_OFF;
import static com.tyrata.tyrata.data.Commands.SLEEP_ON;
import static com.tyrata.tyrata.data.Commands.TEMP;
import static com.tyrata.tyrata.data.Commands.ZEROIZE;
import static com.tyrata.tyrata.data.Constants.CAPDACA;

public class BleAdapterService extends Service {
    private BluetoothAdapter bluetooth_adapter;
    private BluetoothGatt bluetooth_gatt;
    private BluetoothManager bluetooth_manager;
    private Handler activity_handler = null;
    private BluetoothDevice device;
    private BluetoothGattDescriptor descriptor;
    private boolean connected = false;
    public boolean alarm_playing = false;
    // messages sent back to activity
    public static final int GATT_CONNECTED = 1;
    public static final int GATT_DISCONNECT = 2;
    public static final int GATT_SERVICES_DISCOVERED = 3;
    public static final int GATT_CHARACTERISTIC_READ = 4;
    public static final int GATT_CHARACTERISTIC_WRITTEN = 5;
    public static final int GATT_REMOTE_RSSI = 6;
    public static final int MESSAGE = 7;
    public static final int NOTIFICATION_OR_INDICATION_RECEIVED = 8;
    // message parms
    public static final String PARCEL_DESCRIPTOR_UUID = "DESCRIPTOR_UUID";
    public static final String PARCEL_CHARACTERISTIC_UUID = "CHARACTERISTIC_UUID";
    public static final String PARCEL_SERVICE_UUID = "SERVICE_UUID";
    public static final String PARCEL_VALUE = "VALUE";
    public static final String PARCEL_RSSI = "RSSI";
    public static final String PARCEL_TEXT = "TEXT";

    public static String IMMEDIATE_ALERT_SERVICE_UUID = "00001802-0000-1000-8000-00805F9B34FB";
    //    public static String LINK_LOSS_SERVICE_UUID = "00001803-0000-1000-8000-00805F9B34FB";
//    public static String LINK_LOSS_SERVICE_UUID = "00001801-0000-1000-8000-00805F9B34FB";
    //   public static String LINK_LOSS_SERVICE_UUID = "00001800-0000-1000-8000-00805F9B34FB";
    public static String LINK_LOSS_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String TX_POWER_SERVICE_UUID = "00001804-0000-1000-8000-00805F9B34FB";
    public static String PROXIMITY_MONITORING_SERVICE_UUID = "3E099910-293F-11E4-93BD-AFD0FE6D1DFD";
    public static String HEALTH_THERMOMETER_SERVICE_UUID = "00001809-0000-1000-8000-00805F9B34FB";
    public static String NORDIC_UART_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String TX_UUID   = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String RX_UUID   = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    // service characteristics
    public static String ALERT_LEVEL_CHARACTERISTIC = "00002A06-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_PROXIMITY_CHARACTERISTIC = "3E099911-293F-11E4-93BD-AFD0FE6D1DFD";
    public static String TEMPERATURE_MEASUREMENT_CHARACTERISTIC = "00002A1C-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private final BluetoothGattCallback gatt_callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.d(Constants.TAG, "onConnectionStateChange: status=" + status);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(Constants.TAG, "onConnectionStateChange: CONNECTED");
                connected = true;
                Message msg = Message.obtain(activity_handler, GATT_CONNECTED);
                msg.sendToTarget();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(Constants.TAG, "onConnectionStateChange: DISCONNECTED");
                connected = false;
                Message msg = Message.obtain(activity_handler, GATT_DISCONNECT);
                msg.sendToTarget();
                if (bluetooth_gatt != null) {
                    Log.d(Constants.TAG,"Closing and destroying BluetoothGatt object");
                    bluetooth_gatt.close();
                    bluetooth_gatt = null;
                }
            }

        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            sendConsoleMessage("Services Discovered");
            Message msg = Message.obtain(activity_handler,
                    GATT_SERVICES_DISCOVERED);
            BluetoothGattCharacteristic rx = gatt.getService(UUID.fromString(NORDIC_UART_UUID)).getCharacteristic(UUID.fromString(RX_UUID));
            if (!gatt.setCharacteristicNotification(rx, true)) {
                // Stop if the characteristic notification setup failed.
                return;
            }
            BluetoothGattDescriptor desc = rx.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            if (desc == null) {
                // Stop if the RX characteristic has no client descriptor.
                return;
            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(desc)) {
                // Stop if the client descriptor could not be written.
                return;
            }
            msg.sendToTarget();
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid()
                        .toString());
                bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(activity_handler,
                        GATT_CHARACTERISTIC_READ);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                Log.d(Constants.TAG, "failed to read characteristic:"+characteristic.getUuid().toString()+" of service "+characteristic.getService().getUuid().toString()+" : status="+status);
                sendConsoleMessage("characteristic read err:"+status);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Bundle bundle = new Bundle();
            bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
            bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
            bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
// notifications and indications are both communicated from here in this way
            Message msg = Message.obtain(activity_handler, NOTIFICATION_OR_INDICATION_RECEIVED);
            msg.setData(bundle);
            msg.sendToTarget();
        }
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.d(Constants.TAG, "onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID, characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(activity_handler, GATT_CHARACTERISTIC_WRITTEN);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                sendConsoleMessage("characteristic write err:" + status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendConsoleMessage("RSSI read OK");
                Bundle bundle = new Bundle();
                bundle.putInt(PARCEL_RSSI, rssi);
                Message msg = Message
                        .obtain(activity_handler, GATT_REMOTE_RSSI);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                sendConsoleMessage("RSSI read err:"+status);
            }
        }
    };
    public boolean isConnected() {
        return connected;
    }
    @Override
    public void onCreate() {
        if (bluetooth_manager == null) {
            bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetooth_manager == null) {
                return;
            }
        }
        bluetooth_adapter = bluetooth_manager.getAdapter();
        if (bluetooth_adapter == null) {
            return;
        }
    }

    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public BleAdapterService getService() {
            return BleAdapterService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
    public void setActivityHandler(Handler handler) {
        activity_handler = handler;
    }
    private void sendConsoleMessage(String text) {
        Message msg = Message.obtain(activity_handler, MESSAGE);
        Bundle data = new Bundle();
        data.putString(PARCEL_TEXT, text);
        msg.setData(data);
        msg.sendToTarget();
    }

    // connect to the device
    public boolean connect(final String address) {
        if (bluetooth_adapter == null || address == null) {
            sendConsoleMessage("connect: bluetooth_adapter=null");
            return false;
        }
        device = bluetooth_adapter.getRemoteDevice(address);
        if (device == null) {
            sendConsoleMessage("connect: device=null");
            return false;
        }
        bluetooth_gatt = device.connectGatt(this, false, gatt_callback);
        return true;
    }
    // disconnect from device
    public void disconnect() {
        sendConsoleMessage("disconnecting");
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("disconnect: bluetooth_adapter|bluetooth_gatt null");
            return;
        }
        if (bluetooth_gatt != null) {
            bluetooth_gatt.disconnect();
        }
    }

    public void discoverServices() {
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            return;
        }
        Log.d(Constants.TAG,"Discovering GATT services");
        bluetooth_gatt.discoverServices();
    }
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetooth_gatt == null)
            return null;
        return bluetooth_gatt.getServices();
    }

    public boolean readCharacteristic(String serviceUuid,
                                      String characteristicUuid) {
        Log.d(Constants.TAG,"readCharacteristic:"+characteristicUuid+" of service " +serviceUuid);
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("readCharacteristic: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }
        BluetoothGattService gattService = bluetooth_gatt
                .getService(java.util.UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("readCharacteristic: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService
                .getCharacteristic(java.util.UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("readCharacteristic: gattChar null");
            return false;
        }
        return bluetooth_gatt.readCharacteristic(gattChar);
    }
    public boolean writeCharacteristic(String serviceUuid,
                                       String characteristicUuid, byte[] value) {
        Log.d(Constants.TAG,"writeCharacteristic:"+characteristicUuid+" of service " +serviceUuid);
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("writeCharacteristic: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }
        BluetoothGattService gattService = bluetooth_gatt
                .getService(java.util.UUID.fromString(serviceUuid));
        if (gattService == null) {
            System.out.println("Tried to send " +  Arrays.toString(value) + " but gatt was null");
            sendConsoleMessage("writeCharacteristic: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService
                .getCharacteristic(java.util.UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("writeCharacteristic: gattChar null");
            return false;
        }
        System.out.println("Sending " + new String(value));
        gattChar.setValue(value);
        return bluetooth_gatt.writeCharacteristic(gattChar);
    }

    public void readRemoteRssi() {
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            return;
        }
        bluetooth_gatt.readRemoteRssi();
    }

    public boolean setIndicationsState(String serviceUuid, String characteristicUuid, boolean enabled) {
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("setIndicationsState: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }
        BluetoothGattService gattService = bluetooth_gatt.getService(java.util.UUID.fromString(serviceUuid));
        if (gattService == null) {
            sendConsoleMessage("setIndicationsState: gattService null");
            return false;
        }
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(java.util.UUID.fromString(characteristicUuid));
        if (gattChar == null) {
            sendConsoleMessage("setIndicationsState: gattChar null");
            return false;
        }
        bluetooth_gatt.setCharacteristicNotification(gattChar, enabled);
// Enable remote notifications
        descriptor = gattChar.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        boolean ok = bluetooth_gatt.writeDescriptor(descriptor);
        return ok;
    }

    public void sendMessage(String message) {
        this.writeCharacteristic(
                BleAdapterService.LINK_LOSS_SERVICE_UUID,
                BleAdapterService.TX_UUID, message.getBytes());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void requestPeaks() {
        sendMessage(REQUEST_PEAKS);
    }

    public void setPeaks(String peaks) {
        sendMessage(SET_PEAKS + peaks);
    }

    public void requestFreqStart() {
        sendMessage(REQUEST_FREQSTART);
    }

    public void setFreqStart(String freqStart) {
        sendMessage(SET_FREQSTART + freqStart);
    }

    public void requestFreqEnd() {
        sendMessage(REQUEST_FREQEND);
    }

    public void setFreqEnd(String freqEnd) {
        sendMessage(SET_FREQEND + freqEnd);
    }

    public void requestFreq1Inc() {
        sendMessage(REQUEST_FREQ1INC);
    }

    public void setFreq1Inc(String freq1Inc) {
        sendMessage(SET_FREQ1INC + freq1Inc);
    }

    public void requestFreq2Inc() {
        sendMessage(REQUEST_FREQ2INC);
    }

    public void setFreq2Inc(String freq2Inc) {
        sendMessage(SET_FREQ2INC + freq2Inc);
    }

    public void requestFreq3Inc() {
        sendMessage(REQUEST_FREQ3INC);
    }

    public void setFreq3Inc(String freq3Inc) {
        sendMessage(SET_FREQ3INC + freq3Inc);
    }

    public void requestFreq4Inc() {
        sendMessage(REQUEST_FREQ4INC);
    }

    public void setFreq4Inc(String freq4Inc) {
        sendMessage(SET_FREQ4INC + freq4Inc);
    }

    public void requestFreq2Offset() {
        sendMessage(REQUEST_FREQ2OFFSET);
    }

    public void setFreq2Offset(String freq2Offset) {
        sendMessage(SET_FREQ2OFFSET + freq2Offset);
    }

    public void requestFreq3Offset() {
        sendMessage(REQUEST_FREQ3OFFSET);
    }

    public void setFreq3Offset(String freq3Offset) {
        sendMessage(SET_FREQ3OFFSET + freq3Offset);
    }

    public void requestFreq4Offset() {
        sendMessage(REQUEST_FREQ4OFFSET);
    }

    public void setFreq4Offset(String freq4Offset) {
        sendMessage(SET_FREQ4OFFSET + freq4Offset);
    }

    public void requestVoltage() {
        sendMessage(REQUEST_VOLTAGE);
    }

    public void requestBleTimeout() {
        sendMessage(REQUEST_BLETIMOUT);
    }

    public void setBleTimeout(String timeout) {
        sendMessage(SET_BLETIMEOUT + timeout);
    }

    public void factory() {
        sendMessage(SET_FACTORY);
    }

    public void requestSensor() {
        sendMessage(REQUEST_SENSOR);
    }

    public void setSensor(String sensor) {
        sendMessage(SET_SENSOR + sensor);
    }

    public void scan() {
        sendMessage(SCAN);
    }

    public void sleepOn() {
        sendMessage(SLEEP_ON);
    }

    public void sleepOff() {
        sendMessage(SLEEP_OFF);
    }

    public void zeroize() {
        sendMessage(ZEROIZE);
    }

    public void requestReg(String reg) {
        sendMessage(AD7747_REG + reg + REQUEST);
    }

    public void setReg(String reg, String val) {
        sendMessage(AD7747_REG + reg + SET + val);
    }

    public void requestCapDacA() {
        requestReg(CAPDACA);
    }

    public void setCapDacA(String val) {
        setReg(CAPDACA, val);
    }

    public void requestTemp() {
        sendMessage(TEMP);
    }

    public void requestFreq() {
        sendMessage(HZ);
    }

    public void requestId() {
        sendMessage(REQUEST_ID);
    }

    public void setId(String name) {
        sendMessage(SET_ID + name);
    }
}
