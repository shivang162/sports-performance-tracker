import com.tracker.service.PerformanceService;
import com.tracker.service.PerformanceAnalyzer;
import com.tracker.service.RecordFormatter;
import java.util.Arrays;
import java.util.List;

public class TestLogic {

    public static void main(String[] args) {

        PerformanceService  ps       = new PerformanceService();
        PerformanceAnalyzer analyzer = new PerformanceAnalyzer();
        RecordFormatter     fmt      = new RecordFormatter();

        List<Double> scores = Arrays.asList(60.0, 70.0, 80.0, 90.0);

        System.out.println("1. calculateScore(90,50,75) = " + ps.calculateScore(90,50,75));
        // Expected: 73.5

        System.out.println("2. calculateAverage        = " + ps.calculateAverage(scores));
        // Expected: 75.0

        System.out.println("3. detectTrend (improving)  = " + ps.detectTrend(scores));
        // Expected: Improving

        System.out.println("4. detectTrend (declining)  = " +
            ps.detectTrend(Arrays.asList(90.0, 80.0, 70.0, 60.0)));
        // Expected: Declining

        System.out.println("5. periodImprovement        = " +
            String.format("%.1f%%", ps.calculatePeriodImprovement(scores)));
        // Expected: ~30.8%

        System.out.println("6. analyzeLevel             = " + analyzer.analyzeLevel(scores));
        // Expected: Good

        System.out.println("7. gapToNextLevel           = " + analyzer.gapToNextLevel(scores));
        // Expected: You need 10.0 more points to reach Excellent.

        System.out.println("8. formatSaveResponse       = " +
            fmt.formatSaveResponse("Rahul", 8.5, 80, 75, 73.5, "Good"));

        System.out.println("\nAll tests done.");
    }
}
