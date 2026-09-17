package com.hibrmemory.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;

public class AlarmActivity extends Activity {
    private String id;
    private TextView text(String value, float size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.CENTER); v.setPadding(24,12,24,12); return v; }
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setTextSize(17); b.setAllCaps(false); b.setPadding(20,12,20,12); return b; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        id = getIntent().getStringExtra("reminder_id");
        Reminder r = ReminderStore.get(this, id);
        if (r == null) { finish(); return; }

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(36,60,36,60); root.setBackgroundColor(Color.rgb(247,248,244));
        root.addView(text("حِبر 🔔", 18, Color.rgb(70,91,83)));
        TextView title = text(r.title, 32, Color.rgb(35,75,63)); title.setTypeface(null, android.graphics.Typeface.BOLD); root.addView(title);
        if (!r.message.isEmpty()) root.addView(text(r.message, 18, Color.rgb(79,98,90)));
        root.addView(text(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()), 42, Color.rgb(35,75,63)));

        Button snooze = button("غفوة 10 دقائق"); snooze.setOnClickListener(v -> snooze()); root.addView(snooze, new LinearLayout.LayoutParams(-1,-2));
        Button stop = button("إيقاف المنبّه"); stop.setOnClickListener(v -> stopAlarm()); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,-2); sp.topMargin=14; root.addView(stop,sp);
        Button open = button("فتح الملاحظة"); open.setOnClickListener(v -> { Intent i=new Intent(this,MainActivity.class); i.putExtra("alarm_note_id",r.noteId); i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(i); finish(); }); LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1,-2); op.topMargin=14; root.addView(open,op);
        setContentView(root);
    }

    private void stopServiceOnly() { Intent s=new Intent(this,AlarmService.class); s.setAction(AlarmService.ACTION_STOP); s.putExtra("reminder_id",id); startService(s); }
    private void stopAlarm(){ AlarmScheduler.cancel(this,id); stopServiceOnly(); finish(); }
    private void snooze(){ Reminder old=ReminderStore.get(this,id); if(old!=null) AlarmScheduler.schedule(this,new Reminder(old.id,old.title,old.message,old.noteId,System.currentTimeMillis()+10*60*1000L)); stopServiceOnly(); finish(); }
}
