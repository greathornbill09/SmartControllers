package com.hornbill.great.connectingsmartthings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.EditText;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;


import java.util.Calendar;
import java.util.Date;

public class LightController extends FragmentActivity implements AdapterView.OnItemSelectedListener,NumberPicker.OnValueChangeListener {

    public int lightScheduleDayOfMonth = 0;
    public int lightScheduleDayOfWeek = 0;
    public int lightScheduleHour = 0;
    public int lightScheduleMin = 0;
    public int lightScheduleDurHour = 0;
    public int lightScheduleWindow = 0;
    public int lightScheduleDurMin = 0;

    private Activity activity = this;
    private TableLayout scheduleView;
    private Switch lightSwitch;
    private Button lightScheduleButton;
    private Button lightScheduleDurationButton;
    private Button lightScheduleTriggerButton;
    private Byte statusLight;
    private boolean productFlavor;
    private final static String TAG = LightController.class.getSimpleName();
    private BluetoothLeService mLightBluetoothService;
    private int ti_hh, ti_mm;
    private String time, time_mode;
    boolean isRecieverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_controller);
        getActionBar().setTitle(R.string.light_control);

        /* BLE Service Binder*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if (mLightBluetoothService == null) {
            Log.w(TAG,"Bluetooth Service available");
        } else {
            Log.w(TAG,"Could not fetch the bluetooth service");
        }

        /* Register the calibration update receiver*/
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        isRecieverRegistered = true;
        productFlavor = ((globalData)activity.getApplication()).getProductFlavor();

        /* Displaying the next schedule*/
        scheduleView = (TableLayout) findViewById(R.id.scheduleDetails);
        /*Displaying in the Light switch*/
        lightSwitch = (Switch) findViewById(R.id.mySwitch);
        /* Displaying the Date and time Picker*/
        lightScheduleButton = (Button) findViewById(R.id.mySchedule);
        /*Displaying schedule duration button */
        lightScheduleDurationButton = (Button) findViewById(R.id.myScheduleDuration);
        /*Displaying light schedule trigger */
        lightScheduleTriggerButton = (Button) findViewById(R.id.scheduleTrigger);
         /* Select the frequency*/
        Spinner spinner = (Spinner) findViewById(R.id.spinSelect);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.repeat));
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(0, true);

        displaySchedule();

        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
            byte[]data = new byte[9];
            if (isChecked){
                Log.w(TAG,"Write On");
                data[1]= 1;
                Log.w(TAG,"Light Mode : "+data[0]);
                updateGlobalSpace("lightmode",data[0]);
                Log.w(TAG,"Light Status : "+data[1]);
                updateGlobalSpace("lightstatus",data[1]);
            } else {
                Log.w(TAG,"Write Off");
                data[1]= 0;
                Log.w(TAG,"Light Mode : "+data[0]);
                updateGlobalSpace("lightmode",data[0]);
                Log.w(TAG,"Light Status : "+data[1]);
                updateGlobalSpace("lightstatus",data[1]);
            }
            Log.w(TAG," Writing Light Switch Status BLE");
                sendLightCustomCharacteristicDatafromGlobalStructure();
            }
        });

        lightScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG,"Schedule Button Clicked");
                new SlideDateTimePicker.Builder(getSupportFragmentManager())
                        .setListener(listener)
                        .setInitialDate(new Date())
                        .build()
                        .show();
            }
        });

        lightScheduleDurationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG,"Schedule Duration Button Clicked");
                showHourMinPicker();
            }

        });

        lightScheduleTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            updateGlobalSpace("lightmode", (byte) 1);
            /* Write data to the custom characteristics*/
            sendLightCustomCharacteristicDatafromGlobalStructure();
           /* Update the display area*/
            displaySchedule();
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id)
    {
        Log.w(TAG,"onItemSelected"+ parent.getSelectedItem());
        switch((String)parent.getSelectedItem()){
            case "Disable Schedule" :
                updateGlobalSpace("lightrecurrences",(byte)0);
                break;
            case "Daily" :
                updateGlobalSpace("lightrecurrences",(byte)1);
                break;
            case "Weekly" :
                updateGlobalSpace("lightrecurrences",(byte)2);
                break;
            case "Monthly" :
                updateGlobalSpace("lightrecurrences",(byte)3);
                break;
            case "Hourly" :
                // TODO : pop numberpicker to get hourly data, validate as well
                //updateGlobalSpace("hourly",(byte)4);
                lightScheduleDurHour = ((globalData)activity.getApplication()).getAquaLightChar("lightdurationhours");
                lightScheduleDurMin = ((globalData)activity.getApplication()).getAquaLightChar("lightdurationminutes");
                Log.w(TAG,"lightScheduleDurHour"+lightScheduleDurHour );
                if(lightScheduleDurHour == 0 && lightScheduleDurMin == 0)
                {
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(activity);

                    dlgAlert.setMessage("Please Configure the duration!");
                    dlgAlert.setTitle("Error Message...");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();

                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    break;
                }
                showHourPicker();
                updateGlobalSpace("lightrecurrences",(byte)4);
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

        Log.i("value is",""+newVal);
    }

    public void showHourMinPicker(){
        final Dialog d = new Dialog(LightController.this);
        d.setTitle("Choose Duration");
        d.setContentView(R.layout.dialog);
        Button b1 = (Button) d.findViewById(R.id.cancel);
        Button b2 = (Button) d.findViewById(R.id.set);
        final NumberPicker hp = (NumberPicker) d.findViewById(R.id.HourPicker);
        hp.setMaxValue(12); // max value 12
        hp.setMinValue(0);   // min value 0
        hp.setWrapSelectorWheel(false);
        final NumberPicker mp = (NumberPicker) d.findViewById(R.id.MinPicker);
        mp.setMaxValue(60); // max value 60
        mp.setMinValue(0);   // min value 0
        mp.setWrapSelectorWheel(false);
        hp.setOnValueChangedListener(this);
        mp.setOnValueChangedListener(this);
        b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                //tv.setText(String.valueOf(np.getValue())); //set the value to textview
                Log.w(TAG,"Hour Chosen : "+ hp.getValue() );
                lightScheduleDurHour = hp.getValue();
                updateGlobalSpace("lightdurationhours",(byte)lightScheduleDurHour);
                Log.w(TAG,"Min Chosen : "+ mp.getValue() );
                lightScheduleDurMin = mp.getValue();
                updateGlobalSpace("lightdurationminutes",(byte)lightScheduleDurMin);
                d.dismiss();
            }
        });
        b2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss(); // dismiss the dialog
            }
        });
        d.show();
    }

    public void showHourPicker(){
        final Dialog d = new Dialog(LightController.this);
        d.setTitle("Choose Hourly Duration");
        d.setContentView(R.layout.dailoghour);
        Button b1 = (Button) d.findViewById(R.id.cancel);
        Button b2 = (Button) d.findViewById(R.id.set);
        final NumberPicker hp = (NumberPicker) d.findViewById(R.id.HourPicker);
        hp.setMaxValue(12); // max value 12
        hp.setMinValue(1);   // min value 1
        hp.setWrapSelectorWheel(false);
        hp.setOnValueChangedListener(this);
         b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                //tv.setText(String.valueOf(np.getValue())); //set the value to textview
                Log.w(TAG,"Hour Window Chosen : "+ hp.getValue() );
                lightScheduleWindow = hp.getValue();
                lightScheduleDurHour = ((globalData)activity.getApplication()).getAquaLightChar("lightdurationhours");
                if(lightScheduleDurHour > lightScheduleWindow){
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(activity);

                    dlgAlert.setMessage("Duration can not be greater than hourly recurrence!");
                    dlgAlert.setTitle("Error Message...");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();

                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                }else{
                    Log.w(TAG,"All good updating the hourly duration");
                    updateGlobalSpace("hourly",(byte)lightScheduleWindow);
                }

                d.dismiss();
            }
        });
        b2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss(); // dismiss the dialog
            }
        });
        d.show();
    }

    private SlideDateTimeListener listener = new SlideDateTimeListener() {
        @Override
        public void onDateTimeSet(Calendar date)
        {
            // Do something with the date. This Date object contains
            // the date and time that the user has selected.
            Log.e(TAG, "onDateTimeSet:" +date.get(Calendar.DAY_OF_MONTH));
            Log.e(TAG, "onDateTimeSet:" +date.get(Calendar.DAY_OF_WEEK));
            lightScheduleDayOfMonth = date.get(Calendar.DAY_OF_MONTH);
            updateGlobalSpace("lightdom",(byte)lightScheduleDayOfMonth);
            lightScheduleDayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            updateGlobalSpace("lightdow",(byte)lightScheduleDayOfWeek);
            Log.e(TAG, "onDateTimeSet:" +date.get(Calendar.HOUR_OF_DAY));
            lightScheduleHour = date.get(Calendar.HOUR_OF_DAY);
            updateGlobalSpace("lighthours",(byte)lightScheduleHour);
            Log.e(TAG, "onDateTimeSet:" +date.get(Calendar.MINUTE));
            lightScheduleMin = date.get(Calendar.MINUTE);
            updateGlobalSpace("lightminutes",(byte)lightScheduleMin);
        }

        @Override
        public void onDateTimeCancel()
        {
            // Overriding onDateTimeCancel() is optional.
            Log.e(TAG, "onDateTimeCancel");
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
                final byte[]scheduledeviceData;
                if ((scheduledeviceData = intent.getExtras().getByteArray("LIGHTSchedule")) != null) {
                    Log.w(TAG,"mGattDataUpdateReceiver : Got the Light Schedule data in App");
                    ByteBuffer scheduleBuffer = ByteBuffer.wrap(scheduledeviceData);
                    Log.w(TAG, "broadcastUpdate: DeviceSchedule Buffer Length "+ scheduledeviceData.length);
                    ((globalData) activity.getApplication()).setAquaMotorChar("motormode", (scheduleBuffer.get(0)));

                    updateGlobalSpace("maxdevices",(byte) scheduleBuffer.get(1));
                    updateGlobalSpace("lightmode",(scheduleBuffer.get(2)));
                    updateGlobalSpace("lightstatus",(scheduleBuffer.get(3)));
                    updateGlobalSpace("lightdom",(scheduleBuffer.get(4)));
                    updateGlobalSpace("lightdow",(scheduleBuffer.get(5)));
                    updateGlobalSpace("hourly",(scheduleBuffer.get(6)));
                    updateGlobalSpace("lighthours",(scheduleBuffer.get(7)));
                    updateGlobalSpace("lightminutes",(scheduleBuffer.get(8)));
                    updateGlobalSpace("lightrecurrence",(scheduleBuffer.get(9)));
                    updateGlobalSpace("lightdurationhours",(scheduleBuffer.get(10)));
                    updateGlobalSpace("lightdurationminutes",(scheduleBuffer.get(11)));
                    displaySchedule();
                }
            }
        }
    };

    private void displaySchedule(){
        statusLight = ((globalData)activity.getApplication()).getAquaLightChar("lightstatus");
        SimpleDateFormat displayDate = new SimpleDateFormat("EEE dd/MMM/yyyyy");
        Calendar date = Calendar.getInstance();
        int hourly, incr_mnth = 1;
        String duration;

        if(statusLight != 0) {
            lightSwitch.setChecked(true);
        } else {
            lightSwitch.setChecked(false);
        }

        Log.w(TAG, "displaySchedule : Schedule Available ");
        String scheduleRecurrence = null;
        int recurrence = ((globalData) activity.getApplication()).getAquaLightChar("lightrecurrences");
        switch (recurrence) {
            case 1:
                scheduleRecurrence = "Schedule : Daily";
                break;
            case 2:
                scheduleRecurrence = "Schedule : Weekly";
                break;
            case 3:
                scheduleRecurrence = "Schedule : Monthly";
                break;
            case 4:
                scheduleRecurrence = "Schedule : Hourly";
                break;
            default:
                scheduleRecurrence = "No Schedules";
                break;
        }
        // Display the upcoming recurrence
        TableRow row1 = (TableRow) scheduleView.getChildAt(0);
        TextView et = (TextView) row1.getChildAt(0);
        et.setText(scheduleRecurrence);

        // Display next few upcoming schedule time/duration
        TableRow row3 = (TableRow) scheduleView.getChildAt(2);
        this.ti_hh = ((globalData) activity.getApplication()).getAquaLightChar("lighthours");
        this.ti_mm = ((globalData) activity.getApplication()).getAquaLightChar("lightminutes");
        hourly = ((globalData) activity.getApplication()).getAquaLightChar("hourly");
        time_int_string();

        duration = Integer.toString(((globalData) activity.getApplication()).getAquaLightChar("lightdurationhours")) + "h";
        duration += ":" + Integer.toString(((globalData) activity.getApplication()).getAquaLightChar("lightdurationminutes")) + "m";
        for (int i = 0; i < row3.getChildCount(); i++) {
            TextView col = (TextView)row3.getChildAt(i);
            switch (recurrence) {
                case 1:
                    col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode + "\n" + duration);
                    break;
                case 2:
                    if (date.get(Calendar.DAY_OF_WEEK) == (int)((globalData) activity.getApplication()).getAquaLightChar("lightdow")) {
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode + "\n" + duration);
                    } else {
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n\n00:00");
                    }
                    break;
                case 3:
                    if (date.get(Calendar.DAY_OF_MONTH) == (int)((globalData) activity.getApplication()).getAquaLightChar("lightdom")) {
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode + "\n" + duration);
                    } else {
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n\n00:00");
                    }
                    break;
                case 4:
                    col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode + "\n" + duration);
                    this.ti_hh += hourly;
                    incr_mnth = this.ti_hh / 24;
                    time_int_string();
                    break;
                default:
                    col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n\n00:00");
                    break;
            }
            date.add(Calendar.DAY_OF_MONTH, incr_mnth);
        }
    }

    private void time_int_string() {
        int ti_hh = this.ti_hh;
        this.time_mode = " am";

        if (this.ti_hh >= 12) {
            ti_hh = this. ti_hh == 12 ? 12 : this.ti_hh % 12;
            this.time_mode = " pm";
        }

        if (ti_hh == 0) {
            ti_hh = 12;
            this.time_mode = " am";
        }

        if (this.ti_hh >= 24) {
            this.ti_hh -= 24;
            this.time_mode = " am";
        }

        this.time = Integer.toString(ti_hh) + ":";
        if (this.ti_mm < 10) {
            this.time += "0";
        }
        this.time += Integer.toString(this.ti_mm);
    }

    private void updateGlobalSpace(String s,byte val){

        ((globalData)activity.getApplication()).setAquaLightChar(s,val);
    }

    private void sendLightCustomCharacteristicDatafromGlobalStructure(){
        byte[]lightScheduleData = new byte[12];

        Log.w(TAG, "lightScheduleButton "+lightScheduleData[0]);

        lightScheduleData[0] = (byte)0x80;
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("maxdevices"));
        lightScheduleData[1] = ((globalData)activity.getApplication()).getAquaLightChar("maxdevices");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightmode"));
        lightScheduleData[2] = ((globalData)activity.getApplication()).getAquaLightChar("lightmode");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightstatus"));
        lightScheduleData[3] = ((globalData)activity.getApplication()).getAquaLightChar("lightstatus");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightdom"));
        lightScheduleData[4] = ((globalData)activity.getApplication()).getAquaLightChar("lightdom");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightdow"));
        lightScheduleData[5] = ((globalData)activity.getApplication()).getAquaLightChar("lightdow");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("hourly"));
        lightScheduleData[6] = ((globalData)activity.getApplication()).getAquaLightChar("hourly");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lighthours"));
        lightScheduleData[7] = ((globalData)activity.getApplication()).getAquaLightChar("lighthours");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightminutes"));
        lightScheduleData[8] = ((globalData)activity.getApplication()).getAquaLightChar("lightminutes");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightrecurrences"));
        lightScheduleData[9] = ((globalData)activity.getApplication()).getAquaLightChar("lightrecurrences");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightdurationhours"));
        lightScheduleData[10] = ((globalData)activity.getApplication()).getAquaLightChar("lightdurationhours");
        Log.w(TAG, "lightScheduleButton "+((globalData)activity.getApplication()).getAquaLightChar("lightdurationminutes"));
        lightScheduleData[11] = ((globalData)activity.getApplication()).getAquaLightChar("lightdurationminutes");

        Log.w(TAG," Writing Schedule details over BLE");
        mLightBluetoothService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_LIGHT_CHARACTERISTIC,lightScheduleData);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mLightBluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mLightBluetoothService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mLightBluetoothService = null;
        }
    };

    @Override
    public void onBackPressed(){
        if(isRecieverRegistered == true) {
            unregisterReceiver(mGattUpdateReceiver);
            isRecieverRegistered = false;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mLightBluetoothService = null;
    }
}
