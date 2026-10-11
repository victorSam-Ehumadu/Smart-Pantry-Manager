package za.ac.richfield.smartpantry.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * Read access to the seeded recipe book.
 *
 * <p>Recipes are app-owned content, so this class only reads; the pantry is the part the
 * user creates, updates and deletes. Restoring the book after a reset is handled by
 * {@link DatabaseHelper#reseedRecipes()}.</p>
 */
public class RecipeRepository {

    private final DatabaseHelper helper;

    public RecipeRepository(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    /** Recipe rows only, without their ingredient lists. Cheap, for the browse screen header. */
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.T_RECIPES, null, null, null, null, null,
                DatabaseHelper.C_RECIPE_NAME + " COLLATE NOCASE ASC");
        try {
            while (cursor.moveToNext()) {
                recipes.add(recipeFromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return recipes;
    }

    /**
     * Every recipe with its ingredients attached - the input the matcher needs.
     *
     * <p>Two queries in total, not one per recipe. The ingredient rows are fetched in a
     * single pass and bucketed by {@code recipe_id} in memory, which avoids the classic
     * N+1 query pattern that would otherwise mean 21 queries on every screen refresh.</p>
     */
    public List<Recipe> getAllRecipesWithIngredients() {
        List<Recipe> recipes = getAllRecipes();
        Map<Long, List<RecipeIngredient>> byRecipe = loadAllIngredients();
        for (Recipe recipe : recipes) {
            List<RecipeIngredient> ingredients = byRecipe.get(recipe.getId());
            if (ingredients != null) {
                recipe.setIngredients(ingredients);
            }
        }
        return recipes;
    }

    /** One recipe with its ingredients, for the detail screen. */
    public Recipe getRecipeById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Recipe recipe = null;
        Cursor cursor = db.query(DatabaseHelper.T_RECIPES, null,
                DatabaseHelper.C_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);
        try {
            if (cursor.moveToFirst()) {
                recipe = recipeFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }
        if (recipe != null) {
            recipe.setIngredients(getIngredientsFor(recipe.getId()));
        }
        return recipe;
    }

    public List<RecipeIngredient> getIngredientsFor(long recipeId) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.T_RECIPE_INGREDIENTS, null,
                DatabaseHelper.C_RI_RECIPE_ID + " = ?", new String[]{String.valueOf(recipeId)},
                null, null, DatabaseHelper.C_ID + " ASC");
        try {
            while (cursor.moveToNext()) {
                ingredients.add(ingredientFromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return ingredients;
    }

    /**
     * Distinct ingredient names used anywhere in the recipe book.
     *
     * <p>Feeds the auto-complete on the add/edit form. Suggesting the spellings the recipes
     * actually use nudges the user towards names that will match, which does more for the
     * hit rate than any amount of fuzzy matching after the fact.</p>
     */
    public List<String> distinctIngredientNames() {
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT DISTINCT " + DatabaseHelper.C_RI_NAME
                        + " FROM " + DatabaseHelper.T_RECIPE_INGREDIENTS
                        + " ORDER BY " + DatabaseHelper.C_RI_NAME + " COLLATE NOCASE ASC", null);
        try {
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }
        return names;
    }

    public int count() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.T_RECIPES, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private Map<Long, List<RecipeIngredient>> loadAllIngredients() {
        Map<Long, List<RecipeIngredient>> byRecipe = new HashMap<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.T_RECIPE_INGREDIENTS, null, null, null, null, null,
                DatabaseHelper.C_RI_RECIPE_ID + " ASC, " + DatabaseHelper.C_ID + " ASC");
        try {
            while (cursor.moveToNext()) {
                RecipeIngredient ingredient = ingredientFromCursor(cursor);
                List<RecipeIngredient> bucket = byRecipe.get(ingredient.getRecipeId());
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    byRecipe.put(ingredient.getRecipeId(), bucket);
                }
                bucket.add(ingredient);
            }
        } finally {
            cursor.close();
        }
        return byRecipe;
    }

    private Recipe recipeFromCursor(Cursor cursor) {
        Recipe recipe = new Recipe();
        recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.C_ID)));
        recipe.setName(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RECIPE_NAME)));
        recipe.setCategory(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RECIPE_CATEGORY)));
        recipe.setPrepMinutes(cursor.getInt(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RECIPE_PREP)));
        recipe.setServings(cursor.getInt(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RECIPE_SERVINGS)));
        recipe.setSteps(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RECIPE_STEPS)));
        return recipe;
    }

    private RecipeIngredient ingredientFromCursor(Cursor cursor) {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.C_ID)));
        ingredient.setRecipeId(cursor.getLong(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RI_RECIPE_ID)));
        ingredient.setName(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RI_NAME)));
        ingredient.setNameKey(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RI_KEY)));
        ingredient.setQuantity(cursor.getDouble(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RI_QUANTITY)));
        ingredient.setUnit(cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseHelper.C_RI_UNIT)));
        return ingredient;
    }
}
