package com.hornbill.great.connectingsmartthings;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Process;
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

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.hornbill.great.connectingsmartthings.R.color.PowderBlue;
import static com.hornbill.great.connectingsmartthings.R.style.MyActionBar;

public class DeviceScan extends ListActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_COARSE_LOACTION_REQUEST = 2;
    private static final int LOCATION_SERVICE_ENABLED = 3;
    private static final long SCAN_PERIOD = 20000;
    boolean doubleBackToExitPressedOnce = false;
    boolean isRecieverRegistered = false;
    private Activity activity;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private SwipeRefreshLayout mySwipeRefreshLayout;

    /*Bluetooth related*/
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private String mDeviceName;
    ProgressDialog showProgress;

    //For > LolliPop(>21)
    private ScanCallback mScanCallback;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings scansetting;
    private List<ScanFilter> filters;

    //For Jelly Bean MR2 and Kitkat(< 21)
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("DeviceScan Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }


    // Internal state machine.
    public enum ConnectionState {
        IDLE,
        CONNECT_GATT,
        DISCOVER_SERVICES,
        READ_CHARACTERISTIC,
        FAILED,
        SUCCEEDED,
    }

    private ConnectionState mState = ConnectionState.IDLE;

    private final static String TAG = DeviceScan.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        getActionBar().setTitle(R.string.app_name);
        activity = this;
        mHandler = new Handler();
        setContentView(R.layout.activity_device_scan);
        showProgress = new ProgressDialog(this);
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != (int) PackageManager.PERMISSION_GRANTED) {
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
                        Log.e(TAG, "onRefresh called from SwipeRefreshLayout");
                        // This method performs the actual data-refresh operation.
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.

                        scanLeDevice(false);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                        mLeDeviceListAdapter.clear();
                        scanLeDevice(true);
                    }
                }
        );

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "onServiceConnected: initializing");
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

    /*Permission related methods*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_COARSE_LOACTION_REQUEST: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Granted coarse location permission");
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

    public boolean isLocationServiceEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false, network_enabled = false;

        try {
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            //do nothing...
        }

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
            //do nothing...
        }

        return gps_enabled || network_enabled;
    }

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
            if (!mLeDevices.contains(device)) {
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

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private void initScanCallback(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            initScanCallbackLollipop();
        } else {
            initScanCallbackwardSupport();
        }
    }

    // Create scan call back for API < 21 (i.e Lollipop).
    private void initScanCallbackwardSupport() {
        if (mLeScanCallback != null) return;
        // Device scan callback.
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
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
    }

    // Create scan call back for API >= 21 (i.e Lollipop).
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initScanCallbackLollipop() {
        if (mScanCallback != null) return;
        this.mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i("callbackType", String.valueOf(callbackType));
                Log.i("result", result.toString());
                BluetoothDevice btDevice = result.getDevice();
                mLeDeviceListAdapter.addDevice(btDevice);
                mLeDeviceListAdapter.notifyDataSetChanged();
                mySwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult sr : results) {
                    Log.i("ScanResult - Results", sr.toString());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("Scan Failed", "Error Code: " + errorCode);
            }
        };
    }

    protected void stopScan(){
        mScanning = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        } else {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mLEScanner != null) {
                mLEScanner.flushPendingScanResults(mScanCallback);
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private void scanLeDevice(final boolean enable) {

        Log.e(TAG, "scanLeDevice with " + enable);
        initScanCallback();

        if (enable) {
            // Start the discovery just popup the pairing dialog to foreground
            // Give it some time before cancelling the discovery
            // Then do the LeScan and connect to the device
            /*BluetoothAdapter.getDefaultAdapter().startDiscovery();
            try {
                Thread.sleep(200);
            }catch(Exception ex){
                //do nothing...
            }
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();*/

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                if (mLEScanner != null) {
                    mLEScanner.startScan(filters, scansetting, mScanCallback);
                }
            }
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    mySwipeRefreshLayout.setRefreshing(false);
                }
            }, SCAN_PERIOD);
        } else {
            stopScan();
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
            Log.e(TAG, "Request for BT ON");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (mState != ConnectionState.READ_CHARACTERISTIC) {

            Log.e(TAG, "Initializing the list view adapter in Resume");

        /* Initializes list view adapter.*/
            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
            mySwipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.MAGENTA, Color.BLUE);
            mySwipeRefreshLayout.setRefreshing(true);
            //mLeDeviceListAdapter.notifyDataSetChanged();
            mLeDeviceListAdapter.clear();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                scansetting = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        .build();
                ScanFilter scanFilter =
                        new ScanFilter.Builder()
                                .setServiceUuid(ParcelUuid.fromString((BluetoothLeService.UUID_AQUA_SERVICE).toString()))
                                .build();
                Log.e(TAG, "scanFilter -->" + scanFilter);
                Log.e(TAG, "scanSettings -->" + scansetting);
                filters = new ArrayList<ScanFilter>();
                filters.add(scanFilter);
            }

            scanLeDevice(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.disconnect();
        /* Clear off the RTC sync flag*/
        ((globalData) this.getApplication()).setRtcSyncDone(false);
        unbindService(mServiceConnection);
        //mBluetoothLeService = null;
        //((globalData)activity.getApplication()).setBluetoothLeService(mBluetoothLeService);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_SERVICE_ENABLED) {
            //Do whatever you need to
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySwipeRefreshLayout.setRefreshing(false);
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        /*if(isRecieverRegistered == true) {
            unregisterReceiver(mGattUpdateReceiver);
            isRecieverRegistered = false;
        }*/
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
        Log.e(TAG, "onListItemClick clicked ");
        mDeviceName = device.getName();
        mDeviceAddress = device.getAddress();
        showProgress.setProgressStyle(MyActionBar);
        showProgress.setTitle("Connecting");
        showProgress.setMessage("Please wait while we communicate with the device...");
        showProgress.setCancelable(false); // disable dismiss by tapping outside of the dialog
        showProgress.show();
        Log.e(TAG, "Loading Status: " + showProgress.isShowing());
        /* Clear off the RTC sync flag*/
        ((globalData) this.getApplication()).setRtcSyncDone(false);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        isRecieverRegistered = true;
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            // Update state.
            mState = ConnectionState.CONNECT_GATT;
            Log.d(TAG, "Connect request result=" + result);
        }
