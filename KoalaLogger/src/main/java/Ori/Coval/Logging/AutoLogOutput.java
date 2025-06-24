package Ori.Coval.Logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for which an AutoLogged subclass will be generated.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface AutoLogOutput {
    String name() default "";
    /**
     * Post to the FTC Dashboard?
     */
    boolean postToFtcDashboard() default true;
}

