package com.java.kalkulatorkeuangan;

public class Transaction {

    private String type;
    private double amount;
    private String category;
    private String note;
    private String date;

    public Transaction(String type, double amount, String note, String date) {
        this(type, amount, "", note, date);
    }

    public Transaction(String type, double amount, String category, String note, String date) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getNote() {
        return note;
    }

    public String getDate() {
        return date;
    }
}
