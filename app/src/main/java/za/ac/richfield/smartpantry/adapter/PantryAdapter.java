package za.ac.richfield.smartpantry.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import za.ac.richfield.smartpantry.R;
import za.ac.richfield.smartpantry.logic.UnitConverter;
import za.ac.richfield.smartpantry.model.PantryItem;
import za.ac.richfield.smartpantry.util.DateUtils;

/**
 * Custom {@link RecyclerView.Adapter} for the pantry list (Section 3.1).
 *
 * <p>Follows the standard three-part pattern taught in the module: a ViewHolder that caches
 * the {@code findViewById} lookups for one row, {@link #onCreateViewHolder} to inflate a row
 * layout, and {@link #onBindViewHolder} to pour one {@link PantryItem} into an existing row.
 * Caching the views in the holder is the point of RecyclerView - without it, every scroll
 * frame would re-walk the view tree for each visible row.</p>
 *
 * <p>The adapter stays free of database and navigation code. Taps are reported back to the
 * Activity through {@link Listener}, so this class only ever renders what it is given.</p>
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    /** Row interactions, handled by the hosting Activity. */
    public interface Listener {
        /** Row tapped: open the edit form for this item. */
        void onItemClicked(PantryItem item);

        /** Row's delete button tapped: the Activity confirms before removing anything. */
        void onDeleteClicked(PantryItem item);
    }

    private final Context context;
    private final Listener listener;
    private final List<PantryItem> items = new ArrayList<>();

    /** Mirrors the Settings switch; when false no row is ever tinted as expiring. */
    private boolean highlightExpiring = true;
    private int expiryWindowDays = 3;

    public PantryAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Replaces the whole list, e.g. after a database reload in {@code onResume()}. */
    public void submitList(List<PantryItem> replacement) {
        items.clear();
        if (replacement != null) {
            items.addAll(replacement);
        }
        // notifyDataSetChanged() rather than a DiffUtil payload: these lists are at most a
        // few dozen rows, the whole list is replaced at once after a database read, and the
        // simpler call keeps the adapter readable. DiffUtil would be the right choice if the
        // data were paged or updated row by row.
        notifyDataSetChanged();
    }

    public void setExpiryHighlighting(boolean enabled, int windowDays) {
        this.highlightExpiring = enabled;
        this.expiryWindowDays = windowDays;
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);

        holder.nameView.setText(item.getName());
        holder.quantityView.setText(
                UnitConverter.formatAmount(item.getQuantity(), item.getUnit()));

        bindExpiry(holder, item);

        // The listener is wired to the item captured for THIS bind call rather than to the
        // holder's position, because a recycled row's position can change underneath it.
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (listener != null) {
                    listener.onItemClicked(item);
                }
            }
        });
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (listener != null) {
                    listener.onDeleteClicked(item);
                }
            }
        });
        holder.deleteButton.setContentDescription(
                context.getString(R.string.cd_delete_item, item.getName()));
    }

    /** Shows the expiry line and tints it red when expired, amber when close. */
    private void bindExpiry(PantryViewHolder holder, PantryItem item) {
        String expiry = item.getExpiryDate();
        if (expiry == null || expiry.trim().isEmpty()) {
            holder.expiryView.setVisibility(View.GONE);
            return;
        }

        holder.expiryView.setVisibility(View.VISIBLE);
        int daysLeft = DateUtils.daysUntil(expiry);
        String readableDate = DateUtils.toDisplay(expiry);

        if (!highlightExpiring) {
            holder.expiryView.setText(context.getString(R.string.pantry_expires_on, readableDate));
            holder.expiryView.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary));
            return;
        }

        if (daysLeft < 0) {
            holder.expiryView.setText(context.getString(R.string.pantry_expired_on, readableDate));
            holder.expiryView.setTextColor(ContextCompat.getColor(context, R.color.expired));
        } else if (daysLeft == 0) {
            holder.expiryView.setText(R.string.pantry_expires_today);
            holder.expiryView.setTextColor(ContextCompat.getColor(context, R.color.expiring_soon));
        } else if (daysLeft <= expiryWindowDays) {
            holder.expiryView.setText(
                    context.getResources().getQuantityString(
                            R.plurals.pantry_expires_in_days, daysLeft, daysLeft));
            holder.expiryView.setTextColor(ContextCompat.getColor(context, R.color.expiring_soon));
        } else {
            holder.expiryView.setText(context.getString(R.string.pantry_expires_on, readableDate));
            holder.expiryView.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Holds the views of one pantry row so they are looked up once, not once per frame. */
    static class PantryViewHolder extends RecyclerView.ViewHolder {
        final TextView nameView;
        final TextView quantityView;
        final TextView expiryView;
        final ImageButton deleteButton;

        PantryViewHolder(@NonNull View row) {
            super(row);
            nameView = row.findViewById(R.id.textPantryName);
            quantityView = row.findViewById(R.id.textPantryQuantity);
            expiryView = row.findViewById(R.id.textPantryExpiry);
            deleteButton = row.findViewById(R.id.buttonDeleteItem);
        }
    }
}
