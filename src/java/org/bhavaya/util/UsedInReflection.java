package org.bhavaya.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Certain public methods are known to be accessed via reflection.  This is just marks methods
 * known to be accessed this way to stop them being accidentally deleted.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface UsedInReflection {
    /**
     * Totally optional value containing the string value to search for.  Should be pretty obvious anyway
     */
    String value() default "";
}
