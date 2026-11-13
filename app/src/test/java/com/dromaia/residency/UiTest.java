package com.dromaia.residency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;

/** Opens the real screens off-device (Robolectric), checks the texts and saves pictures in build/screens. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "ar-rSA-w411dp-h891dp-xxhdpi")
public class UiTest {
    private final Context c = RuntimeEnvironment.getApplication();

    private Store store(String grant) {
        Store st = Store.load(c);
        st.grant = grant == null ? null : LocalDate.parse(grant);
        return st;
    }

    private View widget(Store st, String today, int wDp, int hDp, String file) throws Exception {
        FrameLayout host = new FrameLayout(c);
        View v = CounterWidget.build(c, st, LocalDate.parse(today), hDp).apply(c, host);
        host.addView(v);
        shoot(host, wDp, hDp, file);
        return v;
    }

    private void shoot(View v, int wDp, int hDp, String file) throws Exception {
        float d = c.getResources().getDisplayMetrics().density;
        int w = Math.round(wDp * d), h = Math.round(hDp * d);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawColor(Color.rgb(40, 60, 90));
        v.draw(canvas);
        File dir = new File("build/screens");
        dir.mkdirs();
        try (FileOutputStream o = new FileOutputStream(new File(dir, file))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
        }
    }

    private static String text(View root, int id) {
        return ((TextView) root.findViewById(id)).getText().toString();
    }

    @Test
    public void firstInstall_startsWithTheTravelRecord() {
        Store st = Store.load(c);
        assertEquals(5, st.trips.size());
        assertNull(st.openTrip());
    }

    @Test
    public void saveAndLoad_keepTheDates() {
        Store st = store("2025-02-01");
        st.trips.add(new Calc.Trip(LocalDate.parse("2026-11-01"), null));
        st.save(c);
        Store back = Store.load(c);
        assertEquals(LocalDate.parse("2025-02-01"), back.grant);
        assertEquals(6, back.trips.size());
        assertNotNull(back.openTrip());
        assertEquals(LocalDate.parse("2026-11-01"), back.openTrip().exit);
    }

    @Test
    public void widget_inside() throws Exception {
        Store st = store("2025-02-01");
        Calc.Result r = Calc.compute(st.grant, st.trips, LocalDate.parse("2026-11-13"));
        View v = widget(st, "2026-11-13", 320, 150, "widget_inside.png");
        assertEquals(String.valueOf(r.remaining), text(v, R.id.remaining));
        assertEquals(r.done + " / 900", text(v, R.id.done));
        assertEquals(c.getString(R.string.st_inside), text(v, R.id.status));
        assertTrue(text(v, R.id.date_h).endsWith("هـ"));
        View small = widget(st, "2026-11-13", 250, 110, "widget_inside_small.png");
        assertEquals(View.GONE, small.findViewById(R.id.date_label).getVisibility());
        widget(st, "2026-11-13", 360, 200, "widget_inside_large.png");
    }

    @Test
    public void widget_abroad_isFrozen() throws Exception {
        Store st = store("2025-02-01");
        st.trips.add(new Calc.Trip(LocalDate.parse("2026-11-10"), null));
        View a = widget(st, "2026-11-13", 320, 150, "widget_abroad.png");
        View b = widget(st, "2026-11-20", 320, 150, "widget_abroad_later.png");
        assertEquals(c.getString(R.string.st_abroad), text(a, R.id.status));
        assertEquals(text(a, R.id.remaining), text(b, R.id.remaining));
        assertTrue(!text(a, R.id.date_g).equals(text(b, R.id.date_g)));
    }

    @Test
    public void widget_withoutGrantDate() throws Exception {
        View v = widget(store(null), "2026-11-13", 320, 150, "widget_nogrant.png");
        assertEquals(c.getString(R.string.st_nogrant), text(v, R.id.status));
    }

    @Test
    public void hijriDate_isUmmAlQura() {
        assertEquals("1 رمضان 1447 هـ", Summary.hijri(c, LocalDate.parse("2026-02-18")));
        assertEquals("13 نوفمبر 2026", Summary.greg(c, LocalDate.parse("2026-11-13")));
    }

    @Test
    public void app_opensAndListsTheTrips() throws Exception {
        Store st = store("2025-02-01");
        st.save(c);
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        LinearLayout list = a.findViewById(R.id.trips);
        assertEquals(5, list.getChildCount());
        assertEquals(c.getString(R.string.act_exit), text(a.getWindow().getDecorView(), R.id.btn_toggle));
        shoot(a.findViewById(android.R.id.content), 411, 891, "app.png");
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
    public void english() throws Exception {
        Store st = store("2025-02-01");
        st.trips.add(new Calc.Trip(LocalDate.parse("2026-11-10"), null));
        st.save(c);
        widget(st, "2026-11-13", 320, 150, "widget_en.png");
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertEquals(c.getString(R.string.act_return), text(a.getWindow().getDecorView(), R.id.btn_toggle));
        shoot(a.findViewById(android.R.id.content), 411, 891, "app_en.png");
    }
}
