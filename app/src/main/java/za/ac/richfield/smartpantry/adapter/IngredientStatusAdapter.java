package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.logic.UnitConverter;
import za.ac.richfield.smartpantry.model.IngredientStatus;

/**
 * The ingredient checklist on the recipe detail screen.
 *
 * <p>Each row shows one required ingredient together with the matcher's verdict, so the user
 * can see not just THAT a recipe is unavailable but exactly why: which ingredient is missing,
 * or which one they have too little of. This is also the clearest on-screen evidence that
 * the strict rule is doing real per-ingredient work rather than a single string compare.</p>
 */
public class IngredientStatusAdapter
        extends RecyclerView.Adapter<IngredientStatusAdapter.StatusViewHolder> {

    private final Context context;
    private final List<IngredientStatus> statuses = new ArrayList<>();

    public IngredientStatusAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<IngredientStatus> replacement) {
        statuses.clear();
        if (replacement != null) {
            statuses.addAll(replacement);
        }
        // notifyDataSetChanged() rather than a DiffUtil payload: these lists are at most a
        // few dozen rows, the whole list is replaced at once after a database read, and the
        // simpler call keeps the adapter readable. DiffUtil would be the right choice if the
        // data were paged or updated row by row.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingredient_status, parent, false);
        return new StatusViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        IngredientStatus status = statuses.get(position);

        holder.nameView.setText(status.getRequired().getName());
        holder.amountView.setText(UnitConverter.formatAmount(
                status.getRequired().getQuantity(), status.getRequired().getUnit()));

        switch (status.getOutcome()) {
            case SATISFIED:
                holder.iconView.setImageResource(R.drawable.ic_check);
                holder.iconView.setColorFilter(
                        ContextCompat.getColor(context, R.color.status_ok));
                holder.detailView.setVisibility(View.GONE);
                break;

            case SATISFIED_APPROXIMATE:
                holder.iconView.setImageResource(R.drawable.ic_check);
                holder.iconView.setColorFilter(
                        ContextCompat.getColor(context, R.color.status_approximate));
                holder.detailView.setVisibility(View.VISIBLE);
                holder.detailView.setText(R.string.status_approximate);
                holder.detailView.setTextColor(
                        ContextCompat.getColor(context, R.color.status_approximate));
                break;

            case NOT_ENOUGH:
                holder.iconView.setImageResource(R.drawable.ic_close);
                holder.iconView.setColorFilter(
                        ContextCompat.getColor(context, R.color.status_missing));
                holder.detailView.setVisibility(View.VISIBLE);
                holder.detailView.setText(context.getString(R.string.status_not_enough,
                        UnitConverter.formatAmount(status.getAvailableInRequiredUnit(),
                                status.getRequired().getUnit())));
                holder.detailView.setTextColor(
                        ContextCompat.getColor(context, R.color.status_missing));
                break;

            case MISSING:
            default:
                holder.iconView.setImageResource(R.drawable.ic_close);
                holder.iconView.setColorFilter(
                        ContextCompat.getColor(context, R.color.status_missing));
                holder.detailView.setVisibility(View.VISIBLE);
                holder.detailView.setText(R.string.status_missing);
                holder.detailView.setTextColor(
                        ContextCompat.getColor(context, R.color.status_missing));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return statuses.size();
    }

    static class StatusViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final TextView nameView;
        final TextView amountView;
        final TextView detailView;

        StatusViewHolder(@NonNull View row) {
            super(row);
            iconView = row.findViewById(R.id.imageStatus);
            nameView = row.findViewById(R.id.textStatusName);
            amountView = row.findViewById(R.id.textStatusAmount);
            detailView = row.findViewById(R.id.textStatusDetail);
        }
    }
}
