package edu.jhu.pacaya.util.report;


public class Reporter {

    protected Reporter() { };
    
    public void report(String key, Object val) { }

    public static Reporter getReporter(Class<?> clazz) {
        return ReporterManager.getReporter(clazz);
    }
    
}
