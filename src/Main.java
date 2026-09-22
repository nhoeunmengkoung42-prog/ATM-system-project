public class Main {

    public static void main(String[] args) {

        // Bank connects to PostgreSQL and loads whatever accounts
        // already exist in the database - nothing is hardcoded here.
        // Add accounts yourself either through the Admin menu
        // (Admin Login -> Create Account) or directly in SQL.
        Bank bank = new Bank();

        ATM atm = new ATM(bank);

        atm.start();
    }
}
