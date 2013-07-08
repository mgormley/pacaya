package edu.jhu.util;

public class Logger extends org.apache.log4j.Logger {

    protected Logger(String name) {
        super(name);
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getCanonicalName());
    }

    public void warn(String formatString, Object... args) {
        this.warn(String.format(formatString, args));
    }
    
    public void warn(String formatString, Object... args) {
        this.warn(String.format(formatString, args));
    }    

    public void info(String formatString, Object... args) {
        this.info(String.format(formatString, args));
    }
    
}
