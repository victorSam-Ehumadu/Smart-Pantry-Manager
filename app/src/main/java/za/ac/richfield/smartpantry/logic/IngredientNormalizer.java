package za.ac.richfield.smartpantry.logic;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns messy, human-typed ingredient names into a stable lookup key.
 *
 * <p>The brief (Section 2.3) says a naive exact-string match that breaks on "tomato" vs
 * "tomatoes" will be marked down, but that a full NLP solution is not needed. This class is
 * that middle ground, applied in a fixed pipeline:</p>
 *
 * <ol>
 *     <li>lower-case, strip accents and punctuation;</li>
 *     <li>drop preparation words that describe the ingredient rather than identify it, so
 *         "finely chopped fresh garlic" and "Garlic" become the same pantry item;</li>
 *     <li>singularise each remaining word ("tomatoes" -&gt; "tomato", "leaves" -&gt; "leaf");</li>
 *     <li>map regional and shop-shelf synonyms onto one spelling
 *         ("courgette" -&gt; "zucchini", "mince" -&gt; "ground beef").</li>
 * </ol>
 *
 * <p>Normalisation runs once, when a row is written to SQLite, and the result is stored in
 * its own {@code name_key} column. That keeps the matching work cheap and still lets the UI
 * show the user exactly what they typed.</p>
 */
public final class IngredientNormalizer {

    /**
     * Words describing how an ingredient was prepared or how big it is. They never change
     * WHICH ingredient it is, so they are removed before anything is compared.
     *
     * <p>"ground" and "minced" are deliberately absent. They look like preparation words but
     * they identify a different product: ground beef is not a substitute for a joint of beef,
     * and stripping the word would merge the two onto one key. The looser word-subset rule in
     * {@link #looselyMatches(String, String)} still lets pantry "minced garlic" satisfy a
     * recipe asking for "garlic", which is the case where the word really is noise.</p>
     */
    private static final Set<String> DESCRIPTORS = new HashSet<>(Arrays.asList(
            "fresh", "freshly", "frozen", "chopped", "diced", "sliced", "grated",
            "shredded", "crushed", "whole", "raw", "cooked", "leftover", "ripe",
            "large", "medium", "small", "extra", "finely", "coarsely", "roughly", "thinly",
            "peeled", "cubed", "halved", "boneless", "skinless", "lean", "plain", "pure",
            "organic", "unsalted", "optional", "approx", "about", "of", "a", "an", "the",
            "any", "some", "good", "quality", "free", "range"
    ));

    /** Irregular plurals that the suffix rules in {@link #singularize(String)} would get wrong. */
    private static final Map<String, String> IRREGULAR_PLURALS;

    /**
     * Whole-name synonyms, applied AFTER singularisation so only the singular spelling needs
     * an entry. Both keys and values are themselves already-normalised keys.
     */
    private static final Map<String, String> SYNONYMS;

