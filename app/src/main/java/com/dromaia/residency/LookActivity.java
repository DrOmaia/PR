package com.dromaia.residency;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.function.IntConsumer;

/** "Widget appearance": colour, sizes, arrangement and shown items, with a live preview of the real widget. */
public class LookActivity extends Activity {
    private static final int[] LAYOUT_IDS = {R.id.lk_l0, R.id.lk_l1, R.id.lk_l2};
    private Look look;
    private boolean filling; // true while the controls are being set from the saved look

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setLayoutDirection(
                Lang.rtl(this) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        setContentView(R.layout.activity_look);
        look = Look.load(this);
        findViewById(R.id.lk_back).setOnClickListener(v -> finish());
        findViewById(R.id.lk_reset).setOnClickListener(v -> {
            look = new Look();
            fill();
            changed();
        });

        seek(R.id.lk_hue, v -> look.color = Color.HSVToColor(new float[]{v, 0.5f, 0.6f}));
        seek(R.id.lk_alpha, v -> look.alpha = v);
        seek(R.id.lk_big, v -> look.big = v);
        seek(R.id.lk_small, v -> look.small = v);
        ((RadioGroup) findViewById(R.id.lk_layout)).setOnCheckedChangeListener((g, id) -> {
            if (filling) return;
            for (int i = 0; i < LAYOUT_IDS.length; i++) if (LAYOUT_IDS[i] == id) look.layout = i;
            changed();
        });
        check(R.id.lk_swap, on -> look.swap = on);
        check(R.id.lk_s_title, on -> look.title = on);
        check(R.id.lk_s_status, on -> look.status = on);
        check(R.id.lk_s_number, on -> look.number = on);
        check(R.id.lk_s_left, on -> look.leftLabel = on);
        check(R.id.lk_s_done, on -> look.done = on);
        check(R.id.lk_s_done_label, on -> look.doneLabel = on);
        check(R.id.lk_s_bar, on -> look.bar = on);
        check(R.id.lk_s_date_label, on -> look.dateLabel = on);
        check(R.id.lk_s_greg, on -> look.greg = on);
        check(R.id.lk_s_hijri, on -> look.hijri = on);
        fill();
        show();
    }

    private interface Flag {
        void set(boolean on);
    }

    private void seek(int id, IntConsumer set) {
        ((SeekBar) findViewById(id)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                if (!fromUser || filling) return;
                set.accept(value);
                changed();
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {}

            @Override
            public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void check(int id, Flag set) {
        ((CheckBox) findViewById(id)).setOnCheckedChangeListener((v, on) -> {
            if (filling) return;
            set.set(on);
            changed();
        });
    }

    /** Sets every control from the look. */
    private void fill() {
        filling = true;
        float[] hsv = new float[3];
        Color.colorToHSV(look.color, hsv);
        ((SeekBar) findViewById(R.id.lk_hue)).setProgress(Math.round(hsv[0]));
        ((SeekBar) findViewById(R.id.lk_alpha)).setProgress(look.alpha);
        ((SeekBar) findViewById(R.id.lk_big)).setProgress(look.big);
        ((SeekBar) findViewById(R.id.lk_small)).setProgress(look.small);
        ((RadioGroup) findViewById(R.id.lk_layout)).check(LAYOUT_IDS[look.layout]);
        ((CheckBox) findViewById(R.id.lk_swap)).setChecked(look.swap);
        ((CheckBox) findViewById(R.id.lk_s_title)).setChecked(look.title);
        ((CheckBox) findViewById(R.id.lk_s_status)).setChecked(look.status);
        ((CheckBox) findViewById(R.id.lk_s_number)).setChecked(look.number);
        ((CheckBox) findViewById(R.id.lk_s_left)).setChecked(look.leftLabel);
        ((CheckBox) findViewById(R.id.lk_s_done)).setChecked(look.done);
        ((CheckBox) findViewById(R.id.lk_s_done_label)).setChecked(look.doneLabel);
        ((CheckBox) findViewById(R.id.lk_s_bar)).setChecked(look.bar);
        ((CheckBox) findViewById(R.id.lk_s_date_label)).setChecked(look.dateLabel);
        ((CheckBox) findViewById(R.id.lk_s_greg)).setChecked(look.greg);
        ((CheckBox) findViewById(R.id.lk_s_hijri)).setChecked(look.hijri);
        filling = false;
    }

    private void changed() {
        look.save(this);
        fill(); // e.g. the big number comes back when everything was switched off
        show();
        CounterWidget.refresh(this);
    }

    /** Preview, colour circles and the percentages next to the sliders. */
    private void show() {
        CounterWidget.preview(this, findViewById(R.id.lk_preview), Store.load(this), look, 0); // exact home-screen size
        ((TextView) findViewById(R.id.lk_alpha_t)).setText(getString(R.string.lk_alpha, Summary.num(look.alpha)));
        ((TextView) findViewById(R.id.lk_big_t)).setText(getString(R.string.lk_big, Summary.num(look.big)));
        ((TextView) findViewById(R.id.lk_small_t)).setText(getString(R.string.lk_small, Summary.num(look.small)));

        LinearLayout row = findViewById(R.id.lk_colors);
        row.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        for (int color : Look.PRESETS) {
            View dot = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(color);
            boolean chosen = color == look.color;
            g.setStroke(Math.round((chosen ? 4 : 1) * d), getColor(chosen ? R.color.text : R.color.line));
            dot.setBackground(g);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Math.round(46 * d), Math.round(46 * d));
            p.setMarginEnd(Math.round(10 * d));
            dot.setOnClickListener(v -> {
                look.color = color;
                fill();
                changed();
            });
            row.addView(dot, p);
        }
    }
}
