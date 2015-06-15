package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;

/**
 * FOR TESTING ONLY. Treats a GlobalFactor as an ExplicitFactor.
 * 
 * TODO: Implement AutodiffFactor.
 * 
 * @author mgormley
 */
public class ExplicitGlobalFactor extends ExplicitFactor {

    private static final long serialVersionUID = 1L;

    public ExplicitGlobalFactor(GlobalFactor gf) {
        super(gf.getVars());
        // Initialize from the global factor.
        for (int c=0; c<this.size(); c++) {
            double score = gf.getLogUnormalizedScore(c);
            this.setValue(c, score);
        }
    }

}
