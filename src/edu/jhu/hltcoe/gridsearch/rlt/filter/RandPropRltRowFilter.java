package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.SparseVector;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Prng;

/**
 * Randomly accepts only a fixed proportion of the rows.
 */
public class RandPropRltRowFilter implements RltRowFilter {

    private double acceptProp;
    
    public RandPropRltRowFilter(double acceptProp) {
        this.acceptProp = acceptProp;
    }
    
    @Override
    public void init(Rlt rlt) throws IloException {
        // Do nothing.
    }
    
    @Override
    public boolean acceptEq(SparseVector row, String rowName, Factor facI, int k) {
        return Prng.nextDouble() < acceptProp; 
    }

    @Override
    public boolean acceptLeq(SparseVector row, String rowName, Factor facI, Factor facJ) {
        return Prng.nextDouble() < acceptProp; 
    }
    
}