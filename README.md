# Smart Pantry Manager

A Java Android application that suggests recipes based **strictly** on the leftover
ingredients you already have at home, to cut food waste.

Module: **Mobile App Development 700** · Richfield Graduate Institute of Technology
Student number: **402309985**

---

## 1. What the app does

Most recipe apps assume a shopping trip. Smart Pantry Manager assumes the opposite: you open
the fridge, type in what is actually there, and the app tells you what you can cook **right
now** — nothing else.

- **My Pantry** — add, edit and delete the ingredients you have, with a quantity, a unit and
  an optional expiry date. Items about to go off are highlighted so they get used first.
- **Suggested Recipes** — runs the strict-matching rule over the pantry and lists only the
  recipes where *every* required ingredient is present in at least the required amount.
- **Recipe Book** — all 20 bundled recipes, unfiltered, so you can see what a recipe needs
  even when you cannot cook it yet.
- **Recipe detail** — the full ingredient list with a tick or a cross against each line, plus
  the method.
- **Settings** — expiry-alert preferences, and switches that change how the matching rule
  behaves.

The app is fully offline. It declares **no Android permissions at all**, and contains no
Google Maps, no mapping SDK and no location or GPS code of any kind.

---

## 2. The strict-matching rule (the core logic)

> A recipe may only be shown as *suggested* if every single ingredient it requires is
> currently present in the pantry, in at least the required quantity.

That rule lives in [`logic/RecipeMatcher.java`](app/src/main/java/za/ac/richfield/smartpantry/logic/RecipeMatcher.java)
and is evaluated per ingredient in three stages:

| Stage | What happens | Where |
|---|---|---|
| 1. Identify | Find the pantry rows that are the same ingredient, comparing **normalised keys** instead of raw text | `IngredientNormalizer` |
| 2. Convert | Put the required amount and the pantry amount into the same base unit | `UnitConverter` |
| 3. Compare | The pantry total must be **>=** the required amount | `RecipeMatcher.evaluate()` |

A recipe needing 5 ingredients with 4 in the pantry is **excluded** — there is no partial
credit. The decision is a single expression, `getMissingCount() == 0`, in
`RecipeMatch.isCookableNow()`.

### Robustness to real-world messiness

The brief notes that a naive exact-string match that breaks on `tomato` vs `tomatoes` will be
marked down. The app handles the common cases without resorting to NLP:

| You typed | Recipe asks for | Matches? | Why |
|---|---|---|---|
| `Fresh Tomatoes` | `Tomato` | yes | descriptors dropped, plural singularised |
| `0.5 kg` chicken | `400 g` chicken | yes | both converted to grams |
| `Courgettes` | `Zucchini` | yes | regional synonym table |
| `Beef mince` | `Ground beef` | yes | synonym table |
| `Cucumbr` | `Cucumber` | yes | one-character edit distance |
| `Chicken breast` | `Chicken` | yes | every word of the requirement is present |
| `Oil` | `Olive oil` | **no** | the reverse is *not* accepted — plain oil is not olive oil |
| 1 egg | 3 eggs | **no** | quantity is short |

The last two rows are the important ones: being generous in the wrong direction would make
the app suggest meals you cannot actually cook, which is the exact failure the strict rule
exists to prevent.

### "Almost There" (optional stretch, Section 8)

Recipes that are short by **exactly one** ingredient appear in a clearly separated, clearly
labelled section *below* the strict suggestions, produced by a different method
(`RecipeMatcher.almostThere()`). Nothing from that list can ever appear among the strict
suggestions, and it can be switched off in Settings.

---

## 3. Database choice: SQLite (`SQLiteOpenHelper`)

**Chosen: SQLite, implemented with `SQLiteOpenHelper`.**

Why, over the other two permitted options:

- **The app is single-user and offline-first.** A pantry only matters on the phone standing in
  the kitchen. There is nothing to sync and nobody to share with, so Firebase's strengths
  (real-time sync, multi-device, auth) buy nothing here while adding an account, a network
  dependency and a Google services configuration.
- **The strict rule reads the entire pantry at once.** Matching 20 recipes against the whole
  pantry is one local query plus in-memory work — a few milliseconds. Over a REST API in front
  of PostgreSQL the same screen becomes a network round trip that can fail, which would mean
  loading states and error handling on the one screen that must feel instant.
- **It works with no permissions and no backend.** The app ships with zero `<uses-permission>`
  entries. A PostgreSQL option would have required building and hosting an API as well.
- **It is the approach taught in the module's persistent data chapter**, so the code uses the
  same `SQLiteOpenHelper` / `ContentValues` / `Cursor` idioms as the lecture material.

### Data model

