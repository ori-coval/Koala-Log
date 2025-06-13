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

/**
 * WpiLog: write WPILOG-format files for Advantage Scope.
 * Supports scalar and array data types.
 */
//TODO: change all the e.printStackTrace() for a better way of logging
@SuppressWarnings("unused")
public class WpiLog implements Closeable {
    private static FileOutputStream fos;
    private static final HashMap<String, Integer> recordIDs = new HashMap<>();
    private static int largestId = 0;
    private static long startTime = System.nanoTime() / 1000;

    public static void register(Logged loggedClass) {
    }

    /**
     * Set up logging to a file named 'robot.wpilog' in SD or internal.
     */
    public static void setup(HardwareMap hardwareMap) {
        // Format: yyyy-MM-dd_HH-mm-ss (example: 2025-05-22_15-42-10)
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                .format(new Date());

        String fileName = timeStamp + ".wpilog";

        setup(hardwareMap, fileName);
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to write WPILOG header", e);
        }
    }

    /**
     * Picks removable SD card if mounted, otherwise primary external-files dir.
     */
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
        fos.write(le16((short) 0x0100));               // version 1.0
        byte[] eb = extra.getBytes(StandardCharsets.UTF_8);
        fos.write(le32(eb.length));                    // extra-header length
        fos.write(eb);                                  // extra-header data
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }

    // ─── Control records ─────────────────────────────────────────────────────
    private static void startEntry(int entryId, String name, String type, long ts) throws IOException {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write(0);
        bb.write(le32(entryId));
        bb.write(le32(name.length()));
        bb.write(name.getBytes(StandardCharsets.UTF_8));
        bb.write(le32(type.length()));
        bb.write(type.getBytes(StandardCharsets.UTF_8));
        bb.write(le32("".length()));
        bb.write("".getBytes(StandardCharsets.UTF_8));
        writeRecord(0, bb.toByteArray(), ts);
    }

    private static void finishEntry(int entryId, long ts) throws IOException {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write(1);
        bb.write(le32(entryId));
        writeRecord(0, bb.toByteArray(), ts);
    }

    private static void setMetadata(int entryId, String metadata, long ts) throws IOException {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write(2);
        bb.write(le32(entryId));
        bb.write(le32(metadata.length()));
        bb.write(metadata.getBytes(StandardCharsets.UTF_8));
        writeRecord(0, bb.toByteArray(), ts);
    }

    // ─── Low-level record writer ─────────────────────────────────────────────
    private static void writeRecord(int entryId, byte[] payload, long ts) throws IOException {
        fos.write(0x7F);               // header: 4B id,4B size,8B timestamp
        fos.write(le32(entryId));
        fos.write(le32(payload.length));
        fos.write(le64(ts));
        fos.write(payload);
    }

    // ─── public static logging ──────────────────────────────────────────────────────
// ─── public static Logging API ──────────────────────────────────────────────────

    public static boolean log(String name, boolean value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "boolean", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logBoolean(getID(name), value, nowMicros());
            if(PostToFTCDashboard){
                FtcDashboard.getInstance().getTelemetry().addData(name, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static Object log(String name, Object obj, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "object", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logString(getID(name), obj == null ? "null" : obj.toString(), nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, obj);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return obj;
    }

    public static long log(String name, long value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "int64", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logInt64(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static float log(String name, float value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "float", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logFloat(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static double log(String name, double value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "double", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logDouble(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static String log(String name, String value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "string", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logString(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static boolean[] log(String name, boolean[] value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "boolean[]", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logBooleanArray(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                // Telemetry will call toString() on the array; you could also format it yourself:
                FtcDashboard.getInstance().getTelemetry().addData(name, Arrays.toString(value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static long[] log(String name, long[] value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "int64[]", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logInt64Array(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, Arrays.toString(value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static float[] log(String name, float[] value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "float[]", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logFloatArray(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, Arrays.toString(value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static double[] log(String name, double[] value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "double[]", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logDoubleArray(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, Arrays.toString(value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }

    public static String[] log(String name, String[] value, boolean PostToFTCDashboard) {
        if (!recordIDs.containsKey(name)) {
            try {
                startEntry(getID(name), name, "String[]", nowMicros());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            logStringArray(getID(name), value, nowMicros());
            if (PostToFTCDashboard) {
                FtcDashboard.getInstance().getTelemetry().addData(name, Arrays.toString(value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return value;
    }


    // ─── INTERNAL LOGGING ────────────────────────────────────────────────────
    // ─── Scalar logging ──────────────────────────────────────────────────────
    private static void logBoolean(int id, boolean v, long ts) throws IOException {
        writeRecord(id, new byte[]{(byte) (v ? 1 : 0)}, ts);
    }

    private static void logInt64(int id, long v, long ts) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v);
        writeRecord(id, b.array(), ts);
    }

    private static void logFloat(int id, float v, long ts) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(v);
        writeRecord(id, b.array(), ts);
    }

    private static void logDouble(int id, double v, long ts) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(v);
        writeRecord(id, b.array(), ts);
    }

    private static void logString(int id, String s, long ts) throws IOException {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        writeRecord(id, data, ts);
    }

    // ─── Array logging ───────────────────────────────────────────────────────
    private static void logBooleanArray(int id, boolean[] arr, long ts) throws IOException {
        byte[] data = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) data[i] = (byte) (arr[i] ? 1 : 0);
        writeRecord(id, data, ts);
    }

    private static void logInt64Array(int id, long[] arr, long ts) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(arr.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (long v : arr) b.putLong(v);
        writeRecord(id, b.array(), ts);
    }

    private static void logFloatArray(int id, float[] arr, long ts) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(arr.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : arr) b.putFloat(v);
        writeRecord(id, b.array(), ts);
    }

    private static void logDoubleArray(int id, double[] arr, long ts) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(arr.length * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double v : arr) b.putDouble(v);
        writeRecord(id, b.array(), ts);
    }

    private static void logStringArray(int id, String[] arr, long ts) throws IOException {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write(le32(arr.length));
        for (String s : arr) {
            byte[] sb = s.getBytes(StandardCharsets.UTF_8);
            bb.write(le32(sb.length));
            bb.write(sb);
        }
        writeRecord(id, bb.toByteArray(), ts);
    }

    // ─── Utils ───────────────────────────────────────────────────────────────
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