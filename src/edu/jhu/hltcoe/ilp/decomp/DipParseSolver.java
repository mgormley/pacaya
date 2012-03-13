package edu.jhu.hltcoe.ilp.decomp;

import java.io.File;

import edu.jhu.hltcoe.ilp.IlpSolver;

/**
 * @author mgormley
 */
public class DipParseSolver extends AbstractDipSolver implements IlpSolver {
    
    private static final String dipParseBinary;
    static {
        File zb = new File("/Users/mgormley/Documents/JHU4_S10/dep_parse/dantzig_wolfe/dip_parse/decomp_parse");
        if (zb.exists()) {
            dipParseBinary = zb.getAbsolutePath();
        } else {
            dipParseBinary = "decomp_parse";
        }
    }
    
    public DipParseSolver(File tempDir, int numThreads, double workMemMegs, BlockFileWriter bfw, int doCut, int doPriceAndCut) {
        super(tempDir, numThreads, workMemMegs, bfw, doCut, doPriceAndCut);
    }

    @Override
    protected String getDipBinary() {
        return dipParseBinary;
    }

    @Override
    protected String getSectionHeader() {
        return "Parse";
    }
    
}
