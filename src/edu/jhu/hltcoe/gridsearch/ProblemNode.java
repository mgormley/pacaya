package edu.jhu.hltcoe.gridsearch;

import java.util.List;

public interface ProblemNode {

    double getOptimisticBound();

    double getOptimisticBound(double incumbentScore);

    Solution getFeasibleSolution();

    List<ProblemNode> branch();
    
    int getId();
    
    int getDepth();

    int getSide();

    void setAsActiveNode();

    double getLogSpace();

    RelaxedSolution getRelaxedSolution();

    void updateTimeRemaining(double timeoutSeconds);

}
