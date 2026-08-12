package com.crystalox.kitpvp.util;

import java.text.DecimalFormat;

public final class Format {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.##");

    private Format() {
    }

    public static String time(long ms) {
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        if (minutes > 0 && seconds > 0) {
            return minutes + "м " + seconds + "с";
        }
        if (minutes > 0) {
            return minutes + "м";
        }
        return seconds + "с";
    }

    public static String timeFull(long ms) {
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        return hours + "ч " + minutes + "м";
    }

    public static String number(double v) {
        return NUMBER_FORMAT.format(v);
    }
}
