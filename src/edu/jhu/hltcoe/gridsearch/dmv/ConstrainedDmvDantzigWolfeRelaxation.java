package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloRange;

import java.io.File;

import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;

// TODO: finish writing this class.
public class ConstrainedDmvDantzigWolfeRelaxation extends DmvDantzigWolfeRelaxation {

    protected static class ConstrainedMasterProblem extends MasterProblem {
        public IloRange couplConsShinyEdges;
    }
    
    public ConstrainedDmvDantzigWolfeRelaxation(File tempDir, int maxCutRounds, CutCountComputer initCutCountComp) {
        super(tempDir, maxCutRounds, initCutCountComp);
    }

    protected MasterProblem getNewMasterProblem() {
        return new ConstrainedMasterProblem();
    }

}
