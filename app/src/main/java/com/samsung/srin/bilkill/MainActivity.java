package com.samsung.srin.bilkill;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.EnterpriseDeviceManager;
import android.app.enterprise.SecurityPolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.samsung.srin.bilkill.controller.PolicyController;
import com.samsung.srin.bilkill.service.NotificationReceiver;
import com.samsung.srin.bilkill.service.PolicyService;
import com.samsung.srin.bilkill.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SRIN on 8/10/2015.
 */
public class MainActivity extends Activity implements PolicyController.LicenseActivationListener {

    private AlertDialog dialog;
    private final String TAG = "MainActivity";
    static final int DEVICE_ADMIN_ADD_RESULT_ENABLE = 1;
    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDPM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = this.getLayoutInflater();
        dialog = new AlertDialog.Builder(this)
                .setTitle("Activate " + getString(R.string.app_name))
                .setView(inflater.inflate(R.layout.progress_dialog, null))
                .create();
        PolicyController.getInstance(this).activateAdmin(this, this);

    }

    @Override
    public void onActivationStart(){
        dialog.show();
    }

    @Override
    public void onActivationSuccess() {


        // activate default policy
        PolicyController.getInstance(this).enableDeviceAdmin(true);
//        PolicyController.getInstance(this).disableFactoryReset(true);
//        PolicyController.getInstance(this).disableApplicationClearData(true, getPackageName());
//        PolicyController.getInstance(this).disableUninstallApplication(true, getPackageName());
//        PolicyController.getInstance(this).lockoutDevice("123456","Ahihi Hacked !!!","0916262170");

        // enable boot receiver
        CommonUtil.enableBootReceiver(this);
        // run periodic service
        CommonUtil.runPeriodicService(this, PolicyService.class, 10);
        // hide launcher icons
        CommonUtil.disableLauncher(this);
        // save state   
        CommonUtil.saveActivationSuccess(this);

        dialog.cancel();
        finish();

    }

    @Override
    public void onActivationFailure() {
        dialog.cancel();
        Toast.makeText(this, "Failed to Activate License", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        dialog.cancel();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PolicyController.RESULT_ENABLE && resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Intent intent_o = getIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
