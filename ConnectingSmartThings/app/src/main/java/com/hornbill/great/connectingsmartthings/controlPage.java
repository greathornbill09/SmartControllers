package com.hornbill.great.connectingsmartthings;

import android.content.ComponentName;
import android.content.Context;
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
import android.widget.CompoundButton;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link controlPage.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link controlPage#newInstance} factory method to
 * create an instance of this fragment.
 */
public class controlPage extends Fragment {

    private BluetoothLeService mLightBluetoothService;
    private final static String TAG = controlPage.class.getSimpleName();

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


        SwitchCompat switchCompat = (SwitchCompat) rootView.findViewById(R.id.switch_compat);

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
        return rootView;
    }


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
        byte[]lightScheduleData = new byte[10];

        Log.w(TAG, "lightScheduleButton "+lightScheduleData[0]);

        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightmode"));
        lightScheduleData[0] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightmode");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightstatus"));
        lightScheduleData[1] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightstatus");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdom"));
        lightScheduleData[2] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdom");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdow"));
        lightScheduleData[3] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdow");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("hourly"));
        lightScheduleData[4] = ((globalData)getActivity().getApplication()).getAquaLightChar("hourly");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lighthours"));
        lightScheduleData[5] = ((globalData)getActivity().getApplication()).getAquaLightChar("lighthours");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightminutes"));
        lightScheduleData[6] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightminutes");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightrecurrences"));
        lightScheduleData[7] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightrecurrences");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationhours"));
        lightScheduleData[8] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationhours");
        Log.w(TAG, "lightScheduleButton "+((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationminutes"));
        lightScheduleData[9] = ((globalData)getActivity().getApplication()).getAquaLightChar("lightdurationminutes");

        Log.w(TAG," Writing Schedule details over BLE");
        mLightBluetoothService.writeDataToCustomCharacteristic(BluetoothLeService.UUID_AQUA_LIGHT_CHARACTERISTIC,lightScheduleData);
    }
}
