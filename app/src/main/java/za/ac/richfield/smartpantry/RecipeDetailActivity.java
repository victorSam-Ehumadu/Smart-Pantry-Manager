package za.ac.richfield.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import za.ac.richfield.smartpantry.adapter.IngredientStatusAdapter;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.logic.RecipeMatcher;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatch;
import za.ac.richfield.smartpantry.util.AppPrefs;

/**
 * Screen 5: one recipe in full - ingredients, the method, and what the pantry covers.
 *
 * <p>Reached from either the Suggested Recipes screen or the recipe book. The launching
 * Intent carries only {@link #EXTRA_RECIPE_ID}; this screen loads the recipe from SQLite and
 * re-runs the matcher itself. Passing the id rather than a serialised object is the safer
 * contract - the pantry may have changed since the list was drawn, and the ingredient ticks
 * on this screen must reflect the pantry as it is now, not as it was then.</p>
 */
public class RecipeDetailActivity extends AppCompatActivity {

    /** Intent extra, in: which recipe to display. */
    public static final String EXTRA_RECIPE_ID = "za.ac.richfield.smartpantry.extra.RECIPE_ID";

    private IngredientStatusAdapter statusAdapter;
    private long recipeId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        statusAdapter = new IngredientStatusAdapter(this);
        RecyclerView ingredientList = findViewById(R.id.recyclerIngredients);
        ingredientList.setLayoutManager(new LinearLayoutManager(this));
        ingredientList.setAdapter(statusAdapter);
        ingredientList.setNestedScrollingEnabled(false);

        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1L);
        if (recipeId == -1L) {
            // Nothing sensible to show without an id; leave rather than render an empty shell.
            finish();
            return;
        }

        findViewById(R.id.buttonGoToPantry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                Intent intent = new Intent(RecipeDetailActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-matched on every resume, so editing the pantry and pressing Back updates the
        // ticks on this screen immediately.
        showRecipe();
    }

    private void showRecipe() {
        Recipe recipe = new RecipeRepository(this).getRecipeById(recipeId);
        if (recipe == null) {
            finish();
            return;
        }

        setTitle(recipe.getName());
        ((TextView) findViewById(R.id.textDetailName)).setText(recipe.getName());
        ((TextView) findViewById(R.id.textDetailMeta)).setText(getString(R.string.recipe_meta,
                recipe.getCategory(), recipe.getPrepMinutes(), recipe.getServings()));

        List<PantryItem> pantry =
                new PantryRepository(this).getAll(PantryRepository.SortOrder.NAME);
        RecipeMatcher matcher =
                new RecipeMatcher(pantry, new AppPrefs(this).isStrictQuantities());
        RecipeMatch match = matcher.match(recipe);

        bindVerdict(match);
        statusAdapter.submitList(match.getStatuses());
        bindSteps(recipe);
    }

    /** The banner at the top: can the user cook this right now, and if not, why not? */
    private void bindVerdict(RecipeMatch match) {
        TextView verdictView = findViewById(R.id.textDetailVerdict);
        View goToPantryButton = findViewById(R.id.buttonGoToPantry);

        if (match.isCookableNow()) {
            verdictView.setText(R.string.detail_can_cook);
            verdictView.setBackgroundResource(R.drawable.bg_verdict_ready);
            verdictView.setTextColor(ContextCompat.getColor(this, R.color.badge_ready_text));
            goToPantryButton.setVisibility(View.GONE);
            return;
        }

        List<String> outstanding = match.getOutstandingNames();
        verdictView.setText(getResources().getQuantityString(
                R.plurals.detail_cannot_cook, outstanding.size(),
                outstanding.size(), joinWithCommas(outstanding)));
        verdictView.setBackgroundResource(R.drawable.bg_verdict_missing);
        verdictView.setTextColor(ContextCompat.getColor(this, R.color.badge_almost_text));
        goToPantryButton.setVisibility(View.VISIBLE);
    }

    private void bindSteps(Recipe recipe) {
        TextView stepsView = findViewById(R.id.textDetailSteps);
        List<String> steps = recipe.getStepList();

        StringBuilder formatted = new StringBuilder();
        for (int index = 0; index < steps.size(); index++) {
            if (index > 0) {
                formatted.append("\n\n");
            }
            formatted.append(index + 1).append(". ").append(steps.get(index));
        }
        stepsView.setText(formatted.toString());
    }

    private String joinWithCommas(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                joined.append(index == values.size() - 1
                        ? getString(R.string.join_and) : ", ");
            }
            joined.append(values.get(index));
        }
        return joined.toString();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Plain finish() returns to whichever list opened this screen, which is what the
            // user expects; navigateUpTo() would always force them to the Suggested screen.
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
