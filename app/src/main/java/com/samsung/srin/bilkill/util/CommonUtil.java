package com.samsung.srin.bilkill.util;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.samsung.srin.bilkill.BootReceiver;
import com.samsung.srin.bilkill.MainActivity;
import com.samsung.srin.bilkill.R;
import com.samsung.srin.bilkill.service.PolicyService;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * Created by SRIN on 8/13/2015.
 */
public class CommonUtil {

    private static final String PARAM_KEY_ACTIVATION = "activate";

    public static void disableLauncher(Context context){
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context.getPackageName(), MainActivity.class.getCanonicalName());
        p.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    public static void enableLauncher(Context context){
        PackageManager p = context.getPackageManager();
        ComponentName componentName = new ComponentName(context.getPackageName(), MainActivity.class.getCanonicalName());
        p.setComponentEnabledSetting(componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    public static void uninstallApplication(Context context, String packageName){
        Uri packageUri = Uri.parse("package:" + packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(uninstallIntent);
    }

    public static void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, BootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                                PackageManager.DONT_KILL_APP);
    }

    public static void runPeriodicService(Context context, Class service, int seconds){
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(context, service);
        PendingIntent pintent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), seconds * 1000, pintent);
    }

    public static void cancelPeriodicService(Context context, Class service){
        Intent intent = new Intent(context, service);
        PendingIntent pintent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pintent);
    }

    public static void sendNotification(Context context, String msg) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg);
        mNotificationManager.notify(1, mBuilder.build());
    }

    public static void saveActivationSuccess(Context context){
        String prefName = context.getString(R.string.preference_file_key);
        SharedPreferences sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PARAM_KEY_ACTIVATION, true);
        editor.commit();
    }

    public static boolean isActivationSuccess(Context context){
        String prefName = context.getString(R.string.preference_file_key);
        SharedPreferences sharedPref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(PARAM_KEY_ACTIVATION, false);
    }

}
