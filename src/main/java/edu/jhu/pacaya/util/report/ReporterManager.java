package edu.jhu.pacaya.util.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import edu.jhu.pacaya.util.cli.Opt;

public class ReporterManager {
    
    @Opt(hasArg = true, description = "File to which to which reports should be written.")
    public static File reportOut = null;
    
    private static Map<Class<?>,Reporter> reps = new HashMap<>();
    private static StreamReporter wr;
    private static boolean useLog4j;
    private static boolean initialized = false;
    
    private ReporterManager() { }
    
    public static void init(File reportOut, boolean useLog4j) {
        ReporterManager.useLog4j = useLog4j;
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
        // Update reporters that were created before initialization.
        for (Class<?> clazz : reps.keySet()) {
            InitReporter rl = (InitReporter) reps.get(clazz);
            rl.set(getReporter(clazz));
        }
        reps = null;
    }
    
    public static Reporter getReporter(Class<?> clazz) {
        Reporter r;
        if (!initialized) {
            r = new InitReporter();
            reps.put(clazz, r);
        } else if (useLog4j && wr != null) {
            r = new ReporterList(new Log4jReporter(clazz), wr);
        } else if (useLog4j && wr == null) {
            r = new Log4jReporter(clazz);
        } else if (!useLog4j && wr != null) {
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
