package edu.jhu.hltcoe.gridsearch;


public interface NodeOrderer extends Iterable<ProblemNode> {

    boolean add(ProblemNode node);
    ProblemNode remove();
    int size();
    boolean isEmpty();
    void clear();
    
}
