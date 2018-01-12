package com.samsung.srin.bilkill;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.ApplicationPolicy;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.samsung.srin.bilkill.util.RegisterUtil;

/**
 * Created by redsu on 12/20/2017.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "VINKNOX_";
    private ApplicationPolicy appPolicy;

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        sendRegistrationToServer(refreshedToken);

    }

    private void sendRegistrationToServer(String token) {
        RegisterUtil reg = new RegisterUtil();
        reg.setMessageCode(getApplicationContext(),token);
    }



}
