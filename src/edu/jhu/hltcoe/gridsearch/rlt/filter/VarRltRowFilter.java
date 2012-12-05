package edu.jhu.hltcoe.gridsearch.rlt.filter;

import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.gridsearch.rlt.filter.RltRowFilter.RowType;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;
import gnu.trove.TIntHashSet;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;

import java.util.List;

import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Accepts only RLT rows that have a non-zero coefficient for some RLT variable corresponding
 * to the given pairs of variables.
 */
public class VarRltRowFilter implements RltRowFilter {
    
    private TIntHashSet rltVarIds;
    private List<Pair<IloNumVar, IloNumVar>> pairs;

    public VarRltRowFilter(List<Pair<IloNumVar,IloNumVar>> pairs) {
        this.pairs = pairs;
    }

    @Override
    public void init(Rlt rlt) throws IloException {
        rltVarIds = new TIntHashSet();
        for (Pair<IloNumVar, IloNumVar> pair : pairs) {
            rltVarIds.add(rlt.getIdForRltVar(pair.get1(), pair.get2()));
        }
        pairs = null;
    }

    @Override
    public boolean acceptLeq(SparseVector row, String rowName, Factor facI, Factor facJ, RowType type) {
        return acceptRow(row);
    }

    @Override
    public boolean acceptEq(SparseVector row, String rowName, Factor facI, int k, RowType type) {
        return acceptRow(row);
    }

    private boolean acceptRow(SparseVector row) {
        for (VectorEntry ve : row) {
            if (!Utilities.equals(ve.get(), 0.0, 1e-13) && rltVarIds.contains(ve.index())) {
                return true;
            }
        }
        return false;
    }
}