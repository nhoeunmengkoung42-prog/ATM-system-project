ATM System - PostgreSQL Edition
==================================

This version stores everything in a real PostgreSQL database instead
of memory. There is NO sample data anywhere in the code - Main.java
is empty, and the app starts with whatever accounts already exist in
your database. You add accounts yourself, either:
  (a) through the app itself: Admin Login -> Create Account, or
  (b) directly with SQL (INSERT INTO accounts ...).

------------------------------------------------------------------
STEP 1 - Create the database
------------------------------------------------------------------
Open a terminal (or psql / pgAdmin) and run:

    createdb -U postgres atm_system

(If that command isn't available, open psql and run instead:
    CREATE DATABASE atm_system;
)

------------------------------------------------------------------
STEP 2 - Create the tables
------------------------------------------------------------------
Run the included schema.sql against the new database:

    psql -U postgres -d atm_system -f schema.sql

This creates two tables:
  - accounts      (one row per account, current balance/state)
  - transactions  (one row per deposit/withdraw/transfer/etc.)

Open schema.sql if you want to see exactly what it creates - it's
short and commented.

------------------------------------------------------------------
STEP 3 - Put in your own PostgreSQL password
------------------------------------------------------------------
Open src/DatabaseConnection.java and edit these three lines to match
your own setup:

    private static final String URL =
            "jdbc:postgresql://localhost:5432/atm_system";
    private static final String USER = "postgres";
    private static final String PASSWORD = "YOUR_POSTGRES_PASSWORD";

Replace YOUR_POSTGRES_PASSWORD with your actual PostgreSQL password.
(If your database isn't on localhost:5432, or is named something
else, update the URL too.)

------------------------------------------------------------------
STEP 4 - Run it in VS Code
------------------------------------------------------------------
1. Open the ATM_System folder in VS Code.
2. Install the "Extension Pack for Java" extension if you don't
   already have it.
3. The lib/postgresql-42.7.2.jar file in this folder is the
   PostgreSQL JDBC driver the code needs. Add it to your project:
      - Command Palette (Ctrl+Shift+P) -> "Java: Configure Classpath"
      - Under "Referenced Libraries" click "+" and select
        lib/postgresql-42.7.2.jar
   (VS Code will remember this in a .vscode/settings.json file it
   creates automatically.)
4. Open src/Main.java and click "Run" above main(), or press Ctrl+F5.

------------------------------------------------------------------
RUNNING FROM THE TERMINAL INSTEAD (no VS Code needed)
------------------------------------------------------------------
    cd ATM_System/src
    javac -cp ../lib/postgresql-42.7.2.jar -d ../out *.java
    java -cp ../out:../lib/postgresql-42.7.2.jar Main

(On Windows, use a semicolon instead of a colon in the -cp classpath:
    java -cp ../out;../lib/postgresql-42.7.2.jar Main
)

------------------------------------------------------------------
ADDING YOUR OWN DATA
------------------------------------------------------------------
Easiest: run the app, choose "2. Admin Login", password is admin123,
then "2. Create Account".

Or insert directly with SQL, e.g.:

    INSERT INTO accounts
      (account_number, pin, name, account_type, balance,
       locked, failed_attempts, interest_rate)
    VALUES
      ('100001', '1234', 'Dara', 'SAVINGS', 1000.00, false, 0, 0.02);

    INSERT INTO accounts
      (account_number, pin, name, account_type, balance,
       locked, failed_attempts, daily_limit, withdrawn_today)
    VALUES
      ('100002', '5678', 'Sokha', 'CHECKING', 500.00, false, 0, 1000.00, 0);

account_type must be exactly 'SAVINGS' or 'CHECKING' (uppercase).

------------------------------------------------------------------
WHAT CHANGED FROM THE IN-MEMORY VERSION
------------------------------------------------------------------
- Main.java no longer creates any sample accounts - it just connects
  and starts the ATM.
- Bank.java now loads accounts from PostgreSQL at startup and writes
  every change (deposit, withdraw, transfer, PIN change, lock/unlock,
  monthly update) straight back to the database, so nothing is lost
  when the program exits.
- Transaction history is also stored in the database (transactions
  table) and reloaded on every startup, so "Transaction History"
  still works correctly even after restarting the app.
- I tested this end-to-end against a real local PostgreSQL instance:
  creating accounts, deposits, withdrawals, transfers, PIN lockout
  after 3 wrong attempts, and admin unlock - then restarted the
  program from scratch each time to confirm the data really
  persisted in the database rather than just in memory.
