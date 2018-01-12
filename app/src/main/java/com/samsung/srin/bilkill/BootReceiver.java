package com.samsung.srin.bilkill;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.samsung.srin.bilkill.controller.PolicyController;
import com.samsung.srin.bilkill.service.PolicyService;
import com.samsung.srin.bilkill.util.CommonUtil;


public class BootReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CommonUtil.runPeriodicService(context, PolicyService.class, 10);
    }
}
