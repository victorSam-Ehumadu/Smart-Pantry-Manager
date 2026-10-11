package za.ac.richfield.smartpantry.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import za.ac.richfield.smartpantry.model.IngredientStatus;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;
import za.ac.richfield.smartpantry.model.RecipeMatch;

/**
 * The strict-matching rule from Section 2.3 of the brief - the core business logic of the app.
 *
 * <p>A recipe is only suggested when EVERY ingredient it requires is in the pantry in at
 * least the required quantity. There is no partial credit: a recipe needing five
 * ingredients with four in the pantry is excluded, full stop. That decision lives in one
 * place, {@link RecipeMatch#isCookableNow()}, which is simply "nothing outstanding".</p>
 *
 * <p>Each required ingredient is checked in three stages:</p>
 * <ol>
 *     <li><b>Identify</b> - find the pantry rows that are the same ingredient, comparing the
 *         normalised keys produced by {@link IngredientNormalizer} rather than raw text, so
 *         "Tomatoes" in the pantry satisfies a recipe asking for "tomato".</li>
 *     <li><b>Convert</b> - put the required amount and the pantry amount into the same base
 *         unit via {@link UnitConverter}, so 0.5 kg in the pantry covers 500 g in a recipe.</li>
 *     <li><b>Compare</b> - the pantry total must be greater than or equal to the required
 *         amount. Only then is the ingredient satisfied.</li>
 * </ol>
 *
 * <p>The matcher holds a snapshot of the pantry and is cheap to build, so the Suggested
 * Recipes screen creates a fresh one every time it resumes. That is what makes a recipe
 * appear or disappear the moment the user adds or deletes an ingredient.</p>
 */
public class RecipeMatcher {

    /** Floating-point slack, so 500 g in the pantry is never "0.0000001 short" of 500 g. */
    private static final double EPSILON = 0.0001;

    /** Pantry rows bucketed by normalised name, so the common case is a map lookup. */
    private final Map<String, List<PantryItem>> pantryByKey = new HashMap<>();

    /**
     * When false the rule degrades to "the ingredient is present at all", ignoring amounts.
     * Driven by a Settings switch so the behaviour can be demonstrated both ways in the video.
     */
    private final boolean enforceQuantities;

