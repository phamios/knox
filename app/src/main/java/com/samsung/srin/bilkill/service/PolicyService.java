package com.samsung.srin.bilkill.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.samsung.srin.bilkill.HttpClient;
import com.samsung.srin.bilkill.R;
import com.samsung.srin.bilkill.controller.PolicyController;
import com.samsung.srin.bilkill.util.ApiServer;
import com.samsung.srin.bilkill.util.CommonUtil;
import com.samsung.srin.bilkill.util.RegisterUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by SRIN on 8/10/2015.
 */
public class PolicyService extends IntentService {

    private static final String TAG = PolicyService.class.getCanonicalName();
    
    private static final String KEY_DEVICE_ID        = "device_id";
    private static final String KEY_DEVICE_IMEI      = "IMEI";
    private static final String KEY_DEVICE_TIMESTAMP = "device_last_update";
    private static final String KEY_DEVICE_LOCK      = "device_lock";
    private static final String KEY_DEVICE_LOCK_MSG  = "device_lock_msg";
    private static final String KEY_DEVICE_LOCK_PIN  = "device_lock_pin";
    private static final String KEY_DEVICE_TRACK     = "device_track";
    private static final String KEY_DEVICE_NOTIF     = "device_notification";
    private static final String KEY_DEVICE_ADMIN     = "disable_device_admin";



    public PolicyService() {
        super("PolicyService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Time = " + System.currentTimeMillis());

        //-============================================================k=====
        // GET DEVICE ID
        String deviceId = Settings.Secure.getString(getContentResolver(),  Settings.Secure.ANDROID_ID);
//        deviceId = FirebaseInstanceId.getInstance().getToken();


        // GET IMEI NUMBER
        TelephonyManager tManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        String deviceIMEI = tManager.getDeviceId();


        String model = Build.MODEL;
        String osversion = Build.MANUFACTURER
                + " " + Build.MODEL + " " + Build.VERSION.RELEASE
                + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();

        //-=================================================================

        if(!RegisterUtil.isDeviceRegistered(this)){
            try {
                RegisterUtil.registerToBackend(this,deviceId,deviceIMEI,osversion);
            } catch (IOException e) {
                Log.d(TAG, "IOException when try to register", e);
            }
        } else {
            try {
                String response = sendFireBaseToken();//sendSyncRequest();
                //Sync with server to get current status of this devices
                getStatusServer();
                if(response.isEmpty()) return;

                Log.d(TAG, "sync response : "+ response);
                boolean result = applySyncResponse(response);
                if(result){
                    String timestamp = getTimeStamp(response);

                    if (timestamp != null) {
                        sendSyncResult(timestamp);
                    }else{
                        Log.e(TAG, "error empty timestamp response");
                    }
                }
            } catch (IOException e){
                Log.e(TAG, "IO Exception from response : ", e);
            }
        }
    }

    private String sendSyncRequest() throws IOException {
        JSONObject jsonRequest = new JSONObject();
        
        try {
            jsonRequest.put(KEY_DEVICE_ID, RegisterUtil.getRegistrationId(this));
        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
        }

        String url = getSyncUrl(this);

        Log.d(TAG, "Post request to : " + url);
        Log.d(TAG, "json : " + jsonRequest.toString());

        return HttpClient.getInstance().post(getApplicationContext(),url, jsonRequest.toString());
    }

    private String sendFireBaseToken() throws IOException {
        JSONObject jsonRequest = new JSONObject();

        try {
            jsonRequest.put("DeviceId", RegisterUtil.getRegistrationId(this));
            jsonRequest.put("IMEI",RegisterUtil.getDeviceId(this));
        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
        }

        String url = getSyncUrl(this);

        Log.d(TAG, "Post request to : " + url);
        Log.d(TAG, "json : " + jsonRequest.toString());

        return HttpClient.getInstance().post(getApplicationContext(),url, jsonRequest.toString());
    }

