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
        assertEquals("{\"grant\":\"2025-05-28\",\"end\":\"2030-04-02\",\"endHijri\":[1451,11,29],"
                + "\"trips\":[[\"2025-05-29\",\"2025-08-08\"],[\"2025-11-11\",\"2025-11-15\"],"
                + "[\"2026-03-07\",\"2026-03-25\"],[\"2026-03-25\",\"2026-03-25\"],[\"2026-05-19\",\"2026-07-31\"]]}", saved);
    }

    @Test
    public void afterABuildWithNoDates_theStartingDatesArrive() {
        // what the build with no dates saves at its first run
        c.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("state", "{\"grant\":\"\",\"trips\":[]}").commit();
        Store st = Store.load(c);
        assertEquals(LocalDate.parse("2025-05-28"), st.grant);
        assertEquals(5, st.trips.size());
    }

    @Test
    public void datesEnteredByHand_areNeverReplaced() {
        c.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("state",
                "{\"grant\":\"2024-01-15\",\"trips\":[[\"2024-03-01\",\"2024-03-10\"]]}").commit();
        Store st = Store.load(c);
        assertEquals(LocalDate.parse("2024-01-15"), st.grant);
        assertEquals(1, st.trips.size());
    }

    @Test
    public void expiryDate_isShownAsOnTheResidency_alsoForDatesSavedBefore() {
        // what 1.6 had saved on the owner's phone: no expiry date yet
        c.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putString("state",
                "{\"grant\":\"2025-05-28\",\"trips\":[[\"2025-05-29\",\"2025-08-08\"],[\"2026-05-19\",\"2026-07-31\"]]}").commit();
        Store st = Store.load(c);
        assertEquals(LocalDate.parse("2030-04-02"), st.end());
        assertEquals(2, st.trips.size());
        Updater.base = "";
        android.app.Activity a = org.robolectric.Robolectric.buildActivity(MainActivity.class).setup().get();
        String shown = ((android.widget.TextView) a.findViewById(R.id.btn_end)).getText().toString();
        assertEquals(org.robolectric.RuntimeEnvironment.getApplication().getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar")
                ? "2 أبريل 2030  ·  29 ذو القعدة 1451 هـ" : "2 April 2030  ·  29 Dhu al-Qi'dah 1451 AH", shown);
        assertEquals(LocalDate.parse("2030-04-02"), Calc.compute(st.grant, st.end, st.trips, LocalDate.parse("2026-11-14")).deadline);
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
