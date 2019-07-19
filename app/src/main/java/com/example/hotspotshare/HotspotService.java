package com.example.hotspotshare;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


public class HotspotService extends Service {
    private String message;
    private IBinder binder = new HotspotBinder();
    private HotspotService.ServiceThread hotspotService;
    private Thread thread;
    private HotspotStateInfo hotspotStateInfo = new HotspotStateInfo();

    private static int AP_STATE_ENABLING = 12;
    private static int AP_STATE_ENABLED = 13;

    public static String hotspotSSID;
    public static String hotspotPreShareKey;

    public static final String TAG = "service";

    @Override
    public IBinder onBind(Intent intent) {
        hotspotService = new ServiceThread();
        thread =  new Thread(hotspotService);
        //open the hotspot
        turnOnHotspot();
        thread.start();
        return binder;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        hotspotService.flag = false;
        turnOffHotspot();  //close the hotspot
        Log.i(TAG,"onDestroy");
    }


    class HotspotStateInfo{
        String  SSID;
        String  preShareKey;
        boolean hotSpotEnabledState;
    }

    class ServiceThread implements Runnable{
        volatile boolean flag = true;

        @Override
        public void run(){
            Log.i(TAG,"thread is running！");
            int i =1;
            while(flag){
                if(mOnDataCallback!=null){
                    hotspotStateInfo.SSID =  hotspotSSID;
                    hotspotStateInfo.preShareKey = hotspotPreShareKey;
                    hotspotStateInfo.hotSpotEnabledState = isHotSpotEnabled();

                    mOnDataCallback.onDataChange(hotspotStateInfo);  //Transport the hotspot state info
                }
                i++;
                try{
                    Thread.sleep(100);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public class HotspotBinder extends Binder {
        public void setData(HotspotStateInfo hotspotStateInfo){
            HotspotService.this.hotspotStateInfo = hotspotStateInfo;
        }

        public HotspotService getService(){
            return HotspotService.this;
        }
    }

    private OnDataCallback mOnDataCallback= null;
    public void setOnDatCallback(OnDataCallback mOnDataCallback){
        this.mOnDataCallback = mOnDataCallback;
    }

    public interface OnDataCallback{
        void onDataChange(HotspotStateInfo hotspotStateInfo);
    }

    public boolean isHotSpotEnabled() {
        Method method = null;
        int actualState = 0;
        try {
            WifiManager mWifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
            method = mWifiManager.getClass().getDeclaredMethod("getWifiApState");
            method.setAccessible(true);

            actualState = (Integer) method.invoke(mWifiManager, (Object[]) null);
            if (actualState == AP_STATE_ENABLING || actualState == AP_STATE_ENABLED) {
                return true;
            }
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private static WifiManager.LocalOnlyHotspotReservation mReservation;
    private static boolean isHotspotEnabledState = false;

    public void turnOnHotspot() {

        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);

        int wifiState = manager.getWifiState();
        if ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
            manager.setWifiEnabled(false);
            wifiState = manager.getWifiState();
        }

        if ((wifiState == WifiManager.WIFI_STATE_DISABLED)) {
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    mReservation = reservation;
                    hotspotSSID = reservation.getWifiConfiguration().SSID;
                    hotspotPreShareKey = reservation.getWifiConfiguration().preSharedKey;
                    isHotspotEnabledState = true;
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    isHotspotEnabledState = false;
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    isHotspotEnabledState = false;
                }
            }, new Handler());
        }
    }

    public void turnOffHotspot() {
        if (isHotspotEnabledState && mReservation != null) {
            mReservation.close();
            isHotspotEnabledState = false;
        }
    }
}
