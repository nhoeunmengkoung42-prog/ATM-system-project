import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


class DatabaseConnection {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/atm_system";

    private static final String USER = "postgres";

    private static final String PASSWORD = "991991";

    public static Connection getConnection() {
        try {
            Connection connection =
                    DriverManager.getConnection(URL, USER, PASSWORD);
            return connection;

        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            System.out.println(e.getMessage());
            return null;
        }
    }
}

class AdminDatabase {

    public boolean createAdmin(Admin admin) {

        String sql = "INSERT INTO admins " +
                     "(admin_id, username, password, full_name) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, admin.getAdminId());
            statement.setString(2, admin.getUsername());
            statement.setString(3, admin.getPassword());
            statement.setString(4, admin.getFullName());

            statement.executeUpdate();

            return true;

        } catch (Exception e) {

            System.out.println("Could not create admin.");
            System.out.println(e.getMessage());

            return false;
        }
    }

    public boolean adminExists(String username) {

        String sql = "SELECT username FROM admins " +
                     "WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            ResultSet result = statement.executeQuery();

            return result.next();

        } catch (Exception e) {

            System.out.println("Error checking admin.");
            System.out.println(e.getMessage());

            return false;
        }
    }

    public Admin login(String username, String password) {

        String sql = "SELECT * FROM admins " +
                     "WHERE username = ? AND password = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);

            ResultSet result = statement.executeQuery();

            if (result.next()) {

                return new Admin(
                    result.getString("admin_id"),
                    result.getString("username"),
                    result.getString("password"),
                    result.getString("full_name")
                );
            }

        } catch (Exception e) {

            System.out.println("Admin login error.");
            System.out.println(e.getMessage());
        }

        return null;
    }
}

class CustomerDatabase {

    public boolean createCustomer(Customer customer,
                                  double balance,
                                  String accountType,
                                  double interestRate,
                                  double overdraftLimit) {

        String sql = "INSERT INTO customers " +
                     "(account_number, pin, full_name, balance, " +
                     "account_type, interest_rate, overdraft_limit) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, customer.getAccountNumber());
            statement.setString(2, customer.getPin());
            statement.setString(3, customer.getFullName());
            statement.setDouble(4, balance);
            statement.setString(5, accountType);

            if (accountType.equalsIgnoreCase("SAVINGS")) {
                statement.setDouble(6, interestRate);
                statement.setNull(7, java.sql.Types.DECIMAL);
            } else {
                statement.setNull(6, java.sql.Types.DECIMAL);
                statement.setDouble(7, overdraftLimit);
            }

            statement.executeUpdate();

            return true;

        } catch (Exception e) {

            System.out.println("Could not create customer.");
            System.out.println(e.getMessage());

            return false;
        }
    }

    public boolean customerExists(String accountNumber) {

        String sql = "SELECT account_number FROM customers " +
                     "WHERE account_number = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, accountNumber);

            ResultSet result = statement.executeQuery();

            return result.next();

        } catch (Exception e) {

            System.out.println("Error checking customer.");
            System.out.println(e.getMessage());

            return false;
        }
    }
}
