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
    LocalDate end;   // expiry date as given (starting data or set by hand); null = worked out from the issue date
    int[] endHijri;  // the Hijri {year, month, day} printed for it on the residency; null = converted by the phone
    List<Calc.Trip> trips = new ArrayList<>();

    /** The residency's expiry date: the one set by hand, else 5 Hijri years from the issue date. */
    LocalDate end() {
        return end != null ? end : grant != null ? Calc.defaultEnd(grant) : null;
    }

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
                String e = o.optString("end", "");
                s.end = e.isEmpty() ? null : LocalDate.parse(e);
                JSONArray eh = o.optJSONArray("endHijri");
                if (eh != null && eh.length() == 3) s.endHijri = new int[]{eh.getInt(0), eh.getInt(1), eh.getInt(2)};
                JSONArray a = o.getJSONArray("trips");
                for (int i = 0; i < a.length(); i++) {
                    JSONArray t = a.getJSONArray(i);
                    String entry = t.optString(1, "");
                    s.trips.add(new Calc.Trip(LocalDate.parse(t.getString(0)),
                            entry.isEmpty() ? null : LocalDate.parse(entry)));
                }
                if (s.grant == null && s.trips.isEmpty() && Defaults.grant() != null) {
                    // nothing was ever entered (e.g. installed first from a build with no dates): take the starting dates
                    s.grant = Defaults.grant();
                    s.end = Defaults.end();
                    s.endHijri = Defaults.endHijri();
                    s.trips = Defaults.trips();
                    s.save(c);
                } else if (s.grant == null && Defaults.grant() != null) {
                    s.grant = Defaults.grant();
                    s.save(c);
                }
                if (s.end == null && Defaults.end() != null && Defaults.grant().equals(s.grant)) {
                    // saved before 1.7 (no expiry date yet): the owner's dates get the expiry date of her residency
                    s.end = Defaults.end();
                    s.endHijri = Defaults.endHijri();
                    s.save(c);
                }
                s.sort();
                return s;
            } catch (Exception e) {
                s = new Store();
            }
        }
        // First run: the starting dates are saved on the phone at once, so later updates (which carry no dates) keep them.
        s.grant = Defaults.grant();
        s.end = Defaults.end();
        s.endHijri = Defaults.endHijri();
        s.trips = Defaults.trips();
        s.save(c);
        return s;
    }

    void save(Context c) {
        sort();
        try {
            JSONObject o = new JSONObject();
            o.put("grant", grant == null ? "" : grant.toString());
            if (end != null) o.put("end", end.toString());
            if (end != null && endHijri != null) o.put("endHijri", new JSONArray().put(endHijri[0]).put(endHijri[1]).put(endHijri[2]));
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
