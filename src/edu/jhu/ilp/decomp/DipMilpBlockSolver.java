package edu.jhu.hltcoe.ilp.decomp;

import java.io.File;

import edu.jhu.hltcoe.ilp.IlpSolver;

/**
 * @author mgormley
 */
public class DipMilpBlockSolver extends AbstractDipSolver implements IlpSolver {
    
    public DipMilpBlockSolver(File tempDir, int numThreads, double workMemMegs, BlockFileWriter bfw, int doCut, int doPriceAndCut) {
        super(tempDir, numThreads, workMemMegs, bfw, doCut, doPriceAndCut);
    }

    @Override
    protected String getDipBinary() {
        return dipDir + "/examples/MILPBlock/decomp_milpblock";
    }

    @Override
    protected String getSectionHeader() {
        return "MILPBlock";
    }
}
