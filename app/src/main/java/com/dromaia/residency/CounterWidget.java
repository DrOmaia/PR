package com.dromaia.residency;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.time.LocalDate;

/** The home-screen widget. Tap = open the app (settings). Refreshed after midnight, every hour, and on every edit. */
public class CounterWidget extends AppWidgetProvider {
    static final String ACTION_TICK = "com.dromaia.residency.TICK";

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (ACTION_TICK.equals(a) || Intent.ACTION_TIME_CHANGED.equals(a) || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            refresh(c);
        }
    }

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        refresh(c);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle options) {
        refresh(c); // resized: the layout depends on the height
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
        LocalDate today = Store.today();
        for (int id : ids) {
            int h = m.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
            m.updateAppWidget(id, build(c, st, today, h));
        }
        schedule(c);
    }

    /** heightDp: the widget's height on the home screen (0 = unknown). Short widgets drop the two small labels. */
    static RemoteViews build(Context c, Store st, LocalDate today, int heightDp) {
        Summary s = Summary.of(c, st, today);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        boolean compact = heightDp > 0 && heightDp < 130;
        v.setTextViewTextSize(R.id.remaining, TypedValue.COMPLEX_UNIT_SP, compact ? 30 : heightDp >= 190 ? 52 : 40);
        v.setViewVisibility(R.id.done_label, compact ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.date_label, compact ? View.GONE : View.VISIBLE);
        v.setInt(R.id.card, "setBackgroundResource", s.abroad ? R.drawable.card_paused : R.drawable.card);
        v.setTextViewText(R.id.status, s.status);
        v.setTextColor(R.id.status, c.getColor(s.abroad || s.noGrant ? R.color.amber : R.color.on_card));
        v.setTextViewText(R.id.remaining, s.remaining);
        v.setTextViewText(R.id.done, s.done);
        v.setProgressBar(R.id.progress, Calc.TARGET, s.progress, false);
        v.setTextViewText(R.id.date_label, s.dateLabel);
        v.setTextViewText(R.id.date_g, s.dateG);
        v.setTextViewText(R.id.date_h, s.dateH);
        v.setViewVisibility(R.id.dates, s.noGrant ? View.INVISIBLE : View.VISIBLE);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.card, PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
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
