package com.hornbill.great.connectingsmartthings;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.R.id.list;

public class DeviceList extends Activity {

    private Activity activity;
    private final static String TAG = DeviceList.class.getSimpleName();
    BluetoothGattCharacteristic characteristic = null;

    public BluetoothLeService mBluetoothLeService;

    private ListView mainListView ;
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;

    int devId = 0;
    int maxDevices = 0;
    public static int deviceId =128;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        Log.w(TAG, "Device List Screen\n");
        activity = this;
        Log.w(TAG, "max devices "+((globalData)activity.getApplication()).getAquaLightChar("maxdevices"));
        maxDevices = ((globalData)activity.getApplication()).getAquaLightChar("maxdevices");

        // Bind Service
        /* BLE Service Binder*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /* Register the calibration update receiver*/
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        // Find the ListView resource.
        mainListView = (ListView) findViewById( R.id.devicelistview );
        arrayList = new ArrayList<String>();
        while(devId < maxDevices) {
            arrayList.add("Device " + devId);
            devId ++;
        }
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,arrayList);
        mainListView.setAdapter(adapter);

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.w(TAG, "Device that is clicked --> "+i);
                enablePeripheralGattDatabase(i);

            }
        });


    }

    //Enable the peripheral database
    private void enablePeripheralGattDatabase(int devId){
        byte[]lightScheduleData = new byte[12];

        characteristic = mBluetoothLeService.getAquaCharacteristic(BluetoothLeService.UUID_AQUA_SERVICE,BluetoothLeService.UUID_AQUA_RTC_CHARACTERISTIC);

        Log.w(TAG, "enablePeripheralGattDatabase "+lightScheduleData[0]);

        deviceId = devId;

        lightScheduleData[0] = (byte)0x00;
        lightScheduleData[0] |= devId;
        lightScheduleData[1] = (byte)0x00;
        lightScheduleData[2] = (byte)0x00;
        lightScheduleData[3] = (byte)0x00;
        lightScheduleData[4] = (byte)0x00;
        lightScheduleData[5] = (byte)0x00;
        lightScheduleData[6] = (byte)0x00;
        lightScheduleData[7] = (byte)0x00;
        lightScheduleData[8] = (byte)0x00;
        lightScheduleData[9] = (byte)0x00;
        lightScheduleData[10] = (byte)0x00;
        lightScheduleData[11] = (byte)0x00;

        for(int i =0; i< 12; i++)
        Log.w(TAG," " + lightScheduleData[i] + "\n");
        mBluetoothLeService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_LIGHT_CHARACTERISTIC,lightScheduleData);
        Log.w(TAG,"Reading the characteristics" +characteristic);
        mBluetoothLeService.readCharacteristic(characteristic,Boolean.TRUE);

    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    /* Connection related methods */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_AQUA_LIGHT_CHAR_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_AQUA_LIGHT_CHAR_AVAILABLE.equals(action)) {

                Log.w(TAG,"Characteristic response for the respective device Id\n");


                /* Update the Global Database */
                final byte[] scheduleData;
                if ((scheduleData = intent.getExtras().getByteArray("LIGHTSchedule")) != null){
                    ByteBuffer scheduleBuffer = ByteBuffer.wrap(scheduleData);
                    storeDeviceDataBasedOnDeviceId(scheduleBuffer);
                    final Intent onDummyClickIntent = new Intent(activity,LightControllerTab.class);
                    LightControllerTab.deviceId = deviceId;
                    startActivity(onDummyClickIntent);

                }

            }
        }
    };

    private void storeDeviceDataBasedOnDeviceId(ByteBuffer scheduleBuffer){

        ((globalData) activity.getApplication()).setAquaLightChar("maxdevices", (scheduleBuffer.get(1)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightmode", (scheduleBuffer.get(2)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightstatus", (scheduleBuffer.get(3)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightdom", (scheduleBuffer.get(4)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightdow", (scheduleBuffer.get(5)));
        ((globalData) activity.getApplication()).setAquaLightChar("hourly", (scheduleBuffer.get(6)));
        ((globalData) activity.getApplication()).setAquaLightChar("lighthours", (scheduleBuffer.get(7)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightminutes", (scheduleBuffer.get(8)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightrecurrences", (scheduleBuffer.get(9)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightdurationhours", (scheduleBuffer.get(10)));
        ((globalData) activity.getApplication()).setAquaLightChar("lightdurationminutes", (scheduleBuffer.get(11)));
    }
}
