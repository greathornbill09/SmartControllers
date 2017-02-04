package com.hornbill.great.connectingsmartthings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class ControlCentre extends Activity {

    public BluetoothLeService mBluetoothLeService;
    private final static String TAG = ControlCentre.class.getSimpleName();
    private Button light_btn;
    private Button motor_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        setContentView(R.layout.activity_control_centre);
        getActionBar().setTitle(R.string.control_centre);
        light_btn = (Button) findViewById(R.id.lightButton);
        motor_btn = (Button) findViewById(R.id.MotorButton);
        light_btn.setVisibility(View.VISIBLE);
        motor_btn.setVisibility(View.VISIBLE);



        light_btn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Light Button On Click");
            }
        });

        motor_btn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Motor Button On Click");
            }
        });



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




    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
}
