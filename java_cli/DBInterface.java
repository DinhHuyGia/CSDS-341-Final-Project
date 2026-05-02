package java_cli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Scanner;

/**
 * Terminal JDBC client for the course schema (MySQL/MariaDB).
 *
 * <p>Compile / run:
 * <pre>
 * javac -cp ".;lib/mysql-connector-j-9.6.0.jar" java_cli/DBInterface.java
 * java -cp ".;lib/mysql-connector-j-9.6.0.jar;java_cli" DBInterface # interactive menu
 * </pre>
 */
class DBInterface {

    private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/company_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "your_mysql_password";

    private static final String[] BROWSABLE_TABLES = {
            "Department", "Center", "Employee", "Supervisor", "Project", "Task", "AssignedTo"
    };

    /**
     * Premade queries: index 0 maps to id 1 in the menu. Edit labels and SQL as needed.
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

            {"All supervisors in Human Resources",
                    "SELECT e.* " +
                            "FROM Employee e, Department d, Supervisor s " +
                            "WHERE s.supervisorID = e.employeeID " +
                            "AND e.departmentID = d.departmentID " +
                            "AND d.departmentName = 'Human Resources'"},

            {"Number of employees in Germany",
                    "SELECT COUNT(*) AS employeeCount " +
                            "FROM Employee e, Center c " +
                            "WHERE e.centerID = c.centerID " +
                            "AND c.country = 'Germany' "},

            {"Information of employees with at least 20 years of experience",
                    "SELECT e.firstName, e.lastName, e.annualSalary, d.departmentName " +
                            "FROM Employee e " +
                            "JOIN Department d ON e.departmentID = d.departmentID " +
                            "WHERE TIMESTAMPDIFF(YEAR, e.startDate, CURDATE()) >= 20 "},

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
                int choice = readInt(scanner, "Enter option: ", 0, QUERY_CATALOG.length + 3);
                if (choice == 0) {
                    System.out.println("Bye.");
                    return;
                }
                if (choice >= 1 && choice <= QUERY_CATALOG.length) {
                    runCatalogQuery(choice);
                } else if (choice == QUERY_CATALOG.length + 1) {
                    runParameterizedQueryMenu(scanner);
                } else if (choice == QUERY_CATALOG.length + 2) {
                    browseTable(scanner);
                } else if (choice == QUERY_CATALOG.length + 3) {
                    runCustomSelect(scanner);
                }
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
        System.out.println((QUERY_CATALOG.length + 1) + ". Search with your own values");
        System.out.println((QUERY_CATALOG.length + 2) + ". Browse a table");
        System.out.println((QUERY_CATALOG.length + 3) + ". Run a custom SELECT query");
        System.out.println("0. Quit");
    }

    /** Returns SQL for 1-based id, or null if out of range. */
    private static String sqlForId(int id) {
        if (id < 1 || id > QUERY_CATALOG.length) {
            return null;
        }
        return QUERY_CATALOG[id - 1][1];
    }

    private static void runParameterizedQueryMenu(Scanner scanner) {
        while (true) {
            System.out.println("--- Search With Your Own Values ---");
            System.out.println("1. Employees by department");
            System.out.println("2. Employee count by country");
            System.out.println("3. Supervisors by department");
            System.out.println("4. Employees with at least N years of experience");
            System.out.println("5. Employees by country, center, department, and minimum salary");
            System.out.println("6. Project count for an employee");
            System.out.println("7. Projects overseen by a supervisor");
            System.out.println("8. Projects under supervisors from a department");
            System.out.println("9. Average salary by department");
            System.out.println("0. Back to main menu");

            int choice = readInt(scanner, "Enter option: ", 0, 9);
            if (choice == 0) {
                return;
            }

            switch (choice) {
                case 1:
                    employeesByDepartment(scanner);
                    break;
                case 2:
                    employeeCountByCountry(scanner);
                    break;
                case 3:
                    supervisorsByDepartment(scanner);
                    break;
                case 4:
                    employeesByYearsOfExperience(scanner);
                    break;
                case 5:
                    qualifiedEmployees(scanner);
                    break;
                case 6:
                    projectCountForEmployee(scanner);
                    break;
                case 7:
                    projectsOverseenBySupervisor(scanner);
                    break;
                case 8:
                    projectsBySupervisorDepartment(scanner);
                    break;
                case 9:
                    averageSalaryByDepartment(scanner);
                    break;
                default:
                    System.err.println("Invalid option.");
            }
            System.out.println();
        }
    }

