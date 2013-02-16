/**
 * 
 */
package edu.jhu.hltcoe.util.cplex;

import no.uib.cipr.matrix.sparse.longs.SparseLVector;

public class LpRow {
    private double lb;
    private double ub;
    private SparseLVector coefs;
    private String name;

    public LpRow(double lb, SparseLVector coefs, double ub, String name) {
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

    public SparseLVector getCoefs() {
        return coefs;
    }

    public String getName() {
        return name;
    }
}