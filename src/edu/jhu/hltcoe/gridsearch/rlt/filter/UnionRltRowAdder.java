package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.util.tuple.OrderedPair;
import edu.jhu.hltcoe.util.tuple.UnorderedPair;

public class UnionRltRowAdder implements RltRowAdder {

    List<RltRowAdder> adders;
    
    public UnionRltRowAdder(RltRowAdder... adders) {
        this.adders = new ArrayList<RltRowAdder>(Arrays.asList(adders));
    }

    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        for (RltRowAdder adder : adders) {
            if (adder != null) {
                adder.init(rlt, numUnfilteredRows);
            }
        }
    }
    
    @Override
    public Collection<OrderedPair> getRltRowsForEq(int startFac, int endFac, int numVars, RowType type) {
        Set<OrderedPair> rltRows = new HashSet<OrderedPair>();
        for (RltRowAdder adder : adders) {
            rltRows.addAll(adder.getRltRowsForEq(startFac, endFac, numVars, type));
        }
        return rltRows;
    }

    @Override
    public Collection<UnorderedPair> getRltRowsForLeq(int startFac1, int endFac1, int startFac2, int endFac2,
            RowType type) {
        Set<UnorderedPair> rltRows = new HashSet<UnorderedPair>();
        for (RltRowAdder adder : adders) {
            rltRows.addAll(adder.getRltRowsForLeq(startFac1, endFac1, startFac2, endFac2, type));
        }
        return rltRows;
    }

}
