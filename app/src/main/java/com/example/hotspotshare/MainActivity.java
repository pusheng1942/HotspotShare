package com.example.hotspotshare;
import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.os.IBinder;
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
import android.widget.Toast;

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
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView wifiDisplayView;
    TextView wifiStateView;
    Switch wifiSwitch;

    Button playButton;
    Button pauseButton;
    Button stopButton;
    TextView ipListView;

    boolean isAllNecessaryPermissionsEnabled = false;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private HotspotService.HotspotBinder binder;
    private HotspotService hotspotService;
    boolean isServiceHaveBeenOpened = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAllNecessaryPermissions();
        initMediaPlayer();
        initWifiHotspotDisplay();

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked && isAllNecessaryPermissionsEnabled) {
                    isServiceHaveBeenOpened = true;
                    Intent bindIntent = new Intent(MainActivity.this, HotspotService.class);
                    bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
                }
                else if(isServiceHaveBeenOpened) {
                    wifiStateView.setText("HotspotState:Closed\n");
                    wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
                    unbindService(serviceConnection);
                    isServiceHaveBeenOpened = false;
                }
            }
        });
    }

    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (HotspotService.HotspotBinder) service;
            hotspotService = binder.getService();

            hotspotService.setOnDatCallback(new HotspotService.OnDataCallback() {
                @Override
                public void onDataChange(final HotspotService.HotspotStateInfo hotspotStateInfo) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (hotspotStateInfo.hotSpotEnabledState) {
                                wifiStateView.setText("HotspotState:Open\n");
                                wifiDisplayView.setText("Ssid:" + hotspotStateInfo.SSID + "\n" + "Pwd:" + hotspotStateInfo.preShareKey);
                            }
                            else {
                                wifiStateView.setText("HotspotState:Closed\n");
                                wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
                            }
                            ipListAndNumDisplay();
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            hotspotService = null;
        }
    };

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

    private void initWifiHotspotDisplay(){
        wifiDisplayView = findViewById(R.id.wifi_display);
        wifiStateView = findViewById(R.id.wifi_state);
        ipListView = findViewById(R.id.ip_list);
        wifiSwitch = findViewById(R.id.wifi_switch);

        playButton =  findViewById(R.id.button_play);
        pauseButton = findViewById(R.id.button_pause);
        stopButton =  findViewById(R.id.button_stop);
        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        wifiStateView.setText("HotspotState:Closed\n");
        wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
        ipListView.setText("Device Num:"+ getConnectedIP().size()+"\n");
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
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start(); // just when the hotspot has been opened,the audio can be played
                }
                break;
            case R.id.button_pause:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;
            case R.id.button_stop:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.reset();
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

    private void requestAllNecessaryPermissions() {
        List<String> permissionList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1002);
        } else {
            isAllNecessaryPermissionsEnabled = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1002:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                            Toast.makeText(MainActivity.this, permissions[i] + " Permission Refused!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    isAllNecessaryPermissionsEnabled = true;
                }
                break;
        }
    }

}