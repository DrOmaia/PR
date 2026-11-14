package com.dromaia.residency;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * "personal" build only: the dates the app starts with on first install (the owner's record). Editable in the app.
 * Private data: it is compiled into the private release of this repository, never into the public update file
 * (the "pub" build has an empty Defaults).
 */
final class Defaults {
    private Defaults() {}

    /** Premium Residency issue date. */
    static LocalDate grant() {
        return LocalDate.parse("2025-05-28");
    }

    /** Expiry date exactly as on the residency: 2030/04/02, shown there as 1451/11/29 in Hijri. */
    static LocalDate end() {
        return LocalDate.parse("2030-04-02");
    }

    /** The Hijri date printed for end() {year, month, day}: the official record, not the phone's conversion. */
    static int[] endHijri() {
        return new int[]{1451, 11, 29};
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
