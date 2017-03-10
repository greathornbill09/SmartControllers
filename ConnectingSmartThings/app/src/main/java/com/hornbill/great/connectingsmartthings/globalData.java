package com.hornbill.great.connectingsmartthings;

import android.app.Application;
import android.icu.text.DateFormat;
import android.util.Log;

/**
 * Created by sangee on 1/29/2017.
 */

public class globalData extends Application {

    private final static String TAG = globalData.class.getSimpleName();


    /* RTC Sync Details*/
    private boolean rtcSyncNotified = false;

    /* Light Char*/
    private byte lightMode = 0;
    private byte lightStatus = 0;
    private byte lightDow = 0;
    private byte lightHours = 0;
    private byte lightMinutes = 0;
    private byte lightRecurrences = 0;
    private byte lightDurationHours = 0;
    private byte lightDurationMinutes = 0;

    /* Motor Char*/
    private byte motorMode = 0;
    private byte motorPump = 0;
    private byte motorValve= 0;
    private byte motorDow = 0;
    private byte motorHours = 0;
    private byte motorMinutes = 0;
    private byte motorRecurrence = 0;
    private byte motorDurationHours = 0;
    private byte motorDurationMinutes = 0;
    private byte motorCalibrateState = 0;

    public boolean getRtcSyncStatus(){
        return rtcSyncNotified;
    }

    public void setRtcSyncDone(Boolean val){

        rtcSyncNotified = val;
    }

    public byte getAquaLightChar(String returnString) {

        byte returnVal=0;
        switch (returnString){
            case "lightmode":
                returnVal = this.lightMode;
                break;
            case "lightstatus":
                returnVal = this.lightStatus;
                break;
            case "lightdow":
                returnVal = this.lightDow;
                break;
            case "lighthours":
                returnVal = this.lightHours;
                break;
            case "lightminutes":
                returnVal = this.lightMinutes;
                break;
            case "lightrecurrences":
                returnVal = this.lightRecurrences;
                break;
            case "lightdurationhours":
                returnVal = this.lightDurationHours;
                break;
            case "lightdurationminutes":
                returnVal = this.lightDurationMinutes ;
                break;
            default:
                Log.w(TAG, "getAquaLightChar: No Matching case ");
                break;
        }
        return returnVal;
    }
    public void setAquaLightChar(String caseVal,byte value) {

        switch (caseVal){
            case "lightmode":
                this.lightMode = value;
                break;
            case "lightstatus":
                this.lightStatus = value;
                break;
            case "lightdow":
                this.lightDow = value;
                break;
            case "lighthours":
                this.lightHours = value;
                break;
            case "lightminutes":
                this.lightMinutes = value;
                break;
            case "lightrecurrences":
                this.lightRecurrences = value;
                break;
            case "lightdurationhours":
                this.lightDurationHours = value;
                break;
            case "lightdurationminutes":
                this.lightDurationMinutes = value;
                break;
            default:
                Log.w(TAG, "setAquaLightChar: No Matching case ");
                break;
        }

    }


    public byte getAquaMotorChar(String returnString) {

        byte returnVal=0;
        switch (returnString){
            case "motormode":
                returnVal = this.motorMode;
                break;
            case "motorpump":
                returnVal = this.motorPump;
                break;
            case "motorvalve":
                returnVal = this.motorValve;
                break;
            case "motordow":
                returnVal = this.motorDow;
                break;
            case "motorhours":
                returnVal = this.motorHours;
                break;
            case "motorminutes":
                returnVal = this.motorMinutes;
                break;
            case "motorrecurrence":
                returnVal = this.motorRecurrence;
                break;
            case "motordurationhours":
                returnVal = this.motorDurationHours;
                break;
            case "motordurationminutes":
                returnVal = this.motorDurationMinutes ;
                break;
            case "motorcalibratestate":
                returnVal = this.motorCalibrateState;
                break;
            default:
                Log.w(TAG, "getAquaMotorChar: No Matching case ");
                break;
        }
        return returnVal;
    }
    public void setAquaMotorChar(String caseVal,byte value) {

        switch (caseVal){
            case "motormode":
                this.motorMode = value;
                break;
            case "motorpump":
                this.motorPump = value;
                break;
            case "motorvalve":
                this.motorValve = value;
                break;
            case "motordow":
                this.motorDow = value;
                break;
            case "motorhours":
                this.motorHours = value;
                break;
            case "motorminutes":
                this.motorMinutes = value;
                break;
            case "motorrecurrence":
                this.motorRecurrence = value;
                break;
            case "motordurationhours":
                this.motorDurationHours = value;
                break;
            case "motordurationminutes":
                this.motorDurationMinutes = value;
                break;
            case "motorcalibratestate":
                this.motorCalibrateState = value;
                break;
            default:
                Log.w(TAG, "setAquaMotorChar: No Matching case ");
                break;
        }

    }

}
