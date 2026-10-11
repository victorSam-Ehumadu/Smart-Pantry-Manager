package za.ac.richfield.smartpantry.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of running the strict-matching rule over one recipe.
 *
 * <p>{@link #isCookableNow()} is the single flag the Suggested Recipes screen filters on:
 * it is true only when every required ingredient was satisfied.</p>
 */
public class RecipeMatch implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Recipe recipe;
    private final ArrayList<IngredientStatus> statuses;

    public RecipeMatch(Recipe recipe, List<IngredientStatus> statuses) {
        this.recipe = recipe;
        this.statuses = statuses == null
                ? new ArrayList<IngredientStatus>()
                : new ArrayList<>(statuses);
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public List<IngredientStatus> getStatuses() {
        return Collections.unmodifiableList(statuses);
    }

    public int getRequiredCount() {
        return statuses.size();
    }

    public int getSatisfiedCount() {
        int satisfied = 0;
        for (IngredientStatus status : statuses) {
            if (status.isSatisfied()) {
                satisfied++;
            }
        }
        return satisfied;
    }

    public int getMissingCount() {
        return getRequiredCount() - getSatisfiedCount();
    }

    /**
     * THE strict rule: a recipe is only "suggested" when nothing at all is outstanding.
     * A recipe needing 5 ingredients with 4 in the pantry returns false here, which is
     * what keeps it out of the suggestions list.
     */
    public boolean isCookableNow() {
        return !statuses.isEmpty() && getMissingCount() == 0;
    }

    /** Bonus "Almost There" bucket (Section 8): exactly one ingredient short. */
    public boolean isOneIngredientShort() {
        return getMissingCount() == 1;
    }

    /** Display names of everything still outstanding, for the "needs: ..." summary line. */
    public List<String> getOutstandingNames() {
        List<String> names = new ArrayList<>();
        for (IngredientStatus status : statuses) {
            if (!status.isSatisfied()) {
                names.add(status.getRequired().getName());
            }
        }
        return names;
    }

    /** True when at least one ingredient passed only because its units were not comparable. */
    public boolean hasApproximateMatch() {
        for (IngredientStatus status : statuses) {
            if (status.getOutcome() == IngredientStatus.Outcome.SATISFIED_APPROXIMATE) {
                return true;
            }
        }
        return false;
    }
}
