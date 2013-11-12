package edu.jhu.gridsearch.rlt.filter;

import ilog.concert.IloException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import edu.jhu.gridsearch.rlt.Rlt;
import edu.jhu.prim.sample.PairSampler;
import edu.jhu.prim.tuple.OrderedPair;
import edu.jhu.prim.tuple.UnorderedPair;
import edu.jhu.util.collections.Lists;

/**
 * Randomly accepts only a fixed proportion of the initial rows, and then
 * accepts (in an arbitrary order) the cut rows.
 */
public class MaxNumRltRowAdder implements RltRowAdder {

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
    public MaxNumRltRowAdder(int initMax, int cutMax) {
        this.initMax = initMax;
        this.cutMax = cutMax;
    }

    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        this.initProp = initMax / numUnfilteredRows;
        initCount = 0;
        cutCount = 0;
    }

    @Override
    public Collection<OrderedPair> getRltRowsForEq(int startFac, int endFac, int numVars, RowType type) {
        if (type == RowType.INITIAL) {
            if (initCount < initMax) {
                Collection<OrderedPair> rltRows = PairSampler.sampleOrderedPairs(startFac, endFac, 0, numVars, initProp);
                if (rltRows.size() + initCount > initMax) {
                    rltRows = Lists.sublist(new ArrayList<OrderedPair>(rltRows), 0, (int)initMax - initCount);
                }
                initCount += rltRows.size();
                return rltRows;
            } else {
                return Collections.emptyList();
            }
        } else if (type == RowType.CUT) {
            if (cutCount < cutMax) {
                Collection<OrderedPair> rltRows = PairSampler.sampleOrderedPairs(startFac, endFac, 0, numVars, 1.0);
                if (rltRows.size() + cutCount > cutMax) {
                    rltRows = Lists.sublist(new ArrayList<OrderedPair>(rltRows), 0, (int)cutMax - cutCount);
                }
                cutCount += rltRows.size();
                return rltRows;
            } else {
                return Collections.emptyList();
            }
        } else {
            throw new IllegalStateException("unhandled type: " + type);
        }
    }

    @Override
    public Collection<UnorderedPair> getRltRowsForLeq(int startFac1, int endFac1, int startFac2, int endFac2, RowType type) {
        if (type == RowType.INITIAL) {
            if (initCount < initMax) {
                Collection<UnorderedPair> rltRows = PairSampler.sampleUnorderedPairs(startFac1, endFac1, startFac2, endFac2, initProp);
                if (rltRows.size() + initCount > initMax) {
                    rltRows = Lists.sublist(new ArrayList<UnorderedPair>(rltRows), 0, (int)initMax - initCount);
                }
                initCount += rltRows.size();
                return rltRows;
            } else {
                return Collections.emptyList();
            }
        } else if (type == RowType.CUT) {
            if (cutCount < cutMax) {
                Collection<UnorderedPair> rltRows = PairSampler.sampleUnorderedPairs(startFac1, endFac1, startFac2, endFac2, 1.0);
                if (rltRows.size() + cutCount > cutMax) {
                    rltRows = Lists.sublist(new ArrayList<UnorderedPair>(rltRows), 0, (int)cutMax - cutCount);
                }
                cutCount += rltRows.size();
                return rltRows;
            } else {
                return Collections.emptyList();
            }
        } else {
            throw new IllegalStateException("unhandled type: " + type);
        }
    }
    
}