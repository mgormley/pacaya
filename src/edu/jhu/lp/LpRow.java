/**
 * 
 */
package edu.jhu.hltcoe.lp;

import edu.jhu.hltcoe.util.vector.SortedLongDoubleVector;

public class LpRow {
    private double lb;
    private double ub;
    private SortedLongDoubleVector coefs;
    private String name;

    public LpRow(double lb, SortedLongDoubleVector coefs, double ub, String name) {
        super();
        this.lb = lb;
        this.coefs = coefs;
        this.ub = ub;
        this.name = name;
    }

    public double getLb() {
        return lb;
    }

    public double getUb() {
        return ub;
    }

    public SortedLongDoubleVector getCoefs() {
        return coefs;
    }

    public String getName() {
        return name;
    }
}