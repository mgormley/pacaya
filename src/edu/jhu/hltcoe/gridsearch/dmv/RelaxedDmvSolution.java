package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.cplex.IloCplex.Status;
import edu.jhu.hltcoe.gridsearch.Solution;

public class RelaxedDmvSolution implements Solution {

    private double score;
    private double[][] logProbs;
    private double[][] fracRoots;
    private double[][][] fracChildren;
    private RelaxStatus status;
    
    public enum RelaxStatus {
        Optimal, Feasible, Infeasible, Unknown;

        public boolean hasSolution() {
            return this == Optimal || this == Feasible;
        }
        
        public static RelaxStatus get(Status status) {
            if (status == Status.Infeasible || status == Status.InfeasibleOrUnbounded || status == Status.Unbounded) {
                return Infeasible;
            } else {
                return Feasible;
            }                
        }
    }
    
    public RelaxedDmvSolution(double[][] logProbs, double[][] fracRoots, double[][][] fracChildren, double score, RelaxStatus status) {
        super();
        this.score = score;
        this.logProbs = logProbs;
        this.fracRoots = fracRoots;
        this.fracChildren = fracChildren;
        this.status = status;
    }

    @Override
    public double getScore() {
        return score;
    }

    public double[][] getLogProbs() {
        return logProbs;
    }
    
    public double[][] getFracRoots() {
        return fracRoots;
    }
    
    public double[][][] getFracChildren() {
        return fracChildren;
    }

    public RelaxStatus getStatus() {
        return status;
    }

}
