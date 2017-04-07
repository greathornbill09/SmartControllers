package com.hornbill.great.connectingsmartthings;

/**
 * Created by sangee on 1/22/2017.
 */

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String ACTION_AQUA_RTC_CHAR_AVAILABLE =
            "com.example.bluetooth.le.ACTION_AQUA_RTC_CHAR_AVAILABLE";
    public final static String ACTION_AQUA_LIGHT_CHAR_AVAILABLE =
            "com.example.bluetooth.le.ACTION_AQUA_LIGHT_CHAR_AVAILABLE";
    public final static String ACTION_AQUA_MOTOR_CHAR_AVAILABLE =
            "com.example.bluetooth.le.ACTION_AQUA_MOTOR_CHAR_AVAILABLE";

    public final static String ACTION_NO_CHAR_AVAILABLE =
            "com.example.bluetooth.le.ACTION_NO_CHAR_AVAILABLE";

    public final static UUID UUID_AQUA_SERVICE =
            UUID.fromString(SampleGattAttributes.AQUA_SERVICE);
    public final static UUID UUID_AQUA_RTC_CHARACTERISTIC =
            UUID.fromString(SampleGattAttributes.AQUA_RTC_CHARACTERISTIC);
    public final static UUID UUID_AQUA_LIGHT_CHARACTERISTIC =
            UUID.fromString(SampleGattAttributes.AQUA_LIGHT_CHARACTERISTIC);
    public final static UUID UUID_AQUA_MOTOR_CHARACTERISTIC =
            UUID.fromString(SampleGattAttributes.AQUA_MOTOR_CHARACTERISTIC);
    public final static UUID UUID_CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                /* Read characteristics */
                BluetoothGattCharacteristic characteristic = null;
                characteristic = getAquaCharacteristic(UUID_AQUA_SERVICE,UUID_AQUA_RTC_CHARACTERISTIC);
                /* Read the characteristics obtained*/
                if (characteristic != null) {
                    Log.w(TAG, "Trying to read Aqua charatceristics==> "+characteristic);
                    readCharacteristic(characteristic);
               } else {
                    Log.w(TAG, "Failed to obtain read characteristic ");
                    broadcastUpdate(ACTION_NO_CHAR_AVAILABLE);
               }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION == status) {
                // This is where the tricky part comes
                try {
                    Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
                    Method createBondMethod = class1.getMethod("createBond", (Class[]) null);
                    Boolean returnValue = (Boolean) createBondMethod.invoke(mBluetoothGatt.getDevice(), (Object[]) null);
                    Log.w(TAG, "Pair initiates status-->" + returnValue);
                } catch (Exception e) {
                    Log.w(TAG, "Exception Pair" + e.getMessage());
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                broadcastUpdate(ACTION_NO_CHAR_AVAILABLE);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onCharacteristicRead: GATT_SUCCESS <==>");
                if (characteristic.getUuid().compareTo(UUID_AQUA_RTC_CHARACTERISTIC) == 0){
                    //broadcastUpdate(ACTION_AQUA_RTC_CHAR_AVAILABLE, characteristic);
                    /* Check for Schedule Light Characteristics*/
                    characteristic = getAquaCharacteristic(UUID_AQUA_SERVICE, UUID_AQUA_LIGHT_CHARACTERISTIC);
                    if (characteristic != null) {
                        Log.w(TAG, "onCharacteristicRead: aquaDeviceLightReadInProgress ===");
                        readCharacteristic(characteristic);
                    }
                } else if (characteristic.getUuid().compareTo(UUID_AQUA_LIGHT_CHARACTERISTIC) == 0) {
                    broadcastUpdate(ACTION_AQUA_LIGHT_CHAR_AVAILABLE, characteristic);
                    characteristic = getAquaCharacteristic(UUID_AQUA_SERVICE, UUID_AQUA_MOTOR_CHARACTERISTIC);
                    if (characteristic != null) {
                        Log.w(TAG, "onCharacteristicRead: aquaDeviceMotorReadInProgress ===");
                        readCharacteristic(characteristic);
                    }
                } else if (characteristic.getUuid().compareTo(UUID_AQUA_MOTOR_CHARACTERISTIC) == 0){
                    broadcastUpdate(ACTION_AQUA_MOTOR_CHAR_AVAILABLE, characteristic);
                    Log.w(TAG, "onCharacteristicRead: All Characteristic Reads done ===");
                    /* Enable Notification/Indication as well*/
                    writeCustomCharacteristic(0x0001,UUID_AQUA_RTC_CHARACTERISTIC);
                } else {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            } else if (BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION == status ||
                    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION == status) {
                // This is where the tricky part comes
                try {
                    Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
                    Method createBondMethod = class1.getMethod("createBond", (Class[]) null);
                    Boolean returnValue = (Boolean) createBondMethod.invoke(mBluetoothGatt.getDevice(), (Object[]) null);
                    Log.w(TAG, "Pair initiate status-->" + returnValue);
                } catch (Exception e) {
                    Log.w(TAG, "Exception Pair" + e.getMessage());
                }
            } else {
                Log.w(TAG, "Some other error");
                broadcastUpdate(ACTION_NO_CHAR_AVAILABLE);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().compareTo(UUID_AQUA_RTC_CHARACTERISTIC) == 0){
                Log.w(TAG, "onCharacteristicChanged: ACTION_AQUA_RTC_CHAR_AVAILABLE" );
                broadcastUpdate(ACTION_AQUA_RTC_CHAR_AVAILABLE, characteristic);
            } else if (characteristic.getUuid().compareTo(UUID_AQUA_MOTOR_CHARACTERISTIC) == 0){
                Log.w(TAG, "onCharacteristicChanged: ACTION_AQUA_MOTOR_CHAR_AVAILABLE" );
                broadcastUpdate(ACTION_AQUA_MOTOR_CHAR_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                /* Enable Pump notification */
                if (descriptor.getCharacteristic().getUuid().compareTo(UUID_AQUA_RTC_CHARACTERISTIC) == 0) {
                    writeCustomCharacteristic(0x0001, UUID_AQUA_MOTOR_CHARACTERISTIC);
                }
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        boolean rtcSyncDone=false;


        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                ByteBuffer bb = ByteBuffer.wrap(data);
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                if (characteristic.getUuid().compareTo(UUID_AQUA_RTC_CHARACTERISTIC) == 0) {
                    ShortBuffer ib = bb.order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    final short year = ib.get(0);
                    final byte month = bb.get(2);
                    final byte day = bb.get(3);
                    final byte hour = bb.get(4);
                    final byte minute = bb.get(5);
                    final byte seconds = bb.get(6);
                    final byte dayofweek = bb.get(7);
                    /*
                    Log.w(TAG, "broadcastUpdate: year "+year );
                    Log.w(TAG, "broadcastUpdate: month "+month );
                    Log.w(TAG, "broadcastUpdate: day "+day );
                    Log.w(TAG, "broadcastUpdate: hour "+hour );
                    Log.w(TAG, "broadcastUpdate: minute "+minute );
                    Log.w(TAG, "broadcastUpdate: seconds "+seconds );
                    Log.w(TAG, "broadcastUpdate: dayofweek "+dayofweek );
                    */
                    rtcSyncDone=syncRTCOnDevice(month,day,hour,minute,dayofweek,characteristic);
                    if ((rtcSyncDone == true) && ((((globalData)this.getApplication()).getRtcSyncStatus()) == false)) {
                        ((globalData) this.getApplication()).setRtcSyncDone(rtcSyncDone);
                        intent.putExtra("RTC_DATA", stringBuilder.toString());
                        Log.w(TAG, "Sending Broadcast Update to App: UUID_AQUA_RTC_CHARACTERISTIC" );
                        sendBroadcast(intent);
                    }
                } else if (characteristic.getUuid().compareTo(UUID_AQUA_LIGHT_CHARACTERISTIC) == 0){
                    Log.w(TAG, "broadcastUpdate: UUID_AQUA_LIGHT_CHARACTERISTIC" );
                    Log.w(TAG, "broadcastUpdate: UUID_AQUA_LIGHT_CHARACTERISTIC data length:" +data.length);
                    intent.putExtra("LIGHTSchedule",data);
                    sendBroadcast(intent);

                } else if (characteristic.getUuid().compareTo(UUID_AQUA_MOTOR_CHARACTERISTIC) == 0) {
                    Log.w(TAG, "broadcastUpdate: UUID_AQUA_MOTOR_CHARACTERISTIC" );
                    intent.putExtra("MOTORSchedule",data);
                    sendBroadcast(intent);
                }
            }
        }
    }

    public boolean syncRTCOnDevice(byte month, byte day,byte hour,byte minute,byte dayofweek,
                                final BluetoothGattCharacteristic characteristic){
        boolean isSyncCompleted = false;
        Log.w(TAG, "syncRTCOnDevice: Checking for the host time and date");
        if (characteristic.getUuid().compareTo(UUID_AQUA_RTC_CHARACTERISTIC) == 0) {
            int hostyear, hostMonth, hostDay, hostHour, hostMinute, hostSeconds, hostDayOfWeek;
            final byte[] data = new byte[8];
            Arrays.fill(data, (byte) 0);
            ByteBuffer bb = ByteBuffer.wrap(data);
            Calendar cal = Calendar.getInstance();
            hostyear = cal.get(Calendar.YEAR);
            hostMonth = cal.get(Calendar.MONTH) + 1; // Note: zero based!
            hostDay = cal.get(Calendar.DAY_OF_MONTH);
            hostHour = cal.get(Calendar.HOUR_OF_DAY);
            hostMinute = cal.get(Calendar.MINUTE);
            hostSeconds = (cal.get(Calendar.SECOND)) + 1;
            hostDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if ((month != hostMonth) || (day != hostDay) || (hour != hostHour) ||
                (minute != hostMinute) || (dayofweek != hostDayOfWeek)) {
                bb.put(0, (byte) (hostyear & 0xFF));
                bb.put(1, (byte) ((hostyear >> 8) & 0xFF));
                bb.put(2, (byte) hostMonth);
                bb.put(3, (byte) hostDay);
                bb.put(4, (byte) hostHour);
                bb.put(5, (byte) hostMinute);
                bb.put(7, (byte) hostDayOfWeek);
                Log.w(TAG, "syncRTCOnDevice: RTC sync in progress");
                characteristic.setValue(data);
                mBluetoothGatt.writeCharacteristic(characteristic);
            } else {
                Log.w(TAG, "syncRTCOnDevice: Sync not required");
                isSyncCompleted = true;
            }
        }

        return isSyncCompleted;
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public BluetoothGattCharacteristic getAquaCharacteristic(UUID Service, UUID Characteristics) {
        BluetoothGattCharacteristic mWriteCharacteristic = null;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return mWriteCharacteristic;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(Service);
        if (mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return mWriteCharacteristic;
        }

        /* get the read characteristic from the service */
        mWriteCharacteristic = mCustomService.getCharacteristic(Characteristics);
        if (mWriteCharacteristic == null) {
            Log.w(TAG, "Wrong Characteristic UUID");
            return mWriteCharacteristic;
        }

        return mWriteCharacteristic;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        /* Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }*/

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void writeDataToCustomCharacteristic(UUID uuid,byte[] data){
        BluetoothGattCharacteristic charValue=null;

        Log.w(TAG, "writeDataToCustomCharacteristic:"+data);

        if(uuid == UUID_AQUA_LIGHT_CHARACTERISTIC) {
            charValue = getAquaCharacteristic(UUID_AQUA_SERVICE, UUID_AQUA_LIGHT_CHARACTERISTIC);
        } else if (uuid == UUID_AQUA_MOTOR_CHARACTERISTIC){
            charValue = getAquaCharacteristic(UUID_AQUA_SERVICE, UUID_AQUA_MOTOR_CHARACTERISTIC);
        }
        charValue.setValue(data);
        mBluetoothGatt.writeCharacteristic(charValue);
    }

    public void writeCustomCharacteristic(int value, UUID uuid) {
        BluetoothGattCharacteristic characteristic = null;
        UUID ccd = null;

        characteristic = getAquaCharacteristic(UUID_AQUA_SERVICE,uuid);
        if (characteristic == null){
            Log.w(TAG, "Failed to obtain  Aqua LIGHT RTC notify characteristic ");
            return;
        }

        setCharacteristicNotification(characteristic, true);
        //Enabled remote notifications
        BluetoothGattDescriptor desc = characteristic.getDescriptor(UUID_CLIENT_CHARACTERISTIC_CONFIG);
        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if(mBluetoothGatt.writeDescriptor(desc) == false) {
            Log.w(TAG, "Failed to write characteristic");
        }
    }
}
