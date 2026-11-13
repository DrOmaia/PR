package com.dromaia.residency;

import android.content.Context;
import android.icu.util.IslamicCalendar;
import android.icu.util.TimeZone;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.Locale;

/** The texts shown on the widget and at the top of the app (one source for both). */
final class Summary {
    String status, remaining, done, dateLabel, dateG, dateH, warning;
    int progress;
    boolean abroad, noGrant;

    static Summary of(Context c, Store st, LocalDate today) {
        Calc.Result r = Calc.compute(st.grant, st.trips, today);
        Summary s = new Summary();
        s.abroad = r.abroad;
        s.noGrant = st.grant == null;
        s.status = c.getString(s.noGrant ? R.string.st_nogrant : r.abroad ? R.string.st_abroad : R.string.st_inside);
        s.remaining = s.noGrant ? "–" : num(r.remaining);
        s.done = num(r.done) + " / " + num(Calc.TARGET);
        s.progress = r.done;
        s.dateLabel = c.getString(r.reached ? R.string.lbl_reached : R.string.lbl_expected);
        s.dateG = r.expected == null ? "" : greg(c, r.expected);
        s.dateH = r.expected == null ? "" : hijri(c, r.expected);
        s.warning = r.late && !r.reached ? c.getString(R.string.warn_late, greg(c, r.deadline)) : null;
        return s;
    }

    static String num(int n) {
        return String.format(Locale.US, "%d", n);
    }

    static String greg(Context c, LocalDate d) {
        String[] m = c.getResources().getStringArray(R.array.greg_months);
        return c.getString(R.string.date_g, num(d.getDayOfMonth()), m[d.getMonthValue() - 1], num(d.getYear()));
    }

    /** Umm al-Qura calendar. */
    static String hijri(Context c, LocalDate d) {
        int day, month, year;
        try {
            HijrahDate h = HijrahDate.from(d);
            day = h.get(ChronoField.DAY_OF_MONTH);
            month = h.get(ChronoField.MONTH_OF_YEAR);
            year = h.get(ChronoField.YEAR);
        } catch (RuntimeException e) {
            IslamicCalendar ic = new IslamicCalendar(TimeZone.getTimeZone("Asia/Riyadh"));
            ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
            ic.setTimeInMillis(d.atTime(12, 0).atZone(Store.RIYADH).toInstant().toEpochMilli());
            day = ic.get(IslamicCalendar.DAY_OF_MONTH);
            month = ic.get(IslamicCalendar.MONTH) + 1;
            year = ic.get(IslamicCalendar.YEAR);
        }
        String[] m = c.getResources().getStringArray(R.array.hijri_months);
        return c.getString(R.string.date_h, num(day), m[month - 1], num(year));
    }
}
