package edu.jhu.globalopt;

import edu.jhu.globalopt.LazyBranchAndBoundSolver.NodeResult;


public interface NodeOrderer extends Iterable<ProblemNode> {

    void addRoot(ProblemNode root);
    void addChildrenOfResult(NodeResult result, double globalUb, double globalLb, boolean isRoot);
    ProblemNode remove();
    int size();
    boolean isEmpty();
    void clear();
    
}
