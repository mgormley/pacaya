package edu.jhu.gridsearch;

import java.util.List;

public interface ChildOrderer {

    /**
     * Orders children to be pushed onto a stack.
     *  
     * @return The children, ordered from low priority to high priority.
     */
    List<ProblemNode> orderChildren(RelaxedSolution relaxSol, RelaxedSolution rootSol, List<ProblemNode> children);

}
