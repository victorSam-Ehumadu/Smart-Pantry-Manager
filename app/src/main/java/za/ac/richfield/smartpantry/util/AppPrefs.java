package za.ac.richfield.smartpantry.util;

import android.content.Context;
import android.content.SharedPreferences;

import za.ac.richfield.smartpantry.data.PantryRepository;

/**
 * Typed wrapper around the app's {@link SharedPreferences} file.
 *
 * <p>Settings are user preferences, not records, so they live in SharedPreferences rather
 * than in SQLite - the pantry database holds the data the CRUD requirement is about.
 * Wrapping the keys in one class keeps string literals out of the Activities and means the
 * defaults are declared exactly once.</p>
 */
public class AppPrefs {

    private static final String PREFS_NAME = "smart_pantry_prefs";

    private static final String KEY_EXPIRY_ALERTS = "expiry_alerts_enabled";
    private static final String KEY_EXPIRY_WINDOW = "expiry_window_days";
    private static final String KEY_STRICT_QUANTITIES = "strict_quantities";
    private static final String KEY_SHOW_ALMOST_THERE = "show_almost_there";
    private static final String KEY_SORT_ORDER = "pantry_sort_order";

    private static final boolean DEFAULT_EXPIRY_ALERTS = true;
    private static final int DEFAULT_EXPIRY_WINDOW = 3;
    private static final boolean DEFAULT_STRICT_QUANTITIES = true;
    private static final boolean DEFAULT_SHOW_ALMOST_THERE = true;

    private final SharedPreferences preferences;

    public AppPrefs(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Whether the pantry screen highlights and counts items that are about to expire. */
    public boolean isExpiryAlertsEnabled() {
        return preferences.getBoolean(KEY_EXPIRY_ALERTS, DEFAULT_EXPIRY_ALERTS);
    }

    public void setExpiryAlertsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_EXPIRY_ALERTS, enabled).apply();
    }

    /** How many days ahead counts as "expiring soon". */
    public int getExpiryWindowDays() {
        return preferences.getInt(KEY_EXPIRY_WINDOW, DEFAULT_EXPIRY_WINDOW);
    }

    public void setExpiryWindowDays(int days) {
        preferences.edit().putInt(KEY_EXPIRY_WINDOW, Math.max(1, days)).apply();
    }

    /**
     * True (the default) means the strict rule checks names AND quantities. Turning it off
     * relaxes it to "the ingredient is present at all", which is useful for showing in the
     * video demonstration what the quantity half of the rule actually contributes.
     */
    public boolean isStrictQuantities() {
        return preferences.getBoolean(KEY_STRICT_QUANTITIES, DEFAULT_STRICT_QUANTITIES);
    }

    public void setStrictQuantities(boolean strict) {
        preferences.edit().putBoolean(KEY_STRICT_QUANTITIES, strict).apply();
    }

    /**
     * Whether the optional "Almost There" section is shown below the strict suggestions.
     * It is always a separate, clearly labelled section - never mixed into the strict list.
     */
    public boolean isShowAlmostThere() {
        return preferences.getBoolean(KEY_SHOW_ALMOST_THERE, DEFAULT_SHOW_ALMOST_THERE);
    }

    public void setShowAlmostThere(boolean show) {
        preferences.edit().putBoolean(KEY_SHOW_ALMOST_THERE, show).apply();
    }

    /** The pantry list's sort order, remembered between launches. */
    public PantryRepository.SortOrder getSortOrder() {
        String stored = preferences.getString(KEY_SORT_ORDER,
                PantryRepository.SortOrder.NAME.name());
        try {
            return PantryRepository.SortOrder.valueOf(stored);
        } catch (IllegalArgumentException unknownValue) {
            // Defensive: an old or hand-edited preferences file should not crash startup.
            return PantryRepository.SortOrder.NAME;
        }
    }

    public void setSortOrder(PantryRepository.SortOrder sortOrder) {
        preferences.edit().putString(KEY_SORT_ORDER, sortOrder.name()).apply();
    }
}
