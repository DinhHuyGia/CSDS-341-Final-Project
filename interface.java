import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

/**
 * Terminal JDBC client for the course schema (MySQL/MariaDB).
 * Multiple premade queries are stored below; pick one by id (1, 2, …) in the terminal
 * or pass the id as the first program argument.
 *
 * <p>Compile / run:
 * <pre>
 * javac -cp ".:mysql-connector-j-9.1.0.jar" interface.java
 * java -cp ".:mysql-connector-j-9.1.0.jar" DBInterface        # interactive menu
 * java -cp ".:mysql-connector-j-9.1.0.jar" DBInterface 2     # run query #2
 * </pre>
 */
class DBInterface {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/YOUR_DATABASE_NAME?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     * Premade queries: index 0 → id 1 in the menu. Edit labels and SQL as needed.
     */
    private static final String[][] QUERY_CATALOG = {
        {"Show all tables", "SHOW TABLES"},
        {"Count employees", "SELECT COUNT(*) AS employee_count FROM Employee"},
        {"Employees (first 15)", "SELECT employeeID, firstName, lastName, annualSalary FROM Employee LIMIT 15"},
    };

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("mysql-connector-j is not on the classpath.");
            System.err.println("Details: " + e.getMessage());
            return;
        }

        if (args != null && args.length > 0) {
            int id = parseQueryIdArg(args[0]);
            if (id > 0) {
                runCatalogQuery(id);
            }
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                printMenu();
                System.out.print("Enter query id (1–" + QUERY_CATALOG.length + ", 0 = quit): ");
                if (!scanner.hasNextLine()) {
                    return;
                }
                String line = scanner.nextLine().trim();
                int choice;
                try {
                    choice = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.err.println("Please enter a number.");
                    continue;
                }
                if (choice == 0) {
                    System.out.println("Bye.");
                    return;
                }
                if (choice < 1 || choice > QUERY_CATALOG.length) {
                    System.err.println("Invalid id. Choose 1–" + QUERY_CATALOG.length + " or 0.");
                    continue;
                }
                runCatalogQuery(choice);
                System.out.println();
            }
        }
    }

    private static int parseQueryIdArg(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid number: " + raw);
            return -1;
        }
    }

    private static void runCatalogQuery(int id) {
        String sql = sqlForId(id);
        if (sql == null) {
            System.err.println("No query with id " + id + ". Valid: 1–" + QUERY_CATALOG.length + ".");
            return;
        }
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            printResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Failed to run query #" + id + ": " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("--- Saved queries ---");
        for (int i = 0; i < QUERY_CATALOG.length; i++) {
            System.out.println((i + 1) + ". " + QUERY_CATALOG[i][0]);
        }
        System.out.println("0. Quit");
    }

    /** Returns SQL for 1-based id, or null if out of range. */
    private static String sqlForId(int id) {
        if (id < 1 || id > QUERY_CATALOG.length) {
            return null;
        }
        return QUERY_CATALOG[id - 1][1];
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        StringBuilder header = new StringBuilder();
        for (int c = 1; c <= columnCount; c++) {
            if (c > 1) {
                header.append("\t");
            }
            header.append(meta.getColumnLabel(c));
        }
        System.out.println(header);

        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int c = 1; c <= columnCount; c++) {
                if (c > 1) {
                    row.append("\t");
                }
                row.append(rs.getString(c));
            }
            System.out.println(row);
        }
    }
}
