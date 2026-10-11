package za.ac.richfield.smartpantry;

import android.content.Intent;
import android.os.Build;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationBarView;

/**
 * Shared bottom-navigation plumbing for the four top-level screens.
 *
 * <p>The app navigates with explicit {@link Intent}s rather than a Navigation-component
 * graph, which is what Section 3.1 of the brief asks to see. Putting the wiring in one base
 * class means the Pantry, Suggestions, Recipes and Settings screens each only declare which
 * tab they are, instead of repeating the same four-way switch.</p>
 *
 * <p>Two details keep the back stack sane. {@code FLAG_ACTIVITY_CLEAR_TOP} combined with
 * {@code FLAG_ACTIVITY_SINGLE_TOP} reuses an existing instance of the target screen rather
 * than piling up a new copy on every tab tap, so Back always leaves the app from a top-level
 * tab instead of walking backwards through a long navigation history. The zero-length
 * transition then stops the tab switch from looking like a push onto a stack.</p>
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Which bottom-navigation item represents this screen. */
    @IdRes
    protected abstract int getNavigationMenuItemId();

    /**
     * Wires up the bottom navigation bar for this screen.
     *
     * <p>Called from {@code onCreate()} AFTER {@code setContentView()}, because it looks the
     * view up in the layout that was just inflated.</p>
     */
    protected void setUpBottomNavigation() {
        NavigationBarView navigationBar = findViewById(R.id.bottomNavigation);
        if (navigationBar == null) {
            // The add/edit and detail screens are not tabs and have no navigation bar.
            return;
        }

        navigationBar.setSelectedItemId(getNavigationMenuItemId());
        navigationBar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int selectedId = item.getItemId();
                if (selectedId == getNavigationMenuItemId()) {
                    // Already here; swallow the tap so the screen does not reload itself.
                    return true;
                }
                Class<?> target = activityForMenuItem(selectedId);
                if (target == null) {
                    return false;
                }
                navigateTo(target);
                return true;
            }
        });
    }

    @Nullable
    private Class<?> activityForMenuItem(@IdRes int menuItemId) {
        if (menuItemId == R.id.nav_pantry) {
            return MainActivity.class;
        }
        if (menuItemId == R.id.nav_suggested) {
            return SuggestedRecipesActivity.class;
        }
        if (menuItemId == R.id.nav_recipes) {
            return RecipeBrowseActivity.class;
        }
        if (menuItemId == R.id.nav_settings) {
            return SettingsActivity.class;
        }
        return null;
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        suppressTransitionAnimation();
    }

    /**
     * Removes the open/close animation from a tab switch, so moving between tabs does not
     * look like pushing another screen onto a stack.
     *
     * <p>Android 14 (API 34) replaced {@code overridePendingTransition()} with
     * {@code overrideActivityTransition()}. The replacement does not exist on older
     * releases and this app supports API 24 upwards, so both paths are kept and chosen at
     * runtime rather than calling the deprecated method unconditionally.</p>
     */
    @SuppressWarnings("deprecation")
    private void suppressTransitionAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
        } else {
            overridePendingTransition(0, 0);
        }
    }
}
