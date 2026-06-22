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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TRANSACTION = 1;

    private final List<RiwayatListItem> riwayatListItems = new ArrayList<>();

    public TransactionAdapter(List<Transaction> transactionList) {
        buildGroupedItems(transactionList);
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
        }
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTransactionIcon;
        TextView tvNote, tvDate, tvCategory, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTransactionIcon = itemView.findViewById(R.id.ivTransactionIcon);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return riwayatListItems.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(
                    R.layout.item_transaction_date_header,
                    parent,
                    false
            );
            return new HeaderViewHolder(view);
        }

        View view = inflater.inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position) {

        RiwayatListItem item = riwayatListItems.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvDateHeader.setText(item.headerText);
            return;
        }

        TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;
        Transaction transaction = item.transaction;
        String category = safeText(transaction.getCategory(), "Lainnya");
        String note = safeText(transaction.getNote(), category);
        boolean isIncome = "Pemasukan".equalsIgnoreCase(transaction.getType());

        transactionHolder.ivTransactionIcon.setImageResource(
                getTransactionIconRes(category, transaction.getType())
        );
        transactionHolder.tvNote.setText(note);
        transactionHolder.tvCategory.setText(category);
        transactionHolder.tvDate.setText(transaction.getDate());
        transactionHolder.tvDate.setVisibility(View.GONE);
        transactionHolder.tvAmount.setText(
                formatSignedRupiah(transaction.getAmount(), isIncome)
        );
        transactionHolder.tvAmount.setTextColor(
                isIncome ? 0xFF306D29 : 0xFFB3261E
        );
    }

    @Override
    public int getItemCount() {
        return riwayatListItems.size();
    }

    private void buildGroupedItems(List<Transaction> transactionList) {
        String currentDateKey = "";

        for (Transaction transaction : transactionList) {
            ParsedTransactionDate parsedDate = parseTransactionDate(transaction.getDate());
            String dateKey = parsedDate.validDate
                    ? parsedDate.year + "/" + parsedDate.month + "/" + parsedDate.day
                    : "invalid";

            if (!dateKey.equals(currentDateKey)) {
                currentDateKey = dateKey;
                riwayatListItems.add(RiwayatListItem.header(formatHeaderText(parsedDate)));
            }

            riwayatListItems.add(RiwayatListItem.transaction(transaction));
        }
    }

    private String formatHeaderText(ParsedTransactionDate parsedDate) {
        if (!parsedDate.validDate) {
            return "Tanggal tidak valid";
        }

        Calendar selectedDate = Calendar.getInstance();
        selectedDate.clear();
        selectedDate.set(parsedDate.year, parsedDate.month - 1, parsedDate.day);

        String dateText = parsedDate.day + " "
                + getIndonesianMonthName(parsedDate.month - 1)
                + " "
                + parsedDate.year;

        Calendar today = Calendar.getInstance();
        clearTime(today);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        if (isSameDate(selectedDate, today)) {
            return "Hari ini, " + dateText;
        }

        if (isSameDate(selectedDate, yesterday)) {
            return "Kemarin, " + dateText;
        }

        return getIndonesianDayName(selectedDate) + ", " + dateText;
    }

    private ParsedTransactionDate parseTransactionDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return ParsedTransactionDate.invalid();
        }

        String[] dateParts = dateText.trim().split("/");

        if (dateParts.length != 3) {
            return ParsedTransactionDate.invalid();
        }

        try {
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);

            if (month < 1 || month > 12) {
                return ParsedTransactionDate.invalid();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setLenient(false);
            calendar.clear();
            calendar.set(year, month - 1, day);
            calendar.getTime();

            return new ParsedTransactionDate(true, day, month, year);
        } catch (IllegalArgumentException exception) {
            return ParsedTransactionDate.invalid();
        }
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean isSameDate(Calendar firstDate, Calendar secondDate) {
        return firstDate.get(Calendar.YEAR) == secondDate.get(Calendar.YEAR)
                && firstDate.get(Calendar.MONTH) == secondDate.get(Calendar.MONTH)
                && firstDate.get(Calendar.DAY_OF_MONTH) == secondDate.get(Calendar.DAY_OF_MONTH);
    }

    private String getIndonesianDayName(Calendar date) {
        String[] dayNames = {
                "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
        };
        return dayNames[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private String getIndonesianMonthName(int month) {
        String[] monthNames = {
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };
        return monthNames[month];
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

    private static class RiwayatListItem {
        int viewType;
        String headerText;
        Transaction transaction;

        static RiwayatListItem header(String headerText) {
            RiwayatListItem item = new RiwayatListItem();
            item.viewType = VIEW_TYPE_HEADER;
            item.headerText = headerText;
            return item;
        }

        static RiwayatListItem transaction(Transaction transaction) {
            RiwayatListItem item = new RiwayatListItem();
            item.viewType = VIEW_TYPE_TRANSACTION;
            item.transaction = transaction;
            return item;
        }
    }

    private static class ParsedTransactionDate {
        boolean validDate;
        int day;
        int month;
        int year;

        ParsedTransactionDate(boolean validDate, int day, int month, int year) {
            this.validDate = validDate;
            this.day = day;
            this.month = month;
            this.year = year;
        }

        static ParsedTransactionDate invalid() {
            return new ParsedTransactionDate(false, 0, 0, 0);
        }
    }
}
