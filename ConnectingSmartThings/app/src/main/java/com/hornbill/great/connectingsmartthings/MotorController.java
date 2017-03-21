package com.hornbill.great.connectingsmartthings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;

public class MotorController extends FragmentActivity implements AdapterView.OnItemSelectedListener,NumberPicker.OnValueChangeListener{

    public int motorScheduleDayOfWeek = 0;
    public int motorScheduleHour = 0;
    public int motorScheduleMin = 0;
    public int motorScheduleDurHour = 0;
    public int motorScheduleDurMin = 0;
    private Byte statusMotor;
    private Byte calibrationState;
    private Byte statusValve;
    public Button motorCalibrateButton;
    public Button motorScheduleTriggerButton;



    private Activity activity = this;
    private TextView scheduleView;
    private Switch motorSwitch;
    private Switch valveSwitch;
    private Button motorScheduleButton;
    private Button motorScheduleDurationButton;
    private byte calibrateButtonState;
    private final static String TAG = MotorController.class.getSimpleName();
    private BluetoothLeService motorBluetoothService;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_controller);
        getActionBar().setTitle(R.string.motor_control);

        /* BLE Service Binder*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if(motorBluetoothService == null)
        {
            Log.w(TAG,"Bluetooth Service available");
        }else
        {
            Log.w(TAG,"Could not fetch the bluetooth service");
        }


        /* Register the calibration update reciever*/
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());


        scheduleView = (TextView) findViewById(R.id.scheduleDetails);
        /* Update the display area*/
        displaySchedule();


        /*Displaying in the Motor switch*/
        motorSwitch = (Switch) findViewById(R.id.myMotorSwitch);
        statusMotor = ((globalData)activity.getApplication()).getAquaMotorChar("motorpump");
        calibrationState = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");
        if(statusMotor == 0x11)
        {
            motorSwitch.setChecked(true);
        }
        motorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                byte[]data = new byte[9];
                if(isChecked){
                    Log.w(TAG,"Write On");
                    data[1]= 0x11;
                    Log.w(TAG,"Motor Mode : "+data[0]);

                    Log.w(TAG,"Motor Pump Status : "+data[1]);
                }else{
                    Log.w(TAG,"Write Off");
                    data[1]= 0x10;
                    Log.w(TAG,"Motor Mode : "+data[0]);
                    Log.w(TAG,"Motor Pump Status : "+data[1]);
                }
                if((calibrationState <3 ) && (calibrationState > 5)) {
                    updateGlobalSpace("motormode", data[0]);
                    updateGlobalSpace("motorpump", data[1]);
                    Log.w(TAG, " Writing Motor Switch Status BLE");
                    sendMotorCustomCharacteristicDatafromGlobalStructure();
                }
                else
                {
                    Log.w(TAG, " Could not Write the motor switch manual status as calibration is in progress");
                }
            }
        });


        /*Displaying in the Motor switch*/
        valveSwitch = (Switch) findViewById(R.id.myValveSwitch);
        statusValve = ((globalData)activity.getApplication()).getAquaMotorChar("motorvalve");
        if(statusValve == 0x11)
        {
            valveSwitch.setChecked(true);
        }
        valveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                byte[]data = new byte[9];
                if(isChecked){
                    Log.w(TAG,"Write On vALVE");
                    data[2]= 0x11;
                    Log.w(TAG," Valve Status : "+data[2]);
                }else{
                    Log.w(TAG,"Write Off");
                    data[2]= 0x10;
                    Log.w(TAG,"Motor Valve Status : "+data[2]);
                }
                if((calibrationState <3 ) && (calibrationState > 5)){
                    updateGlobalSpace("motorvalve",data[2]);
                    Log.w(TAG," Writing vALVE Switch Status BLE");
                    sendMotorCustomCharacteristicDatafromGlobalStructure();
                }
                else
                {
                    Log.w(TAG, " Could not Write the valve switch manual status as calibration is in progress");
                }

            }
        });


        /* Displaying the Date and time Picker*/
        motorScheduleButton = (Button) findViewById(R.id.myMotorSchedule);
        motorScheduleButton.setOnClickListener(new View.OnClickListener() {
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

        motorScheduleDurationButton = (Button) findViewById(R.id.myMotorScheduleDuration);
        motorScheduleDurationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG,"Schedule Duration Button Clicked");
                showNumPicker();

            }

        });

        /* Select the frequency*/
        Spinner spinner = (Spinner) findViewById(R.id.spinSelect);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.repeat));
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);

