import java.util.Scanner;

class ATM {

    private Bank bank;
    private Scanner sc;

    private CustomerDatabase customerDatabase;
    private AdminDatabase adminDatabase;

    public ATM(Bank bank) {

        this.bank = bank;
        this.sc = new Scanner(System.in);

        customerDatabase = new CustomerDatabase();
        adminDatabase = new AdminDatabase();
    }

    public void start() {

        while (true) {

            System.out.println("\n========== ATM SYSTEM ==========");
            System.out.println("1. Create Account");
            System.out.println("2. Customer Login");
            System.out.println("3. Admin Login");
            System.out.println("4. Exit");
            System.out.print("Choose: ");

            int choice = ATMSystemInput.readInt(sc);

            switch (choice) {

                case 1:
                    createAccountMenu();
                    break;

                case 2:
                    customerLogin();
                    break;

                case 3:
                    adminLogin();
                    break;

                case 4:
                    System.out.println("Thank you for using ATM.");
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private void createAccountMenu() {

        while (true) {

            System.out.println("\n========== CREATE ACCOUNT ==========");
            System.out.println("1. Create Customer Account");
            System.out.println("2. Create Admin Account");
            System.out.println("3. Back");
            System.out.print("Choose: ");

            int choice = ATMSystemInput.readInt(sc);

            switch (choice) {

                case 1:
                    createCustomerAccount();
                    break;

                case 2:
                    createAdminAccount();
                    break;

                case 3:
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private void createCustomerAccount() {

        System.out.println("\n========== CREATE CUSTOMER ACCOUNT ==========");

        System.out.print("Account number: ");
        String accountNumber = sc.nextLine();

        try {
            bank.findAccount(accountNumber);
            System.out.println("Account number already exists.");
            return;
        } catch (AccountNotFoundException e) {
            // good - number is free
        }

        System.out.print("PIN: ");
        String pin = sc.nextLine();

        System.out.print("Customer name: ");
        String name = sc.nextLine();

        System.out.print("Initial balance: ");
        double balance = ATMSystemInput.readDouble(sc);

        if (balance < 0) {
            System.out.println("Balance cannot be negative.");
            return;
        }

        System.out.println("\n========== ACCOUNT TYPE ==========");
        System.out.println("1. Savings Account");
        System.out.println("2. Checking Account");
        System.out.print("Choose: ");

        int type = ATMSystemInput.readInt(sc);

        String accountType;
        Account account;

        if (type == 1) {

            accountType = "SAVINGS";

            System.out.print("Interest rate: ");
            double interestRate = ATMSystemInput.readDouble(sc);

            account = new SavingsAccount(accountNumber, pin, name, balance, interestRate);

        } else if (type == 2) {

            accountType = "CHECKING";

            System.out.print("Overdraft limit: ");
            double overdraftLimit = ATMSystemInput.readDouble(sc);

            account = new CheckingAccount(accountNumber, pin, name, balance, overdraftLimit);

        } else {

            System.out.println("Invalid account type.");
            return;
        }

        // Goes through Bank so the new account lands in the same
        // "accounts" table (and in-memory list) that customer login
        // reads from - this is what "customers" table + CustomerDatabase
        // was missing.
        boolean success = bank.createAccount(account);

        if (success) {

            System.out.println("\nCustomer account created successfully!");
            System.out.println("Account Number: " + accountNumber);
            System.out.println("Customer Name: " + name);
            System.out.println("Account Type: " + accountType);
            System.out.printf("Balance: $%.2f%n", balance);
        }
    }

    private void createAdminAccount() {

        System.out.println("\n========== CREATE ADMIN ACCOUNT ==========");

        System.out.print("Admin ID: ");
        String adminId = sc.nextLine();

        System.out.print("Username: ");
        String username = sc.nextLine();

        try {

            if (adminDatabase.adminExists(username)) {

                System.out.println("Username already exists.");
                return;
            }

            System.out.print("Password: ");
            String password = sc.nextLine();

            System.out.print("Full name: ");
            String fullName = sc.nextLine();

            Admin admin =
                    new Admin(
                            adminId,
                            username,
                            password,
                            fullName
                    );

            boolean success =
                    adminDatabase.createAdmin(admin);

            if (success) {

                System.out.println("\nAdmin account created successfully!");
                System.out.println("Admin ID: " + adminId);
                System.out.println("Username: " + username);
                System.out.println("Full Name: " + fullName);
            }

        } catch (Exception e) {

            System.out.println("Error creating admin account.");
            System.out.println(e.getMessage());
        }
    }

    private void customerLogin() {

        System.out.print("Account number: ");
        String number = sc.nextLine();

        Account account;

        try {

            account = bank.findAccount(number);

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
            return;
        }

        System.out.print("PIN: ");
        String pin = sc.nextLine();

        try {

            account.login(pin);

            bank.saveAccount(account);

            System.out.println("Welcome, " + account.getName());

            customerMenu(account);

        } catch (InvalidPinException e) {

            bank.saveAccount(account);

            System.out.println("Error: " + e.getMessage());

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }

    private void customerMenu(Account account) {

        while (true) {

            System.out.println("\n========== CUSTOMER MENU ==========");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Transaction History");
            System.out.println("6. Change PIN");
            System.out.println("7. Monthly Update");
            System.out.println("8. Logout");
            System.out.print("Choose: ");

            int choice = ATMSystemInput.readInt(sc);

            try {

                switch (choice) {

                    case 1:

                        System.out.printf(
                                "Balance: $%.2f%n",
                                account.getBalance()
                        );

                        break;

                    case 2:

                        System.out.print("Deposit amount: ");

                        double deposit =
                                ATMSystemInput.readDouble(sc);

                        account.deposit(deposit);

                        bank.saveAccount(account);
                        bank.saveLatestTransaction(account);

                        System.out.println("Deposit successful.");

                        break;

                    case 3:

                        System.out.print("Withdrawal amount: ");

                        double withdraw =
                                ATMSystemInput.readDouble(sc);

                        account.withdraw(withdraw);

                        bank.saveAccount(account);
                        bank.saveLatestTransaction(account);

                        System.out.println("Withdrawal successful.");

                        break;

                    case 4:

                        transfer(account);

                        break;

                    case 5:

                        account.showHistory();

                        break;

                    case 6:

                        System.out.print("Old PIN: ");
                        String oldPin = sc.nextLine();

                        System.out.print("New 4-digit PIN: ");
                        String newPin = sc.nextLine();

                        account.changePin(oldPin, newPin);

                        bank.saveAccount(account);

                        System.out.println("PIN changed successfully.");

                        break;

                    case 7:

                        int txnCountBefore =
                                account.getTransactions().size();

                        account.monthlyUpdate();

                        bank.saveAccount(account);

                        if (account.getTransactions().size()
                                > txnCountBefore) {

                            bank.saveLatestTransaction(account);
                        }

                        break;

                    case 8:

                        System.out.println("Logged out.");

                        return;

                    default:

                        System.out.println("Invalid choice.");
                }

            } catch (Exception e) {

                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void transfer(Account sender) {

        System.out.print("Receiver account number: ");

        String receiverNumber = sc.nextLine();

        try {

            Account receiver =
                    bank.findAccount(receiverNumber);

            if (sender.getAccountNumber()
                    .equals(receiver.getAccountNumber())) {

                System.out.println(
                        "Cannot transfer to same account."
                );

                return;
            }

            System.out.print("Transfer amount: ");

            double amount =
                    ATMSystemInput.readDouble(sc);

            sender.transferOut(amount);

            receiver.transferIn(amount);

            bank.saveAccount(sender);
            bank.saveAccount(receiver);

            bank.saveLatestTransaction(sender);
            bank.saveLatestTransaction(receiver);

            System.out.println("Transfer successful.");

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }
    }

    private void adminLogin() {

        System.out.println("\n========== ADMIN LOGIN ==========");

        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        try {

            Admin admin =
                    adminDatabase.login(
                            username,
                            password
                    );

            if (admin == null) {

                System.out.println("Wrong username or password.");
                return;
            }

            System.out.println(
                    "Welcome, " + admin.getFullName()
            );

            adminMenu();

        } catch (Exception e) {

            System.out.println("Admin login error.");
            System.out.println(e.getMessage());
        }
    }

    private void adminMenu() {

        while (true) {

            System.out.println("\n========== ADMIN MENU ==========");
            System.out.println("1. Show All Accounts");
            System.out.println("2. Create Customer Account");
            System.out.println("3. Unlock Account");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            int choice = ATMSystemInput.readInt(sc);

            switch (choice) {

                case 1:

                    bank.showAllAccounts();

                    break;

                case 2:

                    createCustomerAccount();

                    break;

                case 3:

                    unlockAccount();

                    break;

                case 4:

                    System.out.println("Admin logged out.");

                    return;

                default:

                    System.out.println("Invalid choice.");
            }
        }
    }

    private void unlockAccount() {

        System.out.print("Account number: ");

        String number = sc.nextLine();

        try {

            Account account =
                    bank.findAccount(number);

            account.unlock();

            bank.saveAccount(account);

            System.out.println("Account unlocked.");

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }
    }
}

class ATMSystemInput {

    public static int readInt(Scanner sc) {
        while (true) {
            try {
                int value = Integer.parseInt(sc.nextLine());
                return value;

            } catch (NumberFormatException e) {
                System.out.print("Enter a valid number: ");
            }
        }
    }

    public static double readDouble(Scanner sc) {
        while (true) {
            try {
                double value = Double.parseDouble(sc.nextLine());

                if (value < 0) {
                    System.out.print("Amount cannot be negative: ");
                    continue;
                }

                return value;

            } catch (NumberFormatException e) {
                System.out.print("Enter a valid amount: ");
            }
        }
    }
}
