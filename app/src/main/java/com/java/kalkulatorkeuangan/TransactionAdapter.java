package com.java.kalkulatorkeuangan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvNote, tvDate, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvNote = itemView.findViewById(R.id.tvNote);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Transaction transaction =
                transactionList.get(position);
        android.util.Log.d(
                "ADAPTER_CEK",
                transaction.getNote()
        );

        holder.tvNote.setText(transaction.getNote());
        holder.tvDate.setText(transaction.getDate());

        holder.tvAmount.setText(
                "Rp " + transaction.getAmount()
        );
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }
}