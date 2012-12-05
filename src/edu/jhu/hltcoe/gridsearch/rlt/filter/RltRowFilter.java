/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.rlt.filter;

import ilog.concert.IloException;
import no.uib.cipr.matrix.sparse.SparseVector;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;

public interface RltRowFilter {
    void init(Rlt rlt) throws IloException;
    boolean acceptEq(SparseVector row, String rowName, Factor facI, int k);
    boolean acceptLeq(SparseVector row, String rowName, Factor facI, Factor facJ);
}