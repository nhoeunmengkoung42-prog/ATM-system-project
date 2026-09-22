// Plain data-holder classes, combined into one file.
// No logic was changed - only merged together.

import java.time.LocalDateTime;

class Customer {

    private String accountNumber;
    private String pin;
    private String fullName;

    public Customer(String accountNumber, String pin, String fullName) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.fullName = fullName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getPin() {
        return pin;
    }

    public String getFullName() {
        return fullName;
    }
}

class Admin {

    private String adminId;
    private String username;
    private String password;
    private String fullName;

    public Admin(String adminId, String username,
                 String password, String fullName) {

        this.adminId = adminId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }
}

class Transaction {
    private String type;
    private double amount;
    private LocalDateTime date;
    private double balance;

    public Transaction(String type, double amount, double balance) {
        this(type, amount, balance, LocalDateTime.now());
    }

    public Transaction(String type, double amount, double balance, LocalDateTime date) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.balance = balance;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalance() {
        return balance;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void show() {
        System.out.printf(
            "%s | Amount: $%.2f | Balance: $%.2f | %s%n",
            type, amount, balance, date
        );
    }
}
