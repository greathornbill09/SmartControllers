package com.hornbill.great.connectingsmartthings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.Spinner;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;

import java.util.Calendar;
import java.util.Date;

public class controlPage extends Fragment implements AdapterView.OnItemSelectedListener,NumberPicker.OnValueChangeListener{


    public int lightScheduleDayOfMonth = 0;
    public int lightScheduleDayOfWeek = 0;
    public int lightScheduleHour = 0;
    public int lightScheduleMin = 0;
    public int lightScheduleDurHour = 0;
    public int lightScheduleWindow = 0;
    public int lightScheduleDurMin = 0;
    public int deviceIdentifier;

    private BluetoothLeService mLightBluetoothService;
    private final static String TAG = controlPage.class.getSimpleName();
    private Button deviceScheduleButton;
    private Button deviceScheduleDurationButton;
    private Button deviceScheduleTriggerButton;
    private Byte statusLight;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* BLE Service Binder*/
        Intent gattServiceIntent = new Intent(getContext(), BluetoothLeService.class);
        getActivity().bindService(gattServiceIntent, mServiceConnection, getContext().BIND_AUTO_CREATE);

        if (mLightBluetoothService == null) {
            Log.w(TAG,"Bluetooth Service available");
        } else {
            Log.w(TAG,"Could not fetch the bluetooth service");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView;

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_control_page, container, false);

        /* ON/OFF Switch Functionality*/
        SwitchCompat switchCompat = (SwitchCompat) rootView.findViewById(R.id.switch_compat);
        statusLight = ((globalData)getActivity().getApplication()).getAquaLightChar("lightstatus");

        if(statusLight == 1)
        {
            switchCompat.setChecked(true);
        }

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                byte[]data = new byte[9];
                if(isChecked == true)
                {
                    Log.e(TAG, "Light Switch Checked");
                    data[1]= 1;
                    Log.w(TAG,"Light Mode : "+data[0]);
                    updateGlobalSpace("lightmode",data[0]);
                    Log.w(TAG,"Light Status : "+data[1]);
                    updateGlobalSpace("lightstatus",data[1]);
                }
                else
                {
                    Log.e(TAG, "Light switch not checked");
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


        /* DATE/TIME Picker functionality*/
        deviceScheduleButton = (Button) rootView.findViewById(R.id.mySchedule);
        deviceScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG,"Schedule Button Clicked");
                new SlideDateTimePicker.Builder(getActivity().getSupportFragmentManager())
                        .setListener(listener)
                        .setInitialDate(new Date())
                        .build()
                        .show();
            }
        });


        /*Displaying schedule duration button */
        deviceScheduleDurationButton = (Button) rootView.findViewById(R.id.myScheduleDuration);
        deviceScheduleDurationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w(TAG,"Schedule Duration Button Clicked");
                showHourMinPicker();
            }

        });


        /* Select the frequency*/
        Spinner spinner = (Spinner) rootView.findViewById(R.id.spinSelect);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,getResources().getStringArray(R.array.repeat));
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(0, true);


        /* Write the schedule */
        deviceScheduleTriggerButton = (Button) rootView.findViewById(R.id.scheduleTrigger);
        deviceScheduleTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateGlobalSpace("lightmode", (byte) 1);
            /* Write data to the custom characteristics*/
                sendLightCustomCharacteristicDatafromGlobalStructure();
            }
        });

        return rootView;
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
                lightScheduleDurHour = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationhours");
                Log.w(TAG,"lightScheduleDurHour"+lightScheduleDurHour );
                if(lightScheduleDurHour == 0)
                {
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(getActivity());

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
        final Dialog d = new Dialog(getContext());
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
        final Dialog d = new Dialog(getContext());
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
                lightScheduleDurHour = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationhours");
                if(lightScheduleDurHour > lightScheduleWindow){
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(getActivity());

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


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mLightBluetoothService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mLightBluetoothService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                getActivity().finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mLightBluetoothService = null;
        }
    };

    private void updateGlobalSpace(String s,byte val){

        ((globalData)getActivity().getApplication()).setAquaLightChar(s,val);
    }



    private void sendLightCustomCharacteristicDatafromGlobalStructure(){
        byte[]lightScheduleData = new byte[12];

        Log.w(TAG, "lightScheduleButton "+lightScheduleData[0]);

        lightScheduleData[0] = (byte)0x80;
        lightScheduleData[0] |= deviceIdentifier;
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("maxdevices"));
        lightScheduleData[1] = ((globalData)getActivity().getApplication()).getAquaLightChar("maxdevices");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightmode"));
        lightScheduleData[2] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightmode");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightstatus"));
        lightScheduleData[3] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightstatus");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdom"));
        lightScheduleData[4] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdom");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdow"));
        lightScheduleData[5] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdow");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("hourly"));
        lightScheduleData[6] = ((globalData)getActivity().getApplication()).getAquaLightChar("hourly");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lighthours"));
        lightScheduleData[7] = ((globalData)getActivity().getApplication()).getAquaLightChar("lighthours");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightminutes"));
        lightScheduleData[8] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightminutes");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightrecurrences"));
        lightScheduleData[9] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightrecurrences");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationhours"));
        lightScheduleData[10] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationhours");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationminutes"));
        lightScheduleData[11] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationminutes");

        Log.w(TAG," Writing Schedule details over BLE");
        mLightBluetoothService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_LIGHT_CHARACTERISTIC,lightScheduleData);
    }
}
