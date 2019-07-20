package com.example.hotspotshare;


import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class HotspotService extends Service {
    private IBinder binder = new HotspotBinder();
    private HotspotService.ServiceThread hotspotService;
    Thread thread;
    private HotspotStateInfo hotspotStateInfo = new HotspotStateInfo();

    private static final int AP_STATE_ENABLING = 12;
    private static final int AP_STATE_ENABLED = 13;

    public static String hotspotSSID;
    public static String hotspotPreShareKey;

    public static final String TAG = "service";

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "onCreate() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        hotspotService = new ServiceThread();
        thread =  new Thread(hotspotService);
        turnOnHotspot();
        thread.start();
        return binder;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        hotspotService.flag = false;
        turnOffHotspot();
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
            while(flag){
                if(mOnDataCallback!=null){
                    hotspotStateInfo.SSID =  hotspotSSID;
                    hotspotStateInfo.preShareKey = hotspotPreShareKey;
                    hotspotStateInfo.hotSpotEnabledState = isHotSpotEnabled();

                    mOnDataCallback.onDataChange(hotspotStateInfo);  //Transport the hotspot state info
                }
                try{
                    Thread.sleep(50);
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
        }
        System.out.println(wifiState);
        if ((wifiState == WifiManager.WIFI_STATE_DISABLED || wifiState == WifiManager.WIFI_STATE_DISABLING )) {
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    mReservation = reservation;
                    hotspotSSID = reservation.getWifiConfiguration().SSID;
                    hotspotPreShareKey = reservation.getWifiConfiguration().preSharedKey;
                    isHotspotEnabledState = true;
//                    Log.i("HotspotService","wifi closed");
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


