package edu.jhu.pacaya.util.report;

import java.util.Arrays;
import java.util.List;

public class ReporterList extends Reporter {

    private List<Reporter> rs;

    protected ReporterList(Reporter... rs) {
        this(Arrays.asList(rs));
    }
    
    protected ReporterList(List<Reporter> rs) {
        super();
        this.rs = rs;
    }
    
    @Override
    public void report(String key, Object val) {
        for (Reporter r : rs) {
            r.report(key, val);
        }
    }

}
