package fr.tt54.protectYourCastle.utils;

import java.util.*;

public enum TimeUnit {

    SECONDS("seconde", "s", 1),
    MINUTES("minute", "min", 60),
    HOURS("heure", "h", 60 * 60),
    DAYS("jour", "d", 24 * 60 * 60),
    MONTHS("mois", "month", 30 * 24 * 60 * 60)/*,
    YEARS("years", "y", 365*30*24*60*60)*/;

    private static Map<String, TimeUnit> durationsSub = new HashMap<>();

    static {
        for (TimeUnit duration : values()) {
            durationsSub.put(duration.subName, duration);
        }
    }

    private String name;
    private String subName;
    private int timeInSeconds;

    TimeUnit(String name, String subName, int timeInSeconds) {
        this.name = name;
        this.subName = subName;
        this.timeInSeconds = timeInSeconds;
    }

    public static boolean existSub(String sub) {
        return durationsSub.containsKey(sub);
    }

    public static TimeUnit getDurationFromSub(String sub) {
        return durationsSub.getOrDefault(sub, SECONDS);
    }

    public static int getTimeInSeconds(int time, TimeUnit duration) {
        return time * duration.getTimeInSeconds();
    }

    public static String getFormattedTimeLeft(long timeLeft) {
        if (timeLeft == -1)
            return "Permanent";
        if (timeLeft < 1) timeLeft = 1;

        int months;
        int days;
        int hours;
        int minutes;
        int seconds;

        months = (int) (timeLeft / MONTHS.getTimeInSeconds());
        timeLeft -= (long) months * MONTHS.getTimeInSeconds();

        days = (int) (timeLeft / DAYS.getTimeInSeconds());
        timeLeft -= (long) days * DAYS.getTimeInSeconds();

        hours = (int) (timeLeft / HOURS.getTimeInSeconds());
        timeLeft -= (long) hours * HOURS.getTimeInSeconds();

        minutes = (int) (timeLeft / MINUTES.getTimeInSeconds());
        timeLeft -= (long) minutes * MINUTES.getTimeInSeconds();

        seconds = (int) timeLeft;

        String str = ((months != 0) ? months + " " + MONTHS.getName() + " " : "") + ((days != 0) ? days + " " + DAYS.getName() + (days > 1 ? "s" : "") + " " : "") + ((hours != 0) ? hours + " " + HOURS.getName() + (hours > 1 ? "s" : "") + " " : "") + ((minutes != 0) ? minutes + " " + MINUTES.getName() + (minutes > 1 ? "s" : "") + " " : "") + ((seconds != 0) ? seconds + " " + SECONDS.getName() + (seconds > 1 ? "s" : "") + " " : "");
        return str.substring(0, str.length() - 1);
    }

    public static String getShortFormattedTimeLeft(int timeLeft, TimeUnit... ignoredUnits) {
        if (timeLeft == -1)
            return "Permanent";
        if (timeLeft < 1) timeLeft = 1;

        Set<TimeUnit> units = Set.of(ignoredUnits);
        int months = 0, days = 0, hours = 0, minutes = 0, seconds = 0;

        if(!units.contains(MONTHS)){
            months = timeLeft / MONTHS.getTimeInSeconds();
            timeLeft %= MONTHS.getTimeInSeconds();
        }
        if(!units.contains(DAYS)){
            days = timeLeft / DAYS.getTimeInSeconds();
            timeLeft %= DAYS.getTimeInSeconds();
        }
        if(!units.contains(HOURS)){
            hours = timeLeft / HOURS.getTimeInSeconds();
            timeLeft %= HOURS.getTimeInSeconds();
        }
        if(!units.contains(MINUTES)){
            minutes = timeLeft / MINUTES.getTimeInSeconds();
            timeLeft %= MINUTES.getTimeInSeconds();
        }
        if(!units.contains(SECONDS)){
            seconds = timeLeft;
        }

        String str = ((months != 0) ? months + " " + MONTHS.getSubName() + " " : "") + ((days != 0) ? days + " " + DAYS.getSubName() + " " : "") + ((hours != 0) ? hours + " " + HOURS.getSubName() + " " : "") + ((minutes != 0) ? minutes + " " + MINUTES.getSubName() + " " : "") + ((seconds != 0) ? seconds + " " + SECONDS.getSubName() + " " : "");
        return str.substring(0, str.length() - 1);
    }


    public String getName() {
        return name;
    }

    public String getSubName() {
        return subName;
    }

    public int getTimeInSeconds() {
        return timeInSeconds;
    }
}
