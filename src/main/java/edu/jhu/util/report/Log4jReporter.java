package edu.jhu.util.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4jReporter extends Reporter {

    private Logger log;
    
    protected Log4jReporter(Class<?> clazz) {
        log = LoggerFactory.getLogger(clazz);
    }
    
    @Override
    public void report(String key, Object val) {
        String msg = String.format("REPORT: %s = %s", key, val.toString());
        log.info(msg);
    }

}