```
pantry_items                         recipes
------------                         -------
_id          INTEGER PK              _id           INTEGER PK
name         TEXT      <- as typed   name          TEXT
name_key     TEXT      <- normalised category      TEXT
quantity     REAL                    prep_minutes  INTEGER
unit         TEXT                    servings      INTEGER
expiry_date  TEXT (ISO, nullable)    steps         TEXT
created_at   INTEGER                       |
                                           | 1 : many  (ON DELETE CASCADE)
                                           v
                                    recipe_ingredients
                                    ------------------
                                    _id        INTEGER PK
                                    recipe_id  INTEGER FK
                                    name       TEXT
                                    name_key   TEXT   <- normalised
                                    quantity   REAL
                                    unit       TEXT
```

Two design decisions worth noting:

1. **Ingredients are their own table**, not a comma-separated string in the `recipes` row.
   The strict rule needs a quantity and a unit *per ingredient*, which one text column could
   not provide.
2. **`name_key` is computed once on write**, not on every read. Matching is then an indexed
   key comparison rather than re-normalising the same text on every screen refresh, and the
   UI can still show the user exactly what they typed.

**Full CRUD** lives in `PantryRepository`: `insert()`, `getAll()`/`search()`/`getById()`,
`update()`, `delete()`/`deleteAll()`. Data persists across restarts because every screen
re-reads the database in `onResume()` — nothing is held in memory between sessions.

---

## 4. How to set up and run

### Requirements

- Android Studio Ladybug (2024.2) or newer
- JDK 17 (bundled with Android Studio)
- Android SDK Platform 34
- An emulator or device running **Android 7.0 (API 24) or newer**

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/victorSam-Ehumadu/Smart-Pantry-Manager.git
   cd Smart-Pantry-Manager
   ```
2. In Android Studio choose **File -> Open** and select the project folder (the one
   containing `settings.gradle`). The Gradle wrapper is included, so Gradle itself does not
   need to be installed — Android Studio downloads Gradle 8.7 and the dependencies on first
   sync. Give it a few minutes.
3. Let the Gradle sync finish, then pick an emulator or a connected device.
4. Press **Run ▶** (or `Shift + F10`).

`local.properties` is intentionally not included, because it stores a machine-specific SDK
path. Android Studio writes a correct one for your machine when you open the project.

From the command line instead (no separate Gradle install needed — the wrapper handles it):

```bash
./gradlew assembleDebug     # build the APK
./gradlew installDebug      # install onto a running device/emulator
```

### First run

The pantry starts **empty** on purpose, so you see the empty-state message. The 20 recipes are
seeded into SQLite automatically the first time the database is created.

To see the strict rule work in a few seconds, add:

| Ingredient | Quantity | Unit |
|---|---|---|
| Eggs | 3 | piece |
| Cheddar cheese | 100 | g |
| Butter | 100 | g |
| Salt | 500 | g |

**Cheese Omelette** now appears under *Ready to cook now*. Delete the butter and it
disappears; add it back and it returns.

---

## 5. Project structure

```
app/src/main/java/za/ac/richfield/smartpantry/
├── MainActivity.java                 Screen 1 - pantry list (launcher), Read + Delete
├── AddEditIngredientActivity.java    Screen 2 - form, Create + Update, validation
├── SuggestedRecipesActivity.java     Screen 3 - strict matching results
├── RecipeBrowseActivity.java         Screen 4 - the full recipe book
├── RecipeDetailActivity.java         Screen 5 - ingredients checklist + method
├── SettingsActivity.java             Screen 6 - preferences
├── BaseActivity.java                 shared bottom-navigation wiring (explicit Intents)
├── adapter/                          four custom RecyclerView adapters
├── data/                             DatabaseHelper, repositories, seeded recipes
├── logic/                            IngredientNormalizer, UnitConverter, RecipeMatcher
├── model/                            PantryItem, Recipe, RecipeIngredient, match results
└── util/                             AppPrefs (SharedPreferences), DateUtils
```

### Requirement checklist

| Requirement | Where it is met |
|---|---|
| Built entirely in Java | no Kotlin anywhere in the project |
| 4+ distinct screens plus Settings | six Activities, listed above |
| Intents for navigation and data | `BaseActivity`, `MainActivity.openEditForm()`, `EXTRA_RECIPE_ID` |
| RecyclerView with a custom Adapter | `PantryAdapter`, `RecipeMatchAdapter`, `RecipeAdapter`, `IngredientStatusAdapter` |
| Working navigation element | `BottomNavigationView` + two toolbar menus |
| Input validation | `AddEditIngredientActivity.attemptSave()` |
| 15–20 seeded recipes | 20, in `RecipeSeedData` |
| Full CRUD that persists | `PantryRepository`, re-read in every `onResume()` |
| Zero-match feedback | `view_empty_state.xml` + per-situation copy |
| No Maps / GPS / location | no such permission, dependency or code exists |

---

## 6. Licence / academic integrity

Submitted as individual coursework for Mobile App Development 700. The recipe content and all
source code in this repository are my own work.
