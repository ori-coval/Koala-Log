package Ori.Coval.Logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Utility for packing primitive and array values into little-endian byte arrays.
 */
public class BytePacker {

    /**
     * Packs doubles in little-endian into a byte array.
     * @param values the double values to pack
     * @return a byte array of length values.length * Double.BYTES
     */
    public static byte[] packDoubles(double... values) {
        ByteBuffer buf = ByteBuffer
                .allocate(values.length * Double.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (double v : values) {
            buf.putDouble(v);
        }
        return buf.array();
    }

    /**
     * Packs floats in little-endian into a byte array.
     * @param values the float values to pack
     * @return a byte array of length values.length * Float.BYTES
     */
    public static byte[] packFloats(float... values) {
        ByteBuffer buf = ByteBuffer
                .allocate(values.length * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float v : values) {
            buf.putFloat(v);
        }
        return buf.array();
    }

    /**
     * Packs longs in little-endian into a byte array.
     * @param values the long values to pack
     * @return a byte array of length values.length * Long.BYTES
     */
    public static byte[] packLongs(long... values) {
        ByteBuffer buf = ByteBuffer
                .allocate(values.length * Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (long v : values) {
            buf.putLong(v);
        }
        return buf.array();
    }

    /**
     * Packs ints in little-endian into a byte array.
     * @param values the int values to pack
     * @return a byte array of length values.length * Integer.BYTES
     */
    public static byte[] packInts(int... values) {
        ByteBuffer buf = ByteBuffer
                .allocate(values.length * Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int v : values) {
            buf.putInt(v);
        }
        return buf.array();
    }

    /**
     * Packs booleans into a byte array, using 1 byte per boolean (1 for true, 0 for false).
     * @param values the boolean values to pack
     * @return a byte array of length values.length
     */
    public static byte[] packBooleans(boolean... values) {
        byte[] arr = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            arr[i] = (byte) (values[i] ? 1 : 0);
        }
        return arr;
    }

    /**
     * Packs an array of UTF‑8 strings into the WPILOG array‑of‑strings format:
     *  4‑byte LE count N,
     *  then for each string: 4‑byte LE length, followed by UTF‑8 bytes.
     */
    public static byte[] packStrings(String... values) throws IOException{
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        // number of strings
        bb.write(Utils.le32(values.length));
        for (String s : values) {
            byte[] sb = s.getBytes(StandardCharsets.UTF_8);
            // length of this string
            bb.write(Utils.le32(sb.length));
            // string bytes
            bb.write(sb);
        }
        return bb.toByteArray();
    }

}