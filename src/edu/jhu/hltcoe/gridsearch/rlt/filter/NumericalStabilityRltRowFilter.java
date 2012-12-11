package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.longs.SparseLVector;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;

/**
 * Accepts only rows with non-zero coefficients whose absolute values are greater than some value.
 */
public class NumericalStabilityRltRowFilter implements RltRowFilter {

    private double minCoef;
    private double maxCoef;
    
    public NumericalStabilityRltRowFilter(double minCoef, double maxCoef) {
        this.minCoef = minCoef;
        this.maxCoef = maxCoef;
    }
    
    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        // Do nothing.
    }
    
    @Override
    public boolean acceptEq(SparseLVector row, String rowName, Factor facI, int k, RowType type) {
        return accept(row);
    }

    @Override
    public boolean acceptLeq(SparseLVector row, String rowName, Factor facI, Factor facJ, RowType type) {
        return accept(row);
    }

    private boolean accept(SparseLVector row) {
        double[] data = row.getData();
        for (int i=0; i<row.getUsed(); i++) {
            double absVal = Math.abs(data[i]);
            if (absVal < minCoef || maxCoef < absVal) {
                return false;
            }
        }
        return true;
    }
}