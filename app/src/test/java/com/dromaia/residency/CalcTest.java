package com.dromaia.residency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CalcTest {
    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private static Calc.Trip trip(String exit, String entry) {
        return new Calc.Trip(d(exit), entry == null ? null : d(entry));
    }

    @Test
    public void noTrips_everyDayCountsFromTheGrantDay() {
        Calc.Result r = Calc.compute(d("2025-01-01"), new ArrayList<>(), d("2025-01-10"));
        assertEquals(10, r.done);
        assertEquals(890, r.remaining);
        assertEquals(d("2025-01-01").plusDays(899), r.expected);
        assertFalse(r.abroad);
        assertFalse(r.reached);
        assertFalse(r.late);
    }

    @Test
    public void exitAndReturnDaysAreNotCounted() {
        // out 5th..8th inclusive = 4 days not counted
        List<Calc.Trip> trips = Arrays.asList(trip("2025-01-05", "2025-01-08"));
        Calc.Result r = Calc.compute(d("2025-01-01"), trips, d("2025-01-10"));
        assertEquals(6, r.done);
        assertEquals(d("2025-01-01").plusDays(899 + 4), r.expected);
        assertEquals(4, trips.get(0).daysOut(d("2025-01-10")));
    }

    @Test
    public void openTrip_freezesTheCounterAndPushesTheDate() {
        List<Calc.Trip> trips = Arrays.asList(trip("2025-01-05", null));
        Calc.Result a = Calc.compute(d("2025-01-01"), trips, d("2025-01-10"));
        Calc.Result b = Calc.compute(d("2025-01-01"), trips, d("2025-01-11"));
        assertTrue(a.abroad);
        assertEquals(4, a.done);
        assertEquals(4, b.done);                          // frozen
        assertEquals(a.expected.plusDays(1), b.expected); // one day later every day
        assertEquals(d("2025-01-10").plusDays(896), a.expected);
    }

    @Test
    public void afterReturn_theCounterRunsAndTheDateStops() {
        List<Calc.Trip> trips = Arrays.asList(trip("2025-01-05", "2025-01-10"));
        Calc.Result a = Calc.compute(d("2025-01-01"), trips, d("2025-01-10"));
        Calc.Result b = Calc.compute(d("2025-01-01"), trips, d("2025-01-11"));
        Calc.Result c = Calc.compute(d("2025-01-01"), trips, d("2025-01-12"));
        assertFalse(a.abroad);          // back today, but today is not counted
        assertEquals(4, a.done);
        assertEquals(5, b.done);
        assertEquals(6, c.done);
        assertEquals(a.expected, b.expected);
        assertEquals(b.expected, c.expected);
    }

    @Test
    public void sameDayAndOverlappingTripsAreNotCountedTwice() {
        List<Calc.Trip> one = Arrays.asList(trip("2025-01-05", "2025-01-08"));
        List<Calc.Trip> two = Arrays.asList(trip("2025-01-05", "2025-01-08"), trip("2025-01-08", "2025-01-08"));
        assertEquals(Calc.compute(d("2025-01-01"), one, d("2025-02-01")).done,
                Calc.compute(d("2025-01-01"), two, d("2025-02-01")).done);
    }

    @Test
    public void reachedAndLate() {
        Calc.Result r = Calc.compute(d("2020-01-01"), new ArrayList<>(), d("2025-01-01"));
        assertTrue(r.reached);
        assertEquals(900, r.done);
        assertEquals(0, r.remaining);
        assertEquals(d("2020-01-01").plusDays(899), r.expected);

        // away for more than 926 days of the 5 years: 900 days no longer fit
        List<Calc.Trip> longTrip = Arrays.asList(trip("2020-02-01", "2023-06-01"));
        Calc.Result late = Calc.compute(d("2020-01-01"), longTrip, d("2023-07-01"));
        assertTrue(late.late);
        assertFalse(late.reached);
    }

    @Test
    public void beforeGrantOrNoGrant() {
        assertEquals(0, Calc.compute(d("2025-06-01"), new ArrayList<>(), d("2025-01-01")).done);
        Calc.Result r = Calc.compute(null, new ArrayList<>(), d("2025-01-01"));
        assertEquals(900, r.remaining);
        assertEquals(null, r.expected);
    }
}
