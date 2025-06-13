package Ori.Coval.Logging;

import com.acmerobotics.dashboard.FtcDashboard;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public class SupplierLog {
    public static BooleanSupplier wrap(String name, BooleanSupplier s, boolean postToFtcDashboard) {
        return () -> {
            boolean v = s.getAsBoolean();
            WpiLog.log(name, v, postToFtcDashboard);
            return v;
        };
    }
    public static IntSupplier wrap(String name, IntSupplier s, boolean postToFtcDashboard) {
        return () -> {
            int v = s.getAsInt();
            WpiLog.log(name, (long)v, postToFtcDashboard);
            return v;
        };
    }
    public static LongSupplier wrap(String name, LongSupplier s, boolean postToFtcDashboard) {
        return () -> {
            long v = s.getAsLong();
            WpiLog.log(name, v, postToFtcDashboard);
            return v;
        };
    }
    public static DoubleSupplier wrap(String name, DoubleSupplier s, boolean postToFtcDashboard) {
        return () -> {
            double v = s.getAsDouble();
            WpiLog.log(name, v, postToFtcDashboard);
            return v;
        };
    }
}
