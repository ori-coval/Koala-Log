package Ori.Coval.Logging;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import Ori.Coval.Logging.Logger.KoalaLog;

public class SupplierLog {
    public static BooleanSupplier wrap(String name, BooleanSupplier s, boolean postToFtcDashboard) {
        return () -> {
            boolean v = s.getAsBoolean();
            KoalaLog.log(name, v, postToFtcDashboard);
            return v;
        };
    }
    public static IntSupplier wrap(String name, IntSupplier s, boolean postToFtcDashboard) {
        return () -> {
            int v = s.getAsInt();
            KoalaLog.log(name, (long)v, postToFtcDashboard);
            return v;
        };
    }
    public static LongSupplier wrap(String name, LongSupplier s, boolean postToFtcDashboard) {
        return () -> {
            long v = s.getAsLong();
            KoalaLog.log(name, v, postToFtcDashboard);
            return v;
        };
    }
    public static DoubleSupplier wrap(String name, DoubleSupplier s, boolean postToFtcDashboard) {
        return () -> {
            double v = s.getAsDouble();
            KoalaLog.log(name, v, postToFtcDashboard);
            return v;
        };
    }
}
