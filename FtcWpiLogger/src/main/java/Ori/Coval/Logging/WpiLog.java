package Ori.Coval.Logging;

import android.content.Context;
import android.os.Environment;

import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * WpiLog: write WPILOG-format files for Advantage Scope.
 * Supports scalar and array data types.
 */
@SuppressWarnings("unused")
public class WpiLog implements Closeable {
    private static FileOutputStream fos;
    private static final HashMap<String, Integer> recordIDs = new HashMap<>();
    private static int largestId = 0;
    private static long startTime = System.nanoTime() / 1000;

    /**
     * Set up logging to a file named 'robot.wpilog' in SD or internal.
     */
    public static void setup(HardwareMap hardwareMap) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                .format(new Date());
        setup(hardwareMap, timeStamp + ".wpilog");
    }

    /**
     * Set up logging to the given filename, choosing SD if present.
     */
    public static void setup(HardwareMap hardwareMap, String filename) {
        File out = chooseLogFile(hardwareMap.appContext, filename);
        try {
            fos = new FileOutputStream(out);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to open log file: " + out, e);
        }
        startTime = System.nanoTime() / 1000;
        try {
            writeHeader("");
            appendRaw("/.schema/struct:Translation2d", "structschema",
                    "double x;double y".getBytes(StandardCharsets.UTF_8));
            appendRaw("/.schema/struct:Rotation2d", "structschema",
                    "double value".getBytes(StandardCharsets.UTF_8));
            appendRaw("/.schema/struct:Pose2d", "structschema",
                    "Translation2d translation;Rotation2d rotation".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write WPILOG header", e);
        }
    }

    private static File chooseLogFile(Context hwMap, String filename) {
        File[] extDirs = hwMap.getExternalFilesDirs(null);
        File sd = null;
        for (File d : extDirs) {
            if (d != null && Environment.isExternalStorageRemovable(d) && d.exists()) {
                sd = d;
                break;
            }
        }
        File root = (sd != null) ? sd : extDirs[0];
        return new File(root, filename);
    }

    private static void writeHeader(String extra) throws IOException {
        fos.write("WPILOG".getBytes(StandardCharsets.US_ASCII));
        fos.write(le16((short) 0x0100));
        byte[] eb = extra.getBytes(StandardCharsets.UTF_8);
        fos.write(le32(eb.length));
        fos.write(eb);
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }

    public static void appendRaw(String name, String type, byte[] payload) throws IOException {
        int id = recordIDs.computeIfAbsent(name, WpiLog::getID);
        startEntry(id, name, type, "", nowMicros());
        writeRecord(id, payload, nowMicros());
    }

    private static <T> T doLog(
            String name,
            T value,
            String wpiType,
            BiConsumer<Integer, T> wpiLogger,
            BiConsumer<String, T> dashboardPoster,
            boolean postToDashboard
    ) {
        boolean isNew = !recordIDs.containsKey(name);
        int id = recordIDs.computeIfAbsent(name, WpiLog::getID);
        long ts = nowMicros();

        try {
            if (isNew) startEntry(id, name, wpiType, "", ts);
            wpiLogger.accept(id, value);
            if (postToDashboard) dashboardPoster.accept(name, value);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    // Scalars
    public static boolean log(String name, boolean value, boolean post) {
        return doLog(
                name, value, "boolean",
                (id, v) -> logBoolean(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static long log(String name, long value, boolean post) {
        return doLog(
                name, value, "int64",
                (id, v) -> logInt64(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static float log(String name, float value, boolean post) {
        return doLog(
                name, value, "float",
                (id, v) -> logFloat(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static double log(String name, double value, boolean post) {
        return doLog(
                name, value, "double",
                (id, v) -> logDouble(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    public static String log(String name, String value, boolean post) {
        return doLog(
                name, value, "string",
                (id, v) -> logString(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, v),
                post
        );
    }

    // Arrays
    public static boolean[] log(String name, boolean[] value, boolean post) {
        return doLog(
                name, value, "boolean[]",
                (id, v) -> logBooleanArray(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static long[] log(String name, long[] value, boolean post) {
        return doLog(
                name, value, "int64[]",
                (id, v) -> logInt64Array(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static float[] log(String name, float[] value, boolean post) {
        return doLog(
                name, value, "float[]",
                (id, v) -> logFloatArray(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static double[] log(String name, double[] value, boolean post) {
        return doLog(
                name, value, "double[]",
                (id, v) -> logDoubleArray(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    public static String[] log(String name, String[] value, boolean post) {
        return doLog(
                name, value, "string[]",
                (id, v) -> logStringArray(id, v, nowMicros()),
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n, Arrays.toString(v)),
                post
        );
    }

    // Pose2d
    public static void logPose2d(String name, double x, double y, double rotation, boolean post) {
        doLog(
                name,
                new double[]{x, y, rotation},
                "struct:Pose2d",
                (id, v) -> {
                    ByteBuffer buf = ByteBuffer.allocate(3 * Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buf.putDouble(v[0]); buf.putDouble(v[1]); buf.putDouble(v[2]);
                    writeRecord(id, buf.array(), nowMicros());
                },
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n,
                        String.format(Locale.US, "x=%.2f,y=%.2f,θ=%.2f", v[0], v[1], v[2])),
                post
        );
    }

    public static void logTranslation2d(String name, double x, double y, boolean post) {
        doLog(
                name,
                new double[]{x, y},
                "struct:Translation2d",
                (id, v) -> {
                    ByteBuffer buf = ByteBuffer.allocate(2 * Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buf.putDouble(v[0]); buf.putDouble(v[1]);
                    writeRecord(id, buf.array(), nowMicros());
                },
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n,
                        String.format(Locale.US, "x=%.2f,y=%.2f", v[0], v[1])),
                post
        );
    }


    public static void logRotation2d(String name, double rotation, boolean post) {
        doLog(
                name,
                new double[]{rotation},
                "struct:Rotation2d",
                (id, v) -> {
                    ByteBuffer buf = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buf.putDouble(v[0]);
                    writeRecord(id, buf.array(), nowMicros());
                },
                (n, v) -> FtcDashboard.getInstance().getTelemetry().addData(n,
                        String.format(Locale.US, "θ=%.2f", v[0])),
                post
        );
    }



    // ─── Internal scalar/array methods (used by doLog lambdas) ─────────────
    private static void logBoolean(int id, boolean v, long ts){
        writeRecord(id, new byte[]{(byte) (v ? 1 : 0)}, ts);
    }
    private static void logInt64(int id, long v, long ts){
        writeRecord(id, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array(), ts);
    }
    private static void logFloat(int id, float v, long ts){
        writeRecord(id, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v).array(), ts);
    }
    private static void logDouble(int id, double v, long ts){
        writeRecord(id, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(v).array(), ts);
    }
    private static void logString(int id, String s, long ts){
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        writeRecord(id, data, ts);
    }
    private static void logBooleanArray(int id, boolean[] arr, long ts){
        byte[] data = new byte[arr.length]; for (int i=0;i<arr.length;i++) data[i] = (byte) (arr[i] ? 1 : 0);
        writeRecord(id, data, ts);
    }
    private static void logInt64Array(int id, long[] arr, long ts){
        ByteBuffer b = ByteBuffer.allocate(arr.length*8).order(ByteOrder.LITTLE_ENDIAN);
        for(long v:arr) b.putLong(v);
        writeRecord(id, b.array(), ts);
    }
    private static void logFloatArray(int id, float[] arr, long ts){
        ByteBuffer b = ByteBuffer.allocate(arr.length*4).order(ByteOrder.LITTLE_ENDIAN);
        for(float v:arr) b.putFloat(v);
        writeRecord(id, b.array(), ts);
    }
    private static void logDoubleArray(int id, double[] arr, long ts){
        ByteBuffer b = ByteBuffer.allocate(arr.length*8).order(ByteOrder.LITTLE_ENDIAN);
        for(double v:arr) b.putDouble(v);
        writeRecord(id, b.array(), ts);
    }
    private static void logStringArray(int id, String[] arr, long ts){
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        try {
            bb.write(le32(arr.length));
            for(String s:arr) {
                byte[] sb = s.getBytes(StandardCharsets.UTF_8);
                bb.write(le32(sb.length)); bb.write(sb);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        writeRecord(id, bb.toByteArray(), ts);
    }

    // Internal writers
    private static void writeRecord(int entryId, byte[] payload, long ts){
        try {
            fos.write(0x7F);
            fos.write(le32(entryId));
            fos.write(le32(payload.length));
            fos.write(le64(ts));
            fos.write(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void startEntry(int entryId, String name, String type, String metadata, long ts) throws IOException {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write(0); // control=Start
        bb.write(le32(entryId));
        byte[] nameB = name.getBytes(StandardCharsets.UTF_8);
        bb.write(le32(nameB.length)); bb.write(nameB);
        byte[] typeB = type.getBytes(StandardCharsets.UTF_8);
        bb.write(le32(typeB.length)); bb.write(typeB);
        byte[] metaB = metadata.getBytes(StandardCharsets.UTF_8);
        bb.write(le32(metaB.length)); bb.write(metaB);
        writeRecord(0, bb.toByteArray(), ts);
    }
    private static int getID(String logName) {
        if (recordIDs.containsKey(logName)) {
            return recordIDs.get(logName);
        }

        largestId++;
        recordIDs.put(logName, largestId);
        return largestId;
    }

    private static long nowMicros() {
        return System.nanoTime() / 1000 - startTime;
    }

    private static byte[] le16(short v) {
        return new byte[]{(byte) v, (byte) (v >> 8)};
    }

    private static byte[] le32(int v) {
        return new byte[]{
                (byte) v,
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24)
        };
    }

    private static byte[] le64(long v) {
        return new byte[]{
                (byte) v,
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24),
                (byte) (v >> 32),
                (byte) (v >> 40),
                (byte) (v >> 48),
                (byte) (v >> 56)
        };
    }
}