    /**
     * @param pantry            every row currently in the pantry table
     * @param enforceQuantities true for the full strict rule (name AND quantity)
     */
    public RecipeMatcher(List<PantryItem> pantry, boolean enforceQuantities) {
        this.enforceQuantities = enforceQuantities;
        if (pantry == null) {
            return;
        }
        for (PantryItem item : pantry) {
            // A row that has run down to zero is not something the user can cook with.
            if (item.getQuantity() <= 0) {
                continue;
            }
            String key = keyOf(item);
            if (key.isEmpty()) {
                continue;
            }
            List<PantryItem> bucket = pantryByKey.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                pantryByKey.put(key, bucket);
            }
            bucket.add(item);
        }
    }

    /** Runs the rule over one recipe and reports the verdict for every ingredient. */
    public RecipeMatch match(Recipe recipe) {
        List<IngredientStatus> statuses = new ArrayList<>();
        for (RecipeIngredient required : recipe.getIngredients()) {
            statuses.add(evaluate(required));
        }
        return new RecipeMatch(recipe, statuses);
    }

    /** Convenience wrapper that evaluates a whole recipe book. */
    public List<RecipeMatch> matchAll(List<Recipe> recipes) {
        List<RecipeMatch> results = new ArrayList<>();
        if (recipes == null) {
            return results;
        }
        for (Recipe recipe : recipes) {
            results.add(match(recipe));
        }
        return results;
    }

    /**
     * The strict suggestions list: only recipes the user can cook right now, quickest first.
     */
    public List<RecipeMatch> suggestions(List<Recipe> recipes) {
        List<RecipeMatch> cookable = new ArrayList<>();
        for (RecipeMatch candidate : matchAll(recipes)) {
            if (candidate.isCookableNow()) {
                cookable.add(candidate);
            }
        }
        Collections.sort(cookable, new Comparator<RecipeMatch>() {
            @Override
            public int compare(RecipeMatch left, RecipeMatch right) {
                int byTime = Integer.compare(left.getRecipe().getPrepMinutes(),
                        right.getRecipe().getPrepMinutes());
                if (byTime != 0) {
                    return byTime;
                }
                return left.getRecipe().getName().compareToIgnoreCase(right.getRecipe().getName());
            }
        });
        return cookable;
    }

    /**
     * Optional stretch bucket (Section 8): recipes short by exactly one ingredient.
     *
     * <p>Deliberately a separate method returning a separate list. These results must never
     * be merged into {@link #suggestions(List)}, because the brief requires the strict list
     * to stay strict and the two lists to be clearly separated in the UI.</p>
     */
    public List<RecipeMatch> almostThere(List<Recipe> recipes) {
        List<RecipeMatch> nearly = new ArrayList<>();
        for (RecipeMatch candidate : matchAll(recipes)) {
            if (candidate.isOneIngredientShort()) {
                nearly.add(candidate);
            }
        }
        Collections.sort(nearly, new Comparator<RecipeMatch>() {
            @Override
            public int compare(RecipeMatch left, RecipeMatch right) {
                return left.getRecipe().getName().compareToIgnoreCase(right.getRecipe().getName());
            }
        });
        return nearly;
    }

    /**
     * Decides whether a single required ingredient is covered by the pantry.
     *
     * <p>This is the method the whole assignment turns on, so each branch is spelled out
     * rather than compressed into one boolean expression.</p>
     */
    private IngredientStatus evaluate(RecipeIngredient required) {
        String requiredKey = required.getNameKey() != null && !required.getNameKey().isEmpty()
                ? required.getNameKey()
                : IngredientNormalizer.normalize(required.getName());

        List<PantryItem> candidates = findCandidates(requiredKey);
        if (candidates.isEmpty()) {
            // Stage 1 failed: the user simply does not have this ingredient.
            return new IngredientStatus(required, IngredientStatus.Outcome.MISSING, 0d, null);
        }

        String matchedName = candidates.get(0).getName();

        // "A pinch of salt" or a Settings switch turned off: presence is enough.
        if (!enforceQuantities || UnitConverter.isNegligibleUnit(required.getUnit())) {
            return new IngredientStatus(required, IngredientStatus.Outcome.SATISFIED,
                    required.getQuantity(), matchedName);
        }

        UnitConverter.Measure requiredMeasure =
                UnitConverter.toBase(required.getQuantity(), required.getUnit());
        if (requiredMeasure == null) {
            // The recipe uses a unit the converter does not know. Accept presence and label
            // it approximate rather than silently rejecting a recipe over a vocabulary gap.
            return new IngredientStatus(required, IngredientStatus.Outcome.SATISFIED_APPROXIMATE,
                    required.getQuantity(), matchedName);
        }

        // Stage 2: total up only the pantry rows measured in a comparable dimension. Two jars
        // of the same ingredient add up; a row recorded in millilitres cannot be added to one
        // recorded in grams without a density, so it is counted separately below.
        double availableBase = 0d;
        boolean sawComparableRow = false;
        for (PantryItem candidate : candidates) {
            UnitConverter.Measure have =
                    UnitConverter.toBase(candidate.getQuantity(), candidate.getUnit());
            if (have != null && have.getDimension() == requiredMeasure.getDimension()) {
                availableBase += have.getBaseAmount();
                sawComparableRow = true;
            }
        }

        if (!sawComparableRow) {
            // The ingredient is there, but in units that cannot be compared with the recipe
            // (for example 2 cups of flour against 250 g of flour). Converting would require
            // per-ingredient densities, which is out of scope, so the app accepts the match
            // and flags it as approximate in the UI instead of inventing a conversion.
            return new IngredientStatus(required, IngredientStatus.Outcome.SATISFIED_APPROXIMATE,
                    required.getQuantity(), matchedName);
        }

        // Stage 3: the actual quantity comparison.
        double availableInRequiredUnit =
                UnitConverter.fromBase(availableBase, required.getUnit());
        if (availableBase + EPSILON >= requiredMeasure.getBaseAmount()) {
            return new IngredientStatus(required, IngredientStatus.Outcome.SATISFIED,
                    availableInRequiredUnit, matchedName);
        }
        return new IngredientStatus(required, IngredientStatus.Outcome.NOT_ENOUGH,
                availableInRequiredUnit, matchedName);
    }

    /**
     * Finds the pantry rows that represent the same ingredient as {@code requiredKey}.
     *
     * <p>An exact key hit is the fast path and covers almost every case, because both sides
     * were normalised the same way on insert. Only when that misses does the method fall
     * back to the fuzzy comparison, which is the part that absorbs typos and the
     * "chicken" vs "chicken breast" style of difference.</p>
     */
    private List<PantryItem> findCandidates(String requiredKey) {
        if (requiredKey == null || requiredKey.isEmpty()) {
            return Collections.emptyList();
        }

        List<PantryItem> exact = pantryByKey.get(requiredKey);
        if (exact != null && !exact.isEmpty()) {
            return exact;
        }

        List<PantryItem> loose = new ArrayList<>();
        for (Map.Entry<String, List<PantryItem>> entry : pantryByKey.entrySet()) {
            if (IngredientNormalizer.looselyMatches(requiredKey, entry.getKey())) {
                loose.addAll(entry.getValue());
            }
        }
        return loose;
    }

    private static String keyOf(PantryItem item) {
        if (item.getNameKey() != null && !item.getNameKey().isEmpty()) {
            return item.getNameKey();
        }
        return IngredientNormalizer.normalize(item.getName());
    }

    /** How many distinct ingredients the snapshot contains, used for the empty-state copy. */
    public int pantryIngredientCount() {
        return pantryByKey.size();
    }
}
