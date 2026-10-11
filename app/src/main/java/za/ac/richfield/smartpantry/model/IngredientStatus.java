package za.ac.richfield.smartpantry.model;

import java.io.Serializable;

/**
 * The verdict for ONE required ingredient of ONE recipe, produced by
 * {@link za.ac.richfield.smartpantry.logic.RecipeMatcher}.
 *
 * <p>Keeping the per-ingredient reasoning (rather than just a yes/no for the whole recipe)
 * is what lets the recipe detail screen show a tick or a cross next to every line and tell
 * the user exactly what is missing.</p>
 */
public class IngredientStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Why an ingredient did or did not pass the strict check. */
    public enum Outcome {
        /** The pantry holds this ingredient in at least the required quantity. */
        SATISFIED,
        /** The pantry holds it, but the units could not be compared, so presence was accepted. */
        SATISFIED_APPROXIMATE,
        /** The pantry holds it, but not enough of it. */
        NOT_ENOUGH,
        /** The pantry does not hold this ingredient at all. */
        MISSING
    }

    private final RecipeIngredient required;
    private final Outcome outcome;
    /** How much the pantry holds, expressed in the required ingredient's own unit. */
    private final double availableInRequiredUnit;
    /** The pantry row that was matched, or {@code null} when nothing matched. */
    private final String matchedPantryName;

    public IngredientStatus(RecipeIngredient required, Outcome outcome,
                            double availableInRequiredUnit, String matchedPantryName) {
        this.required = required;
        this.outcome = outcome;
        this.availableInRequiredUnit = availableInRequiredUnit;
        this.matchedPantryName = matchedPantryName;
    }

    public RecipeIngredient getRequired() {
        return required;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public double getAvailableInRequiredUnit() {
        return availableInRequiredUnit;
    }

    public String getMatchedPantryName() {
        return matchedPantryName;
    }

    /** The strict rule only accepts these two outcomes. */
    public boolean isSatisfied() {
        return outcome == Outcome.SATISFIED || outcome == Outcome.SATISFIED_APPROXIMATE;
    }
}
