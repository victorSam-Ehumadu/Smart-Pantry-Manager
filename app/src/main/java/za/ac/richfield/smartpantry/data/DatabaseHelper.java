package za.ac.richfield.smartpantry.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

import za.ac.richfield.smartpantry.logic.IngredientNormalizer;
import za.ac.richfield.smartpantry.logic.UnitConverter;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * The app's single {@link SQLiteOpenHelper}: schema, upgrades and first-run seeding.
 *
 * <p>SQLite was chosen over Firebase and PostgreSQL because the Smart Pantry Manager is a
 * single-user, offline-first app. The pantry only ever matters on the device standing in the
 * kitchen, the strict-matching rule runs against the whole pantry at once (a local query,
 * not twenty network round trips), and the app declares no INTERNET permission at all.</p>
 *
 * <p>Three tables, deliberately normalised rather than one table of comma-separated
 * ingredient strings:</p>
 *
 * <pre>
 *   pantry_items (_id, name, name_key, quantity, unit, expiry_date, created_at)
 *   recipes      (_id, name, category, prep_minutes, servings, steps)
 *        |
 *        | 1 : many  (ON DELETE CASCADE)
 *        v
 *   recipe_ingredients (_id, recipe_id, name, name_key, quantity, unit)
 * </pre>
 *
 * <p>Splitting the ingredients into their own table is what makes the strict rule possible:
 * the matcher needs a quantity and a unit per ingredient, which a single text column could
 * not give it.</p>
 *
 * <p>The class is a singleton. One writable database per process avoids the
 * {@code SQLiteDatabaseLockedException} that comes from several helper instances opening the
 * same file, and the connection is intentionally never closed - Android closes it when the
 * process dies.</p>
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    public static final String DB_NAME = "smart_pantry.db";
    public static final int DB_VERSION = 1;

    // ---- pantry_items -----------------------------------------------------------------
    public static final String T_PANTRY = "pantry_items";
    public static final String C_ID = "_id";
    public static final String C_PANTRY_NAME = "name";
    public static final String C_PANTRY_KEY = "name_key";
    public static final String C_PANTRY_QUANTITY = "quantity";
    public static final String C_PANTRY_UNIT = "unit";
    public static final String C_PANTRY_EXPIRY = "expiry_date";
    public static final String C_PANTRY_CREATED = "created_at";

    // ---- recipes ----------------------------------------------------------------------
    public static final String T_RECIPES = "recipes";
    public static final String C_RECIPE_NAME = "name";
    public static final String C_RECIPE_CATEGORY = "category";
    public static final String C_RECIPE_PREP = "prep_minutes";
    public static final String C_RECIPE_SERVINGS = "servings";
    public static final String C_RECIPE_STEPS = "steps";

    // ---- recipe_ingredients -----------------------------------------------------------
    public static final String T_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String C_RI_RECIPE_ID = "recipe_id";
    public static final String C_RI_NAME = "name";
    public static final String C_RI_KEY = "name_key";
    public static final String C_RI_QUANTITY = "quantity";
    public static final String C_RI_UNIT = "unit";

    private static final String SQL_CREATE_PANTRY =
            "CREATE TABLE " + T_PANTRY + " ("
                    + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + C_PANTRY_NAME + " TEXT NOT NULL, "
                    + C_PANTRY_KEY + " TEXT NOT NULL, "
                    + C_PANTRY_QUANTITY + " REAL NOT NULL DEFAULT 0, "
                    + C_PANTRY_UNIT + " TEXT NOT NULL DEFAULT '', "
                    + C_PANTRY_EXPIRY + " TEXT, "
                    + C_PANTRY_CREATED + " INTEGER NOT NULL DEFAULT 0)";

    private static final String SQL_CREATE_RECIPES =
            "CREATE TABLE " + T_RECIPES + " ("
                    + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + C_RECIPE_NAME + " TEXT NOT NULL, "
                    + C_RECIPE_CATEGORY + " TEXT NOT NULL DEFAULT '', "
                    + C_RECIPE_PREP + " INTEGER NOT NULL DEFAULT 0, "
                    + C_RECIPE_SERVINGS + " INTEGER NOT NULL DEFAULT 1, "
                    + C_RECIPE_STEPS + " TEXT NOT NULL DEFAULT '')";

    private static final String SQL_CREATE_RECIPE_INGREDIENTS =
            "CREATE TABLE " + T_RECIPE_INGREDIENTS + " ("
                    + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + C_RI_RECIPE_ID + " INTEGER NOT NULL, "
                    + C_RI_NAME + " TEXT NOT NULL, "
                    + C_RI_KEY + " TEXT NOT NULL, "
                    + C_RI_QUANTITY + " REAL NOT NULL DEFAULT 0, "
                    + C_RI_UNIT + " TEXT NOT NULL DEFAULT '', "
                    + "FOREIGN KEY (" + C_RI_RECIPE_ID + ") REFERENCES "
                    + T_RECIPES + "(" + C_ID + ") ON DELETE CASCADE)";

    // Matching looks ingredients up by their normalised key, so both keys get an index.
    private static final String SQL_INDEX_PANTRY_KEY =
            "CREATE INDEX idx_pantry_key ON " + T_PANTRY + "(" + C_PANTRY_KEY + ")";
    private static final String SQL_INDEX_RI_KEY =
            "CREATE INDEX idx_recipe_ingredient_key ON "
                    + T_RECIPE_INGREDIENTS + "(" + C_RI_KEY + ")";
    private static final String SQL_INDEX_RI_RECIPE =
            "CREATE INDEX idx_recipe_ingredient_recipe ON "
                    + T_RECIPE_INGREDIENTS + "(" + C_RI_RECIPE_ID + ")";

    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        // The application context is used so holding the singleton can never leak an Activity.
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // SQLite ignores foreign keys unless they are switched on per connection. Without
        // this, deleting a recipe would leave its ingredient rows behind as orphans.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_PANTRY);
        db.execSQL(SQL_CREATE_RECIPES);
        db.execSQL(SQL_CREATE_RECIPE_INGREDIENTS);
        db.execSQL(SQL_INDEX_PANTRY_KEY);
        db.execSQL(SQL_INDEX_RI_KEY);
        db.execSQL(SQL_INDEX_RI_RECIPE);

        // The recipe book ships with the app, so it is written on first run. The pantry is
        // left empty on purpose: a new user should see the empty-state message and add their
        // own leftovers, which is the whole point of the app.
        seedRecipes(db);
        Log.i(TAG, "Database created and recipe book seeded");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only the recipe book is app-owned data, but a destructive upgrade would also wipe
        // the user's pantry, so future versions should migrate instead of dropping. At
        // version 1 there is nothing to migrate from; this branch exists so the behaviour is
        // explicit rather than an empty override.
        Log.w(TAG, "Upgrading database from " + oldVersion + " to " + newVersion
                + "; rebuilding the recipe book and keeping the pantry");
        db.execSQL("DROP TABLE IF EXISTS " + T_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_RECIPES);
        db.execSQL(SQL_CREATE_RECIPES);
        db.execSQL(SQL_CREATE_RECIPE_INGREDIENTS);
        db.execSQL(SQL_INDEX_RI_KEY);
        db.execSQL(SQL_INDEX_RI_RECIPE);
        seedRecipes(db);
    }

    /**
     * Writes the bundled recipe book, wiping any existing copy first.
     *
     * <p>Public so the Settings screen can offer a "restore recipe book" action, which is
     * also a convenient way to show the seeding code running during the video.</p>
     */
    public void reseedRecipes() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_RECIPE_INGREDIENTS, null, null);
        db.delete(T_RECIPES, null, null);
        seedRecipes(db);
    }

    /**
     * Inserts every bundled recipe and its ingredient rows inside one transaction.
     *
     * <p>A transaction matters here for two reasons: roughly 120 inserts outside one would
     * each get their own disk commit and make first launch visibly slow, and a crash
     * half-way through would otherwise leave a partly-seeded recipe book behind.</p>
     */
    private void seedRecipes(SQLiteDatabase db) {
        List<Recipe> recipes = RecipeSeedData.buildRecipeBook();
        db.beginTransaction();
        try {
            for (Recipe recipe : recipes) {
                ContentValues recipeValues = new ContentValues();
                recipeValues.put(C_RECIPE_NAME, recipe.getName());
                recipeValues.put(C_RECIPE_CATEGORY, recipe.getCategory());
                recipeValues.put(C_RECIPE_PREP, recipe.getPrepMinutes());
                recipeValues.put(C_RECIPE_SERVINGS, recipe.getServings());
                recipeValues.put(C_RECIPE_STEPS, recipe.getSteps());
                long recipeId = db.insert(T_RECIPES, null, recipeValues);

                for (RecipeIngredient ingredient : recipe.getIngredients()) {
                    ContentValues ingredientValues = new ContentValues();
                    ingredientValues.put(C_RI_RECIPE_ID, recipeId);
                    ingredientValues.put(C_RI_NAME, ingredient.getName());
                    // The key is computed once here, at write time, so the matcher never has
                    // to normalise the same recipe text again on every screen refresh.
                    ingredientValues.put(C_RI_KEY,
                            IngredientNormalizer.normalize(ingredient.getName()));
                    ingredientValues.put(C_RI_QUANTITY, ingredient.getQuantity());
                    ingredientValues.put(C_RI_UNIT,
                            UnitConverter.canonicalUnit(ingredient.getUnit()));
                    db.insert(T_RECIPE_INGREDIENTS, null, ingredientValues);
                }
            }
            db.setTransactionSuccessful();
            Log.i(TAG, "Seeded " + recipes.size() + " recipes");
        } finally {
            db.endTransaction();
        }
    }
}
