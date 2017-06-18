package com.hornbill.great.connectingsmartthings;

import android.content.Context;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.caldroid.*;
import com.roomorama.caldroid.CaldroidFragment;


public class schedulePage extends Fragment {
    public int ti_hh, ti_mm;
    public String duration, time, time_mode;
    public int recurrence;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_schedule_page,container,false);
        CaldroidFragment mcaldroidFragment =  new CaldroidSampleCustomFragment();
        Bundle args = new Bundle();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(java.util.Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(java.util.Calendar.YEAR));
        args.putBoolean(CaldroidFragment.ENABLE_SWIPE, true);
        args.putBoolean(CaldroidFragment.SIX_WEEKS_IN_CALENDAR, true);

        time_int_string();
        args.putString("time", time);
        args.putString("time_mode", time_mode);
        args.putInt("recurrence", recurrence);
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
