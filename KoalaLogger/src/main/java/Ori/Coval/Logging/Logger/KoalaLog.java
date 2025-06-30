package Ori.Coval.Logging.Logger;

import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * WpiLog: write WPILOG-format files for Advantage Scope.
 * Supports scalar and array data types.
 */
@SuppressWarnings("unused")
public class KoalaLog {
    /**
     * Set up logging to a file named 'robot.wpilog' in SD or internal.
     */
    public static void setup(HardwareMap hardwareMap) {
        KoalaLogCore.setup(hardwareMap);
    }

    /**
     * Set up logging to the given filename, choosing SD if present.
     */
    public static void setup(HardwareMap hardwareMap, String filename) {
        KoalaLogCore.setup(hardwareMap, filename);
    }

    // Scalars
    public static boolean log(String name, boolean value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "boolean",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packBooleans(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static long log(String name, long value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int64",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packLongs(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static int log(String name, int value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int32",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packInts(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }
    public static Integer log(String name, Integer value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int32",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packInts(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static float log(String name, float value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "float",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packFloats(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static double log(String name, double value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "double",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packDoubles(new double[]{v}), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static String log(String name, String value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "string",
                (id, v) -> {
                    try {
                        KoalaLogCore.writeRecord(id, BytePacker.packStrings(v), KoalaLogCore.nowMicros());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    // Arrays
    public static boolean[] log(String name, boolean[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "boolean[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packBooleans(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }
    public static Boolean[] log(String name, Boolean[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "boolean[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packBooleans(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static long[] log(String name, long[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int64[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packLongs(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }
    public static Long[] log(String name, Long[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int64[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packLongs(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static int[] log(String name, int[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int64[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packInts(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }
    public static Integer[] log(String name, Integer[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "int64[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packInts(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static float[] log(String name, float[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "float[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packFloats(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }
    public static Float[] log(String name, Float[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "float[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packFloats(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static double[] log(String name, double[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "double[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packDoubles(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }
    public static Double[] log(String name, Double[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "double[]",
                (id, v) -> KoalaLogCore.writeRecord(id, BytePacker.packDoubles(v), KoalaLogCore.nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static String[] log(String name, String[] value, boolean post) {
        return KoalaLogCore.doLog(
                name, value, "string[]",
                (id, v) -> {
                    try {
                        KoalaLogCore.writeRecord(id, BytePacker.packStrings(v), KoalaLogCore.nowMicros());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    /**
     * Logs a 2D translation (two doubles) as struct:Translation2d
     */
    public static void logTranslation2d(String name, double x, double y, boolean post) {
        KoalaLogCore.doLog(
                name,
                new double[]{x, y},
                "struct:Translation2d",
                (id, v) -> {
                    // pack two doubles in little‐endian
                    byte[] payload = BytePacker.packDoubles(v);
                    KoalaLogCore.writeRecord(id, payload, KoalaLogCore.nowMicros());
                },
                (n, v) -> FtcDashboard
                        .getInstance()
                        .getTelemetry()
                        .addData(n, String.format(Locale.US, "x=%.2f,y=%.2f", v[0], v[1])),
                post
        );
    }

    /**
     * Logs a 2D rotation (one double) as struct:Rotation2d
     */
    public static void logRotation2d(String name, double rotation, boolean post) {
        KoalaLogCore.doLog(
                name,
                new double[]{rotation},
                "struct:Rotation2d",
                (id, v) -> {
                    // pack one double in little‐endian
                    byte[] payload = BytePacker.packDoubles(v);
                    KoalaLogCore.writeRecord(id, payload, KoalaLogCore.nowMicros());
                },
                (n, v) -> FtcDashboard
                        .getInstance()
                        .getTelemetry()
                        .addData(n, String.format(Locale.US, "θ=%.2f", v[0])),
                post
        );
    }

    /**
     * Logs a full Pose2d (three doubles) as struct:Pose2d
     */
    public static void logPose2d(String name, double x, double y, double rot, boolean post) {
        KoalaLogCore.doLog(
                name,
                new double[]{x, y, rot},
                "struct:Pose2d",
                (id, v) -> {
                    // pack three doubles in little‐endian
                    byte[] payload = BytePacker.packDoubles(v);
                    KoalaLogCore.writeRecord(id, payload, KoalaLogCore.nowMicros());
                },
                (n, v) -> FtcDashboard
                        .getInstance()
                        .getTelemetry()
                        .addData(n, String.format(Locale.US, "x=%.2f,y=%.2f,θ=%.2f", v[0], v[1], v[2])),
                post
        );
    }
    
}
