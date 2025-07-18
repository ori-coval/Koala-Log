package Ori.Coval.Logging;

import com.acmerobotics.dashboard.FtcDashboard;

import java.util.ArrayList;
import java.util.List;

public class AutoLogManager {
    private static final List<Logged> loggedClasses = new ArrayList<>();

    public static void register(Logged logged){
        loggedClasses.add(logged);
    }

    /** Records values from all registered fields. */
    public static void periodic() {
        for (Logged loggedClass : loggedClasses) {
            loggedClass.toLog();
        }

        ReflectionLogger.update();

        FtcDashboard.getInstance().getTelemetry().update();
    }

    static {
        try {
            Class.forName("Ori.Coval.AutoLog.AutoLogStaticRegistry");
        } catch (ClassNotFoundException e) {
            // no statics registered—ignore
        }
    }
}