    static {
        Map<String, String> irregular = new HashMap<>();
        irregular.put("leaves", "leaf");
        irregular.put("loaves", "loaf");
        irregular.put("potatoes", "potato");
        irregular.put("tomatoes", "tomato");
        irregular.put("chillies", "chilli");
        irregular.put("berries", "berry");
        irregular.put("cherries", "cherry");
        irregular.put("peas", "pea");
        irregular.put("oats", "oats");
        irregular.put("molasses", "molasses");
        irregular.put("asparagus", "asparagus");
        irregular.put("hummus", "hummus");
        irregular.put("couscous", "couscous");
        IRREGULAR_PLURALS = Collections.unmodifiableMap(irregular);

        Map<String, String> synonyms = new HashMap<>();
        // Regional names for the same vegetable.
        synonyms.put("courgette", "zucchini");
        synonyms.put("baby marrow", "zucchini");
        synonyms.put("aubergine", "eggplant");
        synonyms.put("brinjal", "eggplant");
        synonyms.put("capsicum", "bell pepper");
        synonyms.put("sweet pepper", "bell pepper");
        synonyms.put("green pepper", "bell pepper");
        synonyms.put("red pepper", "bell pepper");
        synonyms.put("coriander", "cilantro");
        synonyms.put("rocket", "arugula");
        synonyms.put("spring onion", "green onion");
        synonyms.put("scallion", "green onion");
        synonyms.put("brown onion", "onion");
        synonyms.put("white onion", "onion");
        // Meat and dairy.
        synonyms.put("mince", "ground beef");
        synonyms.put("beef mince", "ground beef");
        synonyms.put("minced beef", "ground beef");
        synonyms.put("hamburger meat", "ground beef");
        synonyms.put("chicken fillet", "chicken breast");
        synonyms.put("yogurt", "yoghurt");
        synonyms.put("cheddar", "cheddar cheese");
        synonyms.put("full cream milk", "milk");
        // Pantry staples.
        synonyms.put("maize meal", "cornmeal");
        synonyms.put("mielie meal", "cornmeal");
        synonyms.put("polenta", "cornmeal");
        synonyms.put("cake flour", "flour");
        synonyms.put("all purpose flour", "flour");
        synonyms.put("white flour", "flour");
        synonyms.put("white sugar", "sugar");
        synonyms.put("castor sugar", "sugar");
        synonyms.put("granulated sugar", "sugar");
        synonyms.put("table salt", "salt");
        synonyms.put("sea salt", "salt");
        synonyms.put("pepper", "black pepper");
        synonyms.put("peppercorn", "black pepper");
        synonyms.put("chilli", "chili");
        synonyms.put("chile", "chili");
        synonyms.put("tomato puree", "tomato paste");
        synonyms.put("tomato concentrate", "tomato paste");
        synonyms.put("soya sauce", "soy sauce");
        synonyms.put("mayo", "mayonnaise");
        synonyms.put("stock cube", "vegetable stock");
        synonyms.put("vegetable broth", "vegetable stock");
        synonyms.put("veg stock", "vegetable stock");
        synonyms.put("cooking oil", "oil");
        synonyms.put("sunflower oil", "oil");
        synonyms.put("canola oil", "oil");
        synonyms.put("vegetable oil", "oil");
        synonyms.put("bread roll", "bread");
        synonyms.put("sliced bread", "bread");
        synonyms.put("baked bean", "baked beans");
        synonyms.put("tinned bean", "baked beans");
        synonyms.put("lentil", "lentils");
        synonyms.put("brown lentil", "lentils");
        synonyms.put("red lentil", "lentils");
        synonyms.put("white rice", "rice");
        synonyms.put("basmati rice", "rice");
        synonyms.put("long grain rice", "rice");
        synonyms.put("spaghetti", "pasta");
        synonyms.put("macaroni", "pasta");
        synonyms.put("penne", "pasta");
        synonyms.put("noodle", "pasta");
        synonyms.put("tortilla wrap", "tortilla");
        synonyms.put("wrap", "tortilla");
        synonyms.put("cocoa", "cocoa powder");
        SYNONYMS = Collections.unmodifiableMap(synonyms);
    }

    private IngredientNormalizer() {
        // Static helper only; never instantiated.
    }

