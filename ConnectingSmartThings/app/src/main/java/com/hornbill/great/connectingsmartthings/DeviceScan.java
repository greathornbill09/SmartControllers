package com.hornbill.great.connectingsmartthings;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class DeviceScan extends ListActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_COARSE_LOACTION_REQUEST = 2;
    private static final int LOCATION_SERVICE_ENABLED = 3;
    private static final long SCAN_PERIOD = 10000;
    boolean doubleBackToExitPressedOnce = false;
    private Activity activity;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private SwipeRefreshLayout mySwipeRefreshLayout;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       getActionBar().setTitle(R.string.app_name);
       activity = this;
       mHandler = new Handler();
       setContentView(R.layout.activity_device_scan);


       // Ask for required permission
       //BT/BLE
       // Use this check to determine whether BLE is supported on the device.  Then you can
       // selectively disable BLE-related features.
       if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
           Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
           finish();
       }
       // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
       // BluetoothAdapter through BluetoothManager.
       final BluetoothManager bluetoothManager =
               (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
       mBluetoothAdapter = bluetoothManager.getAdapter();
       // Checks if Bluetooth is supported on the device.
       if (mBluetoothAdapter == null) {
           Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
           finish();
           return;
       }

       // Location
       if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != (int) PackageManager.PERMISSION_GRANTED)
       {
           // if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
           {
               final AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setTitle("This app needs location access");
               builder.setMessage("Location access is required to discover nearby Bluetooth smart devices.");
               builder.setPositiveButton(android.R.string.ok, null);
               builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                   @Override
                   public void onDismiss(DialogInterface dialog) {
                       ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOACTION_REQUEST);
                   }
               });
               builder.show();
           }
       } else {
           //Enable location if it is not enabled already
           if (!isLocationServiceEnabled(activity)) {
               Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
               startActivityForResult(enableLocationIntent, LOCATION_SERVICE_ENABLED);
           }
       }


       /* Swipe to Refresh*/
       /* * Sets up a SwipeRefreshLayout.OnRefreshListener that is invoked when the user * performs a swipe-to-refresh gesture. */
       mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
       mySwipeRefreshLayout.setOnRefreshListener(
               new SwipeRefreshLayout.OnRefreshListener() {
                   @Override
                   public void onRefresh() {
                       Log.w("scanLeDevice", "onRefresh called from SwipeRefreshLayout");

                       // This method performs the actual data-refresh operation.
                       // The method calls setRefreshing(false) when it's finished.

                       scanLeDevice(false);
                       mLeDeviceListAdapter.notifyDataSetChanged();
                       mLeDeviceListAdapter.clear();
                       scanLeDevice(true);
                   }
               }
       );

    }

    /*Permission related methods*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_COARSE_LOACTION_REQUEST: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w("BleActivity", "Granted coarse location permission");
                    //Enable GPS
                    if (!isLocationServiceEnabled(activity)) {
                        Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableLocationIntent, LOCATION_SERVICE_ENABLED);
                    }
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Limited Functionality");
                    builder.setMessage("App will not be able to discover Bluetooth smart devices as access to location is denied.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public boolean isLocationServiceEnabled(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled= false,network_enabled = false;

        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){
            //do nothing...
        }

        try{
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){
            //do nothing...
        }

        return gps_enabled || network_enabled;
    }



    /*Permission related methods*/

    /*Scan and display related methods*/
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScan.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();

        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.devicescan, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                            mySwipeRefreshLayout.setRefreshing(false);
                        }
                    });

                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }



    private void scanLeDevice(final boolean enable) {

        Log.w("scanLeDevice", "scanLeDevice with "+enable);

        if (enable) {
            // Start the discovery just popup the pairing dialog to foreground
            // Give it some time before cancelling the discovery
            // Then do the LeScan and connect to the device
            BluetoothAdapter.getDefaultAdapter().startDiscovery();
            try {
                Thread.sleep(200);
            }catch(Exception ex){
                //do nothing...
            }
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    mySwipeRefreshLayout.setRefreshing(false);
                }
            }, SCAN_PERIOD+10);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

        }
        invalidateOptionsMenu();
    }


    /* Scan and Display related methods*/


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        /* Initializes list view adapter.*/
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        mySwipeRefreshLayout.setColorSchemeColors(Color.BLUE,Color.MAGENTA,Color.BLUE);
        mySwipeRefreshLayout.setRefreshing(true);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == LOCATION_SERVICE_ENABLED)
        {
            //Do whatever you need to
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scanmenu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_filter:
                //openSearchView();
                return true;
            case R.id.action_app_introduction:
                Intent intent = new Intent(this, IntroActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mScanning) {
            scanLeDevice(false);
            mScanning = false;
        }
        Log.w("scanLeDevice", "onListItemClick clicked ");
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            //super.onBackPressed();
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()){
                mBluetoothAdapter.disable();
            }
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

}
