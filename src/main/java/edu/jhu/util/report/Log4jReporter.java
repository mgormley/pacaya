package edu.jhu.util.report;

import org.apache.log4j.Logger;

public class Log4jReporter extends Reporter {

    private Logger log;
    
    protected Log4jReporter(Class<?> clazz) {
        log = Logger.getLogger(clazz);
    }
    
    @Override
    public void report(String key, Object val) {
        String msg = String.format("REPORT: %s = %s", key, val.toString());
        log.info(msg);
    }

}
