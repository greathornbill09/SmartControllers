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
}
