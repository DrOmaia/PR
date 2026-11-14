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

import org.junit.Before;
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

    @Before
    public void noNetwork() {
        Updater.base = ""; // no update checks in these tests
    }

    /** The same example record in every build variant. */
    private Store store(String grant) {
        Store st = new Store();
        st.grant = grant == null ? null : LocalDate.parse(grant);
        String[][] trips = {{"2025-05-29", "2025-08-08"}, {"2025-11-11", "2025-11-15"}, {"2026-03-07", "2026-03-25"},
                {"2026-03-25", "2026-03-25"}, {"2026-05-19", "2026-07-31"}};
        for (String[] t : trips) st.trips.add(new Calc.Trip(LocalDate.parse(t[0]), LocalDate.parse(t[1])));
        return st;
    }

    private View widget(Store st, String today, int wDp, int hDp, String file) throws Exception {
        FrameLayout host = new FrameLayout(c);
        View v = CounterWidget.build(c, st, new Look(), LocalDate.parse(today), wDp, hDp).apply(c, host);
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
        canvas.drawColor(Color.rgb(245, 247, 246));
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
        widget(st, "2026-11-13", 250, 110, "widget_inside_small.png");
        widget(st, "2026-11-13", 360, 200, "widget_inside_large.png");
        widget(st, "2026-11-13", 360, 170, "widget_inside_mid.png");
        widget(st, "2026-11-13", 300, 130, "widget_inside_130.png");
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
    public void expiryDate_setByHand_isKept() {
        Store st = store("2025-02-01");
        LocalDate worked = Calc.defaultEnd(LocalDate.parse("2025-02-01"));
        assertEquals(worked, st.end());
        st.end = worked.plusDays(9);
        st.save(c);
        assertEquals(worked.plusDays(9), Store.load(c).end());
        st.end = null;
        st.save(c);
        assertEquals(worked, Store.load(c).end());
    }

    @Test
    public void app_opensAndListsTheTrips() throws Exception {
        Store st = store("2025-02-01");
        st.save(c);
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        LinearLayout list = a.findViewById(R.id.trips);
        assertEquals(5, list.getChildCount());
        assertEquals(c.getString(R.string.act_exit), text(a.getWindow().getDecorView(), R.id.btn_toggle));
        assertEquals("الإصدار " + BuildConfig.VERSION_NAME, text(a.getWindow().getDecorView(), R.id.version));
        assertEquals("1 فبراير 2025  ·  2 شعبان 1446 هـ", text(a.getWindow().getDecorView(), R.id.btn_grant));
        shoot(a.findViewById(android.R.id.content), 411, 1100, "app.png");
    }

    @Test
    public void languageButton_switchesTheAppAndTheWidget() throws Exception {
        Store st = store("2025-05-28");
        st.save(c);
        Activity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertEquals("English", text(a.getWindow().getDecorView(), R.id.btn_lang));
        a.findViewById(R.id.btn_lang).performClick();
        assertEquals("en", Lang.get(c));

        Activity b = Robolectric.buildActivity(MainActivity.class).setup().get();
        assertEquals("العربية", text(b.getWindow().getDecorView(), R.id.btn_lang));
        assertEquals("Record exit from Saudi Arabia", text(b.getWindow().getDecorView(), R.id.btn_toggle));
        assertEquals(View.LAYOUT_DIRECTION_LTR, b.getWindow().getDecorView().getLayoutDirection());

        // the phone is Arabic, the app was switched to English: the widget follows the app
        View w = widget(st, "2026-11-13", 320, 150, "widget_en_on_arabic_phone.png");
        assertEquals("● In Saudi Arabia", text(w, R.id.status));
        assertEquals("days left", text(w, R.id.left_label));
        assertEquals(View.LAYOUT_DIRECTION_LTR, w.findViewById(R.id.card).getLayoutDirection());
        assertTrue(text(w, R.id.date_h).endsWith("AH"));

        b.findViewById(R.id.btn_lang).performClick();
        assertEquals("ar", Lang.get(c));
        View w2 = widget(st, "2026-11-13", 320, 150, "widget_owner.png");
        assertEquals(View.LAYOUT_DIRECTION_RTL, w2.findViewById(R.id.card).getLayoutDirection());
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
        shoot(a.findViewById(android.R.id.content), 411, 1200, "app_en.png");
    }
}
