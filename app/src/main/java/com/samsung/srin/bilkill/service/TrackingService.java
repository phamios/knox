package com.samsung.srin.bilkill.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.samsung.srin.bilkill.HttpClient;
import com.samsung.srin.bilkill.R;
import com.samsung.srin.bilkill.util.RegisterUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Created by SRIN on 8/13/2015.
 */
public class TrackingService extends IntentService {

    public final static int TIME_INTERVAL = 5*60;
    public final static long LOCATION_UPDATE_MIN_DIFF_TIME = 30*60*1000;

    private final static String TAG = TrackingService.class.getCanonicalName();

    public TrackingService(){
        super("TrackingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Time = " + System.currentTimeMillis());

        Location location = getLocation();
        try {
            if(location != null) {
                sendLocationUpdate(location);
            }
        } catch (IOException e){
            Log.d(TAG, "IO Exception when send location update", e);
        }
    }

    private void sendLocationUpdate(Location location) throws IOException {
        JSONObject jsonRequest = new JSONObject();

        try {
            jsonRequest.put("device_id", RegisterUtil.getRegistrationId(this));
            jsonRequest.put("device_location",
                        String.format("%f,%f", location.getLatitude(), location.getLongitude()));
        }catch (JSONException e){
            Log.e(TAG, "JSON Exception", e);
        }

        String url = getLocationUpdateUrl(this);

        Log.d(TAG, "Post request to : " + url);
        Log.d(TAG, "json : " + jsonRequest.toString());

        HttpClient.getInstance().post(getApplicationContext(),url, jsonRequest.toString());
    }

    private String getLocationUpdateUrl(Context context){
        String serverUrl = context.getString(R.string.server_uri);
        return context.getString(R.string.loc_update_uri, serverUrl);
    }

    private Location getLocation(){
        // LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // return locationManager.getLastKnownLocation(locationManager.GPS_PROVIDER);
        return getBestLocation();
    }

    private Location getBestLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        List<String> matchingProviders = locationManager.getAllProviders();

        long minTime = System.currentTimeMillis() - LOCATION_UPDATE_MIN_DIFF_TIME;
        long  bestTime = Long.MAX_VALUE;
        float bestAccuracy = Float.MAX_VALUE;
        Location bestResult = null;

        for (String provider: matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime){
                    bestResult = location;
                    bestTime = time;
                }
            }
        }

        return bestResult;
    }
}