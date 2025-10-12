package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtils {

    private static final String DB_NAME = "CarDealerDBI";
    private static final String USER_NAME = "sa";
    private static final String PASSWORD = "12345";
    private static final String HOST = "localhost";
    private static final String PORT = "1433";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String url = "jdbc:sqlserver://" + HOST + ":" + PORT
                    + ";databaseName=" + DB_NAME
                    + ";encrypt=false"; // tránh lỗi SSL
            conn = DriverManager.getConnection(url, USER_NAME, PASSWORD);
            System.out.println("Successfully connected to database");
        } catch (ClassNotFoundException e) {
            System.err.println(" Can't find the database" + e.getMessage());
        } catch (SQLException e) {
            System.err.println("fail to connecting to database " + e.getMessage());
        }
        return conn;
    }

}
