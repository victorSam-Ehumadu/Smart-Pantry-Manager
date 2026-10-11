package za.ac.richfield.smartpantry;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import za.ac.richfield.smartpantry.adapter.PantryAdapter;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.AppPrefs;

/**
 * Screen 1: the pantry list - everything the user currently has at home.
 *
 * <p>This is the launcher Activity and the "R" of CRUD: it reads the pantry table through
 * {@link PantryRepository} and renders it with {@link PantryAdapter}. Create and Update are
 * delegated to {@link AddEditIngredientActivity} via an explicit Intent, and Delete happens
 * here behind a confirmation dialog with an undo option.</p>
 *
 * <p><b>Why the reload lives in {@code onResume()}:</b> the pantry can change while this
 * screen is stopped - the user may have added an ingredient on the add form, or cleared the
 * pantry from Settings. {@code onCreate()} runs only once per instance, so a list loaded
 * there would quietly go stale. {@code onResume()} runs every single time the screen becomes
 * interactive again, which is exactly when the data on screen has to be true. It is also why
 * closing and reopening the app shows the saved pantry: the rows are read back out of SQLite,
 * never held in memory.</p>
 */
public class MainActivity extends BaseActivity implements PantryAdapter.Listener {

    /** Lifecycle logging tag. Filter Logcat on this during the video to narrate the flow. */
    private static final String TAG = "PantryLifecycle";

    private PantryRepository pantryRepository;
    private AppPrefs prefs;

    private PantryAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyStateView;
    private TextView emptyStateMessage;
    private TextView expiringBanner;
    private TextView summaryView;

    /** Current search text, or {@code null} when the user is not searching. */
    @Nullable
    private String searchQuery;

    /**
     * Replaces the deprecated {@code startActivityForResult}. The launcher is registered
     * once, during {@code onCreate()}, and survives configuration changes; the callback fires
     * when the add/edit screen finishes so this screen can confirm what happened.
     */
    private final ActivityResultLauncher<Intent> addEditLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            handleAddEditResult(result);
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate - building the pantry screen");
        setContentView(R.layout.activity_main);

        pantryRepository = new PantryRepository(this);
        prefs = new AppPrefs(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_pantry);

        recyclerView = findViewById(R.id.recyclerPantry);
        emptyStateView = findViewById(R.id.emptyState);
        emptyStateMessage = findViewById(R.id.textEmptyMessage);
        expiringBanner = findViewById(R.id.textExpiringBanner);
        summaryView = findViewById(R.id.textPantrySummary);

