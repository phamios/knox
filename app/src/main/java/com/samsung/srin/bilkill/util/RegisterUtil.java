package com.samsung.srin.bilkill.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.samsung.srin.bilkill.HttpClient;
import com.samsung.srin.bilkill.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by SRIN on 8/13/2015.
 */
public class RegisterUtil {

    private static final String TAG = RegisterUtil.class.getCanonicalName();
    private static final String KEY_REGISTER_ID = "device_id";


    public static void registerToBackend(Context context, String deviceID, String deviceIMEI, String osversion) throws IOException {
        JSONObject jsonRequest = new JSONObject();

        String registerUrl = getRegistrationUrl(context);

        try {
            jsonRequest.put("DeviceId", getDeviceId(context));
            jsonRequest.put("IMEI", deviceIMEI);
            jsonRequest.put("DeviceName", "S8");
            jsonRequest.put("OsVersion", osversion);
            jsonRequest.put("Description", "Vinpro setup");
        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
        }

        Log.d(TAG, "Post request to Register: " + registerUrl);
        Log.d(TAG, "json : "+jsonRequest.toString());

        String response = HttpClient.getInstance().post(context,registerUrl, jsonRequest.toString());
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        String registrationID = "";

        try {
            JSONObject jsonRespose = new JSONObject(response);
            registrationID = refreshedToken; //jsonRespose.getString(KEY_REGISTER_ID);
            Log.i(TAG, "response : "+jsonRespose.toString());
        } catch (JSONException e){
            Log.e(TAG, "JSON Exception from response : " + response, e);
        }
        Log.d(">>>REGISTER","SENT");

        if(!registrationID.isEmpty()){
            saveRegistrationId(context, registrationID);
        }
    }

    public static boolean isDeviceRegistered(Context context){
        return !getRegistrationId(context).isEmpty();
    }

    public static String getRegistrationUrl(Context context){
        String serverUrl = context.getString(R.string.server_uri);
        return context.getString(R.string.register_uri, serverUrl);
    }

    public static String getRegistrationId(Context context){
        String prefName = context.getString(R.string.preference_file_key);
        SharedPreferences sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return sharedPref.getString(KEY_REGISTER_ID, "");
    }

    private static void saveRegistrationId(Context context, String registrationId){
        String prefName = context.getString(R.string.preference_file_key);
        SharedPreferences sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_REGISTER_ID, registrationId);
        editor.commit();
    }

    public static String getDeviceId(Context context){
        TelephonyManager telpMngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telpMngr.getDeviceId();
    }

    public static void setMessageCode(Context context, String FirebaseCode){
        String prefName = context.getString(R.string.preference_file_key);
        SharedPreferences sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constant.KEY_FIREBASE_CODE, FirebaseCode);
        editor.commit();
    }

    public static String getMessageCode(Context context){
        String prefName = context.getString(R.string.preference_file_key);
        SharedPreferences sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return sharedPref.getString(Constant.KEY_FIREBASE_CODE, "");
    }

}
