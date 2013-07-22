package edu.jhu.gridsearch.rlt.filter;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;

import java.util.List;

import edu.jhu.gridsearch.rlt.Rlt;
import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.prim.map.LongDoubleEntry;
import edu.jhu.prim.set.LongHashSet;
import edu.jhu.util.Utilities;

/**
 * Accepts only RLT factors that have a non-zero coefficient for one of the given input matrix variable columns.
 */
public class VarRltFactorFilter implements RltFactorFilter {
    
    private LongHashSet cols;
    private List<IloNumVar> vars;

    public VarRltFactorFilter(List<IloNumVar> vars) {
        this.vars = vars;
    }

    @Override
    public void init(Rlt rlt) throws IloException {
        cols = new LongHashSet();
        for (IloNumVar var : vars) {
            cols.add(rlt.getInputMatrix().getIndex(var));
        }
        vars = null;
    }

    @Override
    public boolean accept(Factor f) {
        for (LongDoubleEntry ve : f.G) {
            if (!Utilities.equals(ve.get(), 0.0, 1e-13) && cols.contains(ve.index())) {
                return true;
            }
        }
        return false;
    }
}