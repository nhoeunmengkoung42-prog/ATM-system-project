// Account and its two subclasses, combined into one file.
// No logic was changed - only merged together.

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

abstract class Account {
    private String accountNumber;
    private String pin;
    private String name;
    private double balance;

    private int failedAttempts = 0;
    private boolean locked = false;

    private ArrayList<Transaction> transactions = new ArrayList<>();

    public Account(String accountNumber, String pin,
                   String name, double balance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.name = name;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getPin() {
        return pin;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Restores locked/failedAttempts state when an account is loaded
     * back from the database. Not for normal use during a session.
     */
    public void restoreState(int failedAttempts, boolean locked) {
        this.failedAttempts = failedAttempts;
        this.locked = locked;
    }

    /**
     * Re-adds a transaction that already happened (loaded from the
     * database), keeping its original timestamp instead of "now".
     * Does not touch the balance, since the balance loaded from the
     * database is already correct.
     */
    public void loadPastTransaction(String type, double amount,
                                     double balanceAfter, LocalDateTime date) {
        transactions.add(new Transaction(type, amount, balanceAfter, date));
    }

    public void login(String enteredPin)
            throws InvalidPinException {

        if (locked) {
            throw new InvalidPinException("Account is locked.");
        }

        if (!pin.equals(enteredPin)) {
            failedAttempts++;

            if (failedAttempts >= 3) {
                locked = true;
                throw new InvalidPinException(
                    "Wrong PIN. Account locked after 3 attempts."
                );
            }

            throw new InvalidPinException(
                "Wrong PIN. Attempts left: " + (3 - failedAttempts)
            );
        }

        failedAttempts = 0;
    }

    public void changePin(String oldPin, String newPin)
            throws InvalidPinException {

        if (!pin.equals(oldPin)) {
            throw new InvalidPinException("Old PIN is incorrect.");
        }

        if (!newPin.matches("\\d{4}")) {
            throw new InvalidPinException(
                "PIN must contain exactly 4 digits."
            );
        }

        pin = newPin;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException(
                "Amount must be greater than 0."
            );
        }

        balance += amount;

        transactions.add(
            new Transaction("DEPOSIT", amount, balance)
        );
    }

    public void withdraw(double amount)
            throws InsufficientFundsException {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                "Amount must be greater than 0."
            );
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                "Not enough balance."
            );
        }

        balance -= amount;

        transactions.add(
            new Transaction("WITHDRAW", amount, balance)
        );
    }

    public void transferOut(double amount)
            throws InsufficientFundsException {

        if (amount <= 0) {
            throw new IllegalArgumentException(
                "Amount must be greater than 0."
            );
        }

        if (amount > balance) {
            throw new InsufficientFundsException(
                "Not enough balance."
            );
        }

        balance -= amount;

        transactions.add(
            new Transaction("TRANSFER OUT", amount, balance)
        );
    }

    public void transferIn(double amount) {
        balance += amount;

        transactions.add(
            new Transaction("TRANSFER IN", amount, balance)
        );
    }

    public void showHistory() {
        System.out.println("\n===== TRANSACTION HISTORY =====");

        if (transactions.isEmpty()) {
            System.out.println("No transactions.");
            return;
        }

        for (Transaction t : transactions) {
            t.show();
        }
    }

    public void unlock() {
        locked = false;
        failedAttempts = 0;
    }

    public abstract String getAccountType();

    public abstract void monthlyUpdate();
}

class SavingsAccount extends Account {
    private double interestRate;

    public SavingsAccount(
            String accountNumber,
            String pin,
            String name,
            double balance,
            double interestRate) {

        super(accountNumber, pin, name, balance);
        this.interestRate = interestRate;
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

    public double getInterestRate() {
        return interestRate;
    }

    @Override
    public void monthlyUpdate() {
        double interest = getBalance() * interestRate;

        if (interest <= 0) {
            System.out.println("No interest to add (balance is $0).");
            return;
        }

        deposit(interest);

        System.out.printf(
            "Interest added: $%.2f%n",
            interest
        );
    }
}

class CheckingAccount extends Account {
    private double dailyLimit;
    private double withdrawnToday = 0;
    private LocalDate lastWithdrawDate = LocalDate.now();

    public CheckingAccount(
            String accountNumber,
            String pin,
            String name,
            double balance,
            double dailyLimit) {

        super(accountNumber, pin, name, balance);
        this.dailyLimit = dailyLimit;
    }

    @Override
    public String getAccountType() {
        return "Checking";
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public double getWithdrawnToday() {
        return withdrawnToday;
    }

    public LocalDate getLastWithdrawDate() {
        return lastWithdrawDate;
    }

    /** Restores today's withdrawal tracking when loaded from the database. */
    public void restoreDailyState(double withdrawnToday, LocalDate lastWithdrawDate) {
        this.withdrawnToday = withdrawnToday;
        this.lastWithdrawDate = (lastWithdrawDate != null)
                ? lastWithdrawDate
                : LocalDate.now();
    }

    @Override
    public void withdraw(double amount)
            throws InsufficientFundsException {

        if (!LocalDate.now().equals(lastWithdrawDate)) {
            withdrawnToday = 0;
            lastWithdrawDate = LocalDate.now();
        }

        if (withdrawnToday + amount > dailyLimit) {
            throw new InsufficientFundsException(
                "Daily withdrawal limit exceeded. Limit: $" + dailyLimit
            );
        }

        super.withdraw(amount);
        withdrawnToday += amount;
    }

    @Override
    public void monthlyUpdate() {
        System.out.println("Checking account updated.");
    }
}
