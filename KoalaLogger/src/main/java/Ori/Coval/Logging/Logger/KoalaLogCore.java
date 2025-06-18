package Ori.Coval.Logging.Logger;

import com.qualcomm.robotcore.hardware.HardwareMap;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Core logger for WPILOG format.
 * Handles registration of entries and logging of data values.
 */
public class KoalaLogCore implements Closeable {

    private static FileOutputStream fos;
    private static final HashMap<String, Integer> recordIDs = new HashMap<>();
    private static int largestId = 0;
    private static long startTime = System.nanoTime() / 1000;

    // --- Setup ---

    /**
     * Set up logging to a file named by the current timestamp.
     */
    public static void setup(HardwareMap hardwareMap) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                .format(new Date());
        setup(hardwareMap, timeStamp + ".wpilog");
    }

    /**
     * Set up logging to a specific file.
     */
    public static void setup(HardwareMap hardwareMap, String filename) {
        LogFileManager.setup(hardwareMap.appContext, filename);
        fos = LogFileManager.getOutputStream();

        startTime = System.nanoTime() / 1000;

        SchemaRegistry.registerPose2dSchema();
    }

    // --- Entry Management ---

    private static int getID(String logName) {
        return recordIDs.computeIfAbsent(logName, key -> ++largestId);
    }

    private static void startEntry(int entryId, String name, String type, String metadata, long ts) throws IOException {
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write(0); // control=Start
        bb.write(Utils.le32(entryId));

        byte[] nameB = name.getBytes(StandardCharsets.UTF_8);
        bb.write(Utils.le32(nameB.length));
        bb.write(nameB);

        byte[] typeB = type.getBytes(StandardCharsets.UTF_8);
        bb.write(Utils.le32(typeB.length));
        bb.write(typeB);

        byte[] metaB = metadata.getBytes(StandardCharsets.UTF_8);
        bb.write(Utils.le32(metaB.length));
        bb.write(metaB);

        writeRecord(0, bb.toByteArray(), ts);
    }

    public static void appendRaw(String name, String type, byte[] payload) throws IOException {
        int id = recordIDs.computeIfAbsent(name, KoalaLogCore::getID);
        startEntry(id, name, type, "", nowMicros());
        writeRecord(id, payload, nowMicros());
    }

    // --- Logging API ---

    /**
     * General-purpose log function used for all value types.
     */
    static <T> T doLog(
            String name,
            T value,
            String wpiType,
            BiConsumer<Integer, T> wpiLogger,
            BiConsumer<String, T> dashboardPoster,
            boolean postToDashboard
    ) {
        boolean isNew = !recordIDs.containsKey(name);
        int id = recordIDs.computeIfAbsent(name, KoalaLogCore::getID);
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

    /**
     * Write a binary payload to the log.
     */
    static void writeRecord(int entryId, byte[] payload, long ts) {
        try {
            fos.write(0x7F);
            fos.write(Utils.le32(entryId));
            fos.write(Utils.le32(payload.length));
            fos.write(Utils.le64(ts));
            fos.write(payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Timekeeping ---

    /**
     * Returns time in microseconds since logger start.
     */
    static long nowMicros() {
        return System.nanoTime() / 1000 - startTime;
    }

    // --- close ---

    @Override
    public void close() throws IOException {
        fos.close();
    }
}
