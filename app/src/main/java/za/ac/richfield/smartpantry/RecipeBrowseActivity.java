package za.ac.richfield.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import za.ac.richfield.smartpantry.adapter.RecipeAdapter;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.model.Recipe;

/**
 * Screen 4: the full recipe book, with no matching applied.
 *
 * <p>Useful in its own right - the user can read any recipe and see what it would take - and
 * useful as a control for the demonstration: this screen always lists all 20 seeded recipes,
 * so the difference between it and the Suggested screen is exactly what the strict rule
 * filtered out.</p>
 */
public class RecipeBrowseActivity extends BaseActivity implements RecipeAdapter.Listener {

    private RecipeRepository recipeRepository;
    private RecipeAdapter adapter;
    private TextView headerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_browse);

        recipeRepository = new RecipeRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_recipes);

        headerView = findViewById(R.id.textRecipeBookHeader);

        adapter = new RecipeAdapter(this, this);
        RecyclerView list = findViewById(R.id.recyclerRecipes);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        list.setAdapter(adapter);

        setUpBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reloaded here rather than in onCreate() so that restoring the recipe book from
        // Settings is reflected the moment the user comes back to this tab.
        loadRecipes();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_recipes;
    }

    private void loadRecipes() {
        List<Recipe> recipes = recipeRepository.getAllRecipesWithIngredients();
        adapter.submitList(recipes);
        headerView.setText(getResources().getQuantityString(
                R.plurals.recipe_book_header, recipes.size(), recipes.size()));

        View emptyState = findViewById(R.id.emptyState);
        emptyState.setVisibility(recipes.isEmpty() ? View.VISIBLE : View.GONE);
        if (recipes.isEmpty()) {
            // Only reachable if the book was cleared; Settings can seed it again.
            ((TextView) findViewById(R.id.textEmptyMessage))
                    .setText(R.string.empty_recipe_book_message);
            findViewById(R.id.buttonEmptyAction).setVisibility(View.GONE);
        }
    }

    @Override
    public void onRecipeClicked(Recipe recipe) {
        Intent intent = new Intent(this, RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
