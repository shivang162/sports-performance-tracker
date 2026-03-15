import com.tracker.service.PerformanceService;

public class TestLogic {

    public static void main(String[] args) {

        PerformanceService ps = new PerformanceService();

        double score = ps.calculateScore(90,50,75);

        System.out.println("Performance Score: " + score);
    }
}
