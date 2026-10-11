package za.ac.richfield.smartpantry.logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts a quantity + unit pair into a comparable number.
 *
 * <p>Section 2.3 asks the matching to survive "unit differences". The app cannot compare
 * 0.5 kg against 500 g as raw text, so every recognised unit is mapped to a
 * {@link Dimension} plus a factor that converts it to that dimension's base unit:</p>
 *
 * <ul>
 *     <li>{@link Dimension#MASS} - base unit gram</li>
 *     <li>{@link Dimension#VOLUME} - base unit millilitre</li>
 *     <li>{@link Dimension#COUNT} - base unit "one item" (pieces, cloves, slices, tins)</li>
 * </ul>
 *
 * <p>Two quantities are only compared numerically when they land in the same dimension.
 * Converting across dimensions (grams of flour to cups of flour) needs a density per
 * ingredient, which is well outside the scope of the brief, so the matcher treats those
 * cases as "present, quantity not verifiable" instead of guessing.</p>
 */
public final class UnitConverter {

    /** The kind of physical quantity a unit measures. */
    public enum Dimension {
        MASS, VOLUME, COUNT, UNKNOWN
    }

    /** A quantity expressed in its dimension's base unit. */
    public static final class Measure {
        private final Dimension dimension;
        private final double baseAmount;

        Measure(Dimension dimension, double baseAmount) {
            this.dimension = dimension;
            this.baseAmount = baseAmount;
        }

        public Dimension getDimension() {
            return dimension;
        }

        /** Grams for MASS, millilitres for VOLUME, item count for COUNT. */
        public double getBaseAmount() {
            return baseAmount;
        }
    }

    /** unit alias -> dimension. */
    private static final Map<String, Dimension> DIMENSIONS = new HashMap<>();
    /** unit alias -> multiplier that converts it to the dimension's base unit. */
    private static final Map<String, Double> FACTORS = new HashMap<>();

    /**
     * Units that describe a seasoning rather than a measurable amount. If a recipe asks for
     * "a pinch of salt", owning salt at all is enough - no sane strict rule should reject a
     * meal because the user recorded 1 g of salt instead of 2 g.
     */
    private static final Set<String> NEGLIGIBLE_UNITS = new HashSet<>(Arrays.asList(
            "pinch", "pinches", "dash", "dashes", "splash", "splashes", "sprinkle",
            "to taste", "taste", "as needed", "garnish", "drizzle", "handful"
    ));

    /** Units offered in the add/edit form's dropdown, grouped by dimension. */
    private static final Map<String, String> DISPLAY_UNITS = new LinkedHashMap<>();

    static {
        // ---- Mass, base unit = gram -------------------------------------------------
        registerUnit(Dimension.MASS, 1.0, "g", "gram", "grams", "gr", "gm");
        registerUnit(Dimension.MASS, 1000.0, "kg", "kilo", "kilos", "kilogram", "kilograms");
        registerUnit(Dimension.MASS, 0.001, "mg", "milligram", "milligrams");
        registerUnit(Dimension.MASS, 28.349523125, "oz", "ounce", "ounces");
        registerUnit(Dimension.MASS, 453.59237, "lb", "lbs", "pound", "pounds");

        // ---- Volume, base unit = millilitre ----------------------------------------
        registerUnit(Dimension.VOLUME, 1.0, "ml", "millilitre", "millilitres",
                "milliliter", "milliliters", "cc");
        registerUnit(Dimension.VOLUME, 1000.0, "l", "litre", "litres", "liter", "liters");
        // South African / metric spoon and cup sizes, which is what the module's locale uses.
        registerUnit(Dimension.VOLUME, 5.0, "tsp", "teaspoon", "teaspoons", "t");
        registerUnit(Dimension.VOLUME, 15.0, "tbsp", "tablespoon", "tablespoons", "tbs", "tb");
        registerUnit(Dimension.VOLUME, 250.0, "cup", "cups", "c");
        registerUnit(Dimension.VOLUME, 29.5735295625, "floz", "fl oz", "fluid ounce", "fluid ounces");

        // ---- Count, base unit = one item -------------------------------------------
        registerUnit(Dimension.COUNT, 1.0, "", "piece", "pieces", "pc", "pcs", "unit", "units",
                "item", "items", "each", "x");
        registerUnit(Dimension.COUNT, 1.0, "clove", "cloves");
        registerUnit(Dimension.COUNT, 1.0, "slice", "slices");
        registerUnit(Dimension.COUNT, 1.0, "can", "cans", "tin", "tins");
        registerUnit(Dimension.COUNT, 1.0, "packet", "packets", "pack", "packs", "sachet", "sachets");
        registerUnit(Dimension.COUNT, 1.0, "bunch", "bunches", "sprig", "sprigs",
                "stick", "sticks", "leaf", "leaves", "head", "heads");
        registerUnit(Dimension.COUNT, 1.0, "bottle", "bottles", "jar", "jars", "box", "boxes");
        registerUnit(Dimension.COUNT, 12.0, "dozen", "dozens");

        DISPLAY_UNITS.put("g", "g (grams)");
        DISPLAY_UNITS.put("kg", "kg (kilograms)");
        DISPLAY_UNITS.put("ml", "ml (millilitres)");
        DISPLAY_UNITS.put("l", "l (litres)");
        DISPLAY_UNITS.put("tsp", "tsp (teaspoons)");
        DISPLAY_UNITS.put("tbsp", "tbsp (tablespoons)");
        DISPLAY_UNITS.put("cup", "cup (250 ml)");
        DISPLAY_UNITS.put("piece", "piece(s)");
        DISPLAY_UNITS.put("clove", "clove(s)");
        DISPLAY_UNITS.put("slice", "slice(s)");
        DISPLAY_UNITS.put("can", "can(s) / tin(s)");
        DISPLAY_UNITS.put("packet", "packet(s)");
        DISPLAY_UNITS.put("bunch", "bunch(es)");
        DISPLAY_UNITS.put("pinch", "pinch / to taste");
    }

    private static void registerUnit(Dimension dimension, double factor, String... aliases) {
        for (String alias : aliases) {
            DIMENSIONS.put(alias, dimension);
            FACTORS.put(alias, factor);
        }
    }

    private UnitConverter() {
        // Static helper only; never instantiated.
    }

    /** Lower-cases a unit and strips the punctuation people type ("Tbsp." -> "tbsp"). */
    public static String canonicalUnit(String rawUnit) {
        if (rawUnit == null) {
            return "";
        }
        // Locale.ROOT for the same reason as in IngredientNormalizer: unit codes are
        // internal identifiers, never user-facing text.
        String cleaned = rawUnit.trim().toLowerCase(Locale.ROOT)
                .replace(".", "")
                .replaceAll("\\s+", " ");
        // A trailing "s" is only stripped when that produces a unit we actually know,
        // so "grams" resolves but an unknown unit is left intact for the caller to see.
        if (!DIMENSIONS.containsKey(cleaned) && cleaned.endsWith("s")) {
            String singular = cleaned.substring(0, cleaned.length() - 1);
            if (DIMENSIONS.containsKey(singular)) {
                return singular;
            }
        }
        return cleaned;
    }

    /**
     * Converts a quantity into its dimension's base unit.
     *
     * @return the converted measure, or {@code null} when the unit is not recognised
     */
    public static Measure toBase(double quantity, String rawUnit) {
        String unit = canonicalUnit(rawUnit);
        Dimension dimension = DIMENSIONS.get(unit);
        Double factor = FACTORS.get(unit);
        if (dimension == null || factor == null) {
            return null;
        }
        return new Measure(dimension, quantity * factor);
    }

    /** True when owning the ingredient at all satisfies the recipe (pinch, to taste, ...). */
    public static boolean isNegligibleUnit(String rawUnit) {
        return NEGLIGIBLE_UNITS.contains(canonicalUnit(rawUnit));
    }

    /** True when both units resolve to the same dimension and can be compared numerically. */
    public static boolean isComparable(String unitA, String unitB) {
        Dimension a = DIMENSIONS.get(canonicalUnit(unitA));
        Dimension b = DIMENSIONS.get(canonicalUnit(unitB));
        return a != null && a == b;
    }

    /**
     * Expresses {@code baseAmount} back in {@code rawUnit}, for messages such as
     * "you have 0.5 of the 2 cups needed".
     */
    public static double fromBase(double baseAmount, String rawUnit) {
        Double factor = FACTORS.get(canonicalUnit(rawUnit));
        if (factor == null || factor == 0d) {
            return baseAmount;
        }
        return baseAmount / factor;
    }

    /** Canonical unit codes for the spinner, in display order. */
    public static List<String> spinnerUnitCodes() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(DISPLAY_UNITS.keySet()));
    }

    /** Human-readable labels matching {@link #spinnerUnitCodes()} position for position. */
    public static List<String> spinnerUnitLabels() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(DISPLAY_UNITS.values()));
    }

    /**
     * Formats a quantity for display, dropping the ".0" on whole numbers so the list reads
     * "2 cloves" rather than "2.0 cloves".
     */
    public static String formatQuantity(double quantity) {
        if (Math.abs(quantity - Math.rint(quantity)) < 0.001) {
            return String.valueOf((long) Math.rint(quantity));
        }
        return String.format(Locale.getDefault(), "%.2f", quantity)
                .replaceAll("0+$", "")
                .replaceAll("[.,]$", "");
    }

    /** "400 g", "2 cloves", or just "2" when the unit is a bare count. */
    public static String formatAmount(double quantity, String rawUnit) {
        String unit = canonicalUnit(rawUnit);
        String amount = formatQuantity(quantity);
        if (unit.isEmpty()) {
            return amount;
        }
        return amount + " " + unit;
    }
}
