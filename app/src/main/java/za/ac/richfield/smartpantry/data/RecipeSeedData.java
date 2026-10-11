package za.ac.richfield.smartpantry.data;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * The bundled recipe book: 20 recipes written into SQLite on first run.
 *
 * <p>Section 2.2 asks for 15-20 pre-loaded recipes. They are kept as Java objects rather
 * than a raw SQL script so the compiler checks every row, and they are deliberately built
 * around ordinary leftovers - eggs, bread, rice, tinned beans - because a recipe book full
 * of specialist ingredients would make the strict rule impossible to satisfy and the app
 * useless.</p>
 *
 * <p>Quantities carry real units (g, ml, cloves, slices) so the matcher has something to
 * compare, and the mix of short three-ingredient recipes and longer ones makes the strict
 * rule easy to demonstrate: adding or removing one ingredient visibly moves a recipe in and
 * out of the suggestions list.</p>
 */
final class RecipeSeedData {

    private RecipeSeedData() {
        // Static data holder only.
    }

    /** Short alias so the recipe definitions below stay readable. */
    private static RecipeIngredient need(String name, double quantity, String unit) {
        return new RecipeIngredient(name, quantity, unit);
    }

    static List<Recipe> buildRecipeBook() {
        List<Recipe> book = new ArrayList<>();

        book.add(new Recipe("Scrambled Eggs on Toast", "Breakfast", 10, 2,
                "Whisk the eggs with the milk, salt and pepper.\n"
                        + "Melt the butter in a pan over medium-low heat.\n"
                        + "Pour in the eggs and stir slowly until just set.\n"
                        + "Toast the bread and spoon the eggs over it.")
                .withIngredients(
                        need("Eggs", 3, "piece"),
                        need("Milk", 30, "ml"),
                        need("Butter", 15, "g"),
                        need("Bread", 2, "slice"),
                        need("Salt", 1, "pinch"),
                        need("Black pepper", 1, "pinch")));

        book.add(new Recipe("Cheese Omelette", "Breakfast", 10, 1,
                "Beat the eggs with the salt until the yolks and whites combine.\n"
                        + "Melt the butter in a non-stick pan over medium heat.\n"
                        + "Pour in the eggs and let the base set without stirring.\n"
                        + "Scatter the cheese over one half, fold and serve.")
                .withIngredients(
                        need("Eggs", 3, "piece"),
                        need("Cheddar cheese", 50, "g"),
                        need("Butter", 10, "g"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Classic Pancakes", "Breakfast", 20, 4,
                "Whisk the flour, sugar and baking powder together.\n"
                        + "Beat in the eggs and milk until the batter is smooth.\n"
                        + "Rest the batter for five minutes.\n"
                        + "Fry spoonfuls in a buttered pan until golden on both sides.")
                .withIngredients(
                        need("Flour", 200, "g"),
                        need("Milk", 300, "ml"),
                        need("Eggs", 2, "piece"),
                        need("Sugar", 30, "g"),
                        need("Baking powder", 10, "g"),
                        need("Butter", 20, "g")));

        book.add(new Recipe("French Toast", "Breakfast", 15, 2,
                "Whisk the eggs, milk, sugar and cinnamon in a shallow bowl.\n"
                        + "Soak each slice of bread for about ten seconds per side.\n"
                        + "Fry in butter over medium heat until golden.\n"
                        + "Serve immediately while still crisp.")
                .withIngredients(
                        need("Bread", 4, "slice"),
                        need("Eggs", 2, "piece"),
                        need("Milk", 100, "ml"),
                        need("Sugar", 15, "g"),
                        need("Cinnamon", 2, "g"),
                        need("Butter", 20, "g")));

        book.add(new Recipe("Banana Smoothie", "Drinks", 5, 2,
                "Peel the bananas and break them into chunks.\n"
                        + "Blend with the milk, yoghurt and honey until smooth.\n"
                        + "Pour into glasses and serve cold.")
                .withIngredients(
                        need("Bananas", 2, "piece"),
                        need("Milk", 250, "ml"),
                        need("Yoghurt", 125, "g"),
                        need("Honey", 15, "ml")));

        book.add(new Recipe("Tuna Mayo Sandwich", "Lunch", 10, 2,
                "Drain the tuna and flake it into a bowl.\n"
                        + "Stir through the mayonnaise, finely chopped onion and pepper.\n"
                        + "Spread over two slices of bread and top with the others.\n"
                        + "Cut in half and serve.")
                .withIngredients(
                        need("Bread", 4, "slice"),
                        need("Tuna", 1, "can"),
                        need("Mayonnaise", 45, "g"),
                        need("Onion", 1, "piece"),
                        need("Black pepper", 1, "pinch")));

        book.add(new Recipe("Veggie Toasted Sandwich", "Lunch", 12, 2,
                "Butter the outside of each slice of bread.\n"
                        + "Layer the cheese, sliced tomato and sliced onion inside.\n"
                        + "Toast in a pan or sandwich press until the cheese melts.\n"
                        + "Slice diagonally and serve hot.")
                .withIngredients(
                        need("Bread", 4, "slice"),
                        need("Cheddar cheese", 80, "g"),
                        need("Tomatoes", 1, "piece"),
                        need("Onion", 1, "piece"),
                        need("Butter", 20, "g")));

        book.add(new Recipe("Simple Green Salad", "Lunch", 10, 2,
                "Tear the lettuce into a bowl.\n"
                        + "Add the chopped tomatoes and sliced cucumber.\n"
                        + "Whisk the olive oil with the juice of the lemon and the salt.\n"
                        + "Dress the salad just before serving.")
                .withIngredients(
                        need("Lettuce", 1, "head"),
                        need("Tomatoes", 2, "piece"),
                        need("Cucumber", 1, "piece"),
                        need("Olive oil", 30, "ml"),
                        need("Lemon", 1, "piece"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Leftover Veg Fried Rice", "Dinner", 20, 3,
                "Heat the oil in a wok and fry the garlic for thirty seconds.\n"
                        + "Add the diced carrot and peas and stir-fry for three minutes.\n"
                        + "Push the vegetables aside, scramble the eggs in the space.\n"
                        + "Add the cooked rice and soy sauce, tossing until piping hot.")
                .withIngredients(
                        need("Rice", 400, "g"),
                        need("Eggs", 2, "piece"),
                        need("Carrots", 1, "piece"),
                        need("Peas", 100, "g"),
                        need("Soy sauce", 30, "ml"),
                        need("Garlic", 2, "clove"),
                        need("Oil", 30, "ml")));

        book.add(new Recipe("Tomato Pasta Napoletana", "Dinner", 25, 3,
                "Boil the pasta in salted water until al dente.\n"
                        + "Soften the chopped onion and garlic in the olive oil.\n"
                        + "Add the chopped tomatoes and simmer for fifteen minutes.\n"
                        + "Season with salt and toss the drained pasta through the sauce.")
                .withIngredients(
                        need("Pasta", 250, "g"),
                        need("Tomatoes", 4, "piece"),
                        need("Onion", 1, "piece"),
                        need("Garlic", 2, "clove"),
                        need("Olive oil", 30, "ml"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Mince Bolognese", "Dinner", 40, 4,
                "Brown the ground beef in the olive oil, breaking up any lumps.\n"
                        + "Add the chopped onion and garlic and cook until soft.\n"
                        + "Stir in the tomatoes and tomato paste.\n"
                        + "Simmer for twenty-five minutes and serve over boiled pasta.")
                .withIngredients(
                        need("Ground beef", 500, "g"),
                        need("Tomatoes", 4, "piece"),
                        need("Tomato paste", 30, "g"),
                        need("Onion", 1, "piece"),
                        need("Garlic", 2, "clove"),
                        need("Pasta", 300, "g"),
                        need("Olive oil", 30, "ml")));

        book.add(new Recipe("Chicken Stir-Fry", "Dinner", 25, 3,
                "Slice the chicken breast into thin strips.\n"
                        + "Sear the chicken in hot oil until browned, then set aside.\n"
                        + "Stir-fry the sliced pepper, onion and garlic for three minutes.\n"
                        + "Return the chicken, add the soy sauce and toss for two minutes.")
                .withIngredients(
                        need("Chicken breast", 400, "g"),
                        need("Bell pepper", 1, "piece"),
                        need("Onion", 1, "piece"),
                        need("Garlic", 2, "clove"),
                        need("Soy sauce", 45, "ml"),
                        need("Oil", 30, "ml")));

        book.add(new Recipe("Lentil Curry", "Dinner", 35, 4,
                "Fry the chopped onion and garlic in the oil until golden.\n"
                        + "Add the curry powder and cook for one minute to wake it up.\n"
                        + "Stir in the lentils, chopped tomatoes and coconut milk.\n"
                        + "Simmer for twenty-five minutes until the lentils are soft.")
                .withIngredients(
                        need("Lentils", 300, "g"),
                        need("Onion", 1, "piece"),
                        need("Garlic", 3, "clove"),
                        need("Curry powder", 15, "g"),
                        need("Coconut milk", 400, "ml"),
                        need("Tomatoes", 2, "piece"),
                        need("Oil", 30, "ml")));

        book.add(new Recipe("Pumpkin Soup", "Dinner", 40, 4,
                "Soften the chopped onion and garlic in a large pot.\n"
                        + "Add the cubed pumpkin and the stock and bring to the boil.\n"
                        + "Simmer for twenty-five minutes until the pumpkin collapses.\n"
                        + "Blend smooth, stir in the cream and season with salt.")
                .withIngredients(
                        need("Pumpkin", 600, "g"),
                        need("Onion", 1, "piece"),
                        need("Garlic", 2, "clove"),
                        need("Vegetable stock", 750, "ml"),
                        need("Cream", 100, "ml"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Chakalaka", "Dinner", 30, 4,
                "Fry the sliced onion in the oil until translucent.\n"
                        + "Add the grated carrot and sliced pepper and cook for five minutes.\n"
                        + "Stir in the curry powder and chopped tomatoes.\n"
                        + "Fold in the baked beans and simmer gently for ten minutes.")
                .withIngredients(
                        need("Carrots", 2, "piece"),
                        need("Onion", 1, "piece"),
                        need("Bell pepper", 1, "piece"),
                        need("Baked beans", 1, "can"),
                        need("Tomatoes", 2, "piece"),
                        need("Curry powder", 10, "g"),
                        need("Oil", 30, "ml")));

        book.add(new Recipe("Pap (Maize Porridge)", "Sides", 30, 4,
                "Bring the water to the boil with the salt.\n"
                        + "Rain the maize meal in while stirring to avoid lumps.\n"
                        + "Cover and steam on low heat for twenty-five minutes.\n"
                        + "Stir once more before serving.")
                .withIngredients(
                        need("Maize meal", 250, "g"),
                        need("Water", 750, "ml"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Garlic Butter Rice", "Sides", 25, 4,
                "Rinse the rice until the water runs clear.\n"
                        + "Fry the crushed garlic in the butter for one minute.\n"
                        + "Add the rice and water, bring to the boil, then cover.\n"
                        + "Steam on low for fifteen minutes and fluff with a fork.")
                .withIngredients(
                        need("Rice", 300, "g"),
                        need("Butter", 40, "g"),
                        need("Garlic", 4, "clove"),
                        need("Water", 600, "ml"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Oven Potato Wedges", "Sides", 45, 4,
                "Cut the potatoes into thick wedges, skin on.\n"
                        + "Toss with the olive oil, paprika and salt.\n"
                        + "Spread on a tray in a single layer.\n"
                        + "Roast at 200 C for thirty-five minutes, turning once.")
                .withIngredients(
                        need("Potatoes", 800, "g"),
                        need("Olive oil", 45, "ml"),
                        need("Paprika", 5, "g"),
                        need("Salt", 1, "pinch")));

        book.add(new Recipe("Bean Tacos", "Dinner", 20, 3,
                "Fry the chopped onion and pepper until soft.\n"
                        + "Add the cumin and the baked beans and heat through.\n"
                        + "Warm the tortillas in a dry pan.\n"
                        + "Fill, scatter the cheese over and fold.")
                .withIngredients(
                        need("Tortillas", 6, "piece"),
                        need("Baked beans", 1, "can"),
                        need("Onion", 1, "piece"),
                        need("Bell pepper", 1, "piece"),
                        need("Cheddar cheese", 100, "g"),
                        need("Cumin", 5, "g")));

        book.add(new Recipe("Chocolate Mug Cake", "Dessert", 5, 1,
                "Stir the flour, cocoa, sugar and baking powder in a large mug.\n"
                        + "Add the milk and oil and mix until no dry pockets remain.\n"
                        + "Microwave on high for ninety seconds.\n"
                        + "Let it stand for a minute before eating.")
                .withIngredients(
                        need("Flour", 60, "g"),
                        need("Cocoa powder", 20, "g"),
                        need("Sugar", 40, "g"),
                        need("Milk", 60, "ml"),
                        need("Oil", 30, "ml"),
                        need("Baking powder", 3, "g")));

        return book;
    }
}
