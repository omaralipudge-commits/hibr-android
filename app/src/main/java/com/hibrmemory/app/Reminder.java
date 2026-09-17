package com.hibrmemory.app;

import org.json.JSONException;
import org.json.JSONObject;

public class Reminder {
    public final String id;
    public final String title;
    public final String message;
    public final String noteId;
    public final long triggerAt;

    public Reminder(String id, String title, String message, String noteId, long triggerAt) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "Hibr" : title;
        this.message = message == null ? "" : message;
        this.noteId = noteId == null ? "" : noteId;
        this.triggerAt = triggerAt;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("message", message);
        o.put("noteId", noteId);
        o.put("triggerAt", triggerAt);
        return o;
    }

    public static Reminder fromJson(JSONObject o) {
        return new Reminder(
                o.optString("id", ""),
                o.optString("title", "Hibr"),
                o.optString("message", ""),
                o.optString("noteId", ""),
                o.optLong("triggerAt", 0L)
        );
    }
}
