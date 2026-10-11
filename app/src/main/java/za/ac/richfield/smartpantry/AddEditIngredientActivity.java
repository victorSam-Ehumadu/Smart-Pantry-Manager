package za.ac.richfield.smartpantry;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import za.ac.richfield.smartpantry.data.PantryRepository;
import za.ac.richfield.smartpantry.data.RecipeRepository;
import za.ac.richfield.smartpantry.logic.IngredientNormalizer;
import za.ac.richfield.smartpantry.logic.UnitConverter;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.DateUtils;

/**
 * Screen 2: the add / edit ingredient form - the Create and Update half of CRUD.
 *
 * <p>One Activity serves both jobs. Which one it is doing depends entirely on whether the
 * launching Intent carried an {@link #EXTRA_ITEM_ID}: with an id the form loads that row and
 * updates it, without one it inserts a new row. Sharing the screen means the validation
 * rules exist in exactly one place instead of being duplicated and drifting apart.</p>
 *
 * <p>The result travels back to the pantry list through {@code setResult()} with a short
 * confirmation message, which the list then shows as a Snackbar.</p>
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    /** Intent extra, in: the pantry row to edit. Absent means "create a new item". */
    public static final String EXTRA_ITEM_ID = "za.ac.richfield.smartpantry.extra.ITEM_ID";
    /** Intent extra, out: the confirmation text for the caller to display. */
    public static final String EXTRA_RESULT_MESSAGE =
            "za.ac.richfield.smartpantry.extra.RESULT_MESSAGE";

    /** Guard rail on the quantity field; a pantry item above this is a typo, not a shopping list. */
    private static final double MAX_QUANTITY = 100000d;

    private PantryRepository pantryRepository;

    private TextInputLayout nameLayout;
    private AutoCompleteTextView nameInput;
    private TextInputLayout quantityLayout;
    private TextInputEditText quantityInput;
    private TextInputLayout unitLayout;
    private AutoCompleteTextView unitInput;
    private TextInputLayout expiryLayout;
    private TextInputEditText expiryInput;

    /** The row being edited, or {@code null} in create mode. */
    @Nullable
    private PantryItem editingItem;

    /** ISO expiry chosen in the date picker, or {@code null} when left blank. */
    @Nullable
    private String selectedExpiryIso;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        pantryRepository = new PantryRepository(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bindViews();
        populateNameSuggestions();
        populateUnitDropdown();
        wireDatePicker();
        wireButtons();

        long itemId = getIntent().getLongExtra(EXTRA_ITEM_ID, PantryItem.NO_ID);
        if (itemId != PantryItem.NO_ID) {
            enterEditMode(itemId);
        } else {
            setTitle(R.string.title_add_ingredient);
        }

        // Restore anything the user had typed before a rotation. The text fields keep their
        // own state automatically; the expiry has to be restored by hand because it is held
        // in a field rather than in the (read-only) EditText alone.
        if (savedInstanceState != null) {
            selectedExpiryIso = savedInstanceState.getString("selectedExpiryIso");
            showExpiry(selectedExpiryIso);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedExpiryIso", selectedExpiryIso);
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.layoutName);
        nameInput = findViewById(R.id.inputName);
        quantityLayout = findViewById(R.id.layoutQuantity);
        quantityInput = findViewById(R.id.inputQuantity);
        unitLayout = findViewById(R.id.layoutUnit);
        unitInput = findViewById(R.id.inputUnit);
        expiryLayout = findViewById(R.id.layoutExpiry);
        expiryInput = findViewById(R.id.inputExpiry);
    }

    /**
     * Offers the ingredient spellings the recipe book actually uses.
     *
     * <p>This is quietly one of the most effective parts of the matching story: steering the
     * user towards "Cheddar cheese" instead of "chedder" prevents a mismatch before it
     * happens, which no amount of fuzzy comparison afterwards can do as reliably.</p>
     */
    private void populateNameSuggestions() {
        List<String> suggestions = new ArrayList<>(
                new RecipeRepository(this).distinctIngredientNames());
        nameInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, suggestions));
        nameInput.setThreshold(1);
    }

    private void populateUnitDropdown() {
        final List<String> codes = UnitConverter.spinnerUnitCodes();
        List<String> labels = UnitConverter.spinnerUnitLabels();

        unitInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, labels));
        unitInput.setOnItemClickListener((parent, view, position, id) -> {
            // The dropdown shows friendly labels but the database stores canonical codes,
            // so the position is translated back to a code here.
            unitInput.setTag(codes.get(position));
            unitLayout.setError(null);
        });
    }

    private void wireDatePicker() {
        View.OnClickListener openPicker = new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                showDatePicker();
            }
        };
        // The field is not typeable (see the layout), so both the field and its icon open
        // the picker. That rules out malformed dates by construction rather than by validation.
        expiryInput.setOnClickListener(openPicker);
        expiryLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (selectedExpiryIso == null) {
                    showDatePicker();
                } else {
                    clearExpiry();
                }
            }
        });
    }

    private void wireButtons() {
        MaterialButton saveButton = findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                attemptSave();
            }
        });

        MaterialButton cancelButton = findViewById(R.id.buttonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    /** Loads the row named by the Intent and pre-fills every field with its current values. */
    private void enterEditMode(long itemId) {
        editingItem = pantryRepository.getById(itemId);
        if (editingItem == null) {
            // The row was deleted while this screen was opening; fail gracefully.
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        setTitle(R.string.title_edit_ingredient);
        nameInput.setText(editingItem.getName());
        quantityInput.setText(UnitConverter.formatQuantity(editingItem.getQuantity()));
        unitInput.setText(labelForUnitCode(editingItem.getUnit()), false);
        unitInput.setTag(UnitConverter.canonicalUnit(editingItem.getUnit()));
        selectedExpiryIso = editingItem.getExpiryDate();
        showExpiry(selectedExpiryIso);
        invalidateOptionsMenu();
    }

    private String labelForUnitCode(String code) {
        String canonical = UnitConverter.canonicalUnit(code);
        List<String> codes = UnitConverter.spinnerUnitCodes();
        int index = codes.indexOf(canonical);
        if (index >= 0) {
            return UnitConverter.spinnerUnitLabels().get(index);
        }
        // A unit that is not in the dropdown (seeded data uses a few extras such as "head")
        // is still shown as-is rather than silently blanked.
        return canonical;
    }

    // ------------------------------------------------------------ expiry date

    private void showDatePicker() {
        Calendar startFrom = DateUtils.parseIso(selectedExpiryIso);
        if (startFrom == null) {
            startFrom = Calendar.getInstance();
        }

        DatePickerDialog dialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        selectedExpiryIso = DateUtils.toIso(year, month, dayOfMonth);
                        showExpiry(selectedExpiryIso);
                        expiryLayout.setError(null);
                    }
                },
                startFrom.get(Calendar.YEAR),
                startFrom.get(Calendar.MONTH),
                startFrom.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showExpiry(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            selectedExpiryIso = null;
            expiryInput.setText("");
            expiryLayout.setEndIconDrawable(R.drawable.ic_calendar);
            expiryLayout.setHelperText(getString(R.string.helper_expiry_optional));
            return;
        }

        expiryInput.setText(DateUtils.toDisplay(iso));
        expiryLayout.setEndIconDrawable(R.drawable.ic_close);
        if (DateUtils.isExpired(iso)) {
            // Past dates are allowed on purpose - a user may well be recording something
            // that has already gone off - but the form says so rather than hiding it.
            expiryLayout.setHelperText(getString(R.string.helper_expiry_past));
        } else {
            expiryLayout.setHelperText(getString(R.string.helper_expiry_optional));
        }
    }

    private void clearExpiry() {
        showExpiry(null);
    }

    // -------------------------------------------------------------- validation

    /**
     * Validates the form, then inserts or updates (Section 3.1: input validation).
     *
     * <p>Every field is checked before anything is written, and all failures are reported at
     * once via {@link TextInputLayout#setError(CharSequence)} so the user is not sent round
     * the loop one message at a time.</p>
     */
    private void attemptSave() {
        String name = text(nameInput);
        String quantityText = text(quantityInput);

        boolean valid = true;

        // --- name ---------------------------------------------------------------
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (name.length() < 2) {
            nameLayout.setError(getString(R.string.error_name_too_short));
            valid = false;
        } else if (!name.matches(".*[A-Za-z].*")) {
            // "12" or "!!" is not an ingredient name, and it would normalise to an empty key.
            nameLayout.setError(getString(R.string.error_name_letters));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        // --- quantity -----------------------------------------------------------
        double quantity = 0d;
        if (quantityText.isEmpty()) {
            quantityLayout.setError(getString(R.string.error_quantity_required));
            valid = false;
        } else {
            try {
                // Accept a comma as the decimal separator; many local keyboards produce one.
                quantity = Double.parseDouble(quantityText.replace(',', '.'));
                if (quantity <= 0d) {
                    quantityLayout.setError(getString(R.string.error_quantity_positive));
                    valid = false;
                } else if (quantity > MAX_QUANTITY) {
                    quantityLayout.setError(getString(R.string.error_quantity_too_large));
                    valid = false;
                } else {
                    quantityLayout.setError(null);
                }
            } catch (NumberFormatException notANumber) {
                quantityLayout.setError(getString(R.string.error_quantity_number));
                valid = false;
            }
        }

        // --- unit ---------------------------------------------------------------
        String unitCode = unitInput.getTag() instanceof String ? (String) unitInput.getTag() : null;
        if (unitCode == null || unitCode.isEmpty()) {
            unitLayout.setError(getString(R.string.error_unit_required));
            valid = false;
        } else {
            unitLayout.setError(null);
        }

        if (!valid) {
            return;
        }

        // Two rows for the same ingredient are legal - the matcher adds comparable amounts
        // together - but it is usually a mistake, so the form asks first.
        String nameKey = IngredientNormalizer.normalize(name);
        long excludingId = editingItem == null ? PantryItem.NO_ID : editingItem.getId();
        if (pantryRepository.existsByKey(nameKey, excludingId)) {
            confirmDuplicate(name, quantity, unitCode);
            return;
        }

        save(name, quantity, unitCode);
    }

    private void confirmDuplicate(final String name, final double quantity, final String unitCode) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_duplicate_title)
                .setMessage(getString(R.string.dialog_duplicate_message, name))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_add_anyway,
                        (dialog, which) -> save(name, quantity, unitCode))
                .show();
    }

    private void save(String name, double quantity, String unitCode) {
        if (editingItem == null) {
            PantryItem created = new PantryItem(name, quantity, unitCode, selectedExpiryIso);
            long newId = pantryRepository.insert(created);
            if (newId == -1L) {
                finishWithMessage(getString(R.string.msg_save_failed), false);
                return;
            }
            finishWithMessage(getString(R.string.msg_item_added, name), true);
            return;
        }

        editingItem.setName(name);
        editingItem.setQuantity(quantity);
        editingItem.setUnit(unitCode);
        editingItem.setExpiryDate(selectedExpiryIso);
        int updatedRows = pantryRepository.update(editingItem);
        if (updatedRows == 0) {
            finishWithMessage(getString(R.string.msg_save_failed), false);
            return;
        }
        finishWithMessage(getString(R.string.msg_item_updated, name), true);
    }

    private void finishWithMessage(String message, boolean success) {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_MESSAGE, message);
        setResult(success ? RESULT_OK : RESULT_CANCELED, result);
        finish();
    }

    // ----------------------------------------------------------------- toolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Delete only makes sense on a row that exists, so the action is hidden in create mode.
        if (editingItem != null) {
            getMenuInflater().inflate(R.menu.menu_add_edit, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_delete && editingItem != null) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        final PantryItem target = editingItem;
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, target.getName()))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    pantryRepository.delete(target.getId());
                    finishWithMessage(getString(R.string.msg_item_deleted, target.getName()), true);
                })
                .show();
    }

    private static String text(AutoCompleteTextView field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private static String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
