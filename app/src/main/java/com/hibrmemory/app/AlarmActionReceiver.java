package com.hibrmemory.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmActionReceiver extends BroadcastReceiver {
    public static final String STOP = "com.hibrmemory.app.STOP_ALARM";
    public static final String SNOOZE = "com.hibrmemory.app.SNOOZE_ALARM";

    @Override public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("reminder_id");
        if (id == null) return;
        if (SNOOZE.equals(intent.getAction())) {
            Reminder old = ReminderStore.get(context, id);
            if (old != null) {
                Reminder snoozed = new Reminder(old.id, old.title, old.message, old.noteId, System.currentTimeMillis() + 10 * 60 * 1000L);
                AlarmScheduler.schedule(context, snoozed);
            }
        } else {
            AlarmScheduler.cancel(context, id);
        }
        Intent s = new Intent(context, AlarmService.class);
        s.setAction(AlarmService.ACTION_STOP);
        s.putExtra("reminder_id", id);
        context.startService(s);
    }
}
