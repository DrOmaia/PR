package com.dromaia.residency;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** "pub" build (the public update file): no dates inside. The dates already saved on the phone are kept. */
final class Defaults {
    private Defaults() {}

    static LocalDate grant() {
        return null;
    }

    static LocalDate end() {
        return null;
    }

    static int[] endHijri() {
        return null;
    }

    static List<Calc.Trip> trips() {
        return new ArrayList<>();
    }
}
