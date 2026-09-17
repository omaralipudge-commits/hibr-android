package com.hibrmemory.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReminderStore {
    private static final String PREF = "hibr_native_reminders_v1";
    private static final String KEY = "items";

    private ReminderStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static synchronized List<Reminder> list(Context c) {
        List<Reminder> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) {
                    Reminder r = Reminder.fromJson(o);
                    if (!r.id.isEmpty()) out.add(r);
                }
            }
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingLong(r -> r.triggerAt));
        return out;
    }

    public static synchronized Reminder get(Context c, String id) {
        if (id == null) return null;
        for (Reminder r : list(c)) if (id.equals(r.id)) return r;
        return null;
    }

    public static synchronized void put(Context c, Reminder reminder) {
        List<Reminder> items = list(c);
        items.removeIf(r -> r.id.equals(reminder.id));
        items.add(reminder);
        save(c, items);
    }

    public static synchronized void remove(Context c, String id) {
        List<Reminder> items = list(c);
        items.removeIf(r -> r.id.equals(id));
        save(c, items);
    }

    private static void save(Context c, List<Reminder> items) {
        JSONArray a = new JSONArray();
        for (Reminder r : items) {
            try { a.put(r.toJson()); } catch (Exception ignored) {}
        }
        prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    public static synchronized String asJson(Context c) {
        JSONArray a = new JSONArray();
        for (Reminder r : list(c)) {
            try { a.put(r.toJson()); } catch (Exception ignored) {}
        }
        return a.toString();
    }
}