    private void getStatusServer() throws  IOException{
        JSONObject jsonRequest = new JSONObject();

        try {
            TelephonyManager tManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
            String imeiID = tManager.getDeviceId();
            jsonRequest.put(KEY_DEVICE_IMEI,imeiID);

            String url_info = getSyncResultUrl(this);

            Log.d(TAG, "Post request to : " + url_info);
            Log.d(TAG, "json :  " + jsonRequest.toString());

            String respond = HttpClient.getInstance().post(getApplicationContext(),url_info, jsonRequest.toString());

            JSONObject reader = new JSONObject(respond);
            JSONObject dataRespond  = reader.getJSONObject("Data");
            JSONObject jsonObj = new JSONObject(dataRespond.toString());
            if(Integer.parseInt(jsonObj.getString("LockDevice")) == 1 || Integer.parseInt(jsonObj.getString("LockWarranty")) == 1){
//                PolicyController.getInstance(this).disableFactoryReset(true);
//                PolicyController.getInstance(this).disableApplicationClearData(true, getPackageName());
//                PolicyController.getInstance(this).disableUninstallApplication(true, getPackageName());
                PolicyController.getInstance(this).lockoutDevice("123456","Ahihi Hacked !!!","0916262170");
            }
            if(Integer.parseInt(jsonObj.getString("LockDevice")) == 0 || Integer.parseInt(jsonObj.getString("LockWarranty")) == 0){
                PolicyController.getInstance(this).unlockDevice("123456");
                PolicyController.getInstance(this).removeLockout();
            }



            Log.d("LockDevice",jsonObj.getString("LockDevice"));
            Log.d("DisabledKnox",jsonObj.getString("DisabledKnox"));
            Log.d("LockWarranty",jsonObj.getString("LockWarranty"));

        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
            Toast.makeText(this, "Lỗi kết nối, kiểm tra lại mạng 3G/4G hoặc wifi", Toast.LENGTH_SHORT).show();
        }


    }

    private void sendSyncResult(String timestamp) throws IOException {
        JSONObject jsonRequest = new JSONObject();

        try {
            jsonRequest.put(KEY_DEVICE_ID, RegisterUtil.getRegistrationId(this));
            jsonRequest.put(KEY_DEVICE_TIMESTAMP, timestamp);
        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
        }

        String url = getSyncResultUrl(this);

        Log.d(TAG, "Post request to : " + url);
        Log.d(TAG, "json : " + jsonRequest.toString());

        HttpClient.getInstance().post(getApplicationContext(),url, jsonRequest.toString());
    }

    public  void sendConfirmDevice(int locktype,int DisabledKnox,int LockWarranty) throws IOException{
        JSONObject jsonRequest = new JSONObject();
        ArrayList<ApiServer> arrayStudent = new ArrayList<ApiServer>();

        ApiServer student1= new ApiServer("Device" , 1);
        ApiServer student2= new ApiServer("LockWarranty" , 1);
        ApiServer student3= new ApiServer("DisabledKnox" , 1);

        arrayStudent.add(student1);
        arrayStudent.add(student2);
        arrayStudent.add(student3);

        try {
            jsonRequest.put("IMEI", RegisterUtil.getDeviceId(this));
            jsonRequest.put("Body", arrayStudent);
        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
        }

        String url = PolicyService.getConfirmUrl(this);
        Log.d(TAG, "Post request to : " + url);
        Log.d(TAG, "json : " + jsonRequest.toString());
        HttpClient.getInstance().post(this,url, jsonRequest.toString());
    }






    private static String getSyncUrl(Context context) {
        String serverUrl = context.getString(R.string.server_uri);
        return context.getString(R.string.sync_request_uri, serverUrl);
    }

    public static String getConfirmUrl(Context context){
        String serverURL = context.getString(R.string.server_uri);
        return context.getString(R.string.confirm_status_devices,serverURL);
    }

