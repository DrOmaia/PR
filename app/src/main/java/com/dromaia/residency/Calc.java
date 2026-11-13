package com.dromaia.residency;

import java.time.LocalDate;
import java.util.List;

/**
 * The counting rule (no Android code here, so it is unit tested on the JVM).
 *
 * A day counts when it is on or after the Premium Residency date and the holder is inside Saudi Arabia
 * for the whole day: the exit day and the return day are NOT counted.
 * A trip with no return date is "open": the counter is frozen, and the expected date assumes a return today,
 * so it moves one day later every day until the return is recorded.
 */
public final class Calc {
    public static final int TARGET = 900;
    public static final int WINDOW_YEARS = 5;

    private Calc() {}

    public static final class Trip {
        public LocalDate exit;
        public LocalDate entry; // null = not returned yet

        public Trip(LocalDate exit, LocalDate entry) {
            this.exit = exit;
            this.entry = entry;
        }

        /** Days not counted because of this trip (exit and return days included). */
        public int daysOut(LocalDate today) {
            LocalDate end = entry != null ? entry : (today.isAfter(exit) ? today : exit);
            return (int) (end.toEpochDay() - exit.toEpochDay()) + 1;
        }
    }

    public static final class Result {
        public int done;          // counted days up to today (capped at TARGET)
        public int remaining;     // TARGET - done
        public boolean abroad;    // outside Saudi Arabia today: the counter is frozen
        public boolean reached;   // TARGET already completed
        public boolean late;      // expected date falls after the 5-year window
        public LocalDate expected; // the day the TARGET-th day is completed
        public LocalDate deadline; // grant date + 5 years
    }

    static boolean outside(List<Trip> trips, LocalDate d, LocalDate today) {
        for (Trip t : trips) {
            LocalDate end = t.entry != null ? t.entry : (today.isAfter(t.exit) ? today : t.exit);
            if (!d.isBefore(t.exit) && !d.isAfter(end)) return true;
        }
        return false;
    }

    public static Result compute(LocalDate grant, List<Trip> trips, LocalDate today) {
        Result r = new Result();
        for (Trip t : trips) {
            if (!t.exit.isAfter(today) && (t.entry == null || today.isBefore(t.entry))) r.abroad = true;
        }
        r.remaining = TARGET;
        if (grant == null) return r;

        r.deadline = grant.plusYears(WINDOW_YEARS);
        int count = 0;
        LocalDate limit = grant.plusYears(60);
        for (LocalDate d = grant; d.isBefore(limit); d = d.plusDays(1)) {
            if (!outside(trips, d, today)) count++;
            if (count == TARGET && r.expected == null) r.expected = d;
            if (d.equals(today)) r.done = Math.min(count, TARGET);
            if (r.expected != null && !d.isBefore(today)) break;
        }
        r.remaining = TARGET - r.done;
        r.reached = r.expected != null && !r.expected.isAfter(today);
        r.late = r.expected != null && !r.expected.isBefore(r.deadline);
        return r;
    }
}
