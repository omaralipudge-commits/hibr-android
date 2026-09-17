package com.hibrmemory.app;

import android.app.*;
import android.content.Intent;
import android.media.*;
import android.net.Uri;
import android.os.*;

public class AlarmService extends Service {
    public static final String ACTION_START = "com.hibrmemory.app.START_ALARM";
    public static final String ACTION_STOP = "com.hibrmemory.app.STOP_SERVICE";
    private static final String CHANNEL = "hibr_alarm_channel_v1";
    private static final int NOTIFICATION_ID = 2201;
    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Hibr Alarms", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Alarm reminders from Hibr");
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    private PendingIntent action(String action, String id, int salt) {
        Intent i = new Intent(this, AlarmActionReceiver.class);
        i.setAction(action);
        i.putExtra("reminder_id", id);
        return PendingIntent.getBroadcast(this, id.hashCode() ^ salt, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification notification(Reminder r) {
        Intent full = new Intent(this, AlarmActivity.class);
        full.putExtra("reminder_id", r.id);
        full.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullPi = PendingIntent.getActivity(this, r.id.hashCode() ^ 0x771, full, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent open = new Intent(this, MainActivity.class);
        open.putExtra("alarm_note_id", r.noteId);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, r.id.hashCode() ^ 0x772, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.hibr_icon)
                .setContentTitle(r.title)
                .setContentText(r.message.isEmpty() ? "تذكير من حِبر" : r.message)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(openPi)
                .setFullScreenIntent(fullPi, true)
                .addAction(new Notification.Action.Builder(R.drawable.hibr_icon, "غفوة 10 دقائق", action(AlarmActionReceiver.SNOOZE, r.id, 0x91)).build())
                .addAction(new Notification.Action.Builder(R.drawable.hibr_icon, "إيقاف", action(AlarmActionReceiver.STOP, r.id, 0x92)).build())
                .build();
    }

    private void ring() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception ignored) {}
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 700, 350, 700, 350};
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)); else vibrator.vibrate(pattern, 0);
        }
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hibr:alarm");
        wakeLock.acquire(30 * 60 * 1000L);
    }

    private void stopAlarm() {
        try { if (player != null) { player.stop(); player.release(); player = null; } } catch (Exception ignored) {}
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) {}
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopAlarm(); return START_NOT_STICKY; }
        String id = intent == null ? null : intent.getStringExtra("reminder_id");
        Reminder r = ReminderStore.get(this, id);
        if (r == null) { stopSelf(); return START_NOT_STICKY; }
        startForeground(NOTIFICATION_ID, notification(r));
        ring();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() { stopAlarm(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
