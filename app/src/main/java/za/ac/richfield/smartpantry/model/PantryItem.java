package za.ac.richfield.smartpantry.model;

import java.io.Serializable;

/**
 * One ingredient the user actually has at home.
 *
 * <p>Mirrors a single row of the {@code pantry_items} SQLite table. The class is a plain
 * old Java object (POJO) so that the database layer, the adapters and the matching logic
 * can all pass the same type around without any Android dependency.</p>
 */
public class PantryItem implements Serializable {

    /** Pinned so a pantry item stays readable if it is ever written to a Bundle or a file. */
    private static final long serialVersionUID = 1L;

    /** Value used for {@link #id} before the row has been written to SQLite. */
    public static final long NO_ID = -1L;

    private long id = NO_ID;
    /** Exactly what the user typed, e.g. "Cherry Tomatoes". Shown in the UI. */
    private String name;
    /** Normalised lookup key, e.g. "tomato". Written by the repository, used for matching. */
    private String nameKey;
    private double quantity;
    private String unit;
    /** Optional ISO date ("yyyy-MM-dd"), or {@code null} when the user left it blank. */
    private String expiryDate;
    /** Milliseconds since epoch, used as the tie-breaker for "recently added" sorting. */
    private long createdAt;

    public PantryItem() {
        // Required so Cursor -> object mapping can build an empty instance and fill it in.
    }

    public PantryItem(String name, double quantity, String unit, String expiryDate) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.expiryDate = expiryDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameKey() {
        return nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /** True when this object has never been saved, which tells the form to INSERT not UPDATE. */
    public boolean isNew() {
        return id == NO_ID;
    }

    @Override
    public String toString() {
        return name + " (" + quantity + " " + unit + ")";
    }
}
