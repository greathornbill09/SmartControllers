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
        CaldroidFragment mcaldroidFragment =  new CaldroidFragment();
        Bundle args = new Bundle();
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK,CaldroidFragment.MONDAY);
        mcaldroidFragment.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.cal_container, mcaldroidFragment).commit();

        return  root;
    }
}
