package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloRange;

import java.io.File;

import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOne;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.MasterProblem;

// TODO: finish writing this class.
public class ConstrainedDmvDantzigWolfeRelaxation extends DmvDantzigWolfeRelaxation {

    protected static class ConstrainedMasterProblem extends MasterProblem {
        public IloRange couplConsShinyEdges;
    }
    
    public ConstrainedDmvDantzigWolfeRelaxation(File tempDir, int maxCutRounds, LpSumToOne.CutCountComputer initCutCountComp) {
        super(tempDir, maxCutRounds, initCutCountComp);
    }

    protected MasterProblem getNewMasterProblem() {
        return new ConstrainedMasterProblem();
    }

}
