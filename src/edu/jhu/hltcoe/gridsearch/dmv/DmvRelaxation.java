package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;

public interface DmvRelaxation {

    double computeTrueObjective(double[][] logProbs, DepTreebank treebank);

    IndexedDmvModel getIdm();

    double[][] getRegretCm();

    void reverseApply(DmvBoundsDelta deltas);
    void forwardApply(DmvBoundsDelta deltas);

    DmvBounds getBounds();

    void end();

    RelaxedDmvSolution solveRelaxation();
    RelaxedDmvSolution solveRelaxation(double incumbentScore);

    WarmStart getWarmStart();
    void setWarmStart(WarmStart warmStart);

    // TODO: Fix this initialization API.
    // These two methods must be called in sequence to initialize.
    void setSentences(SentenceCollection sentences);
    void init(DmvSolution initFeasSol);
    
    void addFeasibleSolution(DmvSolution initFeasSol);

}
