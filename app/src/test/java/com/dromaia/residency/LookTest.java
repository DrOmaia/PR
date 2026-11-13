package com.dromaia.residency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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

/** "Widget appearance": every combination must keep all texts fully visible, and each setting must do its job. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "ar-rSA-w411dp-h891dp-xxhdpi")
public class LookTest {
    private final Context c = RuntimeEnvironment.getApplication();
    private final LocalDate today = LocalDate.parse("2026-11-13");

    @Before
    public void noNetwork() {
        Updater.base = "";
    }

    private Store store() {
        Store st = new Store();
        st.grant = LocalDate.parse("2025-05-28");
        String[][] trips = {{"2025-05-29", "2025-08-08"}, {"2025-11-11", "2025-11-15"}, {"2026-03-07", "2026-03-25"},
                {"2026-05-19", "2026-07-31"}};
        for (String[] t : trips) st.trips.add(new Calc.Trip(LocalDate.parse(t[0]), LocalDate.parse(t[1])));
        return st;
    }

    private int px(int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    private View widget(Store st, Look k, int wDp, int hDp, String file) throws Exception {
        FrameLayout host = new FrameLayout(c);
        View v = CounterWidget.build(c, st, k, today, wDp, hDp).apply(c, host);
        boolean fits = CounterWidget.fits(v, px(wDp), px(hDp));
        if (file != null) {
            Bitmap b = Bitmap.createBitmap(px(wDp), px(hDp), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            canvas.drawColor(Color.rgb(120, 140, 170));
            v.draw(canvas);
            File dir = new File("build/screens");
            dir.mkdirs();
            try (FileOutputStream o = new FileOutputStream(new File(dir, file))) {
                b.compress(Bitmap.CompressFormat.PNG, 100, o);
            }
        }
        assertTrue("texts cut at " + wDp + "x" + hDp + " layout " + k.layout + " small " + k.small + " big " + k.big, fits);
        return v;
    }

    private static float sizeDp(View v, int id) {
        TextView t = v.findViewById(id);
        return t.getTextSize() / t.getResources().getDisplayMetrics().density;
    }

    @Test
    public void everyCombination_keepsAllTextsVisible() throws Exception {
        int[][] sizes = {{250, 110}, {280, 120}, {300, 130}, {320, 150}, {360, 170}, {360, 200}, {400, 230}, {180, 180}};
        Store st = store();
        Store abroad = store();
        abroad.trips.add(new Calc.Trip(LocalDate.parse("2026-11-10"), null));
        int n = 0;
        for (String lang : new String[]{"ar", "en"}) {
            Lang.set(c, lang);
            for (int[] s : sizes) {
                for (int layout = 0; layout <= 2; layout++) {
                    for (int small : new int[]{70, 100, 150}) {
                        Look k = new Look();
                        k.layout = layout;
                        k.small = small;
                        k.swap = layout == 2;
                        widget(n % 5 == 0 ? abroad : st, k, s[0], s[1], null);
                        n++;
                    }
                }
            }
        }
        assertEquals(2 * 8 * 3 * 3, n);
    }

    @Test
    public void bigNumber_fillsTheSpace_andThePercentageShrinksIt() throws Exception {
        Look k = new Look();
        float full = sizeDp(widget(store(), k, 360, 170, null), R.id.remaining);
        float taller = sizeDp(widget(store(), k, 360, 220, null), R.id.remaining);
        assertTrue("a taller widget gets a bigger number", taller > full + 8);
        k.big = 50;
        assertEquals(full / 2, sizeDp(widget(store(), k, 360, 170, null), R.id.remaining), 0.6f);
    }

    @Test
    public void otherTexts_followTheirPercentage_andHidingItemsFreesRoomForTheNumber() throws Exception {
        Look k = new Look();
        View normal = widget(store(), k, 400, 230, null);
        k.small = 150;
        View bigger = widget(store(), k, 400, 230, "look_small150.png");
        assertEquals(sizeDp(normal, R.id.date_g) * 1.5f, sizeDp(bigger, R.id.date_g), 0.6f);
        assertTrue(sizeDp(bigger, R.id.remaining) < sizeDp(normal, R.id.remaining));

        Look bare = new Look();
        bare.title = bare.status = bare.leftLabel = bare.done = bare.bar = bare.greg = bare.hijri = false;
        normal = widget(store(), new Look(), 360, 170, null);
        View only = widget(store(), bare, 360, 170, "look_number_only.png");
        assertEquals(View.GONE, only.findViewById(R.id.row_top).getVisibility());
        assertEquals(View.GONE, only.findViewById(R.id.done_box).getVisibility());
        assertEquals(View.GONE, only.findViewById(R.id.bar_box).getVisibility());
        assertEquals(View.GONE, only.findViewById(R.id.dates).getVisibility());
        assertTrue(sizeDp(only, R.id.remaining) > sizeDp(normal, R.id.remaining) + 15);
    }

    @Test
    public void smallestWidgets_showTheChosenItemsInFull() throws Exception {
        Look number = new Look();
        number.title = number.status = number.leftLabel = number.done = number.bar = number.greg = number.hijri = false;
        View oneCell = widget(store(), number, 57, 57, "tiny_1x1_number.png");
        assertTrue(sizeDp(oneCell, R.id.remaining) > 20);
        View twoByOne = widget(store(), number, 130, 57, "tiny_2x1_number.png");
        assertTrue(sizeDp(twoByOne, R.id.remaining) > 38);

        Look withLabel = new Look();
        withLabel.title = withLabel.status = withLabel.done = withLabel.bar = withLabel.greg = withLabel.hijri = false;
        widget(store(), withLabel, 150, 57, "tiny_2x1_number_label.png");

        Look numberAndDate = new Look();
        numberAndDate.title = numberAndDate.status = numberAndDate.done = numberAndDate.bar = numberAndDate.hijri = numberAndDate.dateLabel = false;
        widget(store(), numberAndDate, 200, 80, "tiny_3x1_number_date.png");

        // big number hidden: what stays grows to fill the space
        Look dateOnly = new Look();
        dateOnly.title = dateOnly.status = dateOnly.number = dateOnly.leftLabel = dateOnly.done = dateOnly.bar = dateOnly.hijri = dateOnly.dateLabel = false;
        View date = widget(store(), dateOnly, 200, 57, "tiny_3x1_date.png");
        assertEquals(View.GONE, date.findViewById(R.id.row_number).getVisibility());
        assertTrue("the date fills the widget", sizeDp(date, R.id.date_g) > 20);
        dateOnly.small = 70;
        assertEquals(sizeDp(date, R.id.date_g) * 0.7f, sizeDp(widget(store(), dateOnly, 200, 57, null), R.id.date_g), 0.6f);

        // every tiny size still shows whatever is switched on without cutting it, in the three arrangements
        int[][] sizes = {{57, 57}, {130, 57}, {200, 57}, {130, 130}, {57, 130}, {270, 57}};
        Look[] looks = {new Look(), number, withLabel, numberAndDate, dateOnly};
        for (String lang : new String[]{"ar", "en"}) {
            Lang.set(c, lang);
            for (int[] size : sizes) {
                for (Look k : looks) {
                    for (int layout = 0; layout <= 2; layout++) {
                        k.layout = layout;
                        widget(store(), k, size[0], size[1], null);
                    }
                }
            }
        }
    }

    @Test
    public void everythingSwitchedOff_bringsTheNumberBack_andOldSettingsStillOpen() {
        Look k = new Look();
        k.title = k.status = k.number = k.leftLabel = k.done = k.bar = k.greg = k.hijri = false;
        k.save(c);
        assertTrue(Look.load(c).number);

        // a look saved by 1.5: "title" meant title + status, "labels" meant the two small labels
        c.getSharedPreferences("look", Context.MODE_PRIVATE).edit().putString("look",
                "{\"color\":-13926563,\"alpha\":100,\"layout\":1,\"swap\":false,\"big\":100,\"small\":100,"
                        + "\"title\":false,\"done\":true,\"bar\":true,\"greg\":true,\"hijri\":false,\"labels\":false}").commit();
        Look old = Look.load(c);
        assertFalse(old.title);
        assertFalse(old.status);
        assertFalse(old.doneLabel);
        assertFalse(old.dateLabel);
        assertTrue(old.number);
        assertTrue(old.leftLabel);
        assertFalse(old.hijri);
        assertEquals(1, old.layout);
    }

    @Test
    public void colours_lightCardGetsDarkText_abroadIsGrey() throws Exception {
        Look k = new Look();
        k.color = 0xFF2F6DB5;
        View blue = widget(store(), k, 360, 170, "look_blue.png");
        assertEquals(Color.WHITE, ((TextView) blue.findViewById(R.id.remaining)).getCurrentTextColor());
        assertEquals(View.VISIBLE, blue.findViewById(R.id.progress).getVisibility());

        k.color = 0xFFFFFFFF;
        k.alpha = 80;
        View white = widget(store(), k, 360, 170, "look_white.png");
        assertEquals(0xFF1E2A25, ((TextView) white.findViewById(R.id.remaining)).getCurrentTextColor());
        assertEquals(View.VISIBLE, white.findViewById(R.id.progress_dark).getVisibility());
        assertEquals(View.GONE, white.findViewById(R.id.progress).getVisibility());

        Store abroad = store();
        abroad.trips.add(new Calc.Trip(LocalDate.parse("2026-11-10"), null));
        View grey = widget(abroad, k, 360, 170, "look_abroad.png");
        assertEquals(Color.WHITE, ((TextView) grey.findViewById(R.id.remaining)).getCurrentTextColor());
    }

    @Test
    public void arrangements_andSwap() throws Exception {
        Look k = new Look();
        View side = widget(store(), k, 360, 170, "look_layout0.png");
        TextView number = side.findViewById(R.id.remaining);
        View done = side.findViewById(R.id.done_box);
        assertTrue("Arabic: number on the right, days done on the left", number.getLeft() > done.getLeft());
        k.swap = true;
        View swapped = widget(store(), k, 360, 170, "look_layout0_swapped.png");
        assertTrue(swapped.findViewById(R.id.remaining).getLeft() < swapped.findViewById(R.id.done_box).getLeft());

        k = new Look();
        k.layout = 1;
        View centre = widget(store(), k, 360, 190, "look_layout1.png");
        View row = centre.findViewById(R.id.row_number);
        View num = centre.findViewById(R.id.remaining), label = centre.findViewById(R.id.left_label);
        int left = Math.min(num.getLeft(), label.getLeft()), right = Math.max(num.getRight(), label.getRight());
        assertEquals("number + label centred", left, row.getWidth() - right, 3);

        k.layout = 2;
        View top = widget(store(), k, 360, 170, "look_layout2.png");
        assertTrue("dates above the number", top.findViewById(R.id.dates).getBottom() <= top.findViewById(R.id.row_number).getTop());
    }

    @Test
    public void look_isSavedAndLoaded() {
        Look k = new Look();
        k.color = 0xFF6B4FA3;
        k.alpha = 60;
        k.layout = 2;
        k.swap = true;
        k.big = 70;
        k.small = 130;
        k.hijri = false;
        k.save(c);
        Look b = Look.load(c);
        assertEquals(0xFF6B4FA3, b.color);
        assertEquals(60, b.alpha);
        assertEquals(2, b.layout);
        assertTrue(b.swap);
        assertEquals(70, b.big);
        assertEquals(130, b.small);
        assertFalse(b.hijri);
        assertTrue(b.greg);
    }

    @Test
    public void appearanceScreen_changesTheLookAtOnce() throws Exception {
        store().save(c);
        Activity a = Robolectric.buildActivity(LookActivity.class).setup().get();
        FrameLayout preview = a.findViewById(R.id.lk_preview);
        assertEquals(1, preview.getChildCount());

        LinearLayout colours = a.findViewById(R.id.lk_colors);
        assertEquals(Look.PRESETS.length, colours.getChildCount());
        colours.getChildAt(2).performClick();
        assertEquals(Look.PRESETS[2], Look.load(c).color);

        ((RadioGroup) a.findViewById(R.id.lk_layout)).check(R.id.lk_l1);
        assertEquals(1, Look.load(c).layout);
        ((CheckBox) a.findViewById(R.id.lk_s_hijri)).setChecked(false);
        assertFalse(Look.load(c).hijri);
        assertEquals(View.GONE, preview.findViewById(R.id.date_h).getVisibility());

        View root = a.findViewById(android.R.id.content);
        int w = px(411), h = px(1500);
        root.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawColor(Color.rgb(245, 247, 246));
        root.draw(canvas);
        new File("build/screens").mkdirs();
        try (FileOutputStream o = new FileOutputStream("build/screens/look_screen.png")) {
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
        }

        a.findViewById(R.id.lk_reset).performClick();
        Look k = Look.load(c);
        assertEquals(Look.DEFAULT_COLOR, k.color);
        assertEquals(0, k.layout);
        assertTrue(k.hijri);
        assertNotEquals(View.GONE, preview.findViewById(R.id.date_h).getVisibility());
    }
}
