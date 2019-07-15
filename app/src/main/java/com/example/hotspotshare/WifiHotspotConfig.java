package com.example.hotspotshare;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static android.content.Context.WIFI_SERVICE;

public class WifiHotspotConfig {

    public static int AP_STATE_DISABLING = 10;
    public static int AP_STATE_DISABLED = 11;
    public static int AP_STATE_ENABLING = 12;
    public static int AP_STATE_ENABLED = 13;
    public static int AP_STATE_FAILED = 14;
    public static String SSID;
    public static String preShareKey;
    /**
     * @return status hot spot enabled or not
     */
    private Context context;
    private Activity activity;
//
    public WifiHotspotConfig(Context context) {
        this.context = context;
    }

    public boolean isHotSpotEnabled() {
        Method method = null;
        int actualState = 0;
        try {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
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

    private boolean isLocationPermissionEnable(Activity activity) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            return false;
        }
        return true;
    }

    private static WifiManager.LocalOnlyHotspotReservation mReservation;
    private static boolean isHotspotEnabledState = false;

    /**
     * description:turnOnHotspot() just start the hotspot
     * and can not setup the SSID and Password
     *
     * @param
     * @param
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void turnOnHotspot() {
        if (!isLocationPermissionEnable(activity)) {
            return;
        }
        WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);

        int wifiState = manager.getWifiState();
        if ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
            manager.setWifiEnabled(false);
        }

        if ((wifiState == WifiManager.WIFI_STATE_DISABLED)) {
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    mReservation = reservation;
                    SSID = reservation.getWifiConfiguration().SSID;
                    preShareKey = reservation.getWifiConfiguration().preSharedKey;
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
//        if (!isLocationPermissionEnable(context)) {
//            return;
//        }
        if (isHotspotEnabledState == true && mReservation != null) {
            mReservation.close();
            isHotspotEnabledState = false;
        }
    }

    public ArrayList<String> getConnectedIP(){
        ArrayList<String> connectedIp=new ArrayList<String>();
        try {
            BufferedReader br=new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line=br.readLine())!=null){
                String[] splitted=line.split(" +");
                if (splitted !=null && splitted.length>=4){
                    String ip=splitted[0];
                    if (!ip.equalsIgnoreCase("ip")){
                        connectedIp.add(ip);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connectedIp;
    }

    /**
     * open the mobile network
     * @param context
     * @param enabled  open or closed
     */
    public void setMobileDataState(Context context, boolean enabled) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled",boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param on Boolean status
     */
    public boolean enableMobileData(boolean on) {
        try {
            ConnectivityManager mConnectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method method = mConnectivityManager.getClass().getMethod("setMobileDataEnabled", boolean.class);
            method.invoke(mConnectivityManager, on);
            return true;
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void closeHotSpot(){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Field iConnMgrField;
        try {
            iConnMgrField = connManager.getClass().getDeclaredField("mService");
            iConnMgrField.setAccessible(true);
            Object iConnMgr = iConnMgrField.get(connManager);
            Class<?> iConnMgrClass = Class.forName(iConnMgr.getClass().getName());
            Method stopTethering = iConnMgrClass.getMethod("stopTethering", int.class);
            stopTethering.invoke(iConnMgr, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}


