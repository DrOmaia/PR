package com.dromaia.residency;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import java.util.Locale;

/** Interface language: the phone's language until the button in the app chooses Arabic or English. */
final class Lang {
    private static final String PREFS = "ui";
    private static final String KEY = "lang";

    private Lang() {}

    /** "" = follow the phone, or "ar" / "en". */
    static String get(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "");
    }

    static void set(Context c, String lang) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).commit();
    }

    /** A context whose texts are in the chosen language. */
    static Context wrap(Context c) {
        String lang = get(c);
        if (lang.isEmpty()) return c;
        Configuration cfg = new Configuration(c.getResources().getConfiguration());
        cfg.setLocale(new Locale(lang));
        return c.createConfigurationContext(cfg);
    }

    static boolean arabic(Context wrapped) {
        return "ar".equals(wrapped.getResources().getConfiguration().getLocales().get(0).getLanguage());
    }

    static boolean rtl(Context wrapped) {
        return wrapped.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
}
