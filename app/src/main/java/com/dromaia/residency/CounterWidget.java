package com.dromaia.residency;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.time.LocalDate;

/**
 * The home-screen widget. Tap = open the app (settings). Refreshed after midnight, every hour, on resize, after an
 * update and on every edit. Its look (colour, sizes, arrangement, shown items) comes from {@link Look}.
 */
public class CounterWidget extends AppWidgetProvider {
    static final String ACTION_TICK = "com.dromaia.residency.TICK";
    /** Size used when the home screen does not tell the widget's size (dp). */
    static final int DEFAULT_W = 320, DEFAULT_H = 150;

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (ACTION_TICK.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            refresh(c);
        }
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        refresh(c);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) {
        refresh(c); // resized: the sizes depend on the space
    }

    @Override
    public void onDisabled(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(tick(c));
    }

    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, CounterWidget.class));
        if (ids == null || ids.length == 0) return;
        Store st = Store.load(c);
        Look look = Look.load(c);
        LocalDate today = Store.today();
        for (int id : ids) {
            int[] size = size(m, id);
            m.updateAppWidget(id, build(c, st, look, today, size[0], size[1]));
        }
        schedule(c);
    }

    /** The widget's size on the home screen in dp (portrait), or the default when unknown. */
    static int[] size(AppWidgetManager m, int id) {
        Bundle o = m.getAppWidgetOptions(id);
        int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        return new int[]{w > 0 ? w : DEFAULT_W, h > 0 ? h : DEFAULT_H};
    }

    /** The size of the first widget on the home screen, or the default when there is none (for the preview). */
    static int[] firstSize(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, CounterWidget.class));
        return ids != null && ids.length > 0 ? size(m, ids[0]) : new int[]{DEFAULT_W, DEFAULT_H};
    }

    /**
     * The widget for a space of widthDp x heightDp. The big number gets the largest size at which everything still
     * fits (measured, not guessed), times the user's "big number" percentage. If the space is too small even for a
     * small number, the other texts are reduced step by step.
     */
    static RemoteViews build(Context context, Store st, Look look, LocalDate today, int widthDp, int heightDp) {
        Context c = Lang.wrap(context); // the language chosen in the app, not the home screen's
        Summary s = Summary.of(c, st, today);
        float small = look.small / 100f;
        float number = 0;
        for (int i = 0; i < 14; i++) {
            number = largestNumber(c, s, look, small, widthDp, heightDp);
            if (number >= 26) break;
            small *= 0.88f; // too crowded: smaller side texts, try again
        }
        return views(c, s, look, small, Math.max(12, number) * look.big / 100f);
    }

    private static float largestNumber(Context c, Summary s, Look look, float small, int widthDp, int heightDp) {
        View v = views(c, s, look, small, 40).apply(c, new FrameLayout(c));
        TextView number = v.findViewById(R.id.remaining);
        float d = c.getResources().getDisplayMetrics().density;
        // a little smaller than the real space: home screens differ slightly in fonts and padding
        int w = Math.round(widthDp * d * 0.97f), h = Math.round(heightDp * d * 0.97f);
        float lo = 12, hi = 130;
        number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, lo);
        if (!fits(v, w, h)) return 0; // not even a small number fits with these side texts
        for (int i = 0; i < 9; i++) {
            float mid = (lo + hi) / 2;
            number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mid);
            if (fits(v, w, h)) lo = mid; else hi = mid;
        }
        return lo;
    }

    /** Lays the view out in w x h pixels and tells whether every text is fully visible (nothing cut or pushed out). */
    static boolean fits(View v, int w, int h) {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        return inside(v);
    }

    private static boolean inside(View v) {
        if (v.getVisibility() != View.VISIBLE) return true;
        if (v instanceof TextView) {
            if (v.getId() == R.id.title) return true; // the title may be shortened with "…"
            TextView t = (TextView) v;
            Layout l = t.getLayout();
            if (l == null || t.length() == 0) return true;
            return l.getLineCount() == 1 && l.getEllipsisCount(0) == 0
                    && Math.ceil(l.getLineWidth(0)) <= t.getWidth() - t.getPaddingLeft() - t.getPaddingRight()
                    && l.getHeight() <= t.getHeight() - t.getPaddingTop() - t.getPaddingBottom();
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View child = g.getChildAt(i);
                if (child.getVisibility() != View.VISIBLE) continue;
                if (child.getLeft() < g.getPaddingLeft() || child.getTop() < g.getPaddingTop()
                        || child.getRight() > g.getWidth() - g.getPaddingRight()
                        || child.getBottom() > g.getHeight() - g.getPaddingBottom()) return false;
                if (!inside(child)) return false;
            }
        }
        return true;
    }

    /** One RemoteViews with the texts, colours, sizes and visibility. small = scale of the texts other than the big number. */
    private static RemoteViews views(Context c, Summary s, Look k, float small, float numberDp) {
        int layout = k.layout == 1 ? R.layout.widget_center : k.layout == 2 ? R.layout.widget_dates_top : R.layout.widget;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);
        boolean rtl = Lang.rtl(c);
        v.setInt(R.id.card, "setLayoutDirection", rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        v.setInt(R.id.row_number, "setLayoutDirection", rtl != k.swap ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);

        // colours: any card colour; white or dark text by how light it is
        int card = (s.abroad ? Look.PAUSED_COLOR : k.color) | 0xFF000000;
        boolean light = Look.light(card);
        int on = light ? 0xFF1E2A25 : 0xFFFFFFFF;
        int soft = light ? 0xB31E2A25 : 0xD9FFFFFF;
        int warn = light ? 0xFFA85B00 : 0xFFFFE08A;
        v.setInt(R.id.bg, "setColorFilter", card);
        v.setInt(R.id.bg, "setImageAlpha", k.alpha * 255 / 100);
        v.setInt(R.id.shade, "setImageAlpha", k.alpha * 255 / 100);
        v.setTextColor(R.id.title, soft);
        v.setTextColor(R.id.status, s.abroad || s.noGrant ? warn : on);
        v.setTextColor(R.id.remaining, on);
        v.setTextColor(R.id.left_label, soft);
        v.setTextColor(R.id.done, on);
        v.setTextColor(R.id.done_label, soft);
        v.setTextColor(R.id.date_label, soft);
        v.setTextColor(R.id.date_g, on);
        v.setTextColor(R.id.date_h, on);

        // texts
        v.setTextViewText(R.id.title, c.getString(R.string.app_title));
        v.setTextViewText(R.id.status, s.status);
        v.setTextViewText(R.id.remaining, s.remaining);
        v.setTextViewText(R.id.left_label, c.getString(R.string.days_left));
        v.setTextViewText(R.id.done, s.done);
        v.setTextViewText(R.id.done_label, c.getString(R.string.days_done));
        v.setTextViewText(R.id.date_label, s.dateLabel);
        v.setTextViewText(R.id.date_g, s.dateG);
        v.setTextViewText(R.id.date_h, s.dateH);
        int bar = light ? R.id.progress_dark : R.id.progress;
        v.setProgressBar(bar, Calc.TARGET, s.progress, false);
        v.setViewVisibility(R.id.progress, light ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.progress_dark, light ? View.VISIBLE : View.GONE);

        // sizes
        v.setTextViewTextSize(R.id.remaining, TypedValue.COMPLEX_UNIT_DIP, numberDp);
        v.setTextViewTextSize(R.id.left_label, TypedValue.COMPLEX_UNIT_SP, 15 * small);
        v.setTextViewTextSize(R.id.done, TypedValue.COMPLEX_UNIT_SP, 22 * small);
        v.setTextViewTextSize(R.id.done_label, TypedValue.COMPLEX_UNIT_SP, 12 * small);
        v.setTextViewTextSize(R.id.date_label, TypedValue.COMPLEX_UNIT_SP, 12 * small);
        v.setTextViewTextSize(R.id.date_g, TypedValue.COMPLEX_UNIT_SP, 17 * small);
        v.setTextViewTextSize(R.id.date_h, TypedValue.COMPLEX_UNIT_SP, 17 * small);

        // shown items (the top line stays while the residency date is missing: it says what to do)
        v.setViewVisibility(R.id.row_top, k.title || s.noGrant ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.done_box, k.done ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.done_label, k.labels ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.bar_box, k.bar ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.dates, (k.greg || k.hijri) && !s.noGrant ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.date_label, k.labels ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.date_g, k.greg ? View.VISIBLE : View.GONE);
        v.setViewVisibility(R.id.date_h, k.hijri ? View.VISIBLE : View.GONE);

        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.card, PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }

    /**
     * The widget drawn inside the app with the same code as on the home screen. heightDp 0 = the size of the
     * widget on the home screen (exact preview); otherwise the full width of host and that height.
     */
    static View preview(Context c, FrameLayout host, Store st, Look look, int heightDp) {
        float d = c.getResources().getDisplayMetrics().density;
        int room = Math.round((host.getWidth() > 0 ? host.getWidth()
                : c.getResources().getDisplayMetrics().widthPixels - 32 * d) / d);
        int[] size = heightDp > 0 ? new int[]{room, heightDp} : firstSize(c);
        int wDp = Math.min(size[0], room);
        View v = build(c, st, look, Store.today(), wDp, size[1]).apply(c, host);
        v.findViewById(R.id.card).setOnClickListener(null);
        v.findViewById(R.id.card).setClickable(false);
        host.removeAllViews();
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(Math.round(wDp * d), Math.round(size[1] * d));
        p.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        host.addView(v, p);
        return v;
    }

    private static PendingIntent tick(Context c) {
        Intent i = new Intent(c, CounterWidget.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Next refresh just after midnight (Riyadh time), when the day number changes. */
    private static void schedule(Context c) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        long at = Store.today().plusDays(1).atStartOfDay(Store.RIYADH).toInstant().toEpochMilli() + 2000;
        am.set(AlarmManager.RTC, at, tick(c));
    }
}
