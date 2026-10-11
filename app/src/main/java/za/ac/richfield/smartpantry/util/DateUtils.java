package za.ac.richfield.smartpantry.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Expiry-date helpers.
 *
 * <p>Dates are stored as ISO text ("yyyy-MM-dd") rather than as a millisecond timestamp.
 * That choice means a plain {@code ORDER BY expiry_date} is already a correct chronological
 * sort, a {@code WHERE expiry_date &lt;= ?} comparison works as string comparison, and the
 * value is readable when inspecting the database with Device File Explorer during the demo.
 * It also sidesteps time zones: an expiry date is a calendar day, not an instant.</p>
 */
public final class DateUtils {

    /** Storage format. Locale.US is pinned so the pattern never picks up local digits. */
    private static final String ISO_PATTERN = "yyyy-MM-dd";
    /** Display format, e.g. "24 Oct 2026". */
    private static final String DISPLAY_PATTERN = "dd MMM yyyy";

    private DateUtils() {
        // Static helper only.
    }

    private static SimpleDateFormat isoFormat() {
        // A new instance per call: SimpleDateFormat is not thread-safe, and a shared static
        // one is a classic source of corrupted dates.
        return new SimpleDateFormat(ISO_PATTERN, Locale.US);
    }

    public static String todayIso() {
        return isoFormat().format(new Date());
    }

    /** Today plus {@code days}, as an ISO string. Used by the expiring-soon query. */
    public static String isoDateInDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return isoFormat().format(calendar.getTime());
    }

    public static String toIso(int year, int monthZeroBased, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, monthZeroBased, dayOfMonth);
        return isoFormat().format(calendar.getTime());
    }

    /** Parses an ISO string, returning {@code null} for blank or malformed input. */
    public static Calendar parseIso(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return null;
        }
        try {
            Date parsed = isoFormat().parse(iso.trim());
            if (parsed == null) {
                return null;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsed);
            return calendar;
        } catch (ParseException notADate) {
            // A bad value in the database should not crash the list; the UI just shows no date.
            return null;
        }
    }

    /** "2026-10-24" becomes "24 Oct 2026"; unparseable input is returned untouched. */
    public static String toDisplay(String iso) {
        Calendar calendar = parseIso(iso);
        if (calendar == null) {
            return iso == null ? "" : iso;
        }
        return new SimpleDateFormat(DISPLAY_PATTERN, Locale.getDefault())
                .format(calendar.getTime());
    }

    /**
     * Whole days from today until {@code iso}: 0 means today, negative means already expired.
     *
     * <p>Both sides are truncated to midnight first, so "tomorrow" is always 1 regardless of
     * the time of day the method is called.</p>
     */
    public static int daysUntil(String iso) {
        Calendar target = parseIso(iso);
        if (target == null) {
            return Integer.MAX_VALUE;
        }
        Calendar today = Calendar.getInstance();
        truncateToMidnight(today);
        truncateToMidnight(target);

        long difference = target.getTimeInMillis() - today.getTimeInMillis();
        return (int) Math.round(difference / 86400000d);
    }

    public static boolean isExpired(String iso) {
        return parseIso(iso) != null && daysUntil(iso) < 0;
    }

    public static boolean isExpiringWithin(String iso, int days) {
        if (parseIso(iso) == null) {
            return false;
        }
        int remaining = daysUntil(iso);
        return remaining >= 0 && remaining <= days;
    }

    private static void truncateToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}
