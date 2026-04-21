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
 * javac -cp ".:mysql-connector-j-9.6.0.jar" interface.java
 * java -cp ".:mysql-connector-j-9.6.0.jar" DBInterface        # interactive menu
 * java -cp ".:mysql-connector-j-9.6.0.jar" DBInterface 2     # run query #2
 * </pre>
 */
class DBInterface {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/company_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "Huykhang2005";

    /**
     * Premade queries: index 0 → id 1 in the menu. Edit labels and SQL as needed.
     */
    private static final String[][] QUERY_CATALOG = {
    {"Show all tables",
        "SHOW TABLES"},

    {"Employee count by department",
        "SELECT d.departmentName, COUNT(*) AS employeeCount " +
        "FROM Employee e, Department d " +
        "WHERE e.departmentID = d.departmentID " +
        "GROUP BY d.departmentName"},

    {"Employees in Human Resources Department",
        "SELECT e.* " +
        "FROM Employee e, Department d " +
        "WHERE e.departmentID = d.departmentID " +
        "AND d.departmentName = 'Human Resources'"},

    {"Employee count by country",
        "SELECT c.country, COUNT(*) AS employeeCount " +
        "FROM Employee e, Center c " +
        "WHERE e.centerID = c.centerID " +
        "GROUP BY c.country"},

    {"How many US North Atlantic Operations Hub QA employees with salary over 12000",
        "SELECT COUNT(*) AS qualifiedEmployees " +
        "FROM Employee e, Center c, Department d " +
        "WHERE e.centerID = c.centerID " +
        "AND e.departmentID = d.departmentID " +
        "AND c.country = 'United States' " +
        "AND c.centerName = 'North Atlantic Operations Hub' " +
        "AND d.departmentName = 'Quality Assurance' " +
        "AND e.annualSalary > 12000"},

    {"Project count for Emma Smith",
        "SELECT e.firstName, e.lastName, COUNT(a.projectID) AS projectCount " +
        "FROM Employee e, AssignedTo a " +
        "WHERE e.employeeID = a.employeeID " +
        "AND e.firstName = 'Emma' " +
        "AND e.lastName = 'Smith' " +
        "GROUP BY e.firstName, e.lastName"},

    {"Projects overseen by Oliver Bell",
        "SELECT p.projectName, p.deadline " +
        "FROM Project p, Employee e " +
        "WHERE p.supervisorID = e.employeeID " +
        "AND e.firstName = 'Oliver' " +
        "AND e.lastName = 'Bell'"},

    {"Projects under Quality Assurance supervisors",
        "SELECT DISTINCT p.projectID, p.projectName " +
        "FROM Project p, Employee e, Department d " +
        "WHERE p.supervisorID = e.employeeID " +
        "AND e.departmentID = d.departmentID " +
        "AND d.departmentName = 'Quality Assurance'"},

    {"Average salary in Quality Assurance",
        "SELECT d.departmentName, AVG(e.annualSalary) AS avgSalary " +
        "FROM Employee e, Department d " +
        "WHERE e.departmentID = d.departmentID " +
        "AND d.departmentName = 'Quality Assurance' " +
        "GROUP BY d.departmentName"},

    {"Employees not assigned to any project",
        "SELECT e.employeeID, e.firstName, e.lastName " +
        "FROM Employee e " +
        "WHERE NOT EXISTS ( " +
        "    SELECT * " +
        "    FROM AssignedTo a " +
        "    WHERE a.employeeID = e.employeeID" +
        ")"},

    {"Supervisors with no active projects",
        "SELECT e.employeeID, e.firstName, e.lastName " +
        "FROM Supervisor s, Employee e " +
        "WHERE s.supervisorID = e.employeeID " +
        "AND NOT EXISTS ( " +
        "    SELECT * " +
        "    FROM Project p " +
        "    WHERE p.supervisorID = s.supervisorID " +
        "    AND p.active = TRUE" +
        ")"}
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
                System.out.print("Enter query id (1-" + QUERY_CATALOG.length + ", 0 = quit): ");
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
                    System.err.println("Invalid id. Choose 1-" + QUERY_CATALOG.length + " or 0.");
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
            System.err.println("No query with id " + id + ". Valid: 1-" + QUERY_CATALOG.length + ".");
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
        System.out.println("--- Menu ---");
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

        // Store column widths
        int[] columnWidths = new int[columnCount];

        // Initialize with column name lengths
        for (int i = 1; i <= columnCount; i++) {
            columnWidths[i - 1] = meta.getColumnLabel(i).length();
        }

        // Store rows
        java.util.List<String[]> rows = new java.util.ArrayList<>();

        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                if (value == null) value = "NULL";
                row[i - 1] = value;

                columnWidths[i - 1] = Math.max(columnWidths[i - 1], value.length());
            }
            rows.add(row);
        }

        // Print separator
        printSeparator(columnWidths);

        // Print header
        System.out.print("|");
        for (int i = 1; i <= columnCount; i++) {
            String colName = meta.getColumnLabel(i);
            System.out.print(" " + padRight(colName, columnWidths[i - 1]) + " |");
        }
        System.out.println();

        // Print separator
        printSeparator(columnWidths);

        // Print rows
        for (String[] row : rows) {
            System.out.print("|");
            for (int i = 0; i < columnCount; i++) {
                System.out.print(" " + padRight(row[i], columnWidths[i]) + " |");
            }
            System.out.println();
        }

        // Print separator
        printSeparator(columnWidths);
    }

    private static void printSeparator(int[] widths) {
        System.out.print("+");
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) {
                System.out.print("-");
            }
        System.out.print("+");
    }
    System.out.println();
}

    private static String padRight(String text, int width) {
        return String.format("%-" + width + "s", text);
    } 
}
