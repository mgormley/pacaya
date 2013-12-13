package edu.jhu.globalopt.dmv;

import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.BasisStatus;

public class WarmStart {

    public IloNumVar[] numVars;
    public IloRange[] ranges;
    public BasisStatus[] numVarStatuses;
    public BasisStatus[] rangeStatuses;
    
}
