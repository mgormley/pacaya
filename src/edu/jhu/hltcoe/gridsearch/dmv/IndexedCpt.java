package edu.jhu.hltcoe.gridsearch.dmv;

public interface IndexedCpt {

    public int getNumConds();

    public int getNumTotalParams();

    public int getNumParams(int c);

    public String getName(int c, int m);

    /**
     * Used by DmvBoundsFactory
     */
    public int[][] getTotalMaxFreqCm();

    public int getTotalMaxFreqCm(int c, int m);

    public int getNumNonZeroMaxFreqCms();

}