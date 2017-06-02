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
import android.widget.TableRow;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.EditText;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MotorController extends FragmentActivity implements AdapterView.OnItemSelectedListener,NumberPicker.OnValueChangeListener{

    public int motorScheduleDayOfWeek = 0;
    public int motorScheduleDayOfMonth = 0;
    public int motorScheduleHour = 0;
    public int motorScheduleMin = 0;
    public int motorScheduleDurHour = 0;
    public int motorScheduleDurMin = 0;
    public int motorScheduleWindow = 0;
    private Byte statusMotor;
    private Byte calibrationState;
    private Byte statusValve;
    public Button motorCalibrateButton;
    public Button motorScheduleTriggerButton;
    private boolean productFlavor;
    boolean isRecieverRgistered = false;

    private Activity activity = this;
    private TableLayout scheduleView;
    private Switch motorSwitch;
    private Switch valveSwitch;
    private Button motorScheduleButton;
    private byte calibrateButtonState;
    private final static String TAG = MotorController.class.getSimpleName();
    private BluetoothLeService motorBluetoothService;
    private int ti_hh, ti_mm;
    private String time, time_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_controller);
        getActionBar().setTitle(R.string.motor_control);

        /* BLE Service Binder*/
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        if(motorBluetoothService == null) {
            Log.w(TAG,"Bluetooth Service available");
        } else {
            Log.w(TAG,"Could not fetch the bluetooth service");
        }

        /* Register the calibration update receiver*/
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        isRecieverRgistered = true;

        /* Displaying schedule view */
        scheduleView = (TableLayout) findViewById(R.id.scheduleDetails);
        /*Displaying in the Valve switch*/
        valveSwitch = (Switch) findViewById(R.id.myValveSwitch);
        /*Displaying in the Motor switch*/
        motorSwitch = (Switch) findViewById(R.id.myMotorSwitch);
        /* Displaying the Date and time Picker*/
        motorScheduleButton = (Button) findViewById(R.id.myMotorSchedule);
        /* Displaying Calibrate button */
        motorCalibrateButton = (Button) findViewById(R.id.calibrate);
        /* Displaying schedule button */
        motorScheduleTriggerButton = (Button) findViewById(R.id.scheduleTrigger);
        /* Select the frequency*/
        Spinner spinner = (Spinner) findViewById(R.id.spinSelect);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.repeat));
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setSelection(0, true);
        spinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        /* Update the display area*/
        displaySchedule();
        productFlavor = ((globalData)activity.getApplication()).getProductFlavor();

        motorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
            byte[]data = new byte[2];
            data[0] = 0x00; // manual mode

            if(isChecked) {
                Log.w(TAG,"Write On");
                data[1]= 0x11;
                Log.w(TAG,"Motor Mode : "+data[0]);
                Log.w(TAG,"Motor Pump Status : "+data[1]);
            } else {
                Log.w(TAG,"Write Off");
                data[1]= 0x10;
                Log.w(TAG,"Motor Mode : "+data[0]);
                Log.w(TAG,"Motor Pump Status : "+data[1]);
            }

            calibrationState = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");
            if((calibrationState < 3 ) || (calibrationState > 5)) {
                updateGlobalSpace("motormode", data[0]);
                updateGlobalSpace("motorpump", data[1]);
                Log.w(TAG, " Writing Motor Switch Status BLE");
                sendMotorCustomCharacteristicDatafromGlobalStructure();
                updateGlobalSpace("motormode", calibrationState);
            } else {
                Log.w(TAG, " Could not Write the motor switch manual status as calibration is in progress");
            }
            }
        });

        valveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
            byte[]data = new byte[2];
            data[0] = 0x00; // manual mode

            if(isChecked) {
                Log.w(TAG,"Write On Valve");
                data[1]= 0x11;
                Log.w(TAG," Valve Status : "+data[1]);
            } else {
                Log.w(TAG,"Write Off");
                data[1]= 0x10;
                Log.w(TAG,"Motor Valve Status : "+data[1]);
            }

            calibrationState = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");
            updateGlobalSpace("motormode", data[0]);
            updateGlobalSpace("motorvalve",data[1]);
            Log.w(TAG," Writing Valve Switch Status BLE");
            sendMotorCustomCharacteristicDatafromGlobalStructure();
            updateGlobalSpace("motormode", calibrationState);
            }
        });

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

        calibrateButtonState = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");

        Log.w(TAG, "calibrateButtonState "+calibrateButtonState);
        if((calibrateButtonState == 2) || (calibrateButtonState == 6)) {
            motorCalibrateButton.setText("Calibration Start");
        } else if (calibrateButtonState == 3) {
            motorCalibrateButton.setText("Calibration Stop");
        } else if (calibrateButtonState == 5) {
            motorCalibrateButton.setEnabled(false);
        }

        motorCalibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             if(productFlavor == true) {
                 calibrateButtonState = ((globalData) activity.getApplication()).getAquaMotorChar("motormode");
                 if ((calibrateButtonState == 2) || (calibrateButtonState == 6) || (calibrateButtonState == 0)) {
                     motorCalibrateButton.setText("Calibration Stop");
                        updateGlobalSpace("motormode", (byte) 3);
                 } else if (calibrateButtonState == 3) {
                     motorCalibrateButton.setText("Calibration Start");
                     updateGlobalSpace("motormode", (byte) 4);
                     motorCalibrateButton.setEnabled(false);
                 }
                 sendMotorCustomCharacteristicDatafromGlobalStructure();
             } else {
                 AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(activity);
                 dlgAlert.setMessage("This is a Demo Version.Please Purchase the Full Version for Calibration");
                 dlgAlert.setTitle("Warning...");
                 dlgAlert.setPositiveButton("OK", null);
                 dlgAlert.setCancelable(true);
                 dlgAlert.create().show();

                 dlgAlert.setPositiveButton("Ok",
                     new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int which) {
                         }
                     });
             }
            }
        });

        // Check calibration state
        if (calibrateButtonState != 6) {
            motorScheduleTriggerButton.setEnabled(false);
        }

        motorScheduleTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(productFlavor == true) {
                    calibrationState = ((globalData) activity.getApplication()).getAquaMotorChar("motormode");
            /* Update the display area*/
                    displaySchedule();
                    if (calibrationState == 6) {
                        updateGlobalSpace("motormode", (byte) 1);
                /* Write data to the custom characteristics*/
                        sendMotorCustomCharacteristicDatafromGlobalStructure();
                        updateGlobalSpace("motormode", calibrationState);
                    }
                }
                else
                {
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(activity);

                    dlgAlert.setMessage("This is a Demo Version.Please Purchase the Full Version for Scheduling Functionality");
                    dlgAlert.setTitle("Warning...");
                    dlgAlert.setPositiveButton("OK", null);
                    dlgAlert.setCancelable(true);
                    dlgAlert.create().show();

                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                }
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
                    if(scheduleBuffer.get(0) == 6) {
                        motorScheduleTriggerButton.setEnabled(true);
                    }

                    if (scheduleBuffer.get(0) == 2 || scheduleBuffer.get(0) == 6) {
                        motorCalibrateButton.setText("Calibration Start");
                        motorCalibrateButton.setEnabled(true);
                    }

                    updateGlobalSpace("motormode",(byte) scheduleBuffer.get(0));
                    updateGlobalSpace("motorpump",(scheduleBuffer.get(1)));
                    updateGlobalSpace("motorvalve",(scheduleBuffer.get(2)));
                    updateGlobalSpace("motordom",(scheduleBuffer.get(3)));
                    updateGlobalSpace("motordow",(scheduleBuffer.get(4)));
                    updateGlobalSpace("hourly",(scheduleBuffer.get(5)));
                    updateGlobalSpace("motorhours",(scheduleBuffer.get(6)));
                    updateGlobalSpace("motorminutes",(scheduleBuffer.get(7)));
                    updateGlobalSpace("motorrecurrence",(scheduleBuffer.get(8)));
                    updateGlobalSpace("motordurationhours",(scheduleBuffer.get(9)));
                    updateGlobalSpace("motordurationminutes",(scheduleBuffer.get(10)));
                    displaySchedule();

                    Log.w(TAG, "Calibration Notification"+((globalData)activity.getApplication()).getAquaMotorChar("motormode"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorpump"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorvalve"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motordow"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("hourly"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorhours"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorminutes"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours"));
                    Log.w(TAG, "Calibration Notification "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes"));
                }
            }
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id)
    {
        Log.w(TAG,"onItemSelected"+ parent.getSelectedItem());
        switch((String)parent.getSelectedItem()){
            case "Disable Schedule" :
                updateGlobalSpace("motorrecurrence",(byte)0);
                break;
            case "Daily" :
                updateGlobalSpace("motorrecurrence",(byte)1);
                break;
            case "Weekly" :
                updateGlobalSpace("motorrecurrence",(byte)2);
                break;
            case "Monthly" :
                updateGlobalSpace("motorrecurrence",(byte)3);
                break;
            case "Hourly" :
                showHourPicker();
                updateGlobalSpace("motorrecurrence",(byte)4);
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

    public void showHourPicker(){
        final Dialog d = new Dialog(MotorController.this);
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
                motorScheduleWindow = hp.getValue();

                updateGlobalSpace("hourly",(byte)motorScheduleWindow);
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
            motorScheduleDayOfMonth = date.get(Calendar.DAY_OF_MONTH);
            updateGlobalSpace("motordom",(byte)motorScheduleDayOfMonth);
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
        /*Displaying in the Motor switch*/
        statusMotor = ((globalData)activity.getApplication()).getAquaMotorChar("motorpump");
        SimpleDateFormat displayDate = new SimpleDateFormat("EEE dd/MMM/yyyyy");
        Calendar date = Calendar.getInstance();
        int hourly, incr_mnth = 1;

        if(statusMotor == 0x11) {
            motorSwitch.setChecked(true);
        } else {
            motorSwitch.setChecked(false);
        }

        /*Displaying in the Valve switch*/
        statusValve = ((globalData)activity.getApplication()).getAquaMotorChar("motorvalve");
        if(statusValve == 0x11) {
            valveSwitch.setChecked(true);
        } else {
            valveSwitch.setChecked(false);
        }

        if((((globalData)activity.getApplication()).getAquaMotorChar("motormode")) == 6){
            Log.w(TAG, "displaySchedule : Motor Schedule Available ");
            String scheduleRecurrence = null;
            int recurrence = ((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence");
            switch (recurrence){
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
                    // Display the upcoming recurrence
                    scheduleRecurrence = "No Schedules";
            }

            // Display the upcoming recurrence
            TableRow row1 = (TableRow) scheduleView.getChildAt(0);
            TextView et = (TextView) row1.getChildAt(0);
            et.setText(scheduleRecurrence);

            // Display next few upcoming schedule time/duration
            TableRow row3 = (TableRow) scheduleView.getChildAt(2);
            this.ti_hh = ((globalData) activity.getApplication()).getAquaMotorChar("motorhours");
            this.ti_mm = ((globalData) activity.getApplication()).getAquaMotorChar("motorminutes");
            hourly = ((globalData) activity.getApplication()).getAquaMotorChar("hourly");
            time_int_string();

            for (int i= 0; i < row3.getChildCount() ; i++) {
                TextView col = (TextView)row3.getChildAt(i);
                switch (recurrence) {
                    case 1:
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" +this. time + this.time_mode);
                        break;
                    case 2:
                        if (date.get(Calendar.DAY_OF_WEEK) == (int)((globalData) activity.getApplication()).getAquaMotorChar("motordow")) {
                            col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode );
                        } else {
                            col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n");
                        }
                        break;
                    case 3:
                        if (date.get(Calendar.DAY_OF_MONTH) == (int)((globalData) activity.getApplication()).getAquaMotorChar("motordom")) {
                            col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode);
                        } else {
                            col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n");
                        }
                        break;
                    case 4:
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) + "\n" + this.time + this.time_mode);
                        this.ti_hh += hourly;
                        incr_mnth = this.ti_hh / 24;
                        time_int_string();
                        break;
                    default:
                        col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n");
                        break;
                }
                    date.add(Calendar.DAY_OF_MONTH, incr_mnth);
            }
        } else {
            // Display the upcoming recurrence
            TableRow row1= (TableRow)scheduleView.getChildAt(0);
            TextView et = (TextView )row1.getChildAt(0);
            et.setText("Not yet calibrated.To schedule water maintenance,Calibrate the motor atleast once please");
            // Display next few upcoming schedule time/duration
            TableRow row3 = (TableRow) scheduleView.getChildAt(2);
            for (int i= 0; i < row3.getChildCount() ; i++) {
                TextView col = (TextView)row3.getChildAt(i);
                col.setText(displayDate.format(date.getTime()).substring(0, 3) +"\n00:00\n");
                date.add(Calendar.DAY_OF_MONTH, incr_mnth);
            }
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

    private void sendMotorCustomCharacteristicDatafromGlobalStructure()
    {
        byte[]motorScheduleData = new byte[11];
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motormode"));
        motorScheduleData[0] = ((globalData)activity.getApplication()).getAquaMotorChar("motormode");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorpump"));
        motorScheduleData[1] = ((globalData)activity.getApplication()).getAquaMotorChar("motorpump");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorvalve"));
        motorScheduleData[2] = ((globalData)activity.getApplication()).getAquaMotorChar("motorvalve");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordom"));
        motorScheduleData[3] = ((globalData)activity.getApplication()).getAquaMotorChar("motordom");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordow"));
        motorScheduleData[4] = ((globalData)activity.getApplication()).getAquaMotorChar("motordow");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("hourly"));
        motorScheduleData[5] = ((globalData)activity.getApplication()).getAquaMotorChar("hourly");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorhours"));
        motorScheduleData[6] = ((globalData)activity.getApplication()).getAquaMotorChar("motorhours");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorminutes"));
        motorScheduleData[7] = ((globalData)activity.getApplication()).getAquaMotorChar("motorminutes");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence"));
        motorScheduleData[8] = ((globalData)activity.getApplication()).getAquaMotorChar("motorrecurrence");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours"));
        motorScheduleData[9] = ((globalData)activity.getApplication()).getAquaMotorChar("motordurationhours");
        Log.w(TAG, "motorScheduleButton "+((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes"));
        motorScheduleData[10] = ((globalData)activity.getApplication()).getAquaMotorChar("motordurationminutes");

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
    public void onBackPressed(){
        if(isRecieverRgistered == true) {
            unregisterReceiver(mGattUpdateReceiver);
            isRecieverRgistered = false;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        motorBluetoothService = null;
    }
}
