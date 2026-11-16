package com.example.co_cart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.co_cart.R;
import com.example.co_cart.models.JoinRequest;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.ViewHolder> {

    private List<JoinRequest> requestList = new ArrayList<>();
    private final OnRequestListener listener;

    public interface OnRequestListener {
        void onRespond(JoinRequest request, boolean accept);
    }

    public JoinRequestAdapter(OnRequestListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<JoinRequest> requests) {
        this.requestList = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_join_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JoinRequest request = requestList.get(position);

        holder.tvUserName.setText(request.getUserName() != null ? request.getUserName() : "Roommate");
        holder.tvUserEmail.setText(request.getUserEmail() != null ? request.getUserEmail() : "");

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRespond(request, true);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRespond(request, false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail;
        MaterialButton btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
