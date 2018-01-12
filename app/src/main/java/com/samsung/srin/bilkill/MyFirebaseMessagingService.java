package com.samsung.srin.bilkill;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.enterprise.ApplicationPolicy;
import android.app.enterprise.EnterpriseDeviceManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.samsung.srin.bilkill.controller.PolicyController;
import com.samsung.srin.bilkill.service.NotificationReceiver;

/**
 * Created by redsu on 12/20/2017.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private EnterpriseDeviceManager mEDM;
    private ApplicationPolicy appPolicy;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        processCommand();

        ComponentName componentToEnable = new ComponentName(this, MainActivity.class);
        PackageManager pm = this.getPackageManager();
        pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        NotificationReceiver receiver = new NotificationReceiver();
        final IntentFilter filter = new IntentFilter("com.google.firebase.MESSAGING_EVENT");
        registerReceiver(receiver, filter);

    }

    private void processCommand(){
        PolicyController.getInstance(this).lockoutDevice("123456","YOUR PHONE LOCKED !!!","0916262170");
    }



}