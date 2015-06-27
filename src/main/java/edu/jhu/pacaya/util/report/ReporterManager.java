package edu.jhu.pacaya.util.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.pacaya.util.cli.Opt;

public class ReporterManager {
    
    @Opt(hasArg = true, description = "File to which to which reports should be written.")
    public static File reportOut = null;
    
    private static Map<Class<?>,Reporter> reps = new HashMap<>();
    private static StreamReporter wr;
    private static boolean useLog4j;
    
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
    }
    
    public static Reporter getReporter(Class<?> clazz) {
        Reporter r = reps.get(clazz);
        if (r == null) {
            if (useLog4j && wr != null) {
                r = new ReporterList(new Log4jReporter(clazz), wr);
            } else if (useLog4j && wr == null) {
                r = new Log4jReporter(clazz);
            } else if (!useLog4j && wr != null) {
                r = wr;
            } else {
                r = new ReporterList();
            }
        }
        return r;
    }
    
    public static void close() {
        if (wr != null) {
            wr.close();
        }
    }
    
}
