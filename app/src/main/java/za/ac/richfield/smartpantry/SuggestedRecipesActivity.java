package za.ac.richfield.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import za.ac.richfield.smartpantry.adapter.RecipeMatchAdapter;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.logic.RecipeMatcher;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeMatch;
import za.ac.richfield.smartpantry.util.AppPrefs;

/**
 * Screen 3: Suggested Recipes - the screen the strict-matching rule exists for.
 *
 * <p>Everything here is recomputed in {@code onResume()}: the pantry is read back out of
 * SQLite, a fresh {@link RecipeMatcher} is built from it, and the recipe book is run through
 * it. That is deliberately the cheapest correct design for this data size (one pantry query,
 * two recipe queries, then pure in-memory work), and it is what makes the demonstration in
 * the video work - delete an ingredient on the pantry screen, come back, and the recipe that
 * needed it has gone.</p>
 *
 * <p>The two lists are kept physically separate, as Section 2.3 requires. The top list is
 * the strict result and contains only recipes where every ingredient is covered. The
 * "Almost There" list below it is the optional bonus bucket, under its own heading, and
 * nothing from it can ever leak upwards - they come from two different matcher methods.</p>
 */
public class SuggestedRecipesActivity extends BaseActivity
        implements RecipeMatchAdapter.Listener {

    private static final String TAG = "SuggestedRecipes";

    private PantryRepository pantryRepository;
    private RecipeRepository recipeRepository;
    private AppPrefs prefs;

    private RecipeMatchAdapter readyAdapter;
    private RecipeMatchAdapter almostAdapter;

    private TextView headerView;
    private TextView readyHeadingView;
    private View almostSection;
    private TextView almostHeadingView;
    private View emptyStateView;
    private TextView emptyStateMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        pantryRepository = new PantryRepository(this);
        recipeRepository = new RecipeRepository(this);
        prefs = new AppPrefs(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_suggested);

        headerView = findViewById(R.id.textSuggestionHeader);
        readyHeadingView = findViewById(R.id.textReadyHeading);
        almostSection = findViewById(R.id.sectionAlmost);
        almostHeadingView = findViewById(R.id.textAlmostHeading);
        emptyStateView = findViewById(R.id.emptyState);
        emptyStateMessage = findViewById(R.id.textEmptyMessage);

        readyAdapter = new RecipeMatchAdapter(this, RecipeMatchAdapter.Mode.READY, this);
        RecyclerView readyList = findViewById(R.id.recyclerReady);
        readyList.setLayoutManager(new LinearLayoutManager(this));
        readyList.setAdapter(readyAdapter);
        // Both lists sit inside one scrolling container, so each must stop trying to scroll
        // itself; otherwise the inner list swallows the gesture and the page cannot move.
        readyList.setNestedScrollingEnabled(false);

        almostAdapter = new RecipeMatchAdapter(this, RecipeMatchAdapter.Mode.ALMOST, this);
        RecyclerView almostList = findViewById(R.id.recyclerAlmost);
        almostList.setLayoutManager(new LinearLayoutManager(this));
        almostList.setAdapter(almostAdapter);
        almostList.setNestedScrollingEnabled(false);

        findViewById(R.id.buttonEmptyAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                startActivity(new Intent(SuggestedRecipesActivity.this, MainActivity.class));
            }
        });

        setUpBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The pantry may have changed on another screen, so the whole match is redone.
        runStrictMatching();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_suggested;
    }

    /**
     * Reads the pantry, runs the strict rule over the recipe book and renders both lists.
     */
    private void runStrictMatching() {
        List<PantryItem> pantry = pantryRepository.getAll(PantryRepository.SortOrder.NAME);
        List<Recipe> recipeBook = recipeRepository.getAllRecipesWithIngredients();

        RecipeMatcher matcher = new RecipeMatcher(pantry, prefs.isStrictQuantities());
        List<RecipeMatch> ready = matcher.suggestions(recipeBook);
        List<RecipeMatch> almost = prefs.isShowAlmostThere()
                ? matcher.almostThere(recipeBook)
                : null;

        Log.d(TAG, "Matched " + ready.size() + " of " + recipeBook.size()
                + " recipes against " + pantry.size() + " pantry rows");

        headerView.setText(getResources().getQuantityString(
                R.plurals.suggestion_header, pantry.size(), pantry.size(), recipeBook.size()));

        readyAdapter.submitList(ready);
        readyHeadingView.setText(getString(R.string.heading_ready_count, ready.size()));
        readyHeadingView.setVisibility(ready.isEmpty() ? View.GONE : View.VISIBLE);

        if (almost == null || almost.isEmpty()) {
            almostSection.setVisibility(View.GONE);
        } else {
            almostSection.setVisibility(View.VISIBLE);
            almostHeadingView.setText(getString(R.string.heading_almost_count, almost.size()));
            almostAdapter.submitList(almost);
        }

        updateEmptyState(ready.isEmpty(), pantry.isEmpty(),
                almost != null && !almost.isEmpty());
    }

    /**
     * The "zero matches" feedback required by Section 2.2 - never a blank screen.
     *
     * <p>The message is tailored, because an empty pantry and a stocked pantry that still
     * matches nothing call for different advice.</p>
     */
    private void updateEmptyState(boolean noStrictMatches, boolean pantryEmpty,
                                  boolean hasAlmostMatches) {
        if (!noStrictMatches) {
            emptyStateView.setVisibility(View.GONE);
            return;
        }

        emptyStateView.setVisibility(View.VISIBLE);
        if (pantryEmpty) {
            emptyStateMessage.setText(R.string.empty_suggestions_no_pantry);
        } else if (hasAlmostMatches) {
            emptyStateMessage.setText(R.string.empty_suggestions_almost);
        } else {
            emptyStateMessage.setText(R.string.empty_suggestions_message);
        }
    }

    @Override
    public void onRecipeClicked(RecipeMatch match) {
        // Only the recipe id crosses the Intent boundary. The detail screen re-reads the
        // recipe and re-runs the match itself, so what it shows is always current.
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, match.getRecipe().getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_suggested, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_how_it_works) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_rule_title)
                    .setMessage(R.string.dialog_rule_message)
                    .setPositiveButton(R.string.action_got_it, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
