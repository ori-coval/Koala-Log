package Ori.Coval.Logging.Logger;

public class Utils {
    static byte[] le16(short v) {
        return new byte[]{(byte) v, (byte) (v >> 8)};
    }

    static byte[] le32(int v) {
        return new byte[]{
                (byte) v,
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24)
        };
    }

    static byte[] le64(long v) {
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
