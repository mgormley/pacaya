package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.Arrays;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Bounds for a conditional probability table.
 * 
 * @author mgormley
 */
public class CptBounds {

    /**
     * Model parameter upper bound: log(1.0)
     */
    public static final double DEFAULT_UPPER_BOUND = 0.0;

    /**
     * Model parameter lower bound: log(1 / (one trillion)) ~= -27
     */
    public static final double DEFAULT_LOWER_BOUND = Utilities.log(Math.pow(10, -12));

    private static final double MIN_BOUND_DIFF = 1.0 / Math.pow(2, 6);

    private double[][][] lbs;
    private double[][][] ubs;

    public CptBounds(IndexedCpt icpt) {
        int[][] totMaxFreqCm = icpt.getTotalMaxFreqCm();

        lbs = new double[2][icpt.getNumConds()][];
        ubs = new double[2][icpt.getNumConds()][];
        for (Type type : Type.values()) {
            int t = type.getAsInt();
            for (int c = 0; c < lbs[t].length; c++) {
                lbs[t][c] = new double[icpt.getNumParams(c)];
                ubs[t][c] = new double[lbs[t][c].length];

                if (type == Type.PARAM) {
                    // Fill model paramter bounds.
                    Arrays.fill(lbs[t][c], DEFAULT_LOWER_BOUND);
                    Arrays.fill(ubs[t][c], DEFAULT_UPPER_BOUND);

                    double totMaxFreqC = Vectors.sum(totMaxFreqCm[c]);
                    for (int m = 0; m < lbs[t][c].length; m++) {
                        if (totMaxFreqCm[c][m] == 0 && totMaxFreqC > 0) {
                            // Upper bound by zero(ish) if this parameter
                            // can't be used in the corpus AND if there are
                            // other parameters in its conditional probability
                            // table that
                            // won't be forced to zero.
                            ubs[t][c][m] = lbs[t][c][m];
                        }
                    }
                } else {
                    // Fill frequency count bounds.
                    Arrays.fill(lbs[t][c], 0);
                    for (int m = 0; m < lbs[t][c].length; m++) {
                        ubs[t][c][m] = totMaxFreqCm[c][m];
                    }
                }
            }
        }
    }

    public double getLb(Type type, int c, int m) {
        return lbs[type.getAsInt()][c][m];
    }

    public double getUb(Type type, int c, int m) {
        return ubs[type.getAsInt()][c][m];
    }

    public void set(Type type, int c, int m, double newLb, double newUb) {
        lbs[type.getAsInt()][c][m] = newLb;
        ubs[type.getAsInt()][c][m] = newUb;
    }

    public boolean canBranch(Type type, int c, int m) {
        return !Utilities.lte(ubs[type.getAsInt()][c][m] - lbs[type.getAsInt()][c][m], MIN_BOUND_DIFF);
    }

    public double getLogSpace() {
        double logSpace = 0.0;
        int t = Type.PARAM.getAsInt();
        for (int c = 0; c < lbs[t].length; c++) {
            for (int m = 0; m < lbs[t][c].length; m++) {
                logSpace += Utilities.logSubtractExact(ubs[t][c][m],
                        lbs[t][c][m]);
            }
        }
        return logSpace;
    }

    public int getNumParams(int c) {
        return lbs[Type.PARAM.getAsInt()][c].length;
    }

    @Override
    public String toString() {
        return "CptBounds [lbs=" + Arrays.deepToString(lbs) + ", ubs=" + Arrays.deepToString(ubs) + "]";
    }
    
    
}
