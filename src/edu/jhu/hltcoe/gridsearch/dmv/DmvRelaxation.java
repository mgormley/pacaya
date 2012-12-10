package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.hltcoe.train.DmvTrainCorpus;

public interface DmvRelaxation {

    // TODO: Fix this initialization API.
    // These two methods must be called in sequence to initialize.
    void init1(DmvTrainCorpus corpus);
    void init2(DmvSolution initFeasSol);

    void addFeasibleSolution(DmvSolution initFeasSol);

    RelaxedSolution solveRelaxation();
    RelaxedSolution solveRelaxation(double incumbentScore, int depth);

    WarmStart getWarmStart();
    void setWarmStart(WarmStart warmStart);

    void reverseApply(CptBoundsDeltaList deltas);
    void forwardApply(CptBoundsDeltaList deltas);
    
    double computeTrueObjective(double[][] logProbs, DepTreebank treebank);

    IndexedDmvModel getIdm();

    CptBounds getBounds();

    void updateTimeRemaining(double timeoutSeconds);
    void end();

}
