package edu.jhu.globalopt;

public interface Relaxation {

    RelaxedSolution getRelaxedSolution(ProblemNode curNode);

    RelaxedSolution getRelaxedSolution(ProblemNode curNode, double incumbentScore);

    void updateTimeRemaining(double timeoutSeconds);
    
}
