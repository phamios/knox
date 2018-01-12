package com.samsung.srin.bilkill;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.samsung.srin.bilkill.util.RegisterUtil;

/**
 * Created by redsu on 12/20/2017.
 */

public class RegistrationIntentService  extends IntentService {
    private static final String TAG = "RegIntentService";


    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "FCM Registration Token: " + token);
        RegisterUtil reg = new RegisterUtil();
        reg.setMessageCode(getApplicationContext(),token);
    }
}
