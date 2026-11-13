package com.dromaia.residency;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.function.Consumer;

/** The settings screen (opened from the launcher or by tapping the widget). */
public class MainActivity extends Activity {
    private Store st;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_toggle).setOnClickListener(v -> toggle());
        findViewById(R.id.btn_grant).setOnClickListener(v ->
                pickDate(st.grant != null ? st.grant : Store.today(), null, d -> {
                    st.grant = d;
                    changed();
                }));
        findViewById(R.id.btn_add).setOnClickListener(v -> editTrip(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        st = Store.load(this);
        render();
        CounterWidget.refresh(this);
    }

    private void changed() {
        st.save(this);
        render();
        CounterWidget.refresh(this);
    }

    /** Exit now (the counter freezes) or return now (it runs again). */
    private void toggle() {
        Calc.Trip open = st.openTrip();
        if (open != null) {
            LocalDate today = Store.today();
            pickDate(today.isBefore(open.exit) ? open.exit : today, open.exit, d -> {
                open.entry = d;
                changed();
            });
        } else {
            pickDate(Store.today(), null, d -> {
                st.trips.add(new Calc.Trip(d, null));
                changed();
            });
        }
    }

    private void render() {
        LocalDate today = Store.today();
        Summary s = Summary.of(this, st, today);
        findViewById(R.id.card).setBackgroundResource(s.abroad ? R.drawable.card_paused : R.drawable.card);
        TextView status = findViewById(R.id.status);
        status.setText(s.status);
        status.setTextColor(getColor(s.abroad || s.noGrant ? R.color.amber : R.color.on_card));
        text(R.id.remaining, s.remaining);
        text(R.id.done, s.done);
        ((ProgressBar) findViewById(R.id.progress)).setProgress(s.progress);
        text(R.id.date_label, s.dateLabel);
        text(R.id.date_g, s.dateG);
        text(R.id.date_h, s.dateH);
        findViewById(R.id.dates).setVisibility(s.noGrant ? View.INVISIBLE : View.VISIBLE);

        TextView warning = findViewById(R.id.warning);
        warning.setVisibility(s.warning == null ? View.GONE : View.VISIBLE);
        warning.setText(s.warning);

        ((Button) findViewById(R.id.btn_toggle)).setText(st.openTrip() != null ? R.string.act_return : R.string.act_exit);
        text(R.id.btn_grant, st.grant == null ? getString(R.string.choose_date)
                : Summary.greg(this, st.grant) + "  ·  " + Summary.hijri(this, st.grant));
        TextView deadline = findViewById(R.id.deadline);
        deadline.setVisibility(st.grant == null ? View.GONE : View.VISIBLE);
        if (st.grant != null) {
            deadline.setText(getString(R.string.deadline, Summary.greg(this, st.grant.plusYears(Calc.WINDOW_YEARS))));
        }

        LinearLayout list = findViewById(R.id.trips);
        list.removeAllViews();
        for (int i = st.trips.size() - 1; i >= 0; i--) { // latest first
            Calc.Trip t = st.trips.get(i);
            View row = getLayoutInflater().inflate(R.layout.row_trip, list, false);
            ((TextView) row.findViewById(R.id.row_exit)).setText(getString(R.string.row_exit, Summary.greg(this, t.exit)));
            ((TextView) row.findViewById(R.id.row_entry)).setText(getString(R.string.row_entry,
                    t.entry == null ? getString(R.string.not_returned) : Summary.greg(this, t.entry)));
            ((TextView) row.findViewById(R.id.row_days)).setText(getString(R.string.trip_days, Summary.num(t.daysOut(today))));
            row.setOnClickListener(v -> editTrip(t));
            list.addView(row);
        }
        if (st.trips.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(R.string.no_trips);
            none.setTextColor(getColor(R.color.muted));
            list.addView(none);
        }
    }

    private void text(int id, CharSequence s) {
        ((TextView) findViewById(id)).setText(s);
    }

    /** Add (trip == null) or edit a trip: exit date, return date (may stay empty), delete. */
    private void editTrip(Calc.Trip trip) {
        View v = getLayoutInflater().inflate(R.layout.dialog_trip, null);
        Button exit = v.findViewById(R.id.dlg_exit);
        Button entry = v.findViewById(R.id.dlg_entry);
        Button clear = v.findViewById(R.id.dlg_clear);
        LocalDate[] d = {trip != null ? trip.exit : Store.today(), trip != null ? trip.entry : null};
        Runnable show = () -> {
            exit.setText(Summary.greg(this, d[0]));
            entry.setText(d[1] == null ? getString(R.string.not_returned) : Summary.greg(this, d[1]));
            clear.setVisibility(d[1] == null ? View.GONE : View.VISIBLE);
        };
        show.run();
        exit.setOnClickListener(x -> pickDate(d[0], null, n -> {
            d[0] = n;
            if (d[1] != null && d[1].isBefore(n)) d[1] = null; // a return is never before the exit
            show.run();
        }));
        entry.setOnClickListener(x -> pickDate(d[1] != null ? d[1] : d[0], d[0], n -> {
            d[1] = n;
            show.run();
        }));
        clear.setOnClickListener(x -> {
            d[1] = null;
            show.run();
        });

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(R.string.trip_title)
                .setView(v)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dlg, w) -> {
                    if (trip == null) {
                        st.trips.add(new Calc.Trip(d[0], d[1]));
                    } else {
                        trip.exit = d[0];
                        trip.entry = d[1];
                    }
                    changed();
                });
        if (trip != null) {
            b.setNeutralButton(R.string.delete, (dlg, w) -> {
                st.trips.remove(trip);
                changed();
            });
        }
        b.show();
    }

    private void pickDate(LocalDate initial, LocalDate min, Consumer<LocalDate> then) {
        DatePickerDialog p = new DatePickerDialog(this,
                (view, y, m, day) -> then.accept(LocalDate.of(y, m + 1, day)),
                initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        if (min != null) {
            p.getDatePicker().setMinDate(min.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        p.show();
    }
}
