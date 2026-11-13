package com.dromaia.residency;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** The saved dates (SharedPreferences, on the phone only). */
final class Store {
    private static final String PREFS = "data";
    private static final String KEY = "state";
    static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");

    LocalDate grant;
    List<Calc.Trip> trips = new ArrayList<>();

    static LocalDate today() {
        return LocalDate.now(RIYADH);
    }

    static Store load(Context c) {
        Store s = new Store();
        String json = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null);
        if (json != null) {
            try {
                JSONObject o = new JSONObject(json);
                String g = o.optString("grant", "");
                s.grant = g.isEmpty() ? null : LocalDate.parse(g);
                JSONArray a = o.getJSONArray("trips");
                for (int i = 0; i < a.length(); i++) {
                    JSONArray t = a.getJSONArray(i);
                    String entry = t.optString(1, "");
                    s.trips.add(new Calc.Trip(LocalDate.parse(t.getString(0)),
                            entry.isEmpty() ? null : LocalDate.parse(entry)));
                }
                if (s.grant == null) s.grant = Defaults.grant();
                s.sort();
                return s;
            } catch (Exception e) {
                s = new Store();
            }
        }
        s.grant = Defaults.grant();
        s.trips = Defaults.trips();
        s.sort();
        return s;
    }

    void save(Context c) {
        sort();
        try {
            JSONObject o = new JSONObject();
            o.put("grant", grant == null ? "" : grant.toString());
            JSONArray a = new JSONArray();
            for (Calc.Trip t : trips) {
                JSONArray x = new JSONArray();
                x.put(t.exit.toString());
                x.put(t.entry == null ? "" : t.entry.toString());
                a.put(x);
            }
            o.put("trips", a);
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, o.toString()).apply();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    void sort() {
        trips.sort((a, b) -> a.exit.compareTo(b.exit));
    }

    /** The trip with no return date yet (the latest one), or null. */
    Calc.Trip openTrip() {
        for (int i = trips.size() - 1; i >= 0; i--) if (trips.get(i).entry == null) return trips.get(i);
        return null;
    }
}
