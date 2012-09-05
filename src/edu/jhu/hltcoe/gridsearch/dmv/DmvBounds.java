package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.Arrays;

import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Utilities;

public class DmvBounds {
    
    /** 
     * Upper bound by log(1.0)
     */
    public static final double DEFAULT_UPPER_BOUND = 0.0;
    
    /**
     *  Lower bound by log(1 / (one trillion)) ~= -27 
     */
    public static final double DEFAULT_LOWER_BOUND = Utilities.log(Math.pow(10,-12));
    
    private static final double MIN_BOUND_DIFF = 1.0 / Math.pow(2, 6);
    
    private double[][] lbs;
    private double[][] ubs;

    public DmvBounds(IndexedDmvModel idm) {
        int[][] totMaxFreqCm = idm.getTotalMaxFreqCm();
        
        lbs = new double[idm.getNumConds()][];
        ubs = new double[idm.getNumConds()][];
        for (int c=0; c<lbs.length; c++) {
            lbs[c] = new double[idm.getNumParams(c)];
            ubs[c] = new double[lbs[c].length];
            Arrays.fill(lbs[c], DEFAULT_LOWER_BOUND);
            Arrays.fill(ubs[c], DEFAULT_UPPER_BOUND);
            
            double totMaxFreqC = Vectors.sum(totMaxFreqCm[c]);
            for (int m=0; m<lbs[c].length; m++) {
                if (totMaxFreqCm[c][m] == 0 && totMaxFreqC > 0) {
                    // Upper bound by zero(ish) if this parameter 
                    // can't be used in the corpus AND if there are
                    // other parameters in its conditional probability table that
                    // won't be forced to zero.
                    ubs[c][m] = lbs[c][m];
                }
            }
        }
    }

    public double getLb(int c, int m) {
        return lbs[c][m];
    }

    public double getUb(int c, int m) {
        return ubs[c][m];
    }

    public void set(int c, int m, double newLb, double newUb) {
        lbs[c][m] = newLb;
        ubs[c][m] = newUb;
    }
    
    public boolean canBranch(int c, int m) {
        return ! Utilities.lte(ubs[c][m] - lbs[c][m],  MIN_BOUND_DIFF);
    }

    public double getLogSpace() {
        double logSpace = 0.0;
        for (int c=0; c<lbs.length; c++) {
            for (int m=0; m<lbs[c].length; m++) {
                logSpace += Utilities.logSubtractExact(ubs[c][m], lbs[c][m]);
            }
        }
        return logSpace;
    }

}
