import java.sql.Connection;
import java.sql.DriverManager;

public class TestDB {

    public static void main(String[] args) {

        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/sports_tracker",
                "root",
                "SHIVANG123@k"
            );

            System.out.println("Connected to Database ✅");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
