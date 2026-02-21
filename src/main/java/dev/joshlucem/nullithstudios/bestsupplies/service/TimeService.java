package dev.joshlucem.nullithstudios.bestsupplies.service;

import dev.joshlucem.nullithstudios.bestsupplies.config.ConfigManager;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;

public class TimeService {

    private final ConfigManager configManager;
    private final ZoneId timezone;
    private final DateTimeFormatter dateFormatter;
    private final WeekFields weekFields;
    private final DateTimeFormatter weeklyPeriodFormatter;

    public TimeService(ConfigManager configManager) {
        this.configManager = configManager;
        this.timezone = configManager.getTimezone();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.weekFields = WeekFields.ISO; // ISO weeks start on Monday
        this.weeklyPeriodFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    }

    /**
     * Get current date in configured timezone
     */
    public LocalDate getCurrentDate() {
        return LocalDate.now(timezone);
    }

    /**
     * Get current date-time in configured timezone
     */
    public ZonedDateTime getCurrentDateTime() {
        return ZonedDateTime.now(timezone);
    }

    /**
     * Get today's date as string (yyyy-MM-dd)
     */
    public String getTodayKey() {
        return getCurrentDate().format(dateFormatter);
    }

    /**
     * Get a date key for a specific date
     */
    public String getDateKey(LocalDate date) {
        return date.format(dateFormatter);
    }

    /**
     * Get the current day of week
     */
    public DayOfWeek getCurrentDayOfWeek() {
        return getCurrentDate().getDayOfWeek();
    }

    /**
     * Get the current weekly period key aligned with configured reset day/hour/minute.
     */
    public String getCurrentWeekKey() {
        return getCurrentWeeklyPeriodKey();
    }

    /**
     * Get week key for a specific date
     */
    public String getWeekKey(LocalDate date) {
        int year = date.get(weekFields.weekBasedYear());
        int week = date.get(weekFields.weekOfWeekBasedYear());
        return String.format("%d-W%02d", year, week);
    }

    /**
     * Get the start of the current week (Monday 00:00)
     */
    public ZonedDateTime getWeekStart() {
        LocalDate today = getCurrentDate();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.atStartOfDay(timezone);
    }

    /**
     * Get the last weekly reset time (the start of the current weekly claim period).
     */
    public ZonedDateTime getLastWeeklyReset() {
        DayOfWeek resetDay = configManager.getWeeklyResetDay();
        int resetHour = configManager.getWeeklyResetHour();
        int resetMinute = configManager.getWeeklyResetMinute();

        ZonedDateTime now = getCurrentDateTime();
        LocalTime resetTime = LocalTime.of(resetHour, resetMinute);

        LocalDate resetDate = now.toLocalDate().with(TemporalAdjusters.previousOrSame(resetDay));
        ZonedDateTime lastReset = resetDate.atTime(resetTime).atZone(timezone);

        if (lastReset.isAfter(now)) {
            lastReset = lastReset.minusWeeks(1);
        }

        return lastReset;
    }

    /**
     * Get current weekly claim period key.
     */
    public String getCurrentWeeklyPeriodKey() {
        return getLastWeeklyReset().format(weeklyPeriodFormatter);
    }

    /**
     * Get the next weekly reset time
     */
    public ZonedDateTime getNextWeeklyReset() {
        return getLastWeeklyReset().plusWeeks(1);
    }

    /**
     * Get time until next weekly reset as Duration
     */
    public Duration getTimeUntilWeeklyReset() {
        ZonedDateTime now = getCurrentDateTime();
        ZonedDateTime nextReset = getNextWeeklyReset();
        return Duration.between(now, nextReset);
    }

    /**
     * Get the start of a specific day (00:00)
     */
    public ZonedDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay(timezone);
    }

    /**
     * Get the start of today
     */
    public ZonedDateTime getStartOfToday() {
        return getStartOfDay(getCurrentDate());
    }

    /**
     * Get the start of tomorrow
     */
    public ZonedDateTime getStartOfTomorrow() {
        return getStartOfDay(getCurrentDate().plusDays(1));
    }

    /**
     * Get time until a specific date-time
     */
    public Duration getTimeUntil(ZonedDateTime target) {
        ZonedDateTime now = getCurrentDateTime();
        Duration duration = Duration.between(now, target);
        return duration.isNegative() ? Duration.ZERO : duration;
    }

    /**
     * Get time until the start of a specific day
     */
    public Duration getTimeUntilDay(DayOfWeek targetDay) {
        LocalDate today = getCurrentDate();
        DayOfWeek currentDay = today.getDayOfWeek();

        int daysUntil = targetDay.getValue() - currentDay.getValue();
        if (daysUntil < 0) {
            daysUntil += 7;
        } else if (daysUntil == 0) {
            // Today
            return Duration.ZERO;
        }

        LocalDate targetDate = today.plusDays(daysUntil);
        return getTimeUntil(getStartOfDay(targetDate));
    }

    /**
     * Format a Duration as "Xd Xh Xm Xs"
     */
    public String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "0s";
        }

        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    /**
     * Format milliseconds as duration string
     */
    public String formatMillis(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        return formatDuration(Duration.ofMillis(millis));
    }

    /**
     * Check if a date string is today
     */
    public boolean isToday(String dateKey) {
        return getTodayKey().equals(dateKey);
    }

    /**
     * Check if a date string is in the past (before today)
     */
    public boolean isPast(String dateKey) {
        try {
            LocalDate date = LocalDate.parse(dateKey, dateFormatter);
            return date.isBefore(getCurrentDate());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a date string is in the future (after today)
     */
    public boolean isFuture(String dateKey) {
        try {
            LocalDate date = LocalDate.parse(dateKey, dateFormatter);
            return date.isAfter(getCurrentDate());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the date for a specific day of the current week
     */
    public LocalDate getDateForDayOfWeek(DayOfWeek day) {
        LocalDate today = getCurrentDate();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday.plusDays(day.getValue() - 1);
    }

    /**
     * Check if player was seen yesterday (for streak check)
     */
    public boolean wasYesterday(String dateKey) {
        if (dateKey == null) return false;
        try {
            LocalDate date = LocalDate.parse(dateKey, dateFormatter);
            LocalDate yesterday = getCurrentDate().minusDays(1);
            return date.equals(yesterday);
        } catch (Exception e) {
            return false;
        }
    }

    public ZoneId getTimezone() {
        return timezone;
    }
}
