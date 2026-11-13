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
    boolean title = true, done = true, bar = true, greg = true, hijri = true, labels = true; // shown items

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
            k.done = o.optBoolean("done", true);
            k.bar = o.optBoolean("bar", true);
            k.greg = o.optBoolean("greg", true);
            k.hijri = o.optBoolean("hijri", true);
            k.labels = o.optBoolean("labels", true);
        } catch (Exception e) {
            return new Look();
        }
        return k;
    }

    void save(Context c) {
        try {
            JSONObject o = new JSONObject();
            o.put("color", color).put("alpha", alpha).put("layout", layout).put("swap", swap).put("big", big)
                    .put("small", small).put("title", title).put("done", done).put("bar", bar).put("greg", greg)
                    .put("hijri", hijri).put("labels", labels);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, o.toString()).apply();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
