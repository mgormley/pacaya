package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;

public interface DmvRelaxation {

    double computeTrueObjective(double[][] logProbs, DepTreebank treebank);

    IndexedDmvModel getIdm();

    double[][] getRegretCm();

    void reverseApply(DmvBoundsDelta deltas);
    void forwardApply(DmvBoundsDelta deltas);

    DmvBounds getBounds();

    void end();

    RelaxedDmvSolution solveRelaxation();

    WarmStart getWarmStart();
    void setWarmStart(WarmStart warmStart);

    void init(DmvSolution initFeasSol);

}
