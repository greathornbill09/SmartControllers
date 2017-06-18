package com.hornbill.great.connectingsmartthings;

/**
 * Created by sangee on 6/17/2017.
 */

import android.os.Bundle;

import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidGridAdapter;

public class CaldroidSampleCustomFragment extends CaldroidFragment {
    @Override
    public CaldroidGridAdapter getNewDatesGridAdapter(int month, int year) {
        Bundle args = getArguments();

        // TODO Auto-generated method stub
        return new CaldroidSampleCustomAdapter(getActivity(), month, year,
                getCaldroidData(), extraData, args.getInt("recurrence"), args.getString("time"), args.getString("time_mode"), args.getString("duration"));
    }
}
