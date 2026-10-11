package za.ac.richfield.smartpantry.model;

import java.io.Serializable;

/**
 * One line of a recipe's "you will need" list, e.g. 2 cloves of garlic.
 *
 * <p>Mirrors a row of the {@code recipe_ingredients} table. Recipes and their ingredients
 * are deliberately stored in two tables rather than as one comma-separated string, so the
 * strict-matching rule can compare quantities and units per ingredient.</p>
 */
public class RecipeIngredient implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long recipeId;
    /** Display name, e.g. "Garlic". */
    private String name;
    /** Normalised lookup key, e.g. "garlic". */
    private String nameKey;
    private double quantity;
    private String unit;

    public RecipeIngredient() {
    }

    public RecipeIngredient(String name, double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
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

    @Override
    public String toString() {
        return name;
    }
}
