package com.samsung.srin.bilkill.controller;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.ApplicationPolicy;
import android.app.enterprise.EnterpriseDeviceManager;
import android.app.enterprise.LocationPolicy;
import android.app.enterprise.RestrictionPolicy;
import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.samsung.srin.bilkill.HttpClient;
import com.samsung.srin.bilkill.R;
import com.samsung.srin.bilkill.service.TrackingService;
import com.samsung.srin.bilkill.util.CommonUtil;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by SRIN on 8/3/2015.
 */
public class PolicyController {

    private static PolicyController mInstance;
    private Context mContext;
    private Toast mToast;

    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDPM;
    private EnterpriseLicenseManager mELM;
    private EnterpriseDeviceManager mEDM;
    private ApplicationPolicy appPolicy;

    public static LicenseActivationListener activationListener;

    private static String TAG = "KNOX ADMIN";
    public static final int RESULT_ENABLE = 1;
    public static String MDM_LITE_PREF = "MDM_LITE_PREF";
    public static String ELMKEY = "ELMKEY";

    public static String pkgName = "com.samsung.srin.bilkill";

    public PolicyController(Context context) {
        mContext = context;
        mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        mDeviceAdmin = new ComponentName(mContext, KnoxAdmin.class);
        mDPM = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mELM = EnterpriseLicenseManager.getInstance(mContext);
        mEDM = (EnterpriseDeviceManager) mContext.getSystemService(EnterpriseDeviceManager.ENTERPRISE_POLICY_SERVICE);
        EnterpriseDeviceManager.EnterpriseSdkVersion sdkVer = mEDM.getEnterpriseSdkVer();
        Log.w("VERSION KNOX",sdkVer.toString());
        appPolicy = mEDM.getApplicationPolicy();
    }

