package com.samsung.srin.bilkill.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.samsung.srin.bilkill.controller.PolicyController;

import java.util.Map;

/**
 * Created by dvmin on 1/11/2018.
 */

public class NotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        String myCustomKey = data.get("lock");
        if(Integer.parseInt(myCustomKey) == 1){
            PolicyController.getInstance(this).lockoutDevice("123456","YOUR PHONE LOCKED !!!","0916262170");
        }
    }
}