    private static String getSyncResultUrl(Context context) {
        String serverUrl = context.getString(R.string.server_uri);
        return context.getString(R.string.sync_result_uri, serverUrl);
    }

    private String getTimeStamp(String response) {
        try {
            JSONObject syncResponse = new JSONObject(response);
            if (syncResponse.has(KEY_DEVICE_TIMESTAMP)) {
                return syncResponse.getString(KEY_DEVICE_TIMESTAMP);
            }
        } catch (JSONException e){
            Log.e(TAG, "JSON Exception from response : " + response, e);
        }

        return null;
    }

    private boolean applySyncResponse(String response) {
        boolean uninstallResult  = false;
        boolean lockResult  = false;
        boolean trackResult = false;

        try {
            JSONObject syncResponse = new JSONObject(response);
            uninstallResult = applyDeviceUninstall(syncResponse);
            if (!uninstallResult) {
                lockResult  = applyDeviceLock(syncResponse);
                trackResult = applyDeviceTrack(syncResponse);
            }
            applyNotification(syncResponse);

        } catch (JSONException e){
            Log.e(TAG, "JSON Exception from response : " + response, e);
        }

        return (lockResult && trackResult) || uninstallResult;
    }



    private boolean applyDeviceLock(JSONObject syncResponse) throws JSONException {

        String deviceLock = null;
        String deviceLockMessage = null;
        String deviceLockPin =null;

        if (syncResponse.has(KEY_DEVICE_LOCK)) {
             deviceLock = syncResponse.getString(KEY_DEVICE_LOCK);
        }

        if (syncResponse.has(KEY_DEVICE_LOCK_MSG)) {
             deviceLockMessage = syncResponse.getString(KEY_DEVICE_LOCK_MSG);
        }
        if (syncResponse.has(KEY_DEVICE_LOCK_PIN)) {
             deviceLockPin = syncResponse.getString(KEY_DEVICE_LOCK_PIN);
        }

        if (deviceLock != null) {
            if ("locked".equals(deviceLock)) {
                return PolicyController.getInstance(this).lockoutDevice(deviceLockPin, deviceLockMessage, "");
            } else if ("unlocked".equals(deviceLock)) {
                return PolicyController.getInstance(this).removeLockout();
            }
        }
        return false;
    }

    private boolean applyDeviceTrack(JSONObject syncResponse) throws JSONException {

        String deviceTrack = null;
        if (syncResponse.has(KEY_DEVICE_TRACK)) {
             deviceTrack = syncResponse.getString(KEY_DEVICE_TRACK);
        }

        if (deviceTrack != null) {
            if ("enabled".equals(deviceTrack)) {
                CommonUtil.runPeriodicService(this, TrackingService.class, TrackingService.TIME_INTERVAL);
                return PolicyController.getInstance(this).activateGPS();
            } else if ("disabled".equals(deviceTrack)) {
                CommonUtil.cancelPeriodicService(this, TrackingService.class);
                PolicyController.getInstance(this).deactivateGPS();
                return true;
            }
        }
        return false;
    }

    private void applyNotification(JSONObject syncResponse) throws JSONException {

        String notifMessage = null;
        if (syncResponse.has(KEY_DEVICE_NOTIF)) {
            notifMessage = syncResponse.getString(KEY_DEVICE_NOTIF);
        }

        if (notifMessage != null) {
            CommonUtil.sendNotification(this, notifMessage);
        }
    }

    private boolean applyDeviceUninstall(JSONObject syncResponse) throws JSONException {

        String deviceUninstall = null;

        if (syncResponse.has(KEY_DEVICE_ADMIN)) {
            deviceUninstall = syncResponse.getString(KEY_DEVICE_ADMIN);
        }

        if (deviceUninstall != null) {
            if ("true".equals(deviceUninstall)) {
                PolicyController.getInstance(this).uninstallApplication(PolicyController.pkgName);
                return true;
            }
        }
        return false;
    }
}
