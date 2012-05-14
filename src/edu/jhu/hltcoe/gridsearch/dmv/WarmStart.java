package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex.BasisStatus;

public class WarmStart {

    IloNumVar[] numVars;
    IloRange[] ranges;
    BasisStatus[] numVarStatuses;
    BasisStatus[] rangeStatuses;
    
}
