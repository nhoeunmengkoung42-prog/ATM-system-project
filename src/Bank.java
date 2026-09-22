import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Bank now talks to PostgreSQL instead of keeping accounts only in
 * memory. `accounts` below is an in-memory cache that mirrors the
 * "accounts" table: it's loaded once at startup (loadAccounts) and
 * kept in sync by calling saveAccount(...) after anything changes
 * an account (deposit, withdraw, PIN change, lock/unlock, etc).
 */
class Bank {
    private ArrayList<Account> accounts = new ArrayList<>();
    private Connection connection;

    public Bank() {
        connection = DatabaseConnection.getConnection();

        if (connection == null) {
            System.out.println(
                "Could not connect to the database. " +
                "Check DatabaseConnection.java and make sure PostgreSQL is running."
            );
            System.exit(1);
        }

        loadAccounts();
    }

    // ---------- Loading from the database ----------

    private void loadAccounts() {
        String sql = "SELECT * FROM accounts";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Account account = buildAccountFromRow(rs);
                loadTransactionsFor(account);
                accounts.add(account);
            }

        } catch (SQLException e) {
            System.out.println("Failed to load accounts: " + e.getMessage());
        }
    }

    private Account buildAccountFromRow(ResultSet rs) throws SQLException {
        String number = rs.getString("account_number");
        String pin = rs.getString("pin");
        String name = rs.getString("name");
        String type = rs.getString("account_type");
        double balance = rs.getDouble("balance");

        Account account;

        if ("SAVINGS".equals(type)) {
            double interestRate = rs.getDouble("interest_rate");
            account = new SavingsAccount(number, pin, name, balance, interestRate);

        } else {
            double dailyLimit = rs.getDouble("daily_limit");
            CheckingAccount checking =
                    new CheckingAccount(number, pin, name, balance, dailyLimit);

            double withdrawnToday = rs.getDouble("withdrawn_today");
            java.sql.Date lastWithdrawSqlDate = rs.getDate("last_withdraw_date");
            LocalDate lastWithdrawDate = (lastWithdrawSqlDate != null)
                    ? lastWithdrawSqlDate.toLocalDate()
                    : LocalDate.now();

            checking.restoreDailyState(withdrawnToday, lastWithdrawDate);
            account = checking;
        }

        account.restoreState(rs.getInt("failed_attempts"), rs.getBoolean("locked"));
        return account;
    }

    private void loadTransactionsFor(Account account) {
        String sql = "SELECT * FROM transactions " +
                     "WHERE account_number = ? ORDER BY txn_date ASC, id ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getAccountNumber());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("txn_date");

                    account.loadPastTransaction(
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_after"),
                        ts.toLocalDateTime()
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println(
                "Failed to load transaction history for " +
                account.getAccountNumber() + ": " + e.getMessage()
            );
        }
    }

    // ---------- Lookup / display ----------

    public Account findAccount(String number)
            throws AccountNotFoundException {

        for (Account account : accounts) {
            if (account.getAccountNumber().equals(number)) {
                return account;
            }
        }

        throw new AccountNotFoundException("Account not found.");
    }

    public void showAllAccounts() {
        System.out.println("\n========== ALL ACCOUNTS ==========");

        if (accounts.isEmpty()) {
            System.out.println("No accounts yet.");
            return;
        }

        for (Account account : accounts) {
            System.out.printf(
                "Account: %s | Name: %s | Type: %s | Balance: $%.2f | Locked: %s%n",
                account.getAccountNumber(),
                account.getName(),
                account.getAccountType(),
                account.getBalance(),
                account.isLocked()
            );
        }
    }

    // ---------- Creating accounts ----------

    public void createAccount(Scanner sc) {
        System.out.print("Account number: ");
        String number = sc.nextLine();

        try {
            findAccount(number);
            System.out.println("An account with that number already exists.");
            return;
        } catch (AccountNotFoundException e) {
            // good - number is free
        }

        System.out.print("Name: ");
        String name = sc.nextLine();

        System.out.print("4-digit PIN: ");
        String pin = sc.nextLine();

        if (!pin.matches("\\d{4}")) {
            System.out.println("PIN must contain 4 digits.");
            return;
        }

        System.out.print("Initial balance: ");
        double balance = ATMSystemInput.readDouble(sc);

        System.out.println("1. Savings");
        System.out.println("2. Checking");
        System.out.print("Choose type: ");

        int type = ATMSystemInput.readInt(sc);

        Account account;

        if (type == 1) {
            account = new SavingsAccount(number, pin, name, balance, 0.02);

        } else if (type == 2) {
            account = new CheckingAccount(number, pin, name, balance, 1000);

        } else {
            System.out.println("Invalid account type.");
            return;
        }

        if (insertAccount(account)) {
            accounts.add(account);
            System.out.println(account.getAccountType() + " account created.");
        }
    }

    /**
     * Creates an account that was already built elsewhere (e.g. by the
     * ATM's "Create Customer Account" flow), inserting it into the same
     * "accounts" table and in-memory list used everywhere else, so it's
     * immediately findable via findAccount()/customer login.
     */
    public boolean createAccount(Account account) {
        if (insertAccount(account)) {
            accounts.add(account);
            return true;
        }
        return false;
    }

    private boolean insertAccount(Account account) {
        String sql = "INSERT INTO accounts " +
            "(account_number, pin, name, account_type, balance, locked, failed_attempts, " +
            " interest_rate, daily_limit, withdrawn_today, last_withdraw_date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getAccountNumber());
            stmt.setString(2, account.getPin());
            stmt.setString(3, account.getName());
            stmt.setString(4, account.getAccountType().toUpperCase());
            stmt.setDouble(5, account.getBalance());
            stmt.setBoolean(6, account.isLocked());
            stmt.setInt(7, account.getFailedAttempts());

            if (account instanceof SavingsAccount) {
                SavingsAccount s = (SavingsAccount) account;
                stmt.setDouble(8, s.getInterestRate());
                stmt.setNull(9, java.sql.Types.NUMERIC);
                stmt.setNull(10, java.sql.Types.NUMERIC);
                stmt.setNull(11, java.sql.Types.DATE);

            } else {
                CheckingAccount c = (CheckingAccount) account;
                stmt.setNull(8, java.sql.Types.NUMERIC);
                stmt.setDouble(9, c.getDailyLimit());
                stmt.setDouble(10, c.getWithdrawnToday());
                stmt.setDate(11, java.sql.Date.valueOf(c.getLastWithdrawDate()));
            }

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("Failed to create account: " + e.getMessage());
            return false;
        }
    }

    // ---------- Saving changes back to the database ----------

    /**
     * Call this after any operation that changes an account
     * (deposit, withdraw, transfer, PIN change, lock/unlock,
     * monthly update, etc.) so the database stays in sync.
     */
    public void saveAccount(Account account) {
        String sql = "UPDATE accounts SET pin = ?, name = ?, balance = ?, " +
            "locked = ?, failed_attempts = ?, interest_rate = ?, daily_limit = ?, " +
            "withdrawn_today = ?, last_withdraw_date = ? WHERE account_number = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getPin());
            stmt.setString(2, account.getName());
            stmt.setDouble(3, account.getBalance());
            stmt.setBoolean(4, account.isLocked());
            stmt.setInt(5, account.getFailedAttempts());

            if (account instanceof SavingsAccount) {
                SavingsAccount s = (SavingsAccount) account;
                stmt.setDouble(6, s.getInterestRate());
                stmt.setNull(7, java.sql.Types.NUMERIC);
                stmt.setNull(8, java.sql.Types.NUMERIC);
                stmt.setNull(9, java.sql.Types.DATE);

            } else {
                CheckingAccount c = (CheckingAccount) account;
                stmt.setNull(6, java.sql.Types.NUMERIC);
                stmt.setDouble(7, c.getDailyLimit());
                stmt.setDouble(8, c.getWithdrawnToday());
                stmt.setDate(9, java.sql.Date.valueOf(c.getLastWithdrawDate()));
            }

            stmt.setString(10, account.getAccountNumber());
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Failed to save account: " + e.getMessage());
        }
    }

    /**
     * Persists the most recent transaction recorded on this account
     * (the last entry in account.getTransactions()) into the
     * transactions table. Call this right after an operation that
     * adds exactly one new transaction to the account.
     */
    public void saveLatestTransaction(Account account) {
        ArrayList<Transaction> txns = account.getTransactions();

        if (txns.isEmpty()) {
            return;
        }

        Transaction t = txns.get(txns.size() - 1);

        String sql = "INSERT INTO transactions " +
            "(account_number, type, amount, balance_after, txn_date) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, account.getAccountNumber());
            stmt.setString(2, t.getType());
            stmt.setDouble(3, t.getAmount());
            stmt.setDouble(4, t.getBalance());
            stmt.setTimestamp(5, Timestamp.valueOf(t.getDate()));

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Failed to save transaction: " + e.getMessage());
        }
    }
}