    public static PolicyController getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new PolicyController(context);
        }

        return mInstance;
    }

    public void activateAdmin(Activity activity, LicenseActivationListener listener) {
        // Enable admin
        if (null == mDPM) {
            Log.e(TAG, "Failed to get DPM!!!!");
            return;
        }

        activationListener = listener;

        boolean active = mDPM.isAdminActive(mDeviceAdmin);
        if (!active) {
            try {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Activate device administrator.");
                activity.startActivityForResult(intent, RESULT_ENABLE);
            } catch (Exception e) {
                Log.w(TAG, "Exception: " + e);
                mToast.setText("Error: Exception occurred - " + e);
                mToast.show();
            }
        } else {
            Log.w(TAG, "Admin already activated");
            mToast.setText("Admin already activated");
            mToast.show();

            ActivateLicenseAsyncTask licenseTask = new ActivateLicenseAsyncTask(mContext);
            licenseTask.setLicenseActivationListener(activationListener);
            licenseTask.execute();
        }
    }

    public static class ActivateLicenseAsyncTask extends AsyncTask<Void, Void, String> {

        private Context mContext;
        private EnterpriseLicenseManager mELM;
        private DevicePolicyManager mDPM;
        private ComponentName mDeviceAdmin;

        public ActivateLicenseAsyncTask (Context context) {
            this.mContext = context;
            mELM = EnterpriseLicenseManager.getInstance(context);
            mDPM = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
            mDeviceAdmin = new ComponentName(mContext, KnoxAdmin.class);
        }

        private LicenseActivationListener activationListener;

        public void setLicenseActivationListener(LicenseActivationListener activationListener) {
            this.activationListener = activationListener;
        }

        @Override
        protected void onPreExecute() {
            activationListener.onActivationStart();
        }

        @Override
        protected String doInBackground(Void... params) {

            if (mDPM.isAdminActive(mDeviceAdmin) && !isELMLicenseActive(mContext)) {
                String elmLicense = null;
                try {
                    elmLicense = getElmKey(mContext);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return elmLicense;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String elmLicense) {
            if (elmLicense != null) {
                mELM.activateLicense(mContext.getString(R.string.registerkey), pkgName);
            } else {
                activationListener.onActivationFailure();
            }
        }
    }

    /**
     * ============ KnoxAdmin DeviceAdminReceiver ===========
     */
    public static class KnoxAdmin extends DeviceAdminReceiver {

        void showToast(Context context, CharSequence msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnabled(Context context, Intent intent) {
            showToast(context, context.getString(R.string.app_name) + ": enabled");
            ActivateLicenseAsyncTask licenseTask = new ActivateLicenseAsyncTask(context);
            licenseTask.setLicenseActivationListener(activationListener);
            licenseTask.execute();
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            return "Disable " + context.getString(R.string.app_name) + " from Device administrator";
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            showToast(context, context.getString(R.string.app_name) + ": disabled");
        }
    }

    /**
     * ============ LicenseReceiver BroadcastReceiver ===========
     */
    public static class LicenseReceiver extends BroadcastReceiver {

        private static final String SUCCESS = "success";
        private static final String FAILURE = "fail";

        private boolean mELMActive = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(intent.getAction())) {
                final String status = intent.getExtras().getString(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
                final String errorCode = String.valueOf(intent.getExtras().getInt(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, Integer.MIN_VALUE));

                Log.d(TAG, status);
                Log.d(TAG, errorCode);

                if (SUCCESS.equals(status)) {
                    Log.d(TAG, context.getResources().getString(R.string.elm_activated_succesfully));

                    mELMActive = true;
                    SharedPreferences sharedPref = context.getSharedPreferences(MDM_LITE_PREF, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean(ELMKEY, mELMActive);
                    editor.commit();

                    if(!CommonUtil.isActivationSuccess(context)) {
                        Log.w("WANRNING",context.toString());
                        PolicyController.getInstance(context).activationListener.onActivationSuccess();
                    }
                } else if (FAILURE.equals(status)) {
                    final String errorMsg = "ELM failure: " + errorCode;
                    Log.w(TAG, errorMsg);
                    mELMActive = false;

                    PolicyController.getInstance(context).activationListener.onActivationFailure();

                } else {
                    Log.e(TAG, "unknown ELM state ignored: " + status + ", " + errorCode);

                    PolicyController.getInstance(context).activationListener.onActivationFailure();
                }
            } else {
                Log.e(TAG, "unknown action intent ignored");
            }
        }
    }


    public boolean isAdminActive(){
        return mDPM.isAdminActive(mDeviceAdmin);
    }

    public static boolean isELMLicenseActive(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(MDM_LITE_PREF, Context.MODE_PRIVATE);
        boolean elmKey = sharedPref.getBoolean(ELMKEY, false);
        return elmKey;
    }

    private static String getElmKey(Context mContext) throws IOException {
//        JSONObject jsonRequest = new JSONObject();
//
//        String serverUri  = mContext.getString(R.string.server_uri);
//        String requestUrl = mContext.getString(R.string.elm_key_uri, serverUri);
//        String response = HttpClient.getInstance().post(requestUrl, jsonRequest.toString());
//
//        String elmkey = null;
//        try {
//            JSONObject jsonRespose = new JSONObject(response);
//            elmkey =  jsonRespose.getString("Data");
//            Log.i(TAG, "response : "+jsonRespose.toString());
//        } catch (JSONException e){
//            Log.e(TAG, "JSON Exception from response : " + response, e);
//        }
//
//        return elmkey;

//        String serverUri  = mContext.getString(R.string.server_uri);
//        String requestUrl = mContext.getString(R.string.elm_key_uri, serverUri);
//
//        Log.d(TAG, "Post request to : " + requestUrl);
//
//        try {
//            OkHttpClient mOk = new OkHttpClient();
//            Request request = new Request.Builder().url(requestUrl).build();
//            Response response = mOk.newCall(request).execute();
//
//            if (response.code() == 200) {
//                return response.body().string();
//            } else {
//                throw new RuntimeException("server response error : "+response.code());
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "get elm key error: ", e);
//            return null;
//        }
        return mContext.getString(R.string.registerkey);
    }

    public boolean lockoutDevice(String password, String message, String phone) {
        Log.w(TAG, "lock device");
        try {
            // check password is a number (pin)
            if (Integer.parseInt(password) >= 0) {
                mEDM.getSecurityPolicy().lockoutDevice(password, message, Arrays.asList(phone));
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
        }

        return false;
    }

    public boolean unlockDevice(String password){
        Log.w(TAG, "unlock device");
        try {
            // check password is a number (pin)
            if (Integer.parseInt(password) >= 0) {
                mEDM.getSecurityPolicy().unlockCredentialStorage(password);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
        }

        return false;

    }

    public boolean removeLockout() {
        Log.w(TAG, "remove lock device");
        try {
            mEDM.getSecurityPolicy().removeDeviceLockout();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
        }

        return false;
    }

    public boolean enableDeviceAdmin(boolean allow) {
        Log.w(TAG,"enableDeviceAdmin: " + mEDM.getAdminRemovable());
        try {
            if (mEDM.setAdminRemovable(allow)) {
                Log.w(TAG,"Device Admin " + mEDM.getAdminRemovable());
                return true;
            } else {
                Log.w(TAG, "Failed to enable/disable Device Admin.");
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exception: " + e);
        }

        return false;
    }

    public void disableFactoryReset(boolean allow) {
        RestrictionPolicy restrictionPolicy = mEDM.getRestrictionPolicy();
        try {
            if (restrictionPolicy.allowFactoryReset(!allow)) {
                Log.w(TAG,
                        "Factory Reset "
                                + restrictionPolicy.isFactoryResetAllowed());
            } else {
                Log.w(TAG, "Failed to enable/disable Factory Reset.");
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exception: " + e);
        }
    }

    public void disableUninstallApplication(boolean disable, String packageName) {
        try {
            if (disable) {
                appPolicy.setApplicationUninstallationDisabled(packageName);
                Log.w(TAG, "Disable Uninstall " + packageName);
            } else {
                appPolicy.setApplicationUninstallationEnabled(packageName);
                Log.w(TAG, "Disable Uninstall " + packageName);
            }
        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
        }
    }

    public void disableApplicationClearData(boolean disable, String packageName) {
        List<String> packageList = Arrays.asList(packageName);

        try {
            if (disable) {
                if (appPolicy.addPackagesToClearDataBlackList(packageList)) {
                    Log.w(TAG, "Disable " + packageName + " Clear Data");
                } else {
                    Log.w(TAG, "Failed to Disable " + packageName + " Clear Data");
                }
            } else {
                if (appPolicy.removePackagesFromClearDataBlackList(packageList)) {
                    Log.w(TAG, "Disable " + packageName + " Clear Data");
                } else {
                    Log.w(TAG, "Failed to Disable " + packageName + " Clear Data");
                }
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Exception: " + e);
        }
    }

    public boolean activateGPS () {
        try {
            LocationPolicy locationPolicy = mEDM.getLocationPolicy();
            if(!locationPolicy.isGPSOn()){
                return  locationPolicy.startGPS(true) &&
                        locationPolicy.setGPSStateChangeAllowed(false);
            } else {
                return locationPolicy.setGPSStateChangeAllowed(false);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: " + e);
        }
        return false;
    }

    public boolean deactivateGPS() {
        try {
            LocationPolicy locationPolicy = mEDM.getLocationPolicy();

            if(locationPolicy.isGPSOn()){
                return  locationPolicy.startGPS(false) &&
                        locationPolicy.setGPSStateChangeAllowed(true);
            } else {
                return locationPolicy.setGPSStateChangeAllowed(true);
            }

        } catch (SecurityException e) {
            Log.w(TAG, "SecurityException: " + e);
        }
        return false;
    }

    public void uninstallApplication(String packageName) {
        try {
            if (isAdminActive() && enableDeviceAdmin(true)) {
                removeLockout();
                deactivateGPS();

                disableFactoryReset(false);
                disableApplicationClearData(false, pkgName);
                disableUninstallApplication(false, pkgName);

                mDPM.removeActiveAdmin(mDeviceAdmin);
            }

            CommonUtil.cancelPeriodicService(mContext, TrackingService.class);
            CommonUtil.enableLauncher(mContext);
            CommonUtil.uninstallApplication(mContext, packageName);

        } catch (Exception e) {
            Log.w(TAG, "Exception: " + e);
        }
    }

    public interface LicenseActivationListener {
        public void onActivationStart();
        public void onActivationSuccess();
        public void onActivationFailure();
    }
}