// Apply the adapter to the spinner
        spinner.setAdapter(adapter);



        motorCalibrateButton = (Button) findViewById(R.id.calibrate);
        calibrateButtonState = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");

        Log.w(TAG, "calibrateButtonState "+calibrateButtonState);

        if((calibrateButtonState == 2) || (calibrateButtonState == 6))
        {
            motorCalibrateButton.setText("Calibration Start");
        }else if (calibrateButtonState == 3)
        {
            motorCalibrateButton.setText("Calibration Stop");
        }
        else if (calibrateButtonState == 5)
        {
            motorCalibrateButton.setEnabled(false);
        }



        motorCalibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if((calibrateButtonState == 2) || (calibrateButtonState == 6) || (calibrateButtonState == 0)) {
                    motorCalibrateButton.setText("Calibration Stop");
                    updateGlobalSpace("motormode",(byte)3);
                }else if (calibrateButtonState == 3)
                {
                    //motorCalibrateButton.setText("Calibration Start");
                    updateGlobalSpace("motormode",(byte)4);
                }

                sendMotorCustomCharacteristicDatafromGlobalStructure();

            }
        });



        motorScheduleTriggerButton = (Button) findViewById(R.id.scheduleTrigger);

        if (calibrateButtonState != 6)
        {
            motorScheduleTriggerButton.setEnabled(false);
        }


        motorScheduleTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updateGlobalSpace("motormode",(byte) 1);
                /* Write data to the custom characteristics*/
                sendMotorCustomCharacteristicDatafromGlobalStructure();
                /* Update the display area*/
                displaySchedule();

            }
        });






    }


    /* Connection related methods */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_AQUA_MOTOR_CHAR_AVAILABLE);
        return intentFilter;
    }



    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_AQUA_MOTOR_CHAR_AVAILABLE.equals(action)) {
                final byte[]scheduleMotorData;
                if ((scheduleMotorData = intent.getExtras().getByteArray("MOTORSchedule")) != null) {
                    Log.w(TAG,"mGattDataUpdateReceiver : Got the Light Schedule data in App");
                    ByteBuffer scheduleBuffer = ByteBuffer.wrap(scheduleMotorData);
                    Log.w(TAG, "broadcastUpdate: MOTORSchedule Buffer Length "+ scheduleMotorData.length);
                    ((globalData) activity.getApplication()).setAquaMotorChar("motormode", (scheduleBuffer.get(0)));
                    if(scheduleBuffer.get(0) == 6)
                    {
                        motorCalibrateButton.setText("Calibration Start");
                        motorScheduleTriggerButton.setEnabled(true);
                        displaySchedule();
                    }


                    ((globalData)activity.getApplication()).setAquaMotorChar("motorpump",(scheduleBuffer.get(1)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motorvalve",(scheduleBuffer.get(2)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motordow",(scheduleBuffer.get(3)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motorhours",(scheduleBuffer.get(4)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motorminutes",(scheduleBuffer.get(5)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motorrecurrence",(scheduleBuffer.get(6)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motordurationhours",(scheduleBuffer.get(7)));
                    ((globalData)activity.getApplication()).setAquaMotorChar("motordurationminutes",(scheduleBuffer.get(8)));

                    Log.w(TAG, "Calibration Notification"+((globalData)activity.getApplication()).getAquaMotorChar("motormode"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorpump"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorvalve"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motordow"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorhours"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorminutes"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes"));
                }

            }

        }
    };






    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id)
    {
        Log.w(TAG,"onItemSelected"+ parent.getSelectedItem());
        switch((String)parent.getSelectedItem()){

            case "Daily" :
                updateGlobalSpace("motorrecurrence",(byte)1);
                break;
            case "Weekly" :
                updateGlobalSpace("motorrecurrence",(byte)2);
                break;
            case "Monthly" :
                updateGlobalSpace("motorrecurrence",(byte)3);
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
    private void updateGlobalSpace(String s,byte val){

        ((globalData)activity.getApplication()).setAquaMotorChar(s,val);

    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            motorBluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!motorBluetoothService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            motorBluetoothService = null;
        }
    };


    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

        Log.i("value is",""+newVal);

    }


    public void showNumPicker(){
        final Dialog d = new Dialog(MotorController.this);
        d.setTitle("Choose Duration");
        d.setContentView(R.layout.dialog);
        Button b1 = (Button) d.findViewById(R.id.cancel);
        Button b2 = (Button) d.findViewById(R.id.set);
        final NumberPicker hp = (NumberPicker) d.findViewById(R.id.HourPicker);
        hp.setMaxValue(12); // max value 100
        hp.setMinValue(0);   // min value 0
        hp.setWrapSelectorWheel(false);
        final NumberPicker mp = (NumberPicker) d.findViewById(R.id.MinPicker);
        mp.setMaxValue(60); // max value 100
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
                motorScheduleDurHour = hp.getValue();
                updateGlobalSpace("motordurationhours",(byte)motorScheduleDurHour);
                Log.w(TAG,"Min Chosen : "+ mp.getValue() );
                motorScheduleDurMin = mp.getValue();
                updateGlobalSpace("motordurationminutes",(byte)motorScheduleDurMin);
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
            motorScheduleDayOfWeek = date.get(Calendar.DAY_OF_WEEK);
            updateGlobalSpace("motordow",(byte)motorScheduleDayOfWeek);
            Log.e(TAG, "onDateTimeSet:" +date.get(Calendar.HOUR_OF_DAY));
            motorScheduleHour = date.get(Calendar.HOUR_OF_DAY);
            updateGlobalSpace("motorhours",(byte)motorScheduleHour);
            Log.e(TAG, "onDateTimeSet:" +date.get(Calendar.MINUTE));
            motorScheduleMin = date.get(Calendar.MINUTE);
            updateGlobalSpace("motorminutes",(byte)motorScheduleMin);
        }

        @Override
        public void onDateTimeCancel()
        {
            // Overriding onDateTimeCancel() is optional.
            Log.e(TAG, "onDateTimeCancel");
        }
    };


    private void displaySchedule(){
        if((((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence")!= 0)){
            Log.w(TAG, "displaySchedule : Motor Schedule Available ");
            String scheduleRecurrence = null;
            switch (((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence")){
                case 1:
                    scheduleRecurrence = "Daily";
                    break;
                case 2:
                    scheduleRecurrence = "Weekly";
                    break;
                case 3:
                    scheduleRecurrence = "Monthly";
                    break;
                default:
                    break;
            }
            if((((globalData)activity.getApplication()).getAquaMotorChar("motormode")) == 6) {
                scheduleView.setText("Upcoming Schedule\n" +
                        "Time:" + Byte.toString(((globalData) activity.getApplication()).getAquaMotorChar("motorhours")) + " Hrs " +
                        Byte.toString(((globalData) activity.getApplication()).getAquaMotorChar("motorminutes")) + " Min\n" +
                        "Duration:" + Byte.toString(((globalData) activity.getApplication()).getAquaMotorChar("motordurationhours")) + " Hrs " +
                        Byte.toString(((globalData) activity.getApplication()).getAquaMotorChar("motordurationminutes")) + " Min\n" +
                        "Freq:" + scheduleRecurrence);
            }else{
                scheduleView.setText("Not yet calibrated!!! In order to schedule water maintenance,\n" +
                        " please calibrate the motor atleast once");
            }




        }
        else
        {
            if((((globalData)activity.getApplication()).getAquaMotorChar("motormode")) == 6)
            {
                Log.w(TAG, "displaySchedule : Schedule Not Available ");
                scheduleView.setText("No Schedules Available yet !!!");
            }
            else
            {
                scheduleView.setText("Not yet calibrated!!! To schedule water maintenance,\n" +
                        " calibrate the motor\n" +
                        " atleast once");
            }

        }

    }

    private void sendMotorCustomCharacteristicDatafromGlobalStructure()
    {

        byte[]motorScheduleData = new byte[9];
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motormode"));
        motorScheduleData[0] = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorpump"));
        motorScheduleData[1] = ((globalData)activity.getApplication()).getAquaMotorChar("motorpump");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorvalve"));
        motorScheduleData[2] = ((globalData)activity.getApplication()).getAquaMotorChar("motorvalve");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordow"));
        motorScheduleData[3] = ((globalData)activity.getApplication()).getAquaMotorChar("motordow");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorhours"));
        motorScheduleData[4] = ((globalData)activity.getApplication()).getAquaMotorChar("motorhours");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorminutes"));
        motorScheduleData[5] = ((globalData)activity.getApplication()).getAquaMotorChar("motorminutes");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence"));
        motorScheduleData[6] = ((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours"));
        motorScheduleData[7] = ((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes"));
        motorScheduleData[8] = ((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes");

        Log.w(TAG," Writing Schedule details over BLE");
        motorBluetoothService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_MOTOR_CHARACTERISTIC,motorScheduleData);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calibratehelp, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.calibrate_help:
                Intent intent = new Intent(this, CalibrateHelpActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        motorBluetoothService = null;
    }


}
