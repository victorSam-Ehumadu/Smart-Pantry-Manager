package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.model.RecipeMatch;

/**
 * Renders the result of the strict-matching rule.
 *
 * <p>Used by both lists on the Suggested Recipes screen, switched by {@link Mode}:</p>
 *
 * <ul>
 *     <li>{@link Mode#READY} - the strict list. Every row here passed the full rule, so the
 *         badge reads "all N ingredients in your pantry".</li>
 *     <li>{@link Mode#ALMOST} - the optional bonus list. The badge names the single missing
 *         ingredient instead, and the row is tinted differently so the two sections can never
 *         be mistaken for one another.</li>
 * </ul>
 *
 * <p>One adapter with a mode, rather than two near-identical classes, because the row layout
 * and binding are otherwise identical - only the badge text and its colour differ.</p>
 */
public class RecipeMatchAdapter extends RecyclerView.Adapter<RecipeMatchAdapter.MatchViewHolder> {

    public enum Mode {
        READY, ALMOST
    }

    public interface Listener {
        void onRecipeClicked(RecipeMatch match);
    }

    private final Context context;
    private final Mode mode;
    private final Listener listener;
    private final List<RecipeMatch> matches = new ArrayList<>();

    public RecipeMatchAdapter(Context context, Mode mode, Listener listener) {
        this.context = context;
        this.mode = mode;
        this.listener = listener;
    }

    public void submitList(List<RecipeMatch> replacement) {
        matches.clear();
        if (replacement != null) {
            matches.addAll(replacement);
        }
        // notifyDataSetChanged() rather than a DiffUtil payload: these lists are at most a
        // few dozen rows, the whole list is replaced at once after a database read, and the
        // simpler call keeps the adapter readable. DiffUtil would be the right choice if the
        // data were paged or updated row by row.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_match, parent, false);
        return new MatchViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        RecipeMatch match = matches.get(position);

        holder.nameView.setText(match.getRecipe().getName());
        holder.metaView.setText(context.getString(R.string.recipe_meta,
                match.getRecipe().getCategory(),
                match.getRecipe().getPrepMinutes(),
                match.getRecipe().getServings()));

        if (mode == Mode.READY) {
            holder.badgeView.setText(context.getResources().getQuantityString(
                    R.plurals.suggestion_all_ingredients,
                    match.getRequiredCount(), match.getRequiredCount()));
            holder.badgeView.setBackgroundResource(R.drawable.bg_badge_ready);
            holder.badgeView.setTextColor(ContextCompat.getColor(context, R.color.badge_ready_text));

            // An approximate pass means the ingredient was there but its unit could not be
            // compared with the recipe's. Saying so keeps the suggestion honest.
            if (match.hasApproximateMatch()) {
                holder.noteView.setVisibility(View.VISIBLE);
                holder.noteView.setText(R.string.suggestion_approximate_note);
            } else {
                holder.noteView.setVisibility(View.GONE);
            }
        } else {
            List<String> outstanding = match.getOutstandingNames();
            String missing = outstanding.isEmpty() ? "" : outstanding.get(0);
            holder.badgeView.setText(context.getString(R.string.suggestion_missing_one, missing));
            holder.badgeView.setBackgroundResource(R.drawable.bg_badge_almost);
            holder.badgeView.setTextColor(
                    ContextCompat.getColor(context, R.color.badge_almost_text));
            holder.noteView.setVisibility(View.VISIBLE);
            holder.noteView.setText(context.getResources().getQuantityString(
                    R.plurals.suggestion_have_count, match.getRequiredCount(),
                    match.getSatisfiedCount(), match.getRequiredCount()));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (listener != null) {
                    listener.onRecipeClicked(match);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        final TextView nameView;
        final TextView metaView;
        final TextView badgeView;
        final TextView noteView;

        MatchViewHolder(@NonNull View row) {
            super(row);
            nameView = row.findViewById(R.id.textMatchName);
            metaView = row.findViewById(R.id.textMatchMeta);
            badgeView = row.findViewById(R.id.textMatchBadge);
            noteView = row.findViewById(R.id.textMatchNote);
        }
    }
}
