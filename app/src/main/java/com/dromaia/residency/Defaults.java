package com.dromaia.residency;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The data the app starts with on first install (from the owner's travel record). Editable in the app. */
final class Defaults {
    private Defaults() {}

    /** Premium Residency issue date; null = asked in the app. */
    static LocalDate grant() {
        return null;
    }

    static List<Calc.Trip> trips() {
        List<Calc.Trip> t = new ArrayList<>();
        t.add(trip("2025-05-29", "2025-08-08")); // Jordan
        t.add(trip("2025-11-11", "2025-11-15")); // Kuwait
        t.add(trip("2026-03-07", "2026-03-25")); // Oman
        t.add(trip("2026-03-25", "2026-03-25")); // Oman (out and back the same day)
        t.add(trip("2026-05-19", "2026-07-31")); // Jordan
        return t;
    }

    private static Calc.Trip trip(String exit, String entry) {
        return new Calc.Trip(LocalDate.parse(exit), entry == null ? null : LocalDate.parse(entry));
    }
}
