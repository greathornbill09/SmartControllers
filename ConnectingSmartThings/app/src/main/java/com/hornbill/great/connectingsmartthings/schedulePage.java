package com.hornbill.great.connectingsmartthings;

import android.content.Context;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.caldroid.*;
import com.roomorama.caldroid.CaldroidFragment;


public class schedulePage extends Fragment {
    public int ti_hh, ti_mm, dow, dom;
    public String duration, time, time_mode;
    public int recurrence;
    private final static String TAG = schedulePage.class.getSimpleName();
CaldroidFragment mcaldroidFragment =  new CaldroidSampleCustomFragment();



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.w(TAG,"onViewCreated in schedule page called");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        Log.w(TAG,"onCreateView in schedule page called");
        CaldroidFragment mcaldroidFragment =  new CaldroidSampleCustomFragment();
        View root = inflater.inflate(R.layout.fragment_schedule_page,container,false);

        Bundle args = new Bundle();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(java.util.Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(java.util.Calendar.YEAR));
        args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true);
        args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, true);


        // Getting the data to display
        this.ti_hh = ((globalData) getActivity().getApplication()).getAquaLightChar("lighthours");
        this.ti_mm = ((globalData) getActivity().getApplication()).getAquaLightChar("lightminutes");
        this.recurrence= ((globalData) getActivity().getApplication()).getAquaLightChar("lightrecurrences");
        this.dow = ((globalData) getActivity().getApplication()).getAquaLightChar("lightdow");
        this.dom = ((globalData) getActivity().getApplication()).getAquaLightChar("lightdom");
        this.duration = Integer.toString(((globalData) getActivity().getApplication()).getAquaLightChar("lightdurationhours")) + "h";
        this.duration += ":" + Integer.toString(((globalData) getActivity().getApplication()).getAquaLightChar("lightdurationminutes")) + "m";

        time_int_string();
        args.putString("time", time);
        args.putString("time_mode", time_mode);
        args.putInt("recurrence", recurrence);
        args.putInt("dow", dow);
        args.putInt("dom", dom);
        args.putString("duration", duration);

        // Uncomment this to customize startDayOfWeek
        // args.putInt(CaldroidFragment.START_DAY_OF_WEEK,
        // CaldroidFragment.TUESDAY); // Tuesday

        // Uncomment this line to use Caldroid in compact mode
        // args.putBoolean(CaldroidFragment.SQUARE_TEXT_VIEW_CELL, false);

        // Uncomment this line to use dark theme
//            args.putInt(CaldroidFragment.THEME_RESOURCE, com.caldroid.R.style.CaldroidDefaultDark);

        mcaldroidFragment.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.cal_container, mcaldroidFragment).commit();

        return  root;
    }



    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser)
        {
            Log.w(TAG,"Schedule page visible");
            CaldroidFragment mcaldroidFragment =  new CaldroidSampleCustomFragment();
            Bundle args = new Bundle();
            // Getting the data to display
            this.ti_hh = ((globalData) getActivity().getApplication()).getAquaLightChar("lighthours");
            this.ti_mm = ((globalData) getActivity().getApplication()).getAquaLightChar("lightminutes");
            this.recurrence= ((globalData) getActivity().getApplication()).getAquaLightChar("lightrecurrences");
            this.dow = ((globalData) getActivity().getApplication()).getAquaLightChar("lightdow");
            this.dom = ((globalData) getActivity().getApplication()).getAquaLightChar("lightdom");
            this.duration = Integer.toString(((globalData) getActivity().getApplication()).getAquaLightChar("lightdurationhours")) + "h";
            this.duration += ":" + Integer.toString(((globalData) getActivity().getApplication()).getAquaLightChar("lightdurationminutes")) + "m";
            time_int_string();
            args.putString("time", time);
            args.putString("time_mode", time_mode);
            args.putInt("recurrence", recurrence);
            args.putInt("dow", dow);
            args.putInt("dom", dom);
            args.putString("duration", duration);
            mcaldroidFragment.setArguments(args);
            getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.cal_container, mcaldroidFragment).commit();
        }
        else
        {
            Log.w(TAG,"Schedule page not visible");
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
}