/* To dismiss the dialog
        progress.dismiss();*/
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            //super.onBackPressed();
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                mBluetoothLeService.disconnect();
                /* Clear off the RTC sync flag*/
                ((globalData) this.getApplication()).setRtcSyncDone(false);
                unbindService(mServiceConnection);
                mBluetoothAdapter.disable();
                if (isRecieverRegistered == true) {
                    unregisterReceiver(mGattUpdateReceiver);
                    isRecieverRegistered = false;
                }
            }
            moveTaskToBack(true);
            Process.killProcess(Process.myPid());
            System.exit(1);
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    /* Connection related methods */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_AQUA_RTC_CHAR_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_AQUA_LIGHT_CHAR_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_AQUA_MOTOR_CHAR_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_NO_CHAR_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mState = ConnectionState.DISCOVER_SERVICES;
                Log.e(TAG, "mGattUpdateReceiver : Gatt Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.e(TAG, "mGattUpdateReceiver : Gatt DisConnected in mState = " + mState);
                switch (mState) {
                    case IDLE:
                        // Do nothing in this case.
                        break;
                    case CONNECT_GATT:
                        // This can happen if the bond information is incorrect. Delete it and reconnect.
                        deleteBondInformation(mBluetoothLeService.mBluetoothGatt.getDevice());
                        mBluetoothLeService.connect(mDeviceAddress);
                        break;
                    case DISCOVER_SERVICES:
                        // This can also happen if the bond information is incorrect. Delete it and reconnect.
                        deleteBondInformation(mBluetoothLeService.mBluetoothGatt.getDevice());
                        mBluetoothLeService.connect(mDeviceAddress);
                        break;
                    case READ_CHARACTERISTIC:
                        // Disconnected while reading the characteristic. Probably just a link failure.
                        mBluetoothLeService.mBluetoothGatt.close();
                        mState = ConnectionState.FAILED;
                        break;
                    case FAILED:
                    case SUCCEEDED:
                        // Normal disconnection.
                        mBluetoothLeService.close();
                        break;
                    default:
                        //             mBluetoothLeService.mBluetoothGatt.close();
                }

                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //    displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.w(TAG, "mGattUpdateReceiver : Service Discovery Complete");
                mState = ConnectionState.SUCCEEDED;
            } else if (BluetoothLeService.ACTION_AQUA_RTC_CHAR_AVAILABLE.equals(action)) {
                Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_RTC_CHAR_AVAILABLE ");
                mState = ConnectionState.SUCCEEDED;
                    /* Now its time to move on to control centre activity*/
                if (showProgress.isShowing() == true) {
                    showProgress.dismiss();
                }
                final Intent controlIntent = new Intent(activity, ControlCentre.class);
                startActivity(controlIntent);

            } else if (BluetoothLeService.ACTION_AQUA_LIGHT_CHAR_AVAILABLE.equals(action)) {
                Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE ");
                final byte[] scheduleData;
                if ((scheduleData = intent.getExtras().getByteArray("LIGHTSchedule")) != null) {
                    Log.w(TAG, "mGattDataUpdateReceiver : Got the Light Schedule data in App");
                    ByteBuffer scheduleBuffer = ByteBuffer.wrap(scheduleData);
                    Log.w(TAG, "broadcastUpdate: LIGHTSchedule Buffer Length " + scheduleData.length);

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
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE Max Devices " + ((globalData) activity.getApplication()).getAquaLightChar("maxdevices"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightmode"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightstatus"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightdom"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightdow"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("hourly"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lighthours"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightminutes"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightrecurrences"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightdurationhours"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_LIGHT_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaLightChar("lightdurationminutes"));
                }
            } else if (BluetoothLeService.ACTION_AQUA_MOTOR_CHAR_AVAILABLE.equals(action)) {
                final byte[] scheduleMotorData;
                if ((scheduleMotorData = intent.getExtras().getByteArray("MOTORSchedule")) != null) {
                    Log.w(TAG, "mGattDataUpdateReceiver : Got the Light Schedule data in App");
                    ByteBuffer scheduleBuffer = ByteBuffer.wrap(scheduleMotorData);
                    Log.w(TAG, "broadcastUpdate: MOTORSchedule Buffer Length " + scheduleMotorData.length);
                    ((globalData) activity.getApplication()).setAquaMotorChar("motormode", (scheduleBuffer.get(0)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motorpump", (scheduleBuffer.get(1)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motorvalve", (scheduleBuffer.get(2)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motordom", (scheduleBuffer.get(3)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motordow", (scheduleBuffer.get(4)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("hourly", (scheduleBuffer.get(5)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motorhours", (scheduleBuffer.get(6)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motorminutes", (scheduleBuffer.get(7)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motorrecurrence", (scheduleBuffer.get(8)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motordurationhours", (scheduleBuffer.get(9)));
                    ((globalData) activity.getApplication()).setAquaMotorChar("motordurationminutes", (scheduleBuffer.get(10)));

                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motormode"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motorpump"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motorvalve"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motordow"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motorhours"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motorminutes"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motorrecurrence"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motordurationhours"));
                    Log.w(TAG, "mGattUpdateReceiver : ACTION_AQUA_MOTOR_CHAR_AVAILABLE " + ((globalData) activity.getApplication()).getAquaMotorChar("motordurationminutes"));
                }
            } else if (BluetoothLeService.ACTION_NO_CHAR_AVAILABLE.equals(action)) {
                if (showProgress.isShowing() == true) {
                    showProgress.dismiss();
                }
                Log.e(TAG, "mGattUpdateReceiver : ACTION_NO_CHAR_AVAILABLE ");
                    /* Not the intended message*/
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(activity);

                dlgAlert.setMessage("This is not the right device, Please scan again");
                dlgAlert.setTitle("Error Message...");
                dlgAlert.setPositiveButton("OK", null);
                dlgAlert.setCancelable(true);
                dlgAlert.create().show();

                dlgAlert.setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                mState = ConnectionState.FAILED;
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Log.w(TAG, "Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    Log.w(TAG, "Unpaired");
                }
            }
        }
    };

    public static void deleteBondInformation(BluetoothDevice device) {
        try {
            // FFS Google, just unhide the method.
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
