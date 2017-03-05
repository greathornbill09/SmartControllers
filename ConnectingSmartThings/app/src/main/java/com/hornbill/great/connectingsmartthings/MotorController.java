package com.hornbill.great.connectingsmartthings;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
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

import java.util.Calendar;
import java.util.Date;

public class MotorController extends FragmentActivity implements AdapterView.OnItemSelectedListener,NumberPicker.OnValueChangeListener{

    public int motorScheduleDayOfWeek = 0;
    public int motorScheduleHour = 0;
    public int motorScheduleMin = 0;
    public int motorScheduleDurHour = 0;
    public int motorScheduleDurMin = 0;
    private Byte statusMotor;



    private Activity activity = this;
    private TextView scheduleView;
    private Switch motorSwitch;
    private Switch calibrateMotorSwitch;
    private Button motorScheduleButton;
    private Button motorScheduleDurationButton;
    private Button motorScheduleTriggerButton;
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


        scheduleView = (TextView) findViewById(R.id.scheduleDetails);
        /* Update the display area*/
        displaySchedule();


        /*Displaying in the Motor switch*/
        motorSwitch = (Switch) findViewById(R.id.myMotorSwitch);
        statusMotor = ((globalData)activity.getApplication()).getAquaMotorChar("motorpump");
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
                    data[2]= 0x11;
                    Log.w(TAG,"Motor Mode : "+data[0]);
                    updateGlobalSpace("motormode",data[0]);
                    Log.w(TAG,"Motor Pump Status : "+data[1]);
                    Log.w(TAG,"Motor Valve Status : "+data[1]);
                    updateGlobalSpace("motorpump",data[1]);
                    updateGlobalSpace("motorvalve",data[2]);
                }else{
                    Log.w(TAG,"Write Off");
                    data[1]= 0x10;
                    data[2]= 0x10;
                    Log.w(TAG,"Motor Mode : "+data[0]);
                    updateGlobalSpace("motormode",data[0]);
                    Log.w(TAG,"Motor Pump Status : "+data[1]);
                    Log.w(TAG,"Motor Valve Status : "+data[1]);
                    updateGlobalSpace("motorpump",data[1]);
                    updateGlobalSpace("motorvalve",data[2]);
                }
                Log.w(TAG," Writing Motor Switch Status BLE");
                motorBluetoothService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_MOTOR_CHARACTERISTIC,data);
            }
        });

        calibrateMotorSwitch = (Switch) findViewById(R.id.myCalibrateSwitch);
        calibrateMotorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                /*byte[]data = new byte[9];
                if(isChecked){
                    Log.w(TAG,"Write On");
                    data[1]= 0x11;
                    data[2]= 0x11;
                    Log.w(TAG,"Motor Mode : "+data[0]);
                    updateGlobalSpace("motormode",data[0]);
                    Log.w(TAG,"Motor Pump Status : "+data[1]);
                    Log.w(TAG,"Motor Valve Status : "+data[1]);
                    updateGlobalSpace("motorpump",data[1]);
                    updateGlobalSpace("motorvalve",data[2]);
                }else{
                    Log.w(TAG,"Write Off");
                    data[1]= 0x10;
                    data[2]= 0x10;
                    Log.w(TAG,"Motor Mode : "+data[0]);
                    updateGlobalSpace("motormode",data[0]);
                    Log.w(TAG,"Motor Pump Status : "+data[1]);
                    Log.w(TAG,"Motor Valve Status : "+data[1]);
                    updateGlobalSpace("motorpump",data[1]);
                    updateGlobalSpace("motorvalve",data[2]);
                }*/
                Log.w(TAG," Writing Motor Calibrate Switch Status BLE");
                //motorBluetoothService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_MOTOR_CHARACTERISTIC,data);
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



        motorScheduleTriggerButton = (Button) findViewById(R.id.scheduleTrigger);

        motorScheduleTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                byte[]motorScheduleData = new byte[9];

                motorScheduleData[0] = (byte) 1;
                Log.w(TAG, "motorScheduleButton "+motorScheduleData[0]);
                updateGlobalSpace("motormode",motorScheduleData[0]);
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

                /* Update the display area*/
                displaySchedule();

            }
        });



    }

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
            scheduleView.setText("Upcoming Schedule\n" +
                    "Time:"+ Byte.toString(((globalData)activity.getApplication()).getAquaMotorChar("motorhours"))+" Hrs "+
                    Byte.toString(((globalData)activity.getApplication()).getAquaMotorChar("motorminutes"))+" Min\n"+
                    "Duration:"+ Byte.toString(((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours"))+" Hrs "+
                    Byte.toString(((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes"))+" Min\n"+
                    "Freq:"+scheduleRecurrence);


        }
        else
        {
            Log.w(TAG, "displaySchedule : Schedule Not Available ");
            scheduleView.setText("No Schedules Available yet !!!");
        }

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
                Intent intent = new Intent(this, IntroActivity.class);
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
