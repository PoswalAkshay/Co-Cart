package com.example.co_cart.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.co_cart.R;
import com.example.co_cart.models.GroceryItem;

import java.util.ArrayList;
import java.util.List;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.ViewHolder> {

    private List<GroceryItem> itemList = new ArrayList<>();
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onTogglePurchased(GroceryItem item, boolean isPurchased);
        void onEdit(GroceryItem item);
        void onDelete(GroceryItem item);
    }

    public GroceryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<GroceryItem> items) {
        this.itemList = items;
        notifyDataSetChanged();
    }

    public List<GroceryItem> getItems() {
        return itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_grocery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GroceryItem item = itemList.get(position);

        holder.tvItemName.setText(item.getName());
        holder.tvQuantity.setText(item.getQuantity() != null && !item.getQuantity().isEmpty() ? item.getQuantity() : "1");
        holder.tvCategoryChip.setText(item.getCategory() != null && !item.getCategory().isEmpty() ? item.getCategory() : "General 🛒");

        if (item.isUrgent()) {
            holder.tvUrgentBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvUrgentBadge.setVisibility(View.GONE);
        }

        if (item.getEstimatedCost() > 0) {
            holder.tvEstimatedCost.setVisibility(View.VISIBLE);
            String costText = "₹ " + String.format("%.2f", item.getEstimatedCost());
            holder.tvEstimatedCost.setText(costText);
        } else {
            holder.tvEstimatedCost.setVisibility(View.GONE);
        }

        String addedByText = "Added by " + (item.getAddedByName() != null ? item.getAddedByName() : "Roommate");
        holder.tvAddedBy.setText(addedByText);

        holder.cbPurchased.setOnCheckedChangeListener(null);
        holder.cbPurchased.setChecked(item.isPurchased());

        if (item.isPurchased()) {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvItemName.setAlpha(0.5f);
            holder.tvQuantity.setAlpha(0.5f);
            holder.tvCategoryChip.setAlpha(0.5f);
        } else {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvItemName.setAlpha(1.0f);
            holder.tvQuantity.setAlpha(1.0f);
            holder.tvCategoryChip.setAlpha(1.0f);
        }

        holder.cbPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTogglePurchased(item, isChecked);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbPurchased;
        TextView tvCategoryChip, tvUrgentBadge, tvItemName, tvQuantity, tvEstimatedCost, tvAddedBy;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbPurchased = itemView.findViewById(R.id.cbPurchased);
            tvCategoryChip = itemView.findViewById(R.id.tvCategoryChip);
            tvUrgentBadge = itemView.findViewById(R.id.tvUrgentBadge);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvEstimatedCost = itemView.findViewById(R.id.tvEstimatedCost);
            tvAddedBy = itemView.findViewById(R.id.tvAddedBy);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
