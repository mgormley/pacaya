/**
 * 
 */
package edu.jhu.hltcoe.gridsearch;

public class FathomStats {
    
    public enum FathomStatus {
        Infeasible, Pruned, CompletelySolved, BottomedOut, NotFathomed;
    }
    
    private int numCompletelySolved = 0;
    int numPruned = 0;
    int numInfeasible = 0;
    private int numBottomedOut = 0;
    private int depthSum = 0;
    
    public int getNumFathomed() {
        return numPruned + numInfeasible + numCompletelySolved + numBottomedOut;
    }
    
    public void fathom(ProblemNode node, FathomStatus status) {
        switch (status) {
        case Infeasible:
            numInfeasible++;
            break;
        case Pruned:
            numPruned++;
            break;
        case CompletelySolved:
            numCompletelySolved++;
            break;
        case BottomedOut:
            numBottomedOut++;
            break;
        }
        depthSum += node.getDepth();
    }
        
    public double getAverageDepth() {
        return (double) depthSum / getNumFathomed();
    }
}