package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.longs.SparseLVector;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Prng;

/**
 * Randomly accepts only a fixed proportion of the rows.
 */
public class RandPropRltRowFilter implements RltRowFilter {

    private double initProp;
    private double cutProp;
    
    public RandPropRltRowFilter(double initProp, double cutProp) {
        this.initProp = initProp;
        this.cutProp = cutProp;
    }
    
    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        // Do nothing.
    }
    
    @Override
    public boolean acceptEq(SparseLVector row, String rowName, Factor facI, int k, RowType type) {
        return accept(type);
    }

    @Override
    public boolean acceptLeq(SparseLVector row, String rowName, Factor facI, Factor facJ, RowType type) {
        return accept(type);
    }
    
    private boolean accept(RowType type) {
        if (type == RowType.INITIAL) {
            return Prng.nextDouble() < initProp;
        } else if (type == RowType.CUT) {
            return Prng.nextDouble() < cutProp;
        } else {
            throw new IllegalStateException("unhandled type: " + type);
        }
    }
}