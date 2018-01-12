package com.samsung.srin.bilkill.service;

import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.samsung.srin.bilkill.R;
import com.samsung.srin.bilkill.controller.PolicyController;
import com.samsung.srin.bilkill.util.CommonUtil;

/**
 * Created by redsu on 12/26/2017.
 */

public class LicenseReceiver extends BroadcastReceiver {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "fail";
    public static String MDM_LITE_PREF = "MDM_LITE_PREF";
    private boolean mELMActive = false;
    public static String ELMKEY = "ELMKEY";
    private int DEFAULT_ERROR_CODE = -1;
    private static String TAG = "KNOX Receiver";


    @Override
    public void onReceive(Context context, Intent intent) {

        int msg_res = -1;

        if (intent == null) {
            Log.d(TAG, "intent = null - No intent action is available");
        } else {
            String action = intent.getAction();
            if (action == null) {
                Log.d(TAG, "action = null - No intent action is available");
            }  else if (action.equals(EnterpriseLicenseManager.ACTION_LICENSE_STATUS)) {
                int errorCode = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, DEFAULT_ERROR_CODE);
                int resultType = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, DEFAULT_ERROR_CODE);

                if (resultType == 800) {
                    if (errorCode == EnterpriseLicenseManager.ERROR_NONE) {
                        // ELM activated successfully
                        Toast.makeText(context, R.string.elm_activated_succesfully, Toast.LENGTH_SHORT).show();
                        Log.d("LicenseReceiver", context.getString(R.string.elm_activated_succesfully));
                        mELMActive = true;
                        SharedPreferences sharedPref = context.getSharedPreferences(MDM_LITE_PREF, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean(ELMKEY, mELMActive);
                        editor.commit();

                        if(!CommonUtil.isActivationSuccess(context)) {
                            PolicyController.getInstance(context).activationListener.onActivationSuccess();
                        }

                    } else {
                        // ELM activation failed
                        final String errorMsg = "ELM failure: " + errorCode;
                        Log.w(TAG, errorMsg);
                        mELMActive = false;

                        PolicyController.getInstance(context).activationListener.onActivationFailure();

                        // Display ELM error message
                        Log.d("LicenseReceiver", "ELM activation failed");

                    }
                } else {
                    Log.e(TAG, "unknown ELM state ignored: " + resultType + ", " + errorCode);
                    PolicyController.getInstance(context).activationListener.onActivationFailure();
                }
            }
        }
    }
}