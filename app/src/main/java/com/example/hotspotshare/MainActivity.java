package com.example.hotspotshare;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView wifiDisplayView;
    TextView wifiStateView;
    Switch wifiSwitch;
    Button updateButton;

    public static int AP_STATE_DISABLING = 10;
    public static int AP_STATE_DISABLED = 11;
    public static int AP_STATE_ENABLING = 12;
    public static int AP_STATE_ENABLED = 13;
    public static int AP_STATE_FAILED = 14;
    public static String SSID;
    public static String preShareKey;
    public boolean mobileEnableState = true;


    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private TextView ipListView;


    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWifiHotspotDisplay();
        ipListAndNumDisplay();
        wifiStateView.setText("HotspotState:Closed\n");
        wifiDisplayView.setText("SSID:"+"ull"+"\n"+"PWD:"+"null");

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isHotSpotEnabled()){
                    wifiStateView.setText("HotspotState:Open\n");
                    wifiDisplayView.setText("Ssid:"+SSID+"\n"+"Pwd:"+preShareKey);
                }
                else {
                    wifiStateView.setText("HotspotState:Closed\n");
                    wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");

                }
                ipListAndNumDisplay();
                wifiSwitch.setChecked(isHotSpotEnabled());
            }
        });

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    turnOnHotspot();
                    enableMobileData(mobileEnableState);
                    wifiStateView.setText("HotspotState:Open\n");
                    wifiDisplayView.setText("Ssid:"+SSID+"\n"+"Pwd:" +preShareKey);
                    ipListAndNumDisplay();
                }
                else{
                    turnOffHotspot();
                    wifiStateView.setText("HotspotState:Closed\n");
                    wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
                }
            }
        });
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void turnOnHotspot() {
        if (!isLocationPermissionEnable()) {
            return;
        }
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);

        int wifiState = manager.getWifiState();
        while ((wifiState == WifiManager.WIFI_STATE_ENABLING) || (wifiState == WifiManager.WIFI_STATE_ENABLED)) {
            manager.setWifiEnabled(false);
            wifiState = manager.getWifiState();
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
                if (splitted.length>=4){
                    String ip=splitted[0];
                    if (!ip.equalsIgnoreCase("ip")){
                        connectedIp.add(ip);
                        Log.i("ABC",ip);
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
            stopTethering.invoke(iConnMgr, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private void initWifiHotspotDisplay(){
        wifiDisplayView = findViewById(R.id.wifi_display);
        wifiStateView = findViewById(R.id.wifi_state);
        ipListView = findViewById(R.id.ip_list);
        updateButton = findViewById(R.id.wifi_update);
        wifiSwitch = findViewById(R.id.wifi_switch);

        playButton =  findViewById(R.id.button_play);
        pauseButton = findViewById(R.id.button_pause);
        stopButton =  findViewById(R.id.button_stop);
        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
        } else {
            initMediaPlayer();
        }
    }

    private void initMediaPlayer() {
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "huawei-8211-dream-it-possible.mp3");
            mediaPlayer.setDataSource(file.getPath()); // set the audio file path
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                if (!mediaPlayer.isPlaying() && isHotSpotEnabled()) {
                    mediaPlayer.start(); // just when the hotspot has been opened,the audio can be played
                }
                break;
            case R.id.button_pause:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause(); // 暂停播放
                }
                break;
            case R.id.button_stop:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.reset(); // 停止播放
                    initMediaPlayer();
                }
                break;
            default:
                break;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    void ipListAndNumDisplay(){
        ipListView.setText("Device Num:"+ getConnectedIP().size()+"\n");
        for(int i=0;i<getConnectedIP().size();i++){
            ipListView.append("IP:"+getConnectedIP().get(i)+"\n");
        }
    }


}