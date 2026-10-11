package za.ac.richfield.smartpantry.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A recipe from the seeded recipe book: a name, the ingredients it needs and how to cook it.
 *
 * <p>{@link #getIngredients()} is populated by the repository when the caller asks for a
 * recipe "with ingredients"; for a plain list screen it may stay empty so the app does not
 * run 20 extra queries it does not need.</p>
 */
public class Recipe implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private String category;
    private int prepMinutes;
    private int servings;
    /** Numbered method, stored as one TEXT column with newline-separated steps. */
    private String steps;

    private final ArrayList<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe() {
    }

    public Recipe(String name, String category, int prepMinutes, int servings, String steps) {
        this.name = name;
        this.category = category;
        this.prepMinutes = prepMinutes;
        this.servings = servings;
        this.steps = steps;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPrepMinutes() {
        return prepMinutes;
    }

    public void setPrepMinutes(int prepMinutes) {
        this.prepMinutes = prepMinutes;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> replacement) {
        ingredients.clear();
        if (replacement != null) {
            ingredients.addAll(replacement);
        }
    }

    public Recipe withIngredients(RecipeIngredient... required) {
        for (RecipeIngredient ingredient : required) {
            ingredients.add(ingredient);
        }
        return this;
    }

    /** Splits the stored method into individual steps for the detail screen. */
    public List<String> getStepList() {
        List<String> result = new ArrayList<>();
        if (steps == null) {
            return result;
        }
        for (String line : steps.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }
}