    private static void employeesByDepartment(Scanner scanner) {
        String department = readRequiredLine(scanner, "Department name: ");
        String sql = "SELECT e.* " +
                "FROM Employee e JOIN Department d ON e.departmentID = d.departmentID " +
                "WHERE d.departmentName = ?";
        runPreparedQuery(sql, stmt -> stmt.setString(1, department));
    }

    private static void employeeCountByCountry(Scanner scanner) {
        String country = readRequiredLine(scanner, "Country: ");
        String sql = "SELECT c.country, COUNT(*) AS employeeCount " +
                "FROM Employee e JOIN Center c ON e.centerID = c.centerID " +
                "WHERE c.country = ? " +
                "GROUP BY c.country";
        runPreparedQuery(sql, stmt -> stmt.setString(1, country));
    }

    private static void supervisorsByDepartment(Scanner scanner) {
        String department = readRequiredLine(scanner, "Department name: ");
        String sql = "SELECT e.* " +
                "FROM Employee e " +
                "JOIN Department d ON e.departmentID = d.departmentID " +
                "JOIN Supervisor s ON s.supervisorID = e.employeeID " +
                "WHERE d.departmentName = ?";
        runPreparedQuery(sql, stmt -> stmt.setString(1, department));
    }

    private static void employeesByYearsOfExperience(Scanner scanner) {
        int years = readInt(scanner, "Minimum years of experience: ", 0, 100);
        String sql = "SELECT e.firstName, e.lastName, e.annualSalary, d.departmentName " +
                "FROM Employee e " +
                "JOIN Department d ON e.departmentID = d.departmentID " +
                "WHERE TIMESTAMPDIFF(YEAR, e.startDate, CURDATE()) >= ?";
        runPreparedQuery(sql, stmt -> stmt.setInt(1, years));
    }

    private static void qualifiedEmployees(Scanner scanner) {
        String country = readRequiredLine(scanner, "Country: ");
        String center = readRequiredLine(scanner, "Center name: ");
        String department = readRequiredLine(scanner, "Department name: ");
        int minSalary = readInt(scanner, "Minimum annual salary: ", 0, Integer.MAX_VALUE);
        String sql = "SELECT COUNT(*) AS qualifiedEmployees " +
                "FROM Employee e " +
                "JOIN Center c ON e.centerID = c.centerID " +
                "JOIN Department d ON e.departmentID = d.departmentID " +
                "WHERE c.country = ? " +
                "AND c.centerName = ? " +
                "AND d.departmentName = ? " +
                "AND e.annualSalary > ?";
        runPreparedQuery(sql, stmt -> {
            stmt.setString(1, country);
            stmt.setString(2, center);
            stmt.setString(3, department);
            stmt.setInt(4, minSalary);
        });
    }

    private static void projectCountForEmployee(Scanner scanner) {
        String firstName = readRequiredLine(scanner, "Employee first name: ");
        String lastName = readRequiredLine(scanner, "Employee last name: ");
        String sql = "SELECT e.firstName, e.lastName, COUNT(a.projectID) AS projectCount " +
                "FROM Employee e JOIN AssignedTo a ON e.employeeID = a.employeeID " +
                "WHERE e.firstName = ? " +
                "AND e.lastName = ? " +
                "GROUP BY e.firstName, e.lastName";
        runPreparedQuery(sql, stmt -> {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
        });
    }

