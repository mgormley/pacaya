/**
 * 
 */
package edu.jhu.lp;

import edu.jhu.prim.vector.LongDoubleSortedVector;

public class LpRow {
    private double lb;
    private double ub;
    private LongDoubleSortedVector coefs;
    private String name;

    public LpRow(double lb, LongDoubleSortedVector coefs, double ub, String name) {
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

    public LongDoubleSortedVector getCoefs() {
        return coefs;
    }

    public String getName() {
        return name;
    }
}