        adapter = new PantryAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        FloatingActionButton addButton = findViewById(R.id.fabAddItem);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                openAddForm();
            }
        });

        findViewById(R.id.buttonEmptyAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                openAddForm();
            }
        });

        setUpBottomNavigation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart - screen is visible");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-read from SQLite on every resume: see the class comment above.
        Log.d(TAG, "onResume - reloading the pantry from SQLite");
        loadPantry();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause - screen is losing focus");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop - screen is no longer visible");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - Activity instance is being torn down");
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_pantry;
    }

    // ------------------------------------------------------------------ data

    private void loadPantry() {
        PantryRepository.SortOrder sortOrder = prefs.getSortOrder();
        List<PantryItem> items = searchQuery == null
                ? pantryRepository.getAll(sortOrder)
                : pantryRepository.search(searchQuery, sortOrder);

        adapter.setExpiryHighlighting(prefs.isExpiryAlertsEnabled(), prefs.getExpiryWindowDays());
        adapter.submitList(items);

        updateSummary(items.size());
        updateEmptyState(items.isEmpty());
        updateExpiringBanner();
    }

    private void updateSummary(int shownCount) {
        if (searchQuery != null) {
            summaryView.setVisibility(View.VISIBLE);
            summaryView.setText(getResources().getQuantityString(
                    R.plurals.pantry_search_summary, shownCount, shownCount, searchQuery));
            return;
        }
        if (shownCount == 0) {
            summaryView.setVisibility(View.GONE);
            return;
        }
        summaryView.setVisibility(View.VISIBLE);
        summaryView.setText(getResources().getQuantityString(
                R.plurals.pantry_summary, shownCount, shownCount));
    }

    /**
     * Shows a helpful message instead of a blank screen when there is nothing to list
     * (Section 2.2). The copy differs for "pantry is empty" and "search found nothing",
     * because the two situations need different actions from the user.
     */
    private void updateEmptyState(boolean isEmpty) {
        if (!isEmpty) {
            emptyStateView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            return;
        }

        emptyStateView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateMessage.setText(searchQuery == null
                ? getString(R.string.empty_pantry_message)
                : getString(R.string.empty_search_message, searchQuery));
        findViewById(R.id.buttonEmptyAction).setVisibility(
                searchQuery == null ? View.VISIBLE : View.GONE);
    }

    /** One-line warning above the list when something is about to go off. */
    private void updateExpiringBanner() {
        if (!prefs.isExpiryAlertsEnabled()) {
            expiringBanner.setVisibility(View.GONE);
            return;
        }

        int window = prefs.getExpiryWindowDays();
        int expiringCount = pantryRepository.countExpiringWithin(window);
        if (expiringCount == 0) {
            expiringBanner.setVisibility(View.GONE);
            return;
        }

        expiringBanner.setVisibility(View.VISIBLE);
        expiringBanner.setText(getResources().getQuantityString(
                R.plurals.pantry_expiring_banner, expiringCount, expiringCount, window));
    }

    // ----------------------------------------------------------- navigation

    /** Create: opens the form with no item id, so it inserts. */
    private void openAddForm() {
        Intent intent = new Intent(this, AddEditIngredientActivity.class);
        addEditLauncher.launch(intent);
    }

    /**
     * Update: opens the same form, passing the row id as an Intent extra.
     *
     * <p>Only the id travels in the Intent, not the whole object. The form then loads the
     * current row from SQLite itself, so it can never show a stale copy of a record that
     * changed while this screen was in the background.</p>
     */
    private void openEditForm(PantryItem item) {
        Intent intent = new Intent(this, AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_ITEM_ID, item.getId());
        addEditLauncher.launch(intent);
    }

    private void handleAddEditResult(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            return;
        }
        String message = result.getData()
                .getStringExtra(AddEditIngredientActivity.EXTRA_RESULT_MESSAGE);
        if (message != null && !message.isEmpty()) {
            Snackbar.make(findViewById(R.id.mainRoot), message, Snackbar.LENGTH_LONG).show();
        }
        // onResume() runs straight after this callback and reloads the list, so there is no
        // need to touch the adapter here.
    }

    // ------------------------------------------------------- adapter callbacks

    @Override
    public void onItemClicked(PantryItem item) {
        openEditForm(item);
    }

    @Override
    public void onDeleteClicked(final PantryItem item) {
        // Deleting is destructive and one tap away, so it always asks first.
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, item.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> deleteItem(item))
                .show();
    }

    /** Delete, with an undo that re-inserts the row the user just removed. */
    private void deleteItem(final PantryItem item) {
        int deletedRows = pantryRepository.delete(item.getId());
        if (deletedRows == 0) {
            Snackbar.make(findViewById(R.id.mainRoot), R.string.msg_delete_failed,
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        loadPantry();
        Snackbar.make(findViewById(R.id.mainRoot),
                        getString(R.string.msg_item_deleted, item.getName()),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View clicked) {
                        // A new id is assigned on re-insert; the row's contents are what matter.
                        item.setId(PantryItem.NO_ID);
                        pantryRepository.insert(item);
                        loadPantry();
                    }
                })
                .show();
    }

    // ------------------------------------------------------------- toolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pantry, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.hint_search_pantry));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Treat an empty box as "not searching" rather than as a search for "".
                    searchQuery = newText == null || newText.trim().isEmpty()
                            ? null : newText.trim();
                    loadPantry();
                    return true;
                }
            });
        }

        tickCurrentSortOrder(menu);
        return true;
    }

    private void tickCurrentSortOrder(Menu menu) {
        PantryRepository.SortOrder current = prefs.getSortOrder();
        MenuItem toTick;
        switch (current) {
            case EXPIRY:
                toTick = menu.findItem(R.id.action_sort_expiry);
                break;
            case RECENT:
                toTick = menu.findItem(R.id.action_sort_recent);
                break;
            case NAME:
            default:
                toTick = menu.findItem(R.id.action_sort_name);
                break;
        }
        if (toTick != null) {
            toTick.setChecked(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int selectedId = item.getItemId();

        if (selectedId == R.id.action_sort_name) {
            applySortOrder(PantryRepository.SortOrder.NAME, item);
            return true;
        }
        if (selectedId == R.id.action_sort_expiry) {
            applySortOrder(PantryRepository.SortOrder.EXPIRY, item);
            return true;
        }
        if (selectedId == R.id.action_sort_recent) {
            applySortOrder(PantryRepository.SortOrder.RECENT, item);
            return true;
        }
        if (selectedId == R.id.action_view_suggestions) {
            startActivity(new Intent(this, SuggestedRecipesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applySortOrder(PantryRepository.SortOrder sortOrder, MenuItem item) {
        prefs.setSortOrder(sortOrder);
        item.setChecked(true);
        loadPantry();
    }
}
