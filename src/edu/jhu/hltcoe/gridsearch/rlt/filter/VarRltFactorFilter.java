package edu.jhu.hltcoe.gridsearch.rlt.filter;

import gnu.trove.TIntHashSet;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;

import java.util.List;

import no.uib.cipr.matrix.VectorEntry;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Accepts only RLT factors that have a non-zero coefficient for one of the given input matrix variable columns.
 */
public class VarRltFactorFilter implements RltFactorFilter {
    
    private TIntHashSet cols;
    private List<IloNumVar> vars;

    public VarRltFactorFilter(List<IloNumVar> vars) {
        this.vars = vars;
    }

    @Override
    public void init(Rlt rlt) throws IloException {
        cols = new TIntHashSet();
        for (IloNumVar var : vars) {
            cols.add(rlt.getInputMatrix().getIndex(var));
        }
        vars = null;
    }

    @Override
    public boolean accept(Factor f) {
        for (VectorEntry ve : f.G) {
            if (!Utilities.equals(ve.get(), 0.0, 1e-13) && cols.contains(ve.index())) {
                return true;
            }
        }
        return false;
    }
}