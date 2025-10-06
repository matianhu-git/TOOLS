package testProject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
public class PostgresHelp {

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";  // データベース接続URL
    private static final String USER = "postgres";  // データベースユーザー名
    private static final String PASSWORD = "Postgre";  // データベースパスワード

    // データベース接続を取得するメソッド
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    // Method to execute SQL stored in the Dictionary
    public static void executeSQL(String sqlStatements) throws IOException {
        // SQL script with multiple statements separated by semicolons
        String sqlScript = sqlStatements;

        // Establish the database connection
        try (Connection conn = getConnection()) {
            // Create a Statement object
            Statement stmt = conn.createStatement();

            // Split the script by semicolons to separate each statement
            String[] statements = sqlScript.split(";");

            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty()) {
                    // Execute each SQL statement
                	stmt.addBatch(statement);
//                    stmt.executeUpdate(statement);
//                    System.out.println("Executed: " + statement);
                }
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
       
    }
}
