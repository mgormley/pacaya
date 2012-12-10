/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.SparseVector;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;

public interface RltRowFilter {
    public static enum RowType {
        /**
         * For rows created at initialization time.
         */
        INITIAL,
        /**
         * For rows subsequently added as cuts.
         */
        CUT
    }

    void init(Rlt rlt, int numUnfilteredRows) throws IloException;

    boolean acceptEq(SparseVector row, String rowName, Factor facI, int k, RowType type);

    boolean acceptLeq(SparseVector row, String rowName, Factor facI, Factor facJ, RowType type);
}