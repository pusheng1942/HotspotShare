package com.example.hotspotshare;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    TextView wifiDisplayView;
    TextView wifiStateView;
    Switch wifiSwitch;
    WifiHotspotConfig wifiHotspotConfig = new WifiHotspotConfig(this);
    Button updateButton;
    String wifiSSID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWifiHotspotDisplay();
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(wifiHotspotConfig.isHotSpotEnabled()){
                    wifiStateView.setText("HotspotState:Open");
                    wifiDisplayView.setText("SSID:"+wifiHotspotConfig.SSID+"\n\n"+"PWD:"+wifiHotspotConfig.preShareKey);
                }
                else {
                    wifiStateView.setText("HotspotState:Closed");
//                    wifiDisplayView.setText("Not Found");
                }
            }
        });

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    wifiStateView.setText("HotspotState:Open");
                    wifiHotspotConfig.turnOnHotspot();
                }
                else{
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
//        wifiHotspotConfig.turnOnHotspot(this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        wifiSSID = wifiHotspotConfig.getWIFISSID(this);
        wifiHotspotConfig.configureHotspot("ACD","432534654");
//        wifiHotspotConfig.turnOnHotspot(this);
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
}