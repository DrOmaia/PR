package com.dromaia.residency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;

/** The public update file carries no dates, and keeps the dates already saved on the phone. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PubTest {
    private final Context c = RuntimeEnvironment.getApplication();

    @Test
    public void noDatesInside() {
        assertNull(Defaults.grant());
        assertTrue(Defaults.trips().isEmpty());
        Store st = Store.load(c);
        assertNull(st.grant);
        assertTrue(st.trips.isEmpty());
    }

    @Test
    public void update_keepsTheDatesSavedByThePrivateBuild() {
        // exactly what the private build writes at its first run (see PersonalTest)
        c.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("state",
                "{\"grant\":\"2025-05-28\",\"trips\":[[\"2025-05-29\",\"2025-08-08\"],[\"2025-11-11\",\"2025-11-15\"],"
                        + "[\"2026-03-07\",\"2026-03-25\"],[\"2026-03-25\",\"2026-03-25\"],[\"2026-05-19\",\"2026-07-31\"]]}")
                .commit();
        Store st = Store.load(c);
        assertEquals(LocalDate.parse("2025-05-28"), st.grant);
        assertEquals(5, st.trips.size());
        assertEquals(365, Calc.compute(st.grant, st.trips, LocalDate.parse("2026-11-13")).done);
    }
}
