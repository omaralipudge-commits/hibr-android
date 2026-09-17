package com.hibrmemory.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("reminder_id");
        Reminder r = ReminderStore.get(context, id);
        if (r == null) return;
        Intent s = new Intent(context, AlarmService.class);
        s.setAction(AlarmService.ACTION_START);
        s.putExtra("reminder_id", id);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(s); else context.startService(s);
    }
}
