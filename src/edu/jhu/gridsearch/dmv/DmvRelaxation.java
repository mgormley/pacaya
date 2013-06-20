package edu.jhu.gridsearch.dmv;

import edu.jhu.data.DepTreebank;
import edu.jhu.gridsearch.Relaxation;
import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.CptBoundsDeltaList;
import edu.jhu.train.DmvTrainCorpus;

public interface DmvRelaxation extends Relaxation {

    // TODO: Fix this initialization API.
    // These two methods must be called in sequence to initialize.
    void init1(DmvTrainCorpus corpus);
    void init2(DmvSolution initFeasSol);

    void addFeasibleSolution(DmvSolution initFeasSol);

    WarmStart getWarmStart();
    void setWarmStart(WarmStart warmStart);

    // TODO: make these private.
    void reverseApply(CptBoundsDeltaList deltas);
    void forwardApply(CptBoundsDeltaList deltas);
    
    double computeTrueObjective(double[][] logProbs, DepTreebank treebank);

    IndexedDmvModel getIdm();
    CptBounds getBounds();
    // For assertions only.
    DmvProblemNode getActiveNode();

    void updateTimeRemaining(double timeoutSeconds);
    void end();

}
