package com.example.hotspotshare;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    Context context;
    Activity activity;

    public  WifiHotspotConfig(Context context){
        this.context=context;
    }

    public  boolean isHotSpotEnabled() {
        Method method = null;
        int actualState = 0;
        try {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            method = mWifiManager.getClass().getDeclaredMethod("getWifiApState");
            method.setAccessible(true);

            actualState = (Integer) method.invoke(mWifiManager, (Object[]) null);
            if (actualState == AP_STATE_ENABLING || actualState == AP_STATE_ENABLED) {
                getHotspotSSID(context);
                return true;
            }
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param on Boolean status
     */
    public  boolean enableMobileData( boolean on) {
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


    /**
     * @param
     * @param name     Name of hotspot
     * @param password Password of hotspot
     * @return
     */
    public  boolean configureHotspot(String name, String password) {
        WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiConfiguration configuration = getCustomConfigs(manager, name, password);
        return configure(manager, configuration);
    }

    /**
     * @return
     */
    private  boolean configure(WifiManager manager, WifiConfiguration wifiConfig) {
        try {
            Method setConfigMethod = manager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            boolean status = (boolean) setConfigMethod.invoke(manager, wifiConfig);


            if (status) {
                Log.d("HOTSPOT", "热点已开启 SSID:" + wifiConfig.SSID + wifiConfig.preSharedKey);

            } else {
                Log.d("HOTSPOT", "创建热点失败");
            }
            return status;
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * @param ssid Requested ssid.
     * @param pass Request password.
     * @return
     */
    private  WifiConfiguration getCustomConfigs(WifiManager manager, String ssid, String pass) {
        WifiConfiguration wifiConfig = null;
        try {
            Method getConfigMethod = manager.getClass().getMethod("getWifiApConfiguration");
            wifiConfig = (WifiConfiguration) getConfigMethod.invoke(manager);
            SSID = wifiConfig.SSID;
            preShareKey = wifiConfig.preSharedKey;

            if (!TextUtils.isEmpty(ssid))
                wifiConfig.SSID = ssid;
            if (!TextUtils.isEmpty(pass))
                wifiConfig.preSharedKey = pass;
            return wifiConfig;
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (wifiConfig == null) {
            wifiConfig = new WifiConfiguration();
            if (!TextUtils.isEmpty(ssid))
                wifiConfig.SSID = ssid;
            if (!TextUtils.isEmpty(pass))
                wifiConfig.preSharedKey = pass;
            wifiConfig.hiddenSSID = true;
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfig.status = WifiConfiguration.Status.ENABLED;
        }
        return wifiConfig;
    }

    public  String getHotspotSSID(Context context) {
        WifiConfiguration wifiConfig = null;
        try {
            WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            Method getConfigMethod = manager.getClass().getDeclaredMethod("getWifiApConfiguration");

            getConfigMethod.setAccessible(true);

            wifiConfig = (WifiConfiguration) getConfigMethod.invoke(manager);
            return wifiConfig.SSID;
        }
        catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "abcd";
    }

    private  boolean isLocationPermissionEnable(Activity activity) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            return false;
        }
        return true;
    }
    private static WifiManager.LocalOnlyHotspotReservation mReservation;
    private static boolean isHotspotEnabled = false;

    /**
     * description:turnOnHotspot() just start the hotspot
     * and can not setup the SSID and Password
     * @param
     * @param
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public  void turnOnHotspot() {
        if (!isLocationPermissionEnable(activity)) {
            return;
        }
        WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);

        int wifiState = manager.getWifiState();
        if ((wifiState == WifiManager.WIFI_STATE_ENABLING)||(wifiState == WifiManager.WIFI_STATE_ENABLED)) {
            manager.setWifiEnabled(false);
        }

        if (manager != null && (wifiState == WifiManager.WIFI_STATE_DISABLED|| wifiState==WifiManager.WIFI_STATE_DISABLING)) {
            manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {

                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    super.onStarted(reservation);
                    mReservation = reservation;
                    isHotspotEnabled = true;
                    SSID = reservation.getWifiConfiguration().SSID;
                    preShareKey = reservation.getWifiConfiguration().preSharedKey;
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    Log.d("Hotspot", "onStopped: ");
                    isHotspotEnabled = false;
                }

                @Override
                public void onFailed(int reason) {
                    super.onFailed(reason);
                    Log.d("Hotspot", "onFailed: ");
                    isHotspotEnabled = false;
                }
            }, new Handler());
        }
    }


    /**
     * 获取SSID
     * @param activity 上下文
     * @return  WIFI 的SSID
     */
    public String getWIFISSID(Activity activity) {
        String ssid="unknown id";

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O||Build.VERSION.SDK_INT==Build.VERSION_CODES.P) {

            WifiManager mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            assert mWifiManager != null;
            WifiInfo info = mWifiManager.getConnectionInfo();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return info.getSSID();
            } else {
                return info.getSSID().replace("\"", "");
            }
        } else if (Build.VERSION.SDK_INT==Build.VERSION_CODES.O_MR1){

            ConnectivityManager connManager = (ConnectivityManager) activity.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert connManager != null;
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            if (networkInfo.isConnected()) {
                if (networkInfo.getExtraInfo()!=null){
                    return networkInfo.getExtraInfo().replace("\"","");
                }
            }
        }
        return ssid;
    }

}
