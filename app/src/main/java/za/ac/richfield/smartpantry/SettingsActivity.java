package za.ac.richfield.smartpantry;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import za.ac.richfield.smartpantry.data.DatabaseHelper;
import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.util.AppPrefs;

/**
 * Screen 6: Settings - the preferences screen required by Section 2.2 / 3.1.
 *
 * <p>Four switches and a slider, all persisted through {@link AppPrefs}, plus two
 * maintenance actions. Every setting here visibly changes behaviour elsewhere in the app
 * rather than being decoration:</p>
 *
 * <ul>
 *     <li>expiring-soon alerts and the day window drive the pantry banner and row tinting;</li>
 *     <li>strict quantity checking switches the matcher between "name and amount" and
 *         "name only", which is a neat way to show what the quantity half of the rule does;</li>
 *     <li>the Almost There switch shows or hides the optional bonus section.</li>
 * </ul>
 *
 * <p>Preferences are written the moment they change, so there is no Save button to forget
 * and nothing is lost if the user leaves by pressing Back.</p>
 */
public class SettingsActivity extends BaseActivity {

    private AppPrefs prefs;
    private PantryRepository pantryRepository;

    private TextView expiryWindowLabel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new AppPrefs(this);
        pantryRepository = new PantryRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.title_settings);

        bindExpirySettings();
        bindMatchingSettings();
        bindMaintenanceActions();
        bindAboutSection();

        setUpBottomNavigation();
    }

    @Override
    protected int getNavigationMenuItemId() {
        return R.id.nav_settings;
    }

    private void bindExpirySettings() {
        final MaterialSwitch alertsSwitch = findViewById(R.id.switchExpiryAlerts);
        final Slider windowSlider = findViewById(R.id.sliderExpiryWindow);
        final View windowRow = findViewById(R.id.rowExpiryWindow);
        expiryWindowLabel = findViewById(R.id.textExpiryWindowValue);

        alertsSwitch.setChecked(prefs.isExpiryAlertsEnabled());
        windowSlider.setValue(prefs.getExpiryWindowDays());
        updateExpiryWindowLabel(prefs.getExpiryWindowDays());
        // The day window is meaningless with alerts off, so it is hidden rather than left
        // enabled and ignored.
        windowRow.setVisibility(alertsSwitch.isChecked() ? View.VISIBLE : View.GONE);

        alertsSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            prefs.setExpiryAlertsEnabled(isChecked);
            windowRow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        windowSlider.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            prefs.setExpiryWindowDays(days);
            updateExpiryWindowLabel(days);
        });
    }

    private void updateExpiryWindowLabel(int days) {
        expiryWindowLabel.setText(getResources().getQuantityString(
                R.plurals.settings_expiry_window_value, days, days));
    }

    private void bindMatchingSettings() {
        MaterialSwitch strictSwitch = findViewById(R.id.switchStrictQuantities);
        strictSwitch.setChecked(prefs.isStrictQuantities());
        strictSwitch.setOnCheckedChangeListener((button, isChecked) ->
                prefs.setStrictQuantities(isChecked));

        MaterialSwitch almostSwitch = findViewById(R.id.switchAlmostThere);
        almostSwitch.setChecked(prefs.isShowAlmostThere());
        almostSwitch.setOnCheckedChangeListener((button, isChecked) ->
                prefs.setShowAlmostThere(isChecked));
    }

    private void bindMaintenanceActions() {
        findViewById(R.id.rowClearPantry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                confirmClearPantry();
            }
        });

        findViewById(R.id.rowRestoreRecipes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                confirmRestoreRecipes();
            }
        });
    }

    /** Wiping the pantry is irreversible, so it takes a deliberate confirmation. */
    private void confirmClearPantry() {
        final int itemCount = pantryRepository.count();
        if (itemCount == 0) {
            Snackbar.make(findViewById(R.id.settingsRoot), R.string.msg_pantry_already_empty,
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_clear_pantry_title)
                .setMessage(getResources().getQuantityString(
                        R.plurals.dialog_clear_pantry_message, itemCount, itemCount))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_clear, (dialog, which) -> {
                    int removed = pantryRepository.deleteAll();
                    Snackbar.make(findViewById(R.id.settingsRoot),
                            getResources().getQuantityString(
                                    R.plurals.msg_pantry_cleared, removed, removed),
                            Snackbar.LENGTH_LONG).show();
                })
                .show();
    }

    private void confirmRestoreRecipes() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_restore_recipes_title)
                .setMessage(R.string.dialog_restore_recipes_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_restore, (dialog, which) -> {
                    DatabaseHelper.getInstance(this).reseedRecipes();
                    bindAboutSection();
                    Snackbar.make(findViewById(R.id.settingsRoot),
                            R.string.msg_recipes_restored, Snackbar.LENGTH_LONG).show();
                })
                .show();
    }

    /** Live counts, so the screen also doubles as proof that the database holds real rows. */
    private void bindAboutSection() {
        int pantryCount = pantryRepository.count();
        int recipeCount = new RecipeRepository(this).count();

        ((TextView) findViewById(R.id.textStorageSummary)).setText(
                getString(R.string.settings_storage_summary, pantryCount, recipeCount));
        ((TextView) findViewById(R.id.textAboutBody)).setText(
                getString(R.string.settings_about_body, DatabaseHelper.DB_NAME));
    }
}
