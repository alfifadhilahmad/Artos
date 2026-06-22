package com.java.kalkulatorkeuangan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivTransactionIcon;
        TextView tvNote, tvDate, tvCategory, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTransactionIcon = itemView.findViewById(R.id.ivTransactionIcon);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
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

        String category = safeText(transaction.getCategory(), "Lainnya");
        String note = safeText(transaction.getNote(), category);
        boolean isIncome = "Pemasukan".equalsIgnoreCase(transaction.getType());

        holder.ivTransactionIcon.setImageResource(
                getTransactionIconRes(category, transaction.getType())
        );
        holder.tvNote.setText(note);
        holder.tvCategory.setText(category);
        holder.tvDate.setText(transaction.getDate());
        holder.tvDate.setVisibility(View.GONE);

        holder.tvAmount.setText(formatSignedRupiah(transaction.getAmount(), isIncome));
        holder.tvAmount.setTextColor(
                isIncome ? 0xFF306D29 : 0xFFB3261E
        );
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value.trim();
    }

    private String formatSignedRupiah(double amount, boolean isIncome) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        String sign = isIncome ? "+ " : "- ";

        return sign + "Rp " + formatter.format(Math.abs(Math.round(amount)));
    }

    private int getTransactionIconRes(String category, String type) {
        if ("Pemasukan".equalsIgnoreCase(type)) {
            return R.drawable.ic_category_income_active;
        }

        String normalizedCategory = category == null
                ? ""
                : category.toLowerCase(Locale.ROOT);

        if (normalizedCategory.contains("makan") || normalizedCategory.contains("food")) {
            return R.drawable.ic_category_food_active;
        } else if (normalizedCategory.contains("camilan")
                || normalizedCategory.contains("snack")) {
            return R.drawable.ic_category_snack_active;
        } else if (normalizedCategory.contains("belanja")) {
            return R.drawable.ic_category_shopping_active;
        } else if (normalizedCategory.contains("tagihan")
                || normalizedCategory.contains("bill")) {
            return R.drawable.ic_category_bill_active;
        } else if (normalizedCategory.contains("kesehatan")) {
            return R.drawable.ic_category_health_active;
        } else if (normalizedCategory.contains("edukasi")
                || normalizedCategory.contains("pendidikan")) {
            return R.drawable.ic_category_education_active;
        } else if (normalizedCategory.contains("hiburan")) {
            return R.drawable.ic_category_entertainment_active;
        } else if (normalizedCategory.contains("tabungan")) {
            return R.drawable.ic_category_savings_active;
        } else if (normalizedCategory.contains("sewa kos")
                || normalizedCategory.contains("rumah")) {
            return R.drawable.ic_category_house_active;
        } else if (normalizedCategory.contains("lain")) {
            return R.drawable.ic_category_other_active;
        }

        return R.drawable.ic_category_other_active;
    }
}
