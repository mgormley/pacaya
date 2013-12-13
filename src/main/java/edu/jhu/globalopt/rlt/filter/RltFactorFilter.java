/**
 * 
 */
package edu.jhu.globalopt.rlt.filter;

import ilog.concert.IloException;
import edu.jhu.globalopt.rlt.Rlt;
import edu.jhu.lp.FactorBuilder.Factor;

public interface RltFactorFilter {
    boolean accept(Factor factor);
    void init(Rlt rlt) throws IloException;
}