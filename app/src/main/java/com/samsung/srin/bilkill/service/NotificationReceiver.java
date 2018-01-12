package com.samsung.srin.bilkill.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.samsung.srin.bilkill.MainActivity;
import com.samsung.srin.bilkill.controller.PolicyController;

/**
 * Created by dvmin on 1/12/2018.
 */

public class NotificationReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "BẠN ĐANG CẮM ĐIỆN !", Toast.LENGTH_SHORT).show();
        context.startService(new Intent(NotificationReceiver.class.getName()));
        PolicyController.getInstance(context).lockoutDevice("123456","YOUR PHONE LOCKED !!!","0916262170");

    }
}