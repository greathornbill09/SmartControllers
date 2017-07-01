package com.hornbill.great.connectingsmartthings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class ControlCentre extends Activity {

    public BluetoothLeService mBluetoothLeService;
    private final static String TAG = ControlCentre.class.getSimpleName();
    private Button motor_btn;
    private Button dummy_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        setContentView(R.layout.activity_control_centre);
        getActionBar().setTitle(R.string.control_centre);
        motor_btn = (Button) findViewById(R.id.MotorButton);
        dummy_btn = (Button) findViewById(R.id.dummy);
        motor_btn.setVisibility(View.VISIBLE);
        dummy_btn.setVisibility(View.VISIBLE);
        final Intent onMotorClickIntent = new Intent(this,MotorController.class);
        //final Intent onDummyClickIntent = new Intent(this,LightControllerTab.class);
        final Intent onDummyClickIntent = new Intent(this,DeviceList.class);
        dummy_btn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Light Button On Click");
                startActivity(onDummyClickIntent);
            }
        });

        motor_btn.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Motor Button On Click");
                startActivity(onMotorClickIntent);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.productintro, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.prod_intro:
                Intent intent = new Intent(this, IntroActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
}
