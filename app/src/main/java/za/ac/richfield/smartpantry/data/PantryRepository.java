package za.ac.richfield.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.logic.IngredientNormalizer;
import za.ac.richfield.smartpantry.logic.UnitConverter;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.DateUtils;

/**
 * All four CRUD operations on the pantry table, in one place.
 *
 * <p>The Activities never touch {@link SQLiteDatabase} or a {@link Cursor} themselves. They
 * ask this class for {@link PantryItem} objects and hand {@link PantryItem} objects back,
 * which keeps the SQL in one file and makes the UI code readable.</p>
 *
 * <p>Every write passes the ingredient name through {@link IngredientNormalizer} and stores
 * the result in {@code name_key}. Doing it here rather than in the matcher guarantees that
 * no row can ever reach the database without a key, whichever screen inserted it.</p>
 */
public class PantryRepository {

    /** The orders offered by the toolbar's sort menu on the pantry screen. */
    public enum SortOrder {
        NAME, EXPIRY, RECENT
    }

    private final DatabaseHelper helper;

    public PantryRepository(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    // ---------------------------------------------------------------- CREATE

    /**
     * Inserts a new ingredient.
     *
     * @return the new row id, or -1 if the insert failed
     */
    public long insert(PantryItem item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = toContentValues(item);
        values.put(DatabaseHelper.C_PANTRY_CREATED, System.currentTimeMillis());
        long id = db.insert(DatabaseHelper.T_PANTRY, null, values);
        item.setId(id);
        return id;
    }

    // ------------------------------------------------------------------ READ

    /** Every ingredient in the pantry, in the requested order. */
    public List<PantryItem> getAll(SortOrder sortOrder) {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(DatabaseHelper.T_PANTRY, null, null, null, null, null,
                orderByClause(sortOrder));
        try {
            while (cursor.moveToNext()) {
                items.add(fromCursor(cursor));
            }
        } finally {
            // Closing in a finally block matters: an exception mid-loop would otherwise leak
            // the cursor and Android would log a "finalized without being closed" warning.
            cursor.close();
        }
        return items;
    }

    /** Name-contains search, used by the toolbar's search field. */
    public List<PantryItem> search(String query, SortOrder sortOrder) {
        if (query == null || query.trim().isEmpty()) {
            return getAll(sortOrder);
        }

        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        // Bound as an argument rather than concatenated into the SQL, so an apostrophe in
        // "pig's trotters" cannot break the statement.
        String like = "%" + query.trim() + "%";
        Cursor cursor = db.query(DatabaseHelper.T_PANTRY, null,
                DatabaseHelper.C_PANTRY_NAME + " LIKE ? OR "
                        + DatabaseHelper.C_PANTRY_KEY + " LIKE ?",
                new String[]{like, like}, null, null, orderByClause(sortOrder));
        try {
            while (cursor.moveToNext()) {
                items.add(fromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return items;
    }

    /** A single row by id, or {@code null} when it no longer exists. */
    public PantryItem getById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.T_PANTRY, null,
                DatabaseHelper.C_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    public int count() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.T_PANTRY, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    /**
     * How many items expire within the next {@code days} days, including anything already
     * past its date. Feeds the "expiring soon" banner on the pantry screen.
     */
    public int countExpiringWithin(int days) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.T_PANTRY
                        + " WHERE " + DatabaseHelper.C_PANTRY_EXPIRY + " IS NOT NULL"
                        + " AND " + DatabaseHelper.C_PANTRY_EXPIRY + " != ''"
                        + " AND " + DatabaseHelper.C_PANTRY_EXPIRY + " <= ?",
                new String[]{DateUtils.isoDateInDays(days)});
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    /** True when the user already has a row for this ingredient, used by form validation. */
    public boolean existsByKey(String nameKey, long excludingId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.T_PANTRY,
                new String[]{DatabaseHelper.C_ID},
                DatabaseHelper.C_PANTRY_KEY + " = ? AND " + DatabaseHelper.C_ID + " != ?",
                new String[]{nameKey, String.valueOf(excludingId)},
                null, null, null, "1");
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    // ---------------------------------------------------------------- UPDATE

    /** @return the number of rows changed, so the caller can tell a no-op from a success */
    public int update(PantryItem item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.update(DatabaseHelper.T_PANTRY, toContentValues(item),
                DatabaseHelper.C_ID + " = ?", new String[]{String.valueOf(item.getId())});
    }

    // ---------------------------------------------------------------- DELETE

    public int delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(DatabaseHelper.T_PANTRY,
                DatabaseHelper.C_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /** Clears the whole pantry; offered from Settings behind a confirmation dialog. */
    public int deleteAll() {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(DatabaseHelper.T_PANTRY, null, null);
    }

    // ----------------------------------------------------------------- MAPPING

    private ContentValues toContentValues(PantryItem item) {
        ContentValues values = new ContentValues();
        String displayName = item.getName() == null ? "" : item.getName().trim();
        values.put(DatabaseHelper.C_PANTRY_NAME, displayName);
        values.put(DatabaseHelper.C_PANTRY_KEY, IngredientNormalizer.normalize(displayName));
        values.put(DatabaseHelper.C_PANTRY_QUANTITY, item.getQuantity());
        values.put(DatabaseHelper.C_PANTRY_UNIT, UnitConverter.canonicalUnit(item.getUnit()));
        // An empty expiry is stored as SQL NULL rather than "", so the expiring-soon query
        // does not have to treat the empty string as a date.
        if (item.getExpiryDate() == null || item.getExpiryDate().trim().isEmpty()) {
            values.putNull(DatabaseHelper.C_PANTRY_EXPIRY);
        } else {
            values.put(DatabaseHelper.C_PANTRY_EXPIRY, item.getExpiryDate().trim());
        }
        return values;
    }

    private PantryItem fromCursor(Cursor cursor) {
        PantryItem item = new PantryItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.C_ID)));
        item.setName(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_PANTRY_NAME)));
        item.setNameKey(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_PANTRY_KEY)));
        item.setQuantity(cursor.getDouble(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_PANTRY_QUANTITY)));
        item.setUnit(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_PANTRY_UNIT)));

        int expiryColumn = cursor.getColumnIndexOrThrow(DatabaseHelper.C_PANTRY_EXPIRY);
        item.setExpiryDate(cursor.isNull(expiryColumn) ? null : cursor.getString(expiryColumn));

        item.setCreatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_PANTRY_CREATED)));
        return item;
    }

    private String orderByClause(SortOrder sortOrder) {
        if (sortOrder == null) {
            sortOrder = SortOrder.NAME;
        }
        switch (sortOrder) {
            case EXPIRY:
                // Dates are stored as "yyyy-MM-dd", so a plain text sort is also a date sort.
                // Rows with no expiry go last instead of first, which is what a user expects.
                return "CASE WHEN " + DatabaseHelper.C_PANTRY_EXPIRY + " IS NULL THEN 1 ELSE 0 END, "
                        + DatabaseHelper.C_PANTRY_EXPIRY + " ASC, "
                        + DatabaseHelper.C_PANTRY_NAME + " COLLATE NOCASE ASC";
            case RECENT:
                return DatabaseHelper.C_PANTRY_CREATED + " DESC, "
                        + DatabaseHelper.C_ID + " DESC";
            case NAME:
            default:
                return DatabaseHelper.C_PANTRY_NAME + " COLLATE NOCASE ASC";
        }
    }
}
