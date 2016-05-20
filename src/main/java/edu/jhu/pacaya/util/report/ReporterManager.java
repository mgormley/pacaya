package edu.jhu.pacaya.util.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.util.cli.Opt;

public class ReporterManager {

    private static final Logger log = LoggerFactory.getLogger(ReporterManager.class);

    @Opt(hasArg = true, description = "File to which to which reports should be written.")
    public static File reportOut = null;
    
    private static Map<Class<?>,Reporter> reps = new HashMap<>();
    private static StreamReporter wr;
    private static boolean useLogger;
    private static boolean initialized = false;
    
    private ReporterManager() { }
    
    public static void init(File reportOut, boolean useLogger) {
        if (initialized) {
            log.warn("ReporterManager.init() is being called twice.");
        }
        ReporterManager.useLogger = useLogger;
        if (wr != null) {
            throw new RuntimeException("setReportFile() may only be called once");
        }
        try {            
            if (reportOut != null) {
                wr = new StreamReporter(new FileOutputStream(reportOut));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        initialized = true;
        if (reps != null) {
            // Update reporters that were created before initialization.
            for (Class<?> clazz : reps.keySet()) {
                InitReporter rep = (InitReporter) reps.get(clazz);
                rep.set(getReporter(clazz));
            }
            reps = null;
        }
    }
    
    public static Reporter getReporter(Class<?> clazz) {
        Reporter r;
        if (!initialized) {
            r = new InitReporter();
            reps.put(clazz, r);
        } else if (useLogger && wr != null) {
            r = new ReporterList(new Log4jReporter(clazz), wr);
        } else if (useLogger && wr == null) {
            r = new Log4jReporter(clazz);
        } else if (!useLogger && wr != null) {
            r = wr;
        } else {
            r = new ReporterList();
        }
        return r;
    }
    
    public static void close() {
        if (wr != null) {
            wr.close();
        }
    }
    
}
