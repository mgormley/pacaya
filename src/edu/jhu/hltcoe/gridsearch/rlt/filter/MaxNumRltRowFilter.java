package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.longs.SparseLVector;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Prng;

/**
 * Randomly accepts only a fixed proportion of the rows.
 */
public class MaxNumRltRowFilter implements RltRowFilter {

    private double initProp;
    private double initMax;
    private int cutMax;
    private int initCount;
    private int cutCount;
    
    
    /**
     * Limits the initial and cut rows to a fixed quantity. This will randomly
     * sample a fixed quantity from the initial set of rows, but will then
     * accept all cut rows up to the limit.
     * 
     * @param initFactor
     *            The maximum number of initial RLT rows added.
     * @param cutMax
     *            The maximum number of RLT rows added from cuts.
     */
    public MaxNumRltRowFilter(int initMax, int cutMax) {
        this.initMax = initMax;
        this.cutMax = cutMax;
    }

    @Override
    public void init(Rlt rlt, int numUnfilteredRows) throws IloException {
        this.initProp = initMax / numUnfilteredRows;
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
            if (initCount < initMax && Prng.nextDouble() < initProp) {
                initCount++;
                return true;
            } else {
                return false;
            }
        } else if (type == RowType.CUT) {
            if (cutCount < cutMax) {
                cutCount++;
                return true;
            } else {
                return false;
            }
        } else {
            throw new IllegalStateException("unhandled type: " + type);
        }
    }
}