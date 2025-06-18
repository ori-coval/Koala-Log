package Ori.Coval.Logging.Logger;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LogFileManager {
    private static FileOutputStream fos;

    static FileOutputStream getOutputStream() {
        return fos;
    }

    static void setup(Context context, String filename) {
        try {
            File file = chooseLogFile(context, filename);
            fos = new FileOutputStream(file);
            writeHeader("");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log file", e);
        }
    }
    private static void writeHeader(String extra) throws IOException {
        fos.write("WPILOG".getBytes(StandardCharsets.US_ASCII));
        fos.write(Utils.le16((short) 0x0100));
        byte[] eb = extra.getBytes(StandardCharsets.UTF_8);
        fos.write(Utils.le32(eb.length));
        fos.write(eb);
    }

    private static File chooseLogFile(Context context, String filename) {
        File[] extDirs = context.getExternalFilesDirs(null);
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
}

