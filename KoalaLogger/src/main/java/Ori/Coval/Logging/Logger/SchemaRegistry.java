package Ori.Coval.Logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class SchemaRegistry {
    private static final HashMap<String, String> structSchemas = new HashMap<>();

    /**
     * register a struct schema to the log
     *
     * <p>example of adding a Translation2d struct schema
     *
     *  <pre> {@code registerStructSchemas("struct:Translation2d", "double x;double y");}</pre>
     *
     *
     * @param name the name of the schema
     * @param schema the schema definition
     */
    static void registerStructSchemas(String name, String schema) {
        //add "struct:" prefix if not already present
        String logLocation = !name.startsWith("struct:") ? "/.schema/struct:" : "/.schema/";


        if(!structSchemas.containsKey(name)) {
            structSchemas.put(name, schema);
            try {
                KoalaLogCore.appendRaw(logLocation + name, "structschema", schema.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Failed to register schemas", e);
            }
        }

    }

    static void registerPose2dSchema() {
        registerStructSchemas("struct:Translation2d", "double x;double y");
        registerStructSchemas("struct:Rotation2d", "double value");
        registerStructSchemas("struct:Pose2d", "Translation2d translation;Rotation2d rotation");
    }
}

