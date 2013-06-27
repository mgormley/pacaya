/**
 * 
 */
package edu.jhu.gridsearch.rlt.filter;

import ilog.concert.IloException;
import edu.jhu.gridsearch.rlt.Rlt;
import edu.jhu.lp.FactorBuilder;
import edu.jhu.lp.FactorBuilder.Factor;

public interface RltFactorFilter {
    boolean accept(Factor factor);
    void init(Rlt rlt) throws IloException;
}