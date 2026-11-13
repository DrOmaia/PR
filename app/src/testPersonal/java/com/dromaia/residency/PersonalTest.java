package com.dromaia.residency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;

/** The private build starts with the owner's dates and saves them on the phone at the first run. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PersonalTest {
    private final Context c = RuntimeEnvironment.getApplication();

    @Test
    public void firstRun_startsWithTheRecordAndSavesIt() {
        Store st = Store.load(c);
        assertEquals(LocalDate.parse("2025-05-28"), st.grant);
        assertEquals(5, st.trips.size());
        assertNull(st.openTrip());
        // saved at once: an update that carries no dates still finds them
        String saved = c.getSharedPreferences("data", Context.MODE_PRIVATE).getString("state", null);
        assertNotNull(saved);
        assertEquals("{\"grant\":\"2025-05-28\",\"trips\":[[\"2025-05-29\",\"2025-08-08\"],[\"2025-11-11\",\"2025-11-15\"],"
                + "[\"2026-03-07\",\"2026-03-25\"],[\"2026-03-25\",\"2026-03-25\"],[\"2026-05-19\",\"2026-07-31\"]]}", saved);
    }

    @Test
    public void ownerNumbers_onACheckedDay() {
        Store st = Store.load(c);
        // 28 May 2025 .. 13 Nov 2026 = 535 days; not counted: 72 + 5 + 19 + 74 = 170 (25 March is inside the third trip)
        Calc.Result r = Calc.compute(st.grant, st.trips, LocalDate.parse("2026-11-13"));
        assertEquals(365, r.done);
        assertEquals(535, r.remaining);
        assertEquals(LocalDate.parse("2026-11-13").plusDays(535), r.expected);
    }
}
