package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;

import java.util.Collection;

import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.util.tuple.OrderedPair;
import edu.jhu.hltcoe.util.tuple.PairSampler;
import edu.jhu.hltcoe.util.tuple.UnorderedPair;

/**
 * Randomly accepts only a fixed proportion of the rows.
 */
public class RandPropRltRowAdder implements RltRowAdder {

    private double initProp;
    private double cutProp;
    
    public RandPropRltRowAdder(double initProp, double cutProp) {
        this.initProp = initProp;
        this.cutProp = cutProp;
    }
    
    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        // Do nothing.
    }
    
    @Override
    public Collection<OrderedPair> getRltRowsForEq(int startFac, int endFac, int numVars, RowType type) {
        double prop = getProp(type);
        return PairSampler.sampleOrderedPairs(startFac, endFac, 0, numVars, prop);   
    }

    @Override
    public Collection<UnorderedPair> getRltRowsForLeq(int startFac1, int endFac1, int startFac2, int endFac2, RowType type) {
        double prop = getProp(type);
        return PairSampler.sampleUnorderedPairs(startFac1, endFac1, startFac2, endFac2, prop);
    }

    private double getProp(RowType type) {
        return (type == RowType.INITIAL) ? initProp : cutProp;
    }
}