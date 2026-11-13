package com.dromaia.residency;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONObject;

/** How the widget looks: chosen in the app ("Widget appearance"), saved on the phone. */
final class Look {
    static final int DEFAULT_COLOR = 0xFF2A7F5D;
    static final int PAUSED_COLOR = 0xFF5F6E78; // abroad: the card turns grey whatever the colour
    /** Ready colours offered as circles. */
    static final int[] PRESETS = {DEFAULT_COLOR, 0xFF1F7A7A, 0xFF2F6DB5, 0xFF1F2F4D, 0xFF6B4FA3, 0xFFA34F6E,
            0xFF9C4A3C, 0xFFB8862B, 0xFF2B2F33, 0xFFFFFFFF, 0xFFEFE6D5};
    private static final String PREFS = "look";
    private static final String KEY = "look";

    int color = DEFAULT_COLOR; // card colour
    int alpha = 100;           // card opacity, %
    int layout = 0;            // 0 side by side · 1 number in the centre · 2 dates on top
    boolean swap;              // number and days done on the other sides
    int big = 100;             // big number: % of the largest size that fits
    int small = 100;           // the other numbers and texts, %
    // shown items, one switch each
    boolean title = true, status = true, number = true, leftLabel = true, done = true, doneLabel = true, bar = true,
            dateLabel = true, greg = true, hijri = true;

    /** Light card colours get dark text. */
    static boolean light(int color) {
        return Color.luminance(color) > 0.6f;
    }

    static Look load(Context c) {
        Look k = new Look();
        String json = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (json == null) return k;
        try {
            JSONObject o = new JSONObject(json);
            k.color = o.optInt("color", k.color) | 0xFF000000;
            k.alpha = clamp(o.optInt("alpha", k.alpha), 20, 100);
            k.layout = clamp(o.optInt("layout", k.layout), 0, 2);
            k.swap = o.optBoolean("swap", k.swap);
            k.big = clamp(o.optInt("big", k.big), 50, 100);
            k.small = clamp(o.optInt("small", k.small), 70, 150);
            k.title = o.optBoolean("title", true);
            k.status = o.optBoolean("status", k.title);           // before 1.6: one switch for title + status
            k.number = o.optBoolean("number", true);
            k.leftLabel = o.optBoolean("leftLabel", true);
            k.done = o.optBoolean("done", true);
            boolean labels = o.optBoolean("labels", true);         // before 1.6: one switch for the small labels
            k.doneLabel = o.optBoolean("doneLabel", labels);
            k.bar = o.optBoolean("bar", true);
            k.dateLabel = o.optBoolean("dateLabel", labels);
            k.greg = o.optBoolean("greg", true);
            k.hijri = o.optBoolean("hijri", true);
            k.normalize();
        } catch (Exception e) {
            return new Look();
        }
        return k;
    }

    Look copy() {
        Look k = new Look();
        k.color = color; k.alpha = alpha; k.layout = layout; k.swap = swap; k.big = big; k.small = small;
        k.title = title; k.status = status; k.number = number; k.leftLabel = leftLabel; k.done = done;
        k.doneLabel = doneLabel; k.bar = bar; k.dateLabel = dateLabel; k.greg = greg; k.hijri = hijri;
        return k;
    }

    /**
     * Switches off the least important item that is still on (used when the widget is too small for all of them).
     * Returns false when only one item is left.
     */
    boolean dropOne() {
        int on = (title ? 1 : 0) + (status ? 1 : 0) + (number ? 1 : 0) + (leftLabel ? 1 : 0) + (done ? 1 : 0)
                + (bar ? 1 : 0) + (greg ? 1 : 0) + (hijri ? 1 : 0);
        if (dateLabel && (greg || hijri)) { dateLabel = false; return true; }
        if (doneLabel && done) { doneLabel = false; return true; }
        if (on <= 1) return false;
        if (title) title = false;
        else if (bar) bar = false;
        else if (hijri && (greg || number || done)) hijri = false;
        else if (status) status = false;
        else if (done && (number || greg)) done = false;
        else if (leftLabel) leftLabel = false;
        else if (greg && number) greg = false;
        else if (done) done = false;
        else if (hijri) hijri = false;
        else return false;
        return true;
    }

    /** Never an empty card: with everything switched off the big number comes back. */
    void normalize() {
        if (!title && !status && !number && !leftLabel && !done && !bar && !greg && !hijri) number = true;
    }

    void save(Context c) {
        normalize();
        try {
            JSONObject o = new JSONObject();
            o.put("color", color).put("alpha", alpha).put("layout", layout).put("swap", swap).put("big", big)
                    .put("small", small).put("title", title).put("status", status).put("number", number)
                    .put("leftLabel", leftLabel).put("done", done).put("doneLabel", doneLabel).put("bar", bar)
                    .put("dateLabel", dateLabel).put("greg", greg).put("hijri", hijri);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, o.toString()).apply();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
