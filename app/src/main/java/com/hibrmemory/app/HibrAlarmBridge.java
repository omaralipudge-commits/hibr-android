package com.hibrmemory.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import org.json.JSONObject;

public class HibrAlarmBridge {
    private final Activity activity;
    public HibrAlarmBridge(Activity activity) { this.activity = activity; }

    private boolean notificationsAllowed() {
        return Build.VERSION.SDK_INT < 33 || activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface public boolean canScheduleExact() {
        return AlarmScheduler.canExact(activity) && notificationsAllowed();
    }

    @JavascriptInterface public void requestPermissions() {
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1202);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager am = (AlarmManager)activity.getSystemService(Activity.ALARM_SERVICE);
                if (!am.canScheduleExactAlarms()) {
                    try { activity.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + activity.getPackageName()))); } catch (Exception ignored) {}
                }
            }
        });
    }

    @JavascriptInterface public boolean schedule(String json) {
        try {
            if (!canScheduleExact()) return false;
            JSONObject o = new JSONObject(json);
            return AlarmScheduler.schedule(activity, new Reminder(o.optString("id", ""), o.optString("title", "Hibr"), o.optString("message", ""), o.optString("noteId", ""), o.optLong("triggerAt", 0L)));
        } catch (Exception e) { return false; }
    }

    @JavascriptInterface public void cancel(String id) { AlarmScheduler.cancel(activity, id); }
    @JavascriptInterface public String list() { return ReminderStore.asJson(activity); }
    @JavascriptInterface public void stopRinging() {
        Intent i = new Intent(activity, AlarmService.class); i.setAction(AlarmService.ACTION_STOP); activity.startService(i);
    }
}
