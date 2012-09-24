package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public interface DmvRelaxation {

    double computeTrueObjective(double[][] logProbs, DepTreebank treebank);

    IndexedDmvModel getIdm();

    /**
     * @return A CxM array of doubles containing the regret of each model
     *         parameter, or null if the regret is unavailable.
     */
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
    void init1(DmvTrainCorpus corpus);
    void init2(DmvSolution initFeasSol);

    void addFeasibleSolution(DmvSolution initFeasSol);

}
