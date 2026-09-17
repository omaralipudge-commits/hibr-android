package com.hibrmemory.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    private static PendingIntent pending(Context c, String id) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("com.hibrmemory.app.ALARM." + id);
        i.putExtra("reminder_id", id);
        return PendingIntent.getBroadcast(c, id.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean canExact(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
    }

    public static boolean schedule(Context c, Reminder r) {
        if (r == null || r.id.isEmpty() || r.triggerAt <= System.currentTimeMillis() + 3000L) return false;
        if (!canExact(c)) return false;
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent show = new Intent(c, MainActivity.class);
        show.putExtra("alarm_note_id", r.noteId);
        PendingIntent showPi = PendingIntent.getActivity(c, r.id.hashCode() ^ 0x413, show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(r.triggerAt, showPi);
        am.setAlarmClock(info, pending(c, r.id));
        ReminderStore.put(c, r);
        return true;
    }

    public static void cancel(Context c, String id) {
        if (id == null || id.isEmpty()) return;
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(c, id));
        ReminderStore.remove(c, id);
    }

    public static void rescheduleAll(Context c) {
        long now = System.currentTimeMillis();
        for (Reminder r : ReminderStore.list(c)) {
            if (r.triggerAt > now + 3000L) schedule(c, r);
            else if (now - r.triggerAt <= 6L * 60L * 60L * 1000L && canExact(c))
                schedule(c, new Reminder(r.id, r.title, r.message, r.noteId, now + 5000L));
            else if (now - r.triggerAt > 6L * 60L * 60L * 1000L) ReminderStore.remove(c, r.id);
        }
    }
}
