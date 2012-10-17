package edu.jhu.hltcoe.gridsearch.cpt;

public interface IndexedCpt {

    public int getNumConds();

    public int getNumTotalParams();

    public int getNumParams(int c);

    public String getName(int c, int m);

    /**
     * Used by DmvBoundsFactory
     */
    public int[][] getTotalMaxFreqCm();

    public int getUnsupervisedMaxTotalFreqCm(int c, int m);

    public int getNumNonZeroUnsupMaxFreqCms();

}