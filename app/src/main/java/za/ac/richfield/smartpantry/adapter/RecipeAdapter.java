package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.Recipe;
import za.ac.richfield.smartpantry.model.RecipeIngredient;

/**
 * Lists the whole seeded recipe book, whether or not the user can cook it.
 *
 * <p>This screen deliberately applies no matching at all. It is the reference list, which
 * also makes the strict rule easy to demonstrate: the browse screen shows all 20 recipes
 * while the suggestions screen shows only the ones the pantry currently covers.</p>
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface Listener {
        void onRecipeClicked(Recipe recipe);
    }

    private final Context context;
    private final Listener listener;
    private final List<Recipe> recipes = new ArrayList<>();

    public RecipeAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitList(List<Recipe> replacement) {
        recipes.clear();
        if (replacement != null) {
            recipes.addAll(replacement);
        }
        // notifyDataSetChanged() rather than a DiffUtil payload: these lists are at most a
        // few dozen rows, the whole list is replaced at once after a database read, and the
        // simpler call keeps the adapter readable. DiffUtil would be the right choice if the
        // data were paged or updated row by row.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.nameView.setText(recipe.getName());
        holder.metaView.setText(context.getString(R.string.recipe_meta,
                recipe.getCategory(), recipe.getPrepMinutes(), recipe.getServings()));
        holder.ingredientsView.setText(summarise(recipe));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (listener != null) {
                    listener.onRecipeClicked(recipe);
                }
            }
        });
    }

    /** "Eggs, Milk, Butter, Bread + 2 more" - enough to recognise a recipe at a glance. */
    private String summarise(Recipe recipe) {
        List<RecipeIngredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) {
            return "";
        }

        int shown = Math.min(4, ingredients.size());
        StringBuilder summary = new StringBuilder();
        for (int index = 0; index < shown; index++) {
            if (index > 0) {
                summary.append(", ");
            }
            summary.append(ingredients.get(index).getName());
        }
        int remaining = ingredients.size() - shown;
        if (remaining > 0) {
            summary.append(context.getString(R.string.recipe_more_ingredients, remaining));
        }
        return summary.toString();
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        final TextView nameView;
        final TextView metaView;
        final TextView ingredientsView;

        RecipeViewHolder(@NonNull View row) {
            super(row);
            nameView = row.findViewById(R.id.textRecipeName);
            metaView = row.findViewById(R.id.textRecipeMeta);
            ingredientsView = row.findViewById(R.id.textRecipeIngredients);
        }
    }
}
