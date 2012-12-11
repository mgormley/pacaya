package edu.jhu.hltcoe.gridsearch.rlt.filter;

import gnu.trove.TLongHashSet;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;

import java.util.List;

import no.uib.cipr.matrix.sparse.longs.LVectorEntry;
import no.uib.cipr.matrix.sparse.longs.SparseLVector;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Accepts only RLT rows that have a non-zero coefficient for some RLT variable corresponding
 * to the given pairs of variables.
 */
public class VarRltRowFilter implements RltRowFilter {
    
    private TLongHashSet rltVarIds;
    private List<Pair<IloNumVar, IloNumVar>> pairs;

    public VarRltRowFilter(List<Pair<IloNumVar,IloNumVar>> pairs) {
        this.pairs = pairs;
    }

    @Override
    public void init(Rlt rlt, long numUnfilteredRows) throws IloException {
        rltVarIds = new TLongHashSet();
        for (Pair<IloNumVar, IloNumVar> pair : pairs) {
            rltVarIds.add(rlt.getIdForRltVar(pair.get1(), pair.get2()));
        }
        pairs = null;
    }

    @Override
    public boolean acceptLeq(SparseLVector row, String rowName, Factor facI, Factor facJ, RowType type) {
        return acceptRow(row);
    }

    @Override
    public boolean acceptEq(SparseLVector row, String rowName, Factor facI, int k, RowType type) {
        return acceptRow(row);
    }

    private boolean acceptRow(SparseLVector row) {
        for (LVectorEntry ve : row) {
            if (!Utilities.equals(ve.get(), 0.0, 1e-13) && rltVarIds.contains(ve.index())) {
                return true;
            }
        }
        return false;
    }
}