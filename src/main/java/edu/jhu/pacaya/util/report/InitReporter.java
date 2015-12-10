package edu.jhu.pacaya.util.report;

/**
 * Lazily initialized reporter. This is used for classes that call ReporterManager.getReporter()
 * before it has been initialized.
 * 
 * @author mgormley
 */
public class InitReporter extends Reporter {

    private Reporter r;

    protected InitReporter() {
        this.r = null;
    }

    @Override
    public void report(String key, Object val) {
        if (r == null) {
            throw new RuntimeException("Unable to report before ReporterManager has been initialized");
        }
        r.report(key, val);
    }

    public void set(Reporter r) {
        this.r = r;
    }

}
