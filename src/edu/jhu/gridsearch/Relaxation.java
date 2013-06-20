package edu.jhu.gridsearch;

public interface Relaxation {

    RelaxedSolution getRelaxedSolution(ProblemNode curNode);

    RelaxedSolution getRelaxedSolution(ProblemNode curNode, double incumbentScore);

    void updateTimeRemaining(double timeoutSeconds);
    
}
