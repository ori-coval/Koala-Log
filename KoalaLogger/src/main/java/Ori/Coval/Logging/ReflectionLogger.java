package Ori.Coval.Logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import Ori.Coval.Logging.Logger.KoalaLog;

/**
 * TelemetryManager: automatically logs all fields, zero-arg methods, and suppliers
 * on registered classes via static register/log calls.
 */
public class ReflectionLogger {
    private static class Entry {
        final Object target;
        final boolean post;
        Entry(Object target, boolean post) {
            this.target = target;
            this.post = post;
        }
    }

    private static final List<Entry> entries = new ArrayList<>();

    /**
     * Register an object whose fields and no-arg methods will be logged.
     * Defaults to posting to FTC Dashboard.
     */
    public static void register(Object obj) {
        register(obj, true);
    }

    /**
     * Register an object whose fields and no-arg methods will be logged.
     * @param postToDashboard whether to post each value to FTC Dashboard
     */
    public static void register(Object obj, boolean postToDashboard) {
        entries.add(new Entry(obj, postToDashboard));
    }

    /**
     * Trigger logging of all registered entries.
     */
    public static void update() {
        for (Entry e : entries) {
            try {
                logFields(e);
                logMethods(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void logFields(Entry e) throws IllegalAccessException {
        Object target = e.target;
        Class<?> cls = target.getClass();
        if (cls.isAnnotationPresent(DoNotLog.class)) return;
        String prefix = cls.getSimpleName() + ".";
        for (Field f : cls.getDeclaredFields()) {
            if (f.isAnnotationPresent(DoNotLog.class)) continue;
            f.setAccessible(true);
            String name = prefix + f.getName();
            logValue(name, f.get(target), f.getType(), e.post);
        }
    }

    private static void logMethods(Entry e) throws Exception {
        Object target = e.target;
        Class<?> cls = target.getClass();
        String prefix = cls.getSimpleName() + ".";
        for (Method m : cls.getDeclaredMethods()) {
            if (m.isAnnotationPresent(DoNotLog.class)) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() == void.class) continue;
            m.setAccessible(true);
            Object value = m.invoke(target);
            String name = prefix + m.getName() + "()";
            logValue(name, value, m.getReturnType(), e.post);
        }
    }

    private static void logValue(String name, Object value, Class<?> type, boolean post) {
        if (value == null) {
            KoalaLog.log(name, "", post);
            return;
        }
        // Scalars
        if (type == boolean.class || type == Boolean.class) {
            KoalaLog.log(name, (boolean) value, post);
        } else if (type == byte.class) {
            KoalaLog.log(name, (long) (byte) value, post);
        } else if (type == char.class) {
            KoalaLog.log(name, (long) (char) value, post);
        } else if (type == short.class) {
            KoalaLog.log(name, (long) (short) value, post);
        } else if (type == int.class || type == Integer.class) {
            KoalaLog.log(name, (long) (int) value, post);
        } else if (type == long.class || type == Long.class) {
            KoalaLog.log(name, (long) value, post);
        } else if (type == float.class || type == Float.class) {
            KoalaLog.log(name, (float) value, post);
        } else if (type == double.class || type == Double.class) {
            KoalaLog.log(name, (double) value, post);
        } else if (type == String.class) {
            KoalaLog.log(name, value.toString(), post);
        }
        else if (type.isArray()) {
            Class<?> ct = type.getComponentType();
            // Boxed arrays
            if (ct == Boolean.class) {
                KoalaLog.log(name, (Boolean[]) value, post);
            } else if (ct == Integer.class) {
                KoalaLog.log(name, (Integer[]) value, post);
            } else if (ct == Long.class) {
                KoalaLog.log(name, (Long[]) value, post);
            } else if (ct == Float.class) {
                KoalaLog.log(name, (Float[]) value, post);
            } else if (ct == Double.class) {
                KoalaLog.log(name, (Double[]) value, post);
            }
            // Primitive arrays
            else if (ct == boolean.class) {
                KoalaLog.log(name, (boolean[]) value, post);
            } else if (ct == byte.class) {
                byte[] arr = (byte[]) value;
                long[] la = new long[arr.length]; for (int i = 0; i < arr.length; i++) la[i] = arr[i];
                KoalaLog.log(name, la, post);
            } else if (ct == char.class) {
                char[] arr = (char[]) value;
                long[] la = new long[arr.length]; for (int i = 0; i < arr.length; i++) la[i] = arr[i];
                KoalaLog.log(name, la, post);
            } else if (ct == short.class) {
                short[] arr = (short[]) value;
                long[] la = new long[arr.length]; for (int i = 0; i < arr.length; i++) la[i] = arr[i];
                KoalaLog.log(name, la, post);
            } else if (ct == int.class) {
                int[] arr = (int[]) value;
                long[] la = new long[arr.length]; for (int i = 0; i < arr.length; i++) la[i] = arr[i];
                KoalaLog.log(name, la, post);
            } else if (ct == long.class) {
                KoalaLog.log(name, (long[]) value, post);
            } else if (ct == float.class) {
                KoalaLog.log(name, (float[]) value, post);
            } else if (ct == double.class) {
                KoalaLog.log(name, (double[]) value, post);
            } else if (ct == String.class) {
                KoalaLog.log(name, (String[]) value, post);
            }
        }
    }
}
