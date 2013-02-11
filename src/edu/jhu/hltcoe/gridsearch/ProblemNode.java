package edu.jhu.hltcoe.gridsearch;

import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.WarmStart;

public interface ProblemNode {

    List<ProblemNode> branch(Relaxation relax, RelaxedSolution relaxSol);
    
    int getId();
    
    int getDepth();

    int getSide();

    double getLocalUb();

    WarmStart getWarmStart();

    void setWarmStart(WarmStart warmStart);

    void setOptimisticBound(double optimisticBound);

}