    /**
     * Produces the lookup key for a raw ingredient name.
     *
     * @param raw whatever the user typed, may be {@code null}
     * @return a normalised key, or an empty string when nothing usable was supplied
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        // 1. Accents away ("jalapeno" with a tilde), then lower-case.
        // Locale.ROOT, not the device locale: these keys are internal identifiers, and in
        // a Turkish locale "I".toLowerCase() yields a dotless i, which would quietly
        // break matching for any ingredient containing a capital I.
        String working = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);

        // Anything bracketed is a note to the cook, not part of the name.
        working = working.replaceAll("\\(.*?\\)", " ");
        // Keep letters and spaces only, so "low-fat milk, 2%" loses its punctuation.
        working = working.replaceAll("[^a-z ]", " ");

        // 2 + 3. Drop descriptors, singularise whatever survives.
        LinkedHashSet<String> words = new LinkedHashSet<>();
        for (String word : working.split("\\s+")) {
            if (word.isEmpty() || DESCRIPTORS.contains(word)) {
                continue;
            }
            String singular = singularize(word);
            if (!singular.isEmpty() && !DESCRIPTORS.contains(singular)) {
                words.add(singular);
            }
        }

        String key = join(words);
        if (key.isEmpty()) {
            // Everything was stripped (the user typed only "fresh", say). Fall back to the
            // cleaned raw text so the item stays addressable instead of getting a blank key.
            return working.trim().replaceAll("\\s+", " ");
        }

        // 4. Collapse synonyms onto one spelling.
        String synonym = SYNONYMS.get(key);
        return synonym != null ? synonym : key;
    }

    /** Best-effort singular form of a single lower-case word. */
    static String singularize(String word) {
        String irregular = IRREGULAR_PLURALS.get(word);
        if (irregular != null) {
            return irregular;
        }
        // Short words stay as they are, and "-ss"/"-us" endings are not plurals
        // ("glass", "hummus"), so stripping the final letter would corrupt them.
        if (word.length() <= 3 || word.endsWith("ss") || word.endsWith("us")) {
            return word;
        }
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("oes") || word.endsWith("ches") || word.endsWith("shes")
                || word.endsWith("xes") || word.endsWith("zes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("ves") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "f";
        }
        if (word.endsWith("s")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    /**
     * Fuzzy fallback, used only when no exact key matched.
     *
     * <p>Two keys count as the same ingredient when either:</p>
     * <ul>
     *     <li>every word of the recipe's key also appears in the pantry key, so a pantry
     *         holding "chicken breast" satisfies a recipe asking for "chicken"; or</li>
     *     <li>the two keys are a single typo apart ("tomatoe" vs "tomato").</li>
     * </ul>
     *
     * <p>The word-subset test runs in one direction only, on purpose. Accepting the reverse
     * would let a pantry holding plain "oil" satisfy a recipe that calls for "olive oil",
     * and the app would then suggest a meal the user cannot actually cook - exactly the
     * failure the strict rule exists to prevent.</p>
     *
     * @param recipeKey normalised key the recipe requires
     * @param pantryKey normalised key of a row in the pantry
     */
    public static boolean looselyMatches(String recipeKey, String pantryKey) {
        if (recipeKey == null || pantryKey == null || recipeKey.isEmpty() || pantryKey.isEmpty()) {
            return false;
        }
        if (recipeKey.equals(pantryKey)) {
            return true;
        }

        Set<String> recipeWords = new LinkedHashSet<>(Arrays.asList(recipeKey.split(" ")));
        Set<String> pantryWords = new LinkedHashSet<>(Arrays.asList(pantryKey.split(" ")));
        if (pantryWords.containsAll(recipeWords)) {
            return true;
        }

        // Typo tolerance, but only for single words long enough that one edit cannot
        // flip the meaning (it must never let "oats" match "oils").
        if (recipeWords.size() == 1 && pantryWords.size() == 1) {
            int longest = Math.max(recipeKey.length(), pantryKey.length());
            return longest >= 6 && editDistanceWithin(recipeKey, pantryKey, 1);
        }
        return false;
    }

    /**
     * Levenshtein edit distance, short-circuited as soon as it exceeds {@code maxDistance}.
     *
     * <p>Only two rows of the matrix are kept alive: the full table is never needed, and the
     * method is called once per candidate pantry row during matching.</p>
     */
    static boolean editDistanceWithin(String left, String right, int maxDistance) {
        if (Math.abs(left.length() - right.length()) > maxDistance) {
            return false;
        }

        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }

        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            int bestInRow = current[0];
            for (int column = 1; column <= right.length(); column++) {
                int substitution = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + substitution);
                bestInRow = Math.min(bestInRow, current[column]);
            }
            if (bestInRow > maxDistance) {
                return false;
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()] <= maxDistance;
    }

    private static String join(Set<String> words) {
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(word);
        }
        return builder.toString();
    }

    /** Canonical spellings the app prefers, offered as hints on the add/edit form. */
    public static List<String> knownSynonymTargets() {
        return Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(SYNONYMS.values())));
    }
}