    private static void projectsOverseenBySupervisor(Scanner scanner) {
        String firstName = readRequiredLine(scanner, "Supervisor first name: ");
        String lastName = readRequiredLine(scanner, "Supervisor last name: ");
        String sql = "SELECT p.projectName, p.deadline " +
                "FROM Project p JOIN Employee e ON p.supervisorID = e.employeeID " +
                "WHERE e.firstName = ? " +
                "AND e.lastName = ?";
        runPreparedQuery(sql, stmt -> {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
        });
    }

    private static void projectsBySupervisorDepartment(Scanner scanner) {
        String department = readRequiredLine(scanner, "Department name: ");
        String sql = "SELECT DISTINCT p.projectID, p.projectName " +
                "FROM Project p " +
                "JOIN Employee e ON p.supervisorID = e.employeeID " +
                "JOIN Department d ON e.departmentID = d.departmentID " +
                "WHERE d.departmentName = ?";
        runPreparedQuery(sql, stmt -> stmt.setString(1, department));
    }

    private static void averageSalaryByDepartment(Scanner scanner) {
        String department = readRequiredLine(scanner, "Department name: ");
        String sql = "SELECT d.departmentName, AVG(e.annualSalary) AS avgSalary " +
                "FROM Employee e JOIN Department d ON e.departmentID = d.departmentID " +
                "WHERE d.departmentName = ? " +
                "GROUP BY d.departmentName";
        runPreparedQuery(sql, stmt -> stmt.setString(1, department));
    }

    private static void browseTable(Scanner scanner) {
        System.out.println("--- Browse A Table ---");
        for (int i = 0; i < BROWSABLE_TABLES.length; i++) {
            System.out.println((i + 1) + ". " + BROWSABLE_TABLES[i]);
        }
        System.out.println("0. Back to main menu");

        int choice = readInt(scanner, "Choose table: ", 0, BROWSABLE_TABLES.length);
        if (choice == 0) {
            return;
        }

        int limit = readInt(scanner, "How many rows? ", 1, 500);
        String tableName = BROWSABLE_TABLES[choice - 1];
        String sql = "SELECT * FROM " + tableName + " LIMIT ?";
        runPreparedQuery(sql, stmt -> stmt.setInt(1, limit));
    }

    private static void runCustomSelect(Scanner scanner) {
        System.out.println("--- Custom SELECT Query ---");
        System.out.println("Only SELECT statements are allowed here.");
        String sql = readRequiredLine(scanner, "SQL> ");
        sql = cleanCustomSelect(sql);
        if (sql == null) {
            System.err.println("Rejected. Enter one SELECT statement with no extra semicolons.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            printResultSet(rs);
        } catch (SQLException e) {
            System.err.println("Failed to run custom query: " + e.getMessage());
        }
    }

    private static String cleanCustomSelect(String sql) {
        String cleaned = sql.trim();
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        if (cleaned.contains(";")) {
            return null;
        }
        if (!cleaned.toLowerCase(Locale.ROOT).startsWith("select ")) {
            return null;
        }
        return cleaned;
    }

    private static void runPreparedQuery(String sql, StatementBinder binder) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            binder.bind(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                printResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Failed to run query: " + e.getMessage());
        }
    }

    private static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return min;
            }
            String raw = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Fall through to the message below.
            }
            System.err.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    private static String readRequiredLine(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return "";
            }
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.err.println("Please enter a value.");
        }
    }

    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        int[] columnWidths = new int[columnCount];

        for (int i = 1; i <= columnCount; i++) {
            columnWidths[i - 1] = meta.getColumnLabel(i).length();
        }

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

        printSeparator(columnWidths);

        System.out.print("|");
        for (int i = 1; i <= columnCount; i++) {
            String colName = meta.getColumnLabel(i);
            System.out.print(" " + padRight(colName, columnWidths[i - 1]) + " |");
        }
        System.out.println();

        printSeparator(columnWidths);

        for (String[] row : rows) {
            System.out.print("|");
            for (int i = 0; i < columnCount; i++) {
                System.out.print(" " + padRight(row[i], columnWidths[i]) + " |");
            }
            System.out.println();
        }

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
