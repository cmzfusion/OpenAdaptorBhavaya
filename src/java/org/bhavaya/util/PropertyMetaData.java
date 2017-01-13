package org.bhavaya.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Nov 11, 2006
 * Time: 1:54:24 PM
 * To change this template use File | Settings | File Templates.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PropertyMetaData {
    static final String NULL = "<NULL>";

    String displayName() default NULL;
    String description() default NULL;
    boolean hidden() default false;
    Class<?>[] validPropertyTypes() default {};
}
