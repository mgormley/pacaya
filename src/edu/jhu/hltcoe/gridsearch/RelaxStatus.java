/**
 * 
 */
package edu.jhu.hltcoe.gridsearch;

import ilog.cplex.IloCplex.Status;

public enum RelaxStatus {
    Optimal, Feasible, Infeasible, Unknown, Pruned;

    public boolean hasSolution() {
        return this == Optimal || this == Feasible;
    }
    
    /**
     * Gets the relaxation status for the Dantzig-Wolfe algorithm.
     * This method returns feasible even if CPLEX has found the optimal solution to the master.
     */
    public static RelaxStatus getForDw(Status status) {
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
    
    /**
     * Gets the relaxation status for an LP relaxation.
     * This method returns optimal if CPLEX returns optimal.
     */
    public static RelaxStatus getForLp(Status status) {
        if (status == Status.Infeasible || status == Status.InfeasibleOrUnbounded || status == Status.Unbounded
                || status == Status.Bounded) {
            return Infeasible;
        } else if (status == Status.Error || status == Status.Unknown) {
            return Unknown;
        } else if (status == Status.Optimal) {
            return Optimal;
        } else if (status == Status.Feasible){
            return Feasible;
        } else {
            throw new IllegalStateException("This should never be reached. Status = " + status);
        }
    }
}