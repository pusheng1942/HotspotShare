package com.example.hotspotshare;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView wifiDisplayView;
    TextView wifiStateView;
    Switch wifiSwitch;
    WifiHotspotConfig wifiHotspotConfig = new WifiHotspotConfig(this);
    Button updateButton;

    public static int AP_STATE_DISABLING = 10;
    public static int AP_STATE_DISABLED = 11;
    public static int AP_STATE_ENABLING = 12;
    public static int AP_STATE_ENABLED = 13;
    public static int AP_STATE_FAILED = 14;
    public static String SSID;
    public static String preShareKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWifiHotspotDisplay();
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isHotSpotEnabled()){
                    wifiStateView.setText("HotspotState:Open");
                    wifiDisplayView.setText("SSID:"+SSID+"\n\n"+"PWD:"+preShareKey);
                }
                else {
                    wifiStateView.setText("HotspotState:Closed");
                    wifiDisplayView.setText("Not Found");
                }
            }
        });

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
//                    wifiHotspotConfig.closeHotSpot();
                    turnOnHotspot();
                    wifiStateView.setText("HotspotState:Open");
                }
                else{
                    turnOffHotspot();
//                    wifiHotspotConfig.closeHotSpot();
                    wifiStateView.setText("HotspotState:Close");
                }
            }
        });
    }


    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
    }

    @Override
    protected void onResume(){
        super.onResume();
//        wifiHotspotConfig.turnOnHotspot();
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    private void initWifiHotspotDisplay(){
        wifiDisplayView = findViewById(R.id.wifi_display);
        wifiStateView = findViewById(R.id.wifi_state);
        updateButton = findViewById(R.id.wifi_update);
        wifiSwitch = findViewById(R.id.wifi_switch);
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

    private boolean isLocationPermissionEnable() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
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
        if (!isLocationPermissionEnable()) {
            return;
        }
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);

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
     * @param enabled  open or closed
     */
    public void setMobileDataState(boolean enabled) {
        TelephonyManager telephonyService = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
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
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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