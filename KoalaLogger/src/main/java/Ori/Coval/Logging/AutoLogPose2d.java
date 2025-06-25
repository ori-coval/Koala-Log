package Ori.Coval.Logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * marks a field or method to be logged as a pose2d
 * <p>
 * will only log if the field/method is of type Pose2d
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface AutoLogPose2d {
    String name() default "";

    /**
     * Post to the FTC Dashboard?
     */
    boolean postToFtcDashboard() default true;
}