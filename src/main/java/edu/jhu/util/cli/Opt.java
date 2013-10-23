package edu.jhu.util.cli;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Command line argument annotation.
 * 
 * @author mgormley
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Opt {

    public static final String DEFAULT_STRING = "THIS_IS_THE_DEFAULT";
    
    public String name() default DEFAULT_STRING;
    public String description() default "";
    public boolean hasArg() default true;
    public boolean required() default false;
    
}
