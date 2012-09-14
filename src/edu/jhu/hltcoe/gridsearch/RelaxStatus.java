/**
 * 
 */
package edu.jhu.hltcoe.gridsearch;

import ilog.cplex.IloCplex.Status;

public enum RelaxStatus {
    Optimal, Feasible, Infeasible, Unknown, Fathomed;

    public boolean hasSolution() {
        return this == Optimal || this == Feasible;
    }
    
    public static RelaxStatus get(Status status) {
        if (status == Status.Infeasible || status == Status.InfeasibleOrUnbounded || status == Status.Unbounded
                || status == Status.Bounded) {
            return Infeasible;
        } else if (status == Status.Error || status == Status.Unknown) {
            return Unknown;
        } else if (status == Status.Optimal || status == Status.Feasible){
            return Feasible;
        } else {
            throw new IllegalStateException("This should never be reached. Status = " + status);
        }
    }
}