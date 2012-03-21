package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.Arrays;

public class DmvBounds {
    
    private double[][] lbs;
    private double[][] ubs;

    public DmvBounds(IndexedDmvModel idm) {
        lbs = new double[idm.getNumConds()][];
        ubs = new double[idm.getNumConds()][];
        for (int c=0; c<lbs.length; c++) {
            lbs[c] = new double[idm.getNumParams(c)];
            ubs[c] = new double[lbs[c].length];
            Arrays.fill(lbs[c], Double.NEGATIVE_INFINITY);
            Arrays.fill(ubs[c], 0.0);
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

}
