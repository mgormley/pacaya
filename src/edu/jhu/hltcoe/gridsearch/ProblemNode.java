package edu.jhu.hltcoe.gridsearch;

import java.util.List;

public interface ProblemNode {

    double getOptimisticBound();

    Solution getFeasibleSolution();

    List<ProblemNode> branch();
    
    int getId();
    
    int getDepth();

    void setAsActiveNode(ProblemNode prevNode);

}
