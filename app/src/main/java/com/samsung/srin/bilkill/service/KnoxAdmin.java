package com.samsung.srin.bilkill.service;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.widget.Toast;

import com.samsung.srin.bilkill.R;
import com.samsung.srin.bilkill.controller.PolicyController;

/**
 * Created by redsu on 12/26/2017.
 */

public  class KnoxAdmin extends DeviceAdminReceiver {

    public static PolicyController.LicenseActivationListener activationListener;

    void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        showToast(context, context.getString(R.string.app_name) + ": enabled");
        PolicyController.ActivateLicenseAsyncTask licenseTask = new PolicyController.ActivateLicenseAsyncTask(context);
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
