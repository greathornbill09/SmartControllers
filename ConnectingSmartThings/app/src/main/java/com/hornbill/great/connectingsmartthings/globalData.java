package com.hornbill.great.connectingsmartthings;

import android.app.Application;

/**
 * Created by sangee on 1/29/2017.
 */

public class globalData extends Application {
    private byte aquaChars = 0;

    public byte getAquaChar() {
        return aquaChars;
    }
    public void setAquaChar(byte value) {
        this.aquaChars = value;
    }
